package fr.loic.seances.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import android.os.SystemClock
import kotlinx.coroutines.delay
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import fr.loic.seances.session.SessionState
import fr.loic.seances.session.formatMinutes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.loic.seances.anim.StickFigure
import fr.loic.seances.data.formatDuration
import fr.loic.seances.session.SessionViewModel
import fr.loic.seances.session.Step

@Composable
fun SessionScreen(
    vm: SessionViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    showSummary: Boolean = true,
    /** Ajoute la séance à l'historique et renvoie les records à afficher. */
    onRecord: ((fr.loic.seances.data.HistoryEntry) -> List<String>)? = null,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmExit by remember { mutableStateOf(false) }

    // Écran toujours allumé pendant la séance.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    BackHandler { if (state.finished) onExit() else confirmExit = true }

    if (state.finished) {
        FinishedContent(vm, state, showSummary, onExit, modifier, onRecord)
        return
    }

    val step = vm.steps[state.stepIndex]
    val color = when (step) {
        is Step.Work -> Palette.work
        is Step.Rest, is Step.NextPart -> Palette.rest
        is Step.Prepare, is Step.Transition -> Palette.prepare
    }
    val progress = if (step.durationSec > 0) state.remainingMs / (step.durationSec * 1000f) else 0f
    val repsStep = (step as? Step.Work)?.takeIf { it.isReps }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // En-tête : où on en est
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val part = vm.workoutAt(state.stepIndex)
                Text(
                    (if (vm.isChain) "⛓️ ${vm.partIndexAt(state.stepIndex) + 1}/${vm.parts.size} · " else "") +
                        part.title + (if (part.subtitle.isNotBlank()) " · " + part.subtitle else ""),
                    color = Palette.textMuted, fontSize = 13.sp,
                )
                Text(headerText(step, vm.cycles), color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            TextButton(onClick = { confirmExit = true }) { Text("Arrêter", color = Palette.textMuted) }
        }

        val dotsFor = (step as? Step.Work) ?: (step as? Step.Transition)?.next
        if (dotsFor != null) {
            Spacer(Modifier.height(6.dp))
            CycleDots(current = dotsFor.indexInCycle, blockSizes = dotsFor.blockSizes, color = color)
        }

        Spacer(Modifier.height(8.dp))

        val (title, exercise) = when (step) {
            is Step.Work -> step.exercise.name to step.exercise
            is Step.Rest -> "Repos" to step.next
            is Step.Prepare -> "Prépare-toi" to step.first
            is Step.Transition -> step.next.exercise.name to step.next.exercise
            is Step.NextPart -> step.title to step.first
        }
        Text(
            title,
            color = Palette.text,
            style = titleStyle(26).let { if (Palette.isArcade) it else it.copy(lineHeight = 30.sp) },
            textAlign = TextAlign.Center,
        )
        when (step) {
            is Step.Transition ->
                Text("Change de position, mets-toi en place", color = color, fontSize = 15.sp, textAlign = TextAlign.Center)
            !is Step.Work ->
                Text("Ensuite : ${exercise.name}", color = Palette.textMuted, fontSize = 15.sp, textAlign = TextAlign.Center)
            else -> {}
        }

        // Animation
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(themedShape(24.dp))
                .background(Palette.surface),
            contentAlignment = Alignment.Center,
        ) {
            StickFigure(
                animation = exercise.animation,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                playing = !state.paused,
                bodyColor = if (step is Step.Work) Palette.text else Palette.text.copy(alpha = 0.6f),
                accentColor = color,
            )
        }
        if (step is Step.Work || step is Step.Transition) {
            Text(exercise.cue, color = Palette.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(12.dp))

        // Chrono + contrôles
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            FilledTonalIconButton(onClick = vm::previous, modifier = Modifier.size(56.dp)) {
                Text("⏮", fontSize = 20.sp)
            }
            Spacer(Modifier.width(16.dp))
            if (repsStep != null) {
                RepsRing(
                    reps = repsStep.reps ?: 0,
                    sessionStartedAt = vm.startedAt,
                    color = color,
                    modifier = Modifier.size(150.dp),
                )
            } else {
                TimerRing(
                    remainingMs = state.remainingMs,
                    progress = progress,
                    color = color,
                    paused = state.paused,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .clickable(onClick = vm::togglePause),
                )
            }
            Spacer(Modifier.width(16.dp))
            FilledTonalIconButton(onClick = vm::skip, modifier = Modifier.size(56.dp)) {
                Text("⏭", fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        FilledIconButton(
            onClick = if (repsStep != null) vm::done else vm::togglePause,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = themedShape(16.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = color),
        ) {
            Text(
                when {
                    repsStep != null -> "✓  Fait"
                    state.paused -> "▶  Reprendre"
                    else -> "❚❚  Pause"
                },
                color = Palette.onWork,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }

        // Exercice suivant
        val next = vm.nextWorkAfter(state.stepIndex)
        if (step is Step.Work) {
            Spacer(Modifier.height(8.dp))
            val nextLabel = when {
                next == null -> "Dernier exercice !"
                else -> when (val following = vm.steps.getOrNull(state.stepIndex + 1)) {
                    is Step.Rest -> "Ensuite : ${formatDuration(following.durationSec)} de repos"
                    is Step.NextPart -> "Ensuite : pause, puis ${following.title}"
                    else -> "Ensuite : ${next.exercise.name}"
                }
            }
            Text(nextLabel, color = Palette.textMuted, fontSize = 14.sp)
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Arrêter la séance ?") },
            text = { Text("Si tu as déjà fait des exercices, tu verras le bilan de ce qui a été fait.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    if (!vm.endEarly()) onExit()
                }) { Text("Arrêter") }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Continuer") } },
        )
    }
}

private fun headerText(step: Step, cycles: Int): String = when (step) {
    is Step.Prepare -> "Départ dans quelques secondes"
    is Step.Rest -> "Cycle ${step.afterCycle}/$cycles terminé"
    is Step.NextPart -> "Séance ${step.partIndex}/${step.partCount} terminée · pause"
    is Step.Work -> "Cycle ${step.cycle}/$cycles · Bloc ${step.blockIndex + 1}/${step.blockCount} ${step.blockTitle} · " +
        "Exo ${step.exerciseIndex + 1}/${step.exercisesInBlock}"
    is Step.Transition -> "Exo suivant · Bloc ${step.next.blockIndex + 1}/${step.next.blockCount} ${step.next.blockTitle} · " +
        "Exo ${step.next.exerciseIndex + 1}/${step.next.exercisesInBlock}"
}

@Composable
private fun CycleDots(current: Int, blockSizes: List<Int>, color: Color) {
    val total = blockSizes.sum()
    val small = total > 14
    val dot = if (small) 7.dp else 12.dp
    val active = if (small) 14.dp else 22.dp
    val gap = if (small) 2.dp else 4.dp
    // Indices où commence un nouveau bloc (pour espacer les groupes)
    val blockStarts = blockSizes.runningFold(0) { acc, n -> acc + n }.drop(1).dropLast(1).toSet()
    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until total) {
            if (i in blockStarts) Spacer(Modifier.width(if (small) 4.dp else 8.dp))
            Box(
                Modifier
                    .size(width = if (i == current) active else dot, height = if (small) 6.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            i < current -> color.copy(alpha = 0.6f)
                            i == current -> color
                            else -> Palette.surfaceHigh
                        },
                    ),
            )
        }
    }
}

/** Mode Répétitions : nombre de reps à faire et, en dessous, le temps écoulé depuis le début de la séance. */
@Composable
private fun RepsRing(
    reps: Int,
    sessionStartedAt: Long,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var elapsedSec by remember { mutableStateOf(0L) }
    LaunchedEffect(sessionStartedAt) {
        while (true) {
            elapsedSec = (SystemClock.elapsedRealtime() - sessionStartedAt) / 1000
            delay(500)
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 12.dp.toPx()
            drawCircle(color, radius = (size.minDimension - w) / 2, style = Stroke(w))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("×$reps", style = titleStyle(if (reps >= 100) 40 else 52), color = Palette.text)
            Text(
                "⏱ " + if (elapsedSec >= 3600) "%d:%02d:%02d".format(elapsedSec / 3600, elapsedSec / 60 % 60, elapsedSec % 60)
                else "%d:%02d".format(elapsedSec / 60, elapsedSec % 60),
                color = Palette.textMuted, fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun TimerRing(
    remainingMs: Long,
    progress: Float,
    color: Color,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(progress, label = "ring")
    val seconds = ((remainingMs + 999) / 1000).toInt()
    val urgent = seconds <= 3 && !paused
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 12.dp.toPx()
            val inset = w / 2
            val arcSize = Size(size.width - w, size.height - w)
            drawArc(Palette.surfaceHigh, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
            drawArc(
                color, -90f, 360f * animated, false, Offset(inset, inset), arcSize,
                style = Stroke(w, cap = if (Palette.isArcade) StrokeCap.Butt else StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (seconds >= 60) "%d:%02d".format(seconds / 60, seconds % 60) else "$seconds",
                style = titleStyle(if (seconds >= 60) 40 else 56),
                color = if (urgent) color else Palette.text,
            )
            Text(
                if (paused) "en pause · toucher" else "toucher = pause",
                color = Palette.textMuted, fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun FinishedContent(
    vm: SessionViewModel,
    state: SessionState,
    showSummary: Boolean,
    onExit: () -> Unit,
    modifier: Modifier,
    onRecord: ((fr.loic.seances.data.HistoryEntry) -> List<String>)?,
) {
    val complete = state.exercisesDone >= vm.steps.count { it is Step.Work }
    val records = remember { onRecord?.let { vm.recordHistory(it) } ?: emptyList() }
    Box(modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!showSummary) Spacer(Modifier.weight(1f))
        Text(
            when {
                Palette.isArcade -> if (complete) "LEVEL COMPLETE !" else "GAME OVER"
                else -> if (complete) "Bravo !" else "Séance arrêtée"
            },
            style = titleStyle(40), textAlign = TextAlign.Center, color = Palette.accentText)
        Text(
            "${formatMinutes(state.totalMs)} au total · ${formatMinutes(state.workedMs)} d'effort · " +
                "${state.exercisesDone} exo${if (state.exercisesDone > 1) "s" else ""}",
            color = Palette.textMuted, fontSize = 15.sp, textAlign = TextAlign.Center,
        )
        if (records.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            records.forEachIndexed { i, r -> RecordChip(r, delayMs = 250 + i * 180) }
        }
        if (showSummary) SummaryBlock(vm, state) else Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onExit,
            colors = ButtonDefaults.buttonColors(containerColor = Palette.work),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) { Text("Retour à l'accueil", fontWeight = FontWeight.Bold, color = Palette.onWork) }
    }
    if (complete) Confetti(Modifier.fillMaxSize())
    }
}

/** Résumé à coller dans Strava, avec Copier / Partager. Désactivable dans les réglages. */
@Composable
private fun ColumnScope.SummaryBlock(vm: SessionViewModel, state: SessionState) {
    val context = LocalContext.current
    val summary = remember(state.exercisesDone, state.totalMs) { vm.summary() }
    Spacer(Modifier.height(12.dp))
    Text("Résumé à coller dans Strava", color = Palette.text, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(6.dp))
    SelectionContainer(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(themedShape(16.dp))
            .background(Palette.surface)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
    ) {
        Text(summary, color = Palette.text, fontSize = 14.sp, lineHeight = 19.sp)
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth()) {
        FilledTonalButton(
            onClick = {
                copyToClipboard(context, "Résumé de séance", summary)
                Toast.makeText(context, "Résumé copié, colle-le dans la description Strava", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.weight(1f).height(52.dp),
        ) { Text("Copier") }
        Spacer(Modifier.width(12.dp))
        FilledTonalButton(
            onClick = { shareText(context, summary, "Envoyer le résumé") },
            modifier = Modifier.weight(1f).height(52.dp),
        ) { Text("Partager…") }
    }
}
