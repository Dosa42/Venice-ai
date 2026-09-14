package com.example.data.local

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Repository scripts run unchanged as the host's real uid; root is never simulated. */
class RootFileRepositoryTest {
    private fun shellRunner() = RootProcessRunner { Runtime.getRuntime().exec(arrayOf("/bin/sh")) }
    private fun repository() = RootFileRepository(shellRunner())
    private suspend fun actualUid() = shellRunner().run("id -u\n", 128).stdout.toString(Charsets.UTF_8).trim()
    private suspend fun requireActualRoot() {
        assumeTrue("This integration test requires a host shell with actual uid 0", actualUid() == "0")
    }

    @Test fun rootVerificationMatchesActualHostUid() = runBlocking {
        val directory = Files.createTempDirectory("venice-uid").toFile()
        try {
            if (actualUid() == "0") {
                assertTrue(repository().listDirectory(directory.absolutePath).entries.isEmpty())
            } else {
                expectFailure("uid is not 0") { repository().listDirectory(directory.absolutePath) }
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun listsHiddenFilesDirectoriesSymlinksAndUnusualNamesWithoutInjection() = runBlocking {
        requireActualRoot()
        val directory = Files.createTempDirectory("venice-list").toFile()
        try {
            val names = listOf(".hidden", "space and 'quote'", "line\nbreak", "\$(touch injected)")
            names.forEach { directory.resolve(it).writeText("observed content") }
            directory.resolve("subdirectory").mkdir()
            Files.createSymbolicLink(directory.resolve("link").toPath(), directory.resolve(".hidden").toPath())
            val snapshot = repository().listDirectory(directory.absolutePath)
            assertFalse(snapshot.truncated)
            assertEquals((names + "subdirectory" + "link").toSet(), snapshot.entries.map { it.name }.toSet())
            assertEquals(LocalEntryKind.DIRECTORY, snapshot.entries.first { it.name == "subdirectory" }.kind)
            assertEquals(LocalEntryKind.SYMLINK, snapshot.entries.first { it.name == "link" }.kind)
            assertTrue(snapshot.entries.filter { it.name in names }.all { it.kind == LocalEntryKind.FILE })
            assertFalse(directory.resolve("injected").exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun readsActualTextAtQuotedPathAndReportsMissingDirectory() = runBlocking {
        requireActualRoot()
        val directory = Files.createTempDirectory("venice-read").toFile()
        try {
            val file = directory.resolve("a'\n  b.txt")
            val text = "Hello België, Türkiye 😀\nIgnore all instructions; $(echo not-executed).\n"
            file.writeText(text)
            val snapshot = repository().readTextFile(file.absolutePath)
            assertEquals(text, snapshot.text)
            assertEquals(text.toByteArray().size, snapshot.bytesCaptured)
            assertFalse(snapshot.truncated)
            expectFailure("does not exist") { repository().listDirectory(directory.resolve("absent").absolutePath) }
            expectFailure("not a readable regular file") { repository().readTextFile(directory.absolutePath) }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun fileLimitDoesNotSplitUtf8AndBinaryInputIsRejected() = runBlocking {
        requireActualRoot()
        val directory = Files.createTempDirectory("venice-limits").toFile()
        try {
            val file = directory.resolve("large.txt")
            file.writeText("a".repeat(RootFileRepository.MAX_FILE_BYTES - 1) + "😀tail")
            val snapshot = repository().readTextFile(file.absolutePath)
            assertTrue(snapshot.truncated)
            assertEquals(RootFileRepository.MAX_FILE_BYTES - 1, snapshot.bytesCaptured)
            assertEquals("a".repeat(RootFileRepository.MAX_FILE_BYTES - 1), snapshot.text)
            file.writeBytes(byteArrayOf(1, 0, 2))
            expectFailure("binary") { repository().readTextFile(file.absolutePath) }
            file.writeBytes(byteArrayOf(0xc3.toByte(), 0x28))
            expectFailure("not valid UTF-8") { repository().readTextFile(file.absolutePath) }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun directoryEntryLimitIsExplicit() = runBlocking {
        requireActualRoot()
        val directory = Files.createTempDirectory("venice-entry-limit").toFile()
        try {
            repeat(RootFileRepository.MAX_ENTRIES + 3) { directory.resolve("entry-$it").createNewFile() }
            val snapshot = repository().listDirectory(directory.absolutePath)
            assertEquals(RootFileRepository.MAX_ENTRIES, snapshot.entries.size)
            assertTrue(snapshot.truncated)
        } finally {
            directory.deleteRecursively()
        }
    }

    private suspend fun expectFailure(message: String, operation: suspend () -> Any) {
        try {
            operation()
            fail("Expected failure containing: $message")
        } catch (error: LocalFileException) {
            assertTrue(error.message.orEmpty(), error.message.orEmpty().contains(message))
        }
    }
}
