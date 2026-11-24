package fr.loic.seances.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.window.Dialog
import fr.loic.seances.anim.Anims
import fr.loic.seances.anim.StickFigure
import fr.loic.seances.data.ExerciseDef
import fr.loic.seances.data.HistoryEntry
import fr.loic.seances.data.HistoryOps
import fr.loic.seances.data.Timings
import fr.loic.seances.data.WorkoutMode
import fr.loic.seances.data.WorkoutPlan
import fr.loic.seances.data.formatDuration
import fr.loic.seances.session.chainBreakSec
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun HomeScreen(
    workouts: List<WorkoutPlan>,
    exercises: Map<String, ExerciseDef>,
    cycles: Int,
    onCyclesChange: (Int) -> Unit,
    onStart: (WorkoutPlan) -> Unit,
    onEdit: (WorkoutPlan) -> Unit,
    onNew: () -> Unit,
    onDuplicate: (WorkoutPlan) -> Unit,
    onDelete: (WorkoutPlan) -> Unit,
    onMove: (WorkoutPlan, Int) -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenSettings: () -> Unit,
    onShare: (WorkoutPlan) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    history: List<HistoryEntry> = emptyList(),
    onOpenHistory: () -> Unit = {},
    onRandom: () -> Unit = {},
    /** Super séance : enchaîne les séances choisies, dans l'ordre. */
    onStartChain: (List<WorkoutPlan>) -> Unit = {},
    /** Bandeau « nouvelle version » (vide s'il n'y en a pas). */
    banner: @Composable () -> Unit = {},
) {
    var chaining by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<ExerciseDef?>(null) }
    var toDelete by remember { mutableStateOf<WorkoutPlan?>(null) }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Text("Sporten", style = titleStyle(30), color = Palette.text)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpenCatalog, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("📚  Catalogue", color = Palette.textMuted) }
                TextButton(onClick = onImport, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("📥  Importer", color = Palette.textMuted) }
                TextButton(onClick = onOpenSettings, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("⚙  Réglages", color = Palette.textMuted) }
            }
            Spacer(Modifier.height(8.dp))
            banner()
            HistoryCard(history, onOpenHistory)
            Spacer(Modifier.height(12.dp))
            CyclesPicker(cycles, onCyclesChange)
        }
        if (workouts.isEmpty()) item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(themedShape(20.dp))
                    .background(Palette.surface)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🏁 Pas encore de séance", style = titleStyle(20), color = Palette.text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Crée ta séance avec « + Nouvelle séance », tire-en une au hasard avec « 🎲 Aléatoire », " +
                        "ou importe celle qu'on t'a partagée (QR code ou code) avec « 📥 Importer ».",
                    color = Palette.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center,
                )
            }
        }
        itemsIndexed(workouts, key = { _, w -> w.id }) { index, workout ->
            WorkoutCard(
                workout = workout,
                exercises = exercises,
                cycles = cycles,
                expanded = expanded == workout.id,
                isFirst = index == 0,
                isLast = index == workouts.lastIndex,
                onToggle = { expanded = if (expanded == workout.id) null else workout.id },
                onStart = { onStart(workout) },
                onEdit = { onEdit(workout) },
                onDuplicate = { onDuplicate(workout) },
                onShare = { onShare(workout) },
                onDelete = { toDelete = workout },
                onMove = { onMove(workout, it) },
                onPreview = { preview = it },
            )
        }
        item {
            if (workouts.count { it.exerciseCount > 0 } >= 2) {
                OutlinedButton(onClick = { chaining = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("⛓️ Super séance", color = Palette.text)
                }
                Spacer(Modifier.height(12.dp))
            }
            Row {
                OutlinedButton(onClick = onRandom, modifier = Modifier.weight(1f)) {
                    Text("🎲 Aléatoire", color = Palette.text)
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = onNew, modifier = Modifier.weight(1f)) {
                    Text("+ Nouvelle séance", color = Palette.text)
                }
            }
        }
    }

    preview?.let { ex ->
        Dialog(onDismissRequest = { preview = null }) {
            Column(
                Modifier
                    .clip(themedShape(20.dp))
                    .background(Palette.surface)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(ex.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.text, textAlign = TextAlign.Center)
                StickFigure(Anims.get(ex.animId), Modifier.size(260.dp), accentColor = Palette.work)
                Text(ex.cue, color = Palette.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
                TextButton(onClick = { preview = null }) { Text("Fermer") }
            }
        }
    }

    if (chaining) {
        SuperSessionDialog(
            workouts = workouts.filter { it.exerciseCount > 0 },
            cycles = cycles,
            onStart = { chaining = false; onStartChain(it) },
            onDismiss = { chaining = false },
        )
    }

    toDelete?.let { w ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Supprimer « ${w.title} » ?") },
            text = { Text("La séance sera supprimée. Les exercices restent dans le catalogue.") },
            confirmButton = {
                TextButton(onClick = { onDelete(w); toDelete = null }) { Text("Supprimer", color = Palette.danger) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annuler") } },
        )
    }
}

/** Carte « cette semaine » : ouvre l'historique. */
@Composable
private fun HistoryCard(history: List<HistoryEntry>, onOpen: () -> Unit) {
    val week = HistoryOps.weekCount(history)
    val streak = HistoryOps.streakWeeks(history)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(themedShape(16.dp))
            .background(Palette.surface)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🗓️", fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (history.isEmpty()) "Historique" else "Cette semaine : $week séance${if (week > 1) "s" else ""}",
                color = Palette.text, fontWeight = FontWeight.SemiBold,
            )
            Text(
                when {
                    history.isEmpty() -> "Tes séances terminées apparaîtront ici"
                    streak >= 1 -> "🔥 Série : $streak semaine${if (streak > 1) "s" else ""} d'affilée"
                    else -> "Fais une séance pour lancer ta série"
                },
                color = Palette.textMuted, fontSize = 13.sp,
            )
        }
        Text("›", color = Palette.textMuted, fontSize = 22.sp)
    }
}

@Composable
private fun CyclesPicker(cycles: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(themedShape(16.dp))
            .background(Palette.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Nombre de cycles", color = Palette.text, fontWeight = FontWeight.SemiBold)
            Text("Combien de fois enchaîner tous les blocs", color = Palette.textMuted, fontSize = 13.sp)
        }
        FilledTonalIconButton(onClick = { onChange((cycles - 1).coerceAtLeast(1)) }) {
            Text("−", fontSize = 22.sp)
        }
        Text(
            "$cycles",
            Modifier.width(44.dp),
            style = titleStyle(26),
            color = Palette.accentText,
            textAlign = TextAlign.Center,
        )
        FilledTonalIconButton(onClick = { onChange((cycles + 1).coerceAtMost(Timings.MAX_CYCLES)) }) {
            Text("+", fontSize = 22.sp)
        }
    }
}

private fun totalMinutes(w: WorkoutPlan, cycles: Int): Int =
    (w.durations.totalSec(cycles, w.exerciseCount) + 59) / 60

@Composable
private fun WorkoutCard(
    workout: WorkoutPlan,
    exercises: Map<String, ExerciseDef>,
    cycles: Int,
    expanded: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: () -> Unit,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
    onPreview: (ExerciseDef) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val count = workout.exerciseCount
    Column(
        Modifier
            .fillMaxWidth()
            .clip(themedShape(20.dp))
            .background(Palette.surface)
            .clickable(onClick = onToggle)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(workout.title, style = titleStyle(22), color = Palette.text)
                if (workout.subtitle.isNotBlank()) Text(workout.subtitle, color = Palette.textMuted, fontSize = 15.sp)
            }
            Box {
                IconButton(onClick = { menu = true }) { Text("⋮", color = Palette.textMuted, fontSize = 22.sp) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Modifier") }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Dupliquer") }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Partager (QR / code)") }, onClick = { menu = false; onShare() })
                    if (!isFirst) DropdownMenuItem(text = { Text("Monter") }, onClick = { menu = false; onMove(-1) })
                    if (!isLast) DropdownMenuItem(text = { Text("Descendre") }, onClick = { menu = false; onMove(1) })
                    DropdownMenuItem(
                        text = { Text("Supprimer", color = Palette.danger) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
            Button(
                onClick = onStart,
                enabled = count > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Palette.work),
            ) { Text("▶  Go", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (count == 0) "Séance vide · touche ⋮ puis Modifier pour ajouter des exercices"
            else if (workout.mode == WorkoutMode.REPS) "🔢 Répétitions · ${workout.blocks.size} bloc(s) · $count exos · " +
                "${workout.repsPerCycle} reps par cycle · $cycles cycle(s)"
            else "${workout.blocks.size} bloc(s) · $count exos · ${formatDuration(workout.durations.work)}/exo · " +
                "≈ ${totalMinutes(workout, cycles)} min en $cycles cycle(s)",
            color = Palette.textMuted, fontSize = 12.sp,
        )
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            workout.blocks.forEachIndexed { bi, block ->
                Text(
                    "Bloc ${bi + 1} · ${block.title}",
                    color = Palette.work, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                block.exerciseIds.forEachIndexed { ei, id ->
                    val ex = exercises[id] ?: return@forEachIndexed
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(themedShape(10.dp))
                            .clickable { onPreview(ex) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Palette.textMuted),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(ex.name, color = Palette.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (workout.mode == WorkoutMode.REPS) {
                            Text("×${block.repsAt(ei)}", color = Palette.accentText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(10.dp))
                        }
                        Text("voir", color = Palette.textMuted, fontSize = 12.sp)
                    }
                }
            }
            TextButton(onClick = onEdit) { Text("✎  Modifier cette séance", color = Palette.accentText) }
        }
    }
}

/**
 * Choix des séances d'une super séance : on touche les séances dans l'ordre où on veut les faire
 * (un numéro s'affiche), on touche à nouveau pour en retirer une.
 */
@Composable
private fun SuperSessionDialog(
    workouts: List<WorkoutPlan>,
    cycles: Int,
    onStart: (List<WorkoutPlan>) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosen by remember { mutableStateOf(listOf<String>()) }
    val plans = chosen.mapNotNull { id -> workouts.firstOrNull { it.id == id } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⛓️ Super séance") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Touche les séances dans l'ordre où tu veux les faire. Chacune se fait en $cycles cycle(s), " +
                        "avec une pause entre deux séances.",
                    color = Palette.textMuted, fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                workouts.forEach { w ->
                    val pos = chosen.indexOf(w.id)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(themedShape(12.dp))
                            .background(if (pos >= 0) Palette.surfaceHigh else Color.Transparent)
                            .clickable { chosen = if (pos >= 0) chosen - w.id else chosen + w.id }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (pos >= 0) Palette.work else Palette.surfaceHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (pos >= 0) Text("${pos + 1}", color = Palette.onWork, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.title, color = Palette.text, fontWeight = FontWeight.SemiBold)
                            Text(
                                (if (w.mode == WorkoutMode.REPS) "🔢 Répétitions · " else "") +
                                    "${w.exerciseCount} exos" +
                                    (if (w.subtitle.isNotBlank()) " · ${w.subtitle}" else ""),
                                color = Palette.textMuted, fontSize = 12.sp,
                            )
                        }
                    }
                }
                if (plans.size >= 2) {
                    Spacer(Modifier.height(8.dp))
                    Text(plans.joinToString(" → ") { it.title }, color = Palette.accentText, fontSize = 13.sp)
                    if (plans.none { it.mode == WorkoutMode.REPS }) {
                        Text("≈ ${(chainTotalSec(plans, cycles) + 59) / 60} min au total", color = Palette.textMuted, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStart(plans) },
                enabled = plans.size >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = Palette.work),
            ) { Text("▶  Go", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/** Durée estimée d'une super séance en mode Chrono (pauses entre séances comprises). */
private fun chainTotalSec(plans: List<WorkoutPlan>, cycles: Int): Int =
    plans.mapIndexed { i, w ->
        val own = w.durations.totalSec(cycles, w.exerciseCount)
        if (i == 0) own else own - w.durations.prepare + chainBreakSec(plans[i - 1].durations, w.durations)
    }.sum()
