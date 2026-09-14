package com.example.data.local

import java.io.IOException

enum class LocalEntryKind { DIRECTORY, FILE, SYMLINK, OTHER }

data class LocalFileEntry(val name: String, val path: String, val kind: LocalEntryKind)

data class DirectorySnapshot(
    val path: String,
    val entries: List<LocalFileEntry>,
    val truncated: Boolean,
    val capturedAtEpochMs: Long,
)

data class FileSnapshot(
    val path: String,
    val text: String,
    val bytesCaptured: Int,
    val truncated: Boolean,
    val capturedAtEpochMs: Long,
)

/** A failed local operation. A failed root request never yields a successful snapshot. */
class LocalFileException(message: String, cause: Throwable? = null) : IOException(message, cause)
