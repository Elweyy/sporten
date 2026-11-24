package fr.loic.seances.session

import fr.loic.seances.data.BlockPlan
import fr.loic.seances.data.Catalog
import fr.loic.seances.data.DefaultPlans
import fr.loic.seances.data.Durations
import fr.loic.seances.data.Reps
import fr.loic.seances.data.WorkoutMode
import fr.loic.seances.data.WorkoutPlan
import fr.loic.seances.data.formatDuration
import fr.loic.seances.data.resolve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepsTest {
    private val catalog = Catalog.builtIn.associateBy { it.id }
    private val plans = DefaultPlans.create(Durations())

    @Test
    fun threeCyclesHavePrepareNineExercisesPerCycleAndRestBetween() {
        val steps = buildSteps(plans[0].resolve(catalog), cycles = 3)
        assertTrue(steps.first() is Step.Prepare)
        assertEquals(27, steps.count { it is Step.Work })
        assertEquals(2, steps.count { it is Step.Rest }) // pas de repos après le dernier cycle
        assertEquals(24, steps.count { it is Step.Transition }) // 8 par cycle, aucune autour du repos
        assertTrue(steps.last() is Step.Work)
        assertTrue(steps[1] is Step.Work)
        assertTrue(steps[2] is Step.Transition)
        assertEquals((steps[3] as Step.Work), (steps[2] as Step.Transition).next)
        assertTrue(steps[18] is Step.Rest)
        assertEquals("Arnold Press", (steps[19] as Step.Work).exercise.name)
        assertEquals(2, (steps[19] as Step.Work).cycle)
        assertEquals(steps.sumOf { it.durationSec }, Durations().totalSec(3))
    }

    @Test
    fun withoutRestATransitionSeparatesCycles() {
        val d = Durations(prepare = 0, restBetweenCycles = 0, transition = 5)
        val steps = buildSteps(plans[0].resolve(catalog), cycles = 2, durations = d)
        assertEquals(18, steps.count { it is Step.Work })
        assertEquals(17, steps.count { it is Step.Transition })
        assertEquals(steps.sumOf { it.durationSec }, d.totalSec(2))
    }

    @Test
    fun singleCycleHasNoRest() {
        val steps = buildSteps(plans[2].resolve(catalog), cycles = 1)
        assertEquals(18, steps.size) // préparation + 9 exos + 8 transitions
        assertEquals(0, steps.count { it is Step.Rest })
    }

    @Test
    fun customDurationsApplyAndZeroRemovesSteps() {
        val d = Durations(prepare = 0, work = 45, restBetweenCycles = 0, transition = 0)
        val steps = buildSteps(plans[1].resolve(catalog), cycles = 2, durations = d)
        assertEquals(18, steps.size)
        assertTrue(steps.all { it is Step.Work && it.durationSec == 45 })
        assertEquals(10 + 2 * 9 * 60 + 60 + 16 * 5, Durations().totalSec(2))
    }

    @Test
    fun freeFormatBlocksAndEmptyBlocksAreSkipped() {
        val plan = WorkoutPlan(
            id = "x", title = "Libre",
            blocks = listOf(
                BlockPlan("A", listOf("plank", "burpee")),
                BlockPlan("Vide"),
                BlockPlan("B", listOf("push_up", "superman", "wall_sit", "inconnu")),
            ),
        )
        val w = plan.resolve(catalog)
        assertEquals(2, w.blocks.size) // bloc vide ignoré
        assertEquals(listOf(2, 3), w.blocks.map { it.exercises.size }) // exercice inconnu ignoré
        val works = buildSteps(w, cycles = 1).filterIsInstance<Step.Work>()
        assertEquals(5, works.size)
        assertEquals(listOf(2, 3), works.first().blockSizes)
        assertEquals(3, works.last().exercisesInBlock)
        assertEquals(4, works.last().indexInCycle)
    }

    @Test
    fun repsModeHasUntimedExercisesWithTheirReps() {
        val plan = WorkoutPlan(
            id = "r", title = "Salle", mode = WorkoutMode.REPS,
            blocks = listOf(
                BlockPlan("A", listOf("plank", "inconnu", "burpee"), reps = listOf(12, 99, 8)),
                BlockPlan("B", listOf("push_up")), // reps absentes : valeur par défaut
            ),
        )
        val steps = buildSteps(plan.resolve(catalog), cycles = 2, durations = Durations())
        val works = steps.filterIsInstance<Step.Work>()
        assertEquals(listOf(12, 8, Reps.DEFAULT, 12, 8, Reps.DEFAULT), works.map { it.reps }) // l'exo inconnu emporte ses reps
        assertTrue(works.all { it.isReps && it.durationSec == 0 })
        // Préparation, transitions et repos restent minutés.
        assertTrue(steps.first() is Step.Prepare)
        assertEquals(1, steps.count { it is Step.Rest })
        assertEquals(4, steps.count { it is Step.Transition })
        assertEquals(12 + 99 + 8 + Reps.DEFAULT, plan.repsPerCycle) // calculé sur la séance enregistrée
    }

    @Test
    fun timedModeIgnoresReps() {
        val plan = WorkoutPlan("t", "Chrono", blocks = listOf(BlockPlan("A", listOf("plank"), reps = listOf(5))))
        val work = buildSteps(plan.resolve(catalog), 1).filterIsInstance<Step.Work>().single()
        assertEquals(null, work.reps)
        assertEquals(Durations().work, work.durationSec)
    }

    @Test
    fun emptyWorkoutHasNoSteps() {
        assertTrue(buildSteps(WorkoutPlan("e", "Vide").resolve(catalog), 2).isEmpty())
    }

    @Test
    fun formatsDurations() {
        assertEquals("45 s", formatDuration(45))
        assertEquals("1 min", formatDuration(60))
        assertEquals("1 min 30", formatDuration(90))
    }
}
