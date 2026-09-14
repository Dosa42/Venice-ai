package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShortText
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
                Text(
                    text = "Input for ${currentToolConfig.title}",
                    color = VeniceTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.intelligenceInput,
                    onValueChange = { viewModel.setIntelligenceInput(it) },
                    placeholder = {
                        Text(
                            text = when (uiState.intelligenceTool) {
                                "ENHANCE_PROMPT" -> "Enter draft prompt (e.g. 'Venice cyberpunk night')..."
                                "SUMMARIZE" -> "Paste raw text, articles, or notes here..."
                                "PRIVACY_AUDIT" -> "Paste system architecture, query, or app behavior to audit..."
                                "CODE_REFACTOR" -> "Paste code snippet to inspect and refactor..."
                                else -> "Enter input..."
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

                    // Quick Actions based on tool
                    if (uiState.intelligenceTool == "ENHANCE_PROMPT") {
                        Spacer(modifier = Modifier.height(12.dp))
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
                    }
                }
            }
        }
    }
}
