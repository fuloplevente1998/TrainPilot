package com.repforge.app.ui.coach

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.HealthJournalStore
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.time.*

internal data class CoachTarget(val programId: String, val dayId: String, val title: String,
    val schedule: CalendarScheduleEntity? = null, val upcoming: Boolean = false)
internal data class CoachUiState(val loading: Boolean = true, val error: Boolean = false, val language: String = "hu",
    val readiness: NativeCoachReadiness.Result? = null, val target: CoachTarget? = null,
    val refreshing: Boolean = false, val refreshFailed: Boolean = false,
    val variationPreview: NativeCoachVariation.Preview? = null, val variationBusy: Boolean = false,
    val variationFailed: Boolean = false, val variationSaved: Boolean = false,
    val exercises: List<CoachExerciseAdvice> = emptyList())

/** Reads canonical provider projections and a bounded history slice; never fabricates Health samples. */
internal class CoachViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.applicationContext
    private val db = TrainPilotDatabase.getInstance(app)
    private val _state = MutableStateFlow(CoachUiState())
    val state = _state.asStateFlow()
    private var loading: Job? = null
    init { reload() }
    fun previewVariation(exerciseId: String) {
        val target = _state.value.target ?: return
        if (_state.value.variationBusy || _state.value.loading) return
        _state.value = _state.value.copy(variationBusy = true, variationFailed = false, variationSaved = false)
        viewModelScope.launch {
            try {
                val preview = withContext(Dispatchers.IO) { NativeCoachVariation(app, db).preview(target, exerciseId) }
                _state.value = _state.value.copy(variationPreview = preview)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.value = _state.value.copy(variationFailed = true) }
            finally { _state.value = _state.value.copy(variationBusy = false) }
        }
    }
    fun closeVariation() { if (!_state.value.variationBusy) _state.value = _state.value.copy(variationPreview = null, variationFailed = false) }
    fun saveVariation() {
        val preview = _state.value.variationPreview ?: return
        if (_state.value.variationBusy) return
        _state.value = _state.value.copy(variationBusy = true, variationFailed = false)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { NativeCoachVariation(app, db).save(preview) }
                _state.value = _state.value.copy(variationPreview = null, variationSaved = true)
                try { withContext(Dispatchers.IO) { com.repforge.app.WearSyncRepository.publishWatchHomeSnapshot(app) } }
                catch (_: Exception) { /* Durable program remains available; normal Wear sync retries. */ }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.value = _state.value.copy(variationFailed = true) }
            finally { _state.value = _state.value.copy(variationBusy = false) }
        }
    }
    fun refreshHealth(activity: android.app.Activity?) {
        if (activity == null || _state.value.refreshing || _state.value.loading) return
        _state.value = _state.value.copy(refreshing = true, refreshFailed = false)
        viewModelScope.launch {
            try {
                val result = com.repforge.app.NativeHealthRefresh.readToday(app, activity)
                _state.value = _state.value.copy(refreshFailed = result.isFailure)
                // Read the canonical stored projections even on permission/provider failure;
                // stale or absent measurements remain subject to the same freshness rules.
                loading?.join()
                reload()
            } finally { _state.value = _state.value.copy(refreshing = false) }
        }
    }
    fun reload() {
        if (loading?.isActive == true || _state.value.variationBusy || _state.value.variationPreview != null) return
        loading = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = false)
            try {
                _state.value = withContext(Dispatchers.IO) {
                    com.repforge.app.NativeDataReplacement.mutate(app) {
                    val settings = TrainPilotSettingsStore(app).current()
                    val now = Instant.now(); val zone = ZoneId.systemDefault(); val today = now.atZone(zone).toLocalDate()
                    val page = HealthJournalStore.get(app).page(today.minusDays(14).toString(), today.toString(), "journal", "", 0, 0, 1)
                    val array = page.optJSONArray("days")
                    val days = (0 until (array?.length() ?: 0)).mapNotNull { array?.optJSONObject(it) }
                    val signals = NativeCoachSignals.fromDays(days, today, zone)
                    val history = db.workoutHistoryDao().getCoachRecentHistory(now.minus(Duration.ofDays(14)).toEpochMilli(), now.toEpochMilli()).map { JSONObject(it) }
                    val target = NativeCoachTargets(db).select(settings.activeProgramId, settings.language, now, zone)
                    val readiness = NativeCoachReadiness.calculate(signals.recovery, signals.daily, history, now, zone)
                    val advice = NativeCoachAdviceRepository(db).load(target, settings, readiness, now)
                    CoachUiState(false, false, settings.language, readiness, target,
                        refreshing = _state.value.refreshing, refreshFailed = _state.value.refreshFailed, exercises = advice)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                // Failed re-read must not erase a previously displayed, persisted readiness
                // snapshot; the visible error flag explicitly marks its uncertain freshness.
                _state.value = _state.value.copy(loading = false, error = true)
            }
        }
    }
}
