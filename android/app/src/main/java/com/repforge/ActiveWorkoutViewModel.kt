package com.repforge

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ActiveWorkoutUiState(
    val currentWorkout: EngineWorkoutSnapshot? = null,
    val workoutState: EngineWorkoutState = EngineWorkoutState.IDLE,
    val ownershipRole: PhoneOwnershipRole = PhoneOwnershipRole.PRIMARY,
    val elapsedSeconds: Long = 0L,
    val restSecondsRemaining: Int = 0,
    val isRestActive: Boolean = false,
    val isProgressionPanelExpanded: Boolean = false, // v1.3.71: Flatter collapsed panel
    val selectedStyle: EngineTrainingStyle = EngineTrainingStyle.NORMAL,
    val selectedProgression: EngineProgressionModel = EngineProgressionModel.DOUBLE_PROGRESSION,
    val progressionAdvice: ProgressionAdvice? = null,
    val gpsSnapshot: PhoneGpsSnapshot193 = PhoneGpsSnapshot193(),
    val showCancelDialog: Boolean = false, // v1.3.71: Restored cancel confirmation
    val showFinishDialog: Boolean = false,
    val activePrBreakdowns: List<PersonalRecordBreakdown226> = emptyList(),
    val hapticFeedbackEnabled: Boolean = true
)

class ActiveWorkoutViewModel(
    private val context: Context,
    val execution: PhoneWorkoutExecution = PhoneWorkoutExecution(),
    val ownershipGps: PhoneOwnershipAndGpsCoordinator193 = PhoneOwnershipAndGpsCoordinator193(context),
    val wearSync: WearSyncRepository = WearSyncRepository(context)
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private val historicalPrStore = mutableListOf<PrEntry226>()

    init {
        // Állapotgép és időzítők figyelése
        viewModelScope.launch {
            combine(
                execution.currentWorkout,
                execution.currentState,
                execution.elapsedSeconds,
                execution.restSecondsRemaining,
                execution.isRestActive,
                ownershipGps.ownershipRole,
                ownershipGps.gpsState
            ) { workout, state, elapsed, restSec, isRest, role, gps ->
                _uiState.update { current ->
                    current.copy(
                        currentWorkout = workout,
                        workoutState = state,
                        elapsedSeconds = elapsed,
                        restSecondsRemaining = restSec,
                        isRestActive = isRest,
                        ownershipRole = role,
                        gpsSnapshot = gps
                    )
                }

                // Szinkronizálás a Wear OS óra felé
                wearSync.syncWorkoutState(
                    stateName = state.name,
                    durationSeconds = elapsed,
                    restSecondsRemaining = restSec,
                    isRestActive = isRest
                )
            }.collect()
        }
    }

    fun startWorkout(title: String, style: EngineTrainingStyle = EngineTrainingStyle.NORMAL) {
        val workoutId = System.currentTimeMillis()
        execution.startWorkout(workoutId, title, style, _uiState.value.selectedProgression)
        ownershipGps.startTracking(workoutId, isIndoor = false)
        startService(WorkoutForegroundService.ACTION_START)
    }

    fun onSetValuesChanged(setId: Long, reps: Int, weightKg: Double, rir: Int) {
        val current = _uiState.value.currentWorkout ?: return
        val updatedExercises = current.exercises.map { ex ->
            ex.copy(sets = ex.sets.map { s ->
                if (s.setId == setId) s.copy(reps = reps, weightKg = weightKg, actualRir = rir.coerceIn(0, 10)) else s
            })
        }
        // Lokális állapot frissítése
    }

    fun onCompleteSet(set: EngineSetRecord, reps: Int, weightKg: Double, actualRir: Int) {
        viewModelScope.launch {
            val completed = execution.recording.completeSet(set, weightKg, reps, actualRir)

            // PR ellenőrzés a PR #226 kalkulátorral
            val newPrs = PersonalRecordCalculator226.evaluateSet(
                exerciseName = set.exerciseName,
                weightKg = weightKg,
                reps = reps,
                historicalPrs = historicalPrStore
            )

            if (newPrs.isNotEmpty()) {
                _uiState.update { it.copy(activePrBreakdowns = newPrs) }
                newPrs.forEach { pr ->
                    historicalPrStore.add(
                        PrEntry226(
                            exerciseName = pr.exerciseName,
                            category = pr.category,
                            value = pr.newValue,
                            reps = pr.reps,
                            weightKg = pr.weightKg,
                            achievedAt = System.currentTimeMillis(),
                            formulaUsed = "Epley/Brzycki"
                        )
                    )
                }
            }

            // Pihenőidő indítása
            val restSeconds = if (_uiState.value.selectedStyle == EngineTrainingStyle.HIT) 180 else 90
            execution.startRestTimer(restSeconds)
        }
    }

    fun onUndoSet(set: EngineSetRecord) {
        execution.recording.undoSet(set)
    }

    fun onSkipRest() = execution.skipRest()
    fun onAdjustRest(delta: Int) = execution.adjustRestTimer(delta)

    fun onToggleProgressionPanel() {
        _uiState.update { it.copy(isProgressionPanelExpanded = !it.isProgressionPanelExpanded) }
    }

    fun onSelectStyle(style: EngineTrainingStyle) {
        _uiState.update { it.copy(selectedStyle = style) }
    }

    fun onSelectProgression(prog: EngineProgressionModel) {
        _uiState.update { it.copy(selectedProgression = prog) }
    }

    fun onToggleIndoorGps() = ownershipGps.toggleIndoorMode()

    fun onCompleteExplicitCardio() {
        val result = ownershipGps.completeExplicitCardioInterval(_uiState.value.elapsedSeconds)
        // Explicit rögzítés dialógus nélkül (#193/#202)
    }

    // --- v1.3.71: Visszaállított megerősített Cancel viselkedés ---
    fun onRequestCancel() {
        _uiState.update { it.copy(showCancelDialog = true) }
    }

    fun onDismissCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = false) }
    }

    fun onConfirmCancel() {
        _uiState.update { it.copy(showCancelDialog = false) }
        execution.cancelWorkout()
        ownershipGps.stopTracking()
        startService(WorkoutForegroundService.ACTION_STOP)
        viewModelScope.launch {
            wearSync.sendCommandToWear("CANCEL")
        }
    }

    // --- Befejezés kezelése ---
    fun onRequestFinish() {
        _uiState.update { it.copy(showFinishDialog = true) }
    }

    fun onDismissFinishDialog() {
        _uiState.update { it.copy(showFinishDialog = false) }
    }

    fun onConfirmFinish() {
        _uiState.update { it.copy(showFinishDialog = false) }
        execution.finishWorkout()
        ownershipGps.stopTracking()
        startService(WorkoutForegroundService.ACTION_STOP)
        viewModelScope.launch {
            wearSync.sendCommandToWear("FINISH")
            SyncSpoolWorker.enqueueImmediateSync(context)
        }
    }

    // --- Ownership Transition (#193) ---
    fun onInitiateHandoffToWatch() {
        val payload = ownershipGps.requestHandoffToWatch()
        viewModelScope.launch {
            wearSync.sendOwnershipHandoffMessage(payload)
        }
    }

    fun onReclaimOwnershipToPhone() {
        ownershipGps.reclaimOwnershipToPhone()
    }

    fun onDismissPrBadge() {
        _uiState.update { it.copy(activePrBreakdowns = emptyList()) }
    }

    private fun startService(action: String) {
        val intent = Intent(context, WorkoutForegroundService::class.java).apply {
            this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
