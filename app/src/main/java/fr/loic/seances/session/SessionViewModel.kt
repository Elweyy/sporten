package fr.loic.seances.session

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.loic.seances.data.Durations
import fr.loic.seances.data.Exercise
import fr.loic.seances.data.HistoryEntry
import fr.loic.seances.data.LibraryOps
import fr.loic.seances.data.Workout
import fr.loic.seances.data.WorkoutMode
import fr.loic.seances.ui.Palette
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface Step {
    val durationSec: Int

    /** Compte à rebours avant le premier exercice. */
    data class Prepare(val first: Exercise, override val durationSec: Int) : Step

    data class Work(
        val exercise: Exercise,
        val cycle: Int,          // 1-based
        val blockIndex: Int,     // 0-based
        val blockTitle: String,
        val exerciseIndex: Int,  // 0-based dans le bloc
        val indexInCycle: Int,   // 0-based dans le cycle
        val exercisesInBlock: Int,
        val blockCount: Int,
        /** Nombre d'exercices de chaque bloc (pour la barre de progression). */
        val blockSizes: List<Int>,
        override val durationSec: Int,
        /** Mode Répétitions : nombre de reps à faire, pas de compte à rebours (null en mode Chrono). */
        val reps: Int? = null,
    ) : Step {
        val isReps: Boolean get() = reps != null
    }

    data class Rest(val afterCycle: Int, val next: Exercise, override val durationSec: Int) : Step

    /** Courte pause entre deux exercices : on change de position, on découvre l'exo suivant. */
    data class Transition(val next: Work, override val durationSec: Int) : Step

    /**
     * Super séance : pause entre deux séances. [partIndex] est la séance qui suit (0-based),
     * [title] son nom et [first] son premier exercice.
     */
    data class NextPart(
        val partIndex: Int,
        val partCount: Int,
        val title: String,
        val first: Exercise,
        override val durationSec: Int,
    ) : Step
}

/** Une séance d'une super séance (ou la seule séance d'une séance normale), avec ses durées. */
data class SessionPart(val workout: Workout, val durations: Durations)

/** Étapes d'une suite de séances et, pour chaque étape, l'indice de sa séance. */
data class ChainSteps(val steps: List<Step>, val partOf: List<Int>)

/** Pause entre deux séances : le plus long du repos entre cycles de la première et de la préparation de la suivante. */
fun chainBreakSec(prev: Durations, next: Durations): Int = maxOf(prev.restBetweenCycles, next.prepare)

/**
 * Enchaîne plusieurs séances (super séance). Chacune garde son mode et ses durées ; la préparation
 * des séances suivantes est remplacée par une pause [Step.NextPart]. Les séances vides sont ignorées.
 */
fun buildChainSteps(parts: List<SessionPart>, cycles: Int): ChainSteps {
    val steps = mutableListOf<Step>()
    val partOf = mutableListOf<Int>()
    parts.forEachIndexed { p, part ->
        var own = buildSteps(part.workout, cycles, part.durations)
        if (own.isEmpty()) return@forEachIndexed
        if (steps.isNotEmpty()) {
            if (own.first() is Step.Prepare) own = own.drop(1)
            val first = part.workout.blocks.first().exercises.first()
            val prev = parts[partOf.last()].durations
            val sec = chainBreakSec(prev, part.durations)
            if (sec > 0) {
                steps += Step.NextPart(p, parts.size, part.workout.title, first, sec)
                partOf += p
            }
        }
        steps += own
        own.forEach { _ -> partOf += p }
    }
    return ChainSteps(steps, partOf)
}

data class SessionState(
    val stepIndex: Int = 0,
    val remainingMs: Long = 0,
    val paused: Boolean = false,
    val finished: Boolean = false,
    /** Temps d'effort réellement fait (exercices passés compris en partie). */
    val workedMs: Long = 0,
    /** Durée totale de la séance, pauses comprises (renseignée à la fin). */
    val totalMs: Long = 0,
    /** Nombre d'exercices faits (au moins en partie). */
    val exercisesDone: Int = 0,
)

/**
 * Les étapes d'une séance. Une transition sépare deux exercices qui se suivent (pas après un repos).
 * Une durée de préparation, de repos ou de transition à 0 supprime l'étape.
 * En mode Répétitions, les exercices n'ont pas de durée (0) mais un nombre de reps.
 */
fun buildSteps(workout: Workout, cycles: Int, durations: Durations = Durations()): List<Step> {
    val steps = mutableListOf<Step>()
    val first = workout.blocks.firstOrNull()?.exercises?.firstOrNull() ?: return emptyList()
    val sizes = workout.blocks.map { it.exercises.size }
    if (durations.prepare > 0) steps += Step.Prepare(first, durations.prepare)
    for (c in 1..cycles) {
        var i = 0
        workout.blocks.forEachIndexed { bi, block ->
            block.exercises.forEachIndexed { ei, ex ->
                val reps = if (workout.mode == WorkoutMode.REPS) block.repsAt(ei) else null
                val work = Step.Work(
                    ex, c, bi, block.title, ei, i++,
                    exercisesInBlock = block.exercises.size,
                    blockCount = workout.blocks.size,
                    blockSizes = sizes,
                    durationSec = if (reps != null) 0 else durations.work,
                    reps = reps,
                )
                if (steps.lastOrNull() is Step.Work && durations.transition > 0) {
                    steps += Step.Transition(work, durations.transition)
                }
                steps += work
            }
        }
        if (c < cycles && durations.restBetweenCycles > 0) {
            steps += Step.Rest(c, first, durations.restBetweenCycles)
        }
    }
    return steps
}

class SessionViewModel(
    app: Application,
    /** Les séances à enchaîner : une seule pour une séance normale, plusieurs pour une super séance. */
    val parts: List<SessionPart>,
    val cycles: Int,
    /** Annonces vocales (exercice suivant, repos, 10 dernières secondes). */
    private val voice: Boolean = false,
) : AndroidViewModel(app) {

    private val chain = buildChainSteps(parts, cycles)
    val steps: List<Step> = chain.steps

    /** Super séance : plusieurs séances enchaînées. */
    val isChain: Boolean get() = parts.size > 1

    /** Séance à laquelle appartient l'étape [index]. */
    fun partIndexAt(index: Int): Int = chain.partOf[index]
    fun workoutAt(index: Int): Workout = parts[chain.partOf[index]].workout

    private val _state = MutableStateFlow(SessionState(remainingMs = steps[0].durationSec * 1000L))
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val tone: ToneGenerator? = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }.getOrNull()
    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= 31) {
            app.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Vibrator::class.java)
        }
    }.getOrNull()

    /** Instant (elapsedRealtime) où l'étape en cours se termine, si non en pause. */
    private var stepEndAt = 0L
    private var ticker: Job? = null
    private var lastBeepSecond = -1
    private var stopped = false
    /** Début de la séance (elapsedRealtime), pour le chrono de séance du mode Répétitions. */
    val startedAt = SystemClock.elapsedRealtime()
    /** Début de l'étape en cours (elapsedRealtime) : sert au temps passé sur un exo en mode Répétitions. */
    private var stepStartedAt = startedAt
    private val startedAtWall = System.currentTimeMillis()
    private val speaker: Speaker? = if (voice) runCatching { Speaker(app) }.getOrNull() else null
    private var saidTen = -1
    /** Étapes d'exercice déjà comptées dans le résumé. */
    private val countedWork = mutableSetOf<Int>()

    init {
        ActiveSession.onTogglePause = { viewModelScope.launch { togglePause() } }
        startStep(0)
        WorkoutService.start(app)
    }

    val currentStep: Step get() = steps[_state.value.stepIndex]

    fun nextWorkAfter(index: Int): Step.Work? =
        steps.drop(index + 1).firstOrNull { it is Step.Work } as? Step.Work

    /** Exercice en mode Répétitions : pas de chrono, on attend « Fait ». */
    val waitingForDone: Boolean get() = (currentStep as? Step.Work)?.isReps == true

    fun togglePause() {
        val s = _state.value
        if (s.finished || stopped || waitingForDone) return
        if (s.paused) {
            stepEndAt = SystemClock.elapsedRealtime() + s.remainingMs
            _state.update { it.copy(paused = false) }
            runTicker()
        } else {
            ticker?.cancel()
            _state.update { it.copy(paused = true, remainingMs = (stepEndAt - SystemClock.elapsedRealtime()).coerceAtLeast(0)) }
        }
        publish()
    }

    fun skip() {
        recordWork()
        advance()
    }

    /** Mode Répétitions : l'exercice est fait, il compte dans le bilan, on passe à la suite. */
    fun done() {
        recordWork(repsDone = true)
        advance()
    }

    private fun advance() {
        val i = _state.value.stepIndex
        if (i + 1 < steps.size) startStep(i + 1) else finish()
    }

    fun previous() {
        val s = _state.value
        val elapsed = if (waitingForDone) SystemClock.elapsedRealtime() - stepStartedAt
        else currentStep.durationSec * 1000L - s.remainingMs
        // Si l'étape vient de commencer, on revient à la précédente, sinon on la redémarre.
        val target = if (elapsed < 3000 && s.stepIndex > 0) s.stepIndex - 1 else s.stepIndex
        if (target != s.stepIndex) recordWork()
        startStep(target)
    }

    private fun startStep(index: Int) {
        ticker?.cancel()
        val step = steps[index]
        val isReps = (step as? Step.Work)?.isReps == true
        // Un exo en répétitions n'a pas de chrono à mettre en pause : on sort de la pause.
        val wasPaused = _state.value.paused && !isReps
        val durationMs = step.durationSec * 1000L
        stepStartedAt = SystemClock.elapsedRealtime()
        stepEndAt = stepStartedAt + durationMs
        lastBeepSecond = -1
        _state.update { it.copy(stepIndex = index, remainingMs = durationMs, finished = false, paused = wasPaused) }
        if (index > 0) signalStart()
        speaker?.say(Announcements.forStep(steps, index, cycles))
        if (!wasPaused && !isReps) runTicker()
        publish()
    }

    private fun runTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                val remaining = stepEndAt - SystemClock.elapsedRealtime()
                if (remaining <= 0) {
                    _state.update { it.copy(remainingMs = 0) }
                    recordWork()
                    val next = _state.value.stepIndex + 1
                    if (next < steps.size) startStep(next) else finish()
                    return@launch
                }
                _state.update { it.copy(remainingMs = remaining) }
                val sec = ((remaining + 999) / 1000).toInt()
                val step = currentStep
                if (speaker != null && step is Step.Work && step.durationSec >= 30 && sec == 10 && saidTen != _state.value.stepIndex) {
                    saidTen = _state.value.stepIndex
                    speaker.say(Announcements.TEN_SECONDS)
                }
                if (sec in 1..3 && sec != lastBeepSecond) {
                    lastBeepSecond = sec
                    tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                }
                delay(50)
            }
        }
    }

    /**
     * Ajoute au résumé le temps passé sur l'exercice en cours (une seule fois par étape).
     * Mode Chrono : un exercice passé au bout de moins de 5 s ne compte pas.
     * Mode Répétitions : l'exercice ne compte que s'il a été validé par « Fait » ([repsDone]).
     */
    private fun recordWork(repsDone: Boolean = false) {
        val s = _state.value
        val step = currentStep as? Step.Work ?: return
        if (s.stepIndex in countedWork) return
        val done = if (step.isReps) {
            if (!repsDone) return
            SystemClock.elapsedRealtime() - stepStartedAt
        } else {
            (step.durationSec * 1000L - s.remainingMs.coerceAtLeast(0)).also { if (it < 5000) return }
        }
        countedWork += s.stepIndex
        _state.update { it.copy(workedMs = it.workedMs + done, exercisesDone = it.exercisesDone + 1) }
    }

    /** Met à jour la notification du service. */
    private fun publish() {
        if (stopped) return
        val s = _state.value
        val step = currentStep
        val (title, accent) = when (step) {
            is Step.Prepare -> "Prépare-toi" to Palette.prepare
            is Step.Rest -> "Repos" to Palette.rest
            is Step.Work -> step.exercise.name to Palette.work
            is Step.Transition -> "En place : ${step.next.exercise.name}" to Palette.prepare
            is Step.NextPart -> "Pause · ensuite ${step.title}" to Palette.rest
        }
        val text = when (step) {
            is Step.Prepare -> "Ensuite : ${step.first.name}"
            is Step.Rest -> "Cycle ${step.afterCycle}/$cycles terminé · ensuite : ${step.next.name}"
            is Step.Work -> (if (step.isReps) "${step.reps} reps · " else "") +
                "Cycle ${step.cycle}/$cycles · ${step.blockTitle} · exo ${step.exerciseIndex + 1}/${step.exercisesInBlock}"
            is Step.Transition -> "Changement d'exercice · ${step.next.blockTitle}"
            is Step.NextPart -> "Séance ${step.partIndex}/${step.partCount} terminée · ensuite : ${step.first.name}"
        }
        val remainingSec = ((s.remainingMs + 999) / 1000).toInt()
        ActiveSession.status.value = ActiveSession.Status(
            title = if (s.paused) "⏸ $title" else title,
            text = if (s.paused) "En pause · reste ${remainingSec} s · $text" else text,
            endsAtWallMs = if (s.paused || waitingForDone) null else System.currentTimeMillis() + s.remainingMs,
            paused = s.paused,
            accent = accent.toArgb(),
        )
    }

    private fun signalStart() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 450)
        runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE)) }
    }

    private fun finish() {
        ticker?.cancel()
        _state.update {
            it.copy(finished = true, remainingMs = 0, paused = false, totalMs = SystemClock.elapsedRealtime() - startedAt)
        }
        tone?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 800)
        runCatching { vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 120, 200, 120, 400), -1)) }
        viewModelScope.launch {
            delay(900)
            speaker?.say(Announcements.FINISHED)
        }
        releaseService()
        stopped = true
    }

    /**
     * Arrêt avant la fin : si au moins un exercice a été fait, on passe à l'écran de résumé
     * et on renvoie true. Sinon rien n'est fait et on renvoie false.
     */
    fun endEarly(): Boolean {
        if (stopped) return false
        recordWork()
        if (_state.value.exercisesDone == 0) return false
        ticker?.cancel()
        _state.update {
            it.copy(finished = true, remainingMs = 0, paused = false, totalMs = SystemClock.elapsedRealtime() - startedAt)
        }
        releaseService()
        stopped = true
        return true
    }

    private var recorded: List<String>? = null

    /**
     * Enregistre la séance dans l'historique une seule fois (même si l'écran de fin est recréé,
     * par exemple en tournant le téléphone) et renvoie les records.
     */
    fun recordHistory(add: (HistoryEntry) -> List<String>): List<String> =
        recorded ?: add(historyEntry()).also { recorded = it }

    /** Entrée d'historique de la séance terminée (ou arrêtée après au moins un exercice). */
    fun historyEntry(): HistoryEntry {
        val s = _state.value
        val planned = steps.count { it is Step.Work }
        return HistoryEntry(
            id = LibraryOps.newId("h"),
            date = startedAtWall,
            durationMs = s.totalMs,
            workedMs = s.workedMs,
            exercisesDone = s.exercisesDone,
            exercisesPlanned = planned,
            title = if (isChain) SUPER_TITLE else parts[0].workout.title,
            subtitle = if (isChain) parts.joinToString(" + ") { it.workout.title } else parts[0].workout.subtitle,
            cycles = cycles,
            complete = s.exercisesDone >= planned,
            muscles = countedWork.mapNotNull { (steps[it] as? Step.Work)?.exercise?.muscle?.name }
                .groupingBy { it }.eachCount(),
        )
    }

    /** Texte de résumé de la séance (à appeler une fois la séance terminée). */
    fun summary(): String {
        val done = IntArray(parts.size)
        countedWork.forEach { done[chain.partOf[it]]++ }
        return buildChainSummary(parts, cycles, done.toList())
    }

    /** Arrête définitivement la séance (plus de chrono, de bips ni de notification). */
    fun stop() {
        // La voix est libérée en quittant l'écran, même après une fin normale (l'objet vit jusqu'à la fin de l'activité).
        speaker?.release()
        if (stopped) return
        ticker?.cancel()
        _state.update { it.copy(paused = true) }
        releaseService()
        stopped = true
    }

    private fun releaseService() {
        ActiveSession.onTogglePause = null
        ActiveSession.status.value = null
    }

    companion object {
        const val SUPER_TITLE = "Super séance"
    }

    override fun onCleared() {
        ticker?.cancel()
        if (!stopped) releaseService()
        tone?.release()
        speaker?.release()
    }
}
