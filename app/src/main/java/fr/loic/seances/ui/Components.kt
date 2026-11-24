package fr.loic.seances.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.loic.seances.anim.Anims
import fr.loic.seances.anim.StickFigure
import fr.loic.seances.data.ExerciseDef
import fr.loic.seances.data.Muscle
import fr.loic.seances.data.formatDuration

@Composable
fun DurationRow(
    label: String,
    hint: String,
    value: Int,
    range: IntRange,
    step: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(themedShape(16.dp))
            .background(Palette.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Palette.text, fontWeight = FontWeight.SemiBold)
            if (hint.isNotEmpty()) Text(hint, color = Palette.textMuted, fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(
            onClick = { onChange((value - step).coerceAtLeast(range.first)) },
            enabled = value > range.first,
        ) { Text("−", fontSize = 22.sp) }
        Text(
            formatDuration(value),
            Modifier.width(76.dp),
            style = titleStyle(18),
            color = Palette.accentText,
            textAlign = TextAlign.Center,
        )
        FilledTonalIconButton(
            onClick = { onChange((value + step).coerceAtMost(range.last)) },
            enabled = value < range.last,
        ) { Text("+", fontSize = 22.sp) }
    }
}

/** Puces de groupes musculaires. [selected] = null : « Tous » (si [allowAll]). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuscleChips(selected: Muscle?, onSelect: (Muscle?) -> Unit, allowAll: Boolean = true) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (allowAll) Chip("Tous", selected == null) { onSelect(null) }
        Muscle.entries.forEach { m -> Chip(m.label, selected == m) { onSelect(m) } }
    }
}

/** Choix unique parmi quelques libellés (ex. Maison / Salle). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextChips(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, l -> Chip(l, selected == i) { onSelect(i) } }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Palette.work,
            selectedLabelColor = Palette.onWork,
            labelColor = Palette.text,
        ),
    )
}

/** Miniature animée d'un exercice. */
@Composable
fun AnimThumb(animId: String, size: Dp = 52.dp, playing: Boolean = true) {
    Box(
        Modifier
            .size(size)
            .clip(themedShape(10.dp))
            .background(Palette.surfaceHigh),
    ) {
        StickFigure(
            Anims.get(animId),
            Modifier
                .size(size)
                .padding(3.dp),
            playing = playing,
            accentColor = Palette.work,
        )
    }
}

/** Ligne d'exercice : miniature, nom, groupe musculaire, puis [trailing]. */
@Composable
fun ExerciseRow(
    ex: ExerciseDef,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(themedShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimThumb(ex.animId)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(ex.name, color = Palette.text, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                ex.muscle.label + (if (ex.custom) " · perso" else "") + (if (ex.gym) " · salle" else ""),
                color = Palette.textMuted, fontSize = 12.sp,
            )
        }
        trailing()
    }
}

@Composable
fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.work,
    unfocusedBorderColor = Palette.surfaceHigh,
    focusedLabelColor = Palette.work,
    cursorColor = Palette.work,
    focusedTextColor = Palette.text,
    unfocusedTextColor = Palette.text,
)

fun copyToClipboard(context: android.content.Context, label: String, text: String) {
    val cm = context.getSystemService(android.content.ClipboardManager::class.java)
    cm?.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
}

/** Ouvre le menu de partage Android (WhatsApp, SMS, mail…) avec ce texte. */
fun shareText(context: android.content.Context, text: String, chooserTitle: String) {
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    context.startActivity(android.content.Intent.createChooser(send, chooserTitle))
}
