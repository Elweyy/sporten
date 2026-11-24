package fr.loic.seances

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.loic.seances.data.BlockPlan
import fr.loic.seances.data.LibraryOps
import fr.loic.seances.data.WorkoutPlan
import fr.loic.seances.data.resolve
import fr.loic.seances.session.SessionPart
import fr.loic.seances.session.SessionViewModel
import fr.loic.seances.ui.AppTheme
import fr.loic.seances.ui.AppThemeId
import fr.loic.seances.ui.themeBackdrop
import fr.loic.seances.ui.CatalogScreen
import fr.loic.seances.ui.EditWorkoutScreen
import fr.loic.seances.ui.HistoryScreen
import fr.loic.seances.ui.HomeScreen
import fr.loic.seances.ui.RandomScreen
import fr.loic.seances.ui.ImportDialog
import fr.loic.seances.ui.ShareDialog
import fr.loic.seances.ui.Palette
import fr.loic.seances.ui.SessionScreen
import fr.loic.seances.ui.SettingsScreen
import fr.loic.seances.ui.UpdateBanner
import fr.loic.seances.ui.WhatsNewDialog
import fr.loic.seances.update.WhatsNew

private sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data object Catalog : Screen
    data object History : Screen
    data object Random : Screen
    data class Edit(val plan: WorkoutPlan, val isNew: Boolean) : Screen
    /** Une séance, ou plusieurs à enchaîner (super séance). */
    data class Session(val plans: List<WorkoutPlan>, val runId: Int) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as App
        val settings = app.settings
        val library = app.library
        val updater = app.updater
        // Une fois par lancement (pas en recréant l'écran, par exemple en tournant le téléphone).
        if (savedInstanceState == null) updater.check()

        setContent {
            var themeId by remember { mutableStateOf(AppThemeId.of(settings.theme)) }
            AppTheme(themeId) {
                val data by library.data.collectAsStateWithLifecycle()
                var cycles by remember { mutableIntStateOf(settings.cycles) }
                var defaultDurations by remember { mutableStateOf(settings.durations) }
                var summaryOn by remember { mutableStateOf(settings.summary) }
                var voiceOn by remember { mutableStateOf(settings.voice) }
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                var runCounter by remember { mutableIntStateOf(0) }
                var sharing by remember { mutableStateOf<WorkoutPlan?>(null) }
                var importing by remember { mutableStateOf(false) }
                // « Quoi de neuf » : au premier lancement après une mise à jour (ou depuis les réglages).
                val version = updater.currentVersion
                val notes = remember { WhatsNew.notes(this@MainActivity, version) }
                var whatsNew by remember {
                    mutableStateOf(notes != null && WhatsNew.shouldShow(this@MainActivity, settings.lastSeenVersion, version))
                }
                if (!whatsNew && settings.lastSeenVersion != version) settings.lastSeenVersion = version

                // La notification du chrono a besoin de cette autorisation sur Android 13+.
                // Refusée, la séance fonctionne quand même (le chrono continue), sans notification visible.
                val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                fun startChain(plans: List<WorkoutPlan>) {
                    val playable = plans.filter { it.resolve(library.exercisesById).blocks.isNotEmpty() }
                    if (playable.isEmpty()) return
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    runCounter++
                    screen = Screen.Session(playable, runCounter)
                }
                fun startWorkout(plan: WorkoutPlan) = startChain(listOf(plan))

                Box(
                    Modifier
                        .fillMaxSize()
                        .themeBackdrop()
                        .safeDrawingPadding(),
                ) {
                    when (val s = screen) {
                        is Screen.Session -> {
                            val vm: SessionViewModel = viewModel(
                                key = "session-${s.runId}",
                                factory = viewModelFactory {
                                    initializer {
                                        SessionViewModel(
                                            application,
                                            s.plans.map { SessionPart(it.resolve(library.exercisesById), it.durations) },
                                            cycles,
                                            voiceOn,
                                        )
                                    }
                                },
                            )
                            SessionScreen(
                                vm,
                                onExit = {
                                    vm.stop()
                                    screen = Screen.Home
                                },
                                showSummary = summaryOn,
                                onRecord = { library.addHistory(it) },
                            )
                        }
                        is Screen.Edit -> EditWorkoutScreen(
                            initial = s.plan,
                            isNew = s.isNew,
                            library = library,
                            onSave = {
                                library.saveWorkout(it)
                                screen = Screen.Home
                            },
                            onCancel = { screen = Screen.Home },
                        )
                        Screen.Catalog -> CatalogScreen(library = library, onBack = { screen = Screen.Home })
                        Screen.History -> HistoryScreen(
                            history = data.history,
                            onDelete = { library.deleteHistory(it.id) },
                            onBack = { screen = Screen.Home },
                        )
                        Screen.Random -> RandomScreen(
                            exercises = LibraryOps.exercisesById(data).values.toList(),
                            durations = defaultDurations,
                            cycles = cycles,
                            initialMuscles = settings.randomMuscles,
                            initialPerBlock = settings.randomPerBlock,
                            initialGym = settings.randomGym,
                            onChoiceChange = { m, n, gym ->
                                settings.randomMuscles = m
                                settings.randomPerBlock = n
                                settings.randomGym = gym
                            },
                            onStart = ::startWorkout,
                            onSave = {
                                library.addWorkout(it)
                                Toast.makeText(this@MainActivity, "« ${it.title} » ajoutée à tes séances", Toast.LENGTH_SHORT).show()
                                screen = Screen.Home
                            },
                            onBack = { screen = Screen.Home },
                        )
                        Screen.Settings -> SettingsScreen(
                            durations = defaultDurations,
                            onChange = {
                                defaultDurations = it
                                settings.durations = it
                            },
                            library = library,
                            onBack = { screen = Screen.Home },
                            summary = summaryOn,
                            onSummaryChange = {
                                summaryOn = it
                                settings.summary = it
                            },
                            voice = voiceOn,
                            onVoiceChange = {
                                voiceOn = it
                                settings.voice = it
                            },
                            updater = updater,
                            onShowWhatsNew = if (notes != null) ({ whatsNew = true }) else null,
                            theme = themeId,
                            onThemeChange = {
                                themeId = it
                                settings.theme = it.name
                            },
                        )
                        Screen.Home -> HomeScreen(
                            workouts = data.workouts,
                            exercises = LibraryOps.exercisesById(data),
                            cycles = cycles,
                            onCyclesChange = {
                                cycles = it
                                settings.cycles = it
                            },
                            onStart = ::startWorkout,
                            onEdit = { screen = Screen.Edit(it, isNew = false) },
                            onNew = {
                                screen = Screen.Edit(
                                    WorkoutPlan(
                                        id = LibraryOps.newId("seance"),
                                        title = "Séance ${data.workouts.size + 1}",
                                        blocks = List(3) { BlockPlan("Bloc ${it + 1}") },
                                        durations = defaultDurations,
                                    ),
                                    isNew = true,
                                )
                            },
                            onDuplicate = { library.duplicateWorkout(it.id) },
                            onDelete = { library.deleteWorkout(it.id) },
                            onMove = { w, delta -> library.moveWorkout(w.id, delta) },
                            onOpenCatalog = { screen = Screen.Catalog },
                            onOpenSettings = { screen = Screen.Settings },
                            onShare = { sharing = it },
                            onImport = { importing = true },
                            history = data.history,
                            onOpenHistory = { screen = Screen.History },
                            onRandom = { screen = Screen.Random },
                            onStartChain = ::startChain,
                            banner = { UpdateBanner(updater) },
                        )
                    }
                    if (whatsNew && notes != null && (screen == Screen.Home || screen == Screen.Settings)) {
                        WhatsNewDialog(version, notes, onDismiss = {
                            whatsNew = false
                            settings.lastSeenVersion = version
                        })
                    }
                    if (screen == Screen.Home) {
                        sharing?.let { ShareDialog(it, library, onDismiss = { sharing = null }) }
                        if (importing) {
                            ImportDialog(library, onImported = { importing = false }, onDismiss = { importing = false })
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as App).updater.onResume()
    }
}
