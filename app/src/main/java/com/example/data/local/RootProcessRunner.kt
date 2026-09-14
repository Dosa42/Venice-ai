package com.example.data.local

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal data class RootProcessResult(
    val exitCode: Int,
    val stdout: ByteArray,
    val stderr: ByteArray,
    val stdoutTruncated: Boolean,
    val stderrTruncated: Boolean,
)

/**
 * Runs only the repository's fixed scripts. Tests substitute a real /bin/sh process.
 * Both output pipes are drained while the process runs; retained output is bounded.
 */
internal class RootProcessRunner(
    private val startProcess: () -> Process = { Runtime.getRuntime().exec("su") },
) {
    suspend fun run(
        script: String,
        stdoutLimit: Int,
        timeoutMillis: Long = 20_000,
        stderrLimit: Int = 8_192,
    ): RootProcessResult {
        require(stdoutLimit > 0 && stderrLimit > 0 && timeoutMillis > 0)
        return withTimeoutOrNull(timeoutMillis) {
            withContext(Dispatchers.IO) {
                val process = try {
                    startProcess()
                } catch (error: Exception) {
                    throw LocalFileException("Cannot start su. A working root manager is required.", error)
                }
                val inputFailure = AtomicReference<Throwable?>(null)
                val inputFinished = AtomicBoolean(false)
                val stdout = BoundedCollector(stdoutLimit)
                val stderr = BoundedCollector(stderrLimit)
                // A separate writer prevents a blocked root approval prompt or full stdin
                // pipe from preventing timeout/cancellation or output draining.
                thread(name = "venice-root-input", isDaemon = true) {
                    try {
                        process.outputStream.use { stream ->
                            stream.write(script.toByteArray(Charsets.UTF_8))
                            stream.flush()
                        }
                    } catch (error: Exception) {
                        inputFailure.set(error)
                    } finally {
                        inputFinished.set(true)
                    }
                }
                try {
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        // Read only available bytes, never wait for EOF on a pipe that a
                        // descendant might inherit. Alternating pipes prevents deadlock.
                        stdout.drainAvailable(process.inputStream)
                        stderr.drainAvailable(process.errorStream)
                        val exitCode = try {
                            process.exitValue()
                        } catch (_: IllegalThreadStateException) {
                            null
                        }
                        if (exitCode != null && inputFinished.get()) {
                            stdout.drainAvailable(process.inputStream)
                            stderr.drainAvailable(process.errorStream)
                            if (process.inputStream.available() > 0 || process.errorStream.available() > 0) {
                                continue
                            }
                            if (exitCode == 0 && inputFailure.get() != null) {
                                throw LocalFileException("Could not deliver the complete root request.", inputFailure.get())
                            }
                            return@withContext RootProcessResult(
                                exitCode, stdout.bytes(), stderr.bytes(), stdout.truncated, stderr.truncated,
                            )
                        }
                        delay(10)
                    }
                    @Suppress("UNREACHABLE_CODE")
                    error("Unreachable")
                } catch (error: java.io.IOException) {
                    if (error is LocalFileException) throw error
                    throw LocalFileException("The root process connection failed: ${error.message}", error)
                } finally {
                    // Process.destroy/stream.close can wait on OS pipe locks. Cleanup is
                    // independent so coroutine cancellation never waits on those locks.
                    thread(name = "venice-root-cleanup", isDaemon = true) {
                        runCatching { process.destroy() }
                        runCatching { process.outputStream.close() }
                        runCatching { process.inputStream.close() }
                        runCatching { process.errorStream.close() }
                    }
                }
            }
        } ?: throw LocalFileException("Root operation timed out after ${timeoutMillis / 1_000.0} seconds. Check root approval and the requested path.")
    }

    private class BoundedCollector(private val limit: Int) {
        private val output = ByteArrayOutputStream(minOf(limit, 8_192))
        private val buffer = ByteArray(8_192)
        var truncated = false
            private set

        fun drainAvailable(stream: InputStream) {
            // Bound each drain pass so a continuous stdout producer cannot starve
            // stderr, cancellation, or the deadline.
            repeat(16) {
                val available = stream.available()
                if (available <= 0) return
                val count = stream.read(buffer, 0, minOf(available, buffer.size))
                if (count <= 0) return
                val retained = minOf(count, limit - output.size())
                output.write(buffer, 0, retained)
                if (retained < count) truncated = true
            }
        }

        fun bytes(): ByteArray = output.toByteArray()
    }
}
