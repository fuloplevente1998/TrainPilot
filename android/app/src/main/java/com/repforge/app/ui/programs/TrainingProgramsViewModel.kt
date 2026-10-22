package com.repforge.app.ui.programs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.WearSyncRepository
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TrainingProgramsUiState(
    val activeProgramId: String = "home-basic",
    val programs: List<ProgramWithDays> = emptyList(),
    val isPersonalPlannerOpen: Boolean = false
)

class TrainingProgramsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = TrainPilotDatabase.getInstance(context)
    private val settingsStore = TrainPilotSettingsStore(context)

    private val _uiState = MutableStateFlow(TrainingProgramsUiState())
    val uiState: StateFlow<TrainingProgramsUiState> = _uiState.asStateFlow()

    init {
        loadPrograms()
    }

    fun loadPrograms() {
        viewModelScope.launch {
            val settings = settingsStore.current()
            val progs = database.programDao().getProgramsWithDays()
            _uiState.value = TrainingProgramsUiState(
                activeProgramId = settings.activeProgramId,
                programs = progs
            )
        }
    }

    fun activateProgram(programId: String) {
        viewModelScope.launch {
            settingsStore.setActiveProgramId(programId)
            _uiState.value = _uiState.value.copy(activeProgramId = programId)
            withContext(Dispatchers.IO) {
                WearSyncRepository.publishWatchHomeSnapshot(context)
            }
        }
    }

    fun openPersonalPlanner() {
        _uiState.value = _uiState.value.copy(isPersonalPlannerOpen = true)
    }

    fun closePersonalPlanner() {
        _uiState.value = _uiState.value.copy(isPersonalPlannerOpen = false)
    }
}
