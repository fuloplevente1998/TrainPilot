package com.repforge.app.ui.programs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.NativeDataReplacement
import com.repforge.app.NativeExerciseFavorites
import com.repforge.app.NativeCustomExerciseEditor
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class ExerciseLibraryState(
    val rows: List<ExerciseLibraryEntity> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: Boolean = false,
    val writeFailed: Boolean = false,
    val editError: String? = null,
    val savedId: String? = null,
    val deletedId: String? = null
)

internal class ExerciseLibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val db = TrainPilotDatabase.getInstance(context)
    private val _state = MutableStateFlow(ExerciseLibraryState())
    val state = _state.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = false) }
            try {
                val data = withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        db.exerciseLibraryDao().getAllExercises() to NativeExerciseFavorites.load(db).toSet()
                    }
                }
                _state.update { it.copy(rows = data.first, favorites = data.second, loading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(error = true, loading = false) } }
        }
    }

    fun saveCustom(id: String?, draft: NativeCustomExerciseEditor.Draft, language: String,
        expectedRawJson: String? = null) {
        if (_state.value.saving || _state.value.loading || _state.value.error) return
        _state.update { it.copy(saving = true, editError = null, savedId = null, deletedId = null) }
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) {
                    NativeCustomExerciseEditor.save(context, db, id, draft, language, expectedRawJson)
                }
                val data = withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        db.exerciseLibraryDao().getAllExercises() to NativeExerciseFavorites.load(db).toSet()
                    }
                }
                _state.update { it.copy(rows = data.first, favorites = data.second, savedId = saved) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { _state.update { it.copy(editError = failure.message ?: "Save failed") } }
            finally { _state.update { it.copy(saving = false) } }
        }
    }

    fun deleteCustom(id: String, expectedRawJson: String? = null) {
        if (_state.value.saving || _state.value.loading || _state.value.error) return
        _state.update { it.copy(saving = true, editError = null, savedId = null, deletedId = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { NativeCustomExerciseEditor.delete(context, db, id, expectedRawJson) }
                val data = withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        db.exerciseLibraryDao().getAllExercises() to NativeExerciseFavorites.load(db).toSet()
                    }
                }
                _state.update { it.copy(rows = data.first, favorites = data.second, deletedId = id) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                // Debug-only source locations, without exercise names, raw JSON or query values.
                if (com.repforge.app.BuildConfig.DEBUG) android.util.Log.e("NativeLibraryEdit",
                    "delete failed type=${failure.javaClass.simpleName} at " +
                        failure.stackTrace.take(6).joinToString(" <- "))
                _state.update { it.copy(editError = failure.message ?: "Delete blocked") }
            }
            finally { _state.update { it.copy(saving = false) } }
        }
    }

    fun clearEditOutcome() { _state.update { it.copy(savedId = null, deletedId = null, editError = null) } }

    fun toggleFavorite(id: String) {
        if (_state.value.saving || _state.value.loading || _state.value.error) return
        _state.update { it.copy(saving = true, writeFailed = false) }
        viewModelScope.launch {
            try {
                val updated = withContext(Dispatchers.IO) { NativeExerciseFavorites.toggle(context, db, id) }
                _state.update { it.copy(favorites = updated.toSet()) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(writeFailed = true) } }
            finally { _state.update { it.copy(saving = false) } }
        }
    }
}
