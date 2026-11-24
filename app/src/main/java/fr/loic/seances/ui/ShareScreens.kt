package fr.loic.seances.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import fr.loic.seances.data.Library
import fr.loic.seances.data.ShareCode
import fr.loic.seances.data.SharePayload
import fr.loic.seances.data.WorkoutPlan

@Composable
private fun DialogCard(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(themedShape(20.dp))
                .background(Palette.surface)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) { content() }
    }
}

/** QR code d'une séance + le même code en texte, à copier ou envoyer. */
@Composable
fun ShareDialog(plan: WorkoutPlan, library: Library, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val code = remember(plan) { library.shareCode(plan) }
    val matrix = remember(code) { qrMatrix(code) }

    DialogCard(onDismiss) {
        Text("Partager « ${plan.title} »", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.text, textAlign = TextAlign.Center)
        Text(
            "À scanner depuis Sporten : Accueil → Importer → Scanner un QR code.",
            color = Palette.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center,
        )
        QrCode(matrix, Modifier.fillMaxWidth())
        Text(
            "Pas de QR possible ? Envoie le code : la personne le colle dans Importer.",
            color = Palette.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center,
        )
        SelectionContainer(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 90.dp)
                .clip(themedShape(10.dp))
                .background(Palette.surfaceHigh)
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
        ) {
            Text(code, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Palette.text, lineHeight = 14.sp)
        }
        Row(Modifier.fillMaxWidth()) {
            FilledTonalButton(
                onClick = {
                    copyToClipboard(context, "Code de séance", code)
                    Toast.makeText(context, "Code copié", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
            ) { Text("Copier") }
            Spacer(Modifier.width(10.dp))
            FilledTonalButton(
                onClick = { shareText(context, ShareCode.message(plan, code), "Envoyer la séance") },
                modifier = Modifier.weight(1f),
            ) { Text("Envoyer…") }
        }
        TextButton(onClick = onDismiss) { Text("Fermer", color = Palette.textMuted) }
    }
}

private fun qrMatrix(text: String): BitMatrix = QRCodeWriter().encode(
    text, BarcodeFormat.QR_CODE, 0, 0,
    mapOf(
        EncodeHintType.MARGIN to 0,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.CHARACTER_SET to "UTF-8",
    ),
)

/** QR code noir sur fond blanc, avec la marge blanche dont les scanners ont besoin. */
@Composable
private fun QrCode(matrix: BitMatrix, modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(themedShape(12.dp))
            .background(Color.White)
            .padding(14.dp),
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val n = matrix.width
            val cell = size.width / n
            for (y in 0 until n) for (x in 0 until n) {
                if (matrix[x, y]) {
                    // +0.5 px pour éviter les fins liserés blancs entre les modules
                    drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}

/** Ajout d'une séance reçue : scan du QR code dans l'app, ou code / message collé. */
@Composable
fun ImportDialog(library: Library, onImported: (WorkoutPlan) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<SharePayload?>(null) }

    fun read(input: String) {
        val p = ShareCode.decode(input)
        if (p == null) {
            error = "Code non reconnu. Vérifie qu'il est complet (il commence par SDL1-)."
        } else {
            error = null
            found = p
        }
    }

    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { read(it) }
    }

    DialogCard(onDismiss) {
        val p = found
        if (p != null) {
            val exos = p.w.exerciseCount
            Text("Séance reçue", color = Palette.textMuted, fontSize = 14.sp)
            Text(p.w.title, style = titleStyle(22), color = Palette.text, textAlign = TextAlign.Center)
            if (p.w.subtitle.isNotBlank()) Text(p.w.subtitle, color = Palette.textMuted, textAlign = TextAlign.Center)
            Text(
                "${p.w.blocks.size} bloc(s) · $exos exos" + if (p.x.isNotEmpty()) " · ${p.x.size} exercice(s) perso" else "",
                color = Palette.textMuted, fontSize = 13.sp,
            )
            Button(
                onClick = {
                    val plan = library.addShared(p)
                    Toast.makeText(context, "« ${plan.title} » ajoutée à tes séances", Toast.LENGTH_SHORT).show()
                    onImported(plan)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Palette.work),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Ajouter à mes séances", color = Palette.onWork, fontWeight = FontWeight.Bold) }
            TextButton(onClick = { found = null }) { Text("Retour", color = Palette.textMuted) }
        } else {
            ImportForm(text, { text = it; error = null }, error, onScan = {
                scanner.launch(
                    ScanOptions()
                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        .setPrompt("Vise le QR code de la séance")
                        .setBeepEnabled(false)
                        .setOrientationLocked(true),
                )
            }, onValidate = { read(text) }, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun ImportForm(
    text: String,
    onText: (String) -> Unit,
    error: String?,
    onScan: () -> Unit,
    onValidate: () -> Unit,
    onDismiss: () -> Unit,
) {
    Text("Importer une séance", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.text)
    Button(
        onClick = onScan,
        colors = ButtonDefaults.buttonColors(containerColor = Palette.work),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) { Text("📷  Scanner un QR code", color = Palette.onWork, fontWeight = FontWeight.Bold) }
    Text("ou", color = Palette.textMuted)
    OutlinedTextField(
        value = text,
        onValueChange = onText,
        label = { Text("Colle le code ou le message reçu") },
        colors = textFieldColors(),
        maxLines = 4,
        modifier = Modifier.fillMaxWidth(),
    )
    error?.let { Text(it, color = Palette.danger, fontSize = 13.sp, textAlign = TextAlign.Center) }
    FilledTonalButton(onClick = onValidate, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
        Text("Valider le code")
    }
    TextButton(onClick = onDismiss) { Text("Annuler", color = Palette.textMuted) }
}
