package com.repforge

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HistoryUiState(
    val completedWorkouts: List<EngineWorkoutSnapshot> = emptyList(),
    val personalRecords: List<PrEntry226> = emptyList(),
    val prDetailViewState: PrDetailViewState226 = PrDetailViewState226(),
    val totalVolumeKg: Double = 0.0,
    val totalWorkoutsCount: Int = 0,
    val selectedFilter: String = "ALL"
)

class HistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    /**
     * v1.3.71: Megjavított PR részletek megnyitása.
     * Kiszámolja a formulákat és a fejlődési deltat, majd beállítja a modál állapotot.
     */
    fun onOpenPrDetails(pr: PrEntry226) {
        val estimates = PersonalRecordCalculator226.getFormulaEstimates(pr.weightKg, pr.reps)
        val breakdown = PersonalRecordBreakdown226(
            exerciseName = pr.exerciseName,
            category = pr.category,
            newValue = pr.value,
            oldValue = null,
            deltaKg = 0.0,
            deltaPercent = 0.0,
            reps = pr.reps,
            weightKg = pr.weightKg,
            formulaEstimates = estimates,
            localizedCategoryName = mapOf(
                "hu" to pr.category.name,
                "en" to pr.category.name,
                "de" to pr.category.name,
                "ro" to pr.category.name
            ),
            localizedDescription = mapOf(
                "hu" to "Rekord: %.1f kg (%d ismétlés)".format(pr.value, pr.reps),
                "en" to "Record: %.1f kg (%d reps)".format(pr.value, pr.reps),
                "de" to "Rekord: %.1f kg (%d Wdh.)".format(pr.value, pr.reps),
                "ro" to "Record: %.1f kg (%d rep.)".format(pr.value, pr.reps)
            )
        )

        _uiState.update {
            it.copy(
                prDetailViewState = PrDetailViewState226(
                    isOpen = true,
                    activeBreakdown = breakdown
                )
            )
        }
    }

    /**
     * v1.3.71: Megjavított PR részletek tiszta bezárása.
     */
    fun onClosePrDetails() {
        _uiState.update {
            it.copy(prDetailViewState = PrDetailViewState226(isOpen = false, activeBreakdown = null))
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }
}
