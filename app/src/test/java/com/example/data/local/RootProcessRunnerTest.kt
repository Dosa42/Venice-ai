package com.example.data.local

import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** These tests execute the host shell and real OS pipes; they never substitute fake su output. */
class RootProcessRunnerTest {
    private fun shellRunner() = RootProcessRunner { Runtime.getRuntime().exec(arrayOf("/bin/sh")) }

    @Test fun shellQuotePreservesMetacharactersWithoutExecution() = runBlocking {
        val directory = Files.createTempDirectory("venice-quote").toFile()
        try {
            val marker = directory.resolve("must-not-exist")
            val text = "apostrophe' newline\n tab\t dollar\$(touch ${marker.absolutePath}) `touch ${marker.absolutePath}`; & | >"
            val result = shellRunner().run("printf '%s' ${RootFileRepository.shellQuote(text)}\n", 4_096)
            assertEquals(0, result.exitCode)
            assertArrayEquals(text.toByteArray(), result.stdout)
            assertFalse(marker.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun largeStdoutAndStderrAreDrainedAndRetentionIsBounded() = runBlocking {
        val result = shellRunner().run(
            "i=0; while [ \"\$i\" -lt 10000 ]; do printf 'stdout1234567890'; printf 'stderr1234567890' >&2; i=\$((i + 1)); done\n",
            stdoutLimit = 128,
            stderrLimit = 96,
        )
        assertEquals(0, result.exitCode)
        assertEquals(128, result.stdout.size)
        assertEquals(96, result.stderr.size)
        assertTrue(result.stdoutTruncated)
        assertTrue(result.stderrTruncated)
    }

    @Test fun nonzeroExitAndStderrArePreserved() = runBlocking {
        val result = shellRunner().run("printf 'permission denied' >&2\nexit 77\n", 128)
        assertEquals(77, result.exitCode)
        assertEquals("permission denied", result.stderr.toString(Charsets.UTF_8))
        assertEquals(0, result.stdout.size)
    }

    @Test fun timeoutReturnsPromptlyAndDestroysProcess() = runBlocking {
        val process = AtomicReference<Process>()
        val runner = RootProcessRunner {
            Runtime.getRuntime().exec(arrayOf("/bin/sh")).also(process::set)
        }
        val start = System.nanoTime()
        try {
            runner.run("exec sleep 30\n", stdoutLimit = 128, timeoutMillis = 200)
            fail("Expected an explicit timeout")
        } catch (error: LocalFileException) {
            assertTrue(error.message.orEmpty().contains("timed out"))
        }
        assertTrue((System.nanoTime() - start) / 1_000_000 < 4_000)
        awaitExit(process.get())
    }

    @Test fun cancellationPropagatesAndDestroysProcess() = runBlocking {
        val started = CompletableDeferred<Process>()
        val runner = RootProcessRunner {
            Runtime.getRuntime().exec(arrayOf("/bin/sh")).also { started.complete(it) }
        }
        var cancelled = false
        val job = launch {
            try {
                runner.run("exec sleep 30\n", 128)
            } catch (error: CancellationException) {
                cancelled = true
                throw error
            }
        }
        val process = started.await()
        job.cancelAndJoin()
        assertTrue(cancelled)
        awaitExit(process)
    }

    @Test fun invalidPathsFailBeforeShellExecution() {
        listOf("relative", "", "/nul\u0000tail", "/" + "x".repeat(4_096)).forEach { path ->
            try {
                RootFileRepository.validatePath(path)
                fail("Expected invalid path rejection")
            } catch (_: LocalFileException) { }
        }
        assertEquals("/root", RootFileRepository.validatePath("/root/"))
    }

    private suspend fun awaitExit(process: Process) {
        repeat(200) {
            try {
                process.exitValue()
                return
            } catch (_: IllegalThreadStateException) {
                delay(10)
            }
        }
        fail("The timed out or cancelled shell is still running")
    }
}
