package com.repforge.app

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated emulator data only. The signed-upgrade jobs exercise actual legacy migration separately. */
@RunWith(AndroidJUnit4::class)
class NativePhoneNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    @get:Rule val testIdentity = org.junit.rules.TestName()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val device get() = UiDevice.getInstance(instrumentation)
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var settings: TrainPilotSettingsStore
    private var shot = 0

    @Before fun seedIsolatedEmulator() {
        assertTrue("This suite must use a debuggable isolated test build",
            context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
        context.stopService(Intent(context, IntervalTimerService::class.java))
        context.stopService(Intent(context, DistanceTrackingService::class.java))
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
            (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
            context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
            ActiveWorkoutStore.clear(context)
        }
        NativeReplacementJournal(context).clear()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        settings = TrainPilotSettingsStore(context)
        runBlocking(Dispatchers.IO) {
            val db = TrainPilotDatabase.getInstance(context)
            db.clearAllTables()
            TrainPilotDatabase.ensureBuiltinCatalogSeeded(db, context)
            settings.resetAllSettingsForEraseLocal()
            settings.markMigrationComplete()
        }
    }
    @After fun close() {
        scenario?.close()
        context.stopService(Intent(context, IntervalTimerService::class.java))
        context.stopService(Intent(context, DistanceTrackingService::class.java))
        instrumentation.waitForIdleSync()
    }
    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(30000) { compose.onAllNodesWithTag("navigation_home").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }
    private fun tab(route: String) { compose.onNodeWithTag("navigation_$route").performClick(); compose.waitForIdle() }
    private fun snapshot(): JSONObject? {
        var result: JSONObject? = null
        instrumentation.runOnMainSync { result = ActiveWorkoutStore.current(context) }
        return result
    }
    private fun screenshot(screen: String, language: String = "hu", theme: String = "classicBlue") {
        compose.waitForIdle()
        val name = "${javaClass.simpleName.lowercase()}_${testIdentity.methodName.hashCode().toUInt().toString(16)}_${screen}_${language}_${theme.lowercase()}_${++shot}"
        require(Regex("[a-z0-9_]{1,120}").matches(name))
        val request = JSONObject().put("name", name).put("language", language).put("theme", theme)
            .put("screen", screen).put("data", "isolated-emulator-test-fixture").toString()
        device.executeShellCommand("printf '%s' '$request' > /data/local/tmp/trainpilot-shot-request")
        val deadline = System.nanoTime() + 15_000_000_000L
        while (System.nanoTime() < deadline) {
            if (device.executeShellCommand("cat /data/local/tmp/trainpilot-shot-ack 2>/dev/null").trim() == name) return
            Thread.sleep(100)
        }
        fail("Host did not capture raw adb screenshot: $name")
    }
    @Test fun coldStartRecoveryRequiresExplicitResumeBeforeOpeningTheApp() {
        val token = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        val dir = java.io.File(context.noBackupFilesDir, "archive_transactions/$token").apply { mkdirs() }
        java.io.File(dir, "backup.json").writeText("already committed; must not be imported again")
        NativeReplacementJournal(context).write(NativeReplacementJournal.Entry("json", token, true, "a".repeat(64)))
        instrumentation.runOnMainSync {
            context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().putBoolean("blocked", true).commit()
            PhoneWorkoutExecution.store(context).setRestoreBlocked(true)
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(30000) { compose.onAllNodesWithTag("replacement_recovery").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("navigation_home").assertDoesNotExist()
        screenshot("recovery")
        compose.onNodeWithText("Művelet folytatása").performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithTag("navigation_home").fetchSemanticsNodes().isNotEmpty() }
        assertFalse(NativeReplacementJournal(context).exists())
        assertFalse(NativeDataReplacement.blocked(context))
        assertFalse(dir.exists())
        screenshot("recovered_home")
    }
    @Test fun fiveTabsAndSecondaryPlanningEntriesUseActualNativeScreens() {
        launch(); screenshot("home")
        for (route in listOf("training", "journal", "progress", "health")) { tab(route); screenshot(route) }
        tab("home")
        for (route in listOf("programs", "calendar", "coach")) {
            compose.onNodeWithTag("shortcut_$route").performClick()
            compose.onNodeWithTag("navigation_settings").assertExists()
            if (route == "coach") compose.waitUntil(15000) {
                compose.onAllNodesWithTag("coach_ready").fetchSemanticsNodes().isNotEmpty()
            }
            screenshot(route)
            compose.onNodeWithTag("navigation_back").performClick()
        }
    }
    @Test fun settingsThemeAndLanguageChangesReturnToTheirOrigin() {
        launch(); tab("journal")
        compose.onNodeWithTag("navigation_settings").performClick()
        for ((language, theme) in listOf("hu" to "classicBlue", "en" to "yellow", "de" to "classicRose", "ro" to "cyan")) {
            runBlocking { settings.setLanguage(language) }
            compose.onNodeWithTag("theme_$theme").performScrollTo().performClick()
            compose.waitUntil(10000) { runBlocking { settings.current().themeKey == theme && settings.current().language == language } }
            screenshot("settings", language, theme)
        }
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("journal_search").assertExists()
        compose.onNodeWithText("Caută antrenamente sau exerciții").assertExists()
    }
    @Test fun openingSettingsKeepsTheCanonicalWorkoutAndStopwatchRunning() {
        val quickLaunch = runBlocking { NativeWorkoutSession.startQuickWorkout(context, "plank", false) }
        val originalId = quickLaunch.snapshot.getString("workoutId")
        launch(); tab("training")
        compose.onNodeWithText("Indítás", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitUntil(10000) { snapshot()?.optJSONObject(NativeSetStopwatch.FIELD)?.optBoolean("running") == true }
        compose.onNodeWithTag("navigation_settings").performClick()
        val visiting = requireNotNull(snapshot())
        assertEquals(originalId, visiting.getString("workoutId"))
        assertFalse(visiting.optBoolean("workoutPaused"))
        assertTrue(visiting.getJSONObject(NativeSetStopwatch.FIELD).getBoolean("running"))
        screenshot("settings_during_workout")
        compose.onNodeWithTag("navigation_back").performClick()
        assertEquals(originalId, snapshot()!!.getString("workoutId"))
        assertFalse(snapshot()!!.optBoolean("workoutPaused"))
        screenshot("active_timed")
        tab("home")
        assertTrue(snapshot()!!.optBoolean("workoutPaused"))
    }
    @Test fun failedDiscardStaysInEditorAndExplicitRetryClosesTheSameWorkout() {
        val quick = runBlocking { NativeWorkoutSession.startQuickWorkout(context, "plank", false) }
        val originalId = quick.snapshot.getString("workoutId")
        val db = TrainPilotDatabase.getInstance(context)
        runBlocking(Dispatchers.IO) {
            db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER discard_full BEFORE INSERT ON history_tombstones
                BEGIN SELECT RAISE(ABORT, 'isolated emulator discard fault'); END""")
        }
        try {
            launch(); tab("training")
            compose.onNodeWithTag("workout_discard").performClick()
            compose.onNodeWithTag("workout_confirm_discard").performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithTag("workout_status_dismiss").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(originalId, snapshot()!!.getString("workoutId"))
            assertTrue(snapshot()!!.getBoolean("discardPending"))
            assertTrue(snapshot()!!.getBoolean("active"))
            screenshot("discard_storage_failure")
            compose.onNodeWithTag("workout_status_dismiss").performClick()
            compose.onNodeWithTag("workout_discard").assertExists()
        } finally {
            runBlocking(Dispatchers.IO) { db.openHelper.writableDatabase.execSQL("DROP TRIGGER IF EXISTS discard_full") }
        }
        compose.onNodeWithTag("workout_discard").performClick()
        compose.onNodeWithTag("workout_confirm_discard").performClick()
        compose.waitUntil(10000) { snapshot()?.optBoolean("active") == false }
        compose.onNodeWithTag("workout_discard").assertDoesNotExist()
        assertTrue(runBlocking { db.workoutHistoryDao().isTombstoned(originalId, JSONObject.quote(originalId)) })
        assertEquals(0, runBlocking { db.workoutHistoryDao().getWorkoutCount() })
        screenshot("discard_completed")
    }

    @Test fun intervalConfigurationIsWorkoutScopedUntilGlobalDefaultIsExplicitlyChosen() {
        runBlocking { NativeWorkoutSession.startQuickWorkout(context, "plank", false) }
        launch(); tab("training")
        compose.onNodeWithTag("interval_options").performScrollTo().performClick()
        compose.onNodeWithTag("interval_auto_advance").performScrollTo().performClick()
        compose.onNodeWithTag("interval_save").performScrollTo().performClick()
        compose.waitUntil(10000) { snapshot()?.optJSONObject("timerConfig")?.optBoolean("autoAdvance", true) == false }
        assertTrue(runBlocking { settings.current().intervalAutoAdvance })
        assertFalse(snapshot()!!.has("latestTimer162"))
        compose.onNodeWithTag("interval_global").performScrollTo().performClick()
        compose.onNodeWithTag("interval_save").performScrollTo().performClick()
        compose.waitUntil(10000) { runBlocking { !settings.current().intervalAutoAdvance } }
        screenshot("interval_single_set_global_preset")
        compose.onNodeWithTag("workout_discard").performClick()
        compose.onNodeWithTag("workout_confirm_discard").performClick()
        compose.waitUntil(10000) { snapshot()?.optBoolean("active") == false }
        val next = runBlocking { NativeWorkoutSession.startQuickWorkout(context, "plank", false) }
        assertFalse(next.snapshot.getJSONObject("timerConfig").getBoolean("autoAdvance"))
    }

    @Test fun blockedCoachRefreshKeepsStoredSignalsAndDisplaysAnHonestFailureState() {
        launch()
        compose.onNodeWithTag("shortcut_coach").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("coach_ready").fetchSemanticsNodes().isNotEmpty() }
        val health = HealthJournalStore.get(context)
        val generation = health.generation()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().putBoolean("blocked", true).commit()
        try {
            compose.onNodeWithTag("coach_refresh_health").performScrollTo().performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithTag("coach_refresh_failed").fetchSemanticsNodes().isNotEmpty() }
            compose.waitUntil(15000) { compose.onAllNodesWithTag("coach_ready").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(generation, health.generation())
            assertTrue(snapshot()?.optBoolean("active") != true)
            screenshot("coach_refresh_failure")
        } finally {
            context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test fun coachShowsExerciseAdviceWithoutEditingAnExistingNativeWorkoutOrProgram() {
        val quick = runBlocking { NativeWorkoutSession.startQuickWorkout(context, "plank", false) }
        val original = JSONObject(quick.snapshot.toString())
        val db = TrainPilotDatabase.getInstance(context)
        val programs = runBlocking { db.programDao().getProgramsWithDays().map { it.program.rawJson } }
        launch()
        compose.onNodeWithTag("shortcut_coach").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("coach_ready").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("coach_advice_0").performScrollTo().assertIsDisplayed()
        screenshot("coach_exercise_advice")
        val after = snapshot()!!
        assertEquals(original.getString("workoutId"), after.getString("workoutId"))
        assertEquals(original.getLong("generation"), after.getLong("generation"))
        assertEquals(original.getJSONArray("exercises").toString(), after.getJSONArray("exercises").toString())
        assertEquals(programs, runBlocking { db.programDao().getProgramsWithDays().map { it.program.rawJson } })
        assertEquals(0, runBlocking { db.workoutHistoryDao().getWorkoutCount() })
    }

    @Test fun fullBundledCatalogBootsAndTheLastBuiltinProgramIsReachableInEnglish() {
        runBlocking { settings.setLanguage("en") }
        val db = TrainPilotDatabase.getInstance(context)
        assertEquals(106, runBlocking { db.exerciseLibraryDao().getExerciseIds().size })
        assertEquals(13, runBlocking { db.programDao().getProgramIds().size })
        launch()
        compose.onNodeWithTag("shortcut_programs").performScrollTo().performClick()
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("program_gym-strength-abc"))
        compose.onNodeWithTag("program_gym-strength-abc").assertIsDisplayed()
        compose.onNodeWithText("Gym strength A/B/C").assertIsDisplayed()
        screenshot("programs_full_catalog", "en")
    }

}
