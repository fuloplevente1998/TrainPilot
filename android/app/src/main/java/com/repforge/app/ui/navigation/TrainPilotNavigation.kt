package com.repforge.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.NativeDataReplacement
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.onboarding.NativeOnboardingPolicy
import com.repforge.app.ui.onboarding.NativeOnboardingScreen
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.repforge.app.NativeProgramGpsChoice
import com.repforge.app.NativeWorkoutSession
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.NativeWorkoutTarget
import com.repforge.app.ui.coach.CoachScreen
import com.repforge.app.ui.workout.ActiveWorkoutViewModel
import com.repforge.app.ui.programs.TrainingLandingScreen
import com.repforge.app.ui.calendar.CalendarScreen
import com.repforge.app.ui.home.QuickWorkoutScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.home.HomeScreen
import com.repforge.app.ui.workout.ActiveWorkoutScreen
import com.repforge.app.ui.settings.SettingsScreen
import com.repforge.app.ui.progress.ProgressScreen
import com.repforge.app.ui.journal.JournalScreen
import com.repforge.app.ui.health.HealthDashboardScreen
import com.repforge.app.ui.programs.TrainingProgramsScreen
import com.repforge.app.ui.programs.ExerciseLibraryScreen
import com.repforge.app.ui.theme.LocalTrainPilotColors

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Kezdőlap", Icons.Default.Home)
    data object Training : Screen("training", "Edzés", Icons.Default.FitnessCenter)
    data object Journal : Screen("journal", "Napló", Icons.Default.History)
    data object Progress : Screen("progress", "Fejlődés", Icons.Default.ShowChart)
    data object Health : Screen("health", "Egészség", Icons.Default.DateRange)
}

@Composable
fun TrainPilotAppScaffold(
    settings: TrainPilotUserSettings,
    onFirstFrameReady: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val colors = LocalTrainPilotColors.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val onboardingStore = remember(context) { TrainPilotSettingsStore(context) }
    var onboardingSaving by remember { mutableStateOf(false) }
    var onboardingFailed by remember { mutableStateOf(false) }
    var reopenedOnboarding by remember { mutableStateOf(false) }
    fun writeOnboarding(onSaved: () -> Unit = {},
        change: suspend (NativeDataReplacement.Continuation) -> Boolean) {
        if (onboardingSaving) return
        val ticket = try { NativeDataReplacement.continuationForCurrentData(context) }
            catch (_: Exception) {
                // Restore/erase admission is closed; leave profile and navigation unchanged.
                onboardingFailed = true
                return
            }
        onboardingSaving = true
        onboardingFailed = false
        scope.launch(Dispatchers.Main.immediate) {
            try {
                val saved = withContext(Dispatchers.IO) { change(ticket) }
                if (saved) onSaved() else onboardingFailed = true
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { onboardingFailed = true }
            finally { onboardingSaving = false }
        }
    }

    // MigrationReady is set before the initial settings Flow emission. Never flash
    // a usable Home screen while the stored onboarding decision is still loading.
    if (!settings.migrationV1371Complete) {
        LaunchedEffect(Unit) { onFirstFrameReady() }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val firstLaunch = NativeOnboardingPolicy.destination(settings.onboarding128)
    if (firstLaunch != NativeOnboardingPolicy.Destination.HOME || reopenedOnboarding) {
        LaunchedEffect(Unit) { onFirstFrameReady() }
        NativeOnboardingScreen(
            settings = settings,
            destination = if (reopenedOnboarding && firstLaunch == NativeOnboardingPolicy.Destination.HOME)
                NativeOnboardingPolicy.Destination.WELCOME else firstLaunch,
            saving = onboardingSaving, failed = onboardingFailed,
            onBegin = {
                writeOnboarding { ticket ->
                    onboardingStore.transitionOnboarding(settings.onboarding128,
                        NativeOnboardingPolicy.IN_PROGRESS, ticket)
                }
            },
            onSkip = {
                if (settings.onboarding128 == NativeOnboardingPolicy.SKIPPED) {
                    reopenedOnboarding = false
                } else writeOnboarding(onSaved = { reopenedOnboarding = false }) { ticket ->
                    onboardingStore.transitionOnboarding(settings.onboarding128,
                        NativeOnboardingPolicy.SKIPPED, ticket)
                }
            },
            onProfileSave = { fields ->
                writeOnboarding(onSaved = { reopenedOnboarding = false }) { ticket ->
                    onboardingStore.completeOnboarding(settings.profileJson, fields, ticket)
                }
            },
            onBack = {
                if (settings.onboarding128 == NativeOnboardingPolicy.SKIPPED)
                    reopenedOnboarding = false
            }
        )
        return
    }
    var starting by remember { mutableStateOf(false) }
    // Memory-only user start intent. Process recreation/navigation cannot silently start GPS.
    var launchGps by remember { mutableStateOf<NativeWorkoutTarget?>(null) }
    data class PendingProgram(val option: NativeProgramGpsChoice.Option, val dayId: String, val scheduleId: String?)
    var pendingProgram by remember { mutableStateOf<PendingProgram?>(null) }
    fun launchProgram(programId: String?, dayId: String, scheduleId: String?, option: NativeProgramGpsChoice.Option?, record: Boolean) {
        starting = true
        scope.launch(Dispatchers.Main.immediate) {
            try {
                val launch = NativeWorkoutSession.startProgramWorkout(context, programId, dayId, scheduleId,
                    option?.activityId, record)
                withContext(Dispatchers.Main) {
                    launchGps = launch.gpsTarget
                    navController.openCanonicalWorkout()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { snackbar.showSnackbar(error.message ?: "Az edzést nem sikerült elindítani.") }
            finally { starting = false }
        }
    }
    fun startWorkout(programId: String?, dayId: String, scheduleId: String? = null) {
        if (starting || pendingProgram != null) return
        if (ActiveWorkoutStore.current(context)?.optBoolean("active") == true) {
            launchProgram(programId, dayId, scheduleId, null, false)
            return
        }
        starting = true
        scope.launch(Dispatchers.Main.immediate) {
            try {
                val option = NativeProgramGpsChoice.read(context, programId, dayId)
                if (option != null) {
                    pendingProgram = PendingProgram(option, dayId, scheduleId)
                    starting = false
                } else launchProgram(programId, dayId, scheduleId, null, false)
            } catch (cancelled: CancellationException) { starting = false; throw cancelled }
            catch (error: Exception) { starting = false; snackbar.showSnackbar(error.message ?: "Az edzés nem indítható.") }
        }
    }
    pendingProgram?.let { pending -> ProgramGpsStartDialog(pending.option, settings.language,
        onCancel = { pendingProgram = null }, onStart = { record ->
            pendingProgram = null
            launchProgram(pending.option.programId, pending.dayId, pending.scheduleId, pending.option, record)
        }) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    LaunchedEffect(currentRoute) {
        if (com.repforge.app.BuildConfig.DEBUG) android.util.Log.d("NativeNavigation", "arrived $currentRoute")
    }

    val activeSnapshot by ActiveWorkoutStore.snapshotFlow.collectAsStateWithLifecycle()
    val hasActiveWorkout = activeSnapshot?.optBoolean("active", false) == true
    val isWorkoutPaused = activeSnapshot?.optBoolean("workoutPaused", false) == true
    val timerOwner = activeSnapshot?.optString("timerOwner", "phone").orEmpty()

    // Keep the editor's existing ViewModelStoreOwner when Settings is pushed above it.
    // Its stopwatch collectors remain alive; returning does not create another editor/session.
    val activeEntry = remember(navBackStackEntry) {
        runCatching { navController.getBackStackEntry("active_workout") }.getOrNull()
    }
    val editor = activeEntry?.let {
        androidx.lifecycle.viewmodel.compose.viewModel<ActiveWorkoutViewModel>(viewModelStoreOwner = it)
    }
    // Observe only the navigation guard, not the editor's 250ms stopwatch ticks.
    val finishingFlow = remember(editor) { editor?.uiState?.map { it.isFinishing }?.distinctUntilChanged() }
    val editorFinishing = finishingFlow?.collectAsStateWithLifecycle(initialValue = false)?.value == true
    val settingsOrigin = if (currentRoute == "settings")
        navController.previousBackStackEntry?.destination?.route ?: "home" else "home"
    var personalPlannerRequested by remember { mutableStateOf(false) }
    var settingsSection by remember { mutableStateOf("general") }
    val navigationBlocked = starting || editorFinishing ||
        activeSnapshot?.optBoolean("finalizationPending") == true

    // Check again at click time: a background Wear finish can race with a rendered toolbar.
    fun canNavigate(target: String): Boolean {
        val current = ActiveWorkoutStore.current(context)
        val execution = PhoneWorkoutExecution.store(context)
        return NativeMainMenu.navigationAllowed(target, starting,
            finishing = editor?.uiState?.value?.isFinishing == true,
            finalizing = current?.optBoolean("finalizationPending") == true,
            restoreBlocked = execution.restoreBlocked(), commandsSuspended = execution.commandsSuspended())
    }
    fun selectDestination(requested: String) {
        val current = ActiveWorkoutStore.current(context)
        val target = NativeMainMenu.destination(requested, current?.optBoolean("active") == true)
        if (!canNavigate(target)) {
            if (com.repforge.app.BuildConfig.DEBUG) android.util.Log.d("NativeNavigation",
                "rejected $currentRoute->$target starting=$starting finishing=${editor?.uiState?.value?.isFinishing} " +
                    "finalizing=${current?.optBoolean("finalizationPending")} " +
                    "restore=${PhoneWorkoutExecution.store(context).restoreBlocked()} suspended=${PhoneWorkoutExecution.store(context).commandsSuspended()}")
            return
        }
        if (com.repforge.app.BuildConfig.DEBUG) android.util.Log.d("NativeNavigation", "requested $currentRoute->$target")
        if (target == currentRoute) return
        val navigate = {
            when {
                target == "active_workout" -> navController.openCanonicalWorkout()
                target in NativeMainMenu.routes -> navController.navigate(target) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    // Home is the graph's root. Restoring its saved stack can
                    // resurrect a different tab/secondary destination above it.
                    // Other tabs retain their independent saved state.
                    restoreState = target != "home"
                }
                else -> navController.navigate(target) { launchSingleTop = true }
            }
        }
        if (NativeMainMenu.pausesWorkout(currentRoute, target, settingsOrigin) && editor != null) {
            val current = ActiveWorkoutStore.current(context)
            editor.leaveWorkout(current?.optString("workoutId").orEmpty(), current?.optLong("generation") ?: 0L, navigate)
        } else navigate()
    }
    // A local function reference can compare equal across compositions while
    // retaining the old route/editor capture. Refresh it explicitly when its
    // navigation context changes; guards still read the live execution at click.
    val destinationSelector = remember(currentRoute, settingsOrigin, editor, starting) {
        { requested: String -> selectDestination(requested) }
    }
    fun goBack() {
        val target = navController.previousBackStackEntry?.destination?.route ?: "home"
        if (!canNavigate(target)) return
        if (currentRoute == "active_workout" && editor != null) {
            val current = ActiveWorkoutStore.current(context)
            editor.leaveWorkout(current?.optString("workoutId").orEmpty(), current?.optLong("generation") ?: 0L) {
                navController.popBackStack()
            }
        } else if (currentRoute == "settings" && target == "active_workout" &&
            ActiveWorkoutStore.current(context)?.optBoolean("active") != true) {
            // The watch may have completed the session while Settings was in front.
            navController.navigate("home") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        } else navController.popBackStack()
    }
    // The editor has its own BackHandler. Other screens must also respect a late finish fence.
    BackHandler(enabled = currentRoute != "active_workout" &&
        (currentRoute in NativeMainMenu.secondaryRoutes || navigationBlocked)) { goBack() }

    LaunchedEffect(Unit) {
        ActiveWorkoutStore.current(context)
        onFirstFrameReady()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.bg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                NativeMainHeader(currentRoute, settings.language, enabled = !navigationBlocked,
                    onBack = if (currentRoute in NativeMainMenu.secondaryRoutes) ({ goBack() }) else null,
                    onSettings = { settingsSection = "general"; selectDestination("settings") },
                    onLanguage = { settingsSection = "language"; selectDestination("settings") },
                    onTheme = { settingsSection = "theme"; selectDestination("settings") },
                    onHome = { selectDestination("home") },
                    backEnabled = !starting && !editorFinishing &&
                        (activeSnapshot?.optBoolean("finalizationPending") != true || settingsOrigin == "active_workout"))
                // Intentional Studio design: primary tabs are placed ABOVE all pages.
                NativeTopNavigation(currentRoute, settingsOrigin, settings.language,
                    enabled = !navigationBlocked, onSelect = destinationSelector)
            }
        },
        bottomBar = {
            // The active workout remains reachable without blocking content.
            if (hasActiveWorkout && currentRoute != "active_workout") FloatingActiveWorkoutBanner(
                title = activeSnapshot?.optString("dayName", "Folyamatban lévő edzés") ?: "Edzés",
                isPaused = isWorkoutPaused, timerOwner = timerOwner, language = settings.language,
                enabled = !starting && !editorFinishing,
                onClick = { selectDestination("training") })
        }

    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        navController = navController,
                        onStartWorkout = { programId, dayId, scheduleId ->
                            startWorkout(programId, dayId, scheduleId)
                        },
                        onCalendar = { selectDestination("calendar") }, onCoach = { selectDestination("coach") },
                        onPrograms = { selectDestination("programs") }, language = settings.language, busy = navigationBlocked,
                        onProfile = { settingsSection = "profile"; selectDestination("settings") },
                        onPersonalPlan = {
                            selectDestination("programs")
                            if (navController.currentDestination?.route == "programs") personalPlannerRequested = true
                        },
                        onHealth = { selectDestination("health") },
                        offerOnboarding = settings.onboarding128 == NativeOnboardingPolicy.SKIPPED,
                        onOnboarding = { if (!navigationBlocked && !hasActiveWorkout) reopenedOnboarding = true },
                        onResumeDraft = {
                            if (!starting) {
                                starting = true
                                scope.launch(Dispatchers.Main.immediate) {
                                    try {
                                        NativeWorkoutSession.resumeImportedDraft(context)
                                        withContext(Dispatchers.Main) { navController.openCanonicalWorkout() }
                                    } catch (cancelled: CancellationException) { throw cancelled }
                                    catch (error: Exception) { snackbar.showSnackbar(error.message ?: "A félbehagyott edzés nem vehető át.") }
                                    finally { starting = false }
                                }
                            }
                        },
                        onQuickWorkout = { outdoor ->
                            selectDestination(if (hasActiveWorkout) "training" else if (outdoor) "quick_outdoor" else "quick_workout")
                        }
                    )
                }
                composable(Screen.Training.route) {
                    TrainingLandingScreen(language = settings.language, activeProgramId = settings.activeProgramId,
                        busy = navigationBlocked, onStart = { programId, dayId -> startWorkout(programId, dayId) },
                        onQuick = { outdoor -> selectDestination(if (hasActiveWorkout) "training" else if (outdoor) "quick_outdoor" else "quick_workout") },
                        onPrograms = { selectDestination("programs") },
                        onCalendar = { selectDestination("calendar") }, onCoach = { selectDestination("coach") },
                        onLibrary = { selectDestination("exercise_library") })
                }
                composable("exercise_library") {
                    ExerciseLibraryScreen(language = settings.language)
                }
                composable("programs") {
                    TrainingProgramsScreen(navController = navController, language = settings.language,
                        requestPersonalPlanner = personalPlannerRequested,
                        onPersonalPlannerConsumed = { personalPlannerRequested = false },
                        onStartProgramDay = { programId, dayId -> startWorkout(programId, dayId) })
                }
                composable("coach") {
                    CoachScreen(starting = starting,
                        onStart = { programId, dayId, scheduleId -> startWorkout(programId, dayId, scheduleId) },
                        onCalendar = { selectDestination("calendar") }, onHealth = { selectDestination("health") },
                        onProgress = { selectDestination("progress") }, onPrograms = { selectDestination("programs") })
                }
                composable("calendar") {
                    CalendarScreen(starting = starting,
                        onJournal = { selectDestination("journal") },
                        onStart = { row -> startWorkout(row.programId, row.dayId, row.id) })
                }
                composable(Screen.Journal.route) {
                    JournalScreen(navController = navController, language = settings.language)
                }
                composable(Screen.Progress.route) {
                    ProgressScreen(onWeightJournal = { selectDestination("weight_journal") })
                }
                composable(Screen.Health.route) {
                    HealthDashboardScreen(navController = navController,
                        onWeightJournal = { selectDestination("weight_journal") })
                }
                composable("weight_journal") {
                    com.repforge.app.ui.progress.WeightJournalScreen()
                }
                composable("active_workout") {
                    ActiveWorkoutScreen(navController = navController, language = settings.language, launchGps = launchGps,
                        onLaunchGpsConsumed = { launchGps = null })
                }
                for (outdoor in listOf(false, true)) {
                    composable(if (outdoor) "quick_outdoor" else "quick_workout") {
                        QuickWorkoutScreen(outdoorOnly = outdoor, starting = starting,
                            onCancel = { navController.popBackStack() }, onStart = { exerciseId, gps ->
                                if (!starting) {
                                    starting = true
                                    scope.launch(Dispatchers.Main.immediate) {
                                        try {
                                            val launch = NativeWorkoutSession.startQuickWorkout(context, exerciseId, gps)
                                            withContext(Dispatchers.Main) {
                                                launchGps = launch.gpsTarget
                                                navController.navigate("active_workout") {
                                                    popUpTo(if (outdoor) "quick_outdoor" else "quick_workout") { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            }
                                        } catch (cancelled: CancellationException) { throw cancelled }
                                        catch (error: Exception) { snackbar.showSnackbar(error.message ?: "Az edzés nem indítható. Az adatok megmaradtak.") }
                                        finally { starting = false }
                                    }
                                }
                            })
                    }
                }
                composable("settings") {
                    SettingsScreen(navController = navController, allowDataReplacement = !hasActiveWorkout, initialSection = settingsSection)
                }
            }
        }
    }
}

@Composable
fun FloatingActiveWorkoutBanner(
    title: String,
    isPaused: Boolean,
    timerOwner: String,
    language: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val labels = when (language) {
        "en" -> listOf("Paused", "Watch controls", "Phone controls", "Continue ›")
        "de" -> listOf("Pausiert", "Uhr steuert", "Telefon steuert", "Fortsetzen ›")
        "ro" -> listOf("În pauză", "Controlat de ceas", "Controlat de telefon", "Continuă ›")
        else -> listOf("Szünetel", "Óra vezérli", "Telefon vezérli", "Folytatás ›")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card2)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isPaused) colors.accent else colors.success)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isPaused) labels[0] else if (timerOwner == "watch") labels[1] else labels[2],
                    color = colors.muted,
                    fontSize = 11.sp
                )
            }
        }
        Text(
            text = labels[3],
            color = colors.accentText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Reuse the actual editor entry before considering a restored tab state. */
private fun NavHostController.openCanonicalWorkout() {
    check(android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) { "Navigation requires the main looper" }
    if (popBackStack("active_workout", inclusive = false)) return
    navigate("active_workout") {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
