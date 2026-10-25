package com.repforge.app

import com.repforge.app.data.store.NativeTrainingModes
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeTrainingModesTest {
    private fun config(
        global: Boolean = true,
        style: String = "hit",
        progression: String = "linear"
    ): JSONObject = JSONObject()
        .put("schemaVersion", 1)
        .put("defaults", JSONObject().put("trainingStyle", style).put("progressionMode", progression))
        .put("explicitChoice", global)
        .put("byProgram", JSONObject())

    @Test fun legacyAndMalformedAreNeverOptedIn() {
        listOf(null, "", "[]", "{invalid}", "{}", """{"schemaVersion":"1"}""").forEach {
            val effective = NativeTrainingModes.resolveSavedJson(it, "p")
            assertEquals("legacy", effective.source)
            assertEquals("normal", effective.trainingStyle)
            assertEquals("double", effective.progressionMode)
            assertFalse(effective.explicitChoice)
        }
        assertEquals("legacy", NativeTrainingModes.resolve(config(style = "invalid"), "p").source)
        assertEquals("legacy", NativeTrainingModes.resolve(config().put("schemaVersion", 2), "p").source)
        assertEquals("legacy", NativeTrainingModes.resolve(config().put("explicitChoice", "true"), "p").source)
    }

    @Test fun explicitGlobalNeedsValidSchemaAndApproval() {
        val selected = NativeTrainingModes.resolve(config(), "p")
        assertEquals("global", selected.source)
        assertEquals("hit", selected.trainingStyle)
        assertEquals("linear", selected.progressionMode)
        assertEquals("p", selected.programId)
        assertTrue(selected.explicitChoice)
        assertEquals("legacy", NativeTrainingModes.resolve(config(global = false), "p").source)
    }

    @Test fun programSelectionOverridesGlobalAndIsScoped() {
        val saved = config()
        saved.getJSONObject("byProgram").put("p", JSONObject()
            .put("trainingStyle", "normal").put("progressionMode", "rir")
            .put("explicitChoice", true).put("updatedAt", "2026-10-20T10:00:00Z"))
        val selected = NativeTrainingModes.resolve(saved, "p")
        assertEquals("program", selected.source)
        assertEquals("normal", selected.trainingStyle)
        assertEquals("rir", selected.progressionMode)
        assertEquals("global", NativeTrainingModes.resolve(saved, "q").source)
    }

    @Test fun explicitlyDisabledProgramOverrideFallsBackToGlobal() {
        val saved = config()
        saved.getJSONObject("byProgram").put("p", JSONObject().put("explicitChoice", false))
        assertEquals("global", NativeTrainingModes.resolve(saved, "p").source)
        assertEquals("legacy", NativeTrainingModes.resolve(config(global = false), "p").source)
    }

    @Test fun malformedMatchingProgramOverrideFailsClosedNotGlobal() {
        val saved = config()
        val by = saved.getJSONObject("byProgram")
        listOf(JSONObject.NULL, "hit", JSONObject().put("trainingStyle", "hit"),
            JSONObject().put("explicitChoice", true).put("trainingStyle", "hit")
                .put("progressionMode", "unsafe")).forEach { bad ->
            by.put("p", bad)
            assertEquals("legacy", NativeTrainingModes.resolve(saved, "p").source)
            assertEquals("global", NativeTrainingModes.resolve(saved, "q").source)
        }
        assertEquals("legacy",
            NativeTrainingModes.resolve(config().put("byProgram", "invalid"), "q").source)
    }

    @Test fun readingNeverMutatesConfigOrUnknownFields() {
        val saved = config().put("opaqueExtension", JSONObject().put("future", 42))
            .put("revision", 7)
            .put("undo", JSONObject().put("previous", JSONObject().put("future", "retained")))
        val before = saved.toString()
        repeat(3) { NativeTrainingModes.resolve(saved, "p") }
        assertEquals(before, saved.toString())
    }
}
