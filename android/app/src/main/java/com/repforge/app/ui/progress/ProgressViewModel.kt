package com.repforge.app.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.NativeDataReplacement
import com.repforge.app.WorkoutPhotoRepository
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import java.io.File
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MuscleLoadUiModel(
    val muscleKey: String,
    val label: String,
    val setsCount: Int,
    val loadPercentage: Float
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
    val progressPhotos: List<File> = emptyList(),
    val language: String = "hu",
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val selectedCardioId: String = NativeCardioModel.ALL_ACTIVITIES,
    val cardio: CardioModelResult? = null,
    val cardioActivities: List<CardioActivityOption> = emptyList(),
    val cardioLoading: Boolean = true,
    val cardioError: Boolean = false
)

class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val db = TrainPilotDatabase.getInstance(context)
    private val photoRepo = WorkoutPhotoRepository(context)
    private val store = TrainPilotSettingsStore(context)

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()
    private var summaryJob: Job? = null
    private var trendJob: Job? = null
    private var cardioJob: Job? = null
    private var cardioRevision = 0L

    init {
        // Unlike a count-based cache, this responds to same-count in-place edits.
        viewModelScope.launch {
            NativeProgressQuery.invalidations(db).collect {
                loadProgressData()
                reloadCardio()
            }
        }
        viewModelScope.launch {
            store.settingsFlow.map { it.language }.distinctUntilChanged().collect { language ->
                if (_uiState.value.language != language) {
                    _uiState.update { it.copy(language = language) }
                    loadProgressData()
                }
            }
        }
    }

    fun loadProgressData() {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, loadError = false) }
            try {
                val result = withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        val now = System.currentTimeMillis()
                        val language = store.current().language
                        val exercises = db.exerciseLibraryDao().getAllExercises()
                        val groups = db.workoutHistoryDao().getProgressMuscleSummary(
                            now - 30L * 86_400_000L, now)
                        val photos = photoRepo.photoDir()
                            .listFiles { f -> f.isFile && f.extension.equals("jpg", true) }
                            ?.sortedByDescending { it.lastModified() }.orEmpty()
                        Triple(language, exercises, groups) to photos
                    }
                }
                val language = result.first.first
                val exercises = result.first.second
                val muscle = NativeProgressQuery.muscle(result.first.third, language)
                val id = _uiState.value.selectedExerciseId
                    .takeIf { selected -> exercises.any { it.id == selected } }
                    ?: exercises.firstOrNull()?.id.orEmpty()
                _uiState.update { it.copy(
                    language = language, exercises = exercises, muscleLoads = muscle,
                    progressPhotos = result.second, selectedExerciseId = id,
                    loading = false, loadError = false
                ) }
                selectExercise(id)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(loading = false, loadError = true) } }
        }
    }

    fun selectExercise(exerciseId: String) {
        trendJob?.cancel()
        _uiState.update { it.copy(selectedExerciseId = exerciseId, exercise1RmTrend = emptyList()) }
        trendJob = viewModelScope.launch {
            try {
                val trend = withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        NativeProgressQuery.trend(db.workoutHistoryDao(), exerciseId)
                    }
                }
                _uiState.update { current ->
                    if (current.selectedExerciseId == exerciseId)
                        current.copy(exercise1RmTrend = trend, loadError = false)
                    else current
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(loadError = true) } }
        }
    }

    fun selectCardioActivity(exerciseId: String) {
        if (_uiState.value.selectedCardioId == exerciseId) return
        _uiState.update { it.copy(selectedCardioId = exerciseId, cardioLoading = true, cardio = null) }
        reloadCardio()
    }

    fun retryCardio() = reloadCardio()

    private fun reloadCardio() {
        cardioJob?.cancel()
        val revision = ++cardioRevision
        val selected = _uiState.value.selectedCardioId
        cardioJob = viewModelScope.launch {
            _uiState.update { it.copy(cardioLoading = true, cardioError = false, cardio = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        NativeCardioProgressAdapter.load(
                            db.workoutHistoryDao(), System.currentTimeMillis(),
                            ZoneId.systemDefault(), selected
                        )
                    }
                }
                if (revision != cardioRevision || _uiState.value.selectedCardioId != selected) return@launch
                if (selected.isNotEmpty() && result.activities.none { it.exerciseId == selected }) {
                    _uiState.update { it.copy(selectedCardioId = NativeCardioModel.ALL_ACTIVITIES) }
                    reloadCardio()
                    return@launch
                }
                _uiState.update { it.copy(cardio = result, cardioActivities = result.activities,
                    cardioLoading = false, cardioError = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (revision == cardioRevision && _uiState.value.selectedCardioId == selected)
                    _uiState.update { it.copy(cardio = null, cardioLoading = false, cardioError = true) }
            }
        }
    }
}
