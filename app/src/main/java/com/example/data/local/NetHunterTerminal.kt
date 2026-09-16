package com.example.data.local

import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class TerminalCommand(
    val id: String,
    val command: String,
    val workdir: String,
    val environment: String,
    val status: String = "starting",
    val exitCode: Int? = null,
    val error: String? = null,
    val stdoutPreview: String = "",
    val stderrPreview: String = "",
    val stdoutPath: String,
    val stderrPath: String,
)

internal data class OutputPage(val text: String, val nextOffset: Long, val totalBytes: Long)
internal data class TerminalOutput(val command: TerminalCommand, val stdout: OutputPage, val stderr: OutputPage)

/** Actual root command sessions. The configured chroot is used without mounting or changing it. */
internal class NetHunterTerminal(
    private val scope: CoroutineScope,
    private val directory: File,
    private val runner: RootProcessRunner = RootProcessRunner(),
) {
    private class Running(val record: TerminalCommand) {
        @Volatile var process: Process? = null
        lateinit var job: Job
        val inputMutex = Mutex()
        var lastUpdate = 0L
    }
    private val running = ConcurrentHashMap<String, Running>()
    private val _commands = MutableStateFlow<List<TerminalCommand>>(emptyList())
    val commands = _commands.asStateFlow()

    suspend fun start(command: String, workdir: String, environment: String, chroot: String): TerminalOutput {
        val script = buildScript(command, workdir, environment, chroot)
        val id = UUID.randomUUID().toString()
        val folder = File(directory, id)
        withContext(Dispatchers.IO) { check(folder.mkdirs()) { "Cannot create terminal output directory: $folder" } }
        val item = Running(TerminalCommand(id, command, workdir, environment,
            stdoutPath = File(folder, "stdout").absolutePath, stderrPath = File(folder, "stderr").absolutePath))
        item.job = scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
            try {
                val code = runner.run(script, File(item.record.stdoutPath), File(item.record.stderrPath),
                    onStarted = { process ->
                        item.process = process
                        update(id) { it.copy(status = "running") }
                    },
                    onOutput = {
                        val now = System.nanoTime()
                        if (now - item.lastUpdate >= 250_000_000L) {
                            item.lastUpdate = now
                            refreshPreview(id)
                        }
                    })
                update(id) { it.copy(status = "completed", exitCode = code) }
            } catch (e: CancellationException) {
                update(id) { it.copy(status = "cancelled") }
                throw e
            } catch (e: Exception) {
                update(id) { it.copy(status = "failed", error = e.message ?: e.javaClass.simpleName) }
            } finally {
                item.process = null
                refreshPreview(id)
            }
        }
        running[id] = item
        _commands.update { it + item.record }
        item.job.start()
        // Yield an active session for long commands; this does not terminate the process.
        waitForOutput(id)
        return read(id, 0, 0)
    }

    suspend fun read(id: String, stdoutOffset: Long, stderrOffset: Long): TerminalOutput = withContext(Dispatchers.IO) {
        val record = find(id)
        val finished = record.status !in listOf("starting", "running")
        TerminalOutput(record, page(File(record.stdoutPath), stdoutOffset, finished = finished),
            page(File(record.stderrPath), stderrOffset, finished = finished))
    }

    suspend fun poll(id: String, stdoutOffset: Long, stderrOffset: Long): TerminalOutput {
        waitForOutput(id)
        return read(id, stdoutOffset, stderrOffset)
    }

    suspend fun write(id: String, text: String, closeStdin: Boolean) = withContext(Dispatchers.IO) {
        val item = running[id] ?: error("Unknown terminal session: $id")
        item.inputMutex.withLock {
            val process = item.process ?: error("Root process has not started yet.")
            process.outputStream.write(text.toByteArray(Charsets.UTF_8))
            process.outputStream.flush()
            if (closeStdin) process.outputStream.close()
        }
    }

    fun stop(id: String) {
        val item = running[id] ?: error("Unknown terminal session: $id")
        item.job.cancel()
        update(id) { if (it.status in listOf("starting", "running")) it.copy(status = "cancelled") else it }
    }

    fun stopAll() { running.keys.forEach(::stop) }

    private suspend fun waitForOutput(id: String) {
        repeat(20) {
            if (find(id).status !in listOf("starting", "running")) return
            delay(50)
        }
    }

    private fun find(id: String) = _commands.value.find { it.id == id } ?: error("Unknown terminal session: $id")
    private fun update(id: String, change: (TerminalCommand) -> TerminalCommand) {
        _commands.update { list -> list.map { if (it.id == id) change(it) else it } }
    }
    private fun refreshPreview(id: String) {
        val item = find(id)
        val out = tail(File(item.stdoutPath))
        val err = tail(File(item.stderrPath))
        update(id) { it.copy(stdoutPreview = out, stderrPreview = err) }
    }

    companion object {
        internal fun quote(value: String): String {
            require('\u0000' !in value) { "Shell arguments cannot contain NUL bytes." }
            return "'" + value.replace("'", "'\"'\"'") + "'"
        }

        internal fun buildScript(command: String, workdir: String, environment: String, chroot: String): String {
            val body = "cd -- ${quote(workdir)} || exit\n$command"
            return when (environment) {
                "android" -> "exec /system/bin/sh -c ${quote(body)}"
                "nethunter" -> {
                    val args = "${quote(chroot)} /usr/bin/env HOME=/root USER=root LOGNAME=root TERM=xterm-256color " +
                        "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /bin/bash -c ${quote(body)}"
                    """
                        if command -v chroot >/dev/null 2>&1; then
                            exec chroot $args
                        elif command -v busybox >/dev/null 2>&1; then
                            exec busybox chroot $args
                        elif [ -x /data/adb/magisk/busybox ]; then
                            exec /data/adb/magisk/busybox chroot $args
                        else
                            printf '%s\n' 'No chroot executable found in the root shell.' >&2
                            exit 127
                        fi
                    """.trimIndent()
                }
                else -> error("Unknown shell environment: $environment")
            }
        }

        // Page size controls transport memory only. The complete byte stream stays on disk.
        internal fun page(file: File, offset: Long, bytesPerPage: Int = 32_768, finished: Boolean = true): OutputPage {
            require(offset >= 0) { "Output offset must be nonnegative." }
            if (!file.exists()) return OutputPage("", offset, 0)
            return RandomAccessFile(file, "r").use { input ->
                val total = input.length()
                require(offset <= total) { "Output offset exceeds available bytes." }
                input.seek(offset)
                val bytes = ByteArray(minOf(bytesPerPage.toLong(), total - offset).toInt())
                input.readFully(bytes)
                var count = bytes.size
                // Keep a UTF-8 character together when a page boundary splits it.
                if ((offset + count < total || !finished) && count > 0) {
                    var start = count - 1
                    while (start > 0 && bytes[start].toInt() and 0xC0 == 0x80) start--
                    val lead = bytes[start].toInt() and 0xFF
                    val width = when { lead and 0xF8 == 0xF0 -> 4; lead and 0xF0 == 0xE0 -> 3; lead and 0xE0 == 0xC0 -> 2; else -> 1 }
                    if (start + width > count) count = start
                }
                OutputPage(String(bytes, 0, count, Charsets.UTF_8), offset + count, total)
            }
        }

        private fun tail(file: File): String = runCatching {
            page(file, maxOf(0, file.length() - 4_096), 4_096).text
        }.getOrDefault("")
    }
}
