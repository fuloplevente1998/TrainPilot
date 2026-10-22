package com.repforge

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject

// --- Modellek az Edzésmotorhoz ---

enum class EngineWorkoutState {
    IDLE,
    WARMUP,
    WORK,
    REST,
    PAUSED,
    COMPLETED,
    CANCELLED
}

enum class EngineSetType {
    WARMUP,
    NORMAL,
    DROPSET,
    FAILURE
}

enum class EngineTrainingStyle {
    NORMAL,
    HIT // High Intensity Training: 1 maximális munkasorozat failure-ig
}

enum class EngineProgressionModel {
    NONE,
    DOUBLE_PROGRESSION,
    LINEAR,
    RIR_COACH
}

data class EngineSetRecord(
    val setId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val setIndex: Int,
    val setType: EngineSetType,
    val weightKg: Double,
    val reps: Int,
    val actualRir: Int,
    val isCompleted: Boolean,
    val completedAt: Long?
)

data class EngineExerciseRecord(
    val exerciseId: Long,
    val name: String,
    val muscleGroup: String,
    val orderIndex: Int,
    val sets: List<EngineSetRecord>
)

data class EngineWorkoutSnapshot(
    val workoutId: Long,
    val title: String,
    val style: EngineTrainingStyle,
    val progression: EngineProgressionModel,
    val state: EngineWorkoutState,
    val exercises: List<EngineExerciseRecord>,
    val durationSeconds: Long,
    val isCompleted: Boolean
)

data class PrefillResult(
    val weightKg: Double,
    val reps: Int,
    val actualRir: Int,
    val sourceDescription: String
)

data class ProgressionAdvice(
    val exerciseName: String,
    val model: EngineProgressionModel,
    val currentWeightKg: Double,
    val targetReps: Int,
    val suggestedWeightKg: Double,
    val suggestedReps: Int,
    val adviceMessageHu: String,
    val adviceMessageEn: String
)

// =============================================================================
// ALRENDSZER 1: PhoneWorkoutRecording (Sorozatok, Prefill és RIR kezelés)
// =============================================================================

class PhoneWorkoutRecording(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val historyCache = mutableMapOf<String, MutableList<EngineSetRecord>>()

    fun recordSetHistory(set: EngineSetRecord) {
        val list = historyCache.getOrPut(set.exerciseName) { mutableListOf() }
        list.add(set)
    }

    /**
     * Automatikus korábbi érték kitöltés (Prefill).
     */
    fun resolvePrefill(
        exerciseName: String,
        setIndex: Int,
        style: EngineTrainingStyle = EngineTrainingStyle.NORMAL
    ): PrefillResult {
        val previousSets = historyCache[exerciseName]?.filter { it.isCompleted } ?: emptyList()

        if (previousSets.isEmpty()) {
            return PrefillResult(
                weightKg = 20.0,
                reps = if (style == EngineTrainingStyle.HIT) 8 else 10,
                actualRir = if (style == EngineTrainingStyle.HIT) 0 else 2,
                sourceDescription = "Alapértelmezett kezdőérték"
            )
        }

        val exactMatch = previousSets.lastOrNull { it.setIndex == setIndex }
        val latest = exactMatch ?: previousSets.last()

        return if (style == EngineTrainingStyle.HIT && setIndex > 1) {
            PrefillResult(
                weightKg = latest.weightKg,
                reps = latest.reps,
                actualRir = 0,
                sourceDescription = "Előző HIT munkasorozat alapján"
            )
        } else {
            PrefillResult(
                weightKg = latest.weightKg,
                reps = latest.reps,
                actualRir = latest.actualRir.coerceIn(0, 10),
                sourceDescription = "Előző teljesített sorozat alapján"
            )
        }
    }

    fun completeSet(
        set: EngineSetRecord,
        weightKg: Double,
        reps: Int,
        actualRir: Int
    ): EngineSetRecord {
        val validRir = actualRir.coerceIn(0, 10)
        val completed = set.copy(
            weightKg = weightKg,
            reps = reps,
            actualRir = validRir,
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )
        recordSetHistory(completed)
        return completed
    }

    fun undoSet(set: EngineSetRecord): EngineSetRecord {
        return set.copy(
            isCompleted = false,
            completedAt = null
        )
    }
}

// =============================================================================
// ALRENDSZER 2: PhoneWorkoutPlans (Tervek, Sablonok és Progresszió kalkuláció)
// =============================================================================

class PhoneWorkoutPlans {

    fun evaluateProgression(
        exerciseName: String,
        model: EngineProgressionModel,
        completedSets: List<EngineSetRecord>,
        targetRepMin: Int = 8,
        targetRepMax: Int = 12,
        weightIncrementKg: Double = 2.5
    ): ProgressionAdvice {
        val workSets = completedSets.filter { it.setType != EngineSetType.WARMUP && it.isCompleted }
        if (workSets.isEmpty()) {
            return ProgressionAdvice(
                exerciseName = exerciseName,
                model = model,
                currentWeightKg = 0.0,
                targetReps = targetRepMax,
                suggestedWeightKg = 0.0,
                suggestedReps = targetRepMin,
                adviceMessageHu = "Nincs még befejezett munkasorozat.",
                adviceMessageEn = "No completed working sets yet."
            )
        }

        val lastWeight = workSets.last().weightKg
        val allHitUpperLimit = workSets.all { it.reps >= targetRepMax && it.actualRir <= 2 }

        return when (model) {
            EngineProgressionModel.DOUBLE_PROGRESSION -> {
                if (allHitUpperLimit) {
                    val nextWeight = lastWeight + weightIncrementKg
                    ProgressionAdvice(
                        exerciseName = exerciseName,
                        model = model,
                        currentWeightKg = lastWeight,
                        targetReps = targetRepMax,
                        suggestedWeightKg = nextWeight,
                        suggestedReps = targetRepMin,
                        adviceMessageHu = "Cél elérve! Növeld a súlyt: %.1f kg -> %.1f kg (%d ismétlés).".format(lastWeight, nextWeight, targetRepMin),
                        adviceMessageEn = "Target met! Increase weight: %.1f kg -> %.1f kg (%d reps).".format(lastWeight, nextWeight, targetRepMin)
                    )
                } else {
                    val nextReps = (workSets.map { it.reps }.minOrNull() ?: targetRepMin) + 1
                    ProgressionAdvice(
                        exerciseName = exerciseName,
                        model = model,
                        currentWeightKg = lastWeight,
                        targetReps = targetRepMax,
                        suggestedWeightKg = lastWeight,
                        suggestedReps = nextReps.coerceAtMost(targetRepMax),
                        adviceMessageHu = "Tartsd a súlyt (%.1f kg), cél a következő edzésen: %d ismétlés.".format(lastWeight, nextReps),
                        adviceMessageEn = "Maintain weight (%.1f kg), aim for %d reps next session.".format(lastWeight, nextReps)
                    )
                }
            }
            EngineProgressionModel.LINEAR -> {
                val nextWeight = lastWeight + weightIncrementKg
                ProgressionAdvice(
                    exerciseName = exerciseName,
                    model = model,
                    currentWeightKg = lastWeight,
                    targetReps = targetRepMax,
                    suggestedWeightKg = nextWeight,
                    suggestedReps = targetRepMin,
                    adviceMessageHu = "Lineáris progresszió: lépj tovább %.1f kg-ra.".format(nextWeight),
                    adviceMessageEn = "Linear progression: step up to %.1f kg.".format(nextWeight)
                )
            }
            EngineProgressionModel.RIR_COACH -> {
                val avgRir = workSets.map { it.actualRir }.average()
                if (avgRir >= 3.0) {
                    val nextWeight = lastWeight + weightIncrementKg
                    ProgressionAdvice(
                        exerciseName = exerciseName,
                        model = model,
                        currentWeightKg = lastWeight,
                        targetReps = targetRepMax,
                        suggestedWeightKg = nextWeight,
                        suggestedReps = targetRepMin,
                        adviceMessageHu = "Túl könnyű sorozatok (Átlag RIR: %.1f). Javasolt emelés: +%.1f kg.".format(avgRir, weightIncrementKg),
                        adviceMessageEn = "Effort too low (Avg RIR: %.1f). Recommended bump: +%.1f kg.".format(avgRir, weightIncrementKg)
                    )
                } else {
                    ProgressionAdvice(
                        exerciseName = exerciseName,
                        model = model,
                        currentWeightKg = lastWeight,
                        targetReps = targetRepMax,
                        suggestedWeightKg = lastWeight,
                        suggestedReps = targetRepMax,
                        adviceMessageHu = "Optimális intenzitás (Átlag RIR: %.1f). Maradj a jelenlegi súlynál.".format(avgRir),
                        adviceMessageEn = "Optimal intensity (Avg RIR: %.1f). Maintain current load.".format(avgRir)
                    )
                }
            }
            EngineProgressionModel.NONE -> {
                ProgressionAdvice(
                    exerciseName = exerciseName,
                    model = model,
                    currentWeightKg = lastWeight,
                    targetReps = targetRepMax,
                    suggestedWeightKg = lastWeight,
                    suggestedReps = targetRepMin,
                    adviceMessageHu = "Nincs aktív automatikus progresszió.",
                    adviceMessageEn = "No active progression model."
                )
            }
        }
    }
}

// =============================================================================
// ALRENDSZER 3: PhoneWorkoutExecution (Időzítők, Állapotgép, Cancel megerősítés)
// =============================================================================

class PhoneWorkoutExecution(
    val recording: PhoneWorkoutRecording = PhoneWorkoutRecording(),
    val plans: PhoneWorkoutPlans = PhoneWorkoutPlans(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val tag = "PhoneWorkoutExecution"

    private val _currentState = MutableStateFlow(EngineWorkoutState.IDLE)
    val currentState: StateFlow<EngineWorkoutState> = _currentState.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _restSecondsRemaining = MutableStateFlow(0)
    val restSecondsRemaining: StateFlow<Int> = _restSecondsRemaining.asStateFlow()

    private val _isRestActive = MutableStateFlow(false)
    val isRestActive: StateFlow<Boolean> = _isRestActive.asStateFlow()

    private val _currentWorkout = MutableStateFlow<EngineWorkoutSnapshot?>(null)
    val currentWorkout: StateFlow<EngineWorkoutSnapshot?> = _currentWorkout.asStateFlow()

    private var tickerJob: Job? = null
    private var restJob: Job? = null

    fun startWorkout(
        workoutId: Long,
        title: String,
        style: EngineTrainingStyle = EngineTrainingStyle.NORMAL,
        progression: EngineProgressionModel = EngineProgressionModel.DOUBLE_PROGRESSION
    ) {
        val snapshot = EngineWorkoutSnapshot(
            workoutId = workoutId,
            title = title,
            style = style,
            progression = progression,
            state = EngineWorkoutState.WORK,
            exercises = emptyList(),
            durationSeconds = 0L,
            isCompleted = false
        )
        _currentWorkout.value = snapshot
        _currentState.value = EngineWorkoutState.WORK
        _elapsedSeconds.value = 0L

        startTicker()
        Log.i(tag, "Edzés elindítva: $title (ID: $workoutId)")
    }

    fun pauseWorkout() {
        if (_currentState.value == EngineWorkoutState.WORK || _currentState.value == EngineWorkoutState.REST) {
            _currentState.value = EngineWorkoutState.PAUSED
            tickerJob?.cancel()
            restJob?.cancel()
        }
    }

    fun resumeWorkout() {
        if (_currentState.value == EngineWorkoutState.PAUSED) {
            _currentState.value = if (_isRestActive.value) EngineWorkoutState.REST else EngineWorkoutState.WORK
            startTicker()
            if (_isRestActive.value) startRestCountdown()
        }
    }

    fun startRestTimer(seconds: Int = 90) {
        _isRestActive.value = true
        _restSecondsRemaining.value = seconds
        _currentState.value = EngineWorkoutState.REST
        startRestCountdown()
    }

    fun skipRest() {
        restJob?.cancel()
        _isRestActive.value = false
        _restSecondsRemaining.value = 0
        if (_currentState.value == EngineWorkoutState.REST) {
            _currentState.value = EngineWorkoutState.WORK
        }
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        val updated = (_restSecondsRemaining.value + deltaSeconds).coerceAtLeast(0)
        _restSecondsRemaining.value = updated
        if (updated == 0) skipRest()
    }

    /**
     * V1.3.71: Restored Cancel viselkedés.
     * Tisztán leállítja az időzítőket, törli az aktív draftot és CANCELLED állapotba léptet.
     */
    fun cancelWorkout(): Boolean {
        tickerJob?.cancel()
        restJob?.cancel()
        _isRestActive.value = false
        _restSecondsRemaining.value = 0

        val current = _currentWorkout.value
        if (current != null) {
            _currentWorkout.value = current.copy(state = EngineWorkoutState.CANCELLED)
        }
        _currentState.value = EngineWorkoutState.CANCELLED

        Log.i(tag, "Edzés megszakítva (Cancel). Időzítők leállítva.")
        _currentState.value = EngineWorkoutState.IDLE
        _currentWorkout.value = null
        return true
    }

    fun finishWorkout(): EngineWorkoutSnapshot? {
        tickerJob?.cancel()
        restJob?.cancel()
        _isRestActive.value = false
        _restSecondsRemaining.value = 0

        val finished = _currentWorkout.value?.copy(
            state = EngineWorkoutState.COMPLETED,
            durationSeconds = _elapsedSeconds.value,
            isCompleted = true
        )
        _currentWorkout.value = finished
        _currentState.value = EngineWorkoutState.COMPLETED
        return finished
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                _elapsedSeconds.value += 1L
            }
        }
    }

    private fun startRestCountdown() {
        restJob?.cancel()
        restJob = scope.launch {
            while (isActive && _restSecondsRemaining.value > 0) {
                delay(1000L)
                _restSecondsRemaining.value -= 1
            }
            _isRestActive.value = false
            if (_currentState.value == EngineWorkoutState.REST) {
                _currentState.value = EngineWorkoutState.WORK
            }
        }
    }
}
