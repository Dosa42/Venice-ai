package com.example.ui.components

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VeniceAmber
import com.example.ui.theme.VeniceBorder
import com.example.ui.theme.VeniceBorderHighlight
import com.example.ui.theme.VeniceCyan
import com.example.ui.theme.VenicePrivacyGreen
import com.example.ui.theme.VeniceSurface
import com.example.ui.theme.VeniceSurfaceElevated
import com.example.ui.theme.VeniceTextMuted
import com.example.ui.theme.VeniceTextPrimary
import com.example.ui.theme.VeniceThinkingPurple

@Composable
fun VeniceTopBar(
    isHighThinking: Boolean,
    isZeroRetention: Boolean,
    onOpenModelSelector: () -> Unit,
    onToggleHighThinking: () -> Unit,
    onOpenPersonaSelector: () -> Unit,
    onBurnSession: () -> Unit,
    modifier: Modifier = Modifier,
    modelLabel: String = "ChatGPT — select model",
    reasoningLabel: String? = null,
    compact: Boolean = false
) {
    if (compact) {
        Surface(color = VeniceSurface, modifier = modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("compact_chat_header"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("V", color = VeniceAmber, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp))
                Row(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenModelSelector).padding(horizontal = 8.dp, vertical = 12.dp)
                        .testTag("model_selector_chip"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(modelLabel, color = VeniceTextPrimary, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false))
                    Icon(Icons.Default.ExpandMore, "Switch Model", tint = VeniceTextMuted,
                        modifier = Modifier.size(16.dp))
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(VeniceThinkingPurple.copy(alpha = 0.15f))
                        .clickable(onClick = onToggleHighThinking)
                        .padding(horizontal = 10.dp, vertical = 12.dp).testTag("high_thinking_toggle")
                ) {
                    Text(reasoningLabel ?: "Reasoning", color = VeniceThinkingPurple,
                        fontSize = 11.sp, maxLines = 1)
                }
                IconButton(onClick = onOpenPersonaSelector,
                    modifier = Modifier.size(40.dp).testTag("persona_selector_chip")) {
                    Icon(Icons.Outlined.Person, "Select Persona", tint = VeniceCyan, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onBurnSession,
                    modifier = Modifier.size(40.dp).testTag("burn_session_button")) {
                    Icon(Icons.Default.DeleteSweep, "Burn Session and Clear Logs", tint = VeniceTextMuted,
                        modifier = Modifier.size(20.dp))
                }
            }
        }
        return
    }
    Surface(
        color = VeniceSurface,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Venice Brand Logo & Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenPersonaSelector() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(VeniceSurfaceElevated)
                            .border(1.dp, VeniceAmber.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "V",
                            color = VeniceAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VENICE",
                                color = VeniceTextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(VeniceAmber.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AI",
                                    color = VeniceAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Privacy Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isZeroRetention) VeniceCyan else VenicePrivacyGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isZeroRetention) "Zero Retention Active" else "Private & Encrypted",
                                color = VeniceTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Action Controls: High Thinking & Panic Wipe
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // High Thinking toggle button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isHighThinking) VeniceThinkingPurple.copy(alpha = 0.25f)
                                else VeniceSurfaceElevated
                            )
                            .border(
                                1.dp,
                                if (isHighThinking) VeniceThinkingPurple else VeniceBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onToggleHighThinking() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("high_thinking_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isHighThinking) Icons.Filled.Psychology else Icons.Outlined.Psychology,
                                contentDescription = "High Thinking Mode",
                                tint = if (isHighThinking) VeniceThinkingPurple else VeniceTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = reasoningLabel ?: if (isHighThinking) "Thinking" else "Think",
                                color = if (isHighThinking) VeniceThinkingPurple else VeniceTextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (isHighThinking) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Panic / Burn Session Button
                    IconButton(
                        onClick = onBurnSession,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(VeniceSurfaceElevated)
                            .testTag("burn_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Burn Session and Clear Logs",
                            tint = VeniceTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Model Selector Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(VeniceSurfaceElevated)
                        .border(1.dp, VeniceBorderHighlight, RoundedCornerShape(12.dp))
                        .clickable { onOpenModelSelector() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("model_selector_chip")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = modelLabel,
                            color = if (isHighThinking) VeniceThinkingPurple else VeniceAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Switch Model",
                            tint = VeniceTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Persona Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(VeniceSurfaceElevated)
                        .border(1.dp, VeniceBorder, RoundedCornerShape(12.dp))
                        .clickable { onOpenPersonaSelector() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("persona_selector_chip")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Persona",
                            color = VeniceCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Select Persona",
                            tint = VeniceTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
