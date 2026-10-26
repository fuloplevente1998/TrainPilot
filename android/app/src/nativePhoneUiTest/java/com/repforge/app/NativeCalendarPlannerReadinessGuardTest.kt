package com.repforge.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CompletableDeferred
import com.repforge.app.ui.calendar.NativeCalendarPlanner
import com.repforge.app.ui.calendar.calendarPlannerInitialSelection
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

    @Test fun plannerOpenDisabledUntilSettingsLoadedThenClickOpens() {
        val allowSettingsRead = CompletableDeferred<Unit>()
        harness.launch(plannerSettingsReadGate = { allowSettingsRead.await() }, waitForReady = false)
        // Programs may already be available, but planner settings are deliberately not.
        compose.waitUntil(20_000) {
            harness.model.state.value.programs.any { it.days.isNotEmpty() }
        }
        assertFalse(harness.model.state.value.plannerLoaded)
        harness.assertOpenDisabled()
        assertNull(harness.model.state.value.plannerPreview)

        allowSettingsRead.complete(Unit)
        harness.openPlanner()
        assertTrue(harness.model.state.value.plannerLoaded)
        harness.cancel()
    }

    @Test fun plannerOpenDisabledUntilProgramsLoadedThenClickOpens() {
        val allowProgramRead = CompletableDeferred<Unit>()
        harness.launch(programsReadGate = { allowProgramRead.await() }, waitForReady = false)
        compose.waitUntil(20_000) { harness.model.state.value.plannerLoaded }
        assertTrue("No programs published before the delayed Room subscription",
            harness.model.state.value.programs.isEmpty())
        harness.assertOpenDisabled()
        allowProgramRead.complete(Unit)
        harness.openPlanner()
        assertTrue(harness.model.state.value.programs.any { it.days.isNotEmpty() })
        harness.cancel()
    }

    @Test fun cancelDuringRepositoryPreviewReadDropsLateResultAndContinuation() {
        val target = LocalDate.now().plusYears(6).withDayOfMonth(1)
        val beforeSettings = harness.storedSettings()
        val beforeRows = harness.rows(target)
        val readCompleted = CompletableDeferred<Unit>()
        val releaseLateResult = CompletableDeferred<Unit>()
        harness.launch(previewReadGate = {
            readCompleted.complete(Unit)
            releaseLateResult.await()
        })
        harness.openPlanner()
        harness.replace("calendar_planner_date", target.toString())
        harness.replace("calendar_planner_weeks", "4")
        harness.beginGenerate()
        // The real Room repository.preview has completed, but VM publication is suspended.
        compose.waitUntil(20_000) { readCompleted.isCompleted }
        assertTrue("Preview mutation remains in flight", harness.model.state.value.busy)
        compose.runOnIdle { harness.model.cancelPlanner() }
        assertNull(harness.model.state.value.plannerPreview)
        releaseLateResult.complete(Unit)
        compose.waitUntil(20_000) { !harness.model.state.value.busy }
        assertFalse(harness.model.state.value.error)
        assertNull("Cancelled IO result may not publish a preview",
            harness.model.state.value.plannerPreview)
        compose.runOnIdle {
            harness.model.applyPlanner {
                fail("A cancelled in-flight preview must never call the saved callback")
            }
        }
        compose.waitForIdle()
        assertNull(harness.model.state.value.plannerPreview)
        assertEquals(beforeSettings, harness.storedSettings())
        assertEquals(beforeRows, harness.rows(target))
        harness.cancel()
    }

    @Test fun missingProgramAndShortenedProgramGetValidFirstDayFallback() {
        harness.launch()
        val programs = harness.model.state.value.programs
        val firstUsable = programs.first { it.days.isNotEmpty() }
        val settings = NativeCalendarPlanner.Settings(
            firstUsable.program.id, LocalDate.now().plusYears(3), LocalTime.of(18, 0),
            firstIndex = 0
        )
        val removed = calendarPlannerInitialSelection(
            settings.copy(programId = "deleted-program", firstIndex = 999), programs
        )
        assertEquals(firstUsable.program.id, removed.programId)
        assertEquals(0, removed.firstIndex)

        val shortened = calendarPlannerInitialSelection(
            settings.copy(firstIndex = Int.MAX_VALUE), programs
        )
        assertEquals(firstUsable.program.id, shortened.programId)
        assertEquals(firstUsable.days.lastIndex, shortened.firstIndex)
        val noPrograms = calendarPlannerInitialSelection(settings, emptyList())
        assertEquals("", noPrograms.programId)
        assertEquals(0, noPrograms.firstIndex)
        // Empty choices cannot open a usable planner or generate a plan.
        harness.openPlanner()
        harness.cancel()
    }
}
