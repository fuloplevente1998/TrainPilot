package com.repforge.app.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.HealthJournalStore
import com.repforge.app.WearLiveHealthStore
import com.repforge.app.NativeDataReplacement
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.coach.NativeCoachTargets
import com.repforge.app.ui.coach.NativeCoachReadiness
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class TodayMetricsUiModel(
    val sleepMinutes: Int? = null,
    val restingHeartRate: Int? = null,
    val steps: Int? = null,
    val samsungEnergy: Int? = null,
    val samsungEnergySource: String? = null,
    val isPartial: Boolean = false,
    val isError: Boolean = false
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val todayMetrics: TodayMetricsUiModel = TodayMetricsUiModel(),
    val watchUpdatedAt: Long? = null,
    val hasImportedDraft: Boolean = false,
    val recommendedWorkout: HomeWorkoutRecommendation? = null,
    val recommendationError: Boolean = false
)

data class HomeWorkoutRecommendation(
    val programId: String,
    val dayId: String,
    val scheduleId: String?,
    val programTitle: String,
    val dayTitle: String
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = TrainPilotDatabase.getInstance(context)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var homeDataJob: Job? = null

    init {
        viewModelScope.launch {
            database
                .keyValueDocumentDao()
                .observeDocument("draft")
                .collect { document ->
                    _uiState.value = _uiState.value.copy(hasImportedDraft = document != null)
                }
        }
    }

    fun loadHomeData(language: String) {
        homeDataJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, recommendationError = false)
        homeDataJob = viewModelScope.launch {
            val today = LocalDate.now().toString()
            val metrics = fetchTodayMetrics(today)
            val watch = WearLiveHealthStore.current(context)
            var recommendationError = false
            val recommendation = try {
                withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        val settings = TrainPilotSettingsStore(context).current()
                        val target = NativeCoachTargets(database).select(
                            activeProgramId = settings.activeProgramId,
                            language = language,
                            now = Instant.now(),
                            zone = ZoneId.systemDefault()
                        )
                        target?.let {
                            HomeWorkoutRecommendation(
                                programId = it.programId,
                                dayId = it.dayId,
                                scheduleId = it.schedule?.id,
                                programTitle = it.programTitle,
                                dayTitle = it.title
                            )
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("HomeViewModel", "Could not load the recommended workout", error)
                recommendationError = true
                null
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                todayMetrics = metrics,
                watchUpdatedAt = watch?.optLong("updatedAt")?.takeIf { it > 0L },
                recommendedWorkout = recommendation,
                recommendationError = recommendationError
            )
        }
    }

    private suspend fun fetchTodayMetrics(today: String): TodayMetricsUiModel = withContext(Dispatchers.IO) {
        try {
            val page = HealthJournalStore.get(context).page(today, today, "journal", "", 0L, 0L, 1)
            val days = page.optJSONArray("days")
            val day = if (days != null && days.length() > 0) days.getJSONObject(0) else JSONObject()
            if (day.length() == 0) return@withContext TodayMetricsUiModel()

            val readiness = NativeCoachReadiness.calculate(
                recovery = day,
                daily = day,
                history = emptyList(),
                now = Instant.now(),
                zone = ZoneId.systemDefault()
            )
            TodayMetricsUiModel(
                sleepMinutes = readiness.sleepMinutes?.takeIf { it > 0.0 }?.roundToInt(),
                restingHeartRate = positiveInt(day.optDouble("restingHeartRate", Double.NaN), 300.0),
                steps = nonNegativeInt(day.optDouble("steps", Double.NaN)),
                samsungEnergy = readiness.samsungScore?.roundToInt(),
                samsungEnergySource = readiness.samsungSource,
                isPartial = day.optBoolean("partial", false)
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("HomeViewModel", "Could not load today's health metrics", error)
            TodayMetricsUiModel(isError = true)
        }
    }

    private fun positiveInt(value: Double, maximum: Double): Int? =
        value.takeIf { it.isFinite() && it > 0.0 && it <= maximum }?.roundToInt()

    private fun nonNegativeInt(value: Double): Int? =
        value.takeIf { it.isFinite() && it >= 0.0 && it <= Int.MAX_VALUE }?.roundToInt()
}
