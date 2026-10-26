package com.repforge.app

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.Assert.assertTrue

/**
 * Runs against the real MainActivity and production Room/DataStore implementation, NOT a test app.
 * Deletes data only in a disposable debuggable emulator. Never use with a real user's app data.
 */
internal class NativeLocalizationEmulatorFixture {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context: Context get() = instrumentation.targetContext
    val settings: TrainPilotSettingsStore by lazy { TrainPilotSettingsStore(context) }
    val database: TrainPilotDatabase get() = TrainPilotDatabase.getInstance(context)

    fun reset() {
        val emulator = Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true)
        assertTrue("Requires a disposable emulator; data reset is destructive", emulator)
        assertTrue("Requires an isolated debuggable test build",
            context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        stopServices()
        instrumentation.runOnMainSync {
            val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
            (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
            field.set(null, null)
            context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
            ActiveWorkoutStore.clear(context)
        }
        NativeReplacementJournal(context).clear()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().clear().commit()
        runBlocking(Dispatchers.IO) {
            val db = database
            db.clearAllTables()
            TrainPilotDatabase.ensureBuiltinCatalogSeeded(db, context)
            settings.resetAllSettingsForEraseLocal()
            settings.importPortableSettings(org.json.JSONObject().put("settings", org.json.JSONObject().put("onboarding128", "complete")))
            settings.markMigrationComplete()
            HealthJournalStore.get(context).erase()
        }
    }

    fun launch(): ActivityScenario<MainActivity> =
        ActivityScenario.launch(MainActivity::class.java)

    fun currentWorkout(): JSONObject? {
        var current: JSONObject? = null
        instrumentation.runOnMainSync {
            current = ActiveWorkoutStore.current(context)?.let { JSONObject(it.toString()) }
        }
        return current
    }

    fun selectedLanguage(): String = runBlocking(Dispatchers.IO) { settings.current().language }

    /** Exercise the actual Settings UI/VM. Read DataStore only to await persisted completion. */
    fun selectLanguageThroughUi(compose: ComposeTestRule, language: String) {
        require(language in listOf("hu", "en", "de", "ro"))
        val tag = "settings_language_$language"
        compose.onNodeWithTag("settings_list").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(15_000) { selectedLanguage() == language }
        compose.onNodeWithTag(tag).assertIsSelected()
    }

    fun startQuick(exerciseId: String): JSONObject =
        runBlocking { NativeWorkoutSession.startQuickWorkout(context, exerciseId, false).snapshot }

    fun profileJson(): JSONObject =
        JSONObject(runBlocking(Dispatchers.IO) { settings.current().profileJson })

    fun stopServices() {
        context.stopService(Intent(context, IntervalTimerService::class.java))
        context.stopService(Intent(context, DistanceTrackingService::class.java))
        instrumentation.waitForIdleSync()
    }

    fun close(scenario: ActivityScenario<MainActivity>?) {
        scenario?.close()
        stopServices()
    }
}
