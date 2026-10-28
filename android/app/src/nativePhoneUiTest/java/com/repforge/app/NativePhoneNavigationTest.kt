package com.repforge.app

import android.content.Context
import android.content.Intent
import androidx.room.withTransaction
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.getOrNull
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
            settings.importPortableSettings(org.json.JSONObject().put("settings", org.json.JSONObject().put("onboarding128", "complete")))
            settings.markMigrationComplete()
            HealthJournalStore.get(context).erase()
        }
    }
    @After fun close() {
        try { closeNativeComposeScenario(scenario, compose) } finally {
            context.stopService(Intent(context, IntervalTimerService::class.java))
            context.stopService(Intent(context, DistanceTrackingService::class.java))
            instrumentation.waitForIdleSync()
        }
    }
    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(30000) { compose.onAllNodesWithTag("navigation_home").fetchSemanticsNodes().isNotEmpty() }
        // The global header is composed before the destination finishes loading.
        // Wait for the actual Home body, not only navigation chrome.
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag("shortcut_programs").fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }
    private fun tab(route: String) {
        val destination = compose.onNodeWithTag("navigation_$route")
        // The main header scrolls; the active-workout secondary header is fixed.
        // Already visible navigation needs no scrollable ancestor.
        if (runCatching { destination.assertIsDisplayed() }.isFailure) destination.performScrollTo()
        destination.assertIsDisplayed().performClick()
        // A click is not an admission receipt: await actual selected destination
        // before starting the next screen's work/screenshot.
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("navigation_$route").fetchSemanticsNodes().any {
                it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Selected) == true
            }
        }
        compose.waitForIdle()
    }
    private fun expandInlineWeight() {
        compose.onNodeWithTag("health_weight_journal").performScrollTo()
            .assertIsDisplayed().performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("health_weight_inline_panel").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openFullWeightJournal() {
        expandInlineWeight()
        compose.onNodeWithTag("health_journal_list")
            .performScrollToNode(hasTestTag("weight_journal_action"))
        compose.onNodeWithTag("weight_journal_action").performScrollTo()
            .assertIsDisplayed().performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("weight_journal_list").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openTrainingQuickChoice(choiceTag: String) {
        compose.onNodeWithTag("training_quick_workout").performScrollTo()
            .assertIsDisplayed().performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("training_quick_sheet").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("training_quick_sheet")
            .performScrollToNode(hasTestTag(choiceTag))
        compose.onNodeWithTag(choiceTag).assertIsDisplayed().performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("exercise_search").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openTrainingPrograms() {
        compose.onNodeWithTag("training_list")
            .performScrollToNode(hasTestTag("shortcut_programs"))
        compose.onNodeWithTag("shortcut_programs").assertIsDisplayed().performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("program_list").fetchSemanticsNodes().isNotEmpty()
        }
    }

    // LazyColumn can hide descendants of an expanded, taller-than-viewport item
    // on compact devices; walk the actual viewport rather than assuming the
    // target has already been materialized in its semantics tree.
    private fun revealHealthListTag(tag: String) {
        val list = compose.onNodeWithTag("health_journal_list")
        for (attempt in 0 until 24) {
            if (compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
                return
            }
            list.performTouchInput { swipeUp() }
        }
        throw AssertionError("Health control $tag remained unreachable after scrolling")
    }

    private fun expandHealthSection(sectionTag: String, targetTag: String) {
        compose.onNodeWithTag("health_journal_list").performScrollToNode(hasTestTag(sectionTag))
        if (sectionTag == "health_more_section") {
            // The five compact readings are a different group from the original
            // 26 native metrics. Expand both levels explicitly; target absence
            // alone cannot tell whether a lazy section is open.
            compose.onNodeWithTag(sectionTag).performClick()
            revealHealthListTag("health_all_metrics_toggle")
            compose.onNodeWithTag("health_all_metrics_toggle")
                .assertTextContains("Összes mérés").performClick()
            revealHealthListTag(targetTag)
        } else {
            if (compose.onAllNodesWithTag(targetTag).fetchSemanticsNodes().isEmpty()) {
                compose.onNodeWithTag(sectionTag).performClick()
            }
            compose.onNodeWithTag("health_journal_list").performScrollToNode(hasTestTag(targetTag))
            compose.onNodeWithTag(targetTag).performScrollTo().assertIsDisplayed()
        }
    }

    private fun openSettingsProfileEditor() {
        compose.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_profile_summary"))
        if (compose.onAllNodesWithTag("training_profile_open").fetchSemanticsNodes().isEmpty()) {
            compose.onNodeWithTag("settings_profile_summary").performClick()
        }
        compose.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("training_profile_open"))
        compose.onNodeWithTag("training_profile_open").performClick()
    }

    private fun snapshot(): JSONObject? {
        var result: JSONObject? = null
        instrumentation.runOnMainSync { result = ActiveWorkoutStore.current(context) }
        return result
    }
    private fun screenshot(screen: String, language: String = "hu", theme: String = "classicBlue",
        data: String = "isolated-emulator-test-fixture") {
        compose.waitForIdle()
        val name = "${javaClass.simpleName.lowercase()}_${testIdentity.methodName.hashCode().toUInt().toString(16)}_${screen}_${language}_${theme.lowercase()}_${++shot}"
        require(Regex("[a-z0-9_]{1,120}").matches(name))
        val request = JSONObject().put("name", name).put("language", language).put("theme", theme)
            .put("screen", screen).put("data", data).toString()
        // UiAutomation executes a command, not shell redirection. Stream the request
        // through stdin so the host actually receives a file before waiting for its ack.
        val pipes = instrumentation.uiAutomation.executeShellCommandRw("tee /data/local/tmp/trainpilot-shot-request")
        android.os.ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use {
            it.write(request.toByteArray(Charsets.UTF_8))
        }
        android.os.ParcelFileDescriptor.AutoCloseInputStream(pipes[0]).use { it.readBytes() }
        val deadline = System.nanoTime() + 15_000_000_000L
        while (System.nanoTime() < deadline) {
            if (device.executeShellCommand("cat /data/local/tmp/trainpilot-shot-ack").trim() == name) return
            Thread.sleep(100)
        }
        fail("Host did not capture raw adb screenshot: $name")
    }
    @Test fun originalGoldDumbbellLogoSurvivesStudioShellPort() {
        launch()
        // The displayed branding is the real trainpilot_launcher_foreground vector,
        // not the 18:49 Studio prototype's mountain placeholder.
        compose.onNodeWithTag("trainpilot_brand_dumbbell", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithTag("navigation_home").assertIsSelected()
        screenshot("studio_original_gold_dumbbell", theme = "graphite_gold")
        tab("progress")
        compose.onNodeWithTag("trainpilot_brand_dumbbell", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithTag("navigation_progress").assertIsSelected()
    }

    @Test fun studioJournalMonthSelectorFiltersRealPagedHistoryAndReturnsToAllMonths() {
        launch()
        tab("journal")
        compose.onNodeWithTag("journal_search").assertExists()
        compose.onNodeWithTag("journal_month").performClick()
        compose.onNodeWithTag("journal_picker_all").assertExists()
        val current = java.time.YearMonth.now().toString()
        compose.onNodeWithTag("journal_picker_$current").performClick()
        compose.onNodeWithTag("journal_month").assertExists()
        screenshot("studio_journal_month_selected", theme = "graphite_gold")
        compose.onNodeWithTag("journal_month").performClick()
        compose.onNodeWithTag("journal_picker_all").performClick()
        compose.onNodeWithTag("journal_month").assertExists()
        screenshot("studio_journal_all_months", theme = "graphite_gold")
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
    /** Real native host, original main assets, persistent palette and clearly synthetic local data. */
    @Test fun p1YellowPalettePagesKeepStoredDataAndAnatomyZoomOnlyClosesItsDialog() {
        val db = TrainPilotDatabase.getInstance(context)
        runBlocking(Dispatchers.IO) {
            settings.setTheme(TrainPilotSettingsStore.AVAILABLE_THEMES.first { it.key == "yellow" })
            val importer = com.repforge.app.data.migration.LegacyWebViewMigrator(context, db, settings)
            for (i in 1..3) {
                val end = java.time.Instant.now().minusSeconds(i * 86400L)
                val exercise = JSONObject().put("id", "goblet-squat").put("loadType", "dumbbell").put("repUnit", "ism.")
                    .put("sets", org.json.JSONArray().put(JSONObject().put("done", true).put("weight", 12 + i).put("reps", "10")))
                importer.saveWorkoutJsonToRoom(JSONObject().put("id", "p1-visual-$i")
                    .put("programId", "home-basic").put("programName", "P1 vizuális teszt")
                    .put("dayId", "A").put("dayName", "A").put("started", end.minusSeconds(2700).toString())
                    .put("finished", end.toString()).put("exercises", org.json.JSONArray().put(exercise)))
            }
            val health = HealthJournalStore.get(context)
            health.setPreferences(JSONObject().put("provider", HealthJournalStore.HC))
            health.healthDay(java.time.LocalDate.now().toString(), JSONObject().put("source", "@aggregate")
                .put("steps", 6821).put("averageHeartRate", 65).put("weightKg", 72.3), health.generation())
        }
        fun capture(screen: String) = screenshot(screen, "hu", "yellow", "synthetic_disposable_p1_visual_fixture")
        launch()
        scenario?.onActivity { activity ->
            val bars = androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            assertFalse("Dark native surfaces need light status-bar icons", bars.isAppearanceLightStatusBars)
            assertFalse("Dark native surfaces need light navigation-bar icons", bars.isAppearanceLightNavigationBars)
        }
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("home_loading").fetchSemanticsNodes().isEmpty() }
        capture("p1_home")
        compose.onNodeWithTag("home_metric_recovery").performScrollTo()
        capture("p1_home_metrics")
        tab("training"); capture("p1_training")
        tab("journal")
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("journal_workout_p1-visual-1").fetchSemanticsNodes().isNotEmpty() }
        capture("p1_journal")
        tab("progress")
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("anatomical_muscle_heatmap_card"))
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("progress_anatomy_loaded", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        capture("p1_progress")
        compose.onNodeWithTag("progress_anatomy").performScrollTo().performClick()
        compose.onNodeWithTag("progress_anatomy_dialog").assertExists()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("progress_anatomy_loaded", useUnmergedTree = true).fetchSemanticsNodes().size == 2 }
        capture("p1_anatomy_zoom")
        compose.onNodeWithTag("progress_anatomy_close").performClick()
        compose.onNodeWithTag("progress_anatomy_dialog").assertDoesNotExist()
        compose.onNodeWithTag("navigation_progress").assertIsSelected()
        tab("health")
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText(com.repforge.app.ui.text.nativeResources(context, "hu")
                .getString(R.string.health_history_loading)).fetchSemanticsNodes().isEmpty()
        }
        capture("p1_health")
        for (tag in listOf("health_more_section", "health_history_section",
            "health_connect_section", "health_open_watch_sync_card")) {
            compose.onNodeWithTag("health_journal_list").performScrollToNode(hasTestTag(tag))
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        expandHealthSection("health_more_section", "health_metric_steps")
        compose.onNodeWithTag("health_metric_steps").assertIsDisplayed()
        capture("p1_health_metrics")
        assertEquals("yellow", runBlocking(Dispatchers.IO) { settings.current().themeKey })
        assertEquals(3, runBlocking(Dispatchers.IO) { (1..3).count { db.workoutHistoryDao().getWorkoutWithDetailsById("p1-visual-$it") != null } })
        assertFalse(snapshot()?.optBoolean("active") == true)
    }

    @Test fun fiveTabsAndSecondaryPlanningEntriesUseActualNativeScreens() {
        launch(); screenshot("home")
        for (route in listOf("training", "journal", "progress", "health")) { tab(route); screenshot(route) }
        compose.onNodeWithTag("header_home").performClick()
        compose.onNodeWithTag("navigation_home").assertIsSelected()
        for (route in listOf("programs", "calendar", "coach")) {
            try {
                compose.waitUntil(10_000) { compose.onAllNodesWithTag("shortcut_$route").fetchSemanticsNodes().isNotEmpty() }
            } catch (failure: Throwable) {
                throw AssertionError("Home planning shortcut missing for $route; UI=" +
                    compose.onAllNodes(isRoot(), useUnmergedTree = true).printToString().take(8000), failure)
            }
            compose.onNodeWithTag("shortcut_$route").performScrollTo().performClick()
            compose.onNodeWithTag("navigation_settings").assertExists()
            if (route == "coach") compose.waitUntil(15000) {
                compose.onAllNodesWithTag("coach_ready").fetchSemanticsNodes().isNotEmpty()
            }
            screenshot(route)
            compose.onNodeWithTag("navigation_back").performClick()
        }
    }
    @Test fun progressSelectorCanReachExercisesBeyondOldEightItemLimit() {
        launch(); tab("progress")
        compose.onNodeWithTag("progress_section_tab_2").performClick()
        val target = com.repforge.app.data.db.NativeBuiltinCatalog.exercises.last()
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_exercise_choices"))
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("progress_exercise_choices").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("progress_exercise_choices")
            .performScrollToNode(hasTestTag("progress_exercise_${target.id}"))
        compose.onNodeWithTag("progress_exercise_${target.id}").performClick()
        compose.onNodeWithTag("progress_error").assertDoesNotExist()
        screenshot("progress_last_builtin_exercise")
    }

    @Test fun trainingProfilePersistsAfterSettingsReentryWithoutGuessingMeasurements() {
        launch()
        compose.onNodeWithTag("navigation_settings").performClick()
        openSettingsProfileEditor()
        compose.onNodeWithTag("profile_field_age").performTextReplacement("28")
        compose.onNodeWithTag("profile_field_minutes").performTextReplacement("75")
        compose.onNodeWithTag("profile_field_goal").performTextReplacement("fitness")
        compose.onNodeWithTag("profile_save").performClick()
        compose.waitUntil(15000) { runBlocking(Dispatchers.IO) {
            JSONObject(settings.current().profileJson).optInt("age") == 28
        } }
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("profile_save").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("navigation_settings").performClick()
        openSettingsProfileEditor()
        compose.onNodeWithTag("profile_field_age").assertTextContains("28")
        compose.onNodeWithTag("profile_field_minutes").assertTextContains("75")
        assertFalse(JSONObject(runBlocking(Dispatchers.IO) { settings.current().profileJson }).has("height"))
        screenshot("training_profile_edited")
        val strings = com.repforge.app.ui.text.nativeResources(context, "hu")
        compose.onNodeWithText(strings.getString(R.string.native_profile_18)).performClick()
        compose.onNodeWithTag("navigation_back").performClick()
        val profileBefore = runBlocking(Dispatchers.IO) { settings.current().profileJson }
        val programsBefore = runBlocking(Dispatchers.IO) {
            TrainPilotDatabase.getInstance(context).programDao().getProgramsWithDays()
                .associate { it.program.id to it.program.rawJson }
        }
        compose.onNodeWithTag("home_personal_plan").performScrollTo().performClick()
        compose.onNodeWithTag("personal_preview").assertExists()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("personal_preview_loading").fetchSemanticsNodes().isEmpty() }
        screenshot("home_personal_plan_preview")
        compose.onNodeWithText(strings.getString(R.string.native_preview_10)).performClick()
        compose.onNodeWithTag("personal_preview").assertDoesNotExist()
        assertEquals(profileBefore, runBlocking(Dispatchers.IO) { settings.current().profileJson })
        assertEquals(programsBefore, runBlocking(Dispatchers.IO) {
            TrainPilotDatabase.getInstance(context).programDao().getProgramsWithDays()
                .associate { it.program.id to it.program.rawJson }
        })
        assertTrue(snapshot()?.optBoolean("active") != true)
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("home_studio_greeting").assertExists()
        compose.onNodeWithTag("navigation_home").assertIsSelected()
    }

    @Test fun invalidTrainingProfileDoesNotOverwriteStoredProfile() {
        runBlocking(Dispatchers.IO) {
            settings.saveTrainingProfile(JSONObject().put("age", 27)
                .put("gear", org.json.JSONArray().put("dumbbell")))
        }
        launch()
        compose.onNodeWithTag("navigation_settings").performClick()
        openSettingsProfileEditor()
        compose.onNodeWithTag("profile_field_weight").performTextReplacement("Infinity")
        compose.onNodeWithTag("profile_save").performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("profile_write_error").fetchSemanticsNodes().isNotEmpty()
        }
        val stored = JSONObject(runBlocking(Dispatchers.IO) { settings.current().profileJson })
        assertEquals(27, stored.getInt("age"))
        assertEquals("dumbbell", stored.getJSONArray("gear").getString(0))
        assertFalse(stored.has("weight"))
    }

    @Test fun settingsThemeAndLanguageChangesReturnToTheirOrigin() {
        launch(); tab("journal")
        compose.onNodeWithTag("navigation_settings").performClick()
        for ((language, theme) in listOf("hu" to "classicBlue", "en" to "yellow", "de" to "classicRose", "ro" to "cyan")) {
            compose.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_language_$language"))
            compose.onNodeWithTag("settings_language_$language").performClick()
            compose.waitUntil(10000) { runBlocking { settings.current().language == language } }
            compose.onNodeWithTag("settings_language_$language").assertIsSelected()
            if (compose.onAllNodesWithTag("theme_$theme").fetchSemanticsNodes().isEmpty()) {
                compose.onNodeWithTag("settings_list")
                    .performScrollToNode(hasTestTag("settings_theme_summary"))
                compose.onNodeWithTag("settings_theme_summary").performClick()
            }
            compose.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("theme_$theme"))
            compose.onNodeWithTag("theme_$theme").performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(10000) { runBlocking { settings.current().themeKey == theme && settings.current().language == language } }
            screenshot("settings", language, theme)
        }
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("journal_search").assertExists()
        compose.onNodeWithText("Caută antrenamente sau exerciții").assertExists()
        compose.onNodeWithTag("header_theme").performClick()
        compose.onNodeWithTag("settings_theme_picker").assertIsDisplayed()
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("journal_search").assertExists()
        // The approved 18:49 header has an *inline* language dropdown,
        // not a second Settings navigation. Persist through native DataStore.
        compose.onNodeWithTag("header_language").performClick()
        compose.onNodeWithTag("header_language_ro").assertIsDisplayed()
        compose.onNodeWithTag("header_language_en").performClick()
        compose.waitUntil(10000) { runBlocking { settings.current().language == "en" } }
        compose.onNodeWithTag("journal_search").assertExists()
        compose.onNodeWithTag("header_language").performClick()
        compose.onNodeWithTag("header_language_ro").performClick()
        compose.waitUntil(10000) { runBlocking { settings.current().language == "ro" } }
        compose.onNodeWithTag("journal_search").assertExists()
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
        try {
            compose.waitUntil(10_000) { snapshot()?.optBoolean("workoutPaused") == true }
        } catch (failure: Throwable) {
            throw AssertionError("Leaving native editor did not persist workout pause: " +
                snapshot()?.toString()?.take(2500), failure)
        }
        assertTrue(snapshot()!!.optBoolean("workoutPaused"))
        compose.onNodeWithTag("home_start_workout").performScrollTo()
            .assertIsDisplayed().assertTextContains("Folytatás")
        screenshot("home_active_session")
        compose.onNodeWithTag("home_start_workout").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("workout_discard").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(originalId, snapshot()!!.getString("workoutId"))

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
        val listBounds = compose.onNodeWithTag("active_workout_exercises").fetchSemanticsNode().boundsInRoot
        val dockBounds = compose.onNodeWithTag("active_workout_controls").fetchSemanticsNode().boundsInRoot
        assertTrue("Workout controls must reserve space, not intercept list touches",
            listBounds.bottom <= dockBounds.top + 1f)
        compose.onNodeWithTag("active_workout_exercises")
            .performScrollToNode(hasTestTag("interval_options"))
        compose.onNodeWithTag("interval_options").performScrollTo().performClick()
        compose.onNodeWithTag("active_workout_exercises")
            .performScrollToNode(hasTestTag("interval_auto_advance"))
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
            compose.onNodeWithTag("coach_ready").performScrollToNode(hasTestTag("coach_refresh_health"))
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

    @Test fun journalSeparatesWearAndHcMetricsKeepsDataOnRefreshFailureAndHidesAnOldWindow() {
        val db = TrainPilotDatabase.getInstance(context)
        val end = java.time.Instant.now().minusSeconds(60)
        val start = end.minusSeconds(600)
        val mapper = com.repforge.app.data.migration.LegacyWebViewMigrator(context, db, settings)
        runBlocking(Dispatchers.IO) {
            mapper.saveWorkoutJsonToRoom(JSONObject().put("id", "journal-health-ui")
                .put("started", start.toString()).put("finished", end.toString()).put("dayName", "Forrásonkénti egészségadat")
                .put("wear", JSONObject().put("totalCalories", 500).put("averageHeartRate", 117.2))
                .put("health240", JSONObject().put("windowStart", start.toString()).put("windowEnd", end.toString())
                    .put("syncedAt", end.toString()).put("activeCalories", 243).put("steps", 0)
                    .put("sources", org.json.JSONArray().put("com.sec.android.app.shealth")))
                .put("exercises", org.json.JSONArray()))
        }
        fun stored() = runBlocking(Dispatchers.IO) { db.workoutHistoryDao().getWorkoutWithDetailsById("journal-health-ui")!! }
        val before = stored()
        launch(); tab("journal")
        compose.waitUntil(15000) { compose.onAllNodesWithTag("journal_workout_journal-health-ui").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("journal_workout_journal-health-ui").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("journal_tab_2").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("journal_tab_2").performClick()
        compose.onNodeWithTag("journal_wear_totalCalories_value").performScrollTo().assertTextEquals("500 kcal")
        compose.onNodeWithTag("journal_hc_activeCalories_value").performScrollTo().assertTextEquals("243 kcal")
        compose.onNodeWithTag("journal_hc_steps_value").performScrollTo().assertTextEquals("0")
        val prefs = context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("blocked", true).commit()
        try {
            compose.onNodeWithTag("journal_health_refresh").performScrollTo().performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithTag("journal_health_error").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(before, stored())
            screenshot("journal_health_separate_sources_failure")
        } finally { prefs.edit().clear().commit() }
        runBlocking(Dispatchers.IO) {
            mapper.saveWorkoutJsonToRoom(JSONObject(before.workout.rawJson).put("finished", end.plusSeconds(60).toString()))
        }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("journal_health_status").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithTag("journal_hc_activeCalories_value").assertCountEquals(0)
        compose.onNodeWithTag("journal_wear_totalCalories_value").performScrollTo().assertTextEquals("500 kcal")
        assertEquals(1, runBlocking(Dispatchers.IO) { db.workoutHistoryDao().getWorkoutCount() })
    }

    @Test fun homeTracksPlanSettingsProgramAndHistoryChangesAndStartsTheDisplayedSchedule() {
        val db = TrainPilotDatabase.getInstance(context)
        val start = java.time.Instant.now().plusSeconds(60)
        val zone = java.time.ZoneId.systemDefault()
        val planned = runBlocking(Dispatchers.IO) {
            val source = db.programDao().getProgramWithDaysById("home-basic")!!
            db.programDao().saveProgramWithDays(source.program.copy(id = "home-scheduled-program", nameHu = "Naptári célprogram", builtin = false),
                listOf(source.days.first().copy(compositeId = "home-scheduled-program:scheduled-day", programId = "home-scheduled-program",
                    dayId = "scheduled-day", nameHu = "Naptári célnap", exercisesJson = "[\"goblet-squat\"]")))
            com.repforge.app.data.db.CalendarScheduleEntity("home-target-plan", "", start.atZone(zone).toLocalDate().toString(),
                start.toString(), start.plusSeconds(1800).toString(), "home-scheduled-program", "Naptári célprogram",
                "scheduled-day", "Naptári célnap", "planned", 1L, "{}")
                .also { db.calendarScheduleDao().upsertEntry(it) }
        }
        launch()
        fun awaitHomeProgram(name: String) {
            compose.waitUntil(15000) { compose.onAllNodes(hasTestTag("home_program_name") and hasText(name), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag("home_loading").fetchSemanticsNodes().isEmpty() }
        }
        awaitHomeProgram("Naptári célprogram")
        compose.onNodeWithTag("home_day_name", useUnmergedTree = true).assertTextEquals("Naptári célnap")
        val activeName = runBlocking(Dispatchers.IO) { db.programDao().getProgramWithDaysById("home-basic")!!.program.nameHu }
        runBlocking(Dispatchers.IO) { db.calendarScheduleDao().upsertEntry(planned.copy(status = "skipped")) }
        awaitHomeProgram(activeName)
        runBlocking { settings.setActiveProgramId("home-scheduled-program") }
        awaitHomeProgram("Naptári célprogram")
        runBlocking { settings.setActiveProgramId("home-basic") }
        awaitHomeProgram(activeName)
        runBlocking(Dispatchers.IO) {
            db.calendarScheduleDao().upsertEntry(planned.copy(id = "home-target-plan-second"))
            val source = db.programDao().getProgramWithDaysById("home-scheduled-program")!!
            db.programDao().saveProgramWithDays(source.program.copy(nameHu = "Frissített célprogram"), source.days)
        }
        awaitHomeProgram("Frissített célprogram")
        compose.onNodeWithTag("home_metric_workouts_value", useUnmergedTree = true).performScrollTo().assertTextEquals("0")
        runBlocking(Dispatchers.IO) {
            val now = java.time.Instant.now()
            com.repforge.app.data.migration.LegacyWebViewMigrator(context, db, settings).saveWorkoutJsonToRoom(JSONObject()
                .put("id", "home-live-count").put("started", now.minusSeconds(1).toString()).put("finished", now.toString())
                .put("exercises", org.json.JSONArray()))
        }
        compose.waitUntil(15000) { compose.onAllNodes(hasTestTag("home_metric_workouts_value") and hasText("1"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() &&
            compose.onAllNodesWithTag("home_loading").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("home_start_workout").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(15000) { snapshot()?.optBoolean("active") == true }
        val launched = snapshot()!!
        assertEquals("home-scheduled-program", launched.getString("programId"))
        assertEquals("scheduled-day", launched.getString("dayId"))
        assertEquals("home-target-plan-second", launched.getString("scheduleId"))
        assertEquals("home-basic", runBlocking { settings.current().activeProgramId })
        assertEquals(1, runBlocking(Dispatchers.IO) { db.workoutHistoryDao().getWorkoutCount() })
        screenshot("home_scheduled_target_started")
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("workout_discard").fetchSemanticsNodes().isNotEmpty() }
        screenshot("active_strength") // Real programmed goblet-squat workout UI, emulator fixture.
    }

    @Test fun syntheticGpsRouteOpensOriginalJournalMapWithoutClaimingDeviceMeasurement() {
        // Synthetic route is written through the existing private route-store fixture.
        // The screen and modal are the actual MainActivity -> Journal -> Route UI.
        val db = TrainPilotDatabase.getInstance(context)
        val routeKey = "native-phone-ui-synthetic-route"
        val epoch = System.currentTimeMillis() - 90_000L
        val route = GpsRouteStore(context.noBackupFilesDir)
        route.append(routeKey, 1, GpsRouteTrace.Point(46.902, 18.078, 5.0, 1_000L, epoch))
        route.append(routeKey, 1, GpsRouteTrace.Point(46.903, 18.080, 5.0, 2_000L, epoch + 1_000L))
        route.append(routeKey, 1, GpsRouteTrace.Point(46.904, 18.082, 5.0, 3_000L, epoch + 2_000L))
        assertEquals(3, route.read(routeKey).pointCount)

        val end = java.time.Instant.now().minusSeconds(60)
        val exercise = runBlocking(Dispatchers.IO) {
            db.exerciseLibraryDao().getAllExercises().first {
                it.measurementType == "distance" || it.repUnit.trim().lowercase() in setOf("m", "km")
            }
        }
        runBlocking(Dispatchers.IO) {
            com.repforge.app.data.migration.LegacyWebViewMigrator(context, db, settings)
                .saveWorkoutJsonToRoom(JSONObject()
                    .put("id", "native-synthetic-route-ui")
                    .put("started", end.minusSeconds(600).toString())
                    .put("finished", end.toString())
                    .put("programId", "home-basic")
                    .put("dayName", "Szintetikus GPS-útvonal")
                    .put("exercises", org.json.JSONArray().put(JSONObject()
                        .put("id", exercise.id)
                        .put("hu", exercise.nameHu)
                        .put("loadType", exercise.loadType)
                        .put("repUnit", exercise.repUnit)
                        .put("sets", org.json.JSONArray().put(JSONObject()
                            .put("setId", "native-route-set")
                            .put("set", 1).put("reps", "600").put("distanceMeters", 400.0)
                            .put("done", true).put("gpsRouteKey163", routeKey))))))
        }
        launch(); tab("journal")
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("journal_workout_native-synthetic-route-ui")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("journal_workout_native-synthetic-route-ui").performClick()
        compose.onNodeWithTag("journal_details_list")
            .performScrollToNode(hasTestTag("journal_open_route"))
        compose.onNodeWithTag("journal_open_route").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("route_close").fetchSemanticsNodes().isNotEmpty() }
        screenshot("route_synthetic_gps", data = "synthetic-gps-emulator-fixture-not-a-device-measurement")
        compose.onNodeWithTag("route_close").performClick()
    }

    @Test fun journalEditorCancelInvalidSaveAndRealSavePreserveTheExistingWorkout() {
        val db = TrainPilotDatabase.getInstance(context)
        val end = java.time.Instant.now().minusSeconds(60)
        runBlocking(Dispatchers.IO) {
            com.repforge.app.data.migration.LegacyWebViewMigrator(context, db, settings).saveWorkoutJsonToRoom(
                JSONObject().put("id", "journal-edit-ui").put("syncId", "journal-edit-sync").put("key", "journal-edit-key")
                    .put("started", end.minusSeconds(60).toString()).put("finished", end.toString())
                    .put("dayName", "Szerkeszthető edzés").put("future", "preserve")
                    .put("exercises", org.json.JSONArray().put(JSONObject().put("id", "goblet-squat")
                        .put("hu", "Serleg guggolás").put("loadType", "total").put("repUnit", "ism.")
                        .put("sets", org.json.JSONArray().put(JSONObject().put("setId", "journal-edit-set")
                            .put("reps", "8").put("weight", 10).put("done", true).put("futureSet", true))))))
        }
        fun stored() = runBlocking(Dispatchers.IO) { db.workoutHistoryDao().getWorkoutWithDetailsById("journal-edit-ui")!! }
        val before = stored()
        launch(); tab("journal")
        compose.waitUntil(15000) { compose.onAllNodesWithTag("journal_workout_journal-edit-ui").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("journal_workout_journal-edit-ui").performClick()
        fun openEditor() {
            compose.onNodeWithTag("journal_details_list").performScrollToNode(hasTestTag("journal_edit"))
            compose.onNodeWithTag("journal_edit").performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithTag("journal_editor_save").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("journal_editor_list").performScrollToNode(hasTestTag("journal_editor_weight_0_0"))
        }
        openEditor()
        compose.onNodeWithTag("journal_editor_weight_0_0").performTextReplacement("12,5")
        assertEquals(before, stored())
        compose.onNodeWithTag("journal_editor_cancel").performClick()
        assertEquals(before, stored())
        openEditor()
        compose.onNodeWithTag("journal_editor_list").performScrollToNode(hasTestTag("journal_editor_reps_0_0"))
        compose.onNodeWithTag("journal_editor_reps_0_0").performTextReplacement("")
        compose.onNodeWithTag("journal_editor_save").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("journal_editor_error").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(before, stored())
        compose.onNodeWithTag("journal_editor_list").performScrollToNode(hasTestTag("journal_editor_weight_0_0"))
        compose.onNodeWithTag("journal_editor_weight_0_0").performTextReplacement("12,5")
        compose.onNodeWithTag("journal_editor_list").performScrollToNode(hasTestTag("journal_editor_reps_0_0"))
        compose.onNodeWithTag("journal_editor_reps_0_0").performTextReplacement("10")
        compose.onNodeWithTag("journal_editor_save").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("journal_editor_save").fetchSemanticsNodes().isEmpty() }
        val saved = stored()
        assertEquals("journal-edit-sync", saved.workout.syncId); assertEquals("journal-edit-key", saved.workout.workoutKey)
        assertEquals(125.0, saved.workout.totalVolumeKg, 0.0)
        assertEquals("journal-edit-set", saved.exercises.single().sets.single().setId)
        assertEquals("preserve", JSONObject(saved.workout.rawJson).getString("future"))
        assertTrue(JSONObject(saved.workout.rawJson).getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
            .getJSONObject(0).getBoolean("futureSet"))
        assertEquals(1, runBlocking(Dispatchers.IO) { db.workoutHistoryDao().getWorkoutCount() })
        assertTrue(snapshot()?.optBoolean("active") != true)
        screenshot("journal_edit_saved")
    }

    @Test fun coachShowsExerciseAdviceWithoutEditingAnExistingNativeWorkoutOrProgram() {
        val quick = runBlocking { NativeWorkoutSession.startQuickWorkout(context, "plank", false) }
        val original = JSONObject(quick.snapshot.toString())
        val db = TrainPilotDatabase.getInstance(context)
        val programs = runBlocking { db.programDao().getProgramsWithDays().map { it.program.rawJson } }
        launch()
        compose.onNodeWithTag("shortcut_coach").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("coach_ready").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("coach_ready").performScrollToNode(hasTestTag("coach_advice_0"))
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

    @Test fun journalShowsPrAfterLargeHistoryWatchFinishWithoutDuplicatingOnReplay() {
        val db = TrainPilotDatabase.getInstance(context)
        runBlocking(Dispatchers.IO) {
            db.withTransaction { repeat(1500) { i ->
                val started = java.time.Instant.now().minusSeconds(172800 + i.toLong()).toString()
                db.workoutHistoryDao().insertWorkout(com.repforge.app.data.db.WorkoutHistoryEntity(
                    id = "prior-$i", workoutKey = "prior-$i", syncId = "prior-$i", started = started, finished = started,
                    startedEpochMs = java.time.Instant.parse(started).toEpochMilli(), finishedEpochMs = java.time.Instant.parse(started).toEpochMilli(),
                    durationSeconds = 0, programId = "fixture", programName = "Fixture", dayId = "A", dayName = "A", scheduleId = "",
                    quickWorkout = false, originDevice = "phone", ownershipProtocol193 = 0, ownerEpoch193 = 0,
                    effortFeedback = null, feedbackSkipped = false, feedbackUpdatedAt = 0, totalVolumeKg = 0.0,
                    completedSetsCount = 0, totalSetsCount = 0, totalRepsCount = 0, hasPr226 = false,
                    prRecordsJson226 = "[]", wearMetricsJson = null, health240Json = null, timeLogs162Json = null,
                    activeIntervalsJson = null, editedAt = null, rawJson = "{}"))
            } }
            val end = java.time.Instant.now()
            val importer = com.repforge.app.data.migration.LegacyWebViewMigrator(context, db, settings)
            fun exercise(kg: Double) = JSONObject().put("id", "db-floor-press").put("hu", "Talajon nyomás")
                .put("repUnit", "ism.").put("loadType", "per_hand").put("sets", org.json.JSONArray().put(
                    JSONObject().put("setId", "press-set").put("done", true).put("weight", kg).put("reps", "8")))
            importer.saveWorkoutJsonToRoom(JSONObject().put("id", "previous-press").put("programId", "fixture").put("dayId", "A")
                .put("started", end.minusSeconds(86460).toString()).put("finished", end.minusSeconds(86400).toString())
                .put("exercises", org.json.JSONArray().put(exercise(10.0))))
            val finished = JSONObject().put("id", "watch-pr").put("workoutId", "watch-pr").put("timerOwner", "watch")
                .put("active", false).put("programId", "fixture").put("programName", "PR teszt")
                .put("dayId", "A").put("dayName", "A").put("started", end.minusSeconds(60).toString())
                .put("finished", end.toString()).put("exercises", org.json.JSONArray().put(exercise(12.5)))
            val finalizer = TrainPilotWorkoutFinalization(context, db)
            finalizer.finishWorkout(finished, fromWatch = true); finalizer.finishWorkout(finished, fromWatch = true)
            assertEquals(1502, db.workoutHistoryDao().getWorkoutCount())
            assertTrue(db.workoutHistoryDao().getWorkoutWithDetails("watch-pr")!!.workout.hasPr226)
        }
        launch(); tab("journal")
        compose.waitUntil(15000) { compose.onAllNodesWithTag("journal_workout_watch-pr").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("journal_pr_watch-pr", useUnmergedTree = true)
            .assertContentDescriptionEquals("Személyes csúcs")
        screenshot("journal_large_history_pr")
    }

    @Test fun weightJournalSupportsDatedAddEditAndConfirmedExactDeletion() {
        launch(); tab("health")
        openFullWeightJournal()
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

    private fun seedLargeWeightJournal(count: Int) = runBlocking(Dispatchers.IO) {
        val today = java.time.LocalDate.now()
        TrainPilotDatabase.getInstance(context).bodyWeightDao().upsertWeights((0 until count).map { i ->
            val id = "weight-%04d".format(i); val date = today.minusDays((count - 1 - i).toLong()).toString()
            val kg = when (i) { 0 -> 140.0; count / 2 -> 35.0; count - 1 -> 80.0; else -> 70.0 + i % 100 / 10.0 }
            com.repforge.app.data.db.BodyWeightEntryEntity(id, date, date, kg, i.toLong(),
                JSONObject().put("id", id).put("date", date).put("kg", kg).toString())
        })
    }

    @Test fun weightOverviewCoversFifteenHundredMeasurementsAcrossRangeChanges() {
        seedLargeWeightJournal(1500)
        launch(); tab("health")
        expandInlineWeight()
        compose.waitUntil(15000) { compose.onAllNodes(hasTestTag("weight_measurement_count") and
            hasText("1500 mérés", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("weight_global_extrema").assertTextContains("35 / 140 kg", substring = true)
        compose.onNodeWithTag("weight_range_choices").performScrollToNode(hasTestTag("weight_range_all"))
        compose.onNodeWithTag("weight_range_all").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodes(hasTestTag("weight_chart_coverage") and
            hasText("1500 mérés", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        screenshot("weight_1500_full_coverage")
        compose.onNodeWithTag("weight_range_choices").performScrollToNode(hasTestTag("weight_range_7"))
        compose.onNodeWithTag("weight_range_7").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodes(hasTestTag("weight_chart_coverage") and
            hasText("8 mérés", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("weight_measurement_count").assertTextContains("1500 mérés", substring = true)
        compose.onNodeWithTag("weight_global_extrema").assertTextContains("35 / 140 kg", substring = true)
        screenshot("weight_1500_week_range")
    }

    @Test fun pagedWeightEditUpdatesOverviewWithoutChangingCountOrOlderMeasurements() {
        seedLargeWeightJournal(200)
        launch(); tab("health")
        openFullWeightJournal()
        compose.waitUntil(15000) { compose.onAllNodes(hasTestTag("weight_measurement_count") and
            hasText("200 mérés", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("weight_journal_list").performScrollToNode(hasTestTag("weight_edit_weight-0199"))
        compose.onNodeWithTag("weight_edit_weight-0199").performClick()
        compose.onNodeWithTag("weight_input").performTextReplacement("81,5")
        compose.onNodeWithTag("weight_save").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("weight_input").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("weight_journal_list").performScrollToIndex(0)
        compose.waitUntil(15000) { compose.onAllNodes(hasTestTag("weight_current_value") and
            hasText("81,5 kg")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("weight_current_value").assertTextEquals("81,5 kg")
        compose.onNodeWithTag("weight_measurement_count").assertTextContains("200 mérés", substring = true)
        compose.onNodeWithTag("weight_global_extrema").assertTextContains("35 / 140 kg", substring = true)
        val old = runBlocking(Dispatchers.IO) { TrainPilotDatabase.getInstance(context).bodyWeightDao().getWeightById("weight-0000")!! }
        assertEquals(140.0, old.kg, .0001)
        screenshot("weight_paged_edit_same_count")
    }

    @Test fun failedWeightSaveKeepsTheDialogAndInputForExplicitRetry() {
        launch(); tab("health")
        openFullWeightJournal()
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
        expandHealthSection("health_history_section", "health_open_records")
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
        compose.onNodeWithTag("health_selected_day").assertTextContains(day.toString(), substring = true)
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
        expandHealthSection("health_history_section", "health_open_sleep")
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasTestTag("health_open_sleep") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("health_open_sleep").assertIsEnabled().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("health_records_list").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("health_records_list").performScrollToNode(hasTestTag("health_record_samsung-sleep"))
        compose.onNodeWithTag("health_record_samsung-sleep").assertExists()
        compose.onNodeWithTag("health_records_list").performScrollToNode(hasTestTag("health_record_expand_samsung-sleep"))
        compose.onNodeWithTag("health_record_expand_samsung-sleep").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("health_records_list").performScrollToNode(hasText("Könnyű alvás"))
        compose.onNodeWithText("Könnyű alvás").assertExists()
        screenshot("health_sleep_samsung")
        compose.onNodeWithTag("health_records_list").performScrollToNode(hasTestTag("health_record_expand_hc-sleep"))
        compose.onNodeWithTag("health_record_expand_hc-sleep").performScrollTo().assertIsDisplayed().performClick()
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
        expandHealthSection("health_history_section", "health_open_records")
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
        expandHealthSection("health_connect_section", "health_daily_priority")
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
        compose.waitUntil(15000) { compose.onAllNodesWithTag("health_journal_list").fetchSemanticsNodes().isNotEmpty() }
        expandHealthSection("health_connect_section", "health_daily_priority")
        compose.onNodeWithTag("health_daily_sources_row").performScrollToNode(hasTestTag("health_daily_origin.alpha"))
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
        expandHealthSection("health_connect_section", "health_hc_background_switch")
        compose.onNodeWithTag("health_hc_background_switch").assertIsOff()
        compose.onNodeWithTag("health_hc_background_status").assertExists()
        compose.onNodeWithText("Nincs ütemezett háttérfrissítés.").assertExists()
        assertFalse(HealthJournalStore.get(context).preferences().getBoolean("hcBackground"))
    }

    @Test fun bleBackgroundCannotClaimToStartWithoutARememberedWatch() {
        assertTrue(BleWatchPreference(context).clear())
        launch(); tab("health")
        compose.onNodeWithTag("health_journal_list")
            .performScrollToNode(hasTestTag("health_open_watch_sync_card"))
        compose.onNodeWithTag("health_open_watch_sync_card").performClick()
        compose.onNodeWithTag("health_ble_list").performScrollToNode(hasTestTag("health_ble_background_switch"))
        compose.onNodeWithTag("health_ble_background_switch").assertIsOff()
        compose.onNodeWithTag("health_ble_background_switch").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("health_background_error").fetchSemanticsNodes().isNotEmpty() }
        assertFalse(HealthJournalStore.get(context).preferences().getBoolean("bleBackground"))
        assertFalse(HealthJournalStore.get(context).preferences().getBoolean("logWatchSteps"))
        compose.onNodeWithTag("health_ble_background_status").assertExists()
        compose.onNodeWithText("Nincs aktív BLE háttérkapcsolat.").assertExists()
    }

    @Test fun personalPlannerIsReadOnlyUntilExplicitAcceptance() {
        runBlocking(Dispatchers.IO) {
            settings.saveTrainingProfile(JSONObject().put("minutes", 60)
                .put("goal", "fitness").put("experience", "beginner")
                .put("gear", org.json.JSONArray().put("dumbbells").put("barbell")))
        }
        val db = TrainPilotDatabase.getInstance(context)
        val baseline = runBlocking(Dispatchers.IO) { db.programDao().getProgramIds().toSet() }
        launch(); tab("training")
        openTrainingPrograms()
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("personal_planner_open"))
        compose.onNodeWithTag("personal_planner_open").performClick()
        compose.onNodeWithTag("personal_preview").assertExists()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("personal_preview_loading").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag("personal_preview_accept").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("personal_preview_accept").assertIsEnabled()
        screenshot("personal_ab_preview")
        assertEquals(baseline, runBlocking(Dispatchers.IO) { db.programDao().getProgramIds().toSet() })
        assertEquals("home-basic", runBlocking(Dispatchers.IO) { settings.current().activeProgramId })
    }

    @Test fun level2PreviewShowsWeightedCoreAndUnsetLoadWithoutActivatingIt() {
        runBlocking(Dispatchers.IO) {
            settings.saveTrainingProfile(JSONObject().put("minutes", 90).put("experience", "intermediate")
                .put("goal", "fitness").put("gear", org.json.JSONArray().put("dumbbells").put("barbell")))
        }
        val db = TrainPilotDatabase.getInstance(context)
        val baseline = runBlocking(Dispatchers.IO) { db.programDao().getProgramIds().toSet() }
        launch(); tab("training")
        openTrainingPrograms()
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("personal_planner_open"))
        compose.onNodeWithTag("personal_planner_open").performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("personal_preview_loading").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag("personal_variation_side-plank").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("personal_template_home-level2").assertIsSelected()
        compose.onNodeWithTag("personal_variation_side-plank").performScrollTo()
            .assertTextContains("Súllyal nehezített változat")
        compose.onNodeWithTag("personal_trial_side-plank").assertExists()
        screenshot("personal_level2_weighted_core")
        compose.onNodeWithText("Mégse").performClick()
        assertEquals(baseline, runBlocking(Dispatchers.IO) { db.programDao().getProgramIds().toSet() })
        assertEquals("home-basic", runBlocking { settings.current().activeProgramId })
    }

    @Test fun plannerShowsLocalizedEquipmentSubstitutionWithoutSavingIt() {
        runBlocking(Dispatchers.IO) {
            settings.saveTrainingProfile(JSONObject().put("minutes", 90).put("experience", "intermediate")
                .put("goal", "fitness").put("gear", org.json.JSONArray().put("dumbbells").put("barbell")))
        }
        val db = TrainPilotDatabase.getInstance(context)
        val baseline = runBlocking(Dispatchers.IO) { db.programDao().getProgramIds().toSet() }
        launch(); tab("training")
        openTrainingPrograms()
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("personal_planner_open"))
        compose.onNodeWithTag("personal_planner_open").performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("personal_preview_loading").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag("personal_change_B_bulgarian-split-squat").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("personal_change_B_bulgarian-split-squat").performScrollTo()
            .assertTextContains("→", substring = true).assertTextContains("Hiányzó felszerelés", substring = true)
        screenshot("personal_equipment_substitution")
        compose.onNodeWithText("Mégse").performClick()
        assertEquals(baseline, runBlocking(Dispatchers.IO) { db.programDao().getProgramIds().toSet() })
    }

    @Test fun plannerRetainsAProvenFullDayBeyondStaticCapacityWithoutActivatingPreview() {
        val db = TrainPilotDatabase.getInstance(context)
        val active = "full-day-fixture"
        runBlocking(Dispatchers.IO) {
            val profile = JSONObject().put("minutes", 40).put("experience", "intermediate").put("goal", "fitness")
                .put("gear", org.json.JSONArray(listOf("dumbbells", "barbell", "bench")))
            settings.saveTrainingProfile(profile); settings.setActiveProgramId(active)
            val source = db.programDao().getProgramWithDaysById("home-basic")!!
            val catalog = db.exerciseLibraryDao().getAllExercises()
            val full = NativeProfileProgramPreview.preview(source, profile.toString(), catalog, "hu", true)
            db.programDao().saveProgramWithDays(source.program.copy(id = active, builtin = false, sourceType = "own",
                rawJson = JSONObject(source.program.rawJson).put("id", active).put("builtin", false).toString()),
                full.draft.days.mapIndexed { i, day -> com.repforge.app.data.db.ProgramDayEntity("$active:${day.id}", active,
                    day.id, day.name, day.name, day.name, day.name, i, org.json.JSONArray(day.entries.map(NativeProgramEditor::exerciseId)).toString()) })
            val ids = full.draft.days.first { it.id == "A" }.entries.map(NativeProgramEditor::exerciseId)
            val end = java.time.Instant.now().minusSeconds(86400)
            val exercises = org.json.JSONArray(ids.map { id ->
                val lib = catalog.first { it.id == id }; val rx = full.prescriptions.getJSONObject(id)
                val top = com.repforge.app.ui.coach.NativeCoachExerciseAdvice.target(JSONObject(lib.rawJson), rx).max!!.toInt()
                JSONObject().put("id", id).put("loadType", lib.loadType).put("repUnit", lib.repUnit).put("effort", "good")
                    .put("prescription", JSONObject(rx.toString())).put("sets", org.json.JSONArray((0..1).map { i ->
                        JSONObject().put("setId", "full-$id-$i").put("done", true).put("reps", "$top")
                            .put("weight", if (lib.loadType == "bodyweight") 0 else 5)
                    }))
            })
            com.repforge.app.data.migration.LegacyWebViewMigrator(context, db, settings).saveWorkoutJsonToRoom(
                JSONObject().put("id", "proven-full-day").put("programId", active).put("dayId", "A")
                    .put("started", end.minusSeconds(1800).toString()).put("finished", end.toString()).put("exercises", exercises))
        }
        val baseline = runBlocking(Dispatchers.IO) { db.programDao().getProgramIds().toSet() }
        launch(); tab("training")
        openTrainingPrograms()
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("personal_planner_open"))
        compose.onNodeWithTag("personal_planner_open").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("personal_preview_loading").fetchSemanticsNodes().isEmpty() &&
            compose.onAllNodesWithTag("personal_preview_accept").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("personal_template_home-basic").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("personal_retained_A").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("personal_retained_A").performScrollTo().assertTextContains("teljes nap megmarad", substring = true)
        screenshot("personal_proven_full_day")
        compose.onNodeWithText("Mégse").performClick()
        assertEquals(baseline, runBlocking(Dispatchers.IO) { db.programDao().getProgramIds().toSet() })
        assertEquals(active, runBlocking { settings.current().activeProgramId })
    }

    @Test fun beginnerPlannerExplainsProvenExperienceWithoutActivatingOrInheritingUnrelatedLoad() {
        val db = TrainPilotDatabase.getInstance(context)
        runBlocking(Dispatchers.IO) {
            settings.saveTrainingProfile(JSONObject().put("minutes", 90).put("experience", "beginner").put("goal", "fitness")
                .put("gear", org.json.JSONArray(listOf("dumbbells", "barbell", "bench"))))
            settings.setActiveProgramId("home-basic")
            val end = java.time.Instant.now().minusSeconds(86400)
            com.repforge.app.data.migration.LegacyWebViewMigrator(context, db, settings).saveWorkoutJsonToRoom(
                JSONObject().put("id", "experience-proof").put("programId", "unrelated-workout").put("dayId", "custom")
                    .put("started", end.minusSeconds(600).toString()).put("finished", end.toString())
                    .put("exercises", org.json.JSONArray().put(JSONObject().put("id", "barbell-row")
                        .put("loadType", "total").put("repUnit", "ism.").put("effort", "good")
                        .put("sets", org.json.JSONArray((0..1).map { i -> JSONObject().put("setId", "experience-$i")
                            .put("done", true).put("reps", "12").put("weight", 20) })))))
        }
        val baseline = runBlocking(Dispatchers.IO) { db.programDao().getProgramIds().toSet() }
        launch(); tab("training")
        openTrainingPrograms()
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("personal_planner_open"))
        compose.onNodeWithTag("personal_planner_open").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("personal_preview_loading").fetchSemanticsNodes().isEmpty() &&
            compose.onAllNodesWithTag("personal_preview_accept").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("personal_template_home-basic").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("personal_practiced_barbell-row").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("personal_practiced_barbell-row").performScrollTo().assertTextContains("automatikus terhelésemelés nélkül", substring = true)
        screenshot("personal_beginner_practiced")
        compose.onNodeWithText("Mégse").performClick()
        assertEquals(baseline, runBlocking(Dispatchers.IO) { db.programDao().getProgramIds().toSet() })
        assertEquals("home-basic", runBlocking { settings.current().activeProgramId })
    }

    @Test fun personalPlannerAcceptanceCreatesNewProgramAndRetainsTrialWeights() {
        runBlocking(Dispatchers.IO) {
            settings.saveTrainingProfile(JSONObject().put("minutes", 45)
                .put("goal", "fitness").put("experience", "beginner")
                .put("gear", org.json.JSONArray().put("dumbbells").put("barbell")))
        }
        launch(); tab("training")
        openTrainingPrograms()
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("personal_planner_open"))
        compose.onNodeWithTag("personal_planner_open").performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("personal_preview_loading").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag("personal_preview_accept").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("personal_preview_accept").performClick()
        compose.waitUntil(15000) { runBlocking(Dispatchers.IO) {
            settings.current().activeProgramId.startsWith("custom-program-")
        } }
        val db = TrainPilotDatabase.getInstance(context)
        val active = runBlocking(Dispatchers.IO) {
            db.programDao().getProgramWithDaysById(settings.current().activeProgramId)!! }
        assertEquals("profile-template", active.program.sourceType)
        assertEquals(2, active.program.templateVersion)
        val recipes = JSONObject(active.program.rawJson).getJSONObject("prescriptions")
        assertTrue(recipes.keys().hasNext())
        assertTrue(recipes.keys().asSequence().all {
            val rx = recipes.getJSONObject(it)
            rx.getInt("weight") == 0 && rx.getInt("rir") == 2
        })
        assertTrue(runBlocking(Dispatchers.IO) { db.programDao().getProgramWithDaysById("home-basic")!!.program.builtin })
        assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
    }

    @Test fun personalPlannerRequiresExplicitDurationAndEquipment() {
        launch(); tab("training")
        openTrainingPrograms()
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("personal_planner_open"))
        compose.onNodeWithTag("personal_planner_open").performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("personal_preview_blocked").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("personal_preview_blocked").assertExists()
        compose.onNodeWithTag("personal_preview_accept").assertIsNotEnabled()
    }

    @Test fun programEditorCreatesCustomPlanAndRenamesDayWithoutStartingWorkout() {
        launch(); tab("training")
        openTrainingPrograms()
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
        openTrainingPrograms()
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("program_create"))
        compose.onNodeWithTag("program_create").performClick()
        compose.onNodeWithTag("program_editor_name").performTextInput("Gyakorlatos terv")
        compose.onNodeWithTag("program_editor_search").performScrollTo()
        val name = com.repforge.app.data.db.NativeBuiltinCatalog.exercises.first { it.id == "goblet-squat" }.nameHu
        compose.onNodeWithTag("program_editor_search").performTextInput(name)
        compose.onNodeWithTag("program_editor_choices")
            .performScrollToNode(hasTestTag("program_editor_add_exercise_goblet-squat"))
        compose.onNodeWithTag("program_editor_add_exercise_goblet-squat").performClick()
        compose.onNodeWithTag("program_editor_rx_0").performScrollTo().performClick()
        compose.onNodeWithTag("program_rx_sets").performScrollTo().performTextReplacement("0")
        compose.onNodeWithTag("program_rx_apply").performClick()
        compose.onNodeWithTag("program_rx_error").assertExists()
        compose.onNodeWithTag("program_rx_cancel").performClick()
        compose.onNodeWithTag("program_editor_rx_0").performScrollTo().performClick()
        compose.onNodeWithTag("program_rx_sets").performScrollTo().performTextReplacement("4")
        compose.onNodeWithTag("program_rx_reps").performScrollTo().performTextReplacement("6–8")
        compose.onNodeWithTag("program_rx_weight").performScrollTo().performTextReplacement("22.5")
        compose.onNodeWithTag("program_rx_rest").performScrollTo().performTextReplacement("75")
        compose.onNodeWithTag("program_rx_apply").performClick()
        assertFalse(runBlocking(Dispatchers.IO) {
            TrainPilotDatabase.getInstance(context).programDao().getProgramsWithDays().any { it.program.nameHu == "Gyakorlatos terv" }
        })
        compose.onNodeWithTag("program_editor_save").performClick()
        val db = TrainPilotDatabase.getInstance(context)
        compose.waitUntil(15000) { runBlocking(Dispatchers.IO) {
            db.programDao().getProgramsWithDays().any {
                it.program.nameHu == "Gyakorlatos terv" &&
                    org.json.JSONArray(it.days.singleOrNull()?.exercisesJson ?: "[]").optString(0) == "goblet-squat"
            }
        } }
        val saved = runBlocking(Dispatchers.IO) { db.programDao().getProgramsWithDays().first { it.program.nameHu == "Gyakorlatos terv" } }
        val rx = JSONObject(saved.program.rawJson).getJSONObject("prescriptions").getJSONObject("goblet-squat")
        assertEquals(4, rx.getInt("sets")); assertEquals(22.5, rx.getDouble("weight"), 0.0)
        assertEquals("6–8", rx.getString("reps")); assertEquals(75, rx.getInt("rest"))
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("program_edit_${saved.program.id}"))
        compose.onNodeWithTag("program_edit_${saved.program.id}").performClick()
        compose.onNodeWithTag("program_editor_rx_0").performScrollTo().performClick()
        compose.onNodeWithTag("program_rx_sets").assertTextContains("4")
        compose.onNodeWithTag("program_rx_sets").performScrollTo().performTextReplacement("7")
        compose.onNodeWithTag("program_rx_apply").performClick()
        compose.onNodeWithTag("program_editor_cancel").performClick()
        assertEquals(saved, runBlocking(Dispatchers.IO) { db.programDao().getProgramWithDaysById(saved.program.id) })
        assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
    }

    @Test fun builtinProgramEditorCreatesCloneWithoutChangingTemplate() {
        val db = TrainPilotDatabase.getInstance(context)
        val before = runBlocking(Dispatchers.IO) {
            db.programDao().getProgramWithDaysById("home-basic")!! }
        launch(); tab("training")
        openTrainingPrograms()
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
        compose.onNodeWithTag("training_open_exercises").performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("custom_exercise_create").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("custom_exercise_create").performClick()
        compose.onNodeWithTag("custom_exercise_name").performTextInput("Saját UI próba")
        compose.onNodeWithTag("custom_exercise_save").performClick()
        val db = TrainPilotDatabase.getInstance(context)
        compose.waitUntil(15000) { runBlocking(Dispatchers.IO) {
            db.exerciseLibraryDao().getAllExercises().any { it.custom && it.nameHu == "Saját UI próba" }
        } }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("custom_exercise_save").fetchSemanticsNodes().isEmpty() }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("custom_exercise_edit").fetchSemanticsNodes().isNotEmpty() }
        screenshot("custom_exercise_created")
        compose.onNodeWithTag("custom_exercise_edit").performClick()
        compose.onNodeWithTag("custom_exercise_name").performTextClearance()
        compose.onNodeWithTag("custom_exercise_name").performTextInput("Saját UI javítva")
        compose.onNodeWithTag("custom_exercise_save").performClick()
        compose.waitUntil(15000) { runBlocking(Dispatchers.IO) {
            db.exerciseLibraryDao().getAllExercises().any { it.custom && it.nameHu == "Saját UI javítva" }
        } }
        // Persisted DB change alone is insufficient: await completion of the actual
        // ViewModel->dialog close path before opening the edited record again.
        compose.waitUntil(15000) { compose.onAllNodesWithTag("custom_exercise_save").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("custom_exercise_edit").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("custom_exercise_delete").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("custom_exercise_delete").performClick()
        compose.onNodeWithTag("custom_exercise_confirm_delete").performScrollTo().performClick()
        try {
            compose.waitUntil(15000) { runBlocking(Dispatchers.IO) {
                db.exerciseLibraryDao().getAllExercises().none { it.custom && it.nameHu == "Saját UI javítva" }
            } }
        } catch (failure: Throwable) {
            val saved = runBlocking(Dispatchers.IO) { db.exerciseLibraryDao().getAllExercises()
                .filter { it.custom }.map { it.id + ":" + it.nameHu } }
            val dialogErrors = compose.onAllNodesWithTag("custom_exercise_error", useUnmergedTree = true)
                .fetchSemanticsNodes().map { it.config.toString() }
            throw AssertionError("Real custom exercise deletion did not persist: $saved; " +
                "visible dialog errors: $dialogErrors", failure)
        }
        assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
    }

    @Test fun invalidCustomVideoCannotWriteOrReplaceCatalog() {
        launch(); tab("training")
        compose.onNodeWithTag("training_open_exercises").performScrollTo().assertIsDisplayed().performClick()
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
        compose.onNodeWithTag("training_open_exercises").performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("exercise_library_count").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("exercise_library_count").assertTextContains("106", substring = true)
        compose.onNodeWithTag("exercise_library_search").performTextInput(
            com.repforge.app.data.db.NativeBuiltinCatalog.exercises.first { it.id == "db-squat" }.nameHu)
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("exercise_library_row_db-squat").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("exercise_library_list")
            .performScrollToNode(hasTestTag("exercise_library_row_db-squat"))
        // On a 320dp emulator the card can be taller than the remaining
        // viewport; verify it is actionable and actually opens its real details.
        compose.onNodeWithTag("exercise_library_row_db-squat")
            .performScrollTo().assertHasClickAction().performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("exercise_library_detail").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("exercise_library_detail").assertExists()
        compose.onNodeWithTag("exercise_library_video").assertExists()
        compose.onNodeWithText("Videó forrása: Mike Hildebrandt").assertExists()
        screenshot("library_original_video_details")
        assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
    }

    @Test fun outdoorExerciseOffersOnlyExplicitUnverifiedSearchNotAnInventedVideo() {
        launch(); tab("training")
        compose.onNodeWithTag("training_open_exercises").performScrollTo().assertIsDisplayed().performClick()
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
        openTrainingQuickChoice("training_quick_strength")
        val squats = com.repforge.app.data.db.NativeBuiltinCatalog.exercises.first { it.id == "db-squat" }.nameHu
        compose.onNodeWithTag("exercise_search").performTextInput(squats)
        compose.onNodeWithTag("exercise_select_db-squat").performClick()
        compose.onNodeWithTag("quick_exercise_video").assertExists()
        assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
    }

    @Test fun exerciseFavoritesCanBeToggledFilteredAndRecoveredOnReentry() {
        launch(); tab("training")
        openTrainingQuickChoice("training_quick_outdoor_choice")
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
        openTrainingQuickChoice("training_quick_outdoor_choice")
        compose.onNodeWithTag("exercise_search").performTextInput("Running")
        compose.onNodeWithTag("exercise_favorite_running").assertTextContains("★")
        assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
    }

    @Test fun coachVariationPreviewCancelsWithoutWritingAndSavesOnlyAnExplicitCopy() {
        val db = TrainPilotDatabase.getInstance(context)
        val id = "coach-variation-fixture"
        runBlocking(Dispatchers.IO) {
            val raw = JSONObject().put("id", id).put("prescriptions", JSONObject().put("pushup",
                JSONObject().put("sets", 2).put("reps", "8–15").put("loadType", "bodyweight")))
            val base = db.programDao().getProgramWithDaysById("home-basic")!!.program
            db.programDao().upsertProgram(base.copy(id = id, builtin = false, rawJson = raw.toString()))
            db.programDao().upsertProgramDays(listOf(com.repforge.app.data.db.ProgramDayEntity("$id:A", id, "A", "A", "A", "A", "A", 0, "[\"pushup\"]")))
            settings.setActiveProgramId(id)
            settings.saveTrainingProfile(JSONObject().put("minutes", 60).put("gear", org.json.JSONArray().put("bench"))
                .put("experience", "intermediate").put("goal", "fitness").put("activity", "office"))
            val importer = com.repforge.app.data.migration.LegacyWebViewMigrator(context, db, settings)
            for (i in 1..3) {
                val end = java.time.Instant.now().minusSeconds(i * 86400L)
                importer.saveWorkoutJsonToRoom(JSONObject().put("id", "variation-$i").put("programId", id).put("dayId", "A")
                    .put("started", end.minusSeconds(60).toString()).put("finished", end.toString())
                    .put("exercises", org.json.JSONArray().put(JSONObject().put("id", "pushup").put("loadType", "bodyweight")
                        .put("repUnit", "ism.").put("effort", "good").put("sets", org.json.JSONArray((0..1).map { j ->
                            JSONObject().put("setId", "s-$i-$j").put("done", true).put("weight", 0).put("reps", "15") })))))
            }
        }
        fun programCount() = runBlocking(Dispatchers.IO) { db.programDao().getProgramIds().size }
        val count = programCount()
        launch(); compose.onNodeWithTag("shortcut_coach").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("coach_ready").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("coach_ready").performScrollToNode(hasTestTag("coach_variation_pushup"))
        compose.onNodeWithTag("coach_variation_pushup").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("coach_variation_save").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Mégse").performClick()
        assertEquals(count, programCount())
        compose.onNodeWithTag("coach_variation_pushup").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("coach_variation_save").fetchSemanticsNodes().isNotEmpty() }
        screenshot("coach_variation_preview")
        compose.onNodeWithTag("coach_variation_save").performClick()
        compose.waitUntil(15000) { programCount() == count + 1 }
        assertEquals(id, runBlocking { settings.current().activeProgramId })
        assertNotNull(runBlocking(Dispatchers.IO) { db.workoutHistoryDao().getWorkoutWithDetails("variation-1") })
    }

    @Test fun earlierHealthDayCanBeSelectedAndDisplaysItsStoredWeight() {
        val day = java.time.YearMonth.now().minusMonths(1).atDay(1).toString()
        val health = HealthJournalStore.get(context)
        health.setPreferences(JSONObject().put("provider", HealthJournalStore.HC))
        health.healthDay(day, JSONObject().put("source", "@aggregate").put("weightKg", 72.3), health.generation())
        launch(); tab("health")
        expandHealthSection("health_history_section", "health_previous_month")
        compose.onNodeWithTag("health_previous_month").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText(day).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(day).performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Betöltés…").fetchSemanticsNodes().isEmpty() }
        expandHealthSection("health_more_section", "health_metric_weightKg")
        compose.onNodeWithText("72,3").performScrollTo().assertIsDisplayed()
        screenshot("health_historical_weight")
    }

}
