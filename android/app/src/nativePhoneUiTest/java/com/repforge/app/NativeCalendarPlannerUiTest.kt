package com.repforge.app

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repforge.app.ui.calendar.CalendarScreen
import com.repforge.app.ui.theme.TrainPilotTheme
import com.repforge.app.data.store.TrainPilotUserSettings
import org.junit.*
import org.junit.runner.RunWith

/** Actual Calendar -> VM -> Room controls. No test-only planning implementation. */
@RunWith(AndroidJUnit4::class)
class NativeCalendarPlannerUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    @After fun close() { scenario?.close() }
    private fun open() {
        scenario = ActivityScenario.launch(MainActivity::class.java).also { activity ->
            activity.onActivity { screen -> screen.setContent {
                TrainPilotTheme(TrainPilotUserSettings()) {
                    CalendarScreen(starting = false, onStart = {}, onJournal = {})
                }
            } }
        }
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("calendar_planner_open").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("calendar_planner_open").performClick()
    }
    @Test fun previewCancelThenSaveAndReopenStoredWeeks() {
        open()
        compose.onNodeWithTag("calendar_planner_weeks").performScrollTo().performTextReplacement("4")
        compose.onNodeWithTag("calendar_planner_generate").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("calendar_planner_save").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("calendar_planner_cancel").performClick()
        compose.onNodeWithTag("calendar_planner_open").performClick()
        compose.onNodeWithTag("calendar_planner_generate").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("calendar_planner_save").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("calendar_planner_save").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("calendar_planner_cancel").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("calendar_planner_open").performClick()
        compose.onNodeWithTag("calendar_planner_weeks").performScrollTo().assertTextContains("4")
    }
}
