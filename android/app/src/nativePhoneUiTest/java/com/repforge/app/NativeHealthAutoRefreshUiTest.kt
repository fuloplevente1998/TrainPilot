package com.repforge.app

import android.content.Context
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Compose destination and persisted store. No Samsung/HC SDK success is simulated. */
@RunWith(AndroidJUnit4::class)
class NativeHealthAutoRefreshUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.targetContext
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun resetPreference() {
        assertTrue(context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
        runBlocking(Dispatchers.IO) {
            NativeDataReplacement.mutate(context) {
                HealthJournalStore.get(context).setPreferences(JSONObject()
                    .put("autoOnOpen", false).put("provider", HealthJournalStore.HC))
            }
        }
    }

    @After fun close() {
        scenario?.close()
        runBlocking(Dispatchers.IO) {
            NativeDataReplacement.mutate(context) {
                HealthJournalStore.get(context).setPreferences(JSONObject().put("autoOnOpen", false))
            }
        }
    }

    private fun openHealth() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("navigation_health").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("navigation_health").performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("health_auto_refresh_switch").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("health_journal_list")
            .performScrollToNode(hasTestTag("health_auto_refresh_switch"))
    }

    @Test fun realAutoSwitchPersistsInExistingHealthJournalAndSurvivesDestinationReentry() {
        openHealth()
        compose.onNodeWithTag("health_auto_refresh_switch").assertExists().assertIsOff()
        assertFalse(HealthJournalStore.get(context).preferences().optBoolean("autoOnOpen"))
        compose.onNodeWithTag("health_auto_refresh_switch").performClick()
        compose.waitUntil(15_000) {
            HealthJournalStore.get(context).preferences().optBoolean("autoOnOpen")
        }
        compose.onNodeWithTag("health_auto_refresh_switch").assertIsOn()

        compose.onNodeWithTag("navigation_home").performClick()
        compose.onNodeWithTag("navigation_health").performClick()
        compose.onNodeWithTag("health_journal_list")
            .performScrollToNode(hasTestTag("health_auto_refresh_switch"))
        compose.onNodeWithTag("health_auto_refresh_switch").assertIsOn()

        compose.onNodeWithTag("health_auto_refresh_switch").performClick()
        compose.waitUntil(15_000) {
            !HealthJournalStore.get(context).preferences().optBoolean("autoOnOpen")
        }
        compose.onNodeWithTag("health_auto_refresh_switch").assertIsOff()
    }
}
