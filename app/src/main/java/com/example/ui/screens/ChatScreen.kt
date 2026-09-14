package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.components.MessageBubble
import com.example.ui.theme.VeniceAmber
import com.example.ui.theme.VeniceBackground
import com.example.ui.theme.VeniceBorder
import com.example.ui.theme.VeniceBorderHighlight
import com.example.ui.theme.VeniceCyan
import com.example.ui.theme.VeniceRose
import com.example.ui.theme.VeniceSurface
import com.example.ui.theme.VeniceSurfaceElevated
import com.example.ui.theme.VeniceSurfaceHighlight
import com.example.ui.theme.VeniceTextMuted
import com.example.ui.theme.VeniceTextPrimary
import com.example.ui.theme.VeniceTextSecondary
import com.example.ui.theme.VeniceThinkingPurple
import com.example.ui.viewmodel.VeniceUiState
import com.example.ui.viewmodel.VeniceViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: VeniceViewModel,
    uiState: VeniceUiState,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Photo Picker (Android Photo Picker - zero permission)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        viewModel.attachImage(bitmap)
                    }
                }
            } catch (e: Exception) {
                // handle gracefully
            }
        }
    }

    // Auto-scroll when new messages arrive
    LaunchedEffect(uiState.currentSession.messages.size, uiState.isGeneratingChat) {
        if (uiState.currentSession.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.currentSession.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VeniceBackground)
            .imePadding()
    ) {
        // Active Persona Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeniceSurfaceElevated)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Role: ",
                    color = VeniceTextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = uiState.selectedPersona.name,
                    color = VeniceCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " (${uiState.selectedPersona.title})",
                    color = VeniceTextMuted,
                    fontSize = 10.sp
                )
            }

            if (uiState.isHighThinkingEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = VeniceThinkingPurple,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "High Thinking Active",
                        color = VeniceThinkingPurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Scrollable Messages Thread
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.currentSession.messages.isEmpty()) {
                // Empty state suggestions
                EmptyChatSuggestions(
                    onSelectPrompt = { prompt ->
                        viewModel.setChatInput(prompt)
                        viewModel.sendChatMessage()
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(10.dp)) }

                    items(uiState.currentSession.messages, key = { it.id }) { message ->
                        MessageBubble(message = message)
                    }

                    // Generating Progress Indicator
                    if (uiState.isGeneratingChat) {
                        item {
                            ThinkingIndicator(
                                isHighThinking = uiState.isHighThinkingEnabled,
                                modelName = if (uiState.isHighThinkingEnabled) "Venice Pro" else uiState.selectedModel.displayName
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(14.dp)) }
                }
            }
        }

        // Error Banner if API error occurs
        if (!uiState.chatErrorMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VeniceRose.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = uiState.chatErrorMessage,
                    color = VeniceRose,
                    fontSize = 12.sp
                )
            }
        }

        // Quick Suggestions Horizontal Chips
        QuickSuggestionChips(
            onPromptSelected = { prompt ->
                viewModel.setChatInput(prompt)
            }
        )

        // Attached image preview before sending
        if (uiState.attachedImageBitmap != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VeniceSurface)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, VeniceCyan, RoundedCornerShape(8.dp))
                ) {
                    Image(
                        bitmap = uiState.attachedImageBitmap.asImageBitmap(),
                        contentDescription = "Attached Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Image attached for multimodal vision",
                        color = VeniceTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Gemini will analyze text and image together",
                        color = VeniceTextMuted,
                        fontSize = 10.sp
                    )
                }

                IconButton(onClick = { viewModel.removeAttachedImage() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Image",
                        tint = VeniceTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Input Field & Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeniceSurface)
                .border(width = 0.5.dp, color = VeniceBorder)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attach Photo Button
            IconButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(VeniceSurfaceElevated)
                    .testTag("attach_image_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Attach Photo for Multimodal Vision",
                    tint = if (uiState.attachedImageBitmap != null) VeniceCyan else VeniceTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Chat Input TextField
            OutlinedTextField(
                value = uiState.chatInputText,
                onValueChange = { viewModel.setChatInput(it) },
                placeholder = {
                    Text(
                        text = if (uiState.isHighThinkingEnabled) "Ask Venice anything (High Thinking active)..." else "Ask Venice without censorship...",
                        color = VeniceTextMuted,
                        fontSize = 13.sp
                    )
                },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (!uiState.isGeneratingChat) {
                            viewModel.sendChatMessage()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (uiState.isHighThinkingEnabled) VeniceThinkingPurple else VeniceAmber,
                    unfocusedBorderColor = VeniceBorder,
                    focusedTextColor = VeniceTextPrimary,
                    unfocusedTextColor = VeniceTextPrimary,
                    cursorColor = if (uiState.isHighThinkingEnabled) VeniceThinkingPurple else VeniceAmber,
                    focusedContainerColor = VeniceSurfaceElevated,
                    unfocusedContainerColor = VeniceSurfaceElevated
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (!uiState.isGeneratingChat) {
                        viewModel.sendChatMessage()
                    }
                },
                enabled = (uiState.chatInputText.isNotBlank() || uiState.attachedImageBase64 != null) && !uiState.isGeneratingChat,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if ((uiState.chatInputText.isNotBlank() || uiState.attachedImageBase64 != null) && !uiState.isGeneratingChat)
                            (if (uiState.isHighThinkingEnabled) VeniceThinkingPurple else VeniceAmber)
                        else VeniceSurfaceElevated
                    )
                    .testTag("send_message_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = if ((uiState.chatInputText.isNotBlank() || uiState.attachedImageBase64 != null) && !uiState.isGeneratingChat)
                        Color.Black else VeniceTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickSuggestionChips(
    onPromptSelected: (String) -> Unit
) {
    val suggestions = listOf(
        "Analyze zero-knowledge cryptography",
        "Explain quantum encryption protocols",
        "Audit my smart contract logic",
        "Compare decentralization trade-offs",
        "Write a Python neural network from scratch"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VeniceSurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        suggestions.forEach { suggestion ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(VeniceSurfaceElevated)
                    .border(1.dp, VeniceBorder, RoundedCornerShape(12.dp))
                    .clickable { onPromptSelected(suggestion) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = suggestion,
                    color = VeniceTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyChatSuggestions(
    onSelectPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(VeniceSurfaceElevated)
                .border(1.dp, VeniceAmber, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("V", color = VeniceAmber, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Venice Private AI",
            color = VeniceTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Uncensored intelligence with zero retention architecture. Ask complex STEM, coding, or privacy queries.",
            color = VeniceTextMuted,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )

        val samplePrompts = listOf(
            "How does zero-knowledge proof cryptography guarantee privacy?",
            "Solve this complex algorithm with high thinking step-by-step.",
            "Explain the architectural trade-offs between local and cloud AI models."
        )

        samplePrompts.forEach { prompt ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VeniceSurfaceElevated)
                    .border(1.dp, VeniceBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelectPrompt(prompt) }
                    .padding(14.dp)
            ) {
                Text(
                    text = prompt,
                    color = VeniceTextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ThinkingIndicator(
    isHighThinking: Boolean,
    modelName: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VeniceSurfaceElevated)
            .border(
                1.dp,
                if (isHighThinking) VeniceThinkingPurple.copy(alpha = 0.5f) else VeniceAmber.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = if (isHighThinking) VeniceThinkingPurple else VeniceAmber
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = if (isHighThinking) "$modelName is thinking deeply..." else "$modelName is processing...",
                    color = if (isHighThinking) VeniceThinkingPurple else VeniceAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isHighThinking) "Applying multi-step reasoning verification" else "Streaming private zero-retention response",
                    color = VeniceTextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}
