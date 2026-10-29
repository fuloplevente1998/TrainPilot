package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localization.TrainPilotStrings
import com.example.model.HomeDemoMode
import com.example.model.MainTab
import com.example.model.SubScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.example.ui.components.ExerciseYouTubeVideoModalDialog
import com.example.ui.components.GoalsEditDialog
import com.example.ui.components.LocalOpenExerciseVideo
import com.example.ui.components.PalettePickerBottomSheet
import com.example.ui.components.PersonalPlannerModalDialog
import com.example.ui.components.QuickWorkoutSheet
import com.example.ui.components.TrainPilotGlobalTopHeaderAndNav
import com.example.ui.screens.ActiveBilateralWorkoutScreen
import com.example.ui.screens.ActiveOutdoorGpsScreen
import com.example.ui.screens.ActiveStrengthWorkoutScreen
import com.example.ui.screens.ActiveTimedWorkoutScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.CalendarPlannerScreen
import com.example.ui.screens.CoachDetailScreen
import com.example.ui.screens.ExerciseDetailScreen
import com.example.ui.screens.ExerciseLibraryScreen
import com.example.ui.screens.HealthTabScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IntervalSettingsScreen
import com.example.ui.screens.LogRouteMapScreen
import com.example.ui.screens.LogTabScreen
import com.example.ui.screens.LogWorkoutDetailScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.PrivatePhotosScreen
import com.example.ui.screens.ProgramEditorScreen
import com.example.ui.screens.ProgramsListScreen
import com.example.ui.screens.ProgressTabScreen
import com.example.ui.screens.SettingsMainScreen
import com.example.ui.screens.WatchAndSyncScreen
import com.example.ui.screens.WorkoutSummaryScreen
import com.example.ui.screens.WorkoutTabScreen
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: TrainPilotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val currentPalette = TrainPilotPalettes.getById(uiState.selectedPaletteId)

            TrainPilotTheme(
                palette = currentPalette,
                fontScaleMultiplier = uiState.fontScaleMultiplier
            ) {
                TrainPilotAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TrainPilotAppRoot(viewModel: TrainPilotViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalTrainPilotPalette.current
    val strings = TrainPilotStrings.forLanguage(uiState.language)

    val canNavigateBack = uiState.currentSubScreen != SubScreen.NONE ||
        uiState.currentTab != MainTab.HOME ||
        uiState.backStack.isNotEmpty()

    BackHandler(enabled = canNavigateBack) {
        viewModel.navigateBack()
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        if (uiState.statusBannerMessage != null) {
            delay(3200)
            viewModel.clearBannerMessage()
        }
    }

    CompositionLocalProvider(
        LocalOpenExerciseVideo provides { exTitle -> viewModel.openExerciseVideoModal(exTitle) }
    ) {
        // 0. Embedded YouTube Exercise Video Modal Dialog
        uiState.videoModalExerciseTitle?.let { exTitle ->
            ExerciseYouTubeVideoModalDialog(
                exerciseTitle = exTitle,
                onDismiss = { viewModel.closeExerciseVideoModal() }
            )
        }

        // 1. 32 Color Palette Picker Bottom Sheet
        if (uiState.showPalettePickerSheet) {
            PalettePickerBottomSheet(
                selectedPaletteId = uiState.selectedPaletteId,
                selectedCategory = uiState.paletteFilterCategory,
                currentLanguage = uiState.language,
                fontScaleMultiplier = uiState.fontScaleMultiplier,
                compactPhoneMode = uiState.compactPhoneMode,
                onSelectPalette = { viewModel.selectPalette(it) },
                onSelectCategory = { viewModel.setPaletteFilterCategory(it) },
                onSelectLanguage = { viewModel.selectLanguage(it) },
                onSelectFontScale = { viewModel.setFontScaleMultiplier(it) },
                onToggleCompactMode = { viewModel.toggleCompactPhoneMode(it) },
                onDismiss = { viewModel.togglePalettePickerSheet(false) }
            )
        }

        // 2. Goals Edit Dialog (Célok beállítása)
        if (uiState.showGoalsDialog) {
            GoalsEditDialog(
                currentWeeklyWorkouts = uiState.weeklyGoalTarget,
                currentStepsGoal = uiState.stepsGoal,
                onSave = { weekly, steps -> viewModel.updateGoals(weekly, steps) },
                onDismiss = { viewModel.toggleGoalsDialog(false) }
            )
        }

        // 3. Personal Planner Modal Dialog (Személyes edzéstervező)
        if (uiState.showPersonalPlannerModal) {
            PersonalPlannerModalDialog(
                currentStyle = uiState.trainingStyleHu,
                currentRule = uiState.progressionRuleHu,
                gentleBeginner = uiState.beginnerGentleProfileActive,
                onSaveAndOpenPrograms = { style, rule, gentle ->
                    viewModel.updateProgressionSettings(style, rule, gentle)
                    viewModel.togglePersonalPlannerModal(false)
                    viewModel.navigateToSubScreen(SubScreen.PROGRAMS_LIST, MainTab.WORKOUT)
                },
                onDismiss = { viewModel.togglePersonalPlannerModal(false) }
            )
        }

        // 4. Quick Workout Sheet (Gyors edzés indítása)
        if (uiState.showQuickWorkoutSheet) {
            QuickWorkoutSheet(
                onStartStrengthQuick = {
                    viewModel.toggleQuickWorkoutSheet(false)
                    viewModel.navigateToSubScreen(SubScreen.ACTIVE_STRENGTH, MainTab.WORKOUT)
                },
                onStartTimedQuick = {
                    viewModel.toggleQuickWorkoutSheet(false)
                    viewModel.navigateToSubScreen(SubScreen.ACTIVE_TIMED, MainTab.WORKOUT)
                },
                onStartOutdoorGps = {
                    viewModel.toggleQuickWorkoutSheet(false)
                    viewModel.navigateToSubScreen(SubScreen.ACTIVE_OUTDOOR_GPS, MainTab.WORKOUT)
                },
                onBrowseExercises = {
                    viewModel.toggleQuickWorkoutSheet(false)
                    viewModel.navigateToSubScreen(SubScreen.EXERCISE_LIBRARY, MainTab.WORKOUT)
                },
                onDismiss = { viewModel.toggleQuickWorkoutSheet(false) }
            )
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background),
            containerColor = palette.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TrainPilotGlobalTopHeaderAndNav(
                    currentTab = uiState.currentTab,
                    currentSubScreen = uiState.currentSubScreen,
                    currentLanguage = uiState.language,
                    selectedPaletteId = uiState.selectedPaletteId,
                    strings = strings,
                    onSelectTab = { tab -> viewModel.selectTab(tab) },
                    onNavigateToSubScreen = { sub, tab -> viewModel.navigateToSubScreen(sub, tab) },
                    onSelectLanguage = { lang -> viewModel.selectLanguage(lang) },
                    onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) },
                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) }
                )
            }
        ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val contentMaxWidth = if (uiState.compactPhoneMode) 360.dp else 600.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = contentMaxWidth)
            ) {
                when (uiState.currentSubScreen) {
                    SubScreen.PROGRAMS_LIST -> {
                        ProgramsListScreen(
                            state = uiState,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onToggleCustomTab = { viewModel.setProgramsTabCustomOnly(it) },
                            onActivateProgram = { viewModel.activateProgram(it) },
                            onOpenProgramEditor = { progId -> viewModel.openProgramEditor(progId, 0) },
                            onOpenExerciseLibrary = { viewModel.navigateToSubScreen(SubScreen.EXERCISE_LIBRARY) }
                        )
                    }

                    SubScreen.PROGRAM_EDITOR -> {
                        ProgramEditorScreen(
                            state = uiState,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectDayIndex = { viewModel.selectEditingDayIndex(it) },
                            onToggleExpandExercise = { viewModel.toggleExpandedEditorExercise(it) },
                            onSaveExercisePrescription = { exId, sets, reps, kg, rest ->
                                viewModel.updateProgramExercisePrescription(exId, sets, reps, kg, rest)
                            },
                            onOpenExerciseDetail = { exId -> viewModel.openExerciseDetail(exId) },
                            onOpenExerciseLibrary = { viewModel.navigateToSubScreen(SubScreen.EXERCISE_LIBRARY) }
                        )
                    }

                    SubScreen.CALENDAR_PLANNER -> {
                        CalendarPlannerScreen(
                            state = uiState,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectDay = { viewModel.selectCalendarDay(it) },
                            onOpenReplanModal = { viewModel.toggleReplanModal(it) },
                            onApplyReplan = { dateStr, firstDay ->
                                viewModel.applyCalendarReplan(dateStr, firstDay)
                            },
                            onOpenWorkoutDetail = {
                                viewModel.openLogWorkoutDetail("log_4")
                            }
                        )
                    }

                    SubScreen.EXERCISE_LIBRARY -> {
                        ExerciseLibraryScreen(
                            state = uiState,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSearchQueryChange = { viewModel.updateExerciseSearch(it) },
                            onSelectCategory = { viewModel.selectExerciseCategory(it) },
                            onSelectExercise = { exId -> viewModel.openExerciseDetail(exId) }
                        )
                    }

                    SubScreen.EXERCISE_DETAIL -> {
                        ExerciseDetailScreen(
                            state = uiState,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onAddExerciseToWorkout = { ex ->
                                viewModel.addExerciseToActiveProgramDay(ex)
                            },
                            onStartDemoWithExercise = {
                                viewModel.navigateToSubScreen(SubScreen.ACTIVE_STRENGTH, MainTab.WORKOUT)
                            }
                        )
                    }

                    SubScreen.ACTIVE_STRENGTH -> {
                        ActiveStrengthWorkoutScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectMeasurementMode = { mode -> viewModel.navigateToSubScreen(mode, MainTab.WORKOUT) },
                            onToggleSetCompleted = { setNum -> viewModel.toggleSetCompleted(setNum) },
                            onAdjustSetValues = { setNum, dKg, dReps, dRir ->
                                viewModel.adjustSetValues(setNum, dKg, dReps, dRir)
                            },
                            onUndoLastSet = { viewModel.undoLastCompletedSet() },
                            onAdjustRestTimer = { delta -> viewModel.adjustRestTimer(delta) },
                            onToggleRestRunning = { viewModel.toggleRestTimerRunning() },
                            onSkipRest = { viewModel.skipRestTimer() },
                            onOpenExerciseDetail = { exId -> viewModel.openExerciseDetail(exId) }
                        )
                    }

                    SubScreen.ACTIVE_TIMED -> {
                        ActiveTimedWorkoutScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectMeasurementMode = { mode -> viewModel.navigateToSubScreen(mode, MainTab.WORKOUT) }
                        )
                    }

                    SubScreen.ACTIVE_BILATERAL -> {
                        ActiveBilateralWorkoutScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectMeasurementMode = { mode -> viewModel.navigateToSubScreen(mode, MainTab.WORKOUT) }
                        )
                    }

                    SubScreen.ACTIVE_OUTDOOR_GPS -> {
                        ActiveOutdoorGpsScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectMeasurementMode = { mode -> viewModel.navigateToSubScreen(mode, MainTab.WORKOUT) }
                        )
                    }

                    SubScreen.INTERVAL_SETTINGS -> {
                        IntervalSettingsScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectMeasurementMode = { mode -> viewModel.navigateToSubScreen(mode, MainTab.WORKOUT) },
                            onSaveAndStartIntervals = { work, rest, rounds, sound, vib, auto ->
                                viewModel.updateIntervalSettings(work, rest, rounds, sound, vib, auto)
                            }
                        )
                    }

                    SubScreen.WORKOUT_SUMMARY -> {
                        WorkoutSummaryScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onSelectMood = { viewModel.selectWorkoutSummaryMood(it) },
                            onUpdateNote = { viewModel.updateWorkoutSummaryNote(it) },
                            onSaveWorkout = { viewModel.finishAndSaveWorkout() },
                            onPauseAndExit = {
                                viewModel.cycleHomeDemoMode(HomeDemoMode.PAUSED_WORKOUT)
                                viewModel.selectTab(MainTab.HOME)
                            },
                            onDiscardWorkout = {
                                viewModel.showBannerMessage("DEMO • Edzés elvetve")
                                viewModel.selectTab(MainTab.HOME)
                            }
                        )
                    }

                    SubScreen.LOG_WORKOUT_DETAIL -> {
                        LogWorkoutDetailScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectDetailTab = { viewModel.selectLogDetailTab(it) },
                            onToggleExpandedExercise = { viewModel.toggleExpandedLogExercise(it) },
                            onOpenPhotos = { viewModel.navigateToSubScreen(SubScreen.PRIVATE_PHOTOS) },
                            onOpenRouteMap = { viewModel.navigateToSubScreen(SubScreen.LOG_ROUTE_MAP) },
                            onOpenWatchSync = { viewModel.navigateToSubScreen(SubScreen.WATCH_AND_SYNC) },
                            onOpenHealthTab = { viewModel.selectTab(MainTab.HEALTH) }
                        )
                    }

                    SubScreen.LOG_ROUTE_MAP -> {
                        LogRouteMapScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() }
                        )
                    }

                    SubScreen.COACH_DETAIL -> {
                        CoachDetailScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onStartSuggestedWorkout = {
                                viewModel.navigateToSubScreen(SubScreen.ACTIVE_STRENGTH, MainTab.WORKOUT)
                            },
                            onOpenExerciseDetail = { exId -> viewModel.openExerciseDetail(exId) }
                        )
                    }

                    SubScreen.SETTINGS_MAIN -> {
                        SettingsMainScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) },
                            onSelectLanguage = { viewModel.selectLanguage(it) },
                            onCycleRestTimer = { viewModel.cycleDefaultRestTimer() },
                            onToggleSoundAlert = { viewModel.toggleSoundAlert(it) },
                            onToggleProgressionModal = { viewModel.toggleProgressionModal(it) },
                            onSaveProgression = { style, rule, gentle ->
                                viewModel.updateProgressionSettings(style, rule, gentle)
                            },
                            onNavigateToSubScreen = { viewModel.navigateToSubScreen(it) },
                            onShowBanner = { viewModel.showBannerMessage(it) }
                        )
                    }

                    SubScreen.WATCH_AND_SYNC -> {
                        WatchAndSyncScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onToggleControlMode = { viewModel.toggleWatchControlMode() },
                            onShowBanner = { viewModel.showBannerMessage(it) }
                        )
                    }

                    SubScreen.BACKUP_RESTORE -> {
                        BackupRestoreScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectFormat = { viewModel.selectBackupFormat(it) },
                            onToggleIncludeHealth = { viewModel.toggleIncludeHealthInBackup(it) },
                            onShowBanner = { viewModel.showBannerMessage(it) }
                        )
                    }

                    SubScreen.PERMISSIONS_SCREEN -> {
                        PermissionsScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onTogglePermission = { viewModel.togglePermission(it) }
                        )
                    }

                    SubScreen.PRIVATE_PHOTOS -> {
                        PrivatePhotosScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onShowBanner = { viewModel.showBannerMessage(it) }
                        )
                    }

                    SubScreen.NONE -> {
                        when (uiState.currentTab) {
                            MainTab.HOME -> {
                                HomeScreen(
                                    state = uiState,
                                    strings = strings,
                                    onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) },
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onCycleDemoMode = { mode -> viewModel.cycleHomeDemoMode(mode) },
                                    onStartOrContinueWorkout = {
                                        viewModel.navigateToSubScreen(SubScreen.ACTIVE_STRENGTH, MainTab.WORKOUT)
                                    },
                                    onOpenProgramDayDetails = {
                                        viewModel.openProgramEditor("home_ab_basic", 0)
                                    },
                                    onOpenQuickWorkout = { viewModel.toggleQuickWorkoutSheet(true) },
                                    onOpenWatchAndSync = { viewModel.navigateToSubScreen(SubScreen.WATCH_AND_SYNC) },
                                    onOpenHealthSleep = {
                                        viewModel.openHealthAndExpand(journal = true)
                                    },
                                    onOpenHealthHeart = {
                                        viewModel.openHealthAndExpand(pulse = true)
                                    },
                                    onOpenHealthSteps = {
                                        viewModel.openHealthAndExpand(journal = true)
                                    },
                                    onOpenCoachReadiness = {
                                        viewModel.navigateToSubScreen(SubScreen.COACH_DETAIL)
                                    },
                                    onOpenProgramsList = {
                                        viewModel.navigateToSubScreen(SubScreen.PROGRAMS_LIST, MainTab.WORKOUT)
                                    },
                                    onOpenCalendarPlanner = {
                                        viewModel.navigateToSubScreen(SubScreen.CALENDAR_PLANNER)
                                    },
                                    onOpenGoalsDialog = { viewModel.toggleGoalsDialog(true) },
                                    onOpenPersonalPlanner = { viewModel.togglePersonalPlannerModal(true) }
                                )
                            }

                            MainTab.WORKOUT -> {
                                WorkoutTabScreen(
                                    state = uiState,
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onOpenQuickWorkout = { viewModel.toggleQuickWorkoutSheet(true) },
                                    onOpenExerciseLibrary = { viewModel.navigateToSubScreen(SubScreen.EXERCISE_LIBRARY) },
                                    onOpenOutdoorWorkout = { viewModel.navigateToSubScreen(SubScreen.ACTIVE_OUTDOOR_GPS) },
                                    onOpenProgramsList = { viewModel.navigateToSubScreen(SubScreen.PROGRAMS_LIST) },
                                    onOpenProgramEditor = { progId, dayIdx -> viewModel.openProgramEditor(progId, dayIdx) },
                                    onStartWorkoutDay = { _ ->
                                        viewModel.navigateToSubScreen(SubScreen.ACTIVE_STRENGTH)
                                    },
                                    onOpenCalendar = { viewModel.navigateToSubScreen(SubScreen.CALENDAR_PLANNER) },
                                    onOpenCoach = { viewModel.navigateToSubScreen(SubScreen.COACH_DETAIL) },
                                    onOpenProgressionModal = { viewModel.togglePersonalPlannerModal(true) }
                                )
                            }

                            MainTab.LOG -> {
                                LogTabScreen(
                                    uiState = uiState,
                                    strings = strings,
                                    onSearchChange = { viewModel.updateLogSearch(it) },
                                    onOpenWorkoutDetail = { id -> viewModel.openLogWorkoutDetail(id) },
                                    onOpenRouteMap = { viewModel.navigateToSubScreen(SubScreen.LOG_ROUTE_MAP) },
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) }
                                )
                            }

                            MainTab.PROGRESS -> {
                                ProgressTabScreen(
                                    uiState = uiState,
                                    strings = strings,
                                    onSelectMainSection = { viewModel.selectProgressMainSection(it) },
                                    onSelectExercise = { viewModel.selectProgressExercise(it) },
                                    onSelectRange = { viewModel.selectProgressRange(it) },
                                    onSelectVolumeBar = { viewModel.selectVolumeBar(it) },
                                    onSelectExpandedStatsCard = { viewModel.selectExpandedStatsCard(it) },
                                    onSelectStatsSubTab = { viewModel.selectStatsSubTab(it) },
                                    onToggleCardioFilterStepsMode = { viewModel.toggleCardioFilterStepsMode(it) },
                                    onOpenWorkoutDetail = { id -> viewModel.openLogWorkoutDetail(id) },
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) }
                                )
                            }

                            MainTab.HEALTH -> {
                                HealthTabScreen(
                                    uiState = uiState,
                                    strings = strings,
                                    onToggleHealthSection = { viewModel.toggleHealthSection(it) },
                                    onSelectWeightRange = { viewModel.selectWeightRange(it) },
                                    onToggleAddWeightDialog = { viewModel.toggleAddWeightDialog(it) },
                                    onSaveNewWeight = { viewModel.saveNewBodyWeight(it) },
                                    onTogglePrimaryHealthSource = { viewModel.togglePrimaryHealthSource() },
                                    onTriggerHealthSync = { viewModel.triggerHealthSyncNow() },
                                    onNavigateToSubScreen = { viewModel.navigateToSubScreen(it) },
                                    onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) }
                                )
                            }
                        }
                    }
                }
            }

            // Status / Toast Banner Pill at bottom-top
            AnimatedVisibility(
                visible = uiState.statusBannerMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surfaceElevated,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .border(1.5.dp, palette.primary, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.clearBannerMessage() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.statusBannerMessage.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
    }
}
