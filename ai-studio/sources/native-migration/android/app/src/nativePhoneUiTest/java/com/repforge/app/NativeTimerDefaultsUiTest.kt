package com.repforge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Settings route -> ViewModel -> DataStore, only in a disposable debug emulator. */
@RunWith(AndroidJUnit4::class)
class NativeTimerDefaultsUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val fixture = NativeLocalizationEmulatorFixture()
    private var scenario: ActivityScenario<MainActivity>? = null
    @Before fun prepare() = fixture.reset()
    @After fun close() = fixture.close(scenario, compose)
    private fun current() = runBlocking(Dispatchers.IO) { fixture.settings.current().timerConfiguration().toString() }
    private fun open() {
        scenario = fixture.launch()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("navigation_settings").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("navigation_settings").performClick()
        edit()
    }
    private fun edit() {
        compose.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_timer_open"))
        compose.onNodeWithTag("settings_timer_open").performClick()
    }
    private fun number(tag: String, value: String) {
        compose.onNodeWithTag("settings_timer_$tag").performScrollTo().performTextReplacement(value)
    }
    @Test fun cancelDoesNotWriteAndSaveReopensAllSixPersistedDefaults() {
        val original = current()
        open()
        number("work", "45")
        compose.onNodeWithTag("settings_timer_cancel").performClick()
        assertEquals(original, current())
        edit()
        number("work", "45"); number("rest", "0"); number("rounds", "6")
        for (tag in listOf("sound", "vibration", "auto")) {
            compose.onNodeWithTag("settings_timer_$tag").performScrollTo().assertIsOn().performClick()
        }
        compose.onNodeWithTag("settings_timer_save").performClick()
        compose.waitUntil(15_000) { current() != original }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("settings_timer_save").fetchSemanticsNodes().isEmpty() }
        edit()
        compose.onNodeWithTag("settings_timer_work").assertTextContains("45")
        compose.onNodeWithTag("settings_timer_rest").assertTextContains("0")
        compose.onNodeWithTag("settings_timer_rounds").assertTextContains("6")
        for (tag in listOf("sound", "vibration", "auto")) {
            compose.onNodeWithTag("settings_timer_$tag").performScrollTo().assertIsOff()
        }
    }
    @Test fun zeroWorkAndOver24HourPlanCannotBeSaved() {
        val original = current()
        open()
        number("work", "0")
        compose.onNodeWithTag("settings_timer_save").assertIsNotEnabled()
        number("work", "3600"); number("rest", "3600"); number("rounds", "100")
        compose.onNodeWithTag("settings_timer_save").assertIsNotEnabled()
        assertEquals(original, current())
        compose.onNodeWithTag("settings_timer_cancel").performClick()
        assertEquals(original, current())
    }
}
