package fr.loic.seances.screenshots

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import fr.loic.seances.App
import fr.loic.seances.data.BlockPlan
import fr.loic.seances.data.DefaultPlans
import fr.loic.seances.data.Durations
import fr.loic.seances.data.HistoryEntry
import fr.loic.seances.data.LibraryOps
import fr.loic.seances.data.WorkoutMode
import fr.loic.seances.data.resolve
import fr.loic.seances.session.SessionPart
import fr.loic.seances.session.SessionViewModel
import fr.loic.seances.ui.AppTheme
import fr.loic.seances.ui.CatalogScreen
import fr.loic.seances.ui.HistoryScreen
import fr.loic.seances.ui.HomeScreen
import fr.loic.seances.ui.SessionScreen
import fr.loic.seances.ui.themeBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

/**
 * Captures d'écran du README, dessinées sans téléphone (Robolectric + Roborazzi).
 * ./gradlew testDebugUnitTest -Pscreenshots → images PNG dans docs/screenshots
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xhdpi")
class ScreenshotsTest {
    @get:Rule
    val compose = createComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<Application>() as App
    private val durations = Durations(prepare = 10, work = 45, restBetweenCycles = 60, transition = 10)
    private val catalog = fr.loic.seances.data.Catalog.builtIn.associateBy { it.id }

    private val plans = DefaultPlans.create(durations).let { p ->
        listOf(
            p[0].copy(title = "Haut du corps", subtitle = "Épaules — Abdos — Jambes"),
            p[1].copy(title = "Pecs & bras", subtitle = "Pecs — Bras — Abdos", mode = WorkoutMode.REPS,
                blocks = p[1].blocks.map { b -> b.copy(reps = listOf(12, 10, 15)) }),
            p[2].copy(title = "Cardio jambes", subtitle = "Jambes — Taille — Dos"),
        )
    }

    private fun shot(name: String, afterMs: Long = 700) {
        // Image par image (16 ms), pour que les animations calées sur les frames avancent vraiment.
        repeat((afterMs / 16).toInt()) { compose.mainClock.advanceTimeBy(16) }
        captureScreenRoboImage("../docs/screenshots/$name.png")
    }

    private fun screen(content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AppTheme { Box(Modifier.fillMaxSize().themeBackdrop()) { content() } }
        }
    }

    private fun history(): List<HistoryEntry> {
        val cal = Calendar.getInstance()
        return (0 until 9).map { i ->
            cal.timeInMillis = System.currentTimeMillis() - i * 3L * 86_400_000
            HistoryEntry(
                id = "h$i", date = cal.timeInMillis, durationMs = (28 + i % 4 * 6) * 60_000L, workedMs = 20 * 60_000L,
                exercisesDone = 27, exercisesPlanned = 27, title = plans[i % 3].title, subtitle = plans[i % 3].subtitle,
                cycles = 3, complete = true, muscles = mapOf("JAMBES" to 9, "ABDOS" to 6, "EPAULES" to 6, "BRAS" to 6),
            )
        }
    }

    @Test
    fun home() {
        screen {
            HomeScreen(
                workouts = plans, exercises = catalog, cycles = 3, onCyclesChange = {}, onStart = {}, onEdit = {},
                onNew = {}, onDuplicate = {}, onDelete = {}, onMove = { _, _ -> }, onOpenCatalog = {}, onOpenSettings = {},
                onShare = {}, onImport = {}, history = history(),
            )
        }
        shot("accueil")
    }

    @Test
    fun superSession() {
        screen {
            HomeScreen(
                workouts = plans, exercises = catalog, cycles = 3, onCyclesChange = {}, onStart = {}, onEdit = {},
                onNew = {}, onDuplicate = {}, onDelete = {}, onMove = { _, _ -> }, onOpenCatalog = {}, onOpenSettings = {},
                onShare = {}, onImport = {}, history = history(),
            )
        }
        compose.onNodeWithText("⛓️ Super séance").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onAllNodesWithText(plans[0].title).onLast().performClick()
        compose.onAllNodesWithText(plans[2].title).onLast().performClick()
        shot("super-seance")
    }

    private fun session(mode: WorkoutMode, cycles: Int = 3, advance: SessionViewModel.() -> Unit): SessionViewModel {
        val plan = plans[if (mode == WorkoutMode.REPS) 1 else 0]
        val vm = SessionViewModel(app, listOf(SessionPart(plan.resolve(catalog), durations)), cycles)
        vm.advance()
        return vm
    }

    @Test
    fun timedSession() {
        val vm = session(WorkoutMode.TIMED) { skip(); skip(); skip() }
        screen { SessionScreen(vm, onExit = {}) }
        shot("seance-chrono")
    }

    @Test
    fun repsSession() {
        val vm = session(WorkoutMode.REPS) { skip() }
        screen { SessionScreen(vm, onExit = {}) }
        shot("seance-repetitions")
    }

    @Test
    fun catalogScreen() {
        screen { CatalogScreen(library = app.library, onBack = {}) }
        shot("catalogue")
    }

    @Test
    fun historyScreen() {
        val h = history()
        screen { HistoryScreen(history = h, onDelete = {}, onBack = {}) }
        shot("historique")
    }
}
