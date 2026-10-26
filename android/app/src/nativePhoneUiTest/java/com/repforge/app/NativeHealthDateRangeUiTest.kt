package com.repforge.app

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.DatePicker
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/** Interacts with the real Android DatePickerDialogs in the real native app, not test-only query injection. */
@RunWith(AndroidJUnit4::class)
class NativeHealthDateRangeUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.targetContext
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun isolatedHealthData() {
        assertTrue(context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
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
        runBlocking(Dispatchers.IO) {
            val db = TrainPilotDatabase.getInstance(context)
            db.clearAllTables()
            TrainPilotDatabase.ensureBuiltinCatalogSeeded(db, context)
            TrainPilotSettingsStore(context).apply {
                resetAllSettingsForEraseLocal()
                importPortableSettings(org.json.JSONObject().put("settings", org.json.JSONObject().put("onboarding128", "complete")))
                markMigrationComplete()
                setLanguage("en")
            }
            HealthJournalStore.get(context).erase()
        }
    }

    @After fun close() {
        scenario?.close()
        context.stopService(Intent(context, IntervalTimerService::class.java))
        context.stopService(Intent(context, DistanceTrackingService::class.java))
    }

    private fun record(id: String, date: LocalDate): JSONObject {
        val time = date.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return JSONObject().put("recordId", id).put("day", date.toString())
            .put("channel", HealthJournalStore.HC).put("source", "source.alpha")
            .put("type", "WeightRecord").put("startMs", time).put("endMs", time + 1000)
            .put("lastModifiedMs", time + 2000)
            .put("data", JSONObject().put("value", 72.5).put("unit", "kg"))
    }

    private fun openBrowser() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(30000) {
            compose.onAllNodesWithTag("navigation_health").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("navigation_health").performClick()
        compose.onNodeWithTag("health_journal_list").performScrollToNode(hasTestTag("health_open_records"))
        compose.onNodeWithTag("health_open_records").performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("health_range_from").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun pickActualDialogDate(date: LocalDate) {
        // Espresso locates the platform DatePicker widget actually opened by the Compose button.
        onView(isAssignableFrom(DatePicker::class.java)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(DatePicker::class.java)
            override fun getDescription() = "Select calendar date $date in real DatePicker"
            override fun perform(uiController: UiController, view: View) {
                (view as DatePicker).updateDate(date.year, date.monthValue - 1, date.dayOfMonth)
                uiController.loopMainThreadUntilIdle()
            }
        })
        onView(withId(android.R.id.button1)).perform(click())
    }

    @Test fun datePickersExpandPagedNativeHealthRecordsAndBlockReversedRange() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val store = HealthJournalStore.get(context)
        store.importRecords(JSONArray().put(record("today", today)).put(record("yesterday", yesterday)),
            store.generation())

        openBrowser()
        compose.onNodeWithText("From date").assertExists()
        compose.onNodeWithText("To date").assertExists()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("health_record_today").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("health_record_today").assertExists()
        compose.onNodeWithTag("health_record_yesterday").assertDoesNotExist()

        compose.onNodeWithTag("health_range_from").performClick()
        pickActualDialogDate(yesterday)
        compose.onNodeWithTag("health_range_apply").performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("health_records_list").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("health_records_list").performScrollToNode(hasTestTag("health_record_yesterday"))
        compose.onNodeWithTag("health_records_list").performScrollToNode(hasTestTag("health_record_today"))
        compose.onNodeWithTag("health_record_today").assertExists()
        compose.onNodeWithTag("health_records_list").performScrollToNode(hasTestTag("health_record_yesterday"))
        compose.onNodeWithTag("health_record_yesterday").assertExists()

        // The draft can be reversed temporarily, but cannot be submitted to the store.
        compose.onNodeWithTag("health_range_to").performClick()
        pickActualDialogDate(yesterday)
        compose.onNodeWithTag("health_range_from").performClick()
        pickActualDialogDate(today)
        compose.onNodeWithTag("health_range_error").assertExists()
        compose.onNodeWithTag("health_range_apply").assertIsNotEnabled()
        // Still shows the last successfully queried (two-day) record set.
        compose.onNodeWithTag("health_records_list").performScrollToNode(hasTestTag("health_record_yesterday"))
        compose.onNodeWithTag("health_record_yesterday").assertExists()
    }
}
