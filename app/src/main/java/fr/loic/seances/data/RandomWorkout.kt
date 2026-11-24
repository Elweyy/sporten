package fr.loic.seances.data

import kotlin.random.Random

/** Séance aléatoire : un bloc par groupe musculaire choisi. Mêmes règles que web/js/random.js. */
object RandomWorkout {
    const val MIN_PER_BLOCK = 1
    const val MAX_PER_BLOCK = 6

    /** « Jambes / fessiers » → « Jambes ». */
    fun shortLabel(m: Muscle) = m.label.substringBefore(" /")

    /** Si un groupe a moins d'exercices que demandé, on les prend tous. */
    fun generate(
        exercises: List<ExerciseDef>,
        muscles: List<Muscle>,
        perBlock: Int,
        durations: Durations,
        random: Random = Random.Default,
    ): WorkoutPlan {
        val blocks = muscles.sortedBy { it.ordinal }.mapNotNull { m ->
            val ids = exercises.filter { it.muscle == m }.shuffled(random).take(perBlock).map { it.id }
            if (ids.isEmpty()) null else BlockPlan(shortLabel(m), ids)
        }
        return WorkoutPlan(
            id = LibraryOps.newId("seance"),
            title = "Séance aléatoire",
            subtitle = blocks.joinToString(" — ") { it.title },
            blocks = blocks,
            durations = durations,
        )
    }

    /** Remplace un exercice par un autre du même groupe, absent du bloc. Null s'il n'y en a pas. */
    fun reroll(
        exercises: List<ExerciseDef>,
        plan: WorkoutPlan,
        blockIndex: Int,
        exerciseIndex: Int,
        random: Random = Random.Default,
    ): WorkoutPlan? {
        val block = plan.blocks[blockIndex]
        val muscle = exercises.firstOrNull { it.id == block.exerciseIds[exerciseIndex] }?.muscle ?: return null
        val pool = exercises.filter { it.muscle == muscle && it.id !in block.exerciseIds }
        if (pool.isEmpty()) return null
        val pick = pool.random(random).id
        val newBlock = block.copy(exerciseIds = block.exerciseIds.toMutableList().also { it[exerciseIndex] = pick })
        return plan.copy(blocks = plan.blocks.toMutableList().also { it[blockIndex] = newBlock })
    }
}
