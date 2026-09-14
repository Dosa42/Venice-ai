package com.example.data.local

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

/** Read-only observations of the filesystem visible to the app's granted su process. */
class RootFileRepository internal constructor(private val runner: RootProcessRunner) {
    constructor() : this(RootProcessRunner())

    suspend fun listDirectory(path: String = "/root"): DirectorySnapshot {
        val checkedPath = validatePath(path)
        val result = runner.run(directoryScript(checkedPath), MAX_DIRECTORY_BYTES)
        val body = checkedBody(result)
        val entries = ArrayList<LocalFileEntry>()
        var offset = 0
        var truncated = result.stdoutTruncated
        while (offset < body.size) {
            val kindEnd = body.indexOfZero(offset)
            val nameEnd = if (kindEnd >= 0) body.indexOfZero(kindEnd + 1) else -1
            if (kindEnd < 0 || nameEnd < 0) {
                if (result.stdoutTruncated) break
                throw LocalFileException("The root directory response was incomplete.")
            }
            if (entries.size == MAX_ENTRIES) {
                truncated = true
                break
            }
            val type = body.copyOfRange(offset, kindEnd).toString(Charsets.US_ASCII)
            val relativeName = decodeUtf8(body.copyOfRange(kindEnd + 1, nameEnd), false).first
            if (!relativeName.startsWith("./") || relativeName.length == 2 || '/' in relativeName.substring(2)) {
                throw LocalFileException("The root directory response contained an invalid entry.")
            }
            val kind = when (type) {
                "d" -> LocalEntryKind.DIRECTORY
                "f" -> LocalEntryKind.FILE
                "l" -> LocalEntryKind.SYMLINK
                "o" -> LocalEntryKind.OTHER
                else -> throw LocalFileException("The root directory response contained an invalid entry type.")
            }
            val name = relativeName.substring(2)
            entries += LocalFileEntry(name, checkedPath.trimEnd('/') + "/" + name, kind)
            offset = nameEnd + 1
        }
        return DirectorySnapshot(
            checkedPath,
            entries.sortedWith(compareBy<LocalFileEntry> { it.kind != LocalEntryKind.DIRECTORY }.thenBy { it.name }),
            truncated,
            System.currentTimeMillis(),
        )
    }

    suspend fun readTextFile(path: String): FileSnapshot {
        val checkedPath = validatePath(path)
        val result = runner.run(fileScript(checkedPath), ROOT_MARKER.size + MAX_FILE_BYTES + 1)
        val body = checkedBody(result)
        val truncated = body.size > MAX_FILE_BYTES || result.stdoutTruncated
        val captured = body.copyOf(minOf(body.size, MAX_FILE_BYTES))
        if (captured.any { it == 0.toByte() }) {
            throw LocalFileException("This file contains binary data. Only UTF-8 text can be attached.")
        }
        val (text, bytesConsumed) = decodeUtf8(captured, truncated)
        return FileSnapshot(checkedPath, text, bytesConsumed, truncated, System.currentTimeMillis())
    }

    private fun checkedBody(result: RootProcessResult): ByteArray {
        if (result.exitCode != 0) {
            val detail = result.stderr.toString(Charsets.UTF_8).trim().take(2_000)
            val suffix = if (result.stderrTruncated) " (error output truncated)" else ""
            throw LocalFileException(
                "Root operation failed (exit ${result.exitCode})" +
                    if (detail.isEmpty()) ". Root may have been denied." else ": $detail$suffix",
            )
        }
        if (result.stdout.size < ROOT_MARKER.size ||
            !result.stdout.copyOfRange(0, ROOT_MARKER.size).contentEquals(ROOT_MARKER)
        ) {
            throw LocalFileException("Root verification failed: the process did not return a verified uid 0 response.")
        }
        return result.stdout.copyOfRange(ROOT_MARKER.size, result.stdout.size)
    }

    companion object {
        const val MAX_FILE_BYTES = 32 * 1024
        const val MAX_ENTRIES = 512
        private const val MAX_DIRECTORY_BYTES = 2 * 1024 * 1024
        private val ROOT_MARKER = "VENICE_ROOT_V1\u0000".toByteArray(Charsets.US_ASCII)

        internal fun validatePath(path: String): String {
            if (!path.startsWith('/') || '\u0000' in path || path.toByteArray(Charsets.UTF_8).size > 4_096) {
                throw LocalFileException("Enter an absolute filesystem path of at most 4096 UTF-8 bytes, without NUL characters.")
            }
            return path.trimEnd('/').ifEmpty { "/" }
        }

        internal fun shellQuote(value: String): String {
            require('\u0000' !in value) { "A shell argument cannot contain NUL." }
            return "'" + value.replace("'", "'\"'\"'") + "'"
        }

        private fun rootPreamble(): String = """
            PATH=/system/bin:/system/xbin:/sbin:/usr/bin:/bin
            export PATH
            LC_ALL=C
            export LC_ALL
            uid=${'$'}(id -u) || { printf '%s\n' 'Cannot verify root uid.' >&2; exit 77; }
            [ "${'$'}uid" = 0 ] || { printf '%s\n' 'Root access denied: uid is not 0.' >&2; exit 77; }
            printf 'VENICE_ROOT_V1\000'
        """.trimIndent() + "\n"

        internal fun directoryScript(path: String): String = rootPreamble() + "requested=${shellQuote(path)}\n" + """
            [ -d "${'$'}requested" ] || { printf '%s\n' 'The requested directory does not exist or is inaccessible.' >&2; exit 72; }
            cd "${'$'}requested" || exit 73
            find . -mindepth 1 -maxdepth 1 -exec sh -c '
              for entry do
                if [ -L "${'$'}entry" ]; then kind=l
                elif [ -d "${'$'}entry" ]; then kind=d
                elif [ -f "${'$'}entry" ]; then kind=f
                else kind=o
                fi
                printf "%s\000%s\000" "${'$'}kind" "${'$'}entry" || exit 74
              done
            ' sh {} +
            result=${'$'}?
            exit "${'$'}result"
        """.trimIndent() + "\n"

        internal fun fileScript(path: String): String = rootPreamble() + "requested=${shellQuote(path)}\n" + """
            [ -f "${'$'}requested" ] || { printf '%s\n' 'The requested path is not a readable regular file.' >&2; exit 72; }
            head -c ${MAX_FILE_BYTES + 1} "${'$'}requested"
            result=${'$'}?
            exit "${'$'}result"
        """.trimIndent() + "\n"

        private fun ByteArray.indexOfZero(start: Int): Int {
            for (index in start until size) if (this[index] == 0.toByte()) return index
            return -1
        }

        private fun decodeUtf8(bytes: ByteArray, truncated: Boolean): Pair<String, Int> {
            val input = ByteBuffer.wrap(bytes)
            val chars = CharBuffer.allocate(bytes.size)
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val decoded = decoder.decode(input, chars, !truncated)
            if (decoded.isError) {
                throw LocalFileException("The requested data is not valid UTF-8 text.")
            }
            chars.flip()
            return chars.toString() to input.position()
        }
    }
}
