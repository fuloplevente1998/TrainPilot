package com.repforge.app.ui.workout
import com.repforge.app.TrainPilotWorkoutFinalization
import com.repforge.app.NativeGpsMeasurement
import com.repforge.app.NativeTimerTarget
import com.repforge.app.ui.journal.RouteCoordinatesUiModel
import com.repforge.app.ui.journal.WorkoutRouteCoordinates
import com.repforge.app.NativeSetStopwatch
import com.repforge.app.NativeMeasuredTime
import com.repforge.app.NativeMeasuredSet
import com.repforge.app.NativeWorkoutTarget
import com.repforge.app.NativeWorkoutEditing
import com.repforge.app.NativeWorkoutStartValues
import com.repforge.app.HealthJournalStore
import com.repforge.app.data.store.NativeTrainingModes
import com.repforge.app.ui.coach.CoachTarget
import com.repforge.app.ui.coach.NativeCoachAdviceRepository
import com.repforge.app.ui.coach.NativeCoachSignals
import com.repforge.app.ui.coach.NativeCoachReadiness
import com.repforge.app.ui.coach.NativeCoachAdviceText
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.R
import androidx.core.content.ContextCompat

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.AppHapticsManager
import com.repforge.app.DistanceTrackingService
import com.repforge.app.IntervalTimerService
import com.repforge.app.PhoneOwnership193
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.StoredPhotoMetadata
import com.repforge.app.WearCommandListenerService
import com.repforge.app.WearCommandReceipts
import com.repforge.app.WearLiveHealthStore
import com.repforge.app.WearSyncRepository
import com.repforge.app.WorkoutPhotoRepository
import com.repforge.app.WorkoutUndo188
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.shared.SetEditorRules212
import com.repforge.shared.WorkoutStatsCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.Duration
import java.time.ZoneId
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ActiveSetUiModel(
    val setId: String,
    val setNumber: Int,
    val reps: String,
    val weight: String,
    val leftSeconds: Int,
    val rightSeconds: Int,
    val isCompleted: Boolean,
    val actualRir: Int?,
    val distanceMeters: Double = 0.0,
    val gpsRouteKey: String = ""
)

data class ActiveExerciseUiModel(
    val exerciseId: String,
    val name: String,
    val repUnit: String,
    val loadType: String,
    val measurementType: String,
    val targetReps: String,
    val isBilateral: Boolean,
    val isTimed: Boolean,
    val sets: List<ActiveSetUiModel>,
    val exerciseRir: Int?,
    val setRirEnabled: Boolean
)

data class ActiveWorkoutUiState(
    val workoutId: String = "",
    val generation: Long = 0L,
    val title: String = "Edzés",
    val dayName: String = "",
    val startedIso: String = "",
    val isPaused: Boolean = false,
    val timerOwner: String = "phone",
    val ownerEpoch: Long = 0L,
    val isTransferPending: Boolean = false,
    val canTakeBack: Boolean = false,
    val canHandOver: Boolean = false,
    val currentExerciseIndex: Int = 0,
    val exercises: List<ActiveExerciseUiModel> = emptyList(),
    val availableLibraryExercises: List<ExerciseLibraryEntity> = emptyList(),
    // Időzítő állapot (IntervalTimerService)
    val timerRunning: Boolean = false,
    val timerPaused: Boolean = false,
    val timerPhase: String = "ready", // "work", "rest", "ready", "complete"
    val timerRemainingSec: Int = 0,
    val timerTotalSec: Int = 0,
    val timerRound: Int = 1,
    val timerRounds: Int = 1,
    val timerKey: String = "",
    val timerWorkoutId: String = "",
    val timerGeneration: Long = 0L,
    val timerRestOnly: Boolean = false,
    val timerStopped: Boolean = true,
    val timerInterrupted: Boolean = false,
    val intervalWork: Int = 30,
    val intervalRest: Int = 90,
    val intervalRounds: Int = 1,
    val intervalSound: Boolean = true,
    val intervalVibration: Boolean = true,
    val intervalAutoAdvance: Boolean = true,
    val intervalSaving: Boolean = false,
    val measuredWorkSeconds: Long? = null,
    val measuredRestSeconds: Long? = null,
    val measuredPauseSeconds: Long? = null,
    val stopwatchSetId: String = "",
    val stopwatchSide: String = "",
    val stopwatchSeconds: Long = 0L,
    val stopwatchRunning: Boolean = false,
    // GPS állapot (DistanceTrackingService)
    val isGpsActive: Boolean = false,
    val gpsDistanceMeters: Double = 0.0,
    val gpsElapsedSeconds: Long = 0L,
    val gpsHasFix: Boolean = false,
    val gpsAccuracyMeters: Float = 0f,
    val gpsMeasurementId: String = "",
    val gpsBoundSetId: String = "",
    val gpsBindingState: String = "",
    val gpsBaseMeters: Double = 0.0,
    val gpsBaseSeconds: Long = 0L,
    val gpsError: String = "",
    val activeRoute: RouteCoordinatesUiModel? = null,
    // Szenzorok és visszajelzés
    val liveHeartRateBpm: Int? = null,
    val attachedPhotos: List<StoredPhotoMetadata> = emptyList(),
    // #188 Undo Set állapot
    val undoAvailable: Boolean = false,
    val undoRecordId: String = "",
    val undoExerciseName: String = "",
    val undoSetNumber: Int = 0,
    // Dialógusok állapota
    val showFinishModal: Boolean = false,
    val showDiscardModal: Boolean = false,
    val showExercisePicker: Boolean = false,
    val showOwnershipDialog: Boolean = false,
    val statusMessage: String? = null,
    val isFinishing: Boolean = false,
    val modeProposalLoading: Boolean = false,
    val explicitTrainingMode: Boolean = false,
    val modeProposalForExercise: String = "",
    val modeAction: String = "",
    val modeReason: String = "",
    val modeBaselineWeight: Double? = null,
    val modeSuggestedWeight: Double? = null,
    val modeHistoryCount: Int = 0,
    val modeWorkingSets: Int = 0,
    val modeReviewOpen: Boolean = false
)

class ActiveWorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = TrainPilotDatabase.getInstance(context)
    private val settingsStore = TrainPilotSettingsStore(context)
    private val migrator = LegacyWebViewMigrator(context, database, settingsStore)
    private val photoRepository = WorkoutPhotoRepository(context)

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var lowerStopwatch: JSONObject? = null
    private var timeFrame: JSONObject? = null
    private var latestTimer: JSONObject? = null
    private var timeTotals: NativeMeasuredTime.Totals? = null
    private var timeClockMs = 0L
    private var timeClockPauseMs = 0L
    private var timeSummaryAt = 0L
    private var modeTicket: NativeTrainingModeProposal.Ticket? = null
    private var modeSettingsRaw: String? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                refreshStopwatchUi()
                delay(250L)
            }
        }
        observeActiveWorkout()
        observeTrainingModeSettings()
        observeIntervalTimer()
        observeGpsTracker()
        observeLiveHeartRate()
        loadExerciseLibrary()
    }

    private fun observeTrainingModeSettings() {
        viewModelScope.launch {
            settingsStore.settingsFlow.collect { settings ->
                modeSettingsRaw = settings.trainingModes197Json
                val frame = ActiveWorkoutStore.current(context)
                _uiState.value = _uiState.value.copy(explicitTrainingMode =
                    frame?.optBoolean("active") == true &&
                        NativeTrainingModes.resolveSavedJson(modeSettingsRaw,
                            frame.optString("programId")).explicitChoice)
            }
        }
    }

    private fun observeActiveWorkout() {
        viewModelScope.launch {
            ActiveWorkoutStore.snapshotFlow.collect { snapshot ->
                if (snapshot != null && snapshot.optBoolean("active", false)) {
                    parseSnapshotToUi(snapshot)
                }
            }
        }
    }

    private fun observeIntervalTimer() {
        viewModelScope.launch {
            IntervalTimerService.timerStateFlow.collect { timerObj ->
                if (timerObj.length() > 0) {
                    latestTimer = JSONObject(timerObj.toString())
                    val remainingMs = timerObj.optLong("remainingMs", 0L)
                    val totalMs = timerObj.optLong("totalMs", 0L)
                    _uiState.value = _uiState.value.copy(
                        timerKey = timerObj.optString("key"),
                        timerWorkoutId = timerObj.optString("workoutId"),
                        timerGeneration = timerObj.optLong("generation"),
                        timerRestOnly = timerObj.optBoolean("restOnly"),
                        timerStopped = timerObj.optBoolean("stopped"),
                        timerInterrupted = timerObj.optBoolean("interrupted"),
                        timerRunning = timerObj.optBoolean("running", false),
                        timerPaused = timerObj.optBoolean("stopped", false) || timerObj.optBoolean("interrupted", false),
                        timerPhase = timerObj.optString("phase", "ready"),
                        timerRemainingSec = ((remainingMs + 999L) / 1000L).toInt(),
                        timerTotalSec = ((totalMs + 999L) / 1000L).toInt(),
                        timerRound = timerObj.optInt("round", 1),
                        timerRounds = timerObj.optInt("rounds", 1)
                    )
                    if (SystemClock.elapsedRealtime() - timeSummaryAt >= 1000L || !timerObj.optBoolean("running")) refreshTimeSummary()
                }
            }
        }
    }

    private fun observeGpsTracker() {
        viewModelScope.launch {
            DistanceTrackingService.gpsStateFlow.collect { gpsObj ->
                val frame = PhoneWorkoutExecution.store(context).snapshot()
                val binding = frame?.optJSONObject(NativeGpsMeasurement.FIELD)
                val matching = frame != null && binding != null && binding.optString("id") == gpsObj.optString("measurementId") &&
                    binding.optString("key") == gpsObj.optString("key") && frame.optString("workoutId") == gpsObj.optString("nativeWorkoutId") &&
                    frame.optLong("generation") == gpsObj.optLong("nativeGeneration")
                if (matching && editableSnapshot() != null) runCatching { NativeWorkoutEditing.project(context) }
                _uiState.value = _uiState.value.copy(
                    isGpsActive = matching && gpsObj.optBoolean("active"),
                    gpsDistanceMeters = if (matching) gpsObj.optDouble("distanceMeters", 0.0) else 0.0,
                    gpsElapsedSeconds = if (matching) gpsObj.optLong("elapsedSeconds") else 0L,
                    gpsHasFix = matching && gpsObj.optBoolean("hasFix"),
                    gpsAccuracyMeters = if (matching) gpsObj.optDouble("accuracyMeters", 0.0).toFloat() else 0f,
                    gpsError = if (matching) gpsObj.optString("error") else "")
            }
        }
    }

    private fun observeLiveHeartRate() {
        viewModelScope.launch {
            WearLiveHealthStore.liveHealthFlow.collect { healthObj ->
                val bpm = healthObj?.optJSONObject("summary")?.optDouble("heartRate", Double.NaN)
                    ?: healthObj?.optDouble("heartRate", Double.NaN)
                if (bpm != null && bpm.isFinite() && bpm > 20.0) {
                    _uiState.value = _uiState.value.copy(liveHeartRateBpm = bpm.roundToInt())
                }
            }
        }
    }

    private fun loadExerciseLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = database.exerciseLibraryDao().getAllExercises()
            _uiState.value = _uiState.value.copy(availableLibraryExercises = list)
        }
    }

    private fun parseSnapshotToUi(snapshot: JSONObject) {
        val workoutId = snapshot.optString("workoutId", snapshot.optString("started"))
        val dayName = snapshot.optString("dayName", snapshot.optString("workout", "Edzés"))
        val title = snapshot.optString("programName").takeIf { it.isNotBlank() }?.let { "$it • $dayName" } ?: dayName
        val isPaused = snapshot.optBoolean("workoutPaused", false)
        val timerOwner = snapshot.optString("timerOwner", "phone")
        val ownerEpoch = snapshot.optLong("ownerEpoch193", 0L)
        val currentExIdx = snapshot.optInt("currentExercise", 0)

        val exercisesList = ArrayList<ActiveExerciseUiModel>()
        val exArray = snapshot.optJSONArray("exercises") ?: JSONArray()

        for (i in 0 until exArray.length()) {
            val exObj = exArray.optJSONObject(i) ?: continue
            val repUnit = exObj.optString("repUnit", "ism.")
            val exId = exObj.optString("id", "ex-$i")
            val isBilateral = WorkoutStatsCalculator.isBilateralUnit(repUnit, exId)
            val isTimed = isBilateral || WorkoutStatsCalculator.isTimedUnit(repUnit, exId)

            val setsList = ArrayList<ActiveSetUiModel>()
            val setArray = exObj.optJSONArray("sets") ?: JSONArray()
            for (j in 0 until setArray.length()) {
                val sObj = setArray.optJSONObject(j) ?: continue
                val setNum = sObj.optInt("set", sObj.optInt("number", j + 1))
                val setId = sObj.optString("setId", "$workoutId:$exId:$setNum")
                setsList.add(
                    ActiveSetUiModel(
                        setId = setId,
                        setNumber = setNum,
                        reps = sObj.opt("reps")?.toString().orEmpty(),
                        weight = sObj.opt("weight")?.toString().orEmpty(),
                        leftSeconds = sObj.optInt("leftSeconds", 0),
                        rightSeconds = sObj.optInt("rightSeconds", 0),
                        isCompleted = sObj.optBoolean("done", false),
                        actualRir = SetEditorRules212.parseActualRir229(sObj),
                        distanceMeters = sObj.optDouble("distanceMeters", 0.0),
                        gpsRouteKey = sObj.optString("gpsRouteKey193").ifBlank { sObj.optString("gpsRouteKey163") }
                    )
                )
            }

            exercisesList.add(
                ActiveExerciseUiModel(
                    exerciseId = exId,
                    name = exObj.optString("name", exObj.optString("hu", exId)),
                    repUnit = repUnit,
                    loadType = exObj.optString("loadType", "weight"),
                    measurementType = exObj.optString("measurementType", "reps"),
                    targetReps = exObj.optString("targetReps", "10"),
                    isBilateral = isBilateral,
                    isTimed = isTimed,
                    sets = setsList,
                    exerciseRir = SetEditorRules212.parseActualRir229(exObj),
                    setRirEnabled = exObj.optBoolean("setRir229", false)
                )
            )
        }

        // #188 Undo ellenőrzése
        var undoAvailable = false
        var undoRecId = ""
        var undoExName = ""
        var undoSetNum = 0
        val lastRecord = snapshot.optJSONObject("lastRecord188")
        if (lastRecord != null && !isPaused && timerOwner == "phone") {
            val recAt = lastRecord.optLong("recordedAt", 0L)
            if (System.currentTimeMillis() - recAt <= WorkoutUndo188.QUICK_MILLIS) {
                undoAvailable = true
                undoRecId = lastRecord.optString("recordId")
                val targetExId = lastRecord.optString("exerciseId")
                val targetSetId = lastRecord.optString("setId")
                val ex = exercisesList.firstOrNull { it.exerciseId == targetExId }
                val s = ex?.sets?.firstOrNull { it.setId == targetSetId }
                undoExName = ex?.name ?: "Gyakorlat"
                undoSetNum = s?.setNumber ?: 1
            }
        }

        lowerStopwatch = snapshot.optJSONObject(NativeSetStopwatch.FIELD)
        timeFrame = JSONObject(snapshot.toString())
        refreshTimeSummary()
        val config = snapshot.optJSONObject("timerConfig") ?: JSONObject()
        val selected = snapshot.optJSONArray("exercises")?.optJSONObject(currentExIdx)
        _uiState.value = _uiState.value.copy(
            workoutId = workoutId,
            explicitTrainingMode = NativeTrainingModes.resolveSavedJson(
                modeSettingsRaw, snapshot.optString("programId")).explicitChoice,
            generation = snapshot.optLong("generation"),
            title = title,
            dayName = dayName,
            startedIso = snapshot.optString("started"),
            isPaused = isPaused,
            timerOwner = timerOwner,
            ownerEpoch = ownerEpoch,
            canTakeBack = timerOwner == "watch",
            canHandOver = timerOwner == "phone" && !isPaused,
            currentExerciseIndex = currentExIdx,
            intervalWork = config.optInt("work", 30),
            intervalRest = selected?.optInt("restSeconds162", config.optInt("rest", 90)) ?: config.optInt("rest", 90),
            intervalRounds = config.optInt("rounds", 1),
            intervalSound = config.optBoolean("sound", true),
            intervalVibration = config.optBoolean("vibration", true),
            intervalAutoAdvance = config.optBoolean("autoAdvance", true),
            gpsMeasurementId = snapshot.optJSONObject(NativeGpsMeasurement.FIELD)?.optString("id").orEmpty(),
            gpsBoundSetId = snapshot.optJSONObject(NativeGpsMeasurement.FIELD)?.optString("setId").orEmpty(),
            gpsBindingState = snapshot.optJSONObject(NativeGpsMeasurement.FIELD)?.optString("state").orEmpty(),
            gpsBaseMeters = snapshot.optJSONObject(NativeGpsMeasurement.FIELD)?.optDouble("baseMeters", 0.0) ?: 0.0,
            gpsBaseSeconds = snapshot.optJSONObject(NativeGpsMeasurement.FIELD)?.optLong("baseSeconds") ?: 0L,
            exercises = exercisesList,
            undoAvailable = undoAvailable,
            undoRecordId = undoRecId,
            undoExerciseName = undoExName,
            undoSetNumber = undoSetNum
        )
    }

    private fun refreshStopwatchUi() {
        val clock = lowerStopwatch
        val totals = timeTotals
        val ownsClock = _uiState.value.timerOwner == "phone"
        val delta = if (ownsClock && !_uiState.value.isPaused && clock != null && NativeSetStopwatch.running(clock)) (NativeSetStopwatch.elapsed(clock) - timeClockMs).coerceAtLeast(0L) else 0L
        val pauseDelta = if (ownsClock) clock?.let { (NativeSetStopwatch.pausedElapsed(it) - timeClockPauseMs).coerceAtLeast(0L) } ?: 0L else 0L
        _uiState.value = _uiState.value.copy(stopwatchSetId = clock?.optString("setId").orEmpty(),
            stopwatchSide = clock?.optString("side").orEmpty(),
            stopwatchSeconds = clock?.let { NativeSetStopwatch.elapsed(it) / 1000L } ?: 0L,
            stopwatchRunning = clock?.let { NativeSetStopwatch.running(it) } == true &&
                !_uiState.value.isPaused && _uiState.value.timerOwner == "phone",
            measuredWorkSeconds = totals?.let { (it.workMs + delta) / 1000L }, measuredRestSeconds = totals?.restMs?.div(1000L),
            measuredPauseSeconds = totals?.let { (it.pausedMs + pauseDelta) / 1000L })
    }

    private fun refreshTimeSummary() {
        val now = SystemClock.elapsedRealtime()
        timeTotals = timeFrame?.let { runCatching { NativeMeasuredTime.summary(it, latestTimer, now) }.getOrNull() }
        timeClockMs = lowerStopwatch?.let { NativeSetStopwatch.elapsed(it, now) } ?: 0L
        timeClockPauseMs = lowerStopwatch?.let { NativeSetStopwatch.pausedElapsed(it, now) } ?: 0L
        timeSummaryAt = now
    }

    fun toggleSetStopwatch(target: NativeWorkoutTarget, side: String) {
        if (editableSnapshot() == null) return
        try {
            val before = NativeWorkoutEditing.project(context)
            val clock = before.optJSONObject(NativeSetStopwatch.FIELD)
            val stopping = clock?.let { NativeSetStopwatch.running(it) && it.optString("setId") == target.setId && it.optString("side") == side } == true
            val saved = if (stopping) NativeWorkoutEditing.stopStopwatch(context, target)
                else NativeWorkoutEditing.startStopwatch(context, target, side)
            parseSnapshotToUi(saved)
            refreshStopwatchUi()
            runCatching { WearSyncRepository.publishActiveWorkoutSnapshot(context, saved) }
            if (stopping && target.resolve(saved).set.optBoolean("done")) viewModelScope.launch {
                AppHapticsManager.setRecorded(context, settingsStore.current().hapticSetRecorded227)
            }
        } catch (error: Exception) { _uiState.value = _uiState.value.copy(statusMessage = error.message ?: "A stopper nem vezérelhető.") }
    }

    fun resetSetStopwatch(target: NativeWorkoutTarget) {
        if (editableSnapshot() == null) return
        try {
            val saved = NativeWorkoutEditing.resetStopwatch(context, target)
            parseSnapshotToUi(saved)
            refreshStopwatchUi()
            runCatching { WearSyncRepository.publishActiveWorkoutSnapshot(context, saved) }
        } catch (error: Exception) { _uiState.value = _uiState.value.copy(statusMessage = error.message) }
    }

    // =========================================================================
    // Szett és Gyakorlat szerkesztési műveletek
    // =========================================================================

    fun editSet(target: NativeWorkoutTarget, field: String, value: String) {
        val current = editableSnapshot() ?: return
        val resolved = resolveTarget(current, target) ?: return
        val ex = resolved.exercise
        val set = resolved.set
        if (set.optBoolean("done")) return

        when (field) {
            "weight" -> set.put("weight", value)
            "reps" -> set.put("reps", value)
            "distanceKm" -> {
                val km = value.replace(',', '.').toDoubleOrNull()
                if (km == null || !km.isFinite() || km !in 0.0..2000.0) return
                set.put("distanceMeters", kotlin.math.round(km * 10_000.0) / 10.0).put("distanceSource", "manual")
            }
            "leftSeconds" -> {
                val sec = value.toIntOrNull() ?: 0
                set.put("leftSeconds", sec)
                val right = set.optInt("rightSeconds", 0)
                if (sec > 0 && right > 0) set.put("reps", min(sec, right).toString())
            }
            "rightSeconds" -> {
                val sec = value.toIntOrNull() ?: 0
                set.put("rightSeconds", sec)
                val left = set.optInt("leftSeconds", 0)
                if (sec > 0 && left > 0) set.put("reps", min(left, sec).toString())
            }
        }
        saveAndPublish(current)
    }

    fun toggleSetCompleted(target: NativeWorkoutTarget) {
        val current = editableSnapshot() ?: return
        val resolved = resolveTarget(current, target) ?: return
        val ex = resolved.exercise
        val set = resolved.set
        val wasDone = set.optBoolean("done", false)
        if (current.optJSONObject(NativeSetStopwatch.FIELD)?.let { NativeSetStopwatch.running(it) } == true) {
            _uiState.value = _uiState.value.copy(statusMessage = "Előbb állítsd le a sorozat stopperét.")
            return
        }

        if (!wasDone && NativeWorkoutStartValues.strength(ex)) {
            try {
                NativeWorkoutEditing.completeStrengthSet(context, ex.getString("id"), set.getString("setId"))
                viewModelScope.launch {
                    AppHapticsManager.setRecorded(context, settingsStore.current().hapticSetRecorded227)
                }
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(statusMessage = error.message ?: "A sorozat rögzítése nem sikerült.")
            }
            return
        }

        try {
            if (!wasDone && NativeMeasuredSet.supported(ex)) {
                NativeWorkoutEditing.completeMeasuredSet(context, target)
                viewModelScope.launch { AppHapticsManager.setRecorded(context, settingsStore.current().hapticSetRecorded227) }
            } else if (wasDone) {
                val last = current.optJSONObject("lastRecord188")
                if (last != null && last.optString("exerciseId") == target.exerciseId && last.optString("setId") == target.setId) {
                    NativeWorkoutEditing.undo(context, last.getString("recordId"))
                } else {
                    set.put("done", false).remove("actualRir")
                    saveAndPublish(current)
                }
            }
        } catch (error: Exception) { showStatus(error.message ?: "A mért értékek nem rögzíthetők.") }
    }

    fun configureIntervals(target: NativeWorkoutTarget, work: String, rest: String, rounds: String,
        sound: Boolean, vibration: Boolean, autoAdvance: Boolean, saveGlobally: Boolean) {
        if (editableSnapshot() == null || _uiState.value.intervalSaving) return
        _uiState.value = _uiState.value.copy(intervalSaving = true)
        viewModelScope.launch {
            try {
                com.repforge.app.NativeDataReplacement.mutate(context) {
                    check(editableSnapshot() != null)
                    val config = JSONObject().put("work", work.toInt()).put("rest", rest.toInt()).put("rounds", rounds.toInt())
                        .put("sound", sound).put("vibration", vibration).put("autoAdvance", autoAdvance)
                    NativeWorkoutEditing.configureIntervals(context, target, config)
                    if (saveGlobally) settingsStore.setTimerDefaults(config.getInt("work"), config.getInt("rest"),
                        config.getInt("rounds"), sound, vibration, autoAdvance)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) {
                val language = ActiveWorkoutStore.current(context)?.optString("language", "hu") ?: "hu"
                showStatus(com.repforge.app.ui.text.nativeResources(context, language).getString(com.repforge.app.R.string.interval_save_error))
            } finally { _uiState.value = _uiState.value.copy(intervalSaving = false) }
        }
    }

    fun startIntervals(target: NativeWorkoutTarget) {
        if (editableSnapshot() == null) return
        try { NativeWorkoutEditing.startIntervals(context, target) }
        catch (error: Exception) { showStatus(error.message ?: "Az intervallum nem indítható.") }
    }

    fun stopIntervals(target: NativeTimerTarget) {
        if (editableSnapshot() == null) return
        try { NativeWorkoutEditing.stopIntervals(context, target) }
        catch (error: Exception) { showStatus(error.message ?: "Az intervallum nem állítható le.") }
    }

    fun setSetRir(target: NativeWorkoutTarget, rir: Int?) {
        val current = editableSnapshot() ?: return
        val s = resolveTarget(current, target)?.set ?: return
        if (rir != null && rir in 0..10) {
            s.put("actualRir", rir)
        } else {
            s.remove("actualRir")
        }
        saveAndPublish(current)
    }

    fun toggleSetRirMode(target: NativeWorkoutTarget, enabled: Boolean) {
        val current = editableSnapshot() ?: return
        val ex = resolveTarget(current, target)?.exercise ?: return
        ex.put("setRir229", enabled)
        saveAndPublish(current)
    }

    fun undoLastSet() {
        if (editableSnapshot() == null) return
        val recordId = _uiState.value.undoRecordId
        if (recordId.isEmpty()) return
        viewModelScope.launch(Dispatchers.Main) {
            try {
                NativeWorkoutEditing.undo(context, recordId)
                AppHapticsManager.signal(context)
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(statusMessage = error.message ?: "A visszavonás nem sikerült.")
            }
        }
    }

    fun addSetToExercise(target: NativeWorkoutTarget) {
        val current = editableSnapshot() ?: return
        try {
            target.appendSet(current)
            saveAndPublish(current)
        } catch (error: Exception) {
            _uiState.value = _uiState.value.copy(statusMessage = error.message)
        }
    }

    fun removeSetFromExercise(target: NativeWorkoutTarget) {
        val current = editableSnapshot() ?: return
        val resolved = resolveTarget(current, target) ?: return
        val sets = resolved.exercise.getJSONArray("sets")
        if (sets.length() <= 1 || resolved.set.optBoolean("done")) return
        if (current.optJSONObject(NativeSetStopwatch.FIELD)?.optString("setId") == target.setId) {
            _uiState.value = _uiState.value.copy(statusMessage = "Előbb nullázd a sorozat stopperét.")
            return
        }
        val index = (0 until sets.length()).single { sets.getJSONObject(it).getString("setId") == target.setId }
        sets.remove(index)
        for (i in 0 until sets.length()) sets.getJSONObject(i).put("set", i + 1)
        saveAndPublish(current)
    }

    private fun resolveTarget(frame: JSONObject, target: NativeWorkoutTarget) = try {
        target.resolve(frame)
    } catch (error: Exception) {
        _uiState.value = _uiState.value.copy(statusMessage = error.message)
        null
    }

    fun addExerciseToWorkout(libraryExercise: ExerciseLibraryEntity) {
        val original = editableSnapshot() ?: return
        val workoutId = original.getString("workoutId")
        val generation = original.getLong("generation")
        viewModelScope.launch {
            try {
                val exercise = withContext(Dispatchers.IO) {
                    com.repforge.app.NativeWorkoutSession.buildExercise(database, libraryExercise, settingsStore.current())
                }
                val current = editableSnapshot() ?: return@launch
                check(current.optString("workoutId") == workoutId && current.optLong("generation") == generation) {
                    "Az edzés időközben megváltozott."
                }
                val exercises = current.getJSONArray("exercises")
                val existing = (0 until exercises.length()).firstOrNull { exercises.getJSONObject(it).optString("id") == libraryExercise.id }
                if (existing != null && current.optBoolean("quickWorkout")) {
                    val ex = exercises.getJSONObject(existing)
                    selectExercise(NativeWorkoutTarget(workoutId, generation, libraryExercise.id,
                        ex.getJSONArray("sets").getJSONObject(0).getString("setId")))
                } else {
                    exercises.put(exercise)
                    saveAndPublish(current)
                }
                _uiState.value = _uiState.value.copy(showExercisePicker = false)
            } catch (error: kotlinx.coroutines.CancellationException) { throw error }
            catch (error: Exception) { showStatus(error.message ?: "A gyakorlat nem adható hozzá.") }
        }
    }

    // =========================================================================
    // Időzítő, Szüneteltetés és GPS vezérlés
    // =========================================================================

    fun toggleWorkoutPause(workoutId: String, generation: Long) {
        val current = editableSnapshot() ?: return
        if (current.optString("workoutId") != workoutId || current.optLong("generation") != generation) {
            showStatus("Az edzés időközben megváltozott."); return
        }
        changeWorkoutPause(!current.optBoolean("workoutPaused"))
    }

    fun leaveWorkout(workoutId: String, generation: Long, onLeft: () -> Unit) {
        val current = ActiveWorkoutStore.current(context)
        if (current?.optBoolean("active") != true) { onLeft(); return }
        if (current.optString("workoutId") != workoutId || current.optLong("generation") != generation ||
            _uiState.value.isFinishing || current.optBoolean("finalizationPending")) return
        if (current.optString("timerOwner") == "phone" && !current.optBoolean("workoutPaused") && !changeWorkoutPause(true)) return
        onLeft()
    }

    private fun changeWorkoutPause(willPause: Boolean): Boolean {
        val current = editableSnapshot() ?: return false
        val now = System.currentTimeMillis()
        val frame = JSONObject(current.toString())
            .put("workoutPaused", willPause)
            .put("pauseRevision", max(current.optLong("pauseRevision") + 1L, now))
            .put("pausedRestMillis", if (willPause) max(0L, current.optLong("restEndAt") - now) else 0L)
            .put("restEndAt", if (!willPause && current.optLong("pausedRestMillis") > 0L) now + current.optLong("pausedRestMillis") else 0L)
        val intervals = frame.optJSONArray("activeIntervals") ?: JSONArray()
        if (willPause) {
            for (i in 0 until intervals.length()) {
                val r = intervals.optJSONObject(i)
                if (r != null && (r.isNull("end") || r.optString("end").isEmpty())) {
                    r.put("end", Instant.ofEpochMilli(now).toString())
                }
            }
        } else {
            intervals.put(JSONObject().put("start", Instant.ofEpochMilli(now).toString()).put("end", JSONObject.NULL))
        }
        frame.put("activeIntervals", intervals)
        return try {
            PhoneWorkoutExecution.setWorkoutPause(context, frame)
            if (!willPause) onEditorVisible()
            true
        } catch (error: Exception) { showStatus(error.message ?: "Az edzés szünete nem menthető."); false }
    }

    /** Called from a resumed editor, never from background sync or ownership notifications. */
    fun onEditorVisible() {
        if (editableSnapshot() == null) return
        try {
            val saved = NativeWorkoutEditing.project(context, PhoneWorkoutExecution.display(context))
            if (saved.optBoolean("workoutPaused") || IntervalTimerService.armed(saved.optString("workoutId"), saved.optLong("generation"))) return
            ContextCompat.startForegroundService(context, Intent(context, IntervalTimerService::class.java).apply {
                action = IntervalTimerService.ARM
                putExtra("workoutId", saved.getString("workoutId"))
                putExtra("generation", saved.getLong("generation"))
                putExtra("language", saved.optString("language", "hu"))
            })
        } catch (error: Exception) {
            _uiState.value = _uiState.value.copy(statusMessage = "A háttéridőzítő nem indult: ${error.message.orEmpty()}")
        }
    }

    /** Ephemeral explicit-start intent only; never called by background sync or draft recovery. */
    suspend fun awaitLaunchGps(target: NativeWorkoutTarget): Boolean {
        repeat(30) {
            val frame = editableSnapshot() ?: return false
            if (frame.optString("workoutId") != target.workoutId || frame.optLong("generation") != target.generation ||
                frame.optBoolean("workoutPaused")) return false
            try {
                if (com.repforge.app.NativeOutdoorStart.target(frame, target.exerciseId) != target || NativeGpsMeasurement.busy(frame)) return false
                if (IntervalTimerService.armed(target.workoutId, target.generation)) return true
            } catch (_: Exception) { return false }
            kotlinx.coroutines.delay(100)
        }
        showStatus("Az edzés elindult. A GPS-t a mérés gombjával indíthatod, ha a háttérvezérlő készen áll.")
        return false
    }

    fun setRestPaused(target: NativeTimerTarget, paused: Boolean) {
        if (editableSnapshot() == null) return
        try { NativeWorkoutEditing.setRestPaused(context, target, paused) }
        catch (error: Exception) { showStatus(error.message ?: "A pihenő nem vezérelhető.") }
    }

    fun skipRestTimer(target: NativeTimerTarget) = controlRest(target, null)
    fun adjustRestTimer(target: NativeTimerTarget, deltaSeconds: Int) = controlRest(target, deltaSeconds)

    private fun controlRest(target: NativeTimerTarget, deltaSeconds: Int?) {
        if (editableSnapshot() == null) return
        try { NativeWorkoutEditing.controlRest(context, target, deltaSeconds) }
        catch (error: Exception) { showStatus(error.message ?: "A pihenő nem vezérelhető.") }
    }

    fun selectExercise(target: NativeWorkoutTarget) {
        if (editableSnapshot() == null) return
        try {
            val saved = NativeWorkoutEditing.selectExercise(context, target)
            parseSnapshotToUi(saved)
            refreshStopwatchUi()
            runCatching { WearSyncRepository.publishActiveWorkoutSnapshot(context, saved) }
        } catch (error: Exception) { showStatus(error.message ?: "A gyakorlat nem váltható.") }
    }

    fun toggleGpsTracking(target: NativeWorkoutTarget, expectedMeasurementId: String? = null) {
        if (editableSnapshot() == null) return
        try {
            val current = NativeWorkoutEditing.project(context)
            val binding = current.optJSONObject(NativeGpsMeasurement.FIELD)
            val finishing = expectedMeasurementId != null
            val saved = if (finishing) NativeWorkoutEditing.finishGps(context, target, expectedMeasurementId!!)
                else NativeWorkoutEditing.startGps(context, target)
            parseSnapshotToUi(saved)
            runCatching { WearSyncRepository.publishActiveWorkoutSnapshot(context, saved) }
            if (finishing && target.resolve(saved).set.optBoolean("done")) viewModelScope.launch {
                AppHapticsManager.setRecorded(context, settingsStore.current().hapticSetRecorded227)
            }
        } catch (error: Exception) { showStatus(error.message ?: "A GPS-mérés nem indítható vagy rögzíthető.") }
    }

    private var routeRequest = 0L
    fun openRouteMap(key: String) {
        val request = ++routeRequest
        val workout = _uiState.value.workoutId
        viewModelScope.launch(Dispatchers.IO) {
            val route = WorkoutRouteCoordinates.load(context, key)
            withContext(Dispatchers.Main) {
                if (request == routeRequest && workout == _uiState.value.workoutId) _uiState.value = _uiState.value.copy(activeRoute = route)
            }
        }
    }
    fun closeRouteMap() { routeRequest++; _uiState.value = _uiState.value.copy(activeRoute = null) }
    fun showStatus(message: String) { _uiState.value = _uiState.value.copy(statusMessage = message) }

    // =========================================================================
    // Ownership Transition (#193) & Órás Mérés Távindítás
    // =========================================================================

    fun requestOwnershipBackFromWatch() {
        if (_uiState.value.isFinishing || ActiveWorkoutStore.current(context)?.optBoolean("finalizationPending") == true) return
        viewModelScope.launch(Dispatchers.IO) {
            val current = ActiveWorkoutStore.current(context) ?: return@launch
            runCatching {
                PhoneOwnership193.journal(context).recover()
                val resume = PhoneOwnership193.prepareResume(context)
                val reqId = resume.optJSONObject("snapshot")?.optJSONObject("ownershipImport193")?.optString("id") ?: ""
                val epoch = resume.optJSONObject("snapshot")?.optLong("ownerEpoch193") ?: 0L
                PhoneOwnership193.installResume(context, reqId, epoch)
            }
        }
    }

    fun startWatchMeasurementRemotely(activity: Activity) {
        val workoutId = _uiState.value.workoutId
        if (workoutId.isEmpty()) return
        viewModelScope.launch {
            val result = WearSyncRepository.startWatchMeasurementRemotely(activity, workoutId)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(statusMessage = "Az óra nem érhető el: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // =========================================================================
    // Fotók csatolása
    // =========================================================================

    fun onPhotoCaptured(meta: StoredPhotoMetadata) {
        if (editableSnapshot() == null) return
        val photos = _uiState.value.attachedPhotos + meta
        _uiState.value = _uiState.value.copy(attachedPhotos = photos)
        savePhotosToActiveSnapshot()
    }

    fun deletePhoto(id: String) {
        if (editableSnapshot() == null) return
        photoRepository.delete(id)
        val photos = _uiState.value.attachedPhotos.filter { it.id != id }
        _uiState.value = _uiState.value.copy(attachedPhotos = photos)
        savePhotosToActiveSnapshot()
    }

    private fun savePhotosToActiveSnapshot() {
        val current = ActiveWorkoutStore.current(context) ?: return
        val arr = JSONArray()
        for (p in _uiState.value.attachedPhotos) {
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("label", "")
                    .put("createdAt", Instant.now().toString())
                    .put("width", p.width)
                    .put("height", p.height)
                    .put("bytes", p.bytes)
            )
        }
        current.put("photos", arr)
        saveAndPublish(current)
    }

    // =========================================================================
    // Befejezés & Elvetés
    // =========================================================================

    private suspend fun modeCoachRow(frame: JSONObject): Pair<com.repforge.app.ui.coach.CoachExerciseAdvice, String?>? {
        val settings = settingsStore.current()
        val programId = frame.optString("programId")
        val dayId = frame.optString("dayId").ifBlank { frame.optString("workout") }
        if (programId.isBlank() || dayId.isBlank() ||
            !NativeTrainingModes.resolveSavedJson(settings.trainingModes197Json, programId).explicitChoice) return null
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val date = now.atZone(zone).toLocalDate()
        val page = HealthJournalStore.get(context).page(date.minusDays(14).toString(),
            date.toString(), "journal", "", 0L, 0L, 1)
        val days = page.optJSONArray("days")
        val realDays = (0 until (days?.length() ?: 0)).mapNotNull { days?.optJSONObject(it) }
        val signals = NativeCoachSignals.fromDays(realDays, date, zone)
        val history = database.workoutHistoryDao().getCoachRecentHistory(
            now.minus(Duration.ofDays(14)).toEpochMilli(), now.toEpochMilli()).map { JSONObject(it) }
        val readiness = NativeCoachReadiness.calculate(signals.recovery, signals.daily, history, now, zone)
        val rows = NativeCoachAdviceRepository(database).load(
            CoachTarget(programId, dayId, frame.optString("dayName")), settings, readiness, now)
        val selectedId = frame.optJSONArray("exercises")?.optJSONObject(frame.optInt("currentExercise"))?.optString("id")
            ?: return null
        return rows.singleOrNull { it.id == selectedId }?.let { it to settings.trainingModes197Json }
    }

    /** Preview never writes; the saved ticket is invalidated by any frame/owner/config/history change. */
    fun previewTrainingModeWeight(exerciseId: String) {
        if (_uiState.value.modeProposalLoading || _uiState.value.isFinishing) return
        modeTicket = null
        _uiState.value = _uiState.value.copy(modeProposalLoading = true,
            modeProposalForExercise = "", modeReviewOpen = false)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val frame = ActiveWorkoutStore.current(context)
                        ?.takeIf { it.optBoolean("active") && it.optString("timerOwner") == "phone" }
                        ?: return@withContext null
                    if (frame.optJSONArray("exercises")?.optJSONObject(frame.optInt("currentExercise"))
                            ?.optString("id") != exerciseId) return@withContext null
                    val rowAndRaw = modeCoachRow(frame) ?: return@withContext null
                    val row = rowAndRaw.first
                    val proposal = row.modePreview ?: return@withContext null
                    val fresh = ActiveWorkoutStore.current(context)
                    if (fresh?.toString() != frame.toString()) return@withContext null
                    val ticket = if (row.advice.action == "increase" &&
                        proposal.proposedWeight > proposal.baselineWeight &&
                        proposal.proposedWeight - proposal.baselineWeight <= 10.0) {
                        NativeTrainingModeProposal.ticket(frame, exerciseId,
                            proposal.proposedWeight, row.sourceFingerprint, rowAndRaw.second)
                    } else null
                    Triple(row, ticket, nativeResources(context, settingsStore.current().language))
                }
                if (result == null) {
                    _uiState.value = _uiState.value.copy(statusMessage =
                        nativeResources(context, settingsStore.current().language).getString(R.string.native_modes_coach_blocked))
                } else {
                    val (row, ticket, resources) = result
                    modeTicket = ticket
                    _uiState.value = _uiState.value.copy(
                        modeProposalForExercise = exerciseId,
                        modeAction = row.advice.action,
                        modeReason = NativeCoachAdviceText.reason(resources, row.advice.reason),
                        modeBaselineWeight = row.modePreview?.baselineWeight,
                        modeSuggestedWeight = if (ticket != null) ticket.approvedWeight else null,
                        modeHistoryCount = row.advice.historyCount,
                        modeWorkingSets = row.modePreview?.workingSets ?: 0
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) {
                _uiState.value = _uiState.value.copy(statusMessage =
                    nativeResources(context, settingsStore.current().language).getString(R.string.native_modes_coach_blocked))
            } finally { _uiState.value = _uiState.value.copy(modeProposalLoading = false) }
        }
    }

    fun reviewTrainingModeWeight() {
        if (modeTicket != null && !_uiState.value.modeProposalLoading)
            _uiState.value = _uiState.value.copy(modeReviewOpen = true)
    }
    fun cancelTrainingModeReview() { _uiState.value = _uiState.value.copy(modeReviewOpen = false) }

    fun approveTrainingModeWeight() {
        val ticket = modeTicket ?: return
        if (_uiState.value.modeProposalLoading || !_uiState.value.modeReviewOpen) return
        modeTicket = null
        _uiState.value = _uiState.value.copy(modeReviewOpen = false, modeProposalLoading = true)
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) {
                    // Retain the metadata replacement lease over history/readiness
                    // recomputation AND the canonical commit. It prevents a backup
                    // replacement from invalidating the data behind the proposal.
                    com.repforge.app.NativeDataReplacement.mutate(context) {
                        val frame = ActiveWorkoutStore.current(context)
                        if (frame == null || !NativeTrainingModeProposal.permitted(frame, ticket) ||
                            settingsStore.current().trainingModes197Json != ticket.settingsRaw) {
                            null
                        } else {
                            val computed = modeCoachRow(frame)
                            if (computed == null || computed.second != ticket.settingsRaw ||
                                computed.first.sourceFingerprint != ticket.sourceFingerprint ||
                                computed.first.advice.action != "increase" ||
                                computed.first.modePreview?.proposedWeight != ticket.approvedWeight) {
                                null
                            } else {
                                // PhoneWorkoutExecution.store / NativeWorkoutEditing are
                                // main-looper only. After IO recomputation, re-check
                                // settings and the live frame on Main immediately before
                                // the synchronous, single canonical editor commit.
                                NativeTrainingModeProposal.commitApprovedOnMain(context, ticket) {
                                    settingsStore.current().trainingModes197Json
                                }
                            }
                        }
                    }
                }
                if (saved != null) {
                    runCatching { WearSyncRepository.publishActiveWorkoutSnapshot(context, saved) }
                    _uiState.value = _uiState.value.copy(statusMessage =
                        nativeResources(context, settingsStore.current().language).getString(R.string.native_modes_coach_done))
                } else {
                    _uiState.value = _uiState.value.copy(statusMessage =
                        nativeResources(context, settingsStore.current().language).getString(R.string.native_modes_coach_blocked))
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) {
                _uiState.value = _uiState.value.copy(statusMessage =
                    nativeResources(context, settingsStore.current().language).getString(R.string.native_modes_coach_blocked))
            } finally {
                _uiState.value = _uiState.value.copy(modeProposalLoading = false, modeProposalForExercise = "")
            }
        }
    }

    fun finishWorkout(effort: String?, onSaved: () -> Unit) {
        if (_uiState.value.isFinishing) return
        val current = ActiveWorkoutStore.current(context) ?: return
        if (effort != null) current.put("feedback", JSONObject().put("effort", effort).put("updatedAt", System.currentTimeMillis()))
        _uiState.value = _uiState.value.copy(isFinishing = true)
        viewModelScope.launch {
            try {
                val result = TrainPilotWorkoutFinalization(context, database).finishWorkout(current)
                result.closedSnapshot?.let { runCatching { WearSyncRepository.publishActiveWorkoutSnapshot(context, it) } }
                WearSyncRepository.refreshWatchHomeAsync(context)
                _uiState.value = _uiState.value.copy(isFinishing = false)
                onSaved()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(isFinishing = false,
                    statusMessage = "Az edzés lezárása nem sikerült. Az adatok megmaradtak; próbáld újra. ${error.message.orEmpty()}")
            }
        }
    }

    fun discardWorkout(onDiscarded: () -> Unit) {
        if (_uiState.value.isFinishing) return
        val current = ActiveWorkoutStore.current(context) ?: return
        _uiState.value = _uiState.value.copy(isFinishing = true)
        viewModelScope.launch {
            try {
                val closed = TrainPilotWorkoutFinalization(context, database).discardWorkout(current)
                runCatching { WearSyncRepository.publishActiveWorkoutSnapshot(context, closed) }
                WearSyncRepository.refreshWatchHomeAsync(context)
                _uiState.value = _uiState.value.copy(isFinishing = false)
                onDiscarded()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(isFinishing = false,
                    statusMessage = "Az elvetés nem sikerült. Próbáld újra; az edzés nem indul újra. ${error.message.orEmpty()}")
            }
        }
    }

    fun openExercisePicker() { _uiState.value = _uiState.value.copy(showExercisePicker = true) }
    fun closeExercisePicker() { _uiState.value = _uiState.value.copy(showExercisePicker = false) }
    fun openFinishModal() { _uiState.value = _uiState.value.copy(showFinishModal = true) }
    fun closeFinishModal() { _uiState.value = _uiState.value.copy(showFinishModal = false) }
    fun openDiscardModal() {
        if (ActiveWorkoutStore.current(context)?.let { it.optBoolean("finalizationPending") && !it.optBoolean("discardPending") } == true) {
            _uiState.value = _uiState.value.copy(statusMessage = "Az edzés lezárása függőben maradt. Próbáld újra a befejezést.")
        } else _uiState.value = _uiState.value.copy(showDiscardModal = true)
    }
    fun closeDiscardModal() { _uiState.value = _uiState.value.copy(showDiscardModal = false) }
    fun clearStatusMessage() { _uiState.value = _uiState.value.copy(statusMessage = null) }

    private fun editableSnapshot(): JSONObject? = ActiveWorkoutStore.current(context)?.takeIf {
        !_uiState.value.isFinishing && it.optBoolean("active") && !it.optBoolean("finalizationPending") &&
            it.optString("timerOwner", "phone") == "phone"
    }

    private fun saveAndPublish(snapshot: JSONObject) {
        val current = ActiveWorkoutStore.current(context)
        if (_uiState.value.isFinishing || current?.optBoolean("finalizationPending") == true || current?.optString("timerOwner") == "watch") return
        try {
            val saved = NativeWorkoutEditing.commitEdit(context, snapshot)
            runCatching { WearSyncRepository.publishActiveWorkoutSnapshot(context, saved) }
        } catch (error: Exception) {
            _uiState.value = _uiState.value.copy(statusMessage = error.message ?: "A módosítás nem sikerült. Próbáld újra.")
        }
    }
}
