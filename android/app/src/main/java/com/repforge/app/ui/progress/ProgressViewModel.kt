package com.repforge.app.ui.progress
import java.util.UUID

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.WorkoutPhotoRepository
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.shared.WorkoutStatsCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

data class MuscleLoadUiModel(
    val muscleKey: String,
    val label: String,
    val setsCount: Int,
    val loadPercentage: Float // 0.0f .. 1.0f
)

data class Exercise1RmPointUiModel(
    val date: String,
    val weightKg: Double,
    val reps: Int,
    val e1rmKg: Double
)

data class ProgressUiState(
    val muscleLoads: List<MuscleLoadUiModel> = emptyList(),
    val exercises: List<ExerciseLibraryEntity> = emptyList(),
    val selectedExerciseId: String = "goblet-squat",
    val exercise1RmTrend: List<Exercise1RmPointUiModel> = emptyList(),
    val currentWeightKg: Double? = null,
    val startWeightKg: Double? = null,
    val weightDiffKg: Double? = null,
    val weightRange: String = "30", // "7", "30", "90", "365", "all"
    val filteredWeights: List<BodyWeightEntryEntity> = emptyList(),
    val progressPhotos: List<File> = emptyList()
)

class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = TrainPilotDatabase.getInstance(context)
    private val photoRepo = WorkoutPhotoRepository(context)

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadProgressData()
    }

    fun loadProgressData() {
        viewModelScope.launch {
            val workouts = database.workoutHistoryDao().getAllWorkoutsWithDetails()
            val exercises = database.exerciseLibraryDao().getAllExercises()
            val weights = database.bodyWeightDao().getAllWeights()

            calculateProgress(workouts, exercises, weights)
        }
    }

    private suspend fun calculateProgress(
        workouts: List<WorkoutHistoryWithDetails>,
        exercises: List<ExerciseLibraryEntity>,
        weights: List<BodyWeightEntryEntity>
    ) = withContext(Dispatchers.Default) {
        // 1. #177 Izomcsoport terhelés számítás (utolsó 30 nap edzései alapján)
        val cutoffEpoch = System.currentTimeMillis() - 30L * 86_400_000L
        val recentWorkouts = workouts.filter { it.workout.startedEpochMs >= cutoffEpoch }
        val muscleSets = mutableMapOf(
            "chest" to 0,
            "back" to 0,
            "shoulders" to 0,
            "arms" to 0,
            "core" to 0,
            "legs" to 0
        )

        val exMap = exercises.associateBy { it.id }
        for (w in recentWorkouts) {
            for (ex in w.exercises) {
                val lib = exMap[ex.exercise.exerciseId]
                val group = lib?.primaryMuscle ?: "full"
                val doneSets = ex.sets.count { it.done }
                if (group in muscleSets) {
                    muscleSets[group] = (muscleSets[group] ?: 0) + doneSets
                } else if (group == "cardio") {
                    muscleSets["legs"] = (muscleSets["legs"] ?: 0) + doneSets
                }
            }
        }
        val maxSets = max(1, muscleSets.values.maxOrNull() ?: 1)
        val muscleLabels = mapOf(
            "chest" to "Mellizom",
            "back" to "Hátizom",
            "shoulders" to "Vállizom",
            "arms" to "Karizmok",
            "core" to "Törzs / Has",
            "legs" to "Lábizmok"
        )
        val loadModels = muscleSets.map {
            MuscleLoadUiModel(
                muscleKey = it.key,
                label = muscleLabels[it.key] ?: it.key,
                setsCount = it.value,
                loadPercentage = (it.value.toFloat() / maxSets.toFloat()).coerceIn(0f, 1f)
            )
        }

        // 2. 1RM Fejlődési görbe az aktuálisan kiválasztott gyakorlatra
        val trend = calculate1RmTrend(workouts, _uiState.value.selectedExerciseId)

        // 3. Testsúly statisztika
        val curW = weights.lastOrNull()?.kg
        val startW = weights.firstOrNull()?.kg
        val diff = if (curW != null && startW != null) (curW - startW * 10.0).roundToInt() / 10.0 else null
        val filtered = filterWeightsByRange(weights, _uiState.value.weightRange)

        // 4. Haladás-fotók kigyűjtése a lokális fájlokból
        val photoFiles = photoRepo.photoDir().listFiles { f -> f.extension == "jpg" }?.sortedByDescending { it.lastModified() } ?: emptyList()

        _uiState.value = _uiState.value.copy(
            muscleLoads = loadModels,
            exercises = exercises,
            exercise1RmTrend = trend,
            currentWeightKg = curW,
            startWeightKg = startW,
            weightDiffKg = diff,
            filteredWeights = filtered,
            progressPhotos = photoFiles
        )
    }

    fun selectExercise(exerciseId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val workouts = database.workoutHistoryDao().getAllWorkoutsWithDetails()
            val trend = calculate1RmTrend(workouts, exerciseId)
            _uiState.value = _uiState.value.copy(
                selectedExerciseId = exerciseId,
                exercise1RmTrend = trend
            )
        }
    }

    fun setWeightRange(range: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val weights = database.bodyWeightDao().getAllWeights()
            val filtered = filterWeightsByRange(weights, range)
            _uiState.value = _uiState.value.copy(weightRange = range, filteredWeights = filtered)
        }
    }

    fun addWeightEntry(kg: Double, dateIso: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val dayKey = if (dateIso.length >= 10) dateIso.substring(0, 10) else dateIso
            database.bodyWeightDao().upsertWeight(
                BodyWeightEntryEntity(
                    id = id,
                    date = dateIso,
                    dayKey = dayKey,
                    kg = kg,
                    updatedAt = System.currentTimeMillis(),
                    rawJson = "{\"id\":\"$id\",\"date\":\"$dateIso\",\"kg\":$kg}"
                )
            )
            loadProgressData()
        }
    }

    private fun calculate1RmTrend(workouts: List<WorkoutHistoryWithDetails>, exerciseId: String): List<Exercise1RmPointUiModel> {
        val points = ArrayList<Exercise1RmPointUiModel>()
        for (w in workouts.sortedBy { it.workout.startedEpochMs }) {
            var bestW = 0.0
            var bestR = 0
            var bestE1rm = 0.0
            for (ex in w.exercises.filter { it.exercise.exerciseId == exerciseId }) {
                for (s in ex.sets.filter { it.done }) {
                    val weight = s.weight ?: 0.0
                    val reps = s.reps.toIntOrNull() ?: 0
                    if (weight > 0.0 && reps > 0) {
                        val e1rm = weight * (1.0 + reps / 30.0)
                        if (e1rm > bestE1rm) {
                            bestE1rm = e1rm
                            bestW = weight
                            bestR = reps
                        }
                    }
                }
            }
            if (bestE1rm > 0.0) {
                points.add(Exercise1RmPointUiModel(w.workout.started.take(10), bestW, bestR, bestE1rm))
            }
        }
        return points
    }

    private fun filterWeightsByRange(weights: List<BodyWeightEntryEntity>, range: String): List<BodyWeightEntryEntity> {
        if (range == "all") return weights
        val days = range.toLongOrNull() ?: 30L
        val cutoff = LocalDate.now().minusDays(days).toString()
        return weights.filter { it.dayKey >= cutoff }
    }
}
