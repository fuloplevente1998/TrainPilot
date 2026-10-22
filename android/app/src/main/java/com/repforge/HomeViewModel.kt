package com.repforge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val readinessPercent: Int = 85,
    val energyScore: Int? = 88,
    val sleepDurationHours: Double = 7.75,
    val restingHeartRate: Int = 58,
    val coachAdviceHu: String = "Kiváló állapot. Magas intenzitás javasolt!",
    val coachAdviceEn: String = "Optimal recovery. Ready for high intensity!",
    val scheduledProgramDayTitle: String? = "Felsőtest A (Erő & Hipertrófia)",
    val hasActiveWorkout: Boolean = false,
    val recentPrs: List<PrEntry226> = emptyList()
)

class HomeViewModel(
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshHealthMetrics()
    }

    fun refreshHealthMetrics() {
        viewModelScope.launch {
            val metrics = healthConnectManager.fetchDailyRecoveryMetrics()
            _uiState.update {
                it.copy(
                    readinessPercent = metrics.readinessPercentage,
                    energyScore = metrics.samsungEnergyScore,
                    sleepDurationHours = metrics.sleepDurationMinutes / 60.0,
                    restingHeartRate = metrics.restingHeartRateBpm,
                    coachAdviceHu = metrics.coachAdviceHu,
                    coachAdviceEn = metrics.coachAdviceEn
                )
            }
        }
    }
}
