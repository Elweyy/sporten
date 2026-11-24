package fr.loic.seances.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.loic.seances.data.HistoryEntry
import fr.loic.seances.data.HistoryOps
import fr.loic.seances.session.formatMinutes
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FR = Locale.FRANCE
private val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM 'à' HH:mm", FR)
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", FR)
private fun plural(n: Int, word: String) = "$n $word${if (n > 1) "s" else ""}"
private fun label(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(dateFmt)

/** Historique : chiffres de la semaine, série, calendrier du mois et séances faites. */
@Composable
fun HistoryScreen(
    history: List<HistoryEntry>,
    onDelete: (HistoryEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var month by remember { mutableStateOf(YearMonth.now()) }
    var toDelete by remember { mutableStateOf<HistoryEntry?>(null) }
    var musclesTab by remember { mutableStateOf(false) }
    var days by remember { mutableStateOf(30) }
    var selected by remember { mutableStateOf<fr.loic.seances.data.Muscle?>(null) }
    val inMonth = history.filter { YearMonth.from(HistoryOps.day(it.date)) == month }
    val perDay = inMonth.groupingBy { HistoryOps.day(it.date) }.eachCount()
    val today = LocalDate.now()

    LazyColumn(
        modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("←  Retour", color = Palette.textMuted) }
            Text("Historique", style = titleStyle(30), color = Palette.text)
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Palette.surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf(false to "📅  Séances", true to "💪  Muscles").forEach { (m, label) ->
                    Text(
                        label,
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (musclesTab == m) Palette.surfaceHigh else Color.Transparent)
                            .clickable { musclesTab = m }
                            .padding(vertical = 9.dp),
                        color = if (musclesTab == m) Palette.text else Palette.textMuted,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    )
                }
            }
        }
        if (musclesTab) {
            musclesTab(history, days, { days = it }, selected, { selected = it })
            return@LazyColumn
        }
        item {
            val streak = HistoryOps.streakWeeks(history)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("${HistoryOps.weekCount(history)}", "cette semaine", Modifier.weight(1f))
                Stat(if (streak >= 1) "🔥 $streak" else "0", if (streak > 1) "semaines d'affilée" else "semaine de série", Modifier.weight(1f))
                Stat("${history.size}", "au total", Modifier.weight(1f))
            }
        }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(themedShape(20.dp))
                    .background(Palette.surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1) }) { Text("‹", fontSize = 24.sp, color = Palette.text) }
                    Text(
                        month.format(monthFmt), Modifier.weight(1f), textAlign = TextAlign.Center,
                        color = Palette.text, fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.now()) {
                        Text("›", fontSize = 24.sp, color = if (month < YearMonth.now()) Palette.text else Palette.surfaceHigh)
                    }
                }
                Row {
                    listOf("L", "M", "M", "J", "V", "S", "D").forEach {
                        Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = Palette.textMuted, fontSize = 11.sp)
                    }
                }
                HistoryOps.monthGrid(month).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { d ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (d != null) CalendarDay(d.dayOfMonth, perDay[d] ?: 0, d == today)
                            }
                        }
                    }
                }
                Text(
                    if (inMonth.isEmpty()) "Aucune séance ce mois-ci"
                    else "${plural(inMonth.size, "séance")} · ${formatMinutes(inMonth.sumOf { it.durationMs })} ce mois-ci",
                    Modifier.fillMaxWidth(), color = Palette.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center,
                )
            }
        }
        items(inMonth.reversed(), key = { it.id }) { h ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(themedShape(16.dp))
                    .background(Palette.surface)
                    .padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        h.title + if (h.complete) "" else " · arrêtée",
                        color = Palette.text, fontWeight = FontWeight.SemiBold,
                    )
                    Text(label(h.date), color = Palette.textMuted, fontSize = 13.sp)
                    Text(
                        "${formatMinutes(h.durationMs)} · ${plural(h.exercisesDone, "exo")}" +
                            if (h.complete) " · ✓ complète" else " sur ${h.exercisesPlanned}",
                        color = Palette.textMuted, fontSize = 13.sp,
                    )
                }
                IconButton(onClick = { toDelete = h }) { Text("✕", color = Palette.textMuted) }
            }
        }
    }

    toDelete?.let { h ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Supprimer cette séance de l'historique ?") },
            text = { Text("${h.title}, ${label(h.date)}. Ta série et tes records seront recalculés.") },
            confirmButton = {
                TextButton(onClick = { onDelete(h); toDelete = null }) { Text("Supprimer", color = Palette.danger) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(themedShape(16.dp))
            .background(Palette.surface)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = titleStyle(24), color = Palette.accentText)
        Text(label, fontSize = 12.sp, color = Palette.textMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CalendarDay(day: Int, count: Int, today: Boolean) {
    Box(
        Modifier
            .widthIn(max = 44.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (count > 0) Palette.work else Color.Transparent)
            .then(if (today) Modifier.border(2.dp, Palette.text, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 1) "$day·$count" else "$day",
            color = when {
                count > 0 -> Palette.onWork
                today -> Palette.text
                else -> Palette.textMuted
            },
            fontSize = if (count > 1) 11.sp else 14.sp,
            fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
