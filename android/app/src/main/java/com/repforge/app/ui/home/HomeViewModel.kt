package com.repforge.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.HealthDailySource
import com.repforge.app.HealthJournalStore
import com.repforge.app.HealthProviderProjection
import com.repforge.app.WearLiveHealthStore
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.roundToInt

data class TodayMetricsUiModel(
    val sleepText: String = "—",
    val hrvText: String = "—",
    val caloriesText: String = "—",
    val pulseText: String = "—",
    val stepsText: String = "—",
    val workoutCountText: String = "0",
    val recoveryText: String = "—",
    val isPartial: Boolean = false
)

data class HomeUiState(
    val activeProgram: ProgramWithDays? = null,
    val nextDayName: String = "Alap A",
    val nextDayId: String = "A",
    val nextDayExercisesCount: Int = 4,
    val todayMetrics: TodayMetricsUiModel = TodayMetricsUiModel(),
    val watchConnected: Boolean? = null,
    val watchBattery: Int? = null,
    val hasImportedDraft: Boolean = false,
    val watchState: String = "—"
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = TrainPilotDatabase.getInstance(context)
    private val settingsStore = TrainPilotSettingsStore(context)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
        viewModelScope.launch {
            database.keyValueDocumentDao().observeDocument("draft").collect { document ->
                _uiState.value = _uiState.value.copy(hasImportedDraft = document != null)
            }
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            val settings = settingsStore.current()
            val programs = database.programDao().getProgramsWithDays()
            val active = programs.firstOrNull { it.program.id == settings.activeProgramId } ?: programs.firstOrNull()
            val nextDay = active?.days?.firstOrNull()

            val today = LocalDate.now().toString()
            val workoutsCount = database.workoutHistoryDao().getWorkoutCount()
            val todayMetrics = fetchTodayMetrics(today, workoutsCount)
            val liveHealth = WearLiveHealthStore.current(context)

            _uiState.value = HomeUiState(
                hasImportedDraft = _uiState.value.hasImportedDraft,
                activeProgram = active,
                nextDayName = nextDay?.nameHu ?: "Alap A",
                nextDayId = nextDay?.dayId ?: "A",
                nextDayExercisesCount = nextDay?.exercisesJson?.let {
                    runCatching { org.json.JSONArray(it).length() }.getOrDefault(4)
                } ?: 4,
                todayMetrics = todayMetrics,
                watchConnected = liveHealth != null,
                watchBattery = null,
                watchState = liveHealth?.optString("state") ?: "Nincs kapcsolat"
            )
        }
    }

    private suspend fun fetchTodayMetrics(today: String, workoutCount: Int): TodayMetricsUiModel = withContext(Dispatchers.IO) {
        try {
            val store = HealthJournalStore.get(context)
            val page = store.page(today, today, "journal", "", 0L, 0L, 1)
            val summaries = page.optJSONArray("days")
            val dayObj = if (summaries != null && summaries.length() > 0) summaries.getJSONObject(0) else JSONObject()

            val steps = dayObj.optDouble("steps", Double.NaN)
            val calories = dayObj.optDouble("activeCalories", Double.NaN)
            val pulse = dayObj.optDouble("averageHeartRate", Double.NaN)
            val hrv = dayObj.optDouble("hrvRmssdMs", Double.NaN)
            val sleepMinutes = dayObj.optDouble("sleepMinutes", Double.NaN)
            val energyScore = dayObj.optDouble("energyScore", Double.NaN)

            TodayMetricsUiModel(
                sleepText = if (sleepMinutes.isFinite() && sleepMinutes > 0.0) "${(sleepMinutes / 60.0 * 10.0).roundToInt() / 10.0} h" else "—",
                hrvText = if (hrv.isFinite() && hrv > 0.0) "${hrv.roundToInt()} ms" else "—",
                caloriesText = if (calories.isFinite() && calories > 0.0) "${calories.roundToInt()} kcal" else "—",
                pulseText = if (pulse.isFinite() && pulse > 0.0) "${pulse.roundToInt()} bpm" else "—",
                stepsText = if (steps.isFinite() && steps >= 0.0) steps.roundToInt().toString() else "—",
                workoutCountText = workoutCount.toString(),
                recoveryText = if (energyScore.isFinite() && energyScore > 0.0) "${energyScore.roundToInt()}%" else "—",
                isPartial = dayObj.optBoolean("partial", false)
            )
        } catch (_: Exception) {
            TodayMetricsUiModel()
        }
    }
}
