package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VeniceAmber
import com.example.ui.theme.VeniceBackground
import com.example.ui.theme.VeniceBorder
import com.example.ui.theme.VeniceBorderHighlight
import com.example.ui.theme.VeniceCyan
import com.example.ui.theme.VeniceSurface
import com.example.ui.theme.VeniceSurfaceElevated
import com.example.ui.theme.VeniceSurfaceHighlight
import com.example.ui.theme.VeniceTextMuted
import com.example.ui.theme.VeniceTextPrimary
import com.example.ui.theme.VeniceTextSecondary
import com.example.ui.theme.VeniceThinkingPurple
import com.example.ui.viewmodel.VeniceUiState
import com.example.ui.viewmodel.VeniceViewModel

data class IntelToolConfig(
    val id: String,
    val title: String,
    val subtitle: String,
    val modelBadge: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun IntelligenceScreen(
    viewModel: VeniceViewModel,
    uiState: VeniceUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val tools = listOf(
        IntelToolConfig(
            id = "SELF_CODEBASE_INSPECTION",
            title = "Self-Codebase & Patch Generator",
            subtitle = "Inspects internal source files, generates diffs & GitHub rebuild commands",
            modelBadge = "gemini-3.1-pro-preview",
            icon = Icons.Default.Code,
            accentColor = VeniceThinkingPurple
        ),
        IntelToolConfig(
            id = "DYNAMIC_RUNTIME_ADAPT",
            title = "Dynamic Adaptive Extractor",
            subtitle = "Extracts newly discovered interfaces, drivers & tools into runtime memory",
            modelBadge = "gemini-3.1-pro-preview",
            icon = Icons.Default.Psychology,
            accentColor = VeniceCyan
        ),
        IntelToolConfig(
            id = "TERMINAL_LOG_DIAGNOSTIC",
            title = "Terminal & Kernel Diagnostic",
            subtitle = "Diagnoses root causes in dmesg, error codes, and tool logs",
            modelBadge = "gemini-3.1-pro-preview",
            icon = Icons.Default.Terminal,
            accentColor = VeniceCyan
        ),
        IntelToolConfig(
            id = "SHELL_SCRIPT_AUDIT",
            title = "Shell & Automation Auditor",
            subtitle = "Hardens bash, zsh, and python automation scripts",
            modelBadge = "gemini-3.1-pro-preview",
            icon = Icons.Default.Code,
            accentColor = VeniceAmber
        ),
        IntelToolConfig(
            id = "NETWORK_CONFIG_ANALYZER",
            title = "Network & Routing Analyzer",
            subtitle = "Decodes ifconfig, routes, iptables, and nmap outputs",
            modelBadge = "gemini-3.5-flash",
            icon = Icons.Default.Hub,
            accentColor = VeniceThinkingPurple
        ),
        IntelToolConfig(
            id = "ENHANCE_PROMPT",
            title = "Venice Magic Prompt",
            subtitle = "Rewrites raw ideas into high-fidelity artistic prompts",
            modelBadge = "gemini-3.1-flash-lite",
            icon = Icons.Default.AutoAwesome,
            accentColor = VeniceAmber
        ),
        IntelToolConfig(
            id = "SUMMARIZE",
            title = "Executive Summarizer",
            subtitle = "Extracts key insights and bullet points from long text",
            modelBadge = "gemini-3.5-flash",
            icon = Icons.Default.ShortText,
            accentColor = VeniceCyan
        ),
        IntelToolConfig(
            id = "PRIVACY_AUDIT",
            title = "Privacy & Security Audit",
            subtitle = "Analyzes zero-knowledge threat models & tracking vectors",
            modelBadge = "gemini-3.1-pro-preview",
            icon = Icons.Default.Security,
            accentColor = VeniceThinkingPurple
        ),
        IntelToolConfig(
            id = "CODE_REFACTOR",
            title = "Code Security & Refactor",
            subtitle = "Hardens code, fixes bottlenecks, and optimizes architectures",
            modelBadge = "gemini-3.1-pro-preview",
            icon = Icons.Default.Code,
            accentColor = VeniceCyan
        )
    )

    val currentToolConfig = tools.find { it.id == uiState.intelligenceTool } ?: tools.first()

    // SAF Document Loader for text logs, configs, and scripts
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName = "imported_file.txt"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val textContent = if (bytes.size > 120_000) {
                        String(bytes, 0, 120_000, Charsets.UTF_8) + "\n... [Truncated: File exceeded 120KB]"
                    } else {
                        String(bytes, Charsets.UTF_8)
                    }
                    viewModel.setIntelligenceInput(textContent)
                    Toast.makeText(context, "Loaded $fileName into buffer", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VeniceBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Intelligence Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Venice Intelligence",
                    color = VeniceTextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Specialized AI tools powered by Gemini models",
                    color = VeniceTextMuted,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(VeniceSurfaceElevated)
                    .border(1.dp, VeniceBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = currentToolConfig.modelBadge,
                    color = currentToolConfig.accentColor,
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tool Selector Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tools.forEach { tool ->
                val isSelected = uiState.intelligenceTool == tool.id
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) VeniceSurfaceHighlight else VeniceSurfaceElevated)
                        .border(
                            1.dp,
                            if (isSelected) tool.accentColor else VeniceBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.setIntelligenceTool(tool.id) }
                        .padding(12.dp)
                        .testTag("tool_${tool.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(tool.accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.title,
                                tint = tool.accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tool.title,
                                color = if (isSelected) tool.accentColor else VeniceTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = tool.subtitle,
                                color = VeniceTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Input & Execution Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VeniceSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, VeniceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Input for ${currentToolConfig.title}",
                        color = VeniceTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    // Import file via SAF button
                    Button(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Import File",
                            tint = VeniceCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import File", color = VeniceCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Presets row for rapid testing
                Text(
                    text = "Quick Presets & NetHunter Logs:",
                    color = VeniceTextMuted,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "Self-Patch & Rebuild" to ("Inspect your own DynamicAdaptiveEngine.kt and build-apk.yml. How do we add Bluetooth LE extraction into runtime memory, and what are the exact commands to build the new APK via GitHub Actions?"),
                        "Nmap Scan" to ("PORT STATE SERVICE\n22/tcp open ssh\n80/tcp open http\n443/tcp open https\n8080/tcp filtered http-proxy\nOS details: Linux 5.4.0 (Ubuntu)\nAggressive OS guesses: Linux 4.15 - 5.8"),
                        "dmesg OOM Log" to ("[10423.412091] Out of memory: Killed process 8492 (chroot_worker) total-vm:2451928kB, anon-rss:189404kB\n[10423.412105] oom_reaper: reaped process 8492\n[10423.412120] kernel: [Hardware Error]: CPU 0: Machine Check: 0 Bank 4"),
                        "Bash Root Script" to ("#!/usr/bin/env bash\nset -e\nif [ \"\$(id -u)\" -ne 0 ]; then\n  echo \"Error: Must run as root.\"\n  exit 1\nfi\nTARGET_DIR=\"/opt/nethunter/scripts\"\nmkdir -p \"\$TARGET_DIR\"\nchmod 750 \"\$TARGET_DIR\""),
                        "iptables NAT" to ("Chain PREROUTING (policy ACCEPT)\ntarget prot opt in out source destination\nDNAT tcp -- wlan0 any 0.0.0.0/0 0.0.0.0/0 tcp dpt:80 to:192.168.1.50:8080\nChain POSTROUTING (policy ACCEPT)\nMASQUERADE all -- any rmnet_data0 0.0.0.0/0 0.0.0.0/0")
                    )

                    presets.forEach { (label, content) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(VeniceSurfaceElevated)
                                .border(1.dp, VeniceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setIntelligenceInput(content)
                                    when (label) {
                                        "Self-Patch & Rebuild" -> viewModel.setIntelligenceTool("SELF_CODEBASE_INSPECTION")
                                        "Nmap Scan", "iptables NAT" -> viewModel.setIntelligenceTool("NETWORK_CONFIG_ANALYZER")
                                        "dmesg OOM Log" -> viewModel.setIntelligenceTool("TERMINAL_LOG_DIAGNOSTIC")
                                        "Bash Root Script" -> viewModel.setIntelligenceTool("SHELL_SCRIPT_AUDIT")
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = label, color = VeniceTextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = uiState.intelligenceInput,
                    onValueChange = { viewModel.setIntelligenceInput(it) },
                    placeholder = {
                        Text(
                            text = when (uiState.intelligenceTool) {
                                "SELF_CODEBASE_INSPECTION" -> "Ask how internal features work, request a code patch diff, or generate rebuild commands..."
                                "TERMINAL_LOG_DIAGNOSTIC" -> "Paste terminal error trace, dmesg log, or command output..."
                                "SHELL_SCRIPT_AUDIT" -> "Paste bash, zsh, or python script to audit and harden..."
                                "NETWORK_CONFIG_ANALYZER" -> "Paste ifconfig, iptables, ip route, or nmap scan output..."
                                "ENHANCE_PROMPT" -> "Enter draft prompt (e.g. 'Venice cyberpunk night')..."
                                "SUMMARIZE" -> "Paste raw text, articles, or notes here..."
                                "PRIVACY_AUDIT" -> "Paste system architecture, query, or app behavior to audit..."
                                "CODE_REFACTOR" -> "Paste code snippet to inspect and refactor..."
                                else -> "Enter input or import file above..."
                            },
                            color = VeniceTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = currentToolConfig.accentColor,
                        unfocusedBorderColor = VeniceBorder,
                        focusedTextColor = VeniceTextPrimary,
                        unfocusedTextColor = VeniceTextPrimary,
                        focusedContainerColor = VeniceSurfaceElevated,
                        unfocusedContainerColor = VeniceSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("intelligence_input_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.runIntelligenceTask() },
                    enabled = uiState.intelligenceInput.isNotBlank() && !uiState.isRunningIntelligence,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentToolConfig.accentColor,
                        disabledContainerColor = VeniceSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("run_intelligence_button")
                ) {
                    if (uiState.isRunningIntelligence) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Analyzing with ${currentToolConfig.modelBadge}...",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = currentToolConfig.icon,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Execute ${currentToolConfig.title}",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Output Result Card
        if (uiState.intelligenceOutput.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VeniceSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, currentToolConfig.accentColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Intelligence Result",
                            color = currentToolConfig.accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Row {
                            // Copy button
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Venice Intelligence", uiState.intelligenceOutput)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy output",
                                    tint = VeniceTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = uiState.intelligenceOutput,
                        color = VeniceTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Actions based on tool
                    if (uiState.intelligenceTool == "ENHANCE_PROMPT") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.applyEnhancedPromptToChat() },
                                colors = ButtonDefaults.buttonColors(containerColor = VeniceAmber),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Send to Chat", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.applyEnhancedPromptToStudio() },
                                colors = ButtonDefaults.buttonColors(containerColor = VeniceCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Use in Studio", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // For Terminal, Script, Network, and Audit tools: Send to Venice Chat or commit to DAF memory
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.sendAnalysisToChat(currentToolConfig.title, uiState.intelligenceOutput)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = currentToolConfig.accentColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Discuss in Chat", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.learnFromText(
                                        context,
                                        uiState.intelligenceOutput + "\n" + uiState.intelligenceInput,
                                        "Intelligence: ${currentToolConfig.title}"
                                    )
                                    Toast.makeText(context, "Runtime memory adapted from diagnostic trace", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = VeniceCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Commit to DAF", color = VeniceCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
