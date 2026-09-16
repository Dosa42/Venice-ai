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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.VeniceBorder
import com.example.ui.theme.VeniceBorderHighlight
import com.example.ui.theme.VeniceCyan
import com.example.ui.theme.VeniceSurface
import com.example.ui.theme.VeniceSurfaceElevated
import com.example.ui.theme.VeniceTextMuted
import com.example.ui.theme.VeniceTextPrimary
import com.example.ui.theme.VeniceTextSecondary

@Composable
fun ModelSelectorDialog(
    onDismiss: () -> Unit,
    chatState: VeniceUiState,
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
                            text = "Select the model for Chat and Intelligence",
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
                    val selected = model.id == chatState.selectedChatGptModelId
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
            }
        }
    }
}
