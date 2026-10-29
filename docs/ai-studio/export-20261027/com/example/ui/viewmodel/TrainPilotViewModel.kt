package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.DemoDataProvider
import com.example.model.AppLanguage
import com.example.model.CalendarDayEvent
import com.example.model.CalendarEventStatus
import com.example.model.ExerciseCategory
import com.example.model.ExerciseItem
import com.example.model.ExerciseSetItem
import com.example.model.HomeDemoMode
import com.example.model.LoggedWorkoutEntry
import com.example.model.MainTab
import com.example.model.PrRecordEntry
import com.example.model.TrainingProgramModel
import com.example.model.SubScreen
import com.example.ui.theme.PaletteCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NavRoute(
    val tab: MainTab,
    val subScreen: SubScreen = SubScreen.NONE
)

data class TrainPilotUiState(
    // Theme & Localization & Accessibility
    val selectedPaletteId: Int = 1, // Default: 01 • Grafit & Arany
    val paletteFilterCategory: PaletteCategory? = null, // null = All 32
    val showPalettePickerSheet: Boolean = false,
    val language: AppLanguage = AppLanguage.HU,
    val fontScaleMultiplier: Float = 1.0f,
    val compactPhoneMode: Boolean = false,

    // Navigation & Backstack
    val currentTab: MainTab = MainTab.HOME,
    val currentSubScreen: SubScreen = SubScreen.NONE,
    val backStack: List<NavRoute> = emptyList(),

    // Home Screen State (Mockup 1/4 #1 + main Home)
    val homeDemoMode: HomeDemoMode = HomeDemoMode.PAUSED_WORKOUT,
    val weeklyGoalCompleted: Int = 1,
    val weeklyGoalTarget: Int = 15,
    val showGoalsDialog: Boolean = false,
    val showPersonalPlannerModal: Boolean = false,
    val showQuickWorkoutSheet: Boolean = false,
    val sleepDurationLabel: String = "7 ó 24 p",
    val sleepHoursDecimal: String = "6,2 h",
    val restingHrBpm: Int = 62,
    val currentHrBpm: Int = 65,
    val dailySteps: Int = 8421,
    val stepsGoal: Int = 10000,
    val activeCaloriesKcal: Int = 164,
    val samsungEnergyScore: Int = 87,
    val coachReadinessScore: Int = 54,

    // Watch & Sync State (Mockup 4/4 #2 + main)
    val watchName: String = "Galaxy Watch7",
    val watchConnected: Boolean = true,
    val watchBatteryPercent: Int = 92,
    val phoneControlsWorkout: Boolean = true,
    val offlineContinuationEnabled: Boolean = true,
    val lastWatchSyncLabel: String = "Ma 09:24",
    val autoDriveSync: Boolean = true,
    val autoCalendarSync: Boolean = true,
    val lastDriveSyncLabel: String = "2026. 10. 20. 05:29",
    val lastCalendarSyncLabel: String = "2026. 10. 27. 11:29",

    // Programs & Program Editor State (Mockup 1/4 #3 + 4/4 #6 + main)
    val programs: List<TrainingProgramModel> = DemoDataProvider.defaultPrograms,
    val programsTabCustomOnly: Boolean = true,
    val editingProgramId: String = "home_ab_basic",
    val editingDayIndex: Int = 0,
    val expandedEditorExerciseId: String? = "band_row",
    val showProgressionModal: Boolean = false,
    val trainingStyleHu: String = "Normál hipertrófia",
    val progressionRuleHu: String = "Dupla progresszió",
    val beginnerGentleProfileActive: Boolean = false,

    // Calendar & Replan State (Mockup 1/4 #4 + main Naptár & Újratervezés)
    val selectedCalendarDay: Int = 24,
    val calendarEvents: List<CalendarDayEvent> = DemoDataProvider.initialCalendarEvents,
    val showReplanModal: Boolean = false,
    val planningCadenceHu: String = "Minden másodnap",
    val planningFirstDayHu: String = "Alap A",
    val planningStartDate: String = "2026. 10. 27.",
    val planningTime: String = "10:00",
    val planningDurationMin: Int = 60,
    val planningWeeksCount: Int = 4,

    // Exercise Library & Detail State (Mockup 1/4 #5 & #6)
    val exercises: List<ExerciseItem> = DemoDataProvider.defaultExercises,
    val exerciseSearchQuery: String = "",
    val selectedExerciseCategory: ExerciseCategory = ExerciseCategory.ALL,
    val selectedExerciseId: String = "goblet_squat",
    val showAddCustomExerciseDialog: Boolean = false,

    // Active Workout & Measurement State (Mockup 2/4 #1..#6)
    val activeExerciseIndex: Int = 0,
    val activeSets: List<ExerciseSetItem> = listOf(
        ExerciseSetItem(1, 16f, 10, 2, true),
        ExerciseSetItem(2, 16f, 10, 1, true),
        ExerciseSetItem(3, 16f, 10, 1, false)
    ),
    val restTimerRemainingSec: Int = 52,
    val restTimerRunning: Boolean = true,
    val defaultRestSec: Int = 90,
    val soundAlertEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoStepIntervals: Boolean = true,
    val intervalWorkSec: Int = 30,
    val intervalRestSec: Int = 60,
    val intervalRounds: Int = 3,
    val timedCurrentSec: Int = 38,
    val bilateralLeftSec: Int = 25,
    val bilateralRightSec: Int = 18,
    val outdoorGpsEnabled: Boolean = true,
    val outdoorDistanceKm: Float = 1.62f,
    val outdoorElapsedLabel: String = "09:42",
    val outdoorPaceLabel: String = "6:00",
    val workoutSummaryMoodIndex: Int = 2, // 2 = "Pont jó"
    val workoutSummaryNote: String = "",

    // Log / Diary State (Mockup 3/4 #1..#3 + main)
    val logSearchQuery: String = "",
    val selectedLogMonth: String = "2026. október",
    val loggedWorkouts: List<LoggedWorkoutEntry> = DemoDataProvider.initialLoggedWorkouts,
    val selectedLogWorkoutId: String = "log_4",
    val logDetailTabIndex: Int = 0, // 0 = Gyakorlatok, 1 = Statisztikák, 2 = Forrás
    val expandedLogExerciseIndex: Int = 0,

    // Progress / Fejlődés State (Mockup 3/4 #4 + full main Fejlődés & Statisztikák)
    val progressMainSection: Int = 0, // 0 = Kompakt Fejlődés (main), 1 = Statisztikák (Rekordok & Kardió), 2 = 1RM Erőnlét
    val selectedProgressExercise: String = "Fekvenyomás",
    val selectedProgressRangeIndex: Int = 3, // 0 = 1 nap, 1 = 7 nap, 2 = 30 nap, 3 = 3 hó
    val selectedVolumeBarIndex: Int = 9,
    val expandedStatsCardIndex: Int = -1, // -1 = collapsed; 0 = PR rekordok (45), 1 = Edzések száma (25), 2 = Átlagos volumen (904,3 kg), 3 = Átl. edzésidő (45 min)
    val statsSubTab: Int = 0, // 0 = Személyes rekordok, 1 = Kardióstatisztika
    val cardioFilterStepsMode: Boolean = false, // false = Minden mozgás, true = Napi aktivitás / lépések
    val prRecords: List<PrRecordEntry> = DemoDataProvider.prRecords,
    val videoModalExerciseTitle: String? = null,

    // Health / Egészség State (Mockup 3/4 #5 + full main Egészség)
    val expandedHealthPulseTrend: Boolean = false,
    val expandedHealthWeightLog: Boolean = false,
    val expandedHealthExtraData: Boolean = false,
    val expandedHealthJournal: Boolean = false,
    val expandedHealthConnect: Boolean = false,
    val selectedWeightRangeIndex: Int = 1, // 0=7d, 1=30d, 2=3m, 3=1y, 4=All
    val currentBodyWeightKg: Float = 65.0f,
    val bodyFatPercent: Float = 18.3f,
    val spo2Percent: Float = 97.0f,
    val vo2Max: Float = 38.6f,
    val showAddWeightDialog: Boolean = false,
    val primaryHealthSourceSamsung: Boolean = true,

    // Permissions & Backup State (Mockup 4/4 #3, #4, #5)
    val selectedBackupFormat: String = "JSON", // JSON, ZIP, DRIVE
    val includeHealthInBackup: Boolean = true,
    val permissionGpsEnabled: Boolean = true,
    val permissionCentralLocEnabled: Boolean = true,
    val permissionNotificationsEnabled: Boolean = true,
    val permissionHealthConnectEnabled: Boolean = true,
    val permissionSamsungHealthEnabled: Boolean = false,
    val privatePhotoCount: Int = 2,

    // Toast / Feedback banner for immediate visual confirmation of actions
    val statusBannerMessage: String? = null
)

class TrainPilotViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TrainPilotUiState())
    val uiState: StateFlow<TrainPilotUiState> = _uiState.asStateFlow()

    fun selectTab(tab: MainTab) {
        _uiState.update { state ->
            if (state.currentTab == tab && state.currentSubScreen == SubScreen.NONE) {
                state
            } else {
                val updatedStack = (state.backStack + NavRoute(state.currentTab, state.currentSubScreen)).takeLast(16)
                state.copy(
                    currentTab = tab,
                    currentSubScreen = SubScreen.NONE,
                    backStack = updatedStack
                )
            }
        }
    }

    fun navigateToSubScreen(subScreen: SubScreen, targetTab: MainTab? = null) {
        _uiState.update { state ->
            val updatedStack = (state.backStack + NavRoute(state.currentTab, state.currentSubScreen)).takeLast(16)
            state.copy(
                currentTab = targetTab ?: state.currentTab,
                currentSubScreen = subScreen,
                backStack = updatedStack
            )
        }
    }

    fun navigateBack(): Boolean {
        val current = _uiState.value
        if (current.backStack.isNotEmpty()) {
            val previous = current.backStack.last()
            _uiState.update {
                it.copy(
                    currentTab = previous.tab,
                    currentSubScreen = previous.subScreen,
                    backStack = it.backStack.dropLast(1)
                )
            }
            return true
        } else if (current.currentSubScreen != SubScreen.NONE) {
            _uiState.update { it.copy(currentSubScreen = SubScreen.NONE) }
            return true
        } else if (current.currentTab != MainTab.HOME) {
            _uiState.update { it.copy(currentTab = MainTab.HOME) }
            return true
        }
        return false
    }

    fun selectPalette(paletteId: Int) {
        _uiState.update {
            it.copy(
                selectedPaletteId = paletteId,
                statusBannerMessage = "DEMO • Színpaletta frissítve (#$paletteId)"
            )
        }
    }

    fun setPaletteFilterCategory(category: PaletteCategory?) {
        _uiState.update { it.copy(paletteFilterCategory = category) }
    }

    fun togglePalettePickerSheet(show: Boolean) {
        _uiState.update { it.copy(showPalettePickerSheet = show) }
    }

    fun selectLanguage(lang: AppLanguage) {
        _uiState.update {
            it.copy(
                language = lang,
                statusBannerMessage = "DEMO • Nyelv: ${lang.label} (${lang.code})"
            )
        }
    }

    fun setFontScaleMultiplier(scale: Float) {
        _uiState.update { it.copy(fontScaleMultiplier = scale) }
    }

    fun toggleCompactPhoneMode(compact: Boolean) {
        _uiState.update { it.copy(compactPhoneMode = compact) }
    }

    fun cycleHomeDemoMode(mode: HomeDemoMode) {
        _uiState.update {
            val readiness = if (mode == HomeDemoMode.COACH_LIGHT_DAY) 54 else 87
            it.copy(
                homeDemoMode = mode,
                coachReadinessScore = readiness,
                samsungEnergyScore = if (mode == HomeDemoMode.COACH_LIGHT_DAY) 61 else 87
            )
        }
    }

    fun toggleGoalsDialog(show: Boolean) {
        _uiState.update { it.copy(showGoalsDialog = show) }
    }

    fun updateGoals(weeklyTarget: Int, stepsTarget: Int) {
        _uiState.update {
            it.copy(
                weeklyGoalTarget = weeklyTarget.coerceIn(1, 30),
                stepsGoal = stepsTarget.coerceIn(2000, 30000),
                showGoalsDialog = false,
                statusBannerMessage = "DEMO • Célok elmentve: $weeklyTarget edzés, $stepsTarget lépés"
            )
        }
    }

    fun togglePersonalPlannerModal(show: Boolean) {
        _uiState.update { it.copy(showPersonalPlannerModal = show) }
    }

    fun toggleQuickWorkoutSheet(show: Boolean) {
        _uiState.update { it.copy(showQuickWorkoutSheet = show) }
    }

    // Program & Exercise Editor Actions
    fun setProgramsTabCustomOnly(customOnly: Boolean) {
        _uiState.update { it.copy(programsTabCustomOnly = customOnly) }
    }

    fun activateProgram(programId: String) {
        _uiState.update { state ->
            val updated = state.programs.map { prog ->
                prog.copy(isActive = prog.id == programId)
            }
            state.copy(
                programs = updated,
                editingProgramId = programId,
                statusBannerMessage = "DEMO • Aktív program frissítve"
            )
        }
    }

    fun openProgramEditor(programId: String, dayIndex: Int = 0) {
        _uiState.update {
            val back = (it.backStack + NavRoute(it.currentTab, it.currentSubScreen)).takeLast(16)
            it.copy(
                editingProgramId = programId,
                editingDayIndex = dayIndex,
                currentSubScreen = SubScreen.PROGRAM_EDITOR,
                backStack = back
            )
        }
    }

    fun selectEditingDayIndex(dayIndex: Int) {
        _uiState.update { it.copy(editingDayIndex = dayIndex) }
    }

    fun toggleExpandedEditorExercise(exerciseId: String) {
        _uiState.update {
            it.copy(
                expandedEditorExerciseId = if (it.expandedEditorExerciseId == exerciseId) null else exerciseId
            )
        }
    }

    fun updateProgramExercisePrescription(
        exerciseId: String,
        sets: Int,
        reps: Int,
        weightKg: Float,
        restSec: Int
    ) {
        _uiState.update { state ->
            val updatedPrograms = state.programs.map { prog ->
                if (prog.id != state.editingProgramId) prog
                else {
                    val updatedDays = prog.days.mapIndexed { idx, day ->
                        if (idx != state.editingDayIndex) day
                        else {
                            day.copy(
                                exercises = day.exercises.map { ex ->
                                    if (ex.id != exerciseId) ex
                                    else ex.copy(
                                        defaultSets = sets.coerceIn(1, 10),
                                        defaultReps = reps.coerceIn(1, 50),
                                        defaultWeightKg = weightKg.coerceAtLeast(0f),
                                        defaultRestSec = restSec.coerceIn(15, 300)
                                    )
                                }
                            )
                        }
                    }
                    prog.copy(days = updatedDays)
                }
            }
            state.copy(
                programs = updatedPrograms,
                expandedEditorExerciseId = null,
                statusBannerMessage = "DEMO • Gyakorlat beállításai elmentve"
            )
        }
    }

    fun addExerciseToActiveProgramDay(exercise: ExerciseItem) {
        _uiState.update { state ->
            val updatedPrograms = state.programs.map { prog ->
                if (prog.id != state.editingProgramId) prog
                else {
                    val updatedDays = prog.days.mapIndexed { idx, day ->
                        if (idx != state.editingDayIndex) day
                        else if (day.exercises.any { it.id == exercise.id }) day
                        else day.copy(exercises = day.exercises + exercise)
                    }
                    prog.copy(days = updatedDays)
                }
            }
            state.copy(
                programs = updatedPrograms,
                statusBannerMessage = "DEMO • ${exercise.nameHu} hozzáadva az edzésnaphoz"
            )
        }
    }

    // Calendar & Replan Actions
    fun selectCalendarDay(day: Int) {
        _uiState.update { it.copy(selectedCalendarDay = day) }
    }

    fun toggleReplanModal(show: Boolean) {
        _uiState.update { it.copy(showReplanModal = show) }
    }

    fun applyCalendarReplan(newStartDate: String, firstDayHu: String) {
        _uiState.update { state ->
            val shiftedEvents = state.calendarEvents.map { ev ->
                if (ev.status == CalendarEventStatus.PLANNED && ev.dayOfMonth >= 27) {
                    ev.copy(dayOfMonth = (ev.dayOfMonth + 1).coerceAtMost(31))
                } else ev
            }
            state.copy(
                planningStartDate = newStartDate,
                planningFirstDayHu = firstDayHu,
                calendarEvents = shiftedEvents,
                showReplanModal = false,
                statusBannerMessage = "DEMO • Edzésterv újratervezve ($newStartDate • $firstDayHu)"
            )
        }
    }

    // Exercise Library Actions
    fun updateExerciseSearch(query: String) {
        _uiState.update { it.copy(exerciseSearchQuery = query) }
    }

    fun selectExerciseCategory(cat: ExerciseCategory) {
        _uiState.update { it.copy(selectedExerciseCategory = cat) }
    }

    fun openExerciseDetail(exerciseId: String) {
        _uiState.update {
            val back = (it.backStack + NavRoute(it.currentTab, it.currentSubScreen)).takeLast(16)
            it.copy(
                selectedExerciseId = exerciseId,
                currentSubScreen = SubScreen.EXERCISE_DETAIL,
                backStack = back
            )
        }
    }

    // Active Workout Actions
    fun toggleSetCompleted(setNumber: Int) {
        _uiState.update { state ->
            val updated = state.activeSets.map { s ->
                if (s.setNumber == setNumber) s.copy(completed = !s.completed) else s
            }
            state.copy(
                activeSets = updated,
                restTimerRemainingSec = state.defaultRestSec,
                restTimerRunning = true
            )
        }
    }

    fun adjustSetValues(setNumber: Int, deltaWeight: Float, deltaReps: Int, deltaRir: Int) {
        _uiState.update { state ->
            val updated = state.activeSets.map { s ->
                if (s.setNumber == setNumber) {
                    s.copy(
                        weightKg = (s.weightKg + deltaWeight).coerceAtLeast(0f),
                        reps = (s.reps + deltaReps).coerceIn(1, 50),
                        rir = (s.rir + deltaRir).coerceIn(0, 10)
                    )
                } else s
            }
            state.copy(activeSets = updated)
        }
    }

    fun undoLastCompletedSet() {
        _uiState.update { state ->
            val lastCompleted = state.activeSets.lastOrNull { it.completed }
            if (lastCompleted == null) state
            else {
                state.copy(
                    activeSets = state.activeSets.map {
                        if (it.setNumber == lastCompleted.setNumber) it.copy(completed = false) else it
                    },
                    statusBannerMessage = "DEMO • ${lastCompleted.setNumber}. sorozat visszavonva"
                )
            }
        }
    }

    fun adjustRestTimer(deltaSec: Int) {
        _uiState.update {
            it.copy(restTimerRemainingSec = (it.restTimerRemainingSec + deltaSec).coerceIn(0, 600))
        }
    }

    fun toggleRestTimerRunning() {
        _uiState.update { it.copy(restTimerRunning = !it.restTimerRunning) }
    }

    fun skipRestTimer() {
        _uiState.update { it.copy(restTimerRemainingSec = 0, restTimerRunning = false) }
    }

    fun updateIntervalSettings(workSec: Int, restSec: Int, rounds: Int, sound: Boolean, vibration: Boolean, autoStep: Boolean) {
        _uiState.update {
            it.copy(
                intervalWorkSec = workSec.coerceIn(10, 300),
                intervalRestSec = restSec.coerceIn(10, 300),
                intervalRounds = rounds.coerceIn(1, 20),
                soundAlertEnabled = sound,
                vibrationEnabled = vibration,
                autoStepIntervals = autoStep,
                statusBannerMessage = "DEMO • Intervallumok: ${workSec}mp munka / ${restSec}mp pihenő × $rounds"
            )
        }
    }

    fun selectWorkoutSummaryMood(index: Int) {
        _uiState.update { it.copy(workoutSummaryMoodIndex = index) }
    }

    fun updateWorkoutSummaryNote(note: String) {
        _uiState.update { it.copy(workoutSummaryNote = note) }
    }

    fun finishAndSaveWorkout() {
        _uiState.update { state ->
            val newEntry = LoggedWorkoutEntry(
                id = "log_${state.loggedWorkouts.size + 1}",
                dateLabelHu = "okt. 27., K • Most",
                titleHu = "Teljes test A (DEMO mentett)",
                durationMinutes = 38,
                workTimeLabel = "26:40",
                restTimeLabel = "11:44",
                totalSets = state.activeSets.count { it.completed }.coerceAtLeast(3),
                totalVolumeKg = 1280,
                summaryExercisesHu = "Goblet guggolás, Fekvőtámasz, Evezés gumiszalaggal",
                hasPr = true
            )
            state.copy(
                loggedWorkouts = listOf(newEntry) + state.loggedWorkouts,
                weeklyGoalCompleted = state.weeklyGoalCompleted + 1,
                homeDemoMode = HomeDemoMode.PLANNED_NEXT,
                currentTab = MainTab.LOG,
                currentSubScreen = SubScreen.NONE,
                statusBannerMessage = "DEMO • Edzés elmentve a Naplóba!"
            )
        }
    }

    // Log / Diary Actions
    fun updateLogSearch(query: String) {
        _uiState.update { it.copy(logSearchQuery = query) }
    }

    fun openLogWorkoutDetail(workoutId: String) {
        _uiState.update {
            val back = (it.backStack + NavRoute(it.currentTab, it.currentSubScreen)).takeLast(16)
            it.copy(
                selectedLogWorkoutId = workoutId,
                currentSubScreen = SubScreen.LOG_WORKOUT_DETAIL,
                backStack = back
            )
        }
    }

    fun selectLogDetailTab(index: Int) {
        _uiState.update { it.copy(logDetailTabIndex = index) }
    }

    fun toggleExpandedLogExercise(index: Int) {
        _uiState.update {
            it.copy(expandedLogExerciseIndex = if (it.expandedLogExerciseIndex == index) -1 else index)
        }
    }

    // Progress / Fejlődés Actions
    fun selectProgressMainSection(section: Int) {
        _uiState.update { it.copy(progressMainSection = section) }
    }

    fun selectProgressExercise(name: String) {
        _uiState.update { it.copy(selectedProgressExercise = name) }
    }

    fun selectProgressRange(index: Int) {
        _uiState.update { it.copy(selectedProgressRangeIndex = index) }
    }

    fun selectVolumeBar(index: Int) {
        _uiState.update { it.copy(selectedVolumeBarIndex = index) }
    }

    fun selectExpandedStatsCard(index: Int) {
        _uiState.update {
            it.copy(expandedStatsCardIndex = if (it.expandedStatsCardIndex == index) -1 else index)
        }
    }

    fun selectStatsSubTab(index: Int) {
        _uiState.update { it.copy(statsSubTab = index) }
    }

    fun toggleCardioFilterStepsMode(stepsMode: Boolean) {
        _uiState.update { it.copy(cardioFilterStepsMode = stepsMode) }
    }

    fun openExerciseVideoModal(exerciseTitle: String) {
        _uiState.update { it.copy(videoModalExerciseTitle = exerciseTitle) }
    }

    fun closeExerciseVideoModal() {
        _uiState.update { it.copy(videoModalExerciseTitle = null) }
    }

    // Health / Egészség Actions
    fun openHealthAndExpand(
        pulse: Boolean = false,
        weight: Boolean = false,
        extra: Boolean = false,
        journal: Boolean = false,
        connect: Boolean = false
    ) {
        _uiState.update { state ->
            val back = (state.backStack + NavRoute(state.currentTab, state.currentSubScreen)).takeLast(16)
            state.copy(
                currentTab = MainTab.HEALTH,
                currentSubScreen = SubScreen.NONE,
                backStack = back,
                expandedHealthPulseTrend = pulse || state.expandedHealthPulseTrend,
                expandedHealthWeightLog = weight || state.expandedHealthWeightLog,
                expandedHealthExtraData = extra || state.expandedHealthExtraData,
                expandedHealthJournal = journal || state.expandedHealthJournal,
                expandedHealthConnect = connect || state.expandedHealthConnect
            )
        }
    }

    fun toggleHealthSection(section: String) {
        _uiState.update { state ->
            when (section) {
                "pulse" -> state.copy(expandedHealthPulseTrend = !state.expandedHealthPulseTrend)
                "weight" -> state.copy(expandedHealthWeightLog = !state.expandedHealthWeightLog)
                "extra" -> state.copy(expandedHealthExtraData = !state.expandedHealthExtraData)
                "journal" -> state.copy(expandedHealthJournal = !state.expandedHealthJournal)
                "connect" -> state.copy(expandedHealthConnect = !state.expandedHealthConnect)
                else -> state
            }
        }
    }

    fun selectWeightRange(index: Int) {
        _uiState.update { it.copy(selectedWeightRangeIndex = index) }
    }

    fun toggleAddWeightDialog(show: Boolean) {
        _uiState.update { it.copy(showAddWeightDialog = show) }
    }

    fun saveNewBodyWeight(weightKg: Float) {
        _uiState.update {
            it.copy(
                currentBodyWeightKg = weightKg,
                showAddWeightDialog = false,
                statusBannerMessage = "DEMO • Új testsúlymérés rögzítve: $weightKg kg"
            )
        }
    }

    fun togglePrimaryHealthSource() {
        _uiState.update {
            val next = !it.primaryHealthSourceSamsung
            it.copy(
                primaryHealthSourceSamsung = next,
                statusBannerMessage = if (next) "DEMO • Elsődleges forrás: Samsung Health" else "DEMO • Elsődleges forrás: Health Connect"
            )
        }
    }

    fun triggerHealthSyncNow() {
        _uiState.update {
            it.copy(
                dailySteps = it.dailySteps + 120,
                statusBannerMessage = "DEMO • Samsung Health és Health Connect szinkronizálva"
            )
        }
    }

    // Watch, Settings, Backup & Permissions Actions
    fun toggleWatchControlMode() {
        _uiState.update {
            val next = !it.phoneControlsWorkout
            it.copy(
                phoneControlsWorkout = next,
                lastWatchSyncLabel = "Most • Naprakész",
                statusBannerMessage = if (next) "DEMO • Telefon vezérli az edzéseket" else "DEMO • Edzés átadva a Galaxy Watch7 órának"
            )
        }
    }

    fun toggleSoundAlert(enabled: Boolean) {
        _uiState.update { it.copy(soundAlertEnabled = enabled) }
    }

    fun cycleDefaultRestTimer() {
        _uiState.update {
            val next = when (it.defaultRestSec) {
                60 -> 90
                90 -> 120
                120 -> 180
                else -> 60
            }
            it.copy(defaultRestSec = next, statusBannerMessage = "DEMO • Alapértelmezett pihenőidő: $next mp")
        }
    }

    fun toggleProgressionModal(show: Boolean) {
        _uiState.update { it.copy(showProgressionModal = show) }
    }

    fun updateProgressionSettings(styleHu: String, ruleHu: String, gentleBeginner: Boolean) {
        _uiState.update {
            it.copy(
                trainingStyleHu = styleHu,
                progressionRuleHu = ruleHu,
                beginnerGentleProfileActive = gentleBeginner,
                showProgressionModal = false,
                statusBannerMessage = "DEMO • Progresszió: $styleHu • $ruleHu"
            )
        }
    }

    fun selectBackupFormat(format: String) {
        _uiState.update { it.copy(selectedBackupFormat = format) }
    }

    fun toggleIncludeHealthInBackup(include: Boolean) {
        _uiState.update { it.copy(includeHealthInBackup = include) }
    }

    fun togglePermission(key: String) {
        _uiState.update { state ->
            when (key) {
                "gps" -> state.copy(permissionGpsEnabled = !state.permissionGpsEnabled)
                "central" -> state.copy(permissionCentralLocEnabled = !state.permissionCentralLocEnabled)
                "notif" -> state.copy(permissionNotificationsEnabled = !state.permissionNotificationsEnabled)
                "hc" -> state.copy(permissionHealthConnectEnabled = !state.permissionHealthConnectEnabled)
                "samsung" -> state.copy(permissionSamsungHealthEnabled = !state.permissionSamsungHealthEnabled)
                else -> state
            }
        }
    }

    fun showBannerMessage(message: String) {
        _uiState.update { it.copy(statusBannerMessage = message) }
    }

    fun clearBannerMessage() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }
}
