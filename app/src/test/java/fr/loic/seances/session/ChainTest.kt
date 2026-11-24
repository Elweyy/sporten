package fr.loic.seances.session

import fr.loic.seances.data.Catalog
import fr.loic.seances.data.DefaultPlans
import fr.loic.seances.data.Durations
import fr.loic.seances.data.WorkoutMode
import fr.loic.seances.data.resolve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChainTest {
    private val catalog = Catalog.builtIn.associateBy { it.id }
    private val plans = DefaultPlans.create(Durations())
    private fun part(i: Int, d: Durations = Durations(), mode: WorkoutMode = WorkoutMode.TIMED) =
        SessionPart(plans[i].copy(mode = mode).resolve(catalog), d)

    @Test
    fun twoWorkoutsAreChainedWithOneBreakAndOnePrepare() {
        val a = part(0, Durations(prepare = 10, restBetweenCycles = 90))
        val b = part(1, Durations(prepare = 10, restBetweenCycles = 60), WorkoutMode.REPS)
        val chain = buildChainSteps(listOf(a, b), cycles = 2)
        val steps = chain.steps
        assertEquals(1, steps.count { it is Step.Prepare })
        val breaks = steps.filterIsInstance<Step.NextPart>()
        assertEquals(1, breaks.size)
        assertEquals(90, breaks[0].durationSec)
        assertEquals(1, breaks[0].partIndex)
        assertEquals(2, breaks[0].partCount)
        val own = buildSteps(a.workout, 2, a.durations).size + buildSteps(b.workout, 2, b.durations).size
        assertEquals(own - 1 + 1, steps.size) // préparation de la 2e remplacée par la pause
        assertEquals(steps.size, chain.partOf.size)
        val bi = steps.indexOf(breaks[0])
        assertEquals(1, chain.partOf[bi])
        assertEquals(0, chain.partOf[bi - 1])
        assertTrue(steps[bi + 1] is Step.Work && (steps[bi + 1] as Step.Work).isReps)
        assertTrue(steps.take(bi).filterIsInstance<Step.Work>().none { it.isReps })
    }

    @Test
    fun noBreakWhenDurationsAreZero() {
        val d = Durations(prepare = 0, restBetweenCycles = 0)
        val chain = buildChainSteps(listOf(part(0, d), part(1, d)), cycles = 1)
        assertTrue(chain.steps.none { it is Step.NextPart })
    }

    @Test
    fun chainSummaryListsPartsDone() {
        val a = part(0)
        val b = part(1)
        val text = buildChainSummary(listOf(a, b), 1, listOf(9, 0))
        assertTrue(text.startsWith("⛓️ Super séance : ${a.workout.title} + ${b.workout.title}"))
        assertEquals(1, text.split("🏋️").size - 1) // la 2e séance n'a pas été commencée
        assertEquals(buildSummary(a.workout, 1, a.durations, 3), buildChainSummary(listOf(a), 1, listOf(3)))
    }
}
