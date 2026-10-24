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
    val logWatchSteps: Boolean = false,
    val bleBackground: Boolean = false,
    val hcBackground: Boolean = false,
    val metrics: List<HealthMetricItemUiModel> = emptyList(),
    val sleepSessionsSummaryText: String = "—",
    val lastSyncAt: String = "—",
    val bleState: BleDiscoveryUiState = BleDiscoveryUiState(),
    val hcSupported: Boolean = false,
    val hcGranted: Boolean = false,
    val samsungInstalled: Boolean = false,
    val samsungAvailable: Boolean = false,
    val statusMessage: String? = null
)

class HealthDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val healthStore = HealthJournalStore.get(context)
    val bleRepo = BleDiscoveryRepository(context)

    private val _uiState = MutableStateFlow(HealthDashboardUiState())
    val uiState: StateFlow<HealthDashboardUiState> = _uiState.asStateFlow()

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
    }

    fun loadHealthDashboard() {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = healthStore.preferences()
            val today = LocalDate.now().toString()
            val page = healthStore.page(today, today, "journal", "", 0L, 0L, 1)
            val summaries = page.optJSONArray("days")
            val dayObj = if (summaries != null && summaries.length() > 0) summaries.getJSONObject(0) else JSONObject()

            val provider = prefs.optString("provider", HealthJournalStore.HC)
            val metricItems = parse26Metrics(dayObj)

            val sleepArr = dayObj.optJSONArray("sleepSessions")
            val sleepText = if (sleepArr != null && sleepArr.length() > 0) {
                val dur = sleepArr.getJSONObject(0).optDouble("durationMinutes", 0.0)
                "${(dur / 60.0 * 10.0).roundToInt() / 10.0} óra alvás (${sleepArr.length()} szakasz)"
            } else "Nincs rögzített alvásadat"

            _uiState.value = _uiState.value.copy(
                provider = provider,
                dailySourcePreference = prefs.optString("hcDailySource", "auto"),
                logWatchSteps = prefs.optBoolean("logWatchSteps", false),
                bleBackground = prefs.optBoolean("bleBackground", false),
                hcBackground = prefs.optBoolean("hcBackground", false),
                metrics = metricItems,
                sleepSessionsSummaryText = sleepText,
                lastSyncAt = page.optString("lastSyncAt").ifEmpty { "—" },
                hcSupported = HealthConnectRepository.isSupported(context),
                hcGranted = HealthConnectRepository.anyReadAllowed(context),
                samsungInstalled = SamsungHealthRepository.isInstalled(context),
                samsungAvailable = SamsungHealthRepository.isSdkAvailable()
            )
        }
    }

    private fun parse26Metrics(day: JSONObject): List<HealthMetricItemUiModel> {
        data class Def(val key: String, val label: String, val unit: String, val decimals: Int = 0)
        val defs = listOf(
            Def("steps", "Lépésszám", "lépés"),
            Def("activeCalories", "Aktív kalória", "kcal"),
            Def("totalCalories", "Összes elégetett kalória", "kcal"),
            Def("distanceMeters", "Napi távolság", "m"),
            Def("averageHeartRate", "Átlagpulzus", "bpm"),
            Def("restingHeartRate", "Nyugalmi pulzus", "bpm"),
            Def("minHeartRate", "Minimális pulzus", "bpm"),
            Def("maxHeartRate", "Maximális pulzus", "bpm"),
            Def("hrvRmssdMs", "Szívritmus variabilitás (HRV)", "ms"),
            Def("weightKg", "Testsúly", "kg", 1),
            Def("bodyFatPercent", "Testzsírszázalék", "%", 1),
            Def("oxygenSaturationPercent", "Véroxigénszint (SpO₂)", "%", 1),
            Def("vo2Max", "VO₂max állóképesség", "ml/kg/min", 1),
            Def("energyScore", "Energy Score / Vitalitás", "/ 100"),
            Def("bloodPressureSystolic", "Vérnyomás (szisztolés)", "mmHg"),
            Def("bloodPressureDiastolic", "Vérnyomás (diasztolés)", "mmHg"),
            Def("bloodGlucoseMmolL", "Vércukorszint", "mmol/L", 1),
            Def("respiratoryRate", "Légzésszám", "/perc", 1),
            Def("basalMetabolicRateKcal", "Alapanyagcsere (BMR)", "kcal"),
            Def("bodyMassIndex", "Testtömegindex (BMI)", "", 1)
        )

        val providers = day.optJSONObject("metricProviders")
        return defs.map { d ->
            val has = day.has(d.key) && !day.isNull(d.key)
            val num = day.optDouble(d.key, Double.NaN)
            val valStr = if (has && num.isFinite()) {
                if (d.decimals == 0) num.roundToInt().toString()
                else ((num * 10.0).roundToInt() / 10.0).toString()
            } else "—"
            val prov = providers?.optString(d.key) ?: day.optString("channel", "Health Connect")
            HealthMetricItemUiModel(
                key = d.key,
                label = d.label,
                valueText = valStr,
                unit = d.unit,
                sourceText = if (has) prov else "Nincs adat",
                isAvailable = has
            )
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
        viewModelScope.launch {
            val result = com.repforge.app.NativeHealthRefresh.readToday(context, activity, requestSamsungPermission)
            val language = com.repforge.app.data.store.TrainPilotSettingsStore(context).current().language
            val text = com.repforge.app.ui.text.nativeResources(context, language)
            _uiState.value = _uiState.value.copy(statusMessage = text.getString(if (result.isSuccess)
                com.repforge.app.R.string.health_read_completed else com.repforge.app.R.string.coach_refresh_failed))
            if (result.isSuccess) loadHealthDashboard()
        }
    }

    fun toggleBleBackground(enable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            healthStore.setPreferences(JSONObject().put("bleBackground", enable).put("logWatchSteps", enable))
            if (enable) HealthBackgroundService.resume(context)
            else HealthBackgroundService.stop(context)
            loadHealthDashboard()
        }
    }

    fun toggleHcBackground(enable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            healthStore.setPreferences(JSONObject().put("hcBackground", enable))
            try { HealthBackgroundJob.schedule(context) } catch (_: Exception) {}
            loadHealthDashboard()
        }
    }
}
