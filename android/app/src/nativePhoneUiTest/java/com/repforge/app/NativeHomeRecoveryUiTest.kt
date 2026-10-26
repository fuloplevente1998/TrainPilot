package com.repforge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

/** Real onboarded MainActivity + real HealthJournalStore; synthetic data, NOT a Samsung SDK run. */
@RunWith(AndroidJUnit4::class)
class NativeHomeRecoveryUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrument = InstrumentationRegistry.getInstrumentation()
    private val fixture = NativeLocalizationEmulatorFixture()
    private val context get() = fixture.context
    private val journal get() = HealthJournalStore.get(context)
    private val settings get() = TrainPilotSettingsStore(context)
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun reset() { fixture.reset() }
    @After fun close() { fixture.close(scenario) }

    private fun today(): String = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().toString()
    private fun time(): String = Instant.now().minusSeconds(3600).toString()
    private fun launch() {
        scenario = fixture.launch()
        compose.waitUntil(25_000) {
            compose.onAllNodesWithTag("home_metric_recovery_value").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("home_metric_recovery").performScrollTo()
    }
    private fun shownValue(text: String) {
        compose.waitUntil(25_000) {
            compose.onAllNodesWithTag("home_metric_recovery_value")
                .fetchSemanticsNodes().any { it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)
                    ?.any { value -> value.text == text } == true }
        }
        compose.onNodeWithTag("home_metric_recovery_value").assertTextEquals(text)
    }
    private fun sourceContains(value: String) {
        compose.waitUntil(25_000) {
            compose.onAllNodesWithTag("home_metric_recovery_source")
                .fetchSemanticsNodes().any { node -> node.config.getOrNull(
                    androidx.compose.ui.semantics.SemanticsProperties.Text)?.any {
                        it.text.contains(value)
                    } == true }
        }
    }
    private fun screenshot(name: String) {
        // Shell's unmodified PNG from the *running Home* screen, not a canvas mockup.
        val file = "/sdcard/Download/native-home-${name}.png"
        val descriptor = instrument.uiAutomation.executeShellCommand("screencap -p $file")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        val verify = instrument.uiAutomation.executeShellCommand("ls -l $file")
        val out = android.os.ParcelFileDescriptor.AutoCloseInputStream(verify).bufferedReader().use { it.readText() }
        assertTrue("Screenshot must exist: $file; $out", out.contains(file))
    }
    private fun setLanguage(language: String) = runBlocking(Dispatchers.IO) {
        settings.importPortableSettings(JSONObject().put("settings", JSONObject().put("language", language)
            .put("onboarding128", "complete")))
    }
    private fun samsung(score: Double = 87.0) = runBlocking(Dispatchers.IO) {
        journal.setPreferences(JSONObject().put("provider", HealthJournalStore.SAMSUNG))
        journal.samsungDay(today(),
            JSONObject().put("energyScore", score).put("energyScoreTime", time())
                .put("energyScoreSource", "samsung_health"), JSONArray(), journal.generation())
        val projected = journal.page(today(), today(), "journal", "", 0L, 0L, 1)
            .getJSONArray("days").getJSONObject(0)
        assertEquals(score, projected.getDouble("energyScore"), 0.0)
    }
    private fun hrvEstimate() = runBlocking(Dispatchers.IO) {
        journal.setPreferences(JSONObject().put("provider", HealthJournalStore.HC))
        val day = java.time.LocalDate.parse(today())
        for ((offset, value) in listOf(0L to 65.0, 1L to 50.0, 2L to 45.0, 3L to 55.0)) {
            journal.healthDay(day.minusDays(offset).toString(), JSONObject()
                .put("hrvRmssdMs", value)
                .put("hrvTime", if (offset == 0L) time() else day.minusDays(offset).toString() + "T07:00:00Z")
                .put("hrvSource", "health_connect"), journal.generation())
        }
    }

    @Test fun verifiedSamsungOnRealHomeAndRawScreenshot() {
        samsung()
        launch()
        shownValue("87%")
        sourceContains("Samsung")
        screenshot("samsung")
    }

    @Test fun realHomeDisplaysTrainPilotFallbackNotFabricatedSamsung() {
        hrvEstimate()
        launch()
        sourceContains("TrainPilot")
        screenshot("fallback")
    }

    @Test fun emptyJournalDisplaysDashAndNoData() {
        launch()
        shownValue("—")
        sourceContains("Nincs adat")
        screenshot("none")
    }

    @Test fun localeChangesPreserveEnergyIdentityAndRenderHuEnDeRoLabels() {
        samsung(64.0)
        for ((language, label) in listOf("hu" to "Samsung", "en" to "Samsung",
            "de" to "Samsung", "ro" to "Samsung")) {
            scenario?.close(); scenario = null
            setLanguage(language)
            launch()
            shownValue("64%")
            sourceContains(label)
            screenshot("locale-$language")
        }
        // A translated fallback/no-data key is asserted from actual Home below,
        // not just by testing resources in isolation.
        scenario?.close(); scenario = null
        runBlocking(Dispatchers.IO) { journal.erase() }
        for ((language, label) in listOf("hu" to "Nincs adat", "en" to "No data",
            "de" to "Keine Daten", "ro" to "Fără date")) {
            scenario?.close(); scenario = null
            setLanguage(language)
            launch()
            shownValue("—")
            sourceContains(label)
        }
    }
}
