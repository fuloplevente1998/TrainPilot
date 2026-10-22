package com.repforge.app.ui.journal

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import __PACKAGE_NAME__.BackupRestoreManager
import __PACKAGE_NAME__.GpsRouteStore
import __PACKAGE_NAME__.WearSyncRepository
import __PACKAGE_NAME__.data.db.HistoryTombstoneEntity
import __PACKAGE_NAME__.data.db.TrainPilotDatabase
import __PACKAGE_NAME__.data.db.WorkoutHistoryWithDetails
import com.repforge.shared.GpsRouteLedger193
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class PrRecordUiModel(
    val exerciseId: String,
    val exerciseName: String,
    val kind: String,
    val value: String,
    val previous: String
)

data class RouteCoordinatesUiModel(
    val routeKey: String,
    val segments: List<List<Pair<Double, Double>>>,
    val pointCount: Int,
    val distanceMeters: Double,
    val accuracyMeters: Double?
)

data class JournalUiState(
    val workouts: List<WorkoutHistoryWithDetails> = emptyList(),
    val expandedWorkoutIds: Set<String> = emptySet(),
    val expandedPrWorkoutIds: Set<String> = emptySet(),
    val activeRoute: RouteCoordinatesUiModel? = null,
    val workoutToDelete: WorkoutHistoryWithDetails? = null,
    val statusMessage: String? = null
)

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = TrainPilotDatabase.getInstance(context)
    private val backupManager = BackupRestoreManager(context)

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        observeWorkouts()
    }

    private fun observeWorkouts() {
        viewModelScope.launch {
            database.workoutHistoryDao().observeAllWorkoutsWithDetails().collect { list ->
                _uiState.value = _uiState.value.copy(workouts = list)
            }
        }
    }

    fun toggleWorkoutExpanded(workoutId: String) {
        val current = _uiState.value.expandedWorkoutIds.toMutableSet()
        if (current.contains(workoutId)) current.remove(workoutId) else current.add(workoutId)
        _uiState.value = _uiState.value.copy(expandedWorkoutIds = current)
    }

    fun togglePrExpanded(workoutId: String) {
        val current = _uiState.value.expandedPrWorkoutIds.toMutableSet()
        if (current.contains(workoutId)) current.remove(workoutId) else current.add(workoutId)
        _uiState.value = _uiState.value.copy(expandedPrWorkoutIds = current)
    }

    fun openRouteMap(workout: WorkoutHistoryWithDetails, routeKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val coordinates = loadRouteCoordinates(routeKey)
            _uiState.value = _uiState.value.copy(activeRoute = coordinates)
        }
    }

    fun closeRouteMap() {
        _uiState.value = _uiState.value.copy(activeRoute = null)
    }

    private fun loadRouteCoordinates(key: String): RouteCoordinatesUiModel? {
        return try {
            if (key.startsWith("wear193:")) {
                val ledger = GpsRouteLedger193.open(context.noBackupFilesDir)
                val viewObj = ledger.view(key)
                val segmentsArr = viewObj.optJSONArray("segments") ?: JSONArray()
                val segmentsList = ArrayList<List<Pair<Double, Double>>>()
                for (i in 0 until segmentsArr.length()) {
                    val seg = segmentsArr.getJSONObject(i)
                    val ptsArr = seg.optJSONArray("points") ?: continue
                    val ptsList = ArrayList<Pair<Double, Double>>()
                    for (j in 0 until ptsArr.length()) {
                        val p = ptsArr.getJSONObject(j)
                        ptsList.add(p.getDouble("latitude") to p.getDouble("longitude"))
                    }
                    if (ptsList.isNotEmpty()) segmentsList.add(ptsList)
                }
                RouteCoordinatesUiModel(
                    routeKey = key,
                    segments = segmentsList,
                    pointCount = viewObj.optInt("pointCount", 0),
                    distanceMeters = viewObj.optDouble("distanceMeters", 0.0),
                    accuracyMeters = null
                )
            } else {
                val store = GpsRouteStore(context.noBackupFilesDir)
                val track = store.read(key)
                val segmentsList = track.segments.map {
                    it.points.map { p -> p.latitude to p.longitude }
                }
                RouteCoordinatesUiModel(
                    routeKey = key,
                    segments = segmentsList,
                    pointCount = track.pointCount,
                    distanceMeters = 0.0,
                    accuracyMeters = null
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun requestDeleteWorkout(workout: WorkoutHistoryWithDetails) {
        _uiState.value = _uiState.value.copy(workoutToDelete = workout)
    }

    fun cancelDeleteWorkout() {
        _uiState.value = _uiState.value.copy(workoutToDelete = null)
    }

    fun confirmDeleteWorkout() {
        val target = _uiState.value.workoutToDelete ?: return
        val id = target.workout.id
        viewModelScope.launch(Dispatchers.IO) {
            if (target.workout.ownershipProtocol193 == 1) {
                runCatching { GpsRouteLedger193.open(context.noBackupFilesDir).deleteWorkout(id) }
            }
            database.workoutHistoryDao().upsertTombstone(
                HistoryTombstoneEntity(
                    primaryId = id,
                    workoutKey = target.workout.workoutKey,
                    idsJson = JSONArray().put(id).toString(),
                    deletedAt = System.currentTimeMillis()
                )
            )
            database.workoutHistoryDao().deleteWorkoutByIdOrKey(id)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(workoutToDelete = null)
                WearSyncRepository.publishWatchHomeSnapshot(context)
            }
        }
    }

    fun createCalendarIntent(workout: WorkoutHistoryWithDetails): Intent {
        val w = workout.workout
        return backupManager.createCalendarInsertIntent(
            title = "TrainPilot: ${w.programName} • ${w.dayName}".trim(' ', '•'),
            description = "Időtartam: ${w.durationSeconds / 60} perc, ${w.completedSetsCount} sorozat",
            startMs = w.startedEpochMs,
            endMs = w.finishedEpochMs
        )
    }

    fun parsePrRecords(rawJson: String): List<PrRecordUiModel> {
        return try {
            val arr = JSONArray(rawJson)
            val list = ArrayList<PrRecordUiModel>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PrRecordUiModel(
                        exerciseId = obj.optString("exerciseId"),
                        exerciseName = obj.optString("exerciseName"),
                        kind = obj.optString("kind"),
                        value = obj.optString("value"),
                        previous = obj.optString("previous")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
