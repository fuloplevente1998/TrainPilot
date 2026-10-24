package com.repforge.app.ui.programs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.WearSyncRepository
import com.repforge.app.NativeProgramEditor
import com.repforge.app.data.db.ExerciseLibraryEntity
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
    val isPersonalPlannerOpen: Boolean = false,
    val language: String = "hu",
    val trainingProfileJson: String = "{}",
    val writeFailed: Boolean = false,
    val exerciseChoices: List<ExerciseLibraryEntity> = emptyList(),
    val editorSaving: Boolean = false,
    val editorError: String? = null,
    val savedProgramId: String? = null,
    val deletedProgramId: String? = null
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
            try {
                val result = withContext(Dispatchers.IO) {
                    Triple(settingsStore.current(), database.programDao().getProgramsWithDays(),
                        database.exerciseLibraryDao().getAllExercises())
                }
                _uiState.value = _uiState.value.copy(activeProgramId = result.first.activeProgramId,
                    language = result.first.language, trainingProfileJson = result.first.profileJson,
                    programs = result.second,
                    exerciseChoices = result.third, writeFailed = false)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.value = _uiState.value.copy(writeFailed = true) }
        }
    }

    fun activateProgram(programId: String) {
        viewModelScope.launch {
            try {
                com.repforge.app.NativeDataReplacement.mutate(context) {
                    settingsStore.setActiveProgramId(programId)
                    _uiState.value = _uiState.value.copy(activeProgramId = programId, writeFailed = false)
                    withContext(Dispatchers.IO) { WearSyncRepository.publishWatchHomeSnapshot(context) }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.value = _uiState.value.copy(writeFailed = true) }
        }
    }

    fun saveProgram(opened: ProgramWithDays?, draft: NativeProgramEditor.Draft, language: String) {
        if (_uiState.value.editorSaving) return
        _uiState.value = _uiState.value.copy(editorSaving = true,
            editorError = null, savedProgramId = null, deletedProgramId = null)
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) {
                    NativeProgramEditor.save(context, database, opened, draft, language)
                }
                val all = withContext(Dispatchers.IO) { database.programDao().getProgramsWithDays() }
                _uiState.value = _uiState.value.copy(programs = all, savedProgramId = saved)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                _uiState.value = _uiState.value.copy(editorError = failure.message ?: "Program save failed")
            } finally { _uiState.value = _uiState.value.copy(editorSaving = false) }
        }
    }

    fun savePersonalProgram(preview: com.repforge.app.NativeProfileProgramPreview.Preview, language: String) {
        if (_uiState.value.editorSaving || !_uiState.value.isPersonalPlannerOpen) return
        _uiState.value = _uiState.value.copy(editorSaving = true,
            editorError = null, savedProgramId = null, deletedProgramId = null)
        viewModelScope.launch {
            try {
                val id = withContext(Dispatchers.IO) {
                    com.repforge.app.NativeProgramEditor.save(context, database,
                        preview.source, preview.draft, language,
                        com.repforge.app.NativeProfileProgramPreview.metadata(preview))
                }
                // A new copy is published before making it active; failure to activate
                // never mutates the template or the original active program.
                withContext(Dispatchers.IO) { settingsStore.setActiveProgramId(id) }
                val programs = withContext(Dispatchers.IO) { database.programDao().getProgramsWithDays() }
                _uiState.value = _uiState.value.copy(programs = programs,
                    activeProgramId = id, savedProgramId = id,
                    isPersonalPlannerOpen = false)
                try { withContext(Dispatchers.IO) { WearSyncRepository.publishWatchHomeSnapshot(context) } }
                catch (_: Exception) { /* durable program + active choice remain canonical; Wear retries */ }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                _uiState.value = _uiState.value.copy(
                    editorError = failure.message ?: "Personal program cannot be saved")
            } finally {
                _uiState.value = _uiState.value.copy(editorSaving = false)
            }
        }
    }

    fun deleteProgram(opened: ProgramWithDays) {
        if (_uiState.value.editorSaving) return
        _uiState.value = _uiState.value.copy(editorSaving = true,
            editorError = null, savedProgramId = null, deletedProgramId = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    NativeProgramEditor.delete(context, database, opened, settingsStore)
                }
                val all = withContext(Dispatchers.IO) { database.programDao().getProgramsWithDays() }
                _uiState.value = _uiState.value.copy(programs = all, deletedProgramId = opened.program.id)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                _uiState.value = _uiState.value.copy(editorError = failure.message ?: "Program delete blocked")
            } finally { _uiState.value = _uiState.value.copy(editorSaving = false) }
        }
    }

    fun clearEditorOutcome() {
        _uiState.value = _uiState.value.copy(editorError = null, savedProgramId = null, deletedProgramId = null)
    }

    fun openPersonalPlanner() {
        if (_uiState.value.editorSaving || _uiState.value.isPersonalPlannerOpen) return
        viewModelScope.launch {
            try {
                val fresh = withContext(Dispatchers.IO) {
                    Triple(settingsStore.current().profileJson,
                        database.programDao().getProgramsWithDays(),
                        database.exerciseLibraryDao().getAllExercises())
                }
                _uiState.value = _uiState.value.copy(
                    trainingProfileJson = fresh.first, programs = fresh.second,
                    exerciseChoices = fresh.third,
                    editorError = null, isPersonalPlannerOpen = true)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.value = _uiState.value.copy(writeFailed = true) }
        }
    }

    fun closePersonalPlanner() {
        _uiState.value = _uiState.value.copy(isPersonalPlannerOpen = false)
    }
}
