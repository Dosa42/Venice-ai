package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LocalEntryKind
import com.example.data.local.LocalFileEntry
import com.example.ui.theme.VeniceAmber
import com.example.ui.theme.VeniceBackground
import com.example.ui.theme.VeniceBorder
import com.example.ui.theme.VeniceRose
import com.example.ui.theme.VeniceSurface
import com.example.ui.theme.VeniceSurfaceElevated
import com.example.ui.theme.VeniceTextPrimary
import com.example.ui.theme.VeniceTextSecondary
import com.example.ui.viewmodel.VeniceUiState
import com.example.ui.viewmodel.VeniceViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun LocalFilesScreen(
    viewModel: VeniceViewModel,
    uiState: VeniceUiState,
    modifier: Modifier = Modifier
) {
    val localFiles = uiState.localFiles
    val directory = localFiles.directory
    val file = localFiles.file
    val inputPath = localFiles.pathInput.trimEnd('/')
    val parentPath = inputPath.substringBeforeLast('/', "").ifEmpty { "/" }
    val canRead = !localFiles.busy && localFiles.pathInput.isNotBlank()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VeniceBackground)
            .testTag("local_files_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Local Files",
                    color = VeniceTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Browse directories and read text using device root access. Grant root in your device's root manager when prompted.",
                    color = VeniceTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        item(key = "path") {
            LocalFilesCard {
                OutlinedTextField(
                    value = localFiles.pathInput,
                    onValueChange = viewModel::setLocalPath,
                    enabled = !localFiles.busy,
                    label = { Text("Absolute path") },
                    placeholder = { Text("/root") },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VeniceTextPrimary,
                        unfocusedTextColor = VeniceTextPrimary,
                        disabledTextColor = VeniceTextSecondary,
                        focusedBorderColor = VeniceAmber,
                        unfocusedBorderColor = VeniceBorder,
                        focusedLabelColor = VeniceAmber,
                        unfocusedLabelColor = VeniceTextSecondary,
                        focusedContainerColor = VeniceSurfaceElevated,
                        unfocusedContainerColor = VeniceSurfaceElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("local_path_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.browseLocalDirectory() },
                        enabled = canRead,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VeniceAmber,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_local_directory")
                    ) {
                        Text("Open directory", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.previewLocalFile() },
                        enabled = canRead,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VeniceAmber),
                        border = BorderStroke(1.dp, VeniceBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("read_local_file")
                    ) {
                        Text("Read text", fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { viewModel.browseLocalDirectory("/root") },
                        enabled = !localFiles.busy,
                        colors = ButtonDefaults.textButtonColors(contentColor = VeniceAmber)
                    ) {
                        Text("/root", fontFamily = FontFamily.Monospace)
                    }
                    TextButton(
                        onClick = { viewModel.browseLocalDirectory(parentPath) },
                        enabled = !localFiles.busy && inputPath.startsWith('/') && inputPath.isNotEmpty(),
                        colors = ButtonDefaults.textButtonColors(contentColor = VeniceAmber),
                        modifier = Modifier.testTag("local_parent_directory")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Up", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }

        if (localFiles.busy) {
            item(key = "progress") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = VeniceAmber,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Reading local device…",
                        color = VeniceTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = viewModel::cancelLocalRead) {
                        Text("Cancel", color = VeniceAmber)
                    }
                }
            }
        }

        localFiles.error?.let { error ->
            item(key = "error") {
                LocalFilesCard {
                    Text(
                        text = error,
                        color = VeniceRose,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.testTag("local_files_error")
                    )
                }
            }
        }

        item(key = "sharing_notice") {
            Text(
                text = "Attach a snapshot to chat, then press Send to share it with Gemini. It stays in the current conversation context. A snapshot is captured data, not live device access.",
                color = VeniceTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        if (file != null) {
            item(key = "file_preview") {
                LocalFilesCard {
                    Text(
                        text = "Text preview",
                        color = VeniceAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    SelectionContainer {
                        Text(
                            text = file.path,
                            color = VeniceTextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                    LocalSnapshotDetails(
                        capturedAtEpochMs = file.capturedAtEpochMs,
                        detail = "${file.bytesCaptured} bytes captured",
                        truncated = file.truncated,
                        truncationText = "Capture limit reached. This preview and attachment contain only the captured beginning of the file."
                    )
                    key(file.path, file.capturedAtEpochMs) {
                        SelectionContainer {
                            Text(
                                text = file.text.ifEmpty { "(Empty text file)" },
                                color = VeniceTextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 96.dp, max = 360.dp)
                                    .background(VeniceBackground, RoundedCornerShape(8.dp))
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp)
                                    .testTag("local_file_preview")
                            )
                        }
                    }
                    Button(
                        onClick = viewModel::attachLocalFile,
                        enabled = !localFiles.busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VeniceAmber,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attach_local_file")
                    ) {
                        Text("Attach text to chat", fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = viewModel::clearLocalPreview,
                        enabled = !localFiles.busy,
                        colors = ButtonDefaults.textButtonColors(contentColor = VeniceTextSecondary)
                    ) {
                        Text("Clear preview")
                    }
                }
            }
        }

        if (directory != null) {
            item(key = "directory_summary") {
                LocalFilesCard {
                    Text(
                        text = "Directory snapshot",
                        color = VeniceAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    SelectionContainer {
                        Text(
                            text = directory.path,
                            color = VeniceTextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                    LocalSnapshotDetails(
                        capturedAtEpochMs = directory.capturedAtEpochMs,
                        detail = "${directory.entries.size} entries captured",
                        truncated = directory.truncated,
                        truncationText = "Capture limit reached. This listing and attachment contain only the captured entries."
                    )
                    Button(
                        onClick = viewModel::attachLocalDirectory,
                        enabled = !localFiles.busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VeniceAmber,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attach_local_directory")
                    ) {
                        Text("Attach listing to chat", fontWeight = FontWeight.SemiBold)
                    }
                    if (directory.entries.isEmpty()) {
                        Text("This directory is empty.", color = VeniceTextSecondary, fontSize = 12.sp)
                    }
                }
            }

            items(directory.entries, key = { "entry:${it.path}" }) { entry ->
                LocalDirectoryEntry(
                    entry = entry,
                    enabled = !localFiles.busy,
                    openDirectory = { viewModel.browseLocalDirectory(entry.path) },
                    readFile = { viewModel.previewLocalFile(entry.path) }
                )
            }
        }
    }
}

@Composable
private fun LocalFilesCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = VeniceSurface),
        border = BorderStroke(1.dp, VeniceBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun LocalSnapshotDetails(
    capturedAtEpochMs: Long,
    detail: String,
    truncated: Boolean,
    truncationText: String
) {
    Text(
        text = "$detail · ${if (truncated) "Partial snapshot" else "Complete snapshot"}\nCaptured ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(capturedAtEpochMs))}",
        color = VeniceTextSecondary,
        fontSize = 11.sp,
        lineHeight = 17.sp
    )
    if (truncated) {
        Text(
            text = truncationText,
            color = VeniceAmber,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun LocalDirectoryEntry(
    entry: LocalFileEntry,
    enabled: Boolean,
    openDirectory: () -> Unit,
    readFile: () -> Unit
) {
    val canOpen = entry.kind == LocalEntryKind.DIRECTORY || entry.kind == LocalEntryKind.FILE
    val icon = when (entry.kind) {
        LocalEntryKind.DIRECTORY -> Icons.Default.Folder
        LocalEntryKind.FILE -> Icons.Default.Description
        LocalEntryKind.SYMLINK -> Icons.Default.Link
        LocalEntryKind.OTHER -> Icons.Default.InsertDriveFile
    }
    val kindDescription = when (entry.kind) {
        LocalEntryKind.DIRECTORY -> "Directory · Tap to open"
        LocalEntryKind.FILE -> "File · Tap to read text"
        LocalEntryKind.SYMLINK -> "Symbolic link · Choose how to open"
        LocalEntryKind.OTHER -> "Special entry · Text preview unavailable"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && canOpen) {
                if (entry.kind == LocalEntryKind.DIRECTORY) openDirectory() else readFile()
            },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = VeniceSurfaceElevated),
        border = BorderStroke(1.dp, VeniceBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VeniceAmber,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.name,
                        color = VeniceTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    Text(
                        text = kindDescription,
                        color = VeniceTextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (entry.kind == LocalEntryKind.SYMLINK) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = openDirectory,
                        enabled = enabled,
                        colors = ButtonDefaults.textButtonColors(contentColor = VeniceAmber)
                    ) {
                        Text("Open directory", fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = readFile,
                        enabled = enabled,
                        colors = ButtonDefaults.textButtonColors(contentColor = VeniceAmber)
                    ) {
                        Text("Read text", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
