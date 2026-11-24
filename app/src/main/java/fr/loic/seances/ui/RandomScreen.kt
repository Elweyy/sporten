package fr.loic.seances.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import fr.loic.seances.data.Durations
import fr.loic.seances.data.ExerciseDef
import fr.loic.seances.data.Muscle
import fr.loic.seances.data.RandomWorkout
import fr.loic.seances.data.WorkoutPlan

/** Séance aléatoire : groupes musculaires + nombre d'exos par groupe, puis tirage (↻ par exercice). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RandomScreen(
    exercises: List<ExerciseDef>,
    durations: Durations,
    cycles: Int,
    initialMuscles: List<Muscle>,
    initialPerBlock: Int,
    onChoiceChange: (List<Muscle>, Int) -> Unit,
    onStart: (WorkoutPlan) -> Unit,
    onSave: (WorkoutPlan) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var muscles by remember { mutableStateOf(initialMuscles) }
    var perBlock by remember { mutableStateOf(initialPerBlock) }
    fun draw(m: List<Muscle>, n: Int) = if (m.isEmpty()) null else RandomWorkout.generate(exercises, m, n, durations)
    var plan by remember { mutableStateOf(draw(muscles, perBlock)) }
    val byId = remember(exercises) { exercises.associateBy { it.id } }

    fun change(m: List<Muscle>, n: Int) {
        muscles = m.sortedBy { it.ordinal }
        perBlock = n
        onChoiceChange(muscles, n)
        plan = draw(muscles, n)
    }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            TextButton(onClick = onBack) { Text("←  Retour", color = Palette.textMuted) }
            Text("Séance aléatoire", style = titleStyle(30), color = Palette.text)
            Spacer(Modifier.height(8.dp))
            Text("Groupes musculaires (un bloc par groupe)", color = Palette.textMuted, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Muscle.entries.forEach { m ->
                    FilterChip(
                        selected = m in muscles,
                        onClick = { change(if (m in muscles) muscles - m else muscles + m, perBlock) },
                        label = { Text(m.label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Palette.work,
                            selectedLabelColor = Palette.onWork,
                            labelColor = Palette.text,
                        ),
                    )
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(themedShape(16.dp))
                    .background(Palette.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Exos par groupe", Modifier.weight(1f), color = Palette.text, fontWeight = FontWeight.SemiBold)
                FilledTonalIconButton(
                    onClick = { change(muscles, perBlock - 1) }, enabled = perBlock > RandomWorkout.MIN_PER_BLOCK,
                ) { Text("−", fontSize = 22.sp) }
                Text("$perBlock", Modifier.width(44.dp), style = titleStyle(26), color = Palette.accentText, textAlign = TextAlign.Center)
                FilledTonalIconButton(
                    onClick = { change(muscles, perBlock + 1) }, enabled = perBlock < RandomWorkout.MAX_PER_BLOCK,
                ) { Text("+", fontSize = 22.sp) }
            }
        }
        val p = plan
        if (p == null) {
            item { Text("Choisis au moins un groupe musculaire.", Modifier.fillMaxWidth(), color = Palette.textMuted, textAlign = TextAlign.Center) }
        } else {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val count = p.exerciseCount
                    Text(
                        "$count exos · ≈ ${(p.durations.totalSec(cycles, count) + 59) / 60} min en $cycles cycle(s)",
                        Modifier.weight(1f), color = Palette.textMuted, fontSize = 13.sp,
                    )
                    TextButton(onClick = { plan = draw(muscles, perBlock) }) { Text("🎲  Retirer tout", color = Palette.accentText) }
                }
            }
            p.blocks.forEachIndexed { bi, block ->
                item(key = "b$bi") {
                    Text("Bloc ${bi + 1} · ${block.title}", color = Palette.accentText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                block.exerciseIds.forEachIndexed { ei, id ->
                    val ex = byId[id] ?: return@forEachIndexed
                    item(key = "e$bi-$ei-$id") {
                        ExerciseRow(ex) {
                            IconButton(onClick = { RandomWorkout.reroll(exercises, p, bi, ei)?.let { plan = it } }) {
                                Text("↻", fontSize = 20.sp, color = Palette.text)
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { onSave(p) }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Enregistrer") }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { onStart(p) },
                        colors = ButtonDefaults.buttonColors(containerColor = Palette.work),
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("▶  Lancer", fontWeight = FontWeight.Bold, color = Palette.onWork) }
                }
            }
        }
    }
}
