package fr.loic.seances.data

import android.content.Context
import fr.loic.seances.anim.Anims
import fr.loic.seances.anim.ExerciseAnimation
import kotlinx.serialization.Serializable

// ---------- Ce qui est sauvegardé ----------

@Serializable
enum class Muscle(val label: String) {
    EPAULES("Épaules"),
    PECTORAUX("Pectoraux"),
    BRAS("Bras"),
    DOS("Dos"),
    JAMBES("Jambes / fessiers"),
    ABDOS("Abdos / gainage"),
    CARDIO("Cardio"),
}

/** Un exercice du catalogue. Les exercices perso ont [custom] = true. */
@Serializable
data class ExerciseDef(
    val id: String,
    val name: String,
    val cue: String,
    val muscle: Muscle,
    val animId: String,
    val custom: Boolean = false,
)

/**
 * Façon de faire les exercices d'une séance.
 * [TIMED] « Chrono » : max de répétitions pendant une durée fixe, le chrono passe à l'exo suivant.
 * [REPS] « Répétitions » : un nombre de répétitions par exo, on appuie sur « Fait » quand c'est fini.
 */
@Serializable
enum class WorkoutMode(val label: String) {
    TIMED("Chrono"),
    REPS("Répétitions"),
}

@Serializable
data class BlockPlan(
    val title: String,
    val exerciseIds: List<String> = emptyList(),
    /** Répétitions de chaque exercice (même ordre que [exerciseIds]), utilisées en mode [WorkoutMode.REPS]. */
    val reps: List<Int> = emptyList(),
) {
    fun repsAt(i: Int): Int = reps.getOrNull(i) ?: Reps.DEFAULT

    /** Une valeur de répétitions par exercice, pour pouvoir déplacer / supprimer les deux listes ensemble. */
    fun withAlignedReps(): BlockPlan = copy(reps = List(exerciseIds.size) { repsAt(it) })

    /** Retire les répétitions si elles sont toutes à la valeur par défaut (fichier et codes de partage plus courts). */
    fun compactReps(): BlockPlan =
        if (exerciseIds.indices.all { repsAt(it) == Reps.DEFAULT }) copy(reps = emptyList()) else withAlignedReps()
}

@Serializable
data class WorkoutPlan(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val blocks: List<BlockPlan> = emptyList(),
    val durations: Durations = Durations(),
    val mode: WorkoutMode = WorkoutMode.TIMED,
) {
    val exerciseCount: Int get() = blocks.sumOf { it.exerciseIds.size }

    /** Total des répétitions d'un cycle (mode Répétitions). */
    val repsPerCycle: Int get() = blocks.sumOf { b -> b.exerciseIds.indices.sumOf { b.repsAt(it) } }
}

object Reps {
    const val DEFAULT = 10
    val RANGE = 1..100
}

/** Tout ce que l'utilisateur a créé : séances, exercices perso et historique des séances faites. */
@Serializable
data class LibraryData(
    val version: Int = 1,
    val workouts: List<WorkoutPlan> = emptyList(),
    val customExercises: List<ExerciseDef> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
)

/** Durées en secondes. */
@Serializable
data class Durations(
    val prepare: Int = 10,
    val work: Int = 60,
    val restBetweenCycles: Int = 60,
    /** Pause entre deux exercices pour changer de position. */
    val transition: Int = 5,
) {
    /** Nombre de transitions entre exercices (aussi entre deux cycles s'il n'y a pas de repos). */
    fun transitionCount(cycles: Int, exercisesPerCycle: Int): Int {
        if (transition <= 0 || exercisesPerCycle <= 0) return 0
        return cycles * (exercisesPerCycle - 1) + if (restBetweenCycles <= 0) cycles - 1 else 0
    }

    /** Durée totale d'une séance, en secondes. */
    fun totalSec(cycles: Int, exercisesPerCycle: Int = 9): Int =
        prepare + cycles * exercisesPerCycle * work + (cycles - 1) * restBetweenCycles +
            transitionCount(cycles, exercisesPerCycle) * transition

    companion object {
        val WORK_RANGE = 15..180
        const val WORK_STEP = 5
        val REST_RANGE = 0..300
        const val REST_STEP = 15
        val PREPARE_RANGE = 0..30
        const val PREPARE_STEP = 5
        val TRANSITION_RANGE = 0..30
        const val TRANSITION_STEP = 1
    }
}

// ---------- Ce qu'utilise une séance en cours ----------

data class Exercise(
    val name: String,
    /** Consigne courte affichée sous l'animation. */
    val cue: String,
    val animation: ExerciseAnimation,
    /** Groupe musculaire (pour les statistiques par muscle). */
    val muscle: Muscle? = null,
)

data class Block(
    val title: String,
    val exercises: List<Exercise>,
    /** Répétitions de chaque exercice (mode Répétitions) ; vide = valeur par défaut. */
    val reps: List<Int> = emptyList(),
) {
    fun repsAt(i: Int): Int = reps.getOrNull(i) ?: Reps.DEFAULT
}

data class Workout(
    val title: String,
    val subtitle: String,
    val blocks: List<Block>,
    val mode: WorkoutMode = WorkoutMode.TIMED,
)

fun ExerciseDef.toExercise() = Exercise(name, cue, Anims.get(animId), muscle)

/**
 * Transforme une séance sauvegardée en séance jouable. Les exercices introuvables
 * et les blocs vides sont ignorés.
 */
fun WorkoutPlan.resolve(exercises: Map<String, ExerciseDef>): Workout = Workout(
    title = title,
    subtitle = subtitle,
    blocks = blocks.mapNotNull { b ->
        val found = b.exerciseIds.indices.mapNotNull { i -> exercises[b.exerciseIds[i]]?.let { it.toExercise() to b.repsAt(i) } }
        if (found.isEmpty()) null else Block(b.title, found.map { it.first }, found.map { it.second })
    },
    mode = mode,
)

object Timings {
    const val DEFAULT_CYCLES = 3
    const val MAX_CYCLES = 10
}

/** Réglages mémorisés sur le téléphone. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val defaults = Durations()

    var cycles: Int
        get() = prefs.getInt("cycles", Timings.DEFAULT_CYCLES)
        set(v) = prefs.edit().putInt("cycles", v).apply()

    /** Dernière version dont les nouveautés ont été montrées (fenêtre « Quoi de neuf »). */
    var lastSeenVersion: String?
        get() = prefs.getString("lastSeenVersion", null)
        set(v) = prefs.edit().putString("lastSeenVersion", v).apply()

    /** Résumé à copier (Strava) sur l'écran de fin de séance. Activé par défaut. */
    var summary: Boolean
        get() = prefs.getBoolean("summary", true)
        set(v) = prefs.edit().putBoolean("summary", v).apply()

    /** Annonces vocales pendant la séance (exercice suivant, repos, 10 dernières secondes). Désactivées par défaut. */
    var voice: Boolean
        get() = prefs.getBoolean("voice", false)
        set(v) = prefs.edit().putBoolean("voice", v).apply()

    /** Thème d'affichage (nom d'un AppThemeId : SPORTEN, SYSTEM, LIGHT, OLED, NEON, ARCADE, PASTEL). */
    var theme: String
        get() = prefs.getString("theme", null) ?: "SPORTEN"
        set(v) = prefs.edit().putString("theme", v).apply()

    /** Dernier choix de l'écran « séance aléatoire ». */
    var randomMuscles: List<Muscle>
        get() = (prefs.getString("randomMuscles", null) ?: "JAMBES,ABDOS")
            .split(',').mapNotNull { n -> Muscle.entries.firstOrNull { it.name == n } }
        set(v) = prefs.edit().putString("randomMuscles", v.joinToString(",") { it.name }).apply()

    var randomPerBlock: Int
        get() = prefs.getInt("randomPerBlock", 3)
        set(v) = prefs.edit().putInt("randomPerBlock", v).apply()

    /** Durées proposées pour une nouvelle séance. */
    var durations: Durations
        get() = Durations(
            prepare = prefs.getInt("prepare", defaults.prepare),
            work = prefs.getInt("work", defaults.work),
            restBetweenCycles = prefs.getInt("rest", defaults.restBetweenCycles),
            transition = prefs.getInt("transition", defaults.transition),
        )
        set(d) = prefs.edit()
            .putInt("prepare", d.prepare)
            .putInt("work", d.work)
            .putInt("rest", d.restBetweenCycles)
            .putInt("transition", d.transition)
            .apply()
}

/** Formate une durée : 45 → "45 s", 90 → "1 min 30", 60 → "1 min". */
fun formatDuration(sec: Int): String = when {
    sec < 60 -> "$sec s"
    sec % 60 == 0 -> "${sec / 60} min"
    else -> "${sec / 60} min %02d".format(sec % 60)
}
