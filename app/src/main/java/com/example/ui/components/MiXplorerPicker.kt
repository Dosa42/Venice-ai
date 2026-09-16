package com.example.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File

/** Select a file in the user's existing MiXplorer installation and return its URI. */
internal class MiXplorerFileContract : ActivityResultContract<Unit, ActivityResult>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_GET_CONTENT).apply {
            setPackage("com.mixplorer")
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult = ActivityResult(resultCode, intent)
}

@Composable
internal fun rememberMiXplorerPicker(onFile: (Uri) -> Unit, onError: (String) -> Unit): () -> Unit {
    val currentOnFile = rememberUpdatedState(onFile)
    val currentOnError = rememberUpdatedState(onError)
    val launcher = rememberLauncherForActivityResult(MiXplorerFileContract()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) currentOnFile.value(uri)
            else currentOnError.value("MiXplorer returned no file URI.")
        }
    }
    return {
        try {
            launcher.launch(Unit)
        } catch (e: ActivityNotFoundException) {
            currentOnError.value("MiXplorer could not open its file picker (com.mixplorer).")
        } catch (e: Exception) {
            currentOnError.value("Could not open MiXplorer: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}

internal data class PickedTextFile(val name: String, val text: String, val size: Long)

/** Keep the existing text-import format while reading off the UI thread. */
internal suspend fun readPickedTextFile(context: Context, uri: Uri): PickedTextFile = withContext(Dispatchers.IO) {
    var name = uri.lastPathSegment ?: "document.txt"
    var size = 0L
    if (uri.scheme == "file") {
        val file = File(uri.path ?: throw IOException("Selected file has no path."))
        name = file.name
        size = file.length()
    } else context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IOException("MiXplorer's selected file could not be opened.")
    val text = if (bytes.size > 120_000) {
        String(bytes, 0, 120_000, Charsets.UTF_8) + "\n... [Truncated: File exceeded 120KB]"
    } else String(bytes, Charsets.UTF_8)
    PickedTextFile(name, text, if (size > 0) size else bytes.size.toLong())
}
