package com.example.data.local

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Reused from feature/root-file-explorer (ae0fb6c).
 * The same su process, alternating pipe drain and cancellation cleanup now serve
 * arbitrary commands. Complete output is spooled to files without a deadline or
 * capture limit. Passing the script with -c leaves stdin available to the command.
 */
internal class RootProcessRunner(
    private val startProcess: (String) -> Process = { script ->
        Runtime.getRuntime().exec(arrayOf("su", "-c", script))
    },
) {
    suspend fun run(
        script: String,
        stdoutFile: File,
        stderrFile: File,
        onStarted: (Process) -> Unit = {},
        onOutput: () -> Unit = {},
    ): Int = withContext(Dispatchers.IO) {
        val process = startProcess(script)
        try {
            stdoutFile.outputStream().use { stdout ->
                stderrFile.outputStream().use { stderr ->
                    onStarted(process)
                    val buffer = ByteArray(8_192)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val wroteOut = drainAvailable(process.inputStream, stdout, buffer)
                        val wroteErr = drainAvailable(process.errorStream, stderr, buffer)
                        if (wroteOut || wroteErr) onOutput()
                        val exitCode = try {
                            process.exitValue()
                        } catch (_: IllegalThreadStateException) {
                            null
                        }
                        if (exitCode != null) {
                            if (process.inputStream.available() > 0 || process.errorStream.available() > 0) continue
                            return@withContext exitCode
                        }
                        delay(10)
                    }
                    @Suppress("UNREACHABLE_CODE")
                    error("Unreachable")
                }
            }
        } finally {
            // OS pipe locks must not block coroutine cancellation or the UI.
            thread(name = "venice-root-cleanup", isDaemon = true) {
                runCatching { process.destroy() }
                runCatching { process.outputStream.close() }
                runCatching { process.inputStream.close() }
                runCatching { process.errorStream.close() }
            }
        }
    }

    private fun drainAvailable(stream: InputStream, output: OutputStream, buffer: ByteArray): Boolean {
        var wrote = false
        // Fair scheduling between stdout, stderr and cancellation; no bytes are discarded.
        repeat(16) {
            val available = stream.available()
            if (available <= 0) return wrote
            val count = stream.read(buffer, 0, minOf(available, buffer.size))
            if (count <= 0) return wrote
            output.write(buffer, 0, count)
            wrote = true
        }
        return wrote
    }
}
