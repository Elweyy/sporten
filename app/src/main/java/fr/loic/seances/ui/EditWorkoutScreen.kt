package fr.loic.seances.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.loic.seances.data.BlockPlan
import fr.loic.seances.data.Durations
import fr.loic.seances.data.Library
import fr.loic.seances.data.Reps
import fr.loic.seances.data.WorkoutMode
import fr.loic.seances.data.WorkoutPlan
import fr.loic.seances.data.formatDuration

private data class PickTarget(val block: Int, val replaceIndex: Int?)

private fun <T> List<T>.moved(from: Int, delta: Int): List<T> {
    val to = from + delta
    if (from !in indices || to !in indices) return this
    return toMutableList().also { it.add(to, it.removeAt(from)) }
}

private fun <T> List<T>.replacedAt(i: Int, v: T) = toMutableList().also { it[i] = v }

/** Modifie la liste des exercices d'un bloc en gardant chaque exercice avec ses répétitions. */
private fun BlockPlan.mapExercises(f: (List<Pair<String, Int>>) -> List<Pair<String, Int>>): BlockPlan {
    val pairs = f(exerciseIds.indices.map { exerciseIds[it] to repsAt(it) })
    return copy(exerciseIds = pairs.map { it.first }, reps = pairs.map { it.second })
}

@Composable
fun EditWorkoutScreen(
    initial: WorkoutPlan,
    isNew: Boolean,
    library: Library,
    onSave: (WorkoutPlan) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Une valeur de reps par exercice pendant l'édition ; recompactées à l'enregistrement.
    val start = remember { initial.copy(blocks = initial.blocks.map { it.withAlignedReps() }) }
    var draft by remember { mutableStateOf(start) }
    var picking by remember { mutableStateOf<PickTarget?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    val exercises = library.exercisesById

    picking?.let { target ->
        CatalogScreen(
            library = library,
            onBack = { picking = null },
            modifier = modifier,
            onPick = { ex ->
                draft = draft.copy(
                    blocks = draft.blocks.replacedAt(
                        target.block,
                        draft.blocks[target.block].mapExercises { list ->
                            if (target.replaceIndex != null) list.replacedAt(target.replaceIndex, ex.id to list[target.replaceIndex].second)
                            else list + (ex.id to Reps.DEFAULT)
                        },
                    ),
                )
                picking = null
            },
        )
        return
    }

    val leave = { if (draft != start) confirmLeave = true else onCancel() }
    BackHandler(onBack = leave)

    fun updateBlock(i: Int, f: (BlockPlan) -> BlockPlan) {
        draft = draft.copy(blocks = draft.blocks.replacedAt(i, f(draft.blocks[i])))
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = leave) { Text("Annuler", color = Palette.textMuted) }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    onSave(
                        draft.copy(
                            title = draft.title.trim(),
                            subtitle = draft.subtitle.trim(),
                            blocks = draft.blocks.map { it.compactReps() },
                        ),
                    )
                },
                enabled = draft.title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.work),
            ) { Text("Enregistrer", color = Palette.onWork, fontWeight = FontWeight.Bold) }
        }
        Text(
            if (isNew) "Nouvelle séance" else "Modifier la séance",
            style = titleStyle(26), color = Palette.text,
        )
        OutlinedTextField(
            value = draft.title, onValueChange = { draft = draft.copy(title = it) },
            label = { Text("Nom de la séance") }, singleLine = true,
            colors = textFieldColors(), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.subtitle, onValueChange = { draft = draft.copy(subtitle = it) },
            label = { Text("Description (ex : Épaules — Abdos — Jambes)") }, singleLine = true,
            colors = textFieldColors(), modifier = Modifier.fillMaxWidth(),
        )

        // Type de séance
        Text("Type de séance", color = Palette.accentText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        ModePicker(draft.mode) { draft = draft.copy(mode = it) }
        val reps = draft.mode == WorkoutMode.REPS

        // Durées de la séance
        Text("Durées", color = Palette.accentText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        val d = draft.durations
        fun setD(nd: Durations) { draft = draft.copy(durations = nd) }
        if (!reps) DurationRow("Exercice", "", d.work, Durations.WORK_RANGE, Durations.WORK_STEP) { setD(d.copy(work = it)) }
        DurationRow("Transition entre exos", "Pour changer de position · 0 = aucune", d.transition,
            Durations.TRANSITION_RANGE, Durations.TRANSITION_STEP) { setD(d.copy(transition = it)) }
        DurationRow("Repos entre cycles", "0 = aucun", d.restBetweenCycles, Durations.REST_RANGE, Durations.REST_STEP) {
            setD(d.copy(restBetweenCycles = it))
        }
        DurationRow("Préparation", "0 = aucune", d.prepare, Durations.PREPARE_RANGE, Durations.PREPARE_STEP) {
            setD(d.copy(prepare = it))
        }

        // Blocs
        val count = draft.exerciseCount
        Text(
            if (reps) "Blocs · $count exercice(s) · ${draft.repsPerCycle} reps par cycle"
            else "Blocs · $count exercice(s) · ${formatDuration(count * d.work)} d'effort par cycle",
            color = Palette.work, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp),
        )
        draft.blocks.forEachIndexed { bi, block ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(themedShape(16.dp))
                    .background(Palette.surface)
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = block.title,
                        onValueChange = { t -> updateBlock(bi) { it.copy(title = t) } },
                        label = { Text("Bloc ${bi + 1}") }, singleLine = true,
                        colors = textFieldColors(), modifier = Modifier.weight(1f),
                    )
                    SmallButton("↑", enabled = bi > 0) { draft = draft.copy(blocks = draft.blocks.moved(bi, -1)) }
                    SmallButton("↓", enabled = bi < draft.blocks.lastIndex) { draft = draft.copy(blocks = draft.blocks.moved(bi, 1)) }
                    SmallButton("✕", color = Palette.danger) {
                        draft = draft.copy(blocks = draft.blocks.filterIndexed { i, _ -> i != bi })
                    }
                }
                Spacer(Modifier.height(4.dp))
                block.exerciseIds.forEachIndexed { ei, id ->
                    val ex = exercises[id]
                    if (ex == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Exercice introuvable", color = Palette.textMuted, modifier = Modifier.weight(1f).padding(8.dp))
                            SmallButton("✕") { updateBlock(bi) { b -> b.mapExercises { l -> l.filterIndexed { i, _ -> i != ei } } } }
                        }
                    } else {
                        ExerciseRow(
                            ex,
                            onClick = { picking = PickTarget(bi, ei) },
                            trailing = {
                                SmallButton("↑", enabled = ei > 0) {
                                    updateBlock(bi) { b -> b.mapExercises { it.moved(ei, -1) } }
                                }
                                SmallButton("↓", enabled = ei < block.exerciseIds.lastIndex) {
                                    updateBlock(bi) { b -> b.mapExercises { it.moved(ei, 1) } }
                                }
                                SmallButton("✕") {
                                    updateBlock(bi) { b -> b.mapExercises { l -> l.filterIndexed { i, _ -> i != ei } } }
                                }
                            },
                        )
                        if (reps) {
                            RepsStepper(block.repsAt(ei)) { v ->
                                updateBlock(bi) { b -> b.mapExercises { l -> l.replacedAt(ei, l[ei].first to v) } }
                            }
                        }
                    }
                }
                TextButton(onClick = { picking = PickTarget(bi, null) }) {
                    Text("+ Ajouter un exercice", color = Palette.accentText)
                }
                if (block.exerciseIds.isNotEmpty()) {
                    Text("Touche un exercice pour le remplacer.", color = Palette.textMuted, fontSize = 11.sp)
                }
            }
        }
        OutlinedButton(
            onClick = {
                draft = draft.copy(blocks = draft.blocks + BlockPlan("Bloc ${draft.blocks.size + 1}"))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("+ Ajouter un bloc", color = Palette.text) }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Abandonner les modifications ?") },
            confirmButton = { TextButton(onClick = onCancel) { Text("Abandonner") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Continuer") } },
        )
    }
}

@Composable
private fun ModePicker(mode: WorkoutMode, onChange: (WorkoutMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple(WorkoutMode.TIMED, "⏱  Chrono", "Max de reps en un temps donné"),
            Triple(WorkoutMode.REPS, "🔢  Répétitions", "Un nombre de reps par exo, « Fait » pour passer au suivant"),
        ).forEach { (m, title, hint) ->
            val on = m == mode
            Column(
                Modifier
                    .weight(1f)
                    .clip(themedShape(16.dp))
                    .background(if (on) Palette.work.copy(alpha = 0.18f) else Palette.surface)
                    .border(2.dp, if (on) Palette.work else Color.Transparent, themedShape(16.dp))
                    .clickable { onChange(m) }
                    .padding(12.dp),
            ) {
                Text(title, color = if (on) Palette.text else Palette.textMuted, fontWeight = FontWeight.Bold)
                Text(hint, color = Palette.textMuted, fontSize = 12.sp, lineHeight = 15.sp)
            }
        }
    }
}

/** Réglage du nombre de répétitions d'un exercice, sous sa ligne. */
@Composable
private fun RepsStepper(value: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 60.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Répétitions", color = Palette.textMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        SmallButton("−", enabled = value > Reps.RANGE.first) { onChange((value - 1).coerceAtLeast(Reps.RANGE.first)) }
        Text(
            "$value",
            Modifier.width(40.dp),
            style = titleStyle(18),
            color = Palette.accentText,
            textAlign = TextAlign.Center,
        )
        SmallButton("+", enabled = value < Reps.RANGE.last) { onChange((value + 1).coerceAtMost(Reps.RANGE.last)) }
    }
}

@Composable
private fun SmallButton(
    glyph: String,
    enabled: Boolean = true,
    color: Color = Palette.text,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Text(glyph, color = if (enabled) color else Palette.surfaceHigh, fontSize = 16.sp)
    }
}
