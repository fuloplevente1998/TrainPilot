package com.repforge.app.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class WeightJournalUiState(
    val overview: WeightJournalOverviewData? = null,
    val range: String = "30",
    val language: String = "hu",
    val loading: Boolean = true,
    val readFailed: Boolean = false,
    val writing: Boolean = false,
    val writeFailed: Boolean = false,
    val staleEntry: Boolean = false
) {
    val currentKg: Double? get() = overview?.summary?.currentKg
    val startKg: Double? get() = overview?.summary?.firstKg
}

/** Weight writes/observation never reload workout relations or recalculate exercise progress. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WeightJournalViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = TrainPilotDatabase.getInstance(context)
    private val settingsStore = TrainPilotSettingsStore(context)
    private val repository = NativeWeightJournalRepository(context, database)
    private val _uiState = MutableStateFlow(WeightJournalUiState())
    val uiState = _uiState.asStateFlow()
    private data class OverviewRequest(val range: String = "30", val revision: Long = 0)
    private val overviewRequest = MutableStateFlow(OverviewRequest())
    val entries = repository.pages().cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            settingsStore.settingsFlow.catch { error ->
                if (error is CancellationException) throw error
                _uiState.update { it.copy(readFailed = true) }
            }.collect { settings ->
                _uiState.update { it.copy(language = settings.language) }
                val range = settings.weightRange.takeIf { it in listOf("7", "30", "90", "365", "all") } ?: "30"
                overviewRequest.update { it.copy(range = range) }
            }
        }
        viewModelScope.launch {
            overviewRequest.flatMapLatest { request ->
                repository.observeOverview(request.range, LocalDate.now())
                    .map { request to it }
                    .onStart { _uiState.update { it.copy(range = request.range, overview = null,
                        loading = true, readFailed = false) } }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        if (overviewRequest.value == request)
                            _uiState.update { it.copy(overview = null, loading = false, readFailed = true) }
                    }
            }.collect { (request, overview) ->
                if (overviewRequest.value == request)
                    _uiState.update { it.copy(overview = overview, loading = false, readFailed = false) }
            }
        }
    }

    fun retryReads() { overviewRequest.update { it.copy(revision = it.revision + 1) } }

    fun setRange(range: String) {
        require(range in listOf("7", "30", "90", "365", "all"))
        write({}) { settingsStore.setWeightRange(range) }
    }

    fun clearWriteError() { _uiState.update { it.copy(writeFailed = false, staleEntry = false) } }
    fun add(kg: Double, date: String, saved: () -> Unit) = write(saved) { repository.add(kg, date) }
    fun edit(entry: BodyWeightEntryEntity, kg: Double, saved: () -> Unit) = write(saved) { repository.edit(entry, kg) }
    fun delete(entry: BodyWeightEntryEntity, saved: () -> Unit) = write(saved) { repository.delete(entry) }

    private fun write(saved: () -> Unit, operation: suspend () -> Unit) {
        if (_uiState.value.writing) return
        _uiState.update { it.copy(writing = true, writeFailed = false, staleEntry = false) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { operation() }
                saved() // Dialog only closes after the durable write succeeds.
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: NativeWeightJournalRepository.ChangedEntry) {
                _uiState.update { it.copy(staleEntry = true) }
            } catch (_: Exception) {
                _uiState.update { it.copy(writeFailed = true) }
            } finally { _uiState.update { it.copy(writing = false) } }
        }
    }
}
