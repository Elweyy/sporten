package fr.loic.seances.session

import fr.loic.seances.data.Durations
import fr.loic.seances.data.Workout
import fr.loic.seances.data.WorkoutMode
import fr.loic.seances.data.formatDuration

private val DEFAULT_BLOCK_TITLE = Regex("""^\s*bloc\s*\d*\s*$""", RegexOption.IGNORE_CASE)

/** Durée lisible à la minute près : 34 min, 1 h 05. */
fun formatMinutes(ms: Long): String {
    val min = ((ms + 30_000) / 60_000).toInt()
    return if (min < 60) "$min min" else "${min / 60} h %02d".format(min % 60)
}

/**
 * Résumé de fin de séance, prêt à coller dans la description d'une activité Strava.
 * Même présentation que les séances écrites à la main : « Bloc 1 : Épaules » puis un exo par ligne.
 */
fun buildSummary(
    workout: Workout,
    cycles: Int,
    durations: Durations,
    exercisesDone: Int,
): String {
    val perCycle = workout.blocks.sumOf { it.exercises.size }
    val planned = perCycle * cycles
    val fullCycles = if (perCycle > 0) exercisesDone / perCycle else 0
    val extra = if (perCycle > 0) exercisesDone % perCycle else 0

    val done = when {
        exercisesDone >= planned -> "$cycles cycle${s(cycles)}"
        extra == 0 -> "$fullCycles cycle${s(fullCycles)} sur $cycles"
        fullCycles == 0 -> "$extra exo${s(extra)} sur $planned"
        else -> "$fullCycles cycle${s(fullCycles)} + $extra exo${s(extra)} (sur $cycles cycles)"
    }

    val reps = workout.mode == WorkoutMode.REPS
    return buildString {
        append("🏋️ ").append(workout.title)
        if (workout.subtitle.isNotBlank()) append(" : ").append(workout.subtitle)
        append("\n")
        append("$done · $perCycle exos par cycle")
        if (!reps) append(" · ${formatDuration(durations.work)} par exo, max de reps")
        if (cycles > 1 && durations.restBetweenCycles > 0) {
            append(" · ${formatDuration(durations.restBetweenCycles)} de repos entre les cycles")
        }
        append("\n")
        workout.blocks.forEachIndexed { i, block ->
            append("\nBloc ${i + 1}")
            if (!DEFAULT_BLOCK_TITLE.matches(block.title)) append(" : ").append(block.title.trim())
            append("\n")
            block.exercises.forEachIndexed { ei, ex ->
                append("- ").append(ex.name)
                if (reps) append(" × ").append(block.repsAt(ei))
                append("\n")
            }
        }
    }.trimEnd()
}

/**
 * Résumé d'une super séance : une ligne d'en-tête puis le résumé de chaque séance faite au moins en partie.
 * [donePerPart] : exercices faits dans chaque séance. Une seule séance = résumé habituel.
 */
fun buildChainSummary(parts: List<SessionPart>, cycles: Int, donePerPart: List<Int>): String {
    if (parts.size == 1) return buildSummary(parts[0].workout, cycles, parts[0].durations, donePerPart[0])
    val shown = parts.indices.filter { donePerPart[it] > 0 }
    return buildString {
        append("⛓️ Super séance : ").append(parts.joinToString(" + ") { it.workout.title })
        shown.forEach { i ->
            append("\n\n").append(buildSummary(parts[i].workout, cycles, parts[i].durations, donePerPart[i]))
        }
    }
}

private fun s(n: Int) = if (n > 1) "s" else ""
