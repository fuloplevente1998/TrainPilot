package com.repforge.app.ui.coach

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.HealthJournalStore
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.calendar.NativeCalendarRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.time.*

internal data class CoachTarget(val programId: String, val dayId: String, val title: String,
    val schedule: CalendarScheduleEntity? = null, val upcoming: Boolean = false)
internal data class CoachUiState(val loading: Boolean = true, val error: Boolean = false, val language: String = "hu",
    val readiness: NativeCoachReadiness.Result? = null, val target: CoachTarget? = null)

/** Reads canonical provider projections and a bounded history slice; never fabricates Health samples. */
internal class CoachViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.applicationContext
    private val db = TrainPilotDatabase.getInstance(app)
    private val _state = MutableStateFlow(CoachUiState())
    val state = _state.asStateFlow()
    private var loading: Job? = null
    init { reload() }
    fun reload() {
        if (loading?.isActive == true) return
        loading = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = false)
            try {
                _state.value = withContext(Dispatchers.IO) {
                    val settings = TrainPilotSettingsStore(app).current()
                    val now = Instant.now(); val zone = ZoneId.systemDefault(); val today = now.atZone(zone).toLocalDate()
                    val page = HealthJournalStore.get(app).page(today.minusDays(14).toString(), today.toString(), "journal", "", 0, 0, 1)
                    val array = page.optJSONArray("days")
                    val days = (0 until (array?.length() ?: 0)).mapNotNull { array?.optJSONObject(it) }
                    val signals = NativeCoachSignals.fromDays(days, today, zone)
                    val history = db.workoutHistoryDao().getCoachRecentHistory(now.minus(Duration.ofDays(14)).toEpochMilli(), now.toEpochMilli()).map { JSONObject(it) }
                    val programs = db.programDao().getProgramsWithDays()
                    var offset = 0
                    var plan: CalendarScheduleEntity? = null
                    while (plan == null) {
                        val rows = db.calendarScheduleDao().getCoachUpcoming(today.toString(), 32, offset)
                        plan = rows.firstOrNull { row -> !NativeCalendarRepository.cancelled(row) && programs.any { p ->
                            p.program.id == row.programId && p.days.any { it.dayId == row.dayId } } }
                        if (rows.size < 32) break
                        offset += rows.size
                    }
                    fun title(hu: String, en: String, de: String, ro: String) = when(settings.language) {
                        "en" -> en; "de" -> de; "ro" -> ro; else -> hu
                    }.ifBlank { hu }
                    val target = if (plan != null) {
                        val row = plan!!; val p = programs.first { it.program.id == row.programId }
                        val day = p.days.first { it.dayId == row.dayId }
                        CoachTarget(p.program.id, day.dayId, title(day.nameHu, day.nameEn, day.nameDe, day.nameRo), row, row.date != today.toString())
                    } else {
                        val p = programs.firstOrNull { it.program.id == settings.activeProgramId }
                        if (p == null || p.days.isEmpty()) null else {
                            var last: CalendarScheduleEntity? = null; var cycleOffset = 0
                            while (last == null) {
                                val rows = db.calendarScheduleDao().getCoachCyclePlans(p.program.id, 32, cycleOffset)
                                last = rows.firstOrNull { !NativeCalendarRepository.cancelled(it) }
                                if (rows.size < 32) break
                                cycleOffset += rows.size
                            }
                            val ordered = p.days.sortedBy { it.sortOrder }
                            val index = last?.let { row -> ordered.indexOfFirst { it.dayId == row.dayId } } ?: -1
                            val day = ordered[(index + 1).coerceAtLeast(0) % ordered.size]
                            CoachTarget(p.program.id, day.dayId, title(day.nameHu, day.nameEn, day.nameDe, day.nameRo))
                        }
                    }
                    CoachUiState(false, false, settings.language,
                        NativeCoachReadiness.calculate(signals.recovery, signals.daily, history, now, zone), target)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.value = _state.value.copy(loading = false, error = true) }
        }
    }
}
