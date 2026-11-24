package fr.loic.seances.data

// Séances d'exemple des tests (et de la web app, via WebExportTest). L'app Android démarre sans séance.

/** 3 séances d'exemple. */
object DefaultPlans {
    fun create(durations: Durations): List<WorkoutPlan> = listOf(
        WorkoutPlan(
            id = "jour1", title = "Jour 1", subtitle = "Épaules — Abdos — Jambes", durations = durations,
            blocks = listOf(
                BlockPlan("Épaules", listOf("arnold_press", "lateral_raise", "reverse_fly")),
                BlockPlan("Abdos", listOf("overhead_crunch", "russian_twist", "big_l")),
                BlockPlan("Jambes", listOf("dumbbell_squat", "lunge", "good_morning")),
            ),
        ),
        WorkoutPlan(
            id = "jour2", title = "Jour 2", subtitle = "Pecs — Bras — Abdos", durations = durations,
            blocks = listOf(
                BlockPlan("Pectoraux", listOf("floor_press", "dumbbell_fly", "push_up_diamond")),
                BlockPlan("Bras", listOf("alternate_curl", "skull_crusher", "hammer_curl")),
                BlockPlan("Abdos", listOf("leg_raise", "cross_crunch", "shoulder_taps")),
            ),
        ),
        WorkoutPlan(
            id = "jour3", title = "Jour 3", subtitle = "Jambes — Taille — Dos", durations = durations,
            blocks = listOf(
                BlockPlan("Jambes", listOf("jump_squat", "jump_lunge", "thruster")),
                BlockPlan("Taille", listOf("flutter_kicks", "hip_dips", "windshield_wipers")),
                BlockPlan("Dos", listOf("bent_over_row", "pullover", "dead_stop_row")),
            ),
        ),
    )
}
