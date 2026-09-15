package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.skills.AdaptivePromptEngine
import com.example.data.skills.NativeSkill
import com.example.data.skills.NativeSkillsEngine
import com.example.data.skills.PromptHookResult
import com.example.ui.theme.VeniceAmber
import com.example.ui.theme.VeniceCyan
import com.example.ui.theme.VeniceEmerald
import com.example.ui.theme.VeniceThinkingPurple
import com.example.ui.theme.VeniceViolet

@Composable
fun NativeSkillsHookCard(
    currentMode: AdaptivePromptEngine.InjectionMode,
    enabledSkills: Set<String>,
    lastHookResult: PromptHookResult?,
    onModeChanged: (AdaptivePromptEngine.InjectionMode) -> Unit,
    onSkillToggled: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var testQueryInput by remember { mutableStateOf("") }
    var selectedSkillDetail by remember { mutableStateOf<NativeSkill?>(null) }
    val clipboardManager = LocalClipboardManager.current

    // Live preview hook for test input or default
    val livePreviewResult = remember(testQueryInput, currentMode, enabledSkills) {
        if (testQueryInput.isNotBlank()) {
            AdaptivePromptEngine.synthesizePromptHook(
                userQuery = testQueryInput,
                mode = currentMode,
                manualSelectedSkills = enabledSkills
            )
        } else {
            lastHookResult ?: AdaptivePromptEngine.synthesizePromptHook(
                userQuery = "android compose app icon secret font",
                mode = currentMode,
                manualSelectedSkills = enabledSkills
            )
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(VeniceCyan.copy(alpha = 0.4f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VeniceCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Native Skills Engine",
                            tint = VeniceCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Native Skills Prompt Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Adaptive Intent Matching & Directives",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Selector Segmented Chips
            Text(
                text = "Dynamic Injection Mode",
                style = MaterialTheme.typography.labelMedium,
                color = VeniceCyan,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AdaptivePromptEngine.InjectionMode.values().forEach { mode ->
                    val isSelected = mode == currentMode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onModeChanged(mode) },
                        label = {
                            Text(
                                text = mode.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VeniceCyan.copy(alpha = 0.25f),
                            selectedLabelColor = VeniceCyan,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Skills Chips
            Text(
                text = "Available Native Android Skills (${enabledSkills.size}/${NativeSkillsEngine.SKILLS.size} active)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(NativeSkillsEngine.SKILLS) { skill ->
                    val isMatchedInPreview = livePreviewResult.matchedSkillIds.contains(skill.id)
                    val tagColor = when (skill.id) {
                        "android-cli" -> VeniceEmerald
                        "android-secret-management" -> VeniceAmber
                        "android-typography" -> VeniceThinkingPurple
                        "app-icon-generation" -> VeniceCyan
                        "design-guidelines" -> VeniceViolet
                        else -> MaterialTheme.colorScheme.primary
                    }

                    AssistChip(
                        onClick = {
                            if (currentMode == AdaptivePromptEngine.InjectionMode.CUSTOM_ONLY) {
                                onSkillToggled(skill.id)
                            } else {
                                selectedSkillDetail = skill
                            }
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isMatchedInPreview) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(tagColor)
                                    )
                                }
                                Text(
                                    text = skill.name.replace("Android ", ""),
                                    fontSize = 12.sp,
                                    color = if (isMatchedInPreview) tagColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (skill.id) {
                                    "android-cli" -> Icons.Default.Terminal
                                    "android-secret-management" -> Icons.Default.Lock
                                    "android-typography" -> Icons.Default.TextFields
                                    "app-icon-generation" -> Icons.Default.Brush
                                    "design-guidelines" -> Icons.Default.Palette
                                    else -> Icons.Default.CenterFocusStrong
                                },
                                contentDescription = skill.name,
                                tint = if (isMatchedInPreview) tagColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isMatchedInPreview) tagColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isMatchedInPreview) BorderStroke(1.dp, tagColor.copy(alpha = 0.5f)) else null
                    )
                }
            }

            // Expanded Live Inspector and Intent Tester
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Real-Time Intent Tester & Prompt Inspector",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = testQueryInput,
                        onValueChange = { testQueryInput = it },
                        placeholder = { Text("Type prompt to simulate hook (e.g. 'build adaptive icon with secure api key')...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hook Result Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Matched Skills: ${livePreviewResult.injectedSkillCount} active",
                            style = MaterialTheme.typography.labelSmall,
                            color = VeniceEmerald,
                            fontWeight = FontWeight.Bold
                        )

                        if (livePreviewResult.promptContext.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(livePreviewResult.promptContext))
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Prompt Context", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Directives", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Raw Injected Prompt Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (livePreviewResult.promptContext.isNotBlank()) {
                                livePreviewResult.promptContext
                            } else {
                                "// No skills dynamically matched for this intent. Engine operates in low-latency default mode."
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog for individual skill inspection
    if (selectedSkillDetail != null) {
        val skill = selectedSkillDetail!!
        AlertDialog(
            onDismissRequest = { selectedSkillDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = VeniceCyan
                    )
                    Text(skill.name, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = skill.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Core Directives:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = VeniceCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    skill.coreDirectives.forEach { directive ->
                        Text(
                            text = "• $directive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Recipe Snippet:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = VeniceAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = skill.codeRecipe,
                            color = VeniceEmerald,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSkillDetail = null }) {
                    Text("Close")
                }
            }
        )
    }
}
