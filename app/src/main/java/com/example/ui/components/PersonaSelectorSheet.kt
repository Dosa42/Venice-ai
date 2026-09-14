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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Persona
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

@Composable
fun PersonaSelectorSheet(
    personas: List<Persona>,
    selectedPersona: Persona,
    onSelectPersona: (Persona) -> Unit,
    onCreateCustomPersona: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customTitle by remember { mutableStateOf("") }
    var customPrompt by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = VeniceSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, VeniceBorderHighlight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("persona_selector_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Venice Personas",
                            color = VeniceTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Customized system instructions & roles",
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

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(personas) { persona ->
                        val isSelected = selectedPersona.id == persona.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) VeniceSurfaceHighlight else VeniceSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isSelected) VeniceCyan else VeniceBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSelectPersona(persona)
                                    onDismiss()
                                }
                                .padding(12.dp)
                                .testTag("persona_item_${persona.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = persona.name,
                                            color = if (isSelected) VeniceCyan else VeniceTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(VeniceCyan.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = persona.tag,
                                                color = VeniceCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Text(
                                        text = persona.title,
                                        color = VeniceTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    Text(
                                        text = persona.description,
                                        color = VeniceTextMuted,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(top = 3.dp)
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = VeniceCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Create custom persona button
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VeniceBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_custom_persona_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Persona",
                        tint = VeniceAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Create Custom System Persona",
                        color = VeniceAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = VeniceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, VeniceBorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "New Custom Persona",
                        color = VeniceTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Persona Name (e.g. Socratic Tutor)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VeniceAmber,
                            unfocusedBorderColor = VeniceBorder,
                            focusedTextColor = VeniceTextPrimary,
                            unfocusedTextColor = VeniceTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Short Subtitle") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VeniceAmber,
                            unfocusedBorderColor = VeniceBorder,
                            focusedTextColor = VeniceTextPrimary,
                            unfocusedTextColor = VeniceTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        label = { Text("System Instruction (Role & Behavior)") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VeniceAmber,
                            unfocusedBorderColor = VeniceBorder,
                            focusedTextColor = VeniceTextPrimary,
                            unfocusedTextColor = VeniceTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showCreateDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = VeniceSurfaceElevated)
                        ) {
                            Text("Cancel", color = VeniceTextMuted)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (customName.isNotBlank() && customPrompt.isNotBlank()) {
                                    onCreateCustomPersona(
                                        customName.trim(),
                                        customTitle.ifBlank { "Custom Role" }.trim(),
                                        customPrompt.trim()
                                    )
                                    showCreateDialog = false
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VeniceAmber)
                        ) {
                            Text("Save Persona", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
