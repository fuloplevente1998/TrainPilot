package com.repforge.app.ui.health

import android.app.Application
import android.app.Activity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.BleDiscoveryRepository
import com.repforge.app.BleDiscoveryUiState
import com.repforge.app.HealthBackgroundJob
import com.repforge.app.HealthBackgroundService
import com.repforge.app.HealthConnectRepository
import com.repforge.app.HealthJournalStore
import com.repforge.app.SamsungHealthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.roundToInt

data class HealthMetricItemUiModel(
    val key: String,
    val label: String,
    val valueText: String,
    val unit: String,
    val sourceText: String,
    val isAvailable: Boolean
)

data class HealthDashboardUiState(
    val activeTab: String = "journal", // "journal" | "ble"
    val provider: String = "health_connect", // "health_connect" | "samsung_health"
    val dailySourcePreference: String = "auto", // "auto" | "priority" | specific origin
    val dailySourceOptions: List<String> = emptyList(),
    val logWatchSteps: Boolean = false,
    val bleBackground: Boolean = false,
    val hcBackground: Boolean = false,
    val hcBackgroundBlock: BackgroundBlock = BackgroundBlock.UNSUPPORTED,
    val bleBackgroundBlock: BackgroundBlock = BackgroundBlock.WATCH_NOT_READY,
    val hcBackgroundScheduled: Boolean = false,
    val bleBackgroundRunning: Boolean = false,
    val backgroundSaving: Boolean = false,
    val backgroundWriteFailed: Boolean = false,
    val metrics: List<HealthMetricItemUiModel> = emptyList(),
    val sleepSessionsSummaryText: String = "—",
    val lastSyncAt: String = "—",
    val bleState: BleDiscoveryUiState = BleDiscoveryUiState(),
    val hcSupported: Boolean = false,
    val hcGranted: Boolean = false,
    val samsungInstalled: Boolean = false,
    val samsungAvailable: Boolean = false,
    val statusMessage: String? = null,
    val language: String = "hu",
    val historyMonth: String = java.time.YearMonth.now().toString(),
    val selectedDay: String = LocalDate.now().toString(),
    val availableDays: List<String> = emptyList(),
    val historyChannel: String = "journal",
    val historyLoading: Boolean = false,
    val historyError: Boolean = false,
    val historyPartial: Boolean = false,
    val sourceSaving: Boolean = false,
    val sourceSaveFailed: Boolean = false
)

class HealthDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val healthStore = HealthJournalStore.get(context)
    val bleRepo = BleDiscoveryRepository(context)

    private val _uiState = MutableStateFlow(HealthDashboardUiState())
    val uiState: StateFlow<HealthDashboardUiState> = _uiState.asStateFlow()

    private var historyQuery = NativeHealthQuery()
    private var historyJob: kotlinx.coroutines.Job? = null

    init {
        observeBleState()
        loadHealthDashboard()
    }

    private fun observeBleState() {
        viewModelScope.launch {
            bleRepo.uiState.collect { bleState ->
                _uiState.value = _uiState.value.copy(bleState = bleState)
            }
        }
    }

    fun setTab(tab: String) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
        if (tab == "ble") bleRepo.refreshStatus()
        refreshBackgroundStatus()
    }

    fun shiftHistoryMonth(delta: Long) {
        runCatching { historyQuery.shift(delta) }.onSuccess { historyQuery = it; loadHealthDashboard() }
    }
    fun selectHistoryDay(day: String) {
        val parsed = runCatching { LocalDate.parse(day) }.getOrNull() ?: return
        if (java.time.YearMonth.from(parsed) != historyQuery.month || parsed > LocalDate.now()) return
        historyQuery = historyQuery.copy(day = parsed); loadHealthDashboard()
    }
    fun historyToday() { historyQuery = NativeHealthQuery(); loadHealthDashboard() }
    fun selectHistoryChannel(channel: String) {
        runCatching { historyQuery.copy(channel = channel) }.onSuccess { historyQuery = it; loadHealthDashboard() }
    }
    fun loadHealthDashboard() {
        // init also calls this method; query initialization must precede init.
        val query = historyQuery
        historyJob?.cancel()
        _uiState.value = _uiState.value.copy(historyLoading = true, historyError = false, metrics = emptyList(),
            selectedDay = query.day.toString(), historyMonth = query.month.toString(), historyChannel = query.channel,
            availableDays = emptyList(), sleepSessionsSummaryText = "—", historyPartial = false)
        historyJob = viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    com.repforge.app.NativeDataReplacement.mutate(context) {
                        val language = com.repforge.app.data.store.TrainPilotSettingsStore(context).current().language
                        val prefs = healthStore.preferences()
                        val page = NativeHealthHistory(healthStore).load(query)
                        Triple(
                            Triple(language, prefs, page),
                            listOf(HealthConnectRepository.isSupported(context), HealthConnectRepository.anyReadAllowed(context),
                                SamsungHealthRepository.isInstalled(context), SamsungHealthRepository.isSdkAvailable()),
                            NativeHealthBackgroundPolicy.snapshot(context, healthStore)
                        )
                    }
                }
                val (language, prefs, page) = loaded.first
                val access = loaded.second
                val background = loaded.third
                val text = com.repforge.app.ui.text.nativeResources(context, language)
                _uiState.value = _uiState.value.copy(language = language, historyLoading = false, historyError = false,
                    availableDays = page.days, metrics = NativeHealthMetrics.parse(context, page.selected, language),
                    historyPartial = page.selected.optBoolean("partial"),
                    provider = prefs.optString("provider", HealthJournalStore.HC),
                    dailySourcePreference = prefs.optString("hcDailySource", "auto"),
                    dailySourceOptions = page.dailyOrigins,
                    logWatchSteps = prefs.optBoolean("logWatchSteps"), bleBackground = prefs.optBoolean("bleBackground"),
                    hcBackground = prefs.optBoolean("hcBackground"),
                    hcBackgroundBlock = background.health, bleBackgroundBlock = background.ble,
                    hcBackgroundScheduled = background.healthScheduled,
                    bleBackgroundRunning = background.bleRunning,
                    sleepSessionsSummaryText = text.getString(com.repforge.app.R.string.health_history_sleep,
                        page.selected.optJSONArray("sleepSessions")?.length() ?: 0),
                    hcSupported = access[0], hcGranted = access[1], samsungInstalled = access[2], samsungAvailable = access[3])
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.value = _uiState.value.copy(historyLoading = false, historyError = true,
                metrics = emptyList(), dailySourceOptions = emptyList())
                refreshBackgroundStatus()
            }
        }
    }

    fun setDailySourcePreference(value: String) {
        val state = _uiState.value
        if (state.sourceSaving || state.historyLoading || state.historyError ||
            (value != "auto" && value != "priority" && value !in state.dailySourceOptions)) return
        _uiState.update { it.copy(sourceSaving = true, sourceSaveFailed = false) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    com.repforge.app.NativeDataReplacement.mutate(context) {
                        healthStore.setPreferences(JSONObject().put("hcDailySource", value))
                    }
                }
                loadHealthDashboard() // Read stored values/dirty state; no implicit SDK or permission request.
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(sourceSaveFailed = true) } }
            finally { _uiState.update { it.copy(sourceSaving = false) } }
        }
    }

    fun setProvider(newProvider: String, activity: Activity? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { healthStore.setPreferences(JSONObject().put("provider", newProvider)) }
            _uiState.value = _uiState.value.copy(provider = newProvider)
            loadHealthDashboard()
            if (newProvider == HealthJournalStore.SAMSUNG) refresh(activity, requestSamsungPermission = true)
        }
    }

    fun refresh(activity: Activity?, requestSamsungPermission: Boolean = false) {
        if (activity == null) return
        val selected = historyQuery.day
        viewModelScope.launch {
            val result = com.repforge.app.NativeHealthRefresh.readDay(context, activity, selected, requestSamsungPermission)
            val language = com.repforge.app.data.store.TrainPilotSettingsStore(context).current().language
            val text = com.repforge.app.ui.text.nativeResources(context, language)
            _uiState.value = _uiState.value.copy(statusMessage = text.getString(if (result.isSuccess)
                com.repforge.app.R.string.health_read_completed else com.repforge.app.R.string.coach_refresh_failed))
            if (result.isSuccess) loadHealthDashboard()
        }
    }

    /** Refresh system permission/job status independently from the potentially large journal query. */
    fun refreshBackgroundStatus() {
        viewModelScope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    NativeHealthBackgroundPolicy.snapshot(context, healthStore)
                }
                _uiState.update { it.copy(hcBackgroundBlock = snapshot.health,
                    bleBackgroundBlock = snapshot.ble, hcBackgroundScheduled = snapshot.healthScheduled,
                    bleBackgroundRunning = snapshot.bleRunning) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(backgroundWriteFailed = true) } }
        }
    }

    fun toggleBleBackground(enable: Boolean) {
        if (_uiState.value.backgroundSaving) return
        _uiState.update { it.copy(backgroundSaving = true, backgroundWriteFailed = false) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    com.repforge.app.NativeDataReplacement.mutate(context) {
                        if (enable && NativeHealthBackgroundPolicy.snapshot(context, healthStore).ble != BackgroundBlock.READY)
                            throw IllegalStateException("BLE background prerequisites missing")
                        val previous = healthStore.preferences()
                        val oldBg = previous.optBoolean("bleBackground")
                        val oldSteps = previous.optBoolean("logWatchSteps")
                        healthStore.setPreferences(JSONObject().put("bleBackground", enable).put("logWatchSteps", enable))
                        try {
                            if (enable) {
                                check(HealthBackgroundService.resume(context)) { "BLE service could not start" }
                            } else HealthBackgroundService.stop(context)
                        } catch (failure: Exception) {
                            healthStore.setPreferences(JSONObject().put("bleBackground", oldBg).put("logWatchSteps", oldSteps))
                            throw failure
                        }
                    }
                }
                loadHealthDashboard()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(backgroundWriteFailed = true) } }
            finally {
                _uiState.update { it.copy(backgroundSaving = false) }
                refreshBackgroundStatus()
            }
        }
    }

    fun toggleHcBackground(enable: Boolean) {
        if (_uiState.value.backgroundSaving) return
        _uiState.update { it.copy(backgroundSaving = true, backgroundWriteFailed = false) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    com.repforge.app.NativeDataReplacement.mutate(context) {
                        if (enable && NativeHealthBackgroundPolicy.snapshot(context, healthStore).health != BackgroundBlock.READY)
                            throw IllegalStateException("Health Connect background prerequisites missing")
                        val previous = healthStore.preferences().optBoolean("hcBackground")
                        healthStore.setPreferences(JSONObject().put("hcBackground", enable))
                        try { HealthBackgroundJob.schedule(context) }
                        catch (failure: Exception) {
                            healthStore.setPreferences(JSONObject().put("hcBackground", previous))
                            try { HealthBackgroundJob.schedule(context) } catch (_: Exception) {}
                            throw failure
                        }
                    }
                }
                loadHealthDashboard()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(backgroundWriteFailed = true) } }
            finally {
                _uiState.update { it.copy(backgroundSaving = false) }
                refreshBackgroundStatus()
            }
        }
    }
}
