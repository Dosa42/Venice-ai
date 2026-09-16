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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.OpenAIOAuthManager
import com.example.data.auth.OpenAIOAuthSession
import com.example.data.model.NetHunterHardwareProfile
import com.example.ui.components.AdaptiveGitHubRunnerCard
import com.example.ui.components.DynamicAdaptiveFrameworkCard
import com.example.ui.components.MetacognitiveCodebaseCard
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

    LaunchedEffect(Unit) {
        viewModel.loadOpenAiSession(context)
    }

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

        // OpenAI / ChatGPT OAuth PKCE Card
        OpenAIOAuthCard(
            session = uiState.openAiSession,
            isAuthenticating = uiState.isOpenAiAuthenticating,
            authStatus = uiState.openAiAuthStatus,
            onStartLogin = { viewModel.startOpenAiPkceLogin(context) },
            onRefreshToken = { viewModel.refreshOpenAiSession(context) },
            onDisconnect = { viewModel.disconnectOpenAi(context) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // NetHunter & Host Hardware Vault Card
        NetHunterHardwareCard(
            profile = uiState.hardwareProfile,
            injectHardwareProfile = uiState.injectHardwareProfile,
            onToggleInjection = { viewModel.toggleHardwareProfileInjection(it) },
            onUpdateProfile = { viewModel.updateHardwareProfile(it) },
            onResetDefaults = { viewModel.resetHardwareProfileToDefaults() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic Adaptive Framework (DAF) Card
        DynamicAdaptiveFrameworkCard(
            facts = uiState.adaptiveFacts,
            isAutoLearningEnabled = uiState.isAutoLearningEnabled,
            lastLearnedNotice = uiState.lastLearnedNotice,
            promptContext = uiState.dynamicAdaptivePromptContext,
            onToggleAutoLearning = { viewModel.toggleAutoLearning(context, it) },
            onLearnFromText = { text, src -> viewModel.learnFromText(context, text, src) },
            onAddCustomFact = { cat, key, valStr -> viewModel.addCustomAdaptiveFact(context, cat, key, valStr) },
            onRemoveFact = { viewModel.removeAdaptiveFact(context, it) },
            onToggleFact = { id, enabled -> viewModel.toggleAdaptiveFact(context, id, enabled) },
            onResetDefaults = { viewModel.resetAdaptiveFacts(context) },
            onClearAll = { viewModel.clearAdaptiveFacts(context) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic Adaptive GitHub Workflow Action Runner Card
        AdaptiveGitHubRunnerCard()

        Spacer(modifier = Modifier.height(14.dp))

        // Metacognitive Codebase Self-Introspection Card
        MetacognitiveCodebaseCard(
            isSelfAwarenessEnabled = uiState.isSelfAwarenessEnabled,
            selectedFilePath = uiState.selectedIntrospectionFile,
            onToggleSelfAwareness = { viewModel.toggleSelfAwareness(it) },
            onSelectFile = { viewModel.selectIntrospectionFile(it) }
        )

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

@Composable
fun NetHunterHardwareCard(
    profile: NetHunterHardwareProfile,
    injectHardwareProfile: Boolean,
    onToggleInjection: (Boolean) -> Unit,
    onUpdateProfile: (NetHunterHardwareProfile) -> Unit,
    onResetDefaults: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editNotes by remember(profile.customNotes) { mutableStateOf(profile.customNotes) }
    var editChrootPath by remember(profile.chrootPath) { mutableStateOf(profile.chrootPath) }
    var editInterfaces by remember(profile.networkInterfaces) { mutableStateOf(profile.networkInterfaces) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VeniceSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (injectHardwareProfile) VeniceCyan.copy(alpha = 0.5f) else VeniceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
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
                            .background(VeniceCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = VeniceCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Host Hardware Blueprint",
                                color = VeniceTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (injectHardwareProfile) VenicePrivacyGreen.copy(alpha = 0.2f) else VeniceSurfaceElevated)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (injectHardwareProfile) "ACTIVE IN AI" else "OFF",
                                    color = if (injectHardwareProfile) VenicePrivacyGreen else VeniceTextMuted,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "SM-A326B • Kali 4.14.186 • MagiskSU",
                            color = VeniceTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = injectHardwareProfile,
                    onCheckedChange = onToggleInjection,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VeniceCyan,
                        checkedTrackColor = VeniceCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = VeniceTextMuted,
                        uncheckedTrackColor = VeniceSurfaceHighlight
                    ),
                    modifier = Modifier.testTag("hardware_profile_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Automatically injects your exact device model, kernel release, root chroot path, and memory constraints into Venice AI queries so all terminal and system commands are pinpoint accurate.",
                color = VeniceTextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Specs Terminal Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090D12))
                    .border(1.dp, if (injectHardwareProfile) VeniceCyan.copy(alpha = 0.25f) else VeniceBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val specs = listOf(
                        "Target Device" to profile.deviceModel,
                        "Architecture" to profile.cpuArch,
                        "Kernel Version" to profile.kernelVersion,
                        "Root Privileges" to profile.rootStatus,
                        "Kali Chroot" to profile.chrootPath,
                        "Chroot Mounts" to "proc, sys, dev, dev/pts, system, sdcard",
                        "Active Daemons" to profile.activeServices,
                        "Local Interfaces" to profile.networkInterfaces,
                        "Memory Limits" to profile.ramSummary
                    )

                    specs.forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = VeniceTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = value,
                                color = if (label == "Root Privileges" || label == "Kali Chroot") VeniceCyan else VeniceTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (profile.customNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(VeniceSurfaceElevated)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Notes: ${profile.customNotes}",
                                color = VeniceAmber,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customization / Edit controls
            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editChrootPath,
                        onValueChange = { editChrootPath = it },
                        label = { Text("Kali Chroot Path", color = VeniceTextMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = VeniceSurfaceElevated,
                            unfocusedContainerColor = VeniceSurfaceElevated,
                            focusedTextColor = VeniceTextPrimary,
                            unfocusedTextColor = VeniceTextPrimary,
                            focusedIndicatorColor = VeniceCyan,
                            unfocusedIndicatorColor = VeniceBorder
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editInterfaces,
                        onValueChange = { editInterfaces = it },
                        label = { Text("Network Interfaces (e.g. wlan0, wlan1mon)", color = VeniceTextMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = VeniceSurfaceElevated,
                            unfocusedContainerColor = VeniceSurfaceElevated,
                            focusedTextColor = VeniceTextPrimary,
                            unfocusedTextColor = VeniceTextPrimary,
                            focusedIndicatorColor = VeniceCyan,
                            unfocusedIndicatorColor = VeniceBorder
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Custom Notes & Peripherals (e.g. AR9271 USB-OTG)", color = VeniceTextMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = VeniceSurfaceElevated,
                            unfocusedContainerColor = VeniceSurfaceElevated,
                            focusedTextColor = VeniceTextPrimary,
                            unfocusedTextColor = VeniceTextPrimary,
                            focusedIndicatorColor = VeniceCyan,
                            unfocusedIndicatorColor = VeniceBorder
                        ),
                        maxLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onUpdateProfile(
                                    profile.copy(
                                        chrootPath = editChrootPath,
                                        networkInterfaces = editInterfaces,
                                        customNotes = editNotes
                                    )
                                )
                                isEditing = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VeniceCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Blueprint", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { isEditing = false },
                            colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = VeniceTextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isEditing = true },
                        colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = VeniceCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customize Blueprint", color = VeniceCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onResetDefaults,
                        colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = VeniceAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset SM-A326B", color = VeniceAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun OpenAIOAuthCard(
    session: OpenAIOAuthSession?,
    isAuthenticating: Boolean,
    authStatus: String?,
    onStartLogin: () -> Unit,
    onRefreshToken: () -> Unit,
    onDisconnect: () -> Unit
) {
    var showConfig by remember { mutableStateOf(false) }

    val isConnected = session != null
    val isExpired = session?.isExpired == true

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VeniceSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isConnected && !isExpired) VenicePrivacyGreen.copy(alpha = 0.5f)
            else if (isConnected && isExpired) VeniceAmber.copy(alpha = 0.5f)
            else VeniceBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
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
                            .background(
                                if (isConnected && !isExpired) VenicePrivacyGreen.copy(alpha = 0.2f)
                                else VeniceCyan.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = if (isConnected && !isExpired) VenicePrivacyGreen else VeniceCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ChatGPT / OpenAI OAuth",
                                color = VeniceTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when {
                                            isConnected && !isExpired -> VenicePrivacyGreen.copy(alpha = 0.2f)
                                            isConnected && isExpired -> VeniceAmber.copy(alpha = 0.2f)
                                            isAuthenticating -> VeniceCyan.copy(alpha = 0.2f)
                                            else -> VeniceSurfaceElevated
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when {
                                        isConnected && !isExpired -> "CONNECTED"
                                        isConnected && isExpired -> "TOKEN EXPIRED"
                                        isAuthenticating -> "CONNECTING..."
                                        else -> "NOT CONNECTED"
                                    },
                                    color = when {
                                        isConnected && !isExpired -> VenicePrivacyGreen
                                        isConnected && isExpired -> VeniceAmber
                                        isAuthenticating -> VeniceCyan
                                        else -> VeniceTextMuted
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "PKCE Authorization • Loopback 127.0.0.1:1455",
                            color = VeniceTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Authenticate directly with OpenAI via browser-based OAuth 2.0 PKCE (SHA-256 verifier). The authorization code is received via local loopback listener on port 1455 with automatic token refresh.",
                color = VeniceTextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            // Auth Status notice (if in progress or completed)
            if (!authStatus.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF090D14))
                        .border(1.dp, VeniceCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = VeniceCyan,
                                strokeWidth = 1.5.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = authStatus,
                            color = if (authStatus.startsWith("Auth") || authStatus.startsWith("Refresh")) VeniceAmber else VeniceCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Connected Session Details
            if (session != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF090E16))
                        .border(1.dp, VeniceBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Account ID", color = VeniceTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = session.accountId,
                                color = VeniceCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Access Token", color = VeniceTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "${session.accessToken.take(14)}••••••••",
                                color = VeniceTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Session Expiry", color = VeniceTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = if (session.isExpired) "Expired (refresh needed)" else "Valid for ~${session.expiresInSeconds / 60} min",
                                color = if (session.isExpired) VeniceAmber else VenicePrivacyGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRefreshToken,
                        colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = VeniceCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh Token", color = VeniceCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = VeniceRose,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Disconnect", color = VeniceRose, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                // Login action button
                Button(
                    onClick = onStartLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = VeniceCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("openai_pkce_login_button")
                ) {
                    if (isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Waiting for Browser...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connect with ChatGPT (OAuth PKCE)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable OAuth Configuration Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showConfig) "Hide OAuth Parameters" else "View OAuth Parameters",
                    color = VeniceTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { showConfig = !showConfig }
                )
            }

            if (showConfig) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "OpenAI OAuth Client ID: ${OpenAIOAuthManager.DEFAULT_CLIENT_ID}",
                        color = VeniceTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(VeniceSurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Redirect URI: ${OpenAIOAuthManager.REDIRECT_URI}",
                                color = VeniceTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Loopback Listener: 127.0.0.1:1455",
                                color = VeniceTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Scopes: openid, profile, email, offline_access, api.connectors",
                                color = VeniceTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

