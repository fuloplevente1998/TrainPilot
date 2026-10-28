package com.repforge.app

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assertTextContains
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.health.HealthDashboardUiState
import com.repforge.app.ui.health.HealthJournalTabContent
import com.repforge.app.ui.theme.TrainPilotTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Compose controls/progress/error semantics; provider execution is covered separately. */
@RunWith(AndroidJUnit4::class)
class NativeHealthSyncUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    @After fun close() { closeNativeComposeScenario(scenario, compose) }

    @Test fun fullNormalCancelAndRetryControlsDispatchAndPreserveDisplayedHistory() {
        val state = mutableStateOf(HealthDashboardUiState(selectedDay = "2026-09-21", historyMonth = "2026-09",
            historyChannel = "health_connect", dailySourcePreference = "priority"))
        var normal = 0; var full = 0; var cancelled = 0; var retried = 0
        scenario = ActivityScenario.launch(MainActivity::class.java).also { activity ->
            activity.onActivity { screen -> screen.setContent {
                TrainPilotTheme(TrainPilotUserSettings()) {
                    HealthJournalTabContent(state.value,
                        onWeightJournal = {},
                        onSelectProvider = {}, onToggleHcBackground = {}, onAutoOnOpen = {}, onRefresh = {},
                        onPreviousMonth = {}, onNextMonth = {}, onDay = {}, onChannel = {}, onToday = {},
                        onOpenRecords = {}, onDailySource = {},
                        onSync = { normal++ }, onFullSync = { full++ },
                        onCancelSync = { cancelled++ }, onRetrySync = { retried++ })
                }
            } }
        }
        compose.onNodeWithTag("health_journal_list")
            .performScrollToNode(hasTestTag("health_connect_section"))
        compose.onNodeWithTag("health_connect_section").performClick()

        fun click(tag: String) {
            compose.onNodeWithTag("health_journal_list").performScrollToNode(hasTestTag(tag))
            compose.onNodeWithTag(tag).performClick()
        }
        click("health_sync_now"); click("health_sync_full")
        compose.runOnIdle { state.value = state.value.copy(healthRefreshing = true, syncStage = "DAYS", syncCompleted = 7, syncTotal = 30) }
        compose.onNodeWithTag("health_journal_list").performScrollToNode(hasTestTag("health_sync_progress"))
        compose.onNodeWithTag("health_sync_progress").assertTextContains("7", substring = true).assertTextContains("30", substring = true)
        click("health_sync_cancel")
        compose.runOnIdle { state.value = state.value.copy(healthRefreshing = false, syncPartial = true) }
        compose.onNodeWithTag("health_journal_list").performScrollToNode(hasTestTag("health_sync_failure"))
        compose.onNodeWithTag("health_sync_failure").assertIsDisplayed()
        click("health_sync_retry")
        compose.runOnIdle {
            assertEquals(1, normal); assertEquals(1, full); assertEquals(1, cancelled); assertEquals(1, retried)
            assertEquals("2026-09-21", state.value.selectedDay)
            assertEquals("health_connect", state.value.historyChannel)
            assertEquals("priority", state.value.dailySourcePreference)
        }
    }
}
