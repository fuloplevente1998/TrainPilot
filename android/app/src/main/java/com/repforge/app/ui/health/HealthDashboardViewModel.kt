package com.repforge.app.ui.health

import android.app.Application
import android.app.Activity
import android.os.Build
import android.os.SystemClock
import java.lang.ref.WeakReference
import com.repforge.app.NativeDataReplacement
import com.repforge.app.NativeHealthSync
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
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.ui.home.NativeHomeRecoveryPresentation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

data class HealthMetricItemUiModel(
    val key: String,
    val label: String,
    val valueText: String,
    val unit: String,
    val sourceText: String,
    val isAvailable: Boolean
)

data class HealthPulseTrendPointUiModel(
    val day: String,
    val bpm: Double?
)

data class HealthDaySummaryUiModel(
    val day: String,
    val dateText: String,
    val summaryText: String,
    val partial: Boolean
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
    val syncCompleted: Int = 0,
    val syncTotal: Int = 0,
    val syncPages: Int = 0,
    val syncStage: String = "",
    val syncFailed: Boolean = false,
    val syncPartial: Boolean = false,
    val syncCancelled: Boolean = false,
    val syncRetryFull: Boolean = false,
    val hcBackgroundBlock: BackgroundBlock = BackgroundBlock.UNSUPPORTED,
    val bleBackgroundBlock: BackgroundBlock = BackgroundBlock.WATCH_NOT_READY,
    val hcBackgroundScheduled: Boolean = false,
    val bleBackgroundRunning: Boolean = false,
    val backgroundSaving: Boolean = false,
    val backgroundWriteFailed: Boolean = false,
    val metrics: List<HealthMetricItemUiModel> = emptyList(),
    val overviewMetrics: List<HealthMetricItemUiModel> = emptyList(),
    val additionalMetrics: List<HealthMetricItemUiModel> = emptyList(),
    val pulseTrend: List<HealthPulseTrendPointUiModel> = emptyList(),
    val recentDaySummaries: List<HealthDaySummaryUiModel> = emptyList(),
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

private data class HealthStudioOverviewAux(
    val workoutCount: Int,
    val recovery: NativeHomeRecoveryPresentation.Model?,
    val selectedIsToday: Boolean,
    val pulseTrend: List<HealthPulseTrendPointUiModel>
)

class HealthDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = TrainPilotDatabase.getInstance(context)
    private val healthStore = HealthJournalStore.get(context)
    val bleRepo = BleDiscoveryRepository(context)

    private val _uiState = MutableStateFlow(HealthDashboardUiState())
    val uiState: StateFlow<HealthDashboardUiState> = _uiState.asStateFlow()

    private var historyQuery = NativeHealthQuery()
    private var historyJob: Job? = null
    private val refreshGate = NativeHealthAutoRefreshCoordinator.gate
    private var screenActivity: WeakReference<Activity>? = null
    private var screenVisible = false
    private var screenGeneration = 0L
    private var providerJob: Job? = null
    private var samsungPermissionPending = false
    private var manualSamsungPermissionVisible = false
    private var healthRecordDetailVisible = false

    fun cancelHealthSync() {
        providerJob?.cancel()
        _uiState.update { it.copy(syncCancelled = true) }
        loadHealthDashboard(allowAutoRefresh = false)
    }

    fun syncHealth(activity: Activity?, full: Boolean = false) {
        if (activity != null) {
            _uiState.update { it.copy(syncRetryFull = full) }
            launchRefresh(activity, NativeHealthAutoRefreshGate.Trigger.MANUAL,
                requestSamsungPermission = _uiState.value.provider == HealthJournalStore.SAMSUNG, fullSync = full)
        }
    }

    fun healthRecordDetailChanged(visible: Boolean) {
        if (healthRecordDetailVisible == visible) return
        healthRecordDetailVisible = visible
        invalidateRefresh()
        if (!visible && screenVisible) screenActivity?.get()?.let {
            launchRefresh(it, NativeHealthAutoRefreshGate.Trigger.AUTO)
        }
    }

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
        if (!_uiState.value.historyLoading && _uiState.value.activeTab == "journal" &&
            !healthRecordDetailVisible && !manualSamsungPermissionVisible) {
            launchRefresh(activity, NativeHealthAutoRefreshGate.Trigger.AUTO)
        }
    }

    fun healthScreenHidden(activity: Activity?, force: Boolean = false) {
        if (!screenVisible || (activity != null && screenActivity?.get() !== activity)) return
        // A Samsung permission dialog may STOP the activity. Preserve only this explicitly
        // requested consent; publication itself still requires a RESUMED activity.
        if (!force && manualSamsungPermissionVisible && providerJob?.isActive == true) return
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
    fun loadHealthDashboard(allowAutoRefresh: Boolean = true) {
        // init also calls this method; query initialization must precede init.
        val query = historyQuery
        historyJob?.cancel()
        _uiState.value = _uiState.value.copy(
            historyLoading = true,
            historyError = false,
            metrics = emptyList(),
            overviewMetrics = emptyList(),
            additionalMetrics = emptyList(),
            pulseTrend = emptyList(),
            recentDaySummaries = emptyList(),
            selectedDay = query.day.toString(), historyMonth = query.month.toString(), historyChannel = query.channel,
            availableDays = emptyList(), sleepSessionsSummaryText = "—", historyPartial = false)
        historyJob = viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    val access = listOf(HealthConnectRepository.isSupported(context), HealthConnectRepository.anyReadAllowed(context),
                        SamsungHealthRepository.isInstalled(context), SamsungHealthRepository.isSdkAvailable())
                    val background = NativeHealthBackgroundPolicy.snapshot(context, healthStore)
                    com.repforge.app.NativeDataReplacement.mutate(context) {
                        val language = com.repforge.app.data.store.TrainPilotSettingsStore(context).current().language
                        val prefs = healthStore.preferences()
                        val page = NativeHealthHistory(healthStore).load(query)
                        val zone = ZoneId.systemDefault()
                        val now = Instant.now()
                        val selected = query.day
                        val from = selected.atStartOfDay(zone).toInstant().toEpochMilli()
                        val to = selected.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                        val workoutCount = database.workoutHistoryDao().getWorkoutCountBetween(from, to)
                        val selectedIsToday = selected == now.atZone(zone).toLocalDate()
                        val recovery = if (selectedIsToday) {
                            try {
                                val recoveryPage = healthStore.page(
                                    selected.minusDays(14).toString(), selected.toString(),
                                    "journal", "", 0L, 0L, 1
                                )
                                val summaries = recoveryPage.optJSONArray("days")
                                val days = (0 until (summaries?.length() ?: 0))
                                    .mapNotNull { summaries?.optJSONObject(it) }
                                val history = database.workoutHistoryDao()
                                    .getCoachRecentHistory(
                                        now.minus(Duration.ofDays(14)).toEpochMilli(),
                                        now.toEpochMilli()
                                    )
                                    .map { JSONObject(it) }
                                NativeHomeRecoveryPresentation.select(days, history, now, zone)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                null
                            }
                        } else null
                        val pulseTrend = try {
                            val trendPage = healthStore.page(
                                selected.minusDays(6).toString(), selected.toString(),
                                "journal", "", 0L, 0L, 1
                            )
                            val summaries = trendPage.optJSONArray("days")
                            val byDay = mutableMapOf<String, Double>()
                            for (index in 0 until (summaries?.length() ?: 0)) {
                                val summary = summaries?.optJSONObject(index) ?: continue
                                val average = summary.optDouble("averageHeartRate", Double.NaN)
                                if (average.isFinite() && average > 0.0) {
                                    byDay[summary.optString("day")] = average
                                }
                            }
                            (6 downTo 0).map { offset ->
                                val date = selected.minusDays(offset.toLong()).toString()
                                HealthPulseTrendPointUiModel(date, byDay[date])
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            emptyList()
                        }
                        Triple(
                            Triple(language, prefs, page),
                            access,
                            Triple(
                                background,
                                healthStore.nativeSyncLastSuccess(),
                                HealthStudioOverviewAux(
                                    workoutCount, recovery, selectedIsToday, pulseTrend
                                )
                            )
                        )
                    }
                }
                val (language, prefs, page) = loaded.first
                val access = loaded.second
                val background = loaded.third.first
                val overviewAux = loaded.third.third
                val text = com.repforge.app.ui.text.nativeResources(context, language)
                val metrics = NativeHealthMetrics.parse(context, page.selected, language)
                val overviewMetrics = buildStudioOverviewMetrics(
                    language, page.selected, metrics, overviewAux
                )
                val additionalMetrics = buildStudioAdditionalMetrics(language, page.selected, metrics)
                val recentDaySummaries = buildHealthDaySummaries(language, page.summaries)
                _uiState.value = _uiState.value.copy(language = language, historyLoading = false, historyError = false,
                    availableDays = page.days, metrics = metrics, overviewMetrics = overviewMetrics,
                    additionalMetrics = additionalMetrics, pulseTrend = overviewAux.pulseTrend,
                    recentDaySummaries = recentDaySummaries,
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
                    lastSyncAt = loaded.third.second,
                    hcSupported = access[0], hcGranted = access[1], samsungInstalled = access[2], samsungAvailable = access[3])
                // The first visible open can occur while preferences are still loading.
                if (allowAutoRefresh && screenVisible && _uiState.value.activeTab == "journal" && !healthRecordDetailVisible) {
                    screenActivity?.get()?.let { launchRefresh(it, NativeHealthAutoRefreshGate.Trigger.AUTO) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.value = _uiState.value.copy(historyLoading = false, historyError = true,
                metrics = emptyList(), overviewMetrics = emptyList(), additionalMetrics = emptyList(),
                pulseTrend = emptyList(), recentDaySummaries = emptyList(),
                dailySourceOptions = emptyList())
                refreshBackgroundStatus()
            }
        }
    }

    private fun buildStudioOverviewMetrics(
        language: String,
        day: JSONObject,
        full: List<HealthMetricItemUiModel>,
        aux: HealthStudioOverviewAux
    ): List<HealthMetricItemUiModel> {
        val text = com.repforge.app.ui.text.nativeResources(context, language)
        val noData = text.getString(com.repforge.app.R.string.health_history_no_data)
        val providers = day.optJSONObject("metricProviders")
        val sleepMinutes = day.optDouble("sleepMinutes", Double.NaN)
        val sleepAvailable = sleepMinutes.isFinite() && sleepMinutes > 0.0
        val sleepValue = if (sleepAvailable) {
            val hours = sleepMinutes / 60.0
            NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply {
                minimumFractionDigits = 0
                maximumFractionDigits = 1
            }.format(hours) + " h"
        } else "—"
        val sleep = HealthMetricItemUiModel(
            key = "sleepMinutes",
            label = text.getString(com.repforge.app.R.string.native_home_sleep),
            valueText = sleepValue,
            unit = "",
            sourceText = if (sleepAvailable) {
                providers?.optString("sleepMinutes")?.takeIf { it.isNotBlank() }
                    ?: day.optString("channel").takeIf { it.isNotBlank() } ?: "Health"
            } else noData,
            isAvailable = sleepAvailable
        )
        val workoutLabel = if (aux.selectedIsToday) {
            text.getString(com.repforge.app.R.string.native_home_today_workout)
        } else when (language) {
            "en" -> "Workouts"
            "de" -> "Trainings"
            "ro" -> "Antrenamente"
            else -> "Edzések"
        }
        val workouts = HealthMetricItemUiModel(
            key = "workouts",
            label = workoutLabel,
            valueText = aux.workoutCount.toString(),
            unit = "",
            sourceText = "TrainPilot",
            isAvailable = true
        )
        val recoveryAvailable = aux.recovery?.value != null
        val recovery = HealthMetricItemUiModel(
            key = "recovery",
            label = text.getString(com.repforge.app.R.string.native_home_recovery),
            valueText = aux.recovery?.displayValue ?: "—",
            unit = "",
            sourceText = when (aux.recovery?.source) {
                NativeHomeRecoveryPresentation.Source.SAMSUNG -> "Samsung Health"
                NativeHomeRecoveryPresentation.Source.TRAINPILOT_ESTIMATE -> "TrainPilot"
                else -> noData
            },
            isAvailable = recoveryAvailable
        )
        return NativeHealthMetrics.studioOverview(full, sleep, workouts, recovery)
    }

    private fun buildHealthDaySummaries(
        language: String,
        summaries: List<JSONObject>
    ): List<HealthDaySummaryUiModel> {
        val text = com.repforge.app.ui.text.nativeResources(context, language)
        val locale = Locale.forLanguageTag(language)
        val number0 = NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = 0
        }
        val number1 = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 1
        }
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
        fun value(day: JSONObject, key: String): Double? =
            day.optDouble(key, Double.NaN).takeIf { it.isFinite() }
        return summaries
            .sortedByDescending { it.optString("day") }
            .take(3)
            .mapNotNull { day ->
                val date = runCatching { LocalDate.parse(day.optString("day")) }.getOrNull()
                    ?: return@mapNotNull null
                val sleep = value(day, "sleepMinutes")?.takeIf { it > 0.0 }?.let {
                    number1.format(it / 60.0) + " h"
                } ?: "—"
                val pulse = value(day, "averageHeartRate")?.takeIf { it > 0.0 }?.let {
                    number0.format(it) + " bpm"
                } ?: "—"
                val steps = value(day, "steps")?.takeIf { it >= 0.0 }?.let {
                    number0.format(it)
                } ?: "—"
                val calories = value(day, "activeCalories")?.takeIf { it >= 0.0 }?.let {
                    number0.format(it) + " kcal"
                } ?: "—"
                HealthDaySummaryUiModel(
                    day = date.toString(),
                    dateText = date.format(dateFormatter),
                    summaryText = buildString {
                        append(text.getString(com.repforge.app.R.string.native_home_sleep))
                        append(": "); append(sleep)
                        append(" • ")
                        append(text.getString(com.repforge.app.R.string.native_home_pulse))
                        append(": "); append(pulse)
                        append(" • ")
                        append(text.getString(com.repforge.app.R.string.native_home_steps))
                        append(": "); append(steps)
                        append(" • "); append(calories)
                    },
                    partial = day.optBoolean("partial", false)
                )
            }
    }

    private fun buildStudioAdditionalMetrics(
        language: String,
        day: JSONObject,
        full: List<HealthMetricItemUiModel>
    ): List<HealthMetricItemUiModel> {
        val text = com.repforge.app.ui.text.nativeResources(context, language)
        val noData = text.getString(com.repforge.app.R.string.health_history_no_data)
        val byKey = full.associateBy { it.key }
        val systolic = byKey["bloodPressureSystolic"]
        val diastolic = byKey["bloodPressureDiastolic"]
        val pressureAvailable = systolic?.isAvailable == true && diastolic?.isAvailable == true
        val pressure = HealthMetricItemUiModel(
            key = "bloodPressure",
            label = text.getString(com.repforge.app.R.string.health_record_blood_pressure),
            valueText = if (pressureAvailable) "${systolic!!.valueText}/${diastolic!!.valueText}" else "—",
            unit = "mmHg",
            sourceText = if (pressureAvailable) {
                if (systolic!!.sourceText == diastolic!!.sourceText) systolic.sourceText
                else "${systolic.sourceText} / ${diastolic.sourceText}"
            } else noData,
            isAvailable = pressureAvailable
        )

        val rawDistance = day.optDouble("distanceMeters", Double.NaN)
        val distanceBase = byKey["distanceMeters"]
        val distanceAvailable = distanceBase?.isAvailable == true &&
            rawDistance.isFinite() && rawDistance >= 0.0
        val distance = distanceBase?.copy(
            valueText = if (distanceAvailable) {
                NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply {
                    minimumFractionDigits = 0
                    maximumFractionDigits = 2
                }.format(rawDistance / 1000.0)
            } else "—",
            unit = "km",
            isAvailable = distanceAvailable,
            sourceText = if (distanceAvailable) distanceBase.sourceText else noData
        )

        return listOfNotNull(
            pressure,
            byKey["bloodGlucoseMmolL"],
            byKey["respiratoryRate"],
            distance,
            byKey["oxygenSaturationPercent"]
        )
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
        val previousRead = providerJob
        invalidateRefresh()
        if (newProvider == HealthJournalStore.SAMSUNG) samsungPermissionPending = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        healthStore.setPreferences(JSONObject().put("provider", newProvider))
                    }
                }
                _uiState.update { it.copy(provider = newProvider) }
                loadHealthDashboard()
                // Explicit provider selection owns the permission prompt even when an old
                // automatic operation was cancelled while waiting for the SDK.
                if (newProvider == HealthJournalStore.SAMSUNG) {
                    previousRead?.join()
                    samsungPermissionPending = false
                    refresh(activity, requestSamsungPermission = true)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                val text = com.repforge.app.ui.text.nativeResources(context, _uiState.value.language)
                _uiState.update { it.copy(statusMessage = text.getString(com.repforge.app.R.string.coach_refresh_failed)) }
                loadHealthDashboard()
            } finally {
                samsungPermissionPending = false
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
        requestSamsungPermission: Boolean = false,
        fullSync: Boolean? = null
    ) {
        if (!screenVisible || screenActivity?.get() !== activity ||
            _uiState.value.activeTab != "journal" || healthRecordDetailVisible ||
            (trigger == NativeHealthAutoRefreshGate.Trigger.AUTO && samsungPermissionPending)) return
        val previous = providerJob
        if (previous != null && !previous.isCompleted) {
            // A canceled blocking SDK read can still be unwinding. Queue the new foreground
            // event behind its completion; never overlap two reads or silently lose re-entry.
            viewModelScope.launch {
                previous.join()
                if (screenVisible && screenActivity?.get() === activity)
                    launchRefresh(activity, trigger, requestSamsungPermission, fullSync)
            }
            return
        }
        providerJob = viewModelScope.launch {
            var ticket: NativeHealthAutoRefreshGate.Ticket? = null
            try {
                val visibleGeneration = screenGeneration
                val selected = historyQuery.day.toString()
                // PhoneWorkoutExecution.store is MAIN-ONLY; never call it inside IO/mutate.
                val executionSafe = NativeHealthForegroundAdmission.safeOnMain(context)
                val prepared = withContext(Dispatchers.IO) {
                    val hcPermitted = Build.VERSION.SDK_INT >= 34 && HealthConnectRepository.isSupported(context) &&
                        HealthConnectRepository.anyReadAllowed(context)
                    val samsungPermitted = SamsungHealthRepository.isInstalled(context) && SamsungHealthRepository.isSdkAvailable()
                    NativeDataReplacement.mutate(context) {
                        val prefs = healthStore.preferences()
                        val provider = prefs.optString("provider", HealthJournalStore.HC)
                        val permitted = when (provider) {
                            HealthJournalStore.HC -> hcPermitted
                            HealthJournalStore.SAMSUNG -> samsungPermitted
                            else -> false
                        }
                        Triple(
                            NativeHealthAutoRefreshGate.Snapshot(provider,
                                if (trigger == NativeHealthAutoRefreshGate.Trigger.AUTO) LocalDate.now().toString()
                                else selected,
                                healthStore.generation(), visibleGeneration),
                            prefs.optBoolean("autoOnOpen", false),
                            permitted && NativeHealthForegroundAdmission.safeOffMain(context)
                        )
                    }
                }
                val snapshot = prepared.first
                if (!screenVisible || screenGeneration != visibleGeneration || historyQuery.day.toString() != selected) return@launch
                ticket = refreshGate.begin(trigger, SystemClock.elapsedRealtime(), prepared.second,
                    prepared.third, executionSafe && prepared.third, snapshot)
                if (ticket == null) {
                    if (trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL) _uiState.update { it.copy(syncFailed = true) }
                    return@launch
                }
                val activeTicket = ticket!!
                manualSamsungPermissionVisible = trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL &&
                    requestSamsungPermission && snapshot.provider == HealthJournalStore.SAMSUNG
                val day = if (trigger == NativeHealthAutoRefreshGate.Trigger.AUTO) LocalDate.now()
                    else historyQuery.day
                _uiState.update { it.copy(healthRefreshing = true, syncFailed = false, syncPartial = false,
                    syncCancelled = false, syncCompleted = 0, syncTotal = 0, syncPages = 0, syncStage = "PREPARING") }
                val guard = {
                    check(screenVisible && screenActivity?.get() === activity && _uiState.value.activeTab == "journal" &&
                        !healthRecordDetailVisible && historyQuery.day.toString() == selected &&
                        screenGeneration == visibleGeneration) {
                        "Health screen changed while refreshing"
                    }
                    // Synchronous guard runs on provider IO threads; main-only execution is
                    // checked by NativeHealthRefresh *before* each protected publication.
                    check(NativeHealthForegroundAdmission.safeOffMain(context)) {
                        "Workout ownership, restore or backup blocks Health refresh"
                    }
                    if (trigger == NativeHealthAutoRefreshGate.Trigger.AUTO)
                        check(day == LocalDate.now()) { "Local health day changed while refreshing" }
                    check(refreshGate.isCurrent(activeTicket, snapshot, true, true)) {
                        "Health refresh result is obsolete"
                    }
                    Unit
                }
                // The history query is never replaced by sync output. Manual historical reads remain callable.
                val result = if (trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL && fullSync == null && day < LocalDate.now()) {
                    NativeHealthRefresh.readDay(context, activity, day, requestSamsungPermission, guard)
                } else NativeHealthRefresh.sync(context, activity, fullSync == true,
                    requestSamsungPermission && trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL,
                    progress = { value ->
                        _uiState.update { it.copy(syncStage = value.stage.name, syncCompleted = value.completed,
                            syncTotal = value.total, syncPages = value.pages) }
                    }, onPermissionFinished = { manualSamsungPermissionVisible = false }, requireScreenGuard = guard)
                val partial = result.getOrNull()?.optBoolean("partial") == true
                _uiState.update { it.copy(syncFailed = result.isFailure, syncPartial = partial) }
                if (screenVisible && screenGeneration == visibleGeneration && historyQuery.day.toString() == selected) {
                    if (trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL) {
                        val text = com.repforge.app.ui.text.nativeResources(context, _uiState.value.language)
                        _uiState.update { it.copy(statusMessage = text.getString(if (result.isSuccess && !partial)
                            com.repforge.app.R.string.health_read_completed else com.repforge.app.R.string.coach_refresh_failed)) }
                    }
                    loadHealthDashboard(allowAutoRefresh = false) // Durable partial days remain visible; history selection/source/filter are retained.
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                _uiState.update { it.copy(syncFailed = true) }
                if (trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL && screenVisible) {
                    val text = com.repforge.app.ui.text.nativeResources(context, _uiState.value.language)
                    _uiState.update { it.copy(statusMessage = text.getString(com.repforge.app.R.string.coach_refresh_failed)) }
                }
            } finally {
                manualSamsungPermissionVisible = false
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
