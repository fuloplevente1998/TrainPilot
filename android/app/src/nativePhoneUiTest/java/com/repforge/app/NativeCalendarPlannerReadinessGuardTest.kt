package com.repforge.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Guard regressions on the same real UI -> VM -> Room route. */
@RunWith(AndroidJUnit4::class)
class NativeCalendarPlannerReadinessGuardTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val harness by lazy { CalendarPlannerUiHarness(compose) }

    @Before fun setup() = harness.reset()
    @After fun cleanup() = harness.close()

    @Test fun cancelInvalidatesPreviewBeforeAnyLateApply() {
        val target = LocalDate.now().plusYears(4).withDayOfMonth(1)
        val originalSettings = harness.storedSettings()
        val originalRows = harness.rows(target)
        harness.launch()
        harness.openPlanner()
        harness.replace("calendar_planner_date", target.toString())
        harness.replace("calendar_planner_weeks", "3")
        harness.generate()
        harness.cancel()
        // Simulate a stale caller trying to apply a cancelled preview.
        compose.runOnIdle { harness.model.applyPlanner {} }
        compose.waitForIdle()
        assertNull(harness.model.state.value.plannerPreview)
        assertFalse(harness.model.state.value.busy)
        assertEquals(originalSettings, harness.storedSettings())
        assertEquals(originalRows, harness.rows(target))
    }

    @Test fun duplicateApplyCannotCreateDuplicateRoomRows() {
        val target = LocalDate.now().plusYears(5).withDayOfMonth(1)
        harness.launch()
        harness.openPlanner()
        harness.replace("calendar_planner_date", target.toString())
        harness.replace("calendar_planner_weeks", "4")
        val expected = harness.generate()
        assertTrue(harness.rows(target).isEmpty())
        // Two same-frame apply calls race against the Compose button's disabled recomposition.
        // The production VM busy guard and repository CAS must accept at most one.
        compose.runOnIdle {
            harness.model.applyPlanner {}
            harness.model.applyPlanner {}
        }
        harness.awaitApplyWithoutDialogDismissal()
        harness.assertPersistedSlots(target, expected)
        assertEquals(4, harness.storedSettings()?.weeks)
        harness.cancel()
        harness.close()
        harness.launch()
        harness.openPlanner()
        harness.assertPersistedSlots(target, expected)
        assertEquals(4, harness.storedSettings()?.weeks)
        harness.cancel()
    }
}
