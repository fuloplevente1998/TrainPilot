package com.repforge.app.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.NativeDataReplacement
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.ProgressPeriodTotals
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import java.time.ZoneId
import java.time.Instant
import java.time.LocalDate
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
    val periodTotals: ProgressPeriodTotals? = null,
    val exercises: List<ExerciseLibraryEntity> = emptyList(),
    val selectedExerciseId: String = "goblet-squat",
    val exercise1RmTrend: List<Exercise1RmPointUiModel> = emptyList(),
    val selectedStrengthRange: NativeStrengthRange = NativeStrengthRange.DAYS_90,
    val strengthLoading: Boolean = true,
    val strengthError: Boolean = false,
    val progressPhotos: List<NativeProgressPhoto> = emptyList(),
    val selectedPhotoId: String? = null,
    val photosLoading: Boolean = true,
    val photosError: Boolean = false,
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
    private val store = TrainPilotSettingsStore(context)

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()
    private var summaryJob: Job? = null
    private var summaryRequestedStamp: Pair<LocalDate, ZoneId>? = null
    private var trendJob: Job? = null
    private var trendRevision = 0L
    private var strengthRequestedStamp: Pair<LocalDate, ZoneId>? = null
    private var cardioJob: Job? = null
    private var cardioRevision = 0L
    private var cardioRequestedStamp: NativeCardioCalendarRefresh.Stamp? = null

    /**
     * Screen lifecycle and protected system broadcasts must refresh time buckets
     * even when Room has not changed. The periodic check only reloads on a
     * different local day or zone, so it does not poll Room once per minute.
     */
    fun onCardioScreenResumed() {
        refreshCardioForClock(NativeCardioCalendarRefresh.Trigger.SCREEN_RESUME)
        refreshStrengthForClock()
        refreshSummaryForClock()
    }

    fun onCardioSystemClockChanged() {
        refreshCardioForClock(NativeCardioCalendarRefresh.Trigger.SYSTEM_CLOCK_CHANGE)
        refreshStrengthForClock()
        refreshSummaryForClock()
    }

    fun onCardioCalendarTick() {
        refreshCardioForClock(NativeCardioCalendarRefresh.Trigger.CALENDAR_TICK)
        refreshStrengthForClock()
        refreshSummaryForClock()
    }

    private fun refreshSummaryForClock() {
        val zone = ZoneId.systemDefault()
        val stamp = LocalDate.now(zone) to zone
        if (!_uiState.value.loading && stamp != summaryRequestedStamp) loadProgressData()
    }

    private fun refreshStrengthForClock() {
        val zone = ZoneId.systemDefault()
        val stamp = LocalDate.now(zone) to zone
        if (!_uiState.value.loading && stamp != strengthRequestedStamp) reloadStrength()
    }

    private fun refreshCardioForClock(trigger: NativeCardioCalendarRefresh.Trigger) {
        val stamp = NativeCardioCalendarRefresh.stamp(System.currentTimeMillis(), ZoneId.systemDefault())
        if (NativeCardioCalendarRefresh.shouldRefresh(trigger, cardioRequestedStamp, stamp))
            reloadCardio(stamp)
    }

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
        trendJob?.cancel() // Same-ID strength reads from the previous dataset are stale too.
        trendRevision++
        summaryJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, loadError = false, photosLoading = true, photosError = false) }
            try {
                val (result, admission) = withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        val now = System.currentTimeMillis()
                        val zone = ZoneId.systemDefault()
                        val stamp = Instant.ofEpochMilli(now).atZone(zone).toLocalDate() to zone
                        val language = store.current().language
                        val exercises = db.exerciseLibraryDao().getAllExercises()
                        val groups = db.workoutHistoryDao().getProgressMuscleSummary(
                            now - 30L * 86_400_000L, now)
                        val (fromInclusive, toExclusive) = NativeProgressQuery.thirtyDayWindow(now, zone)
                        val totals = db.workoutHistoryDao().getProgressPeriodTotals(
                            fromInclusive, toExclusive)
                        val photos = runCatching { NativeProgressPhotoData.load(db) }
                        (Triple(Triple(language, exercises, groups), photos, totals) to stamp) to
                            NativeDataReplacement.continuationForCurrentData(context)
                    }
                }
                admission.requireCurrent(context)
                val language = result.first.first.first
                val exercises = result.first.first.second
                val muscle = NativeProgressQuery.muscle(result.first.first.third, language)
                val id = _uiState.value.selectedExerciseId
                    .takeIf { selected -> exercises.any { it.id == selected } }
                    ?: exercises.firstOrNull()?.id.orEmpty()
                summaryRequestedStamp = result.second
                _uiState.update { it.copy(
                    language = language, exercises = exercises, muscleLoads = muscle,
                    periodTotals = result.first.third,
                    progressPhotos = result.first.second.getOrDefault(emptyList()),
                    selectedPhotoId = it.selectedPhotoId?.takeIf { photoId ->
                        result.first.second.getOrNull()?.any { photo -> photo.id == photoId } == true
                    },
                    photosLoading = false, photosError = result.first.second.isFailure,
                    selectedExerciseId = id, loading = false, loadError = false
                ) }
                selectExercise(id)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(loading = false, loadError = true,
                periodTotals = null, photosLoading = false, photosError = true) } }
        }
    }

    fun selectExercise(exerciseId: String) {
        _uiState.update { it.copy(selectedExerciseId = exerciseId) }
        reloadStrength()
    }

    fun selectStrengthRange(range: NativeStrengthRange) {
        if (_uiState.value.selectedStrengthRange == range) return
        _uiState.update { it.copy(selectedStrengthRange = range) }
        reloadStrength()
    }

    fun retryStrength() = reloadStrength()

    fun openPhoto(id: String) {
        _uiState.update { current ->
            if (current.progressPhotos.any { it.id == id }) current.copy(selectedPhotoId = id) else current
        }
    }

    fun closePhoto() { _uiState.update { it.copy(selectedPhotoId = null) } }

    private fun reloadStrength() {
        trendJob?.cancel()
        val revision = ++trendRevision
        val selected = _uiState.value.selectedExerciseId
        val range = _uiState.value.selectedStrengthRange
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        strengthRequestedStamp = LocalDate.now(zone) to zone
        _uiState.update { it.copy(exercise1RmTrend = emptyList(), strengthLoading = true, strengthError = false) }
        trendJob = viewModelScope.launch {
            try {
                val (trend, admission) = withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        NativeProgressQuery.trend(db.workoutHistoryDao(), selected, range, now, zone) to
                            NativeDataReplacement.continuationForCurrentData(context)
                    }
                }
                admission.requireCurrent(context)
                if (revision != trendRevision) return@launch
                if (_uiState.value.selectedExerciseId != selected ||
                    _uiState.value.selectedStrengthRange != range) return@launch
                if ((LocalDate.now(ZoneId.systemDefault()) to ZoneId.systemDefault()) != strengthRequestedStamp) {
                    reloadStrength()
                    return@launch
                }
                _uiState.update { it.copy(exercise1RmTrend = trend,
                    strengthLoading = false, strengthError = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (revision == trendRevision) _uiState.update {
                    it.copy(strengthLoading = false, strengthError = true)
                }
            }
        }
    }

    fun selectCardioActivity(exerciseId: String) {
        if (_uiState.value.selectedCardioId == exerciseId) return
        _uiState.update { it.copy(selectedCardioId = exerciseId, cardioLoading = true, cardio = null) }
        reloadCardio()
    }

    fun retryCardio() = reloadCardio()

    private fun reloadCardio(
        stamp: NativeCardioCalendarRefresh.Stamp =
            NativeCardioCalendarRefresh.stamp(System.currentTimeMillis(), ZoneId.systemDefault())
    ) {
        cardioJob?.cancel()
        cardioRequestedStamp = stamp
        val revision = ++cardioRevision
        val selected = _uiState.value.selectedCardioId
        cardioJob = viewModelScope.launch {
            _uiState.update { it.copy(cardioLoading = true, cardioError = false, cardio = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        NativeCardioProgressAdapter.load(
                            db.workoutHistoryDao(), System.currentTimeMillis(),
                            stamp.zone, selected
                        )
                    }
                }
                if (revision != cardioRevision || _uiState.value.selectedCardioId != selected) return@launch
                // Time/date/zone can change while this paged Room read is running.
                // Do not publish its now-stale week buckets.
                val currentStamp = NativeCardioCalendarRefresh.stamp(
                    System.currentTimeMillis(), ZoneId.systemDefault())
                if (currentStamp != stamp) {
                    reloadCardio(currentStamp)
                    return@launch
                }
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
