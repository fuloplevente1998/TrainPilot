package com.repforge.app

import com.repforge.app.data.store.NativeTrainingModeChange
import com.repforge.app.data.store.NativeTrainingModes
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeTrainingModeChangeTest {
    private fun selected() = JSONObject()
        .put("schemaVersion", 1).put("explicitChoice", true)
        .put("defaults", JSONObject().put("trainingStyle", "normal").put("progressionMode", "double"))
        .put("byProgram", JSONObject())
        .put("revision", 9).put("opaqueFuture", JSONObject().put("keep", "yes"))

    @Test fun globalPreviewHasNoMutationAndApprovalProducesBoundedUndo() {
        val initial = selected().put("undo", JSONObject().put("previous", JSONObject.NULL))
        val raw = initial.toString()
        val preview = NativeTrainingModeChange.prepare(raw, "p", "global", null, "hit", "linear")
        assertEquals("normal", preview.before.trainingStyle)
        assertEquals("hit", preview.after.trainingStyle)
        assertEquals(raw, initial.toString())
        val next = NativeTrainingModeChange.next(preview, "2026-10-25T10:00:00Z")
        assertEquals("hit", next.getJSONObject("defaults").getString("trainingStyle"))
        assertEquals("linear", next.getJSONObject("defaults").getString("progressionMode"))
        assertEquals(10, next.getInt("revision"))
        assertEquals("yes", next.getJSONObject("opaqueFuture").getString("keep"))
        assertFalse(next.getJSONObject("undo").getJSONObject("previous").has("undo"))
        assertEquals("normal", NativeTrainingModeChange.undoValue(next.toString(), "global", null)
            ?.previous?.getJSONObject("defaults")?.getString("trainingStyle"))
        assertNull(NativeTrainingModeChange.undoValue(next.toString(), "program", "p"))
    }

    @Test fun approvedProgramScopeDoesNotReplaceGlobalDefaultOrOtherPrograms() {
        val initial = selected()
        initial.getJSONObject("byProgram").put("q", JSONObject()
            .put("trainingStyle", "hit").put("progressionMode", "rir").put("explicitChoice", true))
        val changed = NativeTrainingModeChange.next(
            NativeTrainingModeChange.prepare(initial.toString(), "p", "program", "p", "hit", "linear"))
        assertEquals("double", changed.getJSONObject("defaults").getString("progressionMode"))
        assertEquals("rir", changed.getJSONObject("byProgram").getJSONObject("q").getString("progressionMode"))
        assertEquals("linear", changed.getJSONObject("byProgram").getJSONObject("p").getString("progressionMode"))
        assertEquals("program", NativeTrainingModes.resolve(changed, "p").source)
        assertEquals("global", NativeTrainingModes.resolve(changed, "other").source)
        assertTrue(NativeTrainingModes.validForWrite(changed))
    }

    @Test fun legacyPreviewUndoDeletesOnlyModeFieldAndOldUndoCannotRepeat() {
        val preview = NativeTrainingModeChange.prepare(null, "p", "global", null, "normal", "rir")
        val next = NativeTrainingModeChange.next(preview)
        assertTrue(NativeTrainingModes.validForWrite(next))
        assertNull(NativeTrainingModeChange.undoValue(next.toString(), "global", null)?.previous)
        assertNull(NativeTrainingModeChange.undoValue(null, "global", null))
        assertNull(NativeTrainingModeChange.undoValue(selected().toString(), "global", null))
    }

    @Test fun invalidOrInactiveProgramAndUnknownOptionsAreRejected() {
        val raw = selected().toString()
        for (arguments in listOf(Triple("program", "other", "hit"), Triple("program", "", "hit"),
            Triple("global", null, "force"))) {
            var threw = false
            try { NativeTrainingModeChange.prepare(raw, "p", arguments.first,
                arguments.second, arguments.third, "linear") }
            catch (_: IllegalArgumentException) { threw = true }
            assertTrue(threw)
        }
    }

    @Test fun malformedUndoAndScopeAreNotApplied() {
        val raw = selected().put("undo", JSONObject().put("scope", "global").put("previous", "corrupt")).toString()
        assertNull(NativeTrainingModeChange.undoValue(raw, "global", null))
        val noPrevious = selected().put("undo", JSONObject().put("scope", "global")).toString()
        assertNull(NativeTrainingModeChange.undoValue(noPrevious, "global", null))
    }
}
