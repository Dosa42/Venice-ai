package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiApi
import com.example.data.model.ChatMessage
import com.example.ui.theme.VeniceAmber
import com.example.ui.theme.VeniceBorder
import com.example.ui.theme.VeniceCyan
import com.example.ui.theme.VeniceSurfaceElevated
import com.example.ui.theme.VeniceSurfaceHighlight
import com.example.ui.theme.VeniceTextMuted
import com.example.ui.theme.VeniceTextPrimary
import com.example.ui.theme.VeniceTextSecondary
import com.example.ui.theme.VeniceThinkingPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val context = LocalContext.current

    val timeString = rememberFormattedTime(message.timestamp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag(if (isUser) "user_message_bubble" else "model_message_bubble"),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Message Header: Role / Persona / Model badge & timestamp
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(VeniceAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("V", color = VeniceAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = message.modelUsed ?: "Venice AI",
                    color = if (message.thinkingEnabled) VeniceThinkingPurple else VeniceAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "You",
                    color = VeniceCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(VeniceCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        tint = VeniceCyan,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = timeString,
                color = VeniceTextMuted,
                fontSize = 10.sp
            )
        }

        message.localContext?.let { attachment ->
            LocalContextCard(
                attachment = attachment,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // Attached image (Multimodal Vision / Studio generation)
        if (message.imageBase64 != null) {
            val bitmap = GeminiApi.base64ToBitmap(message.imageBase64)
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, VeniceBorder, RoundedCornerShape(14.dp))
                        .background(VeniceSurfaceElevated)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Multimodal Image",
                        modifier = Modifier
                            .fillMaxWidth(if (isUser) 0.75f else 0.95f)
                            .heightIn(max = 280.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Thinking Mode Collapsible Card (if assistant reasoned)
        if (!isUser && !message.thoughtProcess.isNullOrBlank()) {
            ThinkingProcessCard(
                thoughtProcess = message.thoughtProcess,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(bottom = 8.dp)
            )
        }

        // Message text bubble
        if (message.text.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isUser) 0.85f else 0.95f)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) VeniceSurfaceHighlight else VeniceSurfaceElevated)
                    .border(
                        1.dp,
                        if (isUser) VeniceCyan.copy(alpha = 0.3f) else VeniceBorder,
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        color = VeniceTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )

                    // Quick Action: Copy message
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Venice AI", message.text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = VeniceTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberFormattedTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
