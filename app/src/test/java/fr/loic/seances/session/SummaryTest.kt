package fr.loic.seances.session

import fr.loic.seances.data.BlockPlan
import fr.loic.seances.data.Catalog
import fr.loic.seances.data.DefaultPlans
import fr.loic.seances.data.Durations
import fr.loic.seances.data.WorkoutMode
import fr.loic.seances.data.WorkoutPlan
import fr.loic.seances.data.resolve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SummaryTest {
    private val catalog = Catalog.builtIn.associateBy { it.id }

    @Test
    fun fullSessionSummaryLooksLikeTheHandWrittenPlan() {
        val plan = DefaultPlans.create(Durations())[0]
        val text = buildSummary(plan.resolve(catalog), 3, plan.durations, 27)
        File("build/summary-example.txt").writeText(text)
        assertTrue(text, text.startsWith("🏋️ ${plan.title} : ${plan.subtitle}\n3 cycles · 9 exos par cycle"))
        assertTrue(text, "min au total" !in text && "Sporten" !in text)
        assertTrue(text, text.endsWith("- Good Morning aux haltères"))
        assertTrue(text, "\nBloc 1 : ${plan.blocks[0].title}\n- Arnold Press\n" in text)
    }

    @Test
    fun partialSessionAndDefaultBlockNames() {
        val plan = WorkoutPlan(
            "x", "Libre",
            blocks = listOf(BlockPlan("Bloc 1", listOf("plank", "burpee")), BlockPlan("Gainage", listOf("superman"))),
        )
        val text = buildSummary(plan.resolve(catalog), 3, plan.durations, 4)
        assertTrue(text, text.startsWith("🏋️ Libre\n1 cycle + 1 exo (sur 3 cycles)"))
        assertTrue(text, "\nBloc 1\n- " in text)
        assertTrue(text, "\nBloc 2 : Gainage\n" in text)
        assertEquals("1 h 05", formatMinutes(65 * 60_000L))
    }

    @Test
    fun repsSummaryListsRepsInsteadOfDuration() {
        val plan = WorkoutPlan(
            "r", "Salle", mode = WorkoutMode.REPS,
            blocks = listOf(BlockPlan("Jambes", listOf("plank", "burpee"), reps = listOf(12, 8))),
        )
        val text = buildSummary(plan.resolve(catalog), 3, plan.durations, 6)
        assertTrue(text, text.startsWith("🏋️ Salle\n3 cycles · 2 exos par cycle"))
        assertTrue(text, "max de reps" !in text && " par exo" !in text)
        assertTrue(text, "\nBloc 1 : Jambes\n- " in text && text.lines().last().endsWith(" × 8"))
        assertTrue(text, text.lines().any { it.endsWith(" × 12") })
    }
}
