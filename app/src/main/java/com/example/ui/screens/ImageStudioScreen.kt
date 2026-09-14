package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.api.GeminiApi
import com.example.data.model.GeneratedArt
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
import com.example.ui.viewmodel.VeniceUiState
import com.example.ui.viewmodel.VeniceViewModel

@Composable
fun ImageStudioScreen(
    viewModel: VeniceViewModel,
    uiState: VeniceUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Photo picker to select an image from device for editing
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        val base64 = GeminiApi.bitmapToBase64(bitmap)
                        val uploadArt = GeneratedArt(
                            prompt = "User uploaded photo",
                            imageBase64 = base64,
                            style = "Custom Photo"
                        )
                        viewModel.selectArtForEditing(uploadArt)
                    }
                }
            } catch (e: Exception) {
                // handle gracefully
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
        // Venice Studio Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, VeniceBorderHighlight, RoundedCornerShape(16.dp))
                .background(VeniceSurfaceElevated)
        ) {
            Image(
                painter = painterResource(id = R.drawable.venice_hero_banner),
                contentDescription = "Venice Studio Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(14.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "VENICE IMAGE STUDIO",
                            color = VeniceAmber,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(VeniceAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "gemini-3.1-flash-image-preview",
                                color = VeniceAmber,
                                fontSize = 9.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                    Text(
                        text = "Create and edit high-resolution visuals with text prompts.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image Editing Mode Banner (if an image is selected to be edited)
        if (uiState.imageToEditBase64 != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(VeniceCyan.copy(alpha = 0.15f))
                    .border(1.dp, VeniceCyan, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val editBitmap = GeminiApi.base64ToBitmap(uiState.imageToEditBase64)
                        if (editBitmap != null) {
                            Image(
                                bitmap = editBitmap.asImageBitmap(),
                                contentDescription = "Image to edit",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = "Image Editing Mode Active",
                                color = VeniceCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Enter edits (e.g. change colors, add cyber lighting, alter background)",
                                color = VeniceTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.clearImageToEdit() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel edit mode",
                            tint = VeniceCyan
                        )
                    }
                }
            }
        }

        // Text Prompt Input Card
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
                        text = if (uiState.imageToEditBase64 != null) "Edit Instructions" else "Visual Prompt",
                        color = VeniceTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    // Upload from device button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VeniceSurfaceElevated)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            tint = VeniceCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Upload to Edit",
                            color = VeniceCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.imagePrompt,
                    onValueChange = { viewModel.setImagePrompt(it) },
                    placeholder = {
                        Text(
                            text = if (uiState.imageToEditBase64 != null)
                                "Describe edits: 'Add neon rain reflections, turn atmosphere to midnight cyber Venice'..."
                            else
                                "Describe the artwork: 'Cinematic Venetian gondola flying across neon futuristic skyscrapers'...",
                            color = VeniceTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VeniceAmber,
                        unfocusedBorderColor = VeniceBorder,
                        focusedTextColor = VeniceTextPrimary,
                        unfocusedTextColor = VeniceTextPrimary,
                        focusedContainerColor = VeniceSurfaceElevated,
                        unfocusedContainerColor = VeniceSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("image_prompt_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Aspect Ratio Selector
                Text(
                    text = "Aspect Ratio",
                    color = VeniceTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                val aspectRatios = listOf("1:1", "16:9", "9:16", "4:3", "3:4")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    aspectRatios.forEach { ratio ->
                        val isSelected = uiState.selectedAspectRatio == ratio
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) VeniceAmber.copy(alpha = 0.2f) else VeniceSurfaceElevated)
                                .border(1.dp, if (isSelected) VeniceAmber else VeniceBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.setAspectRatio(ratio) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("ratio_$ratio")
                        ) {
                            Text(
                                text = ratio,
                                color = if (isSelected) VeniceAmber else VeniceTextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Style Preset Chips (for new creations)
                if (uiState.imageToEditBase64 == null) {
                    Text(
                        text = "Aesthetic Style Preset",
                        color = VeniceTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val styles = listOf("Cinematic", "Cyberpunk", "Photorealistic", "Dark Baroque", "Anime", "3D Surreal")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        styles.forEach { style ->
                            val isSelected = uiState.selectedStylePreset == style
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) VeniceCyan.copy(alpha = 0.2f) else VeniceSurfaceElevated)
                                    .border(1.dp, if (isSelected) VeniceCyan else VeniceBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setStylePreset(style) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("style_$style")
                            ) {
                                Text(
                                    text = style,
                                    color = if (isSelected) VeniceCyan else VeniceTextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Generate / Edit Button
                Button(
                    onClick = { viewModel.generateOrEditImage() },
                    enabled = uiState.imagePrompt.isNotBlank() && !uiState.isGeneratingImage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.imageToEditBase64 != null) VeniceCyan else VeniceAmber,
                        disabledContainerColor = VeniceSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_image_button")
                ) {
                    if (uiState.isGeneratingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (uiState.imageToEditBase64 != null) "Editing with Gemini..." else "Generating Masterpiece...",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = if (uiState.imageToEditBase64 != null) Icons.Default.Edit else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.imageToEditBase64 != null) "Apply AI Edits" else "Generate Artwork",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Error notice if any
                if (!uiState.imageErrorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.imageErrorMessage,
                        color = VeniceRose,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Generated Art Gallery Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Venice Gallery",
                    color = VeniceTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${uiState.galleryArt.size} visuals created or edited in this session",
                    color = VeniceTextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.galleryArt.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(VeniceSurfaceElevated)
                    .border(1.dp, VeniceBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = VeniceTextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No images created yet",
                        color = VeniceTextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Enter a prompt above to generate with gemini-3.1-flash-image-preview",
                        color = VeniceTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            // Gallery Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(((uiState.galleryArt.size + 1) / 2 * 190).dp.coerceAtMost(580.dp)),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.galleryArt, key = { it.id }) { art ->
                    val bitmap = GeminiApi.base64ToBitmap(art.imageBase64)
                    if (bitmap != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = VeniceSurfaceElevated),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VeniceBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSelectedArtDetail(art) }
                                .testTag("gallery_item_${art.id}")
                        ) {
                            Column {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = art.prompt,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                )

                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = art.prompt,
                                        color = VeniceTextPrimary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = art.aspectRatio,
                                            color = VeniceTextMuted,
                                            fontSize = 10.sp
                                        )
                                        if (art.isEdit) {
                                            Text(
                                                text = "AI Edit",
                                                color = VeniceCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Art Preview & Edit Action Dialog
    uiState.selectedArtDetail?.let { art ->
        Dialog(onDismissRequest = { viewModel.setSelectedArtDetail(null) }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = VeniceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, VeniceBorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val detailBitmap = GeminiApi.base64ToBitmap(art.imageBase64)
                    if (detailBitmap != null) {
                        Image(
                            bitmap = detailBitmap.asImageBitmap(),
                            contentDescription = art.prompt,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = art.prompt,
                        color = VeniceTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Ratio: ${art.aspectRatio}",
                            color = VeniceTextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Style: ${art.style}",
                            color = VeniceAmber,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Copy Prompt Button
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Venice Prompt", art.prompt)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Prompt copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = VeniceTextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Prompt", color = VeniceTextPrimary, fontSize = 12.sp)
                        }

                        // Edit with AI Button
                        Button(
                            onClick = {
                                viewModel.selectArtForEditing(art)
                                viewModel.setSelectedArtDetail(null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VeniceCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Image", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
