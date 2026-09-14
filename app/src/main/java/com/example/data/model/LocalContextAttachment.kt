package com.example.data.model

/** A user-selected, read-only filesystem snapshot held in the current chat session. */
data class LocalContextAttachment(
    val path: String,
    val kind: String,
    val capturedAt: Long,
    val content: String,
    val truncated: Boolean = false
) {
    init {
        require(kind == "directory" || kind == "text") {
            "Local context kind must be directory or text"
        }
    }
}
