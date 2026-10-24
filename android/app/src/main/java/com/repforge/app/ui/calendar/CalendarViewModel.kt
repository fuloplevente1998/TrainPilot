package com.repforge.app.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.WearSyncRepository
import com.repforge.app.data.db.CalendarHistorySummary
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

internal data class CalendarUiState(
    val month: YearMonth = YearMonth.now(), val day: LocalDate = LocalDate.now(),
    val plans: List<CalendarScheduleEntity> = emptyList(), val history: List<CalendarHistorySummary> = emptyList(),
    val programs: List<ProgramWithDays> = emptyList(), val language: String = "hu",
    val busy: Boolean = false, val error: Boolean = false, val loaded: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
internal class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.applicationContext
    private val db = TrainPilotDatabase.getInstance(app)
    private val repository = NativeCalendarRepository(db) {
        com.repforge.app.ActiveWorkoutStore.current(app)?.takeIf { it.optBoolean("active") }?.optString("scheduleId")
    }
    private val month = MutableStateFlow(YearMonth.now())
    private val queryRevision = MutableStateFlow(0)
    private val _state = MutableStateFlow(CalendarUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            TrainPilotSettingsStore(app).settingsFlow.collect { settings ->
                _state.update { it.copy(language = settings.language) }
            }
        }
        viewModelScope.launch { db.programDao().observeProgramsWithDays().collect { programs ->
            _state.update { it.copy(programs = programs) }
        } }
        viewModelScope.launch {
            combine(month, queryRevision) { selected, _ -> selected }.flatMapLatest { selected ->
                val zone = ZoneId.systemDefault()
                combine(db.calendarScheduleDao().observeEntriesInRange(selected.atDay(1).toString(), selected.atEndOfMonth().toString()),
                    db.workoutHistoryDao().observeCalendarHistory(selected.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
                        selected.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli())) { plans, history ->
                    Triple(selected, plans.filterNot { NativeCalendarRepository.cancelled(it) }, history)
                }.catch { error ->
                    if (error is CancellationException) throw error
                    _state.update { it.copy(error = true) }
                }
            }.collect { (selected, plans, history) ->
                if (selected == month.value) _state.update { it.copy(plans = plans, history = history, loaded = true) }
            }
        }
    }

    fun selectDay(day: LocalDate) { if (YearMonth.from(day) == month.value) _state.update { it.copy(day = day) } }
    fun changeMonth(delta: Long) {
        val next = month.value.plusMonths(delta)
        if (next.year !in 1900..2200) return
        _state.update { it.copy(month = next, day = next.atDay(1), plans = emptyList(), history = emptyList(), loaded = false) }
        month.value = next
    }
    fun clearError() {
        _state.update { it.copy(error = false) }
        queryRevision.update { it + 1 }
    }
    fun save(edit: NativeCalendarRepository.Edit, saved: () -> Unit) = mutate {
        repository.save(edit); withContext(Dispatchers.Main) { saved() }
    }
    fun status(row: CalendarScheduleEntity, value: String, delete: Boolean = false) = mutate { repository.changeStatus(row, value, delete) }

    private fun mutate(action: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { com.repforge.app.NativeDataReplacement.mutate(app) { action() }; runCatching { WearSyncRepository.publishWatchHomeSnapshot(app) } }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(error = true) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }

    fun historyDay(row: CalendarHistorySummary): LocalDate? = runCatching {
        Instant.parse(row.started).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrNull()
}
