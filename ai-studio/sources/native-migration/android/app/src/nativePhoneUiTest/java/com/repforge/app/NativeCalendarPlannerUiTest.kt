package com.repforge.app

import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.calendar.CalendarScreen
import com.repforge.app.ui.calendar.CalendarViewModel
import com.repforge.app.ui.calendar.NativeCalendarPlanner
import com.repforge.app.ui.calendar.NativeCalendarRepository
import com.repforge.app.ui.theme.TrainPilotTheme
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real MainActivity host, exactly one replacement Calendar composition, the real
 * CalendarViewModel and production Room. The shared emulator fixture resets only
 * disposable debug emulator data and marks onboarding complete before launch.
 */
internal class CalendarPlannerUiHarness(private val compose: ComposeTestRule) {
    private val fixture = NativeLocalizationEmulatorFixture()
    private var scenario: ActivityScenario<MainActivity>? = null
    lateinit var model: CalendarViewModel
        private set

    fun reset() = fixture.reset()

    fun close() {
        fixture.close(scenario, compose)
        scenario = null
    }

    fun launch(
        plannerSettingsReadGate: (suspend () -> Unit)? = null,
        programsReadGate: (suspend () -> Unit)? = null,
        previewReadGate: (suspend () -> Unit)? = null,
        waitForReady: Boolean = true
    ) {
        check(scenario == null) { "Close the previous activity before relaunching" }
        scenario = fixture.launch().also { current ->
            current.onActivity { activity ->
                val customFactory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        require(modelClass == CalendarViewModel::class.java)
                        return CalendarViewModel(activity.application,
                            plannerSettingsReadGate ?: {}, programsReadGate ?: {},
                            previewReadGate ?: {}) as T
                    }
                }
                // Own key prevents reuse of a ViewModel installed by MainActivity navigation.
                model = ViewModelProvider(activity, customFactory).get(
                    "calendar_planner_ui_isolated_model", CalendarViewModel::class.java)
                // Replaces MainActivity's default composition; never mounts a second one.
                activity.setContent {
                    TrainPilotTheme(TrainPilotUserSettings()) {
                        CalendarScreen(starting = false, onStart = {}, onJournal = {}, model = model)
                    }
                }
            }
        }
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("calendar_planner_open").fetchSemanticsNodes().isNotEmpty()
        }
        if (waitForReady) awaitEnabled("calendar_planner_open")
    }

    fun assertOpenDisabled() {
        compose.onNodeWithTag("calendar_planner_open").assertIsNotEnabled()
        assertTrue("Disabled planner must not show a dialog",
            compose.onAllNodesWithTag("calendar_planner_weeks").fetchSemanticsNodes().isEmpty())
    }

    private fun awaitEnabled(tag: String) {
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().any {
                !it.config.contains(SemanticsProperties.Disabled)
            }
        }
        compose.onNodeWithTag(tag).assertIsEnabled()
    }

    fun openPlanner() {
        compose.waitUntil(20_000) {
            model.state.value.plannerLoaded &&
                model.state.value.programs.any { it.days.isNotEmpty() }
        }
        awaitEnabled("calendar_planner_open")
        compose.onNodeWithTag("calendar_planner_open").performScrollTo().performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("calendar_planner_weeks").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("calendar_planner_weeks").assertIsEnabled()
    }

    fun replace(tag: String, text: String) {
        compose.onNodeWithTag(tag).performScrollTo().performTextReplacement(text)
    }

    fun beginGenerate() {
        awaitEnabled("calendar_planner_generate")
        compose.onNodeWithTag("calendar_planner_generate").performClick()
    }

    fun generate(): NativeCalendarRepository.Preview {
        beginGenerate()
        awaitEnabled("calendar_planner_save")
        return requireNotNull(model.state.value.plannerPreview).also {
            assertTrue("Preview must contain planned slots", it.plan.slots.isNotEmpty())
        }
    }

    fun cancel() {
        awaitEnabled("calendar_planner_cancel")
        compose.onNodeWithTag("calendar_planner_cancel").performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("calendar_planner_cancel").fetchSemanticsNodes().isEmpty() &&
                model.state.value.plannerPreview == null
        }
    }

    fun save() {
        awaitEnabled("calendar_planner_save")
        compose.onNodeWithTag("calendar_planner_save").performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("calendar_planner_cancel").fetchSemanticsNodes().isEmpty() &&
                model.state.value.plannerPreview == null && !model.state.value.busy
        }
        assertFalse("Save must not report a VM error", model.state.value.error)
    }

    fun awaitApplyWithoutDialogDismissal() {
        compose.waitUntil(20_000) {
            model.state.value.plannerPreview == null && !model.state.value.busy
        }
        assertFalse("Apply must not report a VM error", model.state.value.error)
    }

    fun storedSettings(): NativeCalendarPlanner.Settings? = runBlocking(Dispatchers.IO) {
        NativeCalendarRepository(fixture.database) { null }.plannerSettings()
    }

    /** Read-only Room observation. Planner generation/saving is always through UI/VM. */
    fun rows(from: LocalDate): List<CalendarScheduleEntity> = runBlocking(Dispatchers.IO) {
        fixture.database.calendarScheduleDao()
            .observeEntriesInRange(from.toString(), from.plusDays(45).toString()).first()
    }

    fun assertPersistedSlots(from: LocalDate, preview: NativeCalendarRepository.Preview) {
        val rows = rows(from)
        val expected = preview.plan.slots.map { it.date.toString() to it.dayId }
        assertEquals("Duplicate slots in generated preview", expected.size, expected.toSet().size)
        assertEquals("One persisted Room entry per preview slot", expected.size, rows.size)
        assertEquals(expected.toSet(), rows.map { it.date to it.dayId }.toSet())
    }
}

/** Preview -> Cancel -> re-open -> preview -> Save -> restart -> re-open. */
@RunWith(AndroidJUnit4::class)
class NativeCalendarPlannerUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val harness by lazy { CalendarPlannerUiHarness(compose) }

    @Before fun setup() = harness.reset()
    @After fun cleanup() = harness.close()

    @Test fun previewCancelThenSaveAndReopenStoredWeeks() {
        val target = LocalDate.now().plusYears(3).withDayOfMonth(1)
        val originalSettings = harness.storedSettings()
        val originalRows = harness.rows(target)
        assertTrue("Disposable emulator must start without future calendar rows", originalRows.isEmpty())

        harness.launch()
        harness.openPlanner()
        // A cancelled edit must differ from the original even if default weeks is 4.
        val cancelledWeeks = if ((originalSettings?.weeks ?: 4) == 4) 3 else 4
        harness.replace("calendar_planner_date", target.toString())
        harness.replace("calendar_planner_weeks", cancelledWeeks.toString())
        harness.generate()
        assertEquals("Preview may not persist settings", originalSettings, harness.storedSettings())
        assertEquals("Preview may not persist rows", originalRows, harness.rows(target))
        harness.cancel()
        assertEquals("Cancel may not persist settings", originalSettings, harness.storedSettings())
        assertEquals("Cancel may not persist calendar rows", originalRows, harness.rows(target))

        harness.openPlanner()
        compose.onNodeWithTag("calendar_planner_weeks").performScrollTo()
            .assertTextContains((originalSettings?.weeks ?: 4).toString())
        // Do not rely on the discarded preview's unsaved weeks or date.
        harness.replace("calendar_planner_date", target.toString())
        harness.replace("calendar_planner_weeks", "4")
        val previewToSave = harness.generate()
        assertEquals(originalRows, harness.rows(target))
        harness.save()
        assertEquals(4, harness.storedSettings()?.weeks)
        harness.assertPersistedSlots(target, previewToSave)

        // A new MainActivity and ViewModel must read the stored value from Room.
        harness.close()
        harness.launch()
        harness.openPlanner()
        compose.onNodeWithTag("calendar_planner_weeks").performScrollTo().assertTextContains("4")
        assertEquals(4, harness.storedSettings()?.weeks)
        harness.assertPersistedSlots(target, previewToSave)
        harness.cancel()
    }
}
