package com.repforge.app.ui.workout
import com.repforge.app.PhoneWorkoutPlans
import com.repforge.app.TrainPilotWorkoutFinalization
import com.repforge.app.NativeSetStopwatch
import com.repforge.app.NativeWorkoutTarget
import com.repforge.app.NativeWorkoutEditing
import com.repforge.app.NativeWorkoutStartValues
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
import com.repforge.app.PersonalRecordCalculator226
import com.repforge.app.PhoneGpsRoute193
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
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.shared.GpsRouteLedger193
import com.repforge.shared.SetEditorRules212
import com.repforge.shared.WorkoutStatsCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
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
    val actualRir: Int?
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
    val isFinishing: Boolean = false
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

    init {
        viewModelScope.launch {
            while (isActive) {
                refreshStopwatchUi()
                delay(250L)
            }
        }
        observeActiveWorkout()
        observeIntervalTimer()
        observeGpsTracker()
        observeLiveHeartRate()
        loadExerciseLibrary()
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
                    val remainingMs = timerObj.optLong("remainingMs", 0L)
                    val totalMs = timerObj.optLong("totalMs", 0L)
                    _uiState.value = _uiState.value.copy(
                        timerRunning = timerObj.optBoolean("running", false),
                        timerPaused = timerObj.optBoolean("stopped", false) || timerObj.optBoolean("interrupted", false),
                        timerPhase = timerObj.optString("phase", "ready"),
                        timerRemainingSec = ((remainingMs + 999L) / 1000L).toInt(),
                        timerTotalSec = ((totalMs + 999L) / 1000L).toInt(),
                        timerRound = timerObj.optInt("round", 1),
                        timerRounds = timerObj.optInt("rounds", 1)
                    )
                }
            }
        }
    }

    private fun observeGpsTracker() {
        viewModelScope.launch {
            DistanceTrackingService.gpsStateFlow.collect { gpsObj ->
                if (gpsObj.length() > 0) {
                    _uiState.value = _uiState.value.copy(
                        isGpsActive = gpsObj.optBoolean("active", false),
                        gpsDistanceMeters = gpsObj.optDouble("distanceMeters", 0.0),
                        gpsElapsedSeconds = gpsObj.optLong("elapsedSeconds", 0L),
                        gpsHasFix = gpsObj.optBoolean("hasFix", false),
                        gpsAccuracyMeters = gpsObj.optDouble("accuracyMeters", 0.0).toFloat()
                    )
                }
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
                        actualRir = SetEditorRules212.parseActualRir229(sObj)
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
        _uiState.value = _uiState.value.copy(
            workoutId = workoutId,
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
            exercises = exercisesList,
            undoAvailable = undoAvailable,
            undoRecordId = undoRecId,
            undoExerciseName = undoExName,
            undoSetNumber = undoSetNum
        )
    }

    private fun refreshStopwatchUi() {
        val clock = lowerStopwatch
        _uiState.value = _uiState.value.copy(stopwatchSetId = clock?.optString("setId").orEmpty(),
            stopwatchSide = clock?.optString("side").orEmpty(),
            stopwatchSeconds = clock?.let { NativeSetStopwatch.elapsed(it) / 1000L } ?: 0L,
            stopwatchRunning = clock?.let { NativeSetStopwatch.running(it) } == true &&
                !_uiState.value.isPaused && _uiState.value.timerOwner == "phone")
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

        if (!wasDone) {
            val repUnit = ex.optString("repUnit", "ism.")
            val isBilateral = WorkoutStatsCalculator.isBilateralUnit(repUnit, ex.optString("id"))
            val left = set.optInt("leftSeconds", 0)
            val right = set.optInt("rightSeconds", 0)
            val reps = set.optString("reps", "0").toIntOrNull() ?: 0
            val valid = if (isBilateral) left > 0 && right > 0 else reps > 0
            if (!valid) return

            val before = JSONObject(current.toString())
            set.put("done", true)
            current.put("currentExercise", resolved.exerciseIndex)
            val recordId = UUID.randomUUID().toString()
            val target = JSONObject().put("exerciseId", ex.getString("id")).put("setId", set.getString("setId"))
            current.put("lastRecord188", WorkoutUndo188.capture(before, current, target, recordId))

            // Haptikus megerősítés (#227)
            viewModelScope.launch {
                val settings = settingsStore.current()
                AppHapticsManager.setRecorded(context, settings.hapticSetRecorded227)
            }

            // Pihenőidő indítása
            val restSec = current.optInt("restSeconds", 90)
            current.put("restEndAt", System.currentTimeMillis() + restSec * 1000L)
            saveAndPublish(current)

            // Indítsuk a telefonos intervallum időzítőt automatikusan
            viewModelScope.launch {
                runCatching {
                    val planTransition = PhoneWorkoutExecution.store(context).snapshot()?.let {
                        PhoneWorkoutPlans.afterRecord(it, resolved.exerciseIndex)
                    }
                    val plan = planTransition?.optJSONObject("plan")
                    if (plan != null) {
                        IntervalTimerService.startPrepared(plan)
                    }
                }
            }
        } else {
            // Kipipálás visszavonása sima érintéssel (ha nem az Undo gombbal történik)
            set.put("done", false)
            set.remove("actualRir")
            saveAndPublish(current)
        }
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
        val current = editableSnapshot() ?: return
        val exercises = current.optJSONArray("exercises") ?: JSONArray()
        val exObj = JSONObject()
            .put("id", libraryExercise.id)
            .put("name", libraryExercise.nameHu)
            .put("hu", libraryExercise.nameHu)
            .put("en", libraryExercise.nameEn)
            .put("loadType", libraryExercise.loadType)
            .put("repUnit", libraryExercise.repUnit)
            .put("measurementType", libraryExercise.measurementType)
            .put("targetReps", libraryExercise.defaultReps)
            .put("setRir229", false)
        val sets = JSONArray()
        for (s in 1..libraryExercise.defaultSets.coerceAtLeast(1)) {
            sets.put(
                JSONObject()
                    .put("set", s)
                    .put("setId", UUID.randomUUID().toString())
                    .put("weight", if (libraryExercise.defaultWeight > 0.0) libraryExercise.defaultWeight.toString() else "0")
                    .put("reps", libraryExercise.defaultReps)
                    .put("done", false)
            )
        }
        exObj.put("sets", sets)
        exercises.put(exObj)
        current.put("exercises", exercises)
        saveAndPublish(current)
        _uiState.value = _uiState.value.copy(showExercisePicker = false)
    }

    // =========================================================================
    // Időzítő, Szüneteltetés és GPS vezérlés
    // =========================================================================

    fun toggleWorkoutPause() {
        if (editableSnapshot() == null) return
        val current = editableSnapshot() ?: return
        val willPause = !current.optBoolean("workoutPaused", false)
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
        PhoneWorkoutExecution.setWorkoutPause(context, frame)
        if (!willPause) onEditorVisible()
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

    fun skipRestTimer() {
        if (editableSnapshot() == null) return
        val current = editableSnapshot() ?: return
        current.put("restEndAt", 0L)
        saveAndPublish(current)
        viewModelScope.launch(Dispatchers.Main) {
            runCatching { IntervalTimerService.command(context, "", "skipRest") }
        }
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        if (editableSnapshot() == null) return
        val current = editableSnapshot() ?: return
        val endAt = current.optLong("restEndAt", 0L)
        val now = System.currentTimeMillis()
        val newEndAt = if (endAt > now) endAt + deltaSeconds * 1000L else now + deltaSeconds * 1000L
        current.put("restEndAt", max(now, newEndAt))
        saveAndPublish(current)
    }

    fun toggleGpsTracking(key: String) {
        if (editableSnapshot() == null) return
        val isRunning = _uiState.value.isGpsActive
        if (!isRunning) {
            val intent = Intent(context, DistanceTrackingService::class.java).apply {
                action = DistanceTrackingService.START
                putExtra("key", key)
                putExtra("name", _uiState.value.title)
                putExtra("language", "hu")
            }
            ContextCompat.startForegroundService(context, intent)
        } else {
            DistanceTrackingService.stop(context)
        }
    }

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
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(isFinishing = false,
                    statusMessage = "Az edzés lezárása nem sikerült. Az adatok megmaradtak; próbáld újra. ${error.message.orEmpty()}")
            }
        }
    }

    fun discardWorkout() {
        val current = ActiveWorkoutStore.current(context) ?: return
        val workoutId = current.optString("workoutId")
        viewModelScope.launch(Dispatchers.IO) {
            if (current.optInt("ownershipProtocol193", 0) == 1) {
                runCatching { GpsRouteLedger193.open(context.noBackupFilesDir).deleteWorkout(workoutId) }
            }
            database.workoutHistoryDao().upsertTombstone(
                HistoryTombstoneEntity(
                    primaryId = workoutId,
                    workoutKey = workoutId,
                    idsJson = JSONArray().put(workoutId).toString(),
                    deletedAt = System.currentTimeMillis()
                )
            )
            withContext(Dispatchers.Main) {
                ActiveWorkoutStore.clear(context)
                try {
                    val execDb = PhoneWorkoutExecution.store(context)
                    val frame = execDb.snapshot()
                    if (frame != null && frame.optString("workoutId") == workoutId) {
                        execDb.discardExecution(workoutId, frame.optLong("generation"), frame.optLong("executionRevision"))
                        IntervalTimerService.closeWorkout(workoutId, frame.optLong("generation"))
                    }
                } catch (_: Exception) {}
                DistanceTrackingService.stop(context)
                WearSyncRepository.publishWatchHomeSnapshot(context)
            }
        }
    }

    fun openExercisePicker() { _uiState.value = _uiState.value.copy(showExercisePicker = true) }
    fun closeExercisePicker() { _uiState.value = _uiState.value.copy(showExercisePicker = false) }
    fun openFinishModal() { _uiState.value = _uiState.value.copy(showFinishModal = true) }
    fun closeFinishModal() { _uiState.value = _uiState.value.copy(showFinishModal = false) }
    fun openDiscardModal() {
        if (ActiveWorkoutStore.current(context)?.optBoolean("finalizationPending") == true) {
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
