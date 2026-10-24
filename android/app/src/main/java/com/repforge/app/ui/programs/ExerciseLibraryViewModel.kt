package com.repforge.app.ui.programs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.NativeDataReplacement
import com.repforge.app.NativeExerciseFavorites
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
    val writeFailed: Boolean = false
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
