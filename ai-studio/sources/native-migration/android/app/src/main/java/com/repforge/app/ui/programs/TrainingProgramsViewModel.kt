package com.repforge.app.ui.programs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.WearSyncRepository
import com.repforge.app.NativeProgramEditor
import com.repforge.app.NativeProgramActions
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.data.store.NativeTrainingModeChange
import com.repforge.app.ui.settings.NativeTrainingModeComparison
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class TrainingProgramsUiState(
    val activeProgramId: String = "home-basic",
    val programs: List<ProgramWithDays> = emptyList(),
    val isPersonalPlannerOpen: Boolean = false,
    val personalPreviews: Map<String, com.repforge.app.NativeProfileProgramPreview.Preview> = emptyMap(),
    val personalLoading: Boolean = false,
    val language: String = "hu",
    val trainingProfileJson: String = "{}",
    val defaultRestSeconds: Int = com.repforge.app.data.store.TrainPilotUserSettings().restSeconds,
    val trainingModes197Json: String? = null,
    val modeSaving: Boolean = false,
    val modeStatus: String? = null,
    val writeFailed: Boolean = false,
    val exerciseChoices: List<ExerciseLibraryEntity> = emptyList(),
    val editorSaving: Boolean = false,
    val editorError: String? = null,
    val savedProgramId: String? = null,
    val deletedProgramId: String? = null,
    val restorePreview: NativeProgramActions.RestorePreview? = null,
    val restoreLoading: Boolean = false,
    val restoreError: String? = null
)

internal class TrainingProgramsViewModel(application: Application) : AndroidViewModel(application) {
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
                    defaultRestSeconds = result.first.restSeconds,
                    trainingModes197Json = result.first.trainingModes197Json,
                    programs = result.second,
                    exerciseChoices = result.third, writeFailed = false)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.value = _uiState.value.copy(writeFailed = true) }
        }
    }

    fun approveTrainingMode(preview: NativeTrainingModeChange.Preview) = changeTrainingMode(R.string.native_modes_saved) {
        NativeTrainingModeChange.approve(settingsStore, preview)
    }

    fun approveTrainingModeCompared(
        preview: NativeTrainingModeChange.Preview,
        snapshot: NativeTrainingModeComparison.Snapshot
    ) = changeTrainingMode(R.string.native_modes_saved) {
        commitComparedTrainingMode(preview, snapshot)
    }

    /** Suspended seam for deterministic admission/race tests; one metadata lease. */
    internal suspend fun commitComparedTrainingMode(
        preview: NativeTrainingModeChange.Preview,
        snapshot: NativeTrainingModeComparison.Snapshot
    ): Boolean = com.repforge.app.NativeDataReplacement.mutate(context) {
        // ProgramEditor.save/delete/restore use this same metadata lease. Keep
        // read-only program/epoch revalidation and existing settings CAS in one
        // reentrant operation; never introduce a writer or an extra mutex.
        NativeTrainingModeComparison.isCurrent(context, preview, snapshot) &&
            NativeTrainingModeChange.approve(settingsStore, preview)
    }

    fun undoTrainingMode(programId: String) = changeTrainingMode(R.string.native_modes_reverted) {
        NativeTrainingModeChange.undo(settingsStore, "program", programId)
    }

    fun clearModeStatus() { _uiState.value = _uiState.value.copy(modeStatus = null) }

    private fun changeTrainingMode(successLabel: Int, change: suspend () -> Boolean) {
        if (_uiState.value.modeSaving) return
        _uiState.value = _uiState.value.copy(modeSaving = true, modeStatus = null)
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) { change() }
                val fresh = settingsStore.current()
                _uiState.value = _uiState.value.copy(
                    activeProgramId = fresh.activeProgramId, trainingModes197Json = fresh.trainingModes197Json,
                    modeStatus = nativeResources(context, fresh.language).getString(
                        if (success) successLabel else R.string.native_modes_error))
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) {
                _uiState.value = _uiState.value.copy(modeStatus =
                    nativeResources(context, _uiState.value.language).getString(R.string.native_modes_error))
            } finally { _uiState.value = _uiState.value.copy(modeSaving = false) }
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

    fun saveProgram(opened: ProgramWithDays?, draft: NativeProgramEditor.Draft, language: String,
        actionToken: String) {
        if (_uiState.value.editorSaving) return
        _uiState.value = _uiState.value.copy(editorSaving = true,
            editorError = null, savedProgramId = null, deletedProgramId = null)
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) {
                    NativeProgramEditor.save(context, database, opened, draft, language, actionToken = actionToken)
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

    fun previewRestoreDay(opened: ProgramWithDays, dayId: String) {
        if (_uiState.value.editorSaving || _uiState.value.restoreLoading) return
        _uiState.value = _uiState.value.copy(restoreLoading = true,
            restoreError = null, restorePreview = null)
        viewModelScope.launch {
            try {
                val preview = NativeProgramActions.preview(context, database, opened, dayId)
                _uiState.value = _uiState.value.copy(restorePreview = preview)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                _uiState.value = _uiState.value.copy(restoreError = failure.message ?: "Restore preview unavailable")
            } finally { _uiState.value = _uiState.value.copy(restoreLoading = false) }
        }
    }

    fun confirmRestoreDay() {
        if (_uiState.value.editorSaving || _uiState.value.restoreLoading) return
        val preview = _uiState.value.restorePreview ?: return
        _uiState.value = _uiState.value.copy(editorSaving = true, restoreError = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { NativeProgramActions.restore(context, database, preview) }
                val all = withContext(Dispatchers.IO) { database.programDao().getProgramsWithDays() }
                _uiState.value = _uiState.value.copy(programs = all, restorePreview = null,
                    restoreError = null)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                _uiState.value = _uiState.value.copy(restoreError = failure.message ?: "Factory day changed")
            } finally { _uiState.value = _uiState.value.copy(editorSaving = false) }
        }
    }

    fun dismissRestoreDay() {
        if (_uiState.value.editorSaving) return
        _uiState.value = _uiState.value.copy(restorePreview = null,
            restoreError = null, restoreLoading = false)
    }

    fun clearEditorOutcome() {
        _uiState.value = _uiState.value.copy(editorError = null, savedProgramId = null, deletedProgramId = null)
    }

    fun openPersonalPlanner() {
        if (_uiState.value.editorSaving || _uiState.value.isPersonalPlannerOpen) return
        _uiState.value = _uiState.value.copy(personalLoading = true,
            personalPreviews = emptyMap(), isPersonalPlannerOpen = true, editorError = null)
        viewModelScope.launch {
            try {
                val fresh = withContext(Dispatchers.IO) {
                    com.repforge.app.NativeDataReplacement.mutate(context) {
                        val settings = settingsStore.current()
                        val profile = settings.profileJson
                        val programs = database.programDao().getProgramsWithDays()
                        val exercises = database.exerciseLibraryDao().getAllExercises()
                        val at = java.time.Instant.now()
                        val previews = programs.filter { it.program.builtin &&
                            it.program.id in setOf("home-basic", "home-level2") }
                            .mapNotNull { source ->
                                val preview = try {
                                    com.repforge.app.NativeProfileHistory128.preview(
                                        database, source, profile, _uiState.value.language, at,
                                        activeProgramId = settings.activeProgramId)
                                } catch (_: IllegalArgumentException) { null } ?: return@mapNotNull null
                                source.program.id to preview
                            }.toMap()
                        Triple(profile, programs, exercises) to previews
                    }
                }
                _uiState.value = _uiState.value.copy(
                    trainingProfileJson = fresh.first.first,
                    programs = fresh.first.second, exerciseChoices = fresh.first.third,
                    personalPreviews = fresh.second, editorError = null)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.value = _uiState.value.copy(writeFailed = true) }
            finally { _uiState.value = _uiState.value.copy(personalLoading = false) }
        }
    }

    fun closePersonalPlanner() {
        if (_uiState.value.editorSaving) return
        _uiState.value = _uiState.value.copy(isPersonalPlannerOpen = false,
            personalPreviews = emptyMap())
    }
}
