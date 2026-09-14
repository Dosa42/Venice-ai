package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import com.example.data.adaptive.AdaptiveCategory
import com.example.data.adaptive.AdaptiveFact
import com.example.ui.theme.VeniceAmber
import com.example.ui.theme.VeniceBorder
import com.example.ui.theme.VeniceCyan
import com.example.ui.theme.VenicePrivacyGreen
import com.example.ui.theme.VeniceRose
import com.example.ui.theme.VeniceSurface
import com.example.ui.theme.VeniceSurfaceElevated
import com.example.ui.theme.VeniceTextMuted
import com.example.ui.theme.VeniceTextPrimary
import com.example.ui.theme.VeniceTextSecondary
import com.example.ui.theme.VeniceThinkingPurple

/**
 * Dynamic Adaptive Framework Card.
 * Allows viewing, learning, and managing runtime environment facts and rules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicAdaptiveFrameworkCard(
    facts: List<AdaptiveFact>,
    isAutoLearningEnabled: Boolean,
    lastLearnedNotice: String?,
    promptContext: String,
    onToggleAutoLearning: (Boolean) -> Unit,
    onLearnFromText: (String, String) -> Unit,
    onAddCustomFact: (AdaptiveCategory, String, String) -> Unit,
    onRemoveFact: (String) -> Unit,
    onToggleFact: (String, Boolean) -> Unit,
    onResetDefaults: () -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showContextDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VeniceSurface),
        border = BorderStroke(1.dp, VeniceCyan.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(VeniceCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = VeniceCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Dynamic Adaptive Framework",
                                color = VeniceTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isAutoLearningEnabled) VenicePrivacyGreen.copy(alpha = 0.2f)
                                        else VeniceAmber.copy(alpha = 0.2f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isAutoLearningEnabled) "DAF: ACTIVE (${facts.size} FACTS)" else "DAF: PAUSED",
                                    color = if (isAutoLearningEnabled) VenicePrivacyGreen else VeniceAmber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Runtime Environment Learning & Telemetry Engine",
                            color = VeniceTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Learns and adapts continuously to NetHunter chroot environment variations, new network interfaces (e.g. wlan1mon), installed binaries, kernel modules, and memory limits in real time.",
                color = VeniceTextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            if (!lastLearnedNotice.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF090E16))
                        .border(1.dp, VeniceCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "⚡ $lastLearnedNotice",
                        color = VeniceCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-Learning Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(VeniceSurfaceElevated)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Continuous Runtime Auto-Learning",
                        color = VeniceTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Auto-extracts interfaces & tools from terminal outputs & chat",
                        color = VeniceTextMuted,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = isAutoLearningEnabled,
                    onCheckedChange = onToggleAutoLearning,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VeniceCyan,
                        checkedTrackColor = VeniceCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = VeniceTextMuted,
                        uncheckedTrackColor = VeniceSurface
                    ),
                    modifier = Modifier.testTag("daf_auto_learn_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val text = clip.getItemAt(0).text?.toString() ?: ""
                            if (text.isNotBlank()) {
                                onLearnFromText(text, "Clipboard / Terminal")
                                Toast.makeText(context, "Scanning clipboard for runtime facts...", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "No text in clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = VeniceCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Learn from Clip", color = VeniceCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = VenicePrivacyGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Rule", color = VenicePrivacyGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { showContextDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = VeniceTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable Facts List Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide Learned Facts (${facts.size})" else "Inspect Learned Facts (${facts.size})",
                    color = VeniceCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (isExpanded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Reset Defaults",
                            color = VeniceAmber,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { onResetDefaults() }
                        )
                        Text(
                            text = "Clear All",
                            color = VeniceRose,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { onClearAll() }
                        )
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (facts.isEmpty()) {
                        Text(
                            text = "No runtime facts learned yet. Paste terminal outputs or trigger chat interactions.",
                            color = VeniceTextMuted,
                            fontSize = 11.sp
                        )
                    } else {
                        facts.forEach { fact ->
                            AdaptiveFactRow(
                                fact = fact,
                                onToggle = { onToggleFact(fact.id, it) },
                                onDelete = { onRemoveFact(fact.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog to Add Custom Rule
    if (showAddDialog) {
        AddCustomRuleDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { cat, key, valStr ->
                onAddCustomFact(cat, key, valStr)
                showAddDialog = false
            }
        )
    }

    // Dialog to View Synthesized Prompt Context
    if (showContextDialog) {
        AlertDialog(
            onDismissRequest = { showContextDialog = false },
            confirmButton = {
                TextButton(onClick = { showContextDialog = false }) {
                    Text("Close", color = VeniceCyan)
                }
            },
            title = {
                Text("Synthesized Adaptive Prompt Context", color = VeniceTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF090E16))
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (promptContext.isNotBlank()) promptContext else "No active adaptive facts enabled.",
                        color = VeniceTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            },
            containerColor = VeniceSurface
        )
    }
}

@Composable
private fun AdaptiveFactRow(
    fact: AdaptiveFact,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val catColor = when (fact.category) {
        AdaptiveCategory.NETWORK_TELEMETRY -> VeniceCyan
        AdaptiveCategory.INSTALLED_PACKAGE -> VenicePrivacyGreen
        AdaptiveCategory.KERNEL_DRIVER -> VeniceThinkingPurple
        AdaptiveCategory.CHROOT_ENVIRONMENT -> VeniceAmber
        AdaptiveCategory.RUNTIME_SERVICE -> Color(0xFF64B5F6)
        AdaptiveCategory.SYSTEM_CONSTRAINT -> VeniceRose
        AdaptiveCategory.RUNTIME_ERROR_FIX -> Color(0xFFFFB74D)
        AdaptiveCategory.CUSTOM_DIRECTIVE -> Color(0xFF81C784)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF090E16))
            .border(
                1.dp,
                if (fact.isEnabled) catColor.copy(alpha = 0.3f) else VeniceBorder,
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(catColor.copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = fact.category.iconLabel,
                        color = catColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = fact.key,
                    color = if (fact.isEnabled) VeniceTextPrimary else VeniceTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = fact.value,
                color = if (fact.isEnabled) VeniceTextSecondary else VeniceTextMuted,
                fontSize = 11.sp
            )
            Text(
                text = "Source: ${fact.source}",
                color = VeniceTextMuted.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = fact.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = catColor,
                    checkedTrackColor = catColor.copy(alpha = 0.3f)
                ),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = VeniceRose.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (AdaptiveCategory, String, String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(AdaptiveCategory.CUSTOM_DIRECTIVE) }
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Dynamic Adaptive Rule", color = VeniceTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = VeniceSurfaceElevated,
                            unfocusedContainerColor = VeniceSurfaceElevated,
                            focusedTextColor = VeniceTextPrimary,
                            unfocusedTextColor = VeniceTextPrimary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        AdaptiveCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName, fontSize = 12.sp) },
                                onClick = {
                                    selectedCategory = cat
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Key / Identifier (e.g. wlan_adapter)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = VeniceSurfaceElevated,
                        unfocusedContainerColor = VeniceSurfaceElevated,
                        focusedTextColor = VeniceTextPrimary,
                        unfocusedTextColor = VeniceTextPrimary
                    )
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Adaptive Value / Rule Description", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = VeniceSurfaceElevated,
                        unfocusedContainerColor = VeniceSurfaceElevated,
                        focusedTextColor = VeniceTextPrimary,
                        unfocusedTextColor = VeniceTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (key.isNotBlank() && value.isNotBlank()) {
                        onAdd(selectedCategory, key, value)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VeniceCyan)
            ) {
                Text("Add Rule", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VeniceTextMuted)
            }
        },
        containerColor = VeniceSurface
    )
}

/**
 * Dynamic Adaptive GitHub Workflow Action Runner Card.
 * Gives user full insight and 1-tap copy/trigger controls for the CI/CD pipeline
 * that builds the Android debug APK.
 */
@Composable
fun AdaptiveGitHubRunnerCard() {
    val context = LocalContext.current
    var showWorkflowYaml by remember { mutableStateOf(false) }

    val workflowYamlContent = remember {
        """
name: Build Android Debug APK (Adaptive CI/CD Runner)

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]
  workflow_dispatch:
    inputs:
      build_variant:
        description: "Build variant to assemble"
        required: true
        default: "debug"
        type: choice
        options:
          - "debug"
          - "release"
      clean_build:
        description: "Perform clean build"
        required: false
        default: false
        type: boolean
      java_version:
        description: "JDK version"
        required: false
        default: "17"
        type: choice
        options:
          - "17"
          - "21"

jobs:
  build-apk:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '${'$'}{{ inputs.java_version || '17' }}'
      - uses: android-actions/setup-android@v3
      - uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: '${'$'}{{ runner.os }}-gradle-${'$'}{{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}'
      - name: Assemble Debug APK
        run: |
          ./gradlew assembleDebug --stacktrace --no-daemon \
            -Dorg.gradle.jvmargs="-Xmx3072m -XX:+UseParallelGC"
      - name: Calculate Checksum & Upload
        uses: actions/upload-artifact@v4
        with:
          name: VeniceAI-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
        """.trimIndent()
    }

    val netHunterBuildCommand = "bash ./build-debug-apk.sh"
    val ghCliTriggerCommand = "gh workflow run build-apk.yml -f build_variant=debug"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VeniceSurface),
        border = BorderStroke(1.dp, VeniceThinkingPurple.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(VeniceThinkingPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = VeniceThinkingPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GitHub Action Runner CI/CD",
                                color = VeniceTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(VenicePrivacyGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "WORKFLOW READY",
                                    color = VenicePrivacyGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = ".github/workflows/build-apk.yml • Adaptive Matrix",
                            color = VeniceTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Dynamic adaptive CI/CD pipeline configured to assemble the debug APK, compute SHA-256 integrity digests, and package artifacts with low-heap memory safeguards.",
                color = VeniceTextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Spec Matrix
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pipeline Trigger", color = VeniceTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("workflow_dispatch, push, PR", color = VeniceCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target Artifact", color = VeniceTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("app-debug.apk (30-day retention)", color = VenicePrivacyGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Adaptive JVM Heap", color = VeniceTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("-Xmx3072m (Auto-scaled for SM-A326B)", color = VeniceAmber, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Workflow YAML", workflowYamlContent))
                        Toast.makeText(context, "Copied .github/workflows/build-apk.yml to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = VeniceThinkingPurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy YAML", color = VeniceThinkingPurple, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("NetHunter Runner Script", netHunterBuildCommand))
                        Toast.makeText(context, "Copied NetHunter build command to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = VeniceCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Script", color = VeniceCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("GitHub CLI Command", ghCliTriggerCommand))
                        Toast.makeText(context, "Copied 'gh workflow run' command", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = VenicePrivacyGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable YAML Viewer
            Text(
                text = if (showWorkflowYaml) "Hide Workflow YAML" else "View Workflow YAML",
                color = VeniceTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { showWorkflowYaml = !showWorkflowYaml }
            )

            if (showWorkflowYaml) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF090D14))
                        .border(1.dp, VeniceBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = workflowYamlContent,
                        color = VeniceTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
