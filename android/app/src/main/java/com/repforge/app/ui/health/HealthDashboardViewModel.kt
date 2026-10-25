package com.repforge.app.ui.health

import android.app.Application
import android.app.Activity
import android.os.Build
import android.os.SystemClock
import java.lang.ref.WeakReference
import com.repforge.app.NativeDataReplacement
import com.repforge.app.NativeHealthRefresh
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
import kotlinx.coroutines.Job
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
    val autoOnOpen: Boolean = false,
    val autoOnOpenSaving: Boolean = false,
    val autoOnOpenSaveFailed: Boolean = false,
    val healthRefreshing: Boolean = false,
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
    private var historyJob: Job? = null
    private val refreshGate = NativeHealthAutoRefreshGate()
    private var screenActivity: WeakReference<Activity>? = null
    private var screenVisible = false
    private var screenGeneration = 0L
    private var providerJob: Job? = null

    private fun invalidateRefresh() {
        screenGeneration++
        refreshGate.invalidate()
        providerJob?.cancel()
    }

    /** Called from the HEALTH navigation destination's lifecycle (not MainActivity alone). */
    fun healthScreenVisible(activity: Activity?) {
        if (activity == null) return
        if (!screenVisible || screenActivity?.get() !== activity) {
            invalidateRefresh()
            screenVisible = true
            screenActivity = WeakReference(activity)
        }
        if (!_uiState.value.historyLoading && _uiState.value.activeTab == "journal") {
            launchRefresh(activity, NativeHealthAutoRefreshGate.Trigger.AUTO)
        }
    }

    fun healthScreenHidden(activity: Activity?) {
        if (!screenVisible || (activity != null && screenActivity?.get() !== activity)) return
        screenVisible = false
        screenActivity = null
        invalidateRefresh()
    }

    override fun onCleared() {
        screenVisible = false
        invalidateRefresh()
        super.onCleared()
    }

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
        if (tab != _uiState.value.activeTab) invalidateRefresh()
        _uiState.value = _uiState.value.copy(activeTab = tab)
        if (tab == "ble") bleRepo.refreshStatus()
        else screenActivity?.get()?.let { launchRefresh(it, NativeHealthAutoRefreshGate.Trigger.AUTO) }
        refreshBackgroundStatus()
    }

    fun shiftHistoryMonth(delta: Long) {
        runCatching { historyQuery.shift(delta) }.onSuccess { invalidateRefresh(); historyQuery = it; loadHealthDashboard() }
    }
    fun selectHistoryDay(day: String) {
        val parsed = runCatching { LocalDate.parse(day) }.getOrNull() ?: return
        if (java.time.YearMonth.from(parsed) != historyQuery.month || parsed > LocalDate.now()) return
        invalidateRefresh(); historyQuery = historyQuery.copy(day = parsed); loadHealthDashboard()
    }
    fun historyToday() { invalidateRefresh(); historyQuery = NativeHealthQuery(); loadHealthDashboard() }
    fun selectHistoryChannel(channel: String) {
        runCatching { historyQuery.copy(channel = channel) }.onSuccess { invalidateRefresh(); historyQuery = it; loadHealthDashboard() }
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
                    autoOnOpen = prefs.optBoolean("autoOnOpen", false),
                    hcBackgroundBlock = background.health, bleBackgroundBlock = background.ble,
                    hcBackgroundScheduled = background.healthScheduled,
                    bleBackgroundRunning = background.bleRunning,
                    sleepSessionsSummaryText = text.getString(com.repforge.app.R.string.health_history_sleep,
                        page.selected.optJSONArray("sleepSessions")?.length() ?: 0),
                    hcSupported = access[0], hcGranted = access[1], samsungInstalled = access[2], samsungAvailable = access[3])
                // The first visible open can occur while preferences are still loading.
                if (screenVisible && _uiState.value.activeTab == "journal") {
                    screenActivity?.get()?.let { launchRefresh(it, NativeHealthAutoRefreshGate.Trigger.AUTO) }
                }
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
        invalidateRefresh()
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
        if (newProvider !in setOf(HealthJournalStore.HC, HealthJournalStore.SAMSUNG)) return
        invalidateRefresh()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        healthStore.setPreferences(JSONObject().put("provider", newProvider))
                    }
                }
                _uiState.update { it.copy(provider = newProvider) }
                // Manual Samsung selection must retain its explicit permission dialog;
                // do not let the implicit open/read win the single-operation gate.
                if (newProvider == HealthJournalStore.SAMSUNG) refresh(activity, requestSamsungPermission = true)
                loadHealthDashboard()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                val text = com.repforge.app.ui.text.nativeResources(context, _uiState.value.language)
                _uiState.update { it.copy(statusMessage = text.getString(com.repforge.app.R.string.coach_refresh_failed)) }
                loadHealthDashboard()
            }
        }
    }

    fun setAutoOnOpen(enable: Boolean) {
        if (_uiState.value.autoOnOpenSaving) return
        invalidateRefresh()
        _uiState.update { it.copy(autoOnOpenSaving = true, autoOnOpenSaveFailed = false) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        healthStore.setPreferences(JSONObject().put("autoOnOpen", enable))
                    }
                }
                _uiState.update { it.copy(autoOnOpen = enable) }
                if (enable) screenActivity?.get()?.let { launchRefresh(it, NativeHealthAutoRefreshGate.Trigger.AUTO) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(autoOnOpenSaveFailed = true) } }
            finally { _uiState.update { it.copy(autoOnOpenSaving = false) } }
        }
    }

    /** Manual refresh bypasses the auto timer, but shares the same single in-flight SDK gate. */
    fun refresh(activity: Activity?, requestSamsungPermission: Boolean = false) {
        if (activity != null) launchRefresh(activity, NativeHealthAutoRefreshGate.Trigger.MANUAL, requestSamsungPermission)
    }

    private fun launchRefresh(
        activity: Activity,
        trigger: NativeHealthAutoRefreshGate.Trigger,
        requestSamsungPermission: Boolean = false
    ) {
        if (!screenVisible || screenActivity?.get() !== activity ||
            _uiState.value.activeTab != "journal" || providerJob?.isActive == true) return
        providerJob = viewModelScope.launch {
            var ticket: NativeHealthAutoRefreshGate.Ticket? = null
            try {
                val visibleGeneration = screenGeneration
                val selected = historyQuery.day.toString()
                val prepared = withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        val prefs = healthStore.preferences()
                        val provider = prefs.optString("provider", HealthJournalStore.HC)
                        val permitted = when (provider) {
                            HealthJournalStore.HC -> Build.VERSION.SDK_INT >= 34 &&
                                HealthConnectRepository.isSupported(context) && HealthConnectRepository.anyReadAllowed(context)
                            HealthJournalStore.SAMSUNG ->
                                SamsungHealthRepository.isInstalled(context) && SamsungHealthRepository.isSdkAvailable()
                            else -> false
                        }
                        Triple(
                            NativeHealthAutoRefreshGate.Snapshot(provider, selected, healthStore.generation(), visibleGeneration),
                            prefs.optBoolean("autoOnOpen", false), permitted && NativeHealthForegroundAdmission.safe(context)
                        )
                    }
                }
                val snapshot = prepared.first
                if (!screenVisible || screenGeneration != visibleGeneration || historyQuery.day.toString() != selected) return@launch
                ticket = refreshGate.begin(trigger, SystemClock.elapsedRealtime(), prepared.second,
                    prepared.third, prepared.third, snapshot) ?: return@launch
                val activeTicket = ticket!!
                _uiState.update { it.copy(healthRefreshing = true) }
                val guard = {
                    check(screenVisible && screenActivity?.get() === activity && _uiState.value.activeTab == "journal" &&
                        historyQuery.day.toString() == selected && screenGeneration == visibleGeneration) {
                        "Health screen changed while refreshing"
                    }
                    check(NativeHealthForegroundAdmission.safe(context)) { "Workout or data replacement in progress" }
                    check(refreshGate.isCurrent(activeTicket, snapshot, true, true)) {
                        "Health refresh result is obsolete"
                    }
                    Unit
                }
                val day = if (trigger == NativeHealthAutoRefreshGate.Trigger.AUTO) LocalDate.now()
                    else historyQuery.day
                // Auto refresh always targets today; the user-selected historical day/filter is never reset.
                val result = NativeHealthRefresh.readDay(
                    context, activity, day, requestSamsungPermission && trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL,
                    requireScreenGuard = guard
                )
                if (screenVisible && screenGeneration == visibleGeneration && historyQuery.day.toString() == selected) {
                    if (trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL) {
                        val text = com.repforge.app.ui.text.nativeResources(context, _uiState.value.language)
                        _uiState.update { it.copy(statusMessage = text.getString(if (result.isSuccess)
                            com.repforge.app.R.string.health_read_completed else com.repforge.app.R.string.coach_refresh_failed)) }
                    }
                    if (result.isSuccess) loadHealthDashboard()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL && screenVisible) {
                    val text = com.repforge.app.ui.text.nativeResources(context, _uiState.value.language)
                    _uiState.update { it.copy(statusMessage = text.getString(com.repforge.app.R.string.coach_refresh_failed)) }
                }
            } finally {
                ticket?.let { refreshGate.finish(it) }
                _uiState.update { it.copy(healthRefreshing = false) }
            }
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
