package com.example.data.filesystem

import android.content.Context
import android.os.Environment
import android.system.Os
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.UUID
import org.json.JSONObject

data class VaultLocation(val root: File, val fallbackReason: String? = null)
data class VaultNoteFile(val relativePath: String, val sizeBytes: Long)

/**
 * Physical shared vault. Existing files are never initialized with defaults or overwritten on
 * startup. The app-private preference stores only the user's chosen directory path.
 */
class VaultFileSystemManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "vault_storage_prefs", Context.MODE_PRIVATE
    )

    fun resolveLocation(): VaultLocation {
        val custom = preferences.getString("custom_vault_path", null)?.trim().orEmpty()
        if (custom.isNotEmpty()) {
            require(custom.startsWith("/")) { "custom_vault_path must be an absolute filesystem path." }
            val directory = File(custom)
            if (directory.isDirectory && directory.canRead() && directory.canWrite()) {
                return VaultLocation(directory.canonicalFile)
            }
        }
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "ObsidianVault"
        )
        if (directory.exists() && !directory.isDirectory) {
            throw IOException("Vault path is not a directory: ${directory.absolutePath}")
        }
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Cannot create shared vault at ${directory.absolutePath}")
        }
        if (!directory.canRead() || !directory.canWrite()) {
            throw IOException("No read/write access to shared vault: ${directory.absolutePath}")
        }
        return VaultLocation(
            directory.canonicalFile,
            if (custom.isNotEmpty()) "Custom vault is inaccessible; using the shared Download vault." else null
        )
    }

    fun setCustomVaultPath(path: String) {
        val clean = path.trim()
        if (clean.isEmpty()) {
            preferences.edit().remove("custom_vault_path").apply()
            return
        }
        require(clean.startsWith("/")) { "Enter an absolute filesystem path." }
        val directory = File(clean)
        require(directory.isDirectory && directory.canRead() && directory.canWrite()) {
            "Selected vault directory is not accessible: $clean"
        }
        preferences.edit().putString("custom_vault_path", directory.canonicalPath).apply()
    }

    fun customVaultPath(): String = preferences.getString("custom_vault_path", null).orEmpty()

    fun ensureVaultTopology(root: File = resolveLocation().root) {
        listOf(
            ".auth", ".config", ".database", ".chat", ".scripts", ".diagnostics",
            "Concepts", "Systems", "Wiki", "Daily Notes"
        ).forEach { name ->
            val directory = child(root, name)
            if (directory.exists() && !directory.isDirectory) {
                throw IOException("Vault path is not a directory: ${directory.absolutePath}")
            }
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Cannot create vault directory: ${directory.absolutePath}")
            }
        }
    }

    fun listNotes(root: File = resolveLocation().root): List<VaultNoteFile> {
        if (!root.isDirectory) throw IOException("Vault directory is unavailable.")
        return root.walkTopDown()
            .onEnter { it == root || (!it.name.startsWith(".") && it.canonicalPath.startsWith(root.canonicalPath + File.separator)) }
            .filter { it.isFile && it.extension.equals("md", ignoreCase = true) }
            .map { VaultNoteFile(it.relativeTo(root).invariantSeparatorsPath, it.length()) }
            .sortedBy { it.relativePath.lowercase() }
            .toList()
    }

    fun readNote(relativePath: String): String {
        val root = resolveLocation().root
        val file = child(root, relativePath)
        require(file.extension.equals("md", ignoreCase = true) && file.isFile) {
            "Markdown note does not exist: $relativePath"
        }
        return file.readText(StandardCharsets.UTF_8)
    }

    /** Explicit user save. Creation rejects collisions; editing requires overwrite=true. */
    fun saveNote(
        relativePath: String,
        content: String,
        overwrite: Boolean = false,
        expectedContent: String? = null
    ) {
        val root = resolveLocation().root
        val file = child(root, relativePath)
        require(file.extension.equals("md", ignoreCase = true)) { "Notes must have a .md extension." }
        file.parentFile?.let { parent ->
            if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Cannot create note directory.")
        }
        if (file.exists() && !overwrite) throw IOException("Note already exists: $relativePath")
        if (overwrite && expectedContent != null &&
            (!file.isFile || file.readText(StandardCharsets.UTF_8) != expectedContent)) {
            throw IOException("Note changed on disk; reload it before saving: $relativePath")
        }
        writeAtomic(file, content.toByteArray(StandardCharsets.UTF_8))
    }

    fun readJson(relativePath: String): JSONObject? {
        val file = child(resolveLocation().root, relativePath)
        require(file.extension.equals("json", ignoreCase = true)) { "Expected a JSON file." }
        return if (file.exists()) JSONObject(file.readText(StandardCharsets.UTF_8)) else null
    }

    fun writeJson(relativePath: String, json: JSONObject, overwrite: Boolean = true) {
        val file = child(resolveLocation().root, relativePath)
        require(file.extension.equals("json", ignoreCase = true)) { "Expected a JSON file." }
        file.parentFile?.let { parent ->
            if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Cannot create JSON directory.")
        }
        if (file.exists() && !overwrite) throw IOException("JSON file already exists: $relativePath")
        writeAtomic(file, json.toString(2).toByteArray(StandardCharsets.UTF_8))
    }

    private fun child(root: File, relativePath: String): File {
        require(relativePath.isNotBlank() && !File(relativePath).isAbsolute) { "Expected relative vault path." }
        val rootPath = root.canonicalPath
        val file = File(root, relativePath).canonicalFile
        require(file.path.startsWith(rootPath + File.separator)) { "Path escapes the shared vault." }
        return file
    }

    private fun writeAtomic(target: File, bytes: ByteArray) {
        val temp = File(target.parentFile, ".${target.name}.${UUID.randomUUID()}.tmp")
        try {
            FileOutputStream(temp).use { stream ->
                stream.write(bytes)
                stream.fd.sync()
            }
            // Linux rename replaces the old file in one operation; never delete it first.
            Os.rename(temp.absolutePath, target.absolutePath)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }
}
