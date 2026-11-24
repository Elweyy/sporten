package fr.loic.seances.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

val LibraryJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

/** Opérations pures sur les données (testables sans Android). */
object LibraryOps {

    fun exercisesById(data: LibraryData): Map<String, ExerciseDef> =
        (Catalog.builtIn + data.customExercises).associateBy { it.id }

    fun upsertWorkout(data: LibraryData, plan: WorkoutPlan): LibraryData {
        val i = data.workouts.indexOfFirst { it.id == plan.id }
        val list = if (i >= 0) data.workouts.toMutableList().also { it[i] = plan } else data.workouts + plan
        return data.copy(workouts = list)
    }

    fun upsertExercise(data: LibraryData, ex: ExerciseDef): LibraryData {
        val custom = ex.copy(custom = true)
        val i = data.customExercises.indexOfFirst { it.id == custom.id }
        val list = if (i >= 0) data.customExercises.toMutableList().also { it[i] = custom } else data.customExercises + custom
        return data.copy(customExercises = list)
    }

    /** Séances qui utilisent cet exercice. */
    fun usages(data: LibraryData, exerciseId: String): List<WorkoutPlan> =
        data.workouts.filter { w -> w.blocks.any { exerciseId in it.exerciseIds } }

    /**
     * Fusionne un import : les séances et exercices de même identifiant sont remplacés,
     * les autres ajoutés. Rien n'est supprimé.
     */
    fun merge(current: LibraryData, imported: LibraryData): LibraryData {
        var out = current
        imported.customExercises.forEach { out = upsertExercise(out, it) }
        imported.workouts.forEach { out = upsertWorkout(out, it) }
        if (imported.history.isNotEmpty()) out = out.copy(history = HistoryOps.merge(out.history, imported.history))
        return out
    }

    /**
     * Ajoute une séance reçue par code de partage, toujours comme nouvelle séance (on n'écrase
     * jamais une séance existante). Les exercices perso inconnus sont ajoutés ; ceux qu'on a déjà
     * (même identifiant) sont gardés tels quels.
     */
    fun addShared(data: LibraryData, payload: SharePayload): Pair<LibraryData, WorkoutPlan> {
        val known = exercisesById(data)
        var out = data
        payload.x.filter { it.id !in known }.forEach { out = upsertExercise(out, it) }
        val plan = payload.w.copy(id = newId("seance"))
        return upsertWorkout(out, plan) to plan
    }

    fun newId(prefix: String) = prefix + "_" + UUID.randomUUID().toString().take(8)
}

/**
 * Séances et exercices perso, sauvegardés dans un fichier JSON sur le téléphone.
 * Au premier lancement, la bibliothèque est vide : on crée ses séances, on en tire au sort ou on en importe.
 */
class Library(context: Context, defaultDurations: Durations) {

    private val file = File(context.filesDir, "library.json")

    private val _data = MutableStateFlow(load(defaultDurations))
    val data: StateFlow<LibraryData> = _data.asStateFlow()

    val exercisesById: Map<String, ExerciseDef> get() = LibraryOps.exercisesById(_data.value)

    private fun load(defaultDurations: Durations): LibraryData {
        val loaded = runCatching { LibraryJson.decodeFromString<LibraryData>(file.readText()) }.getOrNull()
        return loaded ?: LibraryData().also { write(it) }
    }

    private fun update(transform: (LibraryData) -> LibraryData) {
        val next = transform(_data.value)
        _data.value = next
        write(next)
    }

    private fun write(d: LibraryData) {
        // Écriture atomique : fichier temporaire puis renommage.
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(LibraryJson.encodeToString(LibraryData.serializer(), d))
        tmp.renameTo(file)
    }

    fun workout(id: String): WorkoutPlan? = _data.value.workouts.firstOrNull { it.id == id }

    fun saveWorkout(plan: WorkoutPlan) = update { LibraryOps.upsertWorkout(it, plan) }

    fun deleteWorkout(id: String) = update { d -> d.copy(workouts = d.workouts.filterNot { it.id == id }) }

    fun duplicateWorkout(id: String) {
        val src = workout(id) ?: return
        val copy = src.copy(id = LibraryOps.newId("seance"), title = src.title + " (copie)")
        update { d ->
            val i = d.workouts.indexOfFirst { it.id == id }
            d.copy(workouts = d.workouts.toMutableList().also { it.add(i + 1, copy) })
        }
    }

    fun moveWorkout(id: String, delta: Int) = update { d ->
        val list = d.workouts.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in list.indices) d else d.copy(workouts = list.also { it.add(j, it.removeAt(i)) })
    }

    fun saveExercise(ex: ExerciseDef) = update { LibraryOps.upsertExercise(it, ex) }

    /** Supprime un exercice perso, seulement s'il n'est utilisé dans aucune séance. */
    fun deleteExercise(id: String): Boolean {
        if (LibraryOps.usages(_data.value, id).isNotEmpty()) return false
        update { d -> d.copy(customExercises = d.customExercises.filterNot { it.id == id }) }
        return true
    }

    fun shareCode(plan: WorkoutPlan): String = ShareCode.encode(plan, exercisesById)

    /** Ajoute une séance reçue et la renvoie. */
    fun addShared(payload: SharePayload): WorkoutPlan {
        var added: WorkoutPlan? = null
        update { d -> LibraryOps.addShared(d, payload).also { added = it.second }.first }
        return added!!
    }

    /** Ajoute une séance faite à l'historique ; renvoie les records (calculés avant l'ajout). */
    fun addHistory(entry: HistoryEntry): List<String> {
        val records = HistoryOps.records(_data.value.history, entry)
        update { it.copy(history = it.history + entry) }
        return records
    }

    fun deleteHistory(id: String) = update { d -> d.copy(history = d.history.filterNot { it.id == id }) }

    /** Ajoute une séance (tirée au sort) en fin de liste. */
    fun addWorkout(plan: WorkoutPlan) = update { it.copy(workouts = it.workouts + plan) }

    fun exportJson(): String = LibraryJson.encodeToString(LibraryData.serializer(), _data.value)

    /** Importe un fichier exporté. Renvoie le nombre de séances, d'exercices et d'entrées d'historique importés. */
    fun importJson(text: String): Result<Triple<Int, Int, Int>> = runCatching {
        val imported = LibraryJson.decodeFromString<LibraryData>(text)
        update { LibraryOps.merge(it, imported) }
        Triple(imported.workouts.size, imported.customExercises.size, imported.history.size)
    }
}
