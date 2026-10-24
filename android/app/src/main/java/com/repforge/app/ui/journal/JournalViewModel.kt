package com.repforge.app.ui.journal

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.BackupRestoreManager
import com.repforge.app.GpsRouteStore
import com.repforge.app.WearSyncRepository
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.shared.GpsRouteLedger193
import kotlinx.coroutines.Dispatchers
import androidx.paging.cachedIn
import androidx.room.withTransaction
import kotlinx.coroutines.flow.*
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
    val accuracyMeters: Double?,
    val missingRouteParts: Int = 0
)

data class JournalUiState(
    val query: String = "", val month: java.time.YearMonth? = null,
    val selectedWorkoutId: String? = null,
    val detail: WorkoutHistoryWithDetails? = null,
    val detailLoading: Boolean = false, val detailError: Boolean = false,
    val activeRoute: RouteCoordinatesUiModel? = null,
    val routeLoading: Boolean = false, val routeError: Boolean = false,
    val workoutToDelete: WorkoutHistoryWithDetails? = null,
    val deleting: Boolean = false, val operationFailed: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = TrainPilotDatabase.getInstance(context)
    private val backupManager = BackupRestoreManager(context)
    private val repository = NativeJournalRepository(database)
    private val filters = MutableStateFlow(JournalFilter())
    private data class DetailRequest(val id: String, val revision: Long)
    private data class DetailResult(val request: DetailRequest?, val workout: WorkoutHistoryWithDetails?, val error: Boolean = false)
    private val selected = MutableStateFlow<DetailRequest?>(null)
    private var requestRevision = 0L
    private var routeRevision = 0L
    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()
    val workouts = filters.debounce { if (it.query.isBlank()) 0L else 180L }.flatMapLatest { repository.pages(it) }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            selected.flatMapLatest { request ->
                if (request == null) flowOf(DetailResult(null, null))
                else database.workoutHistoryDao().observeWorkoutDetails(request.id)
                    .map { DetailResult(request, it) }
                    .catch { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        emit(DetailResult(request, null, true))
                    }
            }.collect { result ->
                if (selected.value == result.request) _uiState.update { it.copy(detail = result.workout,
                    detailLoading = false, detailError = result.error) }
            }
        }
    }
    fun setSearch(value: String) {
        _uiState.update { it.copy(query = value) }
        filters.value = filters.value.copy(query = value, zone = java.time.ZoneId.systemDefault())
    }
    fun setMonth(value: java.time.YearMonth?) {
        _uiState.update { it.copy(month = value) }
        filters.value = filters.value.copy(month = value, zone = java.time.ZoneId.systemDefault())
    }
    fun openDetails(id: String) {
        closeRouteMap()
        _uiState.update { it.copy(selectedWorkoutId = id, detail = null, detailLoading = true, detailError = false) }
        selected.value = DetailRequest(id, ++requestRevision)
    }
    fun closeDetails() {
        if (_uiState.value.deleting) return
        selected.value = null
        closeRouteMap()
        _uiState.update { it.copy(selectedWorkoutId = null, detail = null, detailLoading = false, detailError = false) }
    }
    fun openRouteMap(keys: List<String>) {
        val request = ++routeRevision
        val workoutId = _uiState.value.selectedWorkoutId
        _uiState.update { it.copy(routeLoading = true, routeError = false) }
        viewModelScope.launch {
            val coordinates = withContext(Dispatchers.IO) { WorkoutRouteCoordinates.loadAll(context, keys) }
            if (request == routeRevision && workoutId == _uiState.value.selectedWorkoutId) _uiState.update {
                it.copy(activeRoute = coordinates, routeLoading = false, routeError = coordinates == null)
            }
        }
    }
    fun closeRouteMap() {
        routeRevision++
        _uiState.update { it.copy(activeRoute = null, routeLoading = false, routeError = false) }
    }
    fun requestDeleteWorkout() { _uiState.update { it.copy(workoutToDelete = it.detail, operationFailed = false) } }
    fun cancelDeleteWorkout() { if (!_uiState.value.deleting) _uiState.update { it.copy(workoutToDelete = null) } }
    fun clearOperationError() { _uiState.update { it.copy(operationFailed = false) } }
    fun confirmDeleteWorkout() {
        val target = _uiState.value.workoutToDelete ?: return
        if (_uiState.value.deleting) return
        _uiState.update { it.copy(deleting = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    com.repforge.app.NativeDataReplacement.mutate(context) {
                    database.withTransaction {
                        val current = database.workoutHistoryDao().getWorkoutWithDetails(target.workout.id) ?: return@withTransaction
                        val w = current.workout
                        val aliases = listOf(w.id, w.syncId, w.workoutKey).filter { it.isNotBlank() }.distinct()
                        database.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(primaryId = w.id,
                            workoutKey = w.workoutKey, idsJson = JSONArray(aliases).toString(), deletedAt = System.currentTimeMillis()))
                        database.workoutHistoryDao().deleteWorkoutByIdOrKey(w.id)
                    }
                    // Private route cleanup follows durable deletion; an SQL failure never removes a live route.
                    if (target.workout.ownershipProtocol193 == 1) runCatching {
                        GpsRouteLedger193.open(context.noBackupFilesDir).deleteWorkout(target.workout.id)
                    }
                    }
                }
                _uiState.update { it.copy(deleting = false, workoutToDelete = null) }
                closeDetails()
                runCatching { WearSyncRepository.publishWatchHomeSnapshot(context) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(deleting = false, operationFailed = true) } }
        }
    }
    fun createCalendarIntent(workout: WorkoutHistoryWithDetails): Intent {
        val w = workout.workout
        return backupManager.createCalendarInsertIntent(title = "TrainPilot: ${w.programName} • ${w.dayName}".trim(' ', '•'),
            description = "", startMs = w.startedEpochMs, endMs = w.finishedEpochMs)
    }
    fun parsePrRecords(rawJson: String): List<PrRecordUiModel> = runCatching {
        val arr = JSONArray(rawJson)
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { obj ->
            PrRecordUiModel(obj.optString("exerciseId"), obj.optString("exerciseName"), obj.optString("kind"),
                obj.optString("value"), obj.optString("previous"))
        } }
    }.getOrDefault(emptyList())
}
