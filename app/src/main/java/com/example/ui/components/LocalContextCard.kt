package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocalContextAttachment
import com.example.ui.theme.VeniceAmber
import com.example.ui.theme.VeniceSurfaceElevated
import com.example.ui.theme.VeniceTextPrimary
import com.example.ui.theme.VeniceTextSecondary
import java.util.Date

@Composable
fun LocalContextCard(
    attachment: LocalContextAttachment,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember(attachment) { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth().testTag("local_context_card"),
        colors = CardDefaults.cardColors(containerColor = VeniceSurfaceElevated)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Local ${attachment.kind}: ${attachment.path}", color = VeniceAmber, fontSize = 12.sp)
            Text(
                "Captured ${Date(attachment.capturedAt)}${if (attachment.truncated) " · Partial snapshot" else ""}",
                color = VeniceTextSecondary, fontSize = 11.sp
            )
            if (onRemove != null) {
                Text(
                    "Send shares this snapshot with Gemini. It remains in this conversation's context.",
                    color = VeniceTextSecondary, fontSize = 11.sp
                )
            }
            Row {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide content" else "Preview content")
                }
                if (onRemove != null) {
                    TextButton(onClick = onRemove, modifier = Modifier.testTag("remove_local_context")) {
                        Text("Remove")
                    }
                }
            }
            if (expanded) {
                SelectionContainer {
                    Text(
                        attachment.content.ifEmpty { "(empty)" },
                        modifier = Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState()),
                        color = VeniceTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp
                    )
                }
            }
        }
    }
}
