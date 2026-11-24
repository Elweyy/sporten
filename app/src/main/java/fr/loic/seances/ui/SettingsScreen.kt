package fr.loic.seances.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.loic.seances.data.Durations
import fr.loic.seances.data.Library

@Composable
fun SettingsScreen(
    durations: Durations,
    onChange: (Durations) -> Unit,
    library: Library,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    summary: Boolean = true,
    onSummaryChange: (Boolean) -> Unit = {},
    voice: Boolean = false,
    onVoiceChange: (Boolean) -> Unit = {},
    theme: AppThemeId = AppThemeId.SPORTEN,
    onThemeChange: (AppThemeId) -> Unit = {},
    updater: fr.loic.seances.update.Updater? = null,
    /** Rouvre la fenêtre « Quoi de neuf » (null si cette version n'a pas de notes). */
    onShowWhatsNew: (() -> Unit)? = null,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)!!.use { it.write(library.exportJson().toByteArray()) }
        }.isSuccess
        Toast.makeText(context, if (ok) "Sauvegarde exportée" else "Échec de l'export", Toast.LENGTH_SHORT).show()
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val result = runCatching {
            context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
        }.mapCatching { library.importJson(it).getOrThrow() }
        val msg = result.fold(
            onSuccess = { (w, e, h) -> "Importé : $w séance(s), $e exercice(s) perso" + if (h > 0) ", $h séance(s) d'historique" else "" },
            onFailure = { "Fichier illisible : ce n'est pas une sauvegarde de l'app" },
        )
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("←  Retour", color = Palette.textMuted) }
        Text("Réglages", style = titleStyle(30), color = Palette.text)

        Text("Durées par défaut des nouvelles séances", color = Palette.accentText, fontWeight = FontWeight.SemiBold)
        Text(
            "Chaque séance a ses propres durées, modifiables dans l'éditeur de séance (✎).",
            color = Palette.textMuted, fontSize = 13.sp,
        )
        DurationRow(
            "Durée d'un exercice", "", durations.work, Durations.WORK_RANGE, Durations.WORK_STEP,
        ) { onChange(durations.copy(work = it)) }
        DurationRow(
            "Transition entre les exos", "Pour changer de position · 0 = aucune", durations.transition,
            Durations.TRANSITION_RANGE, Durations.TRANSITION_STEP,
        ) { onChange(durations.copy(transition = it)) }
        DurationRow(
            "Repos entre les cycles", "0 = pas de repos", durations.restBetweenCycles,
            Durations.REST_RANGE, Durations.REST_STEP,
        ) { onChange(durations.copy(restBetweenCycles = it)) }
        DurationRow(
            "Préparation", "0 = démarrage direct", durations.prepare,
            Durations.PREPARE_RANGE, Durations.PREPARE_STEP,
        ) { onChange(durations.copy(prepare = it)) }

        Spacer(Modifier.height(8.dp))
        Text("Apparence", color = Palette.accentText, fontWeight = FontWeight.SemiBold)
        ThemePicker(theme, onThemeChange)

        Spacer(Modifier.height(8.dp))
        Text("Pendant et après la séance", color = Palette.accentText, fontWeight = FontWeight.SemiBold)
        SwitchRow("Annonces vocales", "Le téléphone annonce l'exercice suivant, le repos et les 10 dernières secondes", voice, onVoiceChange)
        SwitchRow("Résumé de fin de séance", "Texte à copier dans Strava (blocs et exercices faits)", summary, onSummaryChange)

        if (updater != null) {
            Spacer(Modifier.height(8.dp))
            UpdateSettings(updater, onShowWhatsNew)
        }

        Spacer(Modifier.height(8.dp))
        Text("Sauvegarde", color = Palette.accentText, fontWeight = FontWeight.SemiBold)
        Text(
            "Exporte tes séances, tes exercices perso et ton historique dans un fichier, pour les garder en cas de " +
                "réinstallation ou les partager. L'import ajoute le contenu du fichier (les séances de même " +
                "origine sont mises à jour, rien n'est supprimé).",
            color = Palette.textMuted, fontSize = 13.sp,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = { exportLauncher.launch("sporten.json") }, modifier = Modifier.weight(1f)) {
                Text("Exporter")
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.weight(1f)) {
                Text("Importer")
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(themedShape(16.dp))
            .background(Palette.surface)
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Palette.text, fontWeight = FontWeight.SemiBold)
            Text(hint, color = Palette.textMuted, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Palette.work, checkedThumbColor = Palette.onWork),
        )
    }
}

/** Vignettes des thèmes, deux par ligne : fond, une « carte » et un bouton d'accent. */
@Composable
private fun ThemePicker(selected: AppThemeId, onSelect: (AppThemeId) -> Unit) {
    val systemDark = isSystemInDarkTheme()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppThemeId.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { id ->
                    val on = id == selected
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(themedShape(16.dp))
                            .background(Palette.surface)
                            .border(2.dp, if (on) Palette.work else Color.Transparent, themedShape(16.dp))
                            .clickable { onSelect(id) }
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ThemeSwatch(id, systemDark)
                        Text(
                            id.label + if (on) " ✓" else "",
                            color = Palette.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        val now = Themes.resolve(selected, systemDark).id
        Text(
            selected.hint + if (selected == AppThemeId.SYSTEM) ". En ce moment : ${now.label}" else "",
            color = Palette.textMuted, fontSize = 13.sp,
        )
    }
}

@Composable
private fun ThemeSwatch(id: AppThemeId, systemDark: Boolean) {
    val t = Themes.resolve(id, systemDark)
    val square = id == AppThemeId.ARCADE
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(if (Palette.isArcade) 0.dp else 11.dp)),
    ) {
        val pad = 10.dp.toPx()
        if (id == AppThemeId.SYSTEM) {
            drawRect(Themes.LIGHT.background)
            val tri = Path().apply { moveTo(size.width, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
            drawPath(tri, Themes.SPORTEN.background)
        } else if (id == AppThemeId.NEON) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0F0620), Color(0xFF3A0B4A))))
        } else {
            drawRect(t.background)
        }
        val r = if (square) 0f else 6.dp.toPx()
        val cardColor = if (id == AppThemeId.SYSTEM) Color(0x73808080) else t.surfaceHigh
        drawRoundRect(cardColor, Offset(pad, pad), Size(size.width - 2 * pad, 20.dp.toPx()), CornerRadius(r))
        if (id == AppThemeId.NEON) {
            drawRoundRect(t.rest.copy(alpha = 0.6f), Offset(pad, pad), Size(size.width - 2 * pad, 20.dp.toPx()), CornerRadius(r), style = Stroke(1.dp.toPx()))
        }
        val accent = Size(size.width * 0.46f, 14.dp.toPx())
        val accentAt = Offset(pad, size.height - pad - accent.height)
        if (square) drawRect(Color.Black, accentAt + Offset(3.dp.toPx(), 3.dp.toPx()), accent)
        drawRoundRect(t.work, accentAt, accent, CornerRadius(if (square) 0f else accent.height / 2))
    }
}
