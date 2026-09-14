package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.VenicePrivacyGreen
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
fun PrivacyAccountScreen(
    viewModel: VeniceViewModel,
    uiState: VeniceUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VeniceBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Privacy Vault Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Venice Privacy Vault",
                    color = VeniceTextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Zero-knowledge architecture & authentication controls",
                    color = VeniceTextMuted,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VenicePrivacyGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = VenicePrivacyGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Notice Banner (if any)
        if (!uiState.statusNotice.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VeniceAmber.copy(alpha = 0.15f))
                    .border(1.dp, VeniceAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = uiState.statusNotice,
                    color = VeniceAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Firebase Auth Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(VeniceSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = VeniceCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Authentication & Identity",
                                color = VeniceTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (uiState.currentUser != null) {
                                    if (uiState.currentUser.isAnonymous) "Venice Anonymous Guest Session"
                                    else uiState.currentUser.email ?: "Signed in with Google"
                                } else "Not Authenticated (Local Session)",
                                color = if (uiState.currentUser != null) VenicePrivacyGreen else VeniceTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (uiState.currentUser != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(VenicePrivacyGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (uiState.currentUser.isAnonymous) "ANONYMOUS" else "VERIFIED",
                                color = VenicePrivacyGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.currentUser == null) {
                    Text(
                        text = "Sign in to persist your sessions to encrypted Firestore cloud storage, or continue with anonymous permissionless guest mode.",
                        color = VeniceTextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Google Sign In button
                        Button(
                            onClick = { viewModel.signInWithGoogle(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = VeniceAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("google_sign_in_button")
                        ) {
                            Text("Google Sign-In", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Anonymous Guest Session button
                        Button(
                            onClick = { viewModel.signInAnonymously() },
                            colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VeniceBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("anonymous_sign_in_button")
                        ) {
                            Text("Guest Mode", color = VeniceTextPrimary, fontSize = 12.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UID: ${uiState.currentUser.uid.take(12)}...",
                            color = VeniceTextMuted,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )

                        Button(
                            onClick = { viewModel.signOut() },
                            colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sign Out", color = VeniceRose, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy Architecture Toggles Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VeniceSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, VeniceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Venice Architecture Policies",
                    color = VeniceTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Zero Retention Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = VeniceCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zero Data Retention Mode",
                                color = VeniceTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "RAM-only execution. Prompts are never written to disk or Firestore database.",
                            color = VeniceTextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Switch(
                        checked = uiState.isZeroRetentionMode,
                        onCheckedChange = { viewModel.toggleZeroRetention(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VeniceCyan,
                            checkedTrackColor = VeniceCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = VeniceTextMuted,
                            uncheckedTrackColor = VeniceSurfaceHighlight
                        ),
                        modifier = Modifier.testTag("zero_retention_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Firestore Cloud Vault Sync Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.isCloudSyncEnabled) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = VeniceAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Firestore Cloud Vault Sync",
                                color = VeniceTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "Back up your sessions and studio gallery to Firebase Firestore.",
                            color = VeniceTextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Switch(
                        checked = uiState.isCloudSyncEnabled,
                        onCheckedChange = { viewModel.toggleCloudSync(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VeniceAmber,
                            checkedTrackColor = VeniceAmber.copy(alpha = 0.3f),
                            uncheckedThumbColor = VeniceTextMuted,
                            uncheckedTrackColor = VeniceSurfaceHighlight
                        ),
                        modifier = Modifier.testTag("cloud_sync_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy Telemetry Audit Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VeniceSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, VeniceBorderHighlight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = VenicePrivacyGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Venice Live Telemetry Audit",
                        color = VenicePrivacyGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val telemetryItems = listOf(
                    "Network Transport" to "TLS 1.3 End-to-End Encrypted",
                    "Prompt Retention" to if (uiState.isZeroRetentionMode) "0 bytes (Ephemeral RAM)" else "Local Secure Store",
                    "User IP Tracking" to "Anonymized via Private Gateway",
                    "Model Training" to "Disabled (Zero opt-in)",
                    "Cloud Sync Status" to if (uiState.currentUser != null && uiState.isCloudSyncEnabled) "Active (Firestore)" else "Disabled (Local only)"
                )

                telemetryItems.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = label, color = VeniceTextSecondary, fontSize = 12.sp)
                        Text(text = value, color = VeniceTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Venice Panic Button (Burn Session)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VeniceRose.copy(alpha = 0.1f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, VeniceRose.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = VeniceRose,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Panic Wipe: Burn All Data",
                        color = VeniceRose,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Instantly clears all active chat memory, generated artwork, and purges all documents from Firestore. This action cannot be reversed.",
                    color = VeniceTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.burnSessionAndData() },
                    colors = ButtonDefaults.buttonColors(containerColor = VeniceRose),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("panic_burn_button")
                ) {
                    Text("Purge & Burn All Records", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
