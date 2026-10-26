package com.repforge.app

import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repforge.app.data.store.NativeTrainingModes
import com.repforge.app.ui.text.nativeResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Settings and Programs screens on the isolated debuggable emulator. */
@RunWith(AndroidJUnit4::class)
class NativeTrainingModesUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val fixture = NativeLocalizationEmulatorFixture()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun prepare() = fixture.reset()
    @After fun close() = fixture.close(scenario)
    private fun current() = runBlocking(Dispatchers.IO) { fixture.settings.current() }
    private fun launch() {
        scenario = fixture.launch()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag("navigation_home").fetchSemanticsNodes().isNotEmpty()
        }
    }
    private fun settings() {
        compose.onNodeWithTag("navigation_settings").performClick()
        compose.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("training_modes_global_preview"))
    }
    private fun dismissSettingsStatus() {
        val text = nativeResources(fixture.context, current().language)
            .getString(R.string.native_settings_ok)
        compose.onNodeWithText(text).performClick()
    }
    private fun select(tag: String, option: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.onNodeWithTag("${tag}_$option").performClick()
    }

    @Test fun globalPreviewCancelApproveUndoChangesOnlySettings() {
        launch()
        settings()
        assertNull(current().trainingModes197Json)
        compose.onNodeWithTag("training_modes_global_preview").performClick()
        compose.onNodeWithTag("training_modes_global_cancel").performClick()
        assertNull(current().trainingModes197Json)

        select("training_modes_global_style", "hit")
        select("training_modes_global_progression", "linear")
        compose.onNodeWithTag("training_modes_global_preview").performClick()
        assertNull(current().trainingModes197Json)
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag("training_modes_global_approve").fetchSemanticsNodes()
                .any { it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Disabled) == null }
        }
        compose.onNodeWithTag("training_modes_global_comparison_0").assertExists()
        compose.onNodeWithTag("training_modes_global_approve").assertIsEnabled().performClick()
        compose.waitUntil(15_000) { current().trainingModes197Json != null }
        assertEquals("global", NativeTrainingModes.resolveSavedJson(current().trainingModes197Json, "p").source)
        dismissSettingsStatus()
        compose.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("training_modes_global_undo"))
        compose.onNodeWithTag("training_modes_global_undo").performClick()
        compose.waitUntil(15_000) { current().trainingModes197Json == null }
        assertEquals("legacy", NativeTrainingModes.resolveSavedJson(current().trainingModes197Json, "p").source)
    }

    @Test fun globalPickerWithProgramHitRirChangesOnlyGlobalProgressionThroughRealSettingsVm() {
        val programId = runBlocking(Dispatchers.IO) {
            fixture.database.programDao().getProgramsWithDays().first().program.id
        }
        val initial = JSONObject()
            .put("schemaVersion", 1).put("explicitChoice", true)
            .put("defaults", JSONObject().put("trainingStyle", "normal")
                .put("progressionMode", "double"))
            .put("byProgram", JSONObject().put(programId, JSONObject()
                .put("trainingStyle", "hit").put("progressionMode", "rir")
                .put("explicitChoice", true)))
        runBlocking(Dispatchers.IO) {
            fixture.settings.setActiveProgramId(programId)
            fixture.settings.importPortableSettings(JSONObject().put("settings",
                JSONObject().put("trainingModes197", initial)))
        }
        launch()
        settings()
        // Only change progress, not style. Before the fix the global style
        // inherited the selected program's HIT override and got saved as HIT.
        compose.onNodeWithTag("training_modes_global_style").assertTextContains("Normál")
        select("training_modes_global_progression", "linear")
        compose.onNodeWithTag("training_modes_global_preview").performClick()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag("training_modes_global_approve").fetchSemanticsNodes()
                .any { it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Disabled) == null }
        }
        compose.onNodeWithTag("training_modes_global_approve").performClick()
        compose.waitUntil(15_000) {
            JSONObject(current().trainingModes197Json!!).getJSONObject("defaults")
                .optString("progressionMode") == "linear"
        }
        val applied = JSONObject(current().trainingModes197Json!!)
        assertEquals("normal", applied.getJSONObject("defaults").getString("trainingStyle"))
        assertEquals("linear", applied.getJSONObject("defaults").getString("progressionMode"))
        assertEquals("hit", applied.getJSONObject("byProgram")
            .getJSONObject(programId).getString("trainingStyle"))
        assertEquals("rir", applied.getJSONObject("byProgram")
            .getJSONObject(programId).getString("progressionMode"))
    }

    @Test fun modePickerShowsSelectedLanguageInAllFourNativeLocalizations() {
        launch()
        settings()
        for ((language, expected) in listOf(
            "hu" to "Edzésstílus és progresszió",
            "en" to "Training style and progression",
            "de" to "Trainingsstil und Progression",
            "ro" to "Stil de antrenament și progresie")) {
            fixture.selectLanguageThroughUi(compose, language)
            compose.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("training_modes_global_preview"))
            compose.onNodeWithText(expected).assertExists()
        }
    }

    @Test fun programPickerApprovalOverridesGlobalWithoutChangingProgramRecipes() {
        val programId = runBlocking(Dispatchers.IO) {
            val first = fixture.database.programDao().getProgramsWithDays().first().program.id
            fixture.settings.setActiveProgramId(first)
            first
        }
        val global = JSONObject()
            .put("schemaVersion", 1).put("explicitChoice", true)
            .put("defaults", JSONObject().put("trainingStyle", "normal")
                .put("progressionMode", "double"))
            .put("byProgram", JSONObject()).put("revision", 1)
        runBlocking(Dispatchers.IO) {
            fixture.settings.importPortableSettings(JSONObject().put("settings",
                JSONObject().put("trainingModes197", global)))
        }
        val before = runBlocking(Dispatchers.IO) {
            fixture.database.programDao().getProgramWithDaysById(programId)!!.program.rawJson
        }
        launch()
        compose.onNodeWithTag("shortcut_programs").performScrollTo().performClick()
        compose.onNodeWithTag("program_list")
            .performScrollToNode(hasTestTag("training_modes_program_preview"))
        select("training_modes_program_style", "hit")
        select("training_modes_program_progression", "rir")
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("training_modes_program_preview"))
        compose.onNodeWithTag("training_modes_program_preview").performScrollTo().performClick()
        assertEquals("global", NativeTrainingModes.resolveSavedJson(current().trainingModes197Json, programId).source)
        try {
            compose.waitUntil(30_000) {
                compose.onAllNodesWithTag("training_modes_program_approve").fetchSemanticsNodes()
                    .any { it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Disabled) == null }
            }
        } catch (failure: Throwable) {
            throw AssertionError("Program mode comparison: " +
                compose.onRoot(useUnmergedTree = true).printToString().take(8500), failure)
        }
        compose.onNodeWithTag("training_modes_program_comparison_0").assertExists()
        compose.onNodeWithTag("training_modes_program_approve").assertIsEnabled().performClick()
        compose.waitUntil(15_000) {
            NativeTrainingModes.resolveSavedJson(current().trainingModes197Json, programId).source == "program"
        }
        val after = runBlocking(Dispatchers.IO) {
            fixture.database.programDao().getProgramWithDaysById(programId)!!.program.rawJson
        }
        assertEquals(before, after)
        assertEquals("global", NativeTrainingModes.resolveSavedJson(current().trainingModes197Json, "other").source)
        // Program inheritance requires a separate preview and confirmation.
        val buttonLabel = nativeResources(fixture.context, current().language)
            .getString(R.string.native_modes_approve)
        compose.onNodeWithText(buttonLabel).performClick()
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("training_modes_program_inherit"))
        compose.onNodeWithTag("training_modes_program_inherit").performClick()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag("training_modes_program_approve").fetchSemanticsNodes()
                .any { it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Disabled) == null }
        }
        compose.onNodeWithTag("training_modes_program_approve").assertIsEnabled().performClick()
        compose.waitUntil(15_000) {
            val raw = current().trainingModes197Json
            raw != null && JSONObject(raw).getJSONObject("byProgram")
                .getJSONObject(programId).getBoolean("explicitChoice") == false
        }
        assertEquals("global", NativeTrainingModes.resolveSavedJson(current().trainingModes197Json, programId).source)
    }
}
