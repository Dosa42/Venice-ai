package com.example.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import com.example.ui.viewmodel.VeniceUiState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.VeniceModel
import com.example.ui.theme.VeniceAmber
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

@Composable
fun ModelSelectorDialog(
    selectedModel: VeniceModel,
    isHighThinking: Boolean,
    onSelectModel: (VeniceModel) -> Unit,
    onToggleHighThinking: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    chatState: VeniceUiState? = null,
    onSelectChatGptModel: (String) -> Unit = {},
    onSelectReasoning: (String) -> Unit = {},
    onRefreshChatGptModels: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = VeniceSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, VeniceBorderHighlight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("model_selector_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Model Architecture",
                            color = VeniceTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Select intelligence tier for your session",
                            color = VeniceTextMuted,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = VeniceTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (chatState != null) {
                    Text("ChatGPT", color = VeniceCyan, fontWeight = FontWeight.Bold)
                    when {
                        chatState.openAiSession == null -> Text(
                            "Connect ChatGPT in Privacy Vault to load your models.", color = VeniceTextSecondary)
                        chatState.isLoadingChatGptModels -> Text("Loading your models…", color = VeniceTextSecondary)
                    }
                    chatState.chatGptModelError?.let { Text(it, color = VeniceTextSecondary) }
                    if (chatState.openAiSession != null) {
                        TextButton(onClick = onRefreshChatGptModels,
                            enabled = !chatState.isLoadingChatGptModels && !chatState.isGeneratingChat) {
                            Text("Refresh models")
                        }
                    }
                    chatState.chatGptModels.forEach { model ->
                        val selected = chatState.useChatGpt && model.id == chatState.selectedChatGptModelId
                        Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(VeniceSurfaceElevated)
                            .border(1.dp, if (selected) VeniceCyan else VeniceBorder, RoundedCornerShape(14.dp))
                            .clickable(enabled = !chatState.isGeneratingChat && !chatState.isLoadingChatGptModels) {
                                onSelectChatGptModel(model.id)
                            }.padding(14.dp).testTag("chatgpt_model_${model.id}")) {
                            Text((if (selected) "✓ " else "") + model.displayName,
                                color = if (selected) VeniceCyan else VeniceTextPrimary,
                                fontWeight = FontWeight.SemiBold)
                            Text(model.id, color = VeniceTextMuted, fontSize = 11.sp)
                            if (model.description.isNotBlank()) Text(model.description,
                                color = VeniceTextSecondary, fontSize = 12.sp)
                            if (selected && model.reasoningLevels.isNotEmpty()) {
                                Text("Reasoning effort", color = VeniceTextPrimary, modifier = Modifier.padding(top = 8.dp))
                                model.reasoningLevels.forEach { effort ->
                                    TextButton(onClick = { onSelectReasoning(effort) }, enabled = !chatState.isGeneratingChat,
                                        modifier = Modifier.testTag("chatgpt_reasoning_$effort")) {
                                        Text((if (effort == chatState.chatGptReasoning) "✓ " else "") + effort)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Gemini", color = VeniceAmber, fontWeight = FontWeight.Bold)
                }

                // Model Options: Balanced, Pro, Fast
                val selectableModels = listOf(
                    VeniceModel.BALANCED,
                    VeniceModel.PRO,
                    VeniceModel.FAST
                )

                selectableModels.forEach { model ->
                    val isSelected = chatState?.useChatGpt != true && selectedModel == model
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) VeniceSurfaceHighlight else VeniceSurfaceElevated)
                            .border(
                                1.dp,
                                if (isSelected) VeniceAmber else VeniceBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = chatState?.isGeneratingChat != true) { onSelectModel(model) }
                            .padding(14.dp)
                            .testTag("model_option_${model.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = model.displayName,
                                        color = if (isSelected) VeniceAmber else VeniceTextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (model == VeniceModel.PRO) VeniceThinkingPurple.copy(alpha = 0.2f)
                                                else VeniceBorder
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = model.id,
                                            color = if (model == VeniceModel.PRO) VeniceThinkingPurple else VeniceTextSecondary,
                                            fontSize = 10.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }

                                Text(
                                    text = model.subtitle,
                                    color = VeniceTextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = VeniceAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Gemini-only reasoning control.
                if (chatState?.useChatGpt != true) Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(VeniceSurfaceElevated)
                        .border(
                            1.dp,
                            if (isHighThinking) VeniceThinkingPurple else VeniceBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                        .testTag("high_thinking_section")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "High Thinking Mode",
                                tint = VeniceThinkingPurple,
                                modifier = Modifier
                                    .size(26.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "High Thinking Mode",
                                    color = VeniceTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Forces gemini-3.1-pro-preview with thinkingLevel HIGH for maximum reasoning depth.",
                                    color = VeniceTextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Switch(
                            checked = isHighThinking,
                            enabled = chatState?.isGeneratingChat != true,
                            onCheckedChange = { enabled ->
                                onToggleHighThinking(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VeniceThinkingPurple,
                                checkedTrackColor = VeniceThinkingPurple.copy(alpha = 0.3f),
                                uncheckedThumbColor = VeniceTextMuted,
                                uncheckedTrackColor = VeniceSurfaceHighlight
                            ),
                            modifier = Modifier.testTag("high_thinking_switch")
                        )
                    }
                }
            }
        }
    }
}
