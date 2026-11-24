package fr.loic.seances.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.loic.seances.update.WhatsNew

/** Fenêtre « Quoi de neuf dans la X » avec les notes de version. */
@Composable
fun WhatsNewDialog(version: String, notes: String, onDismiss: () -> Unit) {
    val lines = remember(notes) { WhatsNew.parse(notes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✨ Quoi de neuf dans la $version") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                lines.forEachIndexed { i, line ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    val text = buildAnnotatedString {
                        line.spans.forEach { s ->
                            if (s.bold) withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Palette.text)) { append(s.text) }
                            else append(s.text)
                        }
                    }
                    if (line.bullet) {
                        Row {
                            Text("•", color = Palette.accentText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Text(text, color = Palette.textMuted, fontSize = 15.sp, lineHeight = 20.sp)
                        }
                    } else {
                        Text(text, color = Palette.textMuted, fontSize = 15.sp, lineHeight = 20.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Palette.work)) {
                Text("C'est parti", color = Palette.onWork, fontWeight = FontWeight.Bold)
            }
        },
    )
}
