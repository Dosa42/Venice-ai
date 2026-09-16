package com.example.data.local

import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class RootProcessRunnerTest {
    private fun runner() = RootProcessRunner { script -> Runtime.getRuntime().exec(arrayOf("/bin/sh", "-c", script)) }
    private suspend fun <T> files(block: suspend (File, File, File) -> T): T {
        val root = Files.createTempDirectory("venice-terminal-test").toFile()
        return try { block(root, File(root, "stdout"), File(root, "stderr")) } finally { root.deleteRecursively() }
    }

    @Test fun arbitraryCommandsCanWriteReadAndRemoveFilesAndReturnNonzeroExit() = runBlocking {
        files { root, out, err ->
            val path = NetHunterTerminal.quote(File(root, "file with ' quotes").path)
            val result = runner().run("printf 'actual file data' > $path; cat $path; rm $path; printf 'diagnostic' >&2; exit 7", out, err)
            assertEquals(7, result)
            assertEquals("actual file data", out.readText())
            assertEquals("diagnostic", err.readText())
            assertFalse(File(root, "file with ' quotes").exists())
        }
    }

    @Test fun completeLargeStdoutAndStderrAreRetained() = runBlocking {
        files { _, out, err ->
            val result = withTimeout(10_000) {
                runner().run("i=0; while [ \"\$i\" -lt 12000 ]; do printf '0123456789abcdef'; printf 'stderr-0123456789' >&2; i=\$((i + 1)); done", out, err)
            }
            assertEquals(0, result)
            assertEquals("0123456789abcdef".repeat(12000), out.readText())
            assertEquals("stderr-0123456789".repeat(12000), err.readText())
        }
    }

    @Test fun commandCanOutliveTheOriginalTwentySecondDeadline() = runBlocking {
        files { _, out, err ->
            assertEquals(0, withTimeout(30_000) { runner().run("sleep 21; printf finished", out, err) })
            assertEquals("finished", out.readText())
        }
    }

    @Test fun stdinIsAvailableToTheCommand() = runBlocking {
        files { _, out, err ->
            val result = withTimeout(5_000) {
                runner().run("read answer; printf '%s' \"\$answer\"; cat", out, err, onStarted = { process ->
                    process.outputStream.write("answer from stdin\nremaining input".toByteArray())
                    process.outputStream.close()
                })
            }
            assertEquals(0, result)
            assertEquals("answer from stdinremaining input", out.readText())
        }
    }

    @Test fun cancellationTerminatesTheRealProcess() = runBlocking {
        files { _, out, err ->
            val process = AtomicReference<Process>()
            val started = CompletableDeferred<Unit>()
            val job = launch {
                runner().run("exec sleep 60", out, err, onStarted = { process.set(it); started.complete(Unit) })
            }
            started.await()
            job.cancelAndJoin()
            withTimeout(5_000) {
                while (runCatching { process.get().exitValue() }.isFailure) delay(10)
            }
        }
    }

    @Test fun outputPagesPreserveUtf8AndAllBytesAcrossThePageBoundary() {
        val file = Files.createTempFile("venice-utf8", ".log").toFile()
        try {
            val text = "a".repeat(32767) + "🌍" + "z".repeat(40000)
            file.writeText(text)
            val rebuilt = StringBuilder()
            var offset = 0L
            while (offset < file.length()) {
                val page = NetHunterTerminal.page(file, offset)
                assertTrue(page.nextOffset > offset)
                rebuilt.append(page.text)
                offset = page.nextOffset
            }
            assertEquals(file.length(), offset)
            assertEquals(text, rebuilt.toString())
        } finally { file.delete() }
    }

    @Test fun chrootScriptPreservesConfiguredPathAndCommandArguments() = runBlocking {
        files { root, out, err ->
            val chroot = "/a path/'quoted chroot'"
            val dir = File(root, "work ' directory").also { it.mkdir() }
            val command = "printf '%s' 'literal \$HOME'; printf '\\n'; pwd"
            // A test executable records the chroot argument then launches real env/bash.
            // Entering the phone's rootfs remains an on-device test.
            File(root, "chroot").apply {
                writeText("#!/bin/sh\nprintf '%s\\n' \"\$1\"\nshift\nexec \"\$@\"\n")
                check(setExecutable(true))
            }
            val path = "export PATH=${NetHunterTerminal.quote(root.path)}:\$PATH\n"
            val bash = RootProcessRunner { script -> Runtime.getRuntime().exec(arrayOf("/bin/bash", "-c", script)) }
            val code = bash.run(path + NetHunterTerminal.buildScript(command, dir.path, "nethunter", chroot), out, err)
            assertEquals(err.readText(), 0, code)
            assertEquals("$chroot\nliteral \$HOME\n${dir.path}\n", out.readText())
        }
    }

    @Test fun incompleteLiveUtf8CharacterWaitsForItsRemainingBytes() {
        val file = Files.createTempFile("venice-live-utf8", ".log").toFile()
        try {
            val bytes = "a🌍".toByteArray()
            file.writeBytes(bytes.copyOf(3))
            val first = NetHunterTerminal.page(file, 0, finished = false)
            assertEquals("a", first.text)
            assertEquals(1L, first.nextOffset)
            file.appendBytes(bytes.copyOfRange(3, bytes.size))
            val last = NetHunterTerminal.page(file, first.nextOffset, finished = true)
            assertEquals("🌍", last.text)
            assertEquals(bytes.size.toLong(), last.nextOffset)
        } finally { file.delete() }
    }
}
