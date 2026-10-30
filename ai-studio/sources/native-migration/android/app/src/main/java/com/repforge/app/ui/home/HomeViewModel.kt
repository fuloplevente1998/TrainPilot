package com.repforge.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeDataReplacement
import com.repforge.app.WearLiveHealthStore
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.Duration
import kotlin.math.roundToInt

data class TodayMetricsUiModel(
    val sleepText: String = "—",
    val hrvText: String = "—",
    val caloriesText: String = "—",
    val pulseText: String = "—",
    val stepsText: String = "—",
    val workoutCountText: String = "—",
    val recoveryText: String = "—",
    val recovery: NativeHomeRecoveryPresentation.Model = NativeHomeRecoveryPresentation.Model(
        NativeHomeRecoveryPresentation.Source.NONE, null, null, null, false, false),
    val isPartial: Boolean = false
)

data class HomeWorkoutTarget(val programId: String, val dayId: String, val scheduleId: String? = null)

data class HomeUiState(
    val recommendedProgram: ProgramWithDays? = null,
    val nextTarget: HomeWorkoutTarget? = null,
    val nextDayExercisesCount: Int = 0,
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val todayMetrics: TodayMetricsUiModel = TodayMetricsUiModel(),
    val watchConnected: Boolean? = null,
    val watchBattery: Int? = null,
    val hasImportedDraft: Boolean = false,
    val watchState: String = ""
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = TrainPilotDatabase.getInstance(context)
    private val settingsStore = TrainPilotSettingsStore(context)
    private val repository = NativeHomeRepository(database)
    private var loading: Job? = null
    private val readEpoch = NativeHomeRecoveryLoadGate()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.invalidations().catch { error -> if (error is CancellationException) throw error else failed() }
                .collect { loadHomeData() }
        }
        viewModelScope.launch {
            // Theme/timer edits need no program/history read. Language and active program do.
            settingsStore.settingsFlow.map { it.activeProgramId to it.language }.distinctUntilChanged()
                .catch { error -> if (error is CancellationException) throw error else failed() }
                .collect { loadHomeData() }
        }
        viewModelScope.launch {
            database.keyValueDocumentDao().observeDocument("draft")
                .catch { error -> if (error is CancellationException) throw error else failed() }
                .collect { document -> _uiState.update { it.copy(hasImportedDraft = document != null) } }
        }
    }

    /** Cancelling an older read prevents its result from overwriting a changed plan or dataset. */
    fun loadHomeData() {
        val epoch = readEpoch.next()
        loading?.cancel()
        loading = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, loadFailed = false) }
            try {
                val (result, admission) = withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        val settings = settingsStore.current()
                        val now = Instant.now(); val zone = ZoneId.systemDefault()
                        val snapshot = repository.read(settings.activeProgramId, settings.language, now, zone)
                        val metrics = fetchTodayMetrics(now, zone, snapshot.workoutCount)
                        val liveHealth = WearLiveHealthStore.current(context)
                        HomeUiState(recommendedProgram = snapshot.program, nextTarget = snapshot.target,
                            nextDayExercisesCount = snapshot.exerciseCount, loading = false, todayMetrics = metrics,
                            watchConnected = liveHealth != null, watchState = liveHealth?.optString("state").orEmpty()) to
                            NativeDataReplacement.continuationForCurrentData(context)
                    }
                }
                admission.requireCurrent(context)
                if (readEpoch.isCurrent(epoch)) {
                    _uiState.update { result.copy(hasImportedDraft = it.hasImportedDraft) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (readEpoch.isCurrent(epoch)) failed() }
        }
    }

    private fun failed() {
        _uiState.update { it.copy(loading = false, loadFailed = true, recommendedProgram = null, nextTarget = null,
            nextDayExercisesCount = 0, todayMetrics = it.todayMetrics.copy(isPartial = true,
                recovery = it.todayMetrics.recovery.copy(stale = true, refreshFailed = true))) }
    }

    /**
     * Bounded canonical provider projection and recent effort records; the same
     * signal/readiness pipeline as Coach, without reading all history or writing
     * the journal. All SQLite, JSON and scoring work remains off the UI thread.
     */
    private suspend fun fetchTodayMetrics(now: Instant, zone: ZoneId, workoutCount: Int):
        TodayMetricsUiModel = withContext(Dispatchers.IO) {
        try {
            val today = now.atZone(zone).toLocalDate()
            val store = HealthJournalStore.get(context)
            val page = store.page(today.minusDays(14).toString(), today.toString(), "journal", "", 0L, 0L, 1)
            val summaries = page.optJSONArray("days")
            val days = (0 until (summaries?.length() ?: 0)).mapNotNull { summaries?.optJSONObject(it) }
            val dayObj = days.firstOrNull { it.optString("day") == today.toString() } ?: JSONObject()
            val history = database.workoutHistoryDao()
                .getCoachRecentHistory(now.minus(Duration.ofDays(14)).toEpochMilli(), now.toEpochMilli())
                .map { JSONObject(it) }
            val recovery = NativeHomeRecoveryPresentation.select(days, history, now, zone)

            val steps = dayObj.optDouble("steps", Double.NaN)
            val calories = dayObj.optDouble("activeCalories", Double.NaN)
            val pulse = dayObj.optDouble("averageHeartRate", Double.NaN)
            val hrv = dayObj.optDouble("hrvRmssdMs", Double.NaN)
            val sleepMinutes = dayObj.optDouble("sleepMinutes", Double.NaN)

            TodayMetricsUiModel(
                sleepText = if (sleepMinutes.isFinite() && sleepMinutes > 0.0) "${(sleepMinutes / 60.0 * 10.0).roundToInt() / 10.0} h" else "—",
                hrvText = if (hrv.isFinite() && hrv > 0.0) "${hrv.roundToInt()} ms" else "—",
                caloriesText = if (calories.isFinite() && calories > 0.0) "${calories.roundToInt()} kcal" else "—",
                pulseText = if (pulse.isFinite() && pulse > 0.0) "${pulse.roundToInt()} bpm" else "—",
                stepsText = if (steps.isFinite() && steps >= 0.0) steps.roundToInt().toString() else "—",
                workoutCountText = workoutCount.toString(),
                recoveryText = recovery.displayValue,
                recovery = recovery,
                isPartial = recovery.partial
            )
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            TodayMetricsUiModel(workoutCountText = workoutCount.toString(),
                recovery = NativeHomeRecoveryPresentation.Model(
                    NativeHomeRecoveryPresentation.Source.NONE, null, null, null,
                    partial = true, stale = true, refreshFailed = true), isPartial = true)
        }
    }
}
