package com.repforge.app

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Source-only until the integrator runs the canonical isolated emulator gate. */
@RunWith(AndroidJUnit4::class)
class NativeOnboardingUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var store: TrainPilotSettingsStore
    private val fixture = NativeLocalizationEmulatorFixture()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun isolatedData() {
        fixture.reset()
        assertTrue("Do not run against a release or live-data installation",
            context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().clear().commit()
        NativeReplacementJournal(context).clear()
        instrumentation.runOnMainSync { ActiveWorkoutStore.clear(context) }
        store = TrainPilotSettingsStore(context)
        runBlocking(Dispatchers.IO) {
            store.resetAllSettingsForEraseLocal()
            store.markMigrationComplete()
        }
    }

    @After fun close() { fixture.close(scenario) }

    private fun launch(expected: String = "onboarding_screen") {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitFor(expected)
    }

    private fun waitFor(tag: String) {
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun welcomeSkipColdRestartThenContinueProfile() {
        launch()
        compose.onNodeWithTag("onboarding_welcome").assertIsDisplayed()
        compose.onNodeWithTag("onboarding_skip").performClick()
        waitFor("navigation_home")
        assertEquals("skipped", runBlocking(Dispatchers.IO) { store.current().onboarding128 })
        scenario?.close()
        launch("navigation_home")
        compose.onNodeWithTag("onboarding_screen").assertDoesNotExist()
        compose.onNodeWithTag("home_resume_onboarding").performClick()
        waitFor("onboarding_welcome")
        compose.onNodeWithTag("onboarding_begin").performClick()
        waitFor("onboarding_profile")
        assertEquals("in_progress", runBlocking(Dispatchers.IO) { store.current().onboarding128 })
        scenario?.close()
        launch()
        compose.onNodeWithTag("onboarding_welcome").assertDoesNotExist()
        waitFor("onboarding_profile")
        compose.onNodeWithTag("onboarding_edit_profile").performClick()
        waitFor("profile_field_goal")
        compose.onNodeWithTag("profile_field_goal").performScrollTo().performTextInput("fitness")
        compose.onNodeWithTag("profile_save").performClick()
        waitFor("navigation_home")
        assertEquals("complete", runBlocking(Dispatchers.IO) { store.current().onboarding128 })
        assertEquals("fitness", JSONObject(runBlocking(Dispatchers.IO) { store.current().profileJson }).getString("goal"))
    }

    @Test fun invalidProfileCannotCompleteThenValidProfileSaves() {
        launch()
        compose.onNodeWithTag("onboarding_begin").performClick()
        waitFor("onboarding_profile")
        compose.onNodeWithTag("onboarding_edit_profile").performClick()
        waitFor("profile_field_weight")
        compose.onNodeWithTag("profile_field_weight").performTextInput("-2")
        compose.onNodeWithTag("profile_save").performClick()
        waitFor("profile_write_error")
        assertEquals("in_progress", runBlocking(Dispatchers.IO) { store.current().onboarding128 })
        compose.onNodeWithTag("profile_field_weight").performTextClearance()
        compose.onNodeWithTag("profile_field_weight").performTextInput("70")
        compose.onNodeWithTag("profile_save").performClick()
        waitFor("navigation_home")
        assertEquals("complete", runBlocking(Dispatchers.IO) { store.current().onboarding128 })
        assertEquals(70.0, JSONObject(runBlocking(Dispatchers.IO) { store.current().profileJson }).getDouble("weight"), 0.01)
    }
}
