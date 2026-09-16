package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TerminalCommand
import com.example.ui.theme.*

@Composable
fun TerminalExecutionCard(commands: List<TerminalCommand>, onStop: (String) -> Unit, onStopWork: () -> Unit) {
    if (commands.isEmpty()) return
    var selectedId by remember { mutableStateOf<String?>(null) }
    val index = commands.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: commands.lastIndex
    val command = commands[index]
    var expanded by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = VeniceSurfaceElevated),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Terminal · ${command.environment}", color = VeniceCyan, fontSize = 12.sp)
                Text(command.exitCode?.let { "Exit $it" } ?: command.status, color = VeniceTextPrimary, fontSize = 12.sp)
            }
            Text(command.command, fontFamily = FontFamily.Monospace, color = VeniceTextPrimary,
                fontSize = 12.sp, maxLines = if (expanded) Int.MAX_VALUE else 2)
            if (expanded) {
                SelectionContainer {
                    Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                        Text("Directory: ${command.workdir}", color = VeniceTextMuted, fontSize = 11.sp)
                        Text(command.stdoutPreview, fontFamily = FontFamily.Monospace, color = VeniceTextPrimary, fontSize = 11.sp)
                        if (command.stderrPreview.isNotEmpty()) Text(command.stderrPreview,
                            fontFamily = FontFamily.Monospace, color = VeniceRose, fontSize = 11.sp)
                        command.error?.let { Text(it, color = VeniceRose, fontSize = 11.sp) }
                        Text("Latest output shown. Complete logs:\n${command.stdoutPath}\n${command.stderrPath}",
                            color = VeniceTextMuted, fontSize = 10.sp)
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide output" else "Show output") }
                if (index > 0) TextButton(onClick = { selectedId = commands[index - 1].id }) { Text("Previous") }
                if (index < commands.lastIndex) TextButton(onClick = { selectedId = commands[index + 1].id }) { Text("Next") }
                if (command.status == "starting" || command.status == "running") {
                    TextButton(onClick = { onStop(command.id) }) { Text("Stop") }
                }
                TextButton(onClick = onStopWork) { Text("Stop all") }
            }
        }
    }
}
