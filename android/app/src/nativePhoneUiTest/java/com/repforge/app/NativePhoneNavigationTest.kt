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
            HealthJournalStore.get(context).erase()
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

    @Test fun weightJournalSupportsDatedAddEditAndConfirmedExactDeletion() {
        launch(); tab("health")
        compose.onNodeWithTag("health_weight_journal").performClick()
        compose.onNodeWithTag("weight_journal_action").performClick()
        compose.onNodeWithTag("weight_input").performTextReplacement("72,3")
        compose.onNodeWithTag("weight_date").performTextReplacement("2026-10-20")
        compose.onNodeWithTag("weight_save").performClick()
        fun weights() = runBlocking(Dispatchers.IO) { TrainPilotDatabase.getInstance(context).bodyWeightDao().getAllWeights() }
        compose.waitUntil(15000) { weights().size == 1 }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("weight_input").fetchSemanticsNodes().isEmpty() }
        val saved = weights().single()
        assertEquals("2026-10-20", saved.dayKey); assertEquals(72.3, saved.kg, .0001)
        compose.onNodeWithTag("weight_journal_list").performScrollToNode(hasTestTag("weight_edit_${saved.id}"))
        compose.onNodeWithTag("weight_edit_${saved.id}").performClick()
        compose.onNodeWithTag("weight_input").performTextReplacement("73,4")
        compose.onNodeWithTag("weight_save").performClick()
        compose.waitUntil(15000) { weights().single().kg == 73.4 }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("weight_input").fetchSemanticsNodes().isEmpty() }
        assertEquals(saved.id, weights().single().id); assertEquals(saved.date, weights().single().date)
        screenshot("weight_journal_edited")
        compose.onNodeWithTag("weight_delete_${saved.id}").performClick()
        compose.onNodeWithText("Mégse").performClick()
        assertEquals(1, weights().size)
        compose.onNodeWithTag("weight_delete_${saved.id}").performClick()
        compose.onNodeWithTag("weight_confirm_delete").performClick()
        compose.waitUntil(15000) { weights().isEmpty() }
        compose.onNodeWithText("Még nincs rögzített testsúly.").assertExists()
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("health_weight_journal").assertExists()
    }

    @Test fun failedWeightSaveKeepsTheDialogAndInputForExplicitRetry() {
        launch(); tab("health")
        compose.onNodeWithTag("health_weight_journal").performClick()
        compose.onNodeWithTag("weight_journal_action").performClick()
        compose.onNodeWithTag("weight_input").performTextReplacement("74,5")
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().putBoolean("blocked", true).commit()
        try {
            compose.onNodeWithTag("weight_save").performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithTag("weight_write_error").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("weight_input").assertTextContains("74,5")
            assertTrue(runBlocking(Dispatchers.IO) { TrainPilotDatabase.getInstance(context).bodyWeightDao().getAllWeights().isEmpty() })
        } finally {
            context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().putBoolean("blocked", false).commit()
        }
        compose.onNodeWithTag("weight_save").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("weight_input").fetchSemanticsNodes().isEmpty() }
        assertEquals(74.5, runBlocking(Dispatchers.IO) {
            TrainPilotDatabase.getInstance(context).bodyWeightDao().getAllWeights().single().kg
        }, .0001)
    }

    private fun healthRecord(id: String, type: String, source: String, channel: String, start: Long, end: Long, data: JSONObject) =
        JSONObject().put("recordId", id).put("type", type).put("source", source).put("channel", channel)
            .put("startMs", start).put("endMs", end).put("lastModifiedMs", end + 1)
            .put("day", HealthJournalStore.day(if (type == "SleepSessionRecord") end else start)).put("data", data)

    @Test fun individualHealthRecordsPageAndFilterWithoutChangingTheDailySummary() {
        val store = HealthJournalStore.get(context)
        val day = java.time.LocalDate.now()
        val time = day.atTime(8, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val rows = org.json.JSONArray((0 until 31).map { i -> healthRecord("weight-$i", "WeightRecord", "origin.alpha",
            HealthJournalStore.HC, time, time + 1, JSONObject().put("value", 72.3).put("unit", "kg")) })
        rows.put(healthRecord("beta-weight", "WeightRecord", "origin.beta", HealthJournalStore.HC, time - 1000, time,
            JSONObject().put("value", 73.4).put("unit", "kg")))
        store.importRecords(rows, store.generation())
        launch(); tab("health")
        compose.onNodeWithTag("health_journal_list").performScrollToNode(hasTestTag("health_open_records"))
        compose.onNodeWithTag("health_open_records").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("health_record_weight-30").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("health_records_next").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("2. oldal").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("health_records_list").performScrollToNode(hasTestTag("health_record_weight-0"))
        compose.onNodeWithTag("health_record_weight-30").assertDoesNotExist()
        compose.onNodeWithTag("health_records_previous").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("health_record_weight-30").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("health_origin_origin.beta").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("health_record_beta-weight").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Forrás: Health Connect · origin.beta").assertExists()
        compose.onNodeWithTag("health_records_next").assertIsNotEnabled()
        screenshot("health_raw_origin")
        compose.onNodeWithTag("health_records_reload").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("health_record_beta-weight").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("health_records_close").performClick()
        compose.onNodeWithTag("health_selected_day").assertTextContains(day.toString())
    }

    @Test fun sleepDetailsRenderHealthConnectAndSamsungStagesWithActualProvenance() {
        val day = java.time.LocalDate.now()
        val wake = day.atTime(7, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val store = HealthJournalStore.get(context)
        val hc = JSONObject().put("value", 45).put("unit", "min").put("durationKind", "asleep_stages")
            .put("stages", org.json.JSONArray().put(JSONObject().put("startMs", wake - 3_600_000L)
                .put("endMs", wake - 900_000L).put("type", 5).put("isAsleep", true)))
        val samsung = JSONObject().put("durationMinutes", 60).put("stages", org.json.JSONArray().put(JSONObject()
            .put("start", java.time.Instant.ofEpochMilli(wake).toString())
            .put("end", java.time.Instant.ofEpochMilli(wake + 3_600_000L).toString()).put("stage", "LIGHT").put("isAsleep", true)))
        store.importRecords(org.json.JSONArray()
            .put(healthRecord("hc-sleep", "SleepSessionRecord", "hc.origin", HealthJournalStore.HC, wake - 32_400_000L, wake, hc))
            .put(healthRecord("samsung-sleep", "SleepSessionRecord", "samsung.origin", HealthJournalStore.SAMSUNG, wake, wake + 3_600_000L, samsung)), store.generation())
        launch(); tab("health")
        compose.onNodeWithTag("health_journal_list").performScrollToNode(hasTestTag("health_open_sleep"))
        compose.onNodeWithTag("health_open_sleep").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("health_record_samsung-sleep").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("health_records_list").performScrollToNode(hasTestTag("health_record_expand_samsung-sleep"))
        compose.onNodeWithTag("health_record_expand_samsung-sleep").performClick()
        compose.onNodeWithTag("health_records_list").performScrollToNode(hasText("Könnyű alvás"))
        compose.onNodeWithText("Könnyű alvás").assertExists()
        screenshot("health_sleep_samsung")
        compose.onNodeWithTag("health_records_list").performScrollToNode(hasTestTag("health_record_expand_hc-sleep"))
        compose.onNodeWithTag("health_record_expand_hc-sleep").performClick()
        compose.onNodeWithTag("health_records_list").performScrollToNode(hasText("Mélyalvás"))
        compose.onNodeWithText("Mélyalvás").assertExists()
        screenshot("health_sleep_hc")
    }

    @Test fun replacingHealthDataDuringPagingRequiresExplicitReloadAndClearsOldRecords() {
        val store = HealthJournalStore.get(context)
        val day = java.time.LocalDate.now()
        val time = day.atTime(8, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        store.importRecords(org.json.JSONArray((0 until 26).map { i -> healthRecord("old-$i", "WeightRecord", "old.origin",
            HealthJournalStore.HC, time, time + 1, JSONObject().put("value", 72.3).put("unit", "kg")) }), store.generation())
        launch(); tab("health")
        compose.onNodeWithTag("health_journal_list").performScrollToNode(hasTestTag("health_open_records"))
        compose.onNodeWithTag("health_open_records").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("health_record_old-25").fetchSemanticsNodes().isNotEmpty() }
        runBlocking(Dispatchers.IO) { store.erase() }
        compose.onNodeWithTag("health_records_next").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("health_records_error").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("health_record_old-25").assertDoesNotExist()
        screenshot("health_raw_replaced")
        compose.onNodeWithTag("health_records_reload").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("health_records_list").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Nincs tárolt egyedi mérés ehhez a naphoz és szűréshez.").assertExists()
        compose.onNodeWithTag("health_records_error").assertDoesNotExist()
    }

    @Test fun activitySourceSelectionPersistsAndMarksPreviousDailyValuesPartial() {
        val store = HealthJournalStore.get(context)
        val day = java.time.LocalDate.now().toString()
        store.healthDay(day, JSONObject().put("weightKg", 72.3), store.generation())
        launch(); tab("health")
        compose.onNodeWithTag("health_journal_list").performScrollToNode(hasTestTag("health_daily_priority"))
        compose.onNodeWithTag("health_daily_priority").performClick()
        compose.waitUntil(15000) { store.preferences().optString("hcDailySource") == "priority" &&
            compose.onAllNodesWithTag("health_daily_partial").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("health_daily_priority").assertIsSelected()
        assertEquals(72.3, store.page(day, day, "journal", "", 0, 0, 1)
            .getJSONArray("days").getJSONObject(0).getDouble("weightKg"), .0001)
    }

    @Test fun realHealthConnectOriginIsSelectableAndOldAggregateNeedsRefresh() {
        val store = HealthJournalStore.get(context)
        val date = java.time.LocalDate.now()
        val day = date.toString()
        val at = date.atTime(8, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        store.importRecords(org.json.JSONArray().put(healthRecord("health-origin-steps", "StepsRecord",
            "origin.alpha", HealthJournalStore.HC, at, at + 1000, JSONObject().put("count", 123))),
            store.generation())
        store.healthDay(day, JSONObject().put("source", "@aggregate").put("steps", 123), store.generation())
        launch(); tab("health")
        compose.waitUntil(15000) { compose.onAllNodesWithTag("health_daily_origin.alpha").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("health_journal_list").performScrollToNode(hasTestTag("health_daily_origin.alpha"))
        compose.onNodeWithTag("health_daily_origin.alpha").performClick()
        compose.waitUntil(15000) { store.preferences().optString("hcDailySource") == "origin.alpha" &&
            compose.onAllNodesWithTag("health_daily_partial").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("health_daily_origin.alpha").assertIsSelected()
        assertEquals(123, store.page(day, day, "journal", "", 0, 0, 1)
            .getJSONArray("days").getJSONObject(0).getInt("steps"))
        screenshot("health_specific_origin")
    }

    @Test fun healthBackgroundShowsSchedulingSeparatelyFromSavedOptIn() {
        launch(); tab("health")
        compose.onNodeWithTag("health_journal_list").performScrollToNode(hasTestTag("health_hc_background_switch"))
        compose.onNodeWithTag("health_hc_background_switch").assertIsOff()
        compose.onNodeWithTag("health_hc_background_status").assertExists()
        compose.onNodeWithText("Nincs ütemezett háttérfrissítés.").assertExists()
        assertFalse(HealthJournalStore.get(context).preferences().getBoolean("hcBackground"))
    }

    @Test fun bleBackgroundCannotClaimToStartWithoutARememberedWatch() {
        assertTrue(BleWatchPreference(context).clear())
        launch(); tab("health")
        compose.onNodeWithText("BLE Okosóra (#111)").performClick()
        compose.onNodeWithTag("health_ble_list").performScrollToNode(hasTestTag("health_ble_background_switch"))
        compose.onNodeWithTag("health_ble_background_switch").assertIsOff()
        compose.onNodeWithTag("health_ble_background_switch").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("health_background_error").fetchSemanticsNodes().isNotEmpty() }
        assertFalse(HealthJournalStore.get(context).preferences().getBoolean("bleBackground"))
        assertFalse(HealthJournalStore.get(context).preferences().getBoolean("logWatchSteps"))
        compose.onNodeWithTag("health_ble_background_status").assertExists()
        compose.onNodeWithText("Nincs aktív BLE háttérkapcsolat.").assertExists()
    }

    @Test fun programEditorCreatesCustomPlanAndRenamesDayWithoutStartingWorkout() {
        launch(); tab("training")
        compose.onNodeWithTag("shortcut_programs").performScrollTo().performClick()
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("program_create"))
        compose.onNodeWithTag("program_create").performClick()
        compose.onNodeWithTag("program_editor_name").performTextInput("Saját időterv")
        compose.onNodeWithTag("program_editor_day_name").performTextClearance()
        compose.onNodeWithTag("program_editor_day_name").performTextInput("Kedd")
        compose.onNodeWithTag("program_editor_save").performClick()
        val db = TrainPilotDatabase.getInstance(context)
        compose.waitUntil(15000) { runBlocking(Dispatchers.IO) {
            db.programDao().getProgramsWithDays().any { it.program.nameHu == "Saját időterv" &&
                it.days.singleOrNull()?.nameHu == "Kedd" }
        } }
        val custom = runBlocking(Dispatchers.IO) { db.programDao().getProgramsWithDays().first {
            it.program.nameHu == "Saját időterv" } }
        assertFalse(custom.program.builtin)
        assertTrue(custom.program.id.startsWith("custom-program-"))
        assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
    }

    @Test fun programEditorAddsRealCatalogExerciseToNewDay() {
        launch(); tab("training")
        compose.onNodeWithTag("shortcut_programs").performScrollTo().performClick()
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("program_create"))
        compose.onNodeWithTag("program_create").performClick()
        compose.onNodeWithTag("program_editor_name").performTextInput("Gyakorlatos terv")
        compose.onNodeWithTag("program_editor_search").performScrollTo()
        val name = com.repforge.app.data.db.NativeBuiltinCatalog.exercises.first { it.id == "goblet-squat" }.nameHu
        compose.onNodeWithTag("program_editor_search").performTextInput(name)
        compose.onNodeWithTag("program_editor_choices")
            .performScrollToNode(hasTestTag("program_editor_add_exercise_goblet-squat"))
        compose.onNodeWithTag("program_editor_add_exercise_goblet-squat").performClick()
        compose.onNodeWithTag("program_editor_save").performClick()
        val db = TrainPilotDatabase.getInstance(context)
        compose.waitUntil(15000) { runBlocking(Dispatchers.IO) {
            db.programDao().getProgramsWithDays().any {
                it.program.nameHu == "Gyakorlatos terv" &&
                    org.json.JSONArray(it.days.singleOrNull()?.exercisesJson ?: "[]").optString(0) == "goblet-squat"
            }
        } }
        assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
    }

    @Test fun builtinProgramEditorCreatesCloneWithoutChangingTemplate() {
        val db = TrainPilotDatabase.getInstance(context)
        val before = runBlocking(Dispatchers.IO) {
            db.programDao().getProgramWithDaysById("home-basic")!! }
        launch(); tab("training")
        compose.onNodeWithTag("shortcut_programs").performScrollTo().performClick()
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("program_home-basic"))
        compose.onNodeWithTag("program_edit_home-basic").performClick()
        compose.onNodeWithTag("program_editor_name").performTextClearance()
        compose.onNodeWithTag("program_editor_name").performTextInput("Másolt terv")
        compose.onNodeWithTag("program_editor_save").performClick()
        compose.waitUntil(15000) { runBlocking(Dispatchers.IO) {
            db.programDao().getProgramsWithDays().any { it.program.nameHu == "Másolt terv" && !it.program.builtin }
        } }
        assertEquals(before, runBlocking(Dispatchers.IO) {
            db.programDao().getProgramWithDaysById("home-basic")!! })
        assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
    }

    @Test fun customExerciseCanBeCreatedEditedAndDeletedWithoutStartingWorkout() {
        launch(); tab("training")
        compose.onNodeWithText("Gyakorlatkönyvtár").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("custom_exercise_create").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("custom_exercise_create").performClick()
        compose.onNodeWithTag("custom_exercise_name").performTextInput("Saját UI próba")
        compose.onNodeWithTag("custom_exercise_save").performClick()
        val db = TrainPilotDatabase.getInstance(context)
        compose.waitUntil(15000) { runBlocking(Dispatchers.IO) {
            db.exerciseLibraryDao().getAllExercises().any { it.custom && it.nameHu == "Saját UI próba" }
        } }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("custom_exercise_edit").fetchSemanticsNodes().isNotEmpty() }
        screenshot("custom_exercise_created")
        compose.onNodeWithTag("custom_exercise_edit").performClick()
        compose.onNodeWithTag("custom_exercise_name").performTextClearance()
        compose.onNodeWithTag("custom_exercise_name").performTextInput("Saját UI javítva")
        compose.onNodeWithTag("custom_exercise_save").performClick()
        compose.waitUntil(15000) { runBlocking(Dispatchers.IO) {
            db.exerciseLibraryDao().getAllExercises().any { it.custom && it.nameHu == "Saját UI javítva" }
        } }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("custom_exercise_edit").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("custom_exercise_edit").performClick()
        compose.onNodeWithTag("custom_exercise_delete").performClick()
        compose.onNodeWithTag("custom_exercise_confirm_delete").performClick()
        compose.waitUntil(15000) { runBlocking(Dispatchers.IO) {
            db.exerciseLibraryDao().getAllExercises().none { it.custom && it.nameHu == "Saját UI javítva" }
        } }
        assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
    }

    @Test fun invalidCustomVideoCannotWriteOrReplaceCatalog() {
        launch(); tab("training")
        compose.onNodeWithText("Gyakorlatkönyvtár").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("custom_exercise_create").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("custom_exercise_create").performClick()
        compose.onNodeWithTag("custom_exercise_name").performTextInput("Rossz videó")
        compose.onNodeWithTag("custom_exercise_video").performTextInput("javascript:alert(1)")
        compose.onNodeWithTag("custom_exercise_save").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("custom_exercise_error").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(106, runBlocking(Dispatchers.IO) {
            TrainPilotDatabase.getInstance(context).exerciseLibraryDao().getAllExercises().size
        })
    }

    @Test fun fullNativeLibraryShows106EntriesAndOpensItsOriginalVideoCard() {
        launch(); tab("training")
        compose.onNodeWithText("Gyakorlatkönyvtár").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("exercise_library_count").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("exercise_library_count").assertTextContains("106")
        compose.onNodeWithTag("exercise_library_search").performTextInput(
            com.repforge.app.data.db.NativeBuiltinCatalog.exercises.first { it.id == "db-squat" }.nameHu)
        compose.onNodeWithTag("exercise_library_row_db-squat").performClick()
        compose.onNodeWithTag("exercise_library_detail").assertExists()
        compose.onNodeWithTag("exercise_library_video").assertExists()
        compose.onNodeWithText("Videó forrása: Mike Hildebrandt").assertExists()
        screenshot("library_original_video_details")
        assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
    }

    @Test fun outdoorExerciseOffersOnlyExplicitUnverifiedSearchNotAnInventedVideo() {
        launch(); tab("training")
        compose.onNodeWithText("Gyakorlatkönyvtár").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("exercise_library_search").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("exercise_library_search").performTextInput("Futás")
        compose.onNodeWithTag("exercise_library_row_running").performClick()
        compose.onNodeWithTag("exercise_library_unverified").assertExists()
        compose.onNodeWithTag("exercise_library_search_video").assertExists()
        compose.onNodeWithTag("exercise_library_video").assertDoesNotExist()
        screenshot("library_outdoor_search_not_video")
    }

    @Test fun quickWorkoutShowsOriginalVideoWithoutStartingWorkout() {
        launch(); tab("training")
        compose.onNodeWithText("Gyakorlat választása").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("exercise_search").fetchSemanticsNodes().isNotEmpty() }
        val squats = com.repforge.app.data.db.NativeBuiltinCatalog.exercises.first { it.id == "db-squat" }.nameHu
        compose.onNodeWithTag("exercise_search").performTextInput(squats)
        compose.onNodeWithTag("exercise_select_db-squat").performClick()
        compose.onNodeWithTag("quick_exercise_video").assertExists()
        assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
    }

    @Test fun exerciseFavoritesCanBeToggledFilteredAndRecoveredOnReentry() {
        launch(); tab("training")
        compose.onNodeWithText("Gyakorlat választása").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("exercise_search").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("exercise_search").performTextInput("Running")
        compose.waitUntil(15000) { compose.onAllNodesWithTag("exercise_favorite_running").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("exercise_favorite_running").performClick()
        val db = TrainPilotDatabase.getInstance(context)
        compose.waitUntil(15000) { runBlocking(Dispatchers.IO) {
            com.repforge.app.NativeExerciseFavorites.load(db).contains("running")
        } }
        compose.onNodeWithTag("exercise_favorites_filter").performClick()
        compose.onNodeWithTag("exercise_select_running").assertExists()
        screenshot("quick_favorite_filter")
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithText("Gyakorlat választása").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("exercise_search").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("exercise_search").performTextInput("Running")
        compose.onNodeWithTag("exercise_favorite_running").assertTextContains("★")
        assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
    }

    @Test fun earlierHealthDayCanBeSelectedAndDisplaysItsStoredWeight() {
        val day = java.time.YearMonth.now().minusMonths(1).atDay(1).toString()
        val health = HealthJournalStore.get(context)
        health.setPreferences(JSONObject().put("provider", HealthJournalStore.HC))
        health.healthDay(day, JSONObject().put("source", "@aggregate").put("weightKg", 72.3), health.generation())
        launch(); tab("health")
        compose.onNodeWithTag("health_previous_month").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText(day).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(day).performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Betöltés…").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("health_journal_list").performScrollToNode(hasTestTag("health_metric_weightKg"))
        compose.onNodeWithText("72,3").assertIsDisplayed()
        screenshot("health_historical_weight")
    }

}
