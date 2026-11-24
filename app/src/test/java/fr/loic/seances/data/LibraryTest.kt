package fr.loic.seances.data

import fr.loic.seances.anim.Anims
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryTest {
    @Test
    fun catalogIdsAreUniqueAndAnimationsExist() {
        val ids = Catalog.builtIn.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        Catalog.builtIn.forEach { assertTrue("animation manquante : ${it.animId}", Anims.has(it.animId)) }
        val animIds = Anims.entries.map { it.id }
        assertEquals(animIds.size, animIds.toSet().size)
    }

    @Test
    fun defaultPlansOnlyUseCatalogExercisesInThreeByThree() {
        val ids = Catalog.builtIn.map { it.id }.toSet()
        DefaultPlans.create(Durations()).forEach { w ->
            assertEquals(3, w.blocks.size)
            w.blocks.forEach { b ->
                assertEquals(3, b.exerciseIds.size)
                b.exerciseIds.forEach { assertTrue("$it absent du catalogue", it in ids) }
            }
        }
    }

    @Test
    fun oldFilesWithoutModeAreTimedAndRepsCompact() {
        val old = """{"workouts":[{"id":"a","title":"Vieille","blocks":[{"title":"B","exerciseIds":["plank"]}]}]}"""
        val plan = LibraryJson.decodeFromString(LibraryData.serializer(), old).workouts.single()
        assertEquals(WorkoutMode.TIMED, plan.mode)
        assertEquals(Reps.DEFAULT, plan.blocks[0].repsAt(0))
        val b = BlockPlan("B", listOf("x", "y"), reps = listOf(10))
        assertEquals(listOf(10, 10), b.withAlignedReps().reps)
        assertEquals(emptyList<Int>(), b.compactReps().reps)
        assertEquals(listOf(10, 7), b.copy(reps = listOf(10, 7)).compactReps().reps)
    }

    @Test
    fun jsonRoundTripKeepsEverything() {
        val custom = ExerciseDef("perso_1", "Curl chaise", "Assis…", Muscle.BRAS, "concentration_curl", custom = true)
        val data = LibraryData(
            workouts = DefaultPlans.create(Durations(work = 45)) +
                WorkoutPlan("s1", "Libre", blocks = listOf(BlockPlan("A", listOf("perso_1")))) +
                WorkoutPlan("s2", "Salle", mode = WorkoutMode.REPS, blocks = listOf(BlockPlan("A", listOf("plank"), reps = listOf(15)))),
            customExercises = listOf(custom),
        )
        val text = LibraryJson.encodeToString(LibraryData.serializer(), data)
        assertEquals(data, LibraryJson.decodeFromString(LibraryData.serializer(), text))
    }

    @Test
    fun importMergesWithoutDeleting() {
        val base = LibraryData(workouts = DefaultPlans.create(Durations()))
        val incoming = LibraryData(
            workouts = listOf(
                WorkoutPlan("jour1", "Jour 1 modifié"),
                WorkoutPlan("nouvelle", "Nouvelle"),
            ),
            customExercises = listOf(ExerciseDef("perso_x", "X", "", Muscle.CARDIO, "burpee")),
        )
        val merged = LibraryOps.merge(base, incoming)
        assertEquals(listOf("jour1", "jour2", "jour3", "nouvelle"), merged.workouts.map { it.id })
        assertEquals("Jour 1 modifié", merged.workouts[0].title)
        assertTrue(merged.customExercises.single().custom)
    }

    @Test
    fun usagesFindWorkoutsUsingAnExercise() {
        val data = LibraryData(workouts = DefaultPlans.create(Durations()))
        assertEquals(listOf("jour2"), LibraryOps.usages(data, "hammer_curl").map { it.id })
        assertTrue(LibraryOps.usages(data, "burpee").isEmpty())
    }
}

class ShareCodeTest {
    @Test
    fun roundTripWithCustomExerciseAndLengthStaysReasonable() {
        val custom = ExerciseDef("perso_1", "Pompes mains serrées", "Coudes collés", Muscle.BRAS, "push_up", custom = true)
        val data = LibraryData(customExercises = listOf(custom))
        val plan = DefaultPlans.create(Durations())[1].let {
            it.copy(blocks = it.blocks + BlockPlan("Finisher", listOf("perso_1", "burpee")))
        }
        val code = ShareCode.encode(plan, LibraryOps.exercisesById(data))
        println("Longueur du code : ${code.length} caractères")
        java.io.File("../.webtools/android-code.txt").apply { parentFile?.mkdirs() }.writeText(code)
        assertTrue(code.length < 600)

        // Le message complet (avec texte autour) est accepté.
        val payload = ShareCode.decode("Salut !\n" + ShareCode.message(plan, code))!!
        assertEquals(plan.blocks, payload.w.blocks)
        assertEquals(plan.durations, payload.w.durations)
        assertEquals(listOf(custom), payload.x)

        val (added, newPlan) = LibraryOps.addShared(LibraryData(), payload)
        assertEquals(1, added.workouts.size)
        assertEquals(1, added.customExercises.size)
        assertTrue(newPlan.id != "s")
        // Réimporté chez quelqu'un qui l'a déjà : nouvelle séance, pas de doublon d'exercice.
        val (again, _) = LibraryOps.addShared(added, payload)
        assertEquals(2, again.workouts.size)
        assertEquals(1, again.customExercises.size)

        assertEquals(null, ShareCode.decode("SDL1-pasunvraicode"))
        assertEquals(null, ShareCode.decode("rien à voir"))
    }
}

class WebCompatTest {
    /** Code créé par la web app (Sporten 2.0), exporté par .webtools/tests.html. */
    @Test
    fun androidReadsCodeMadeByWebApp() {
        val f = java.io.File("../.webtools/web-code.txt")
        if (!f.exists()) return
        val p = ShareCode.decode(f.readText().trim())!!
        assertEquals(4, p.w.blocks.size)
        assertEquals(listOf("perso_x", "burpee"), p.w.blocks[3].exerciseIds)
        assertEquals(45, p.w.durations.work)
        assertEquals(5, p.w.durations.transition)
        assertEquals("Mon exo", p.x.single().name)
        assertEquals(Muscle.BRAS, p.x.single().muscle)
    }
}
