package com.repforge.app

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Robolectric
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeWorkoutEditingTest {
    private lateinit var context: Context
    private var controller: ServiceController<IntervalTimerService>? = null
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }

    @Before fun open() {
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        NativeWorkoutSession.installPrepared(context, JSONObject("""{
            "active":true,"timerOwner":"phone","workoutId":"edit-me","id":"edit-me","syncId":"edit-me",
            "started":"2026-10-23T10:00:00Z","currentExercise":0,"notes":"keep my note",
            "automaticEnabled":true,"timerConfig":{"work":30,"rest":90,"rounds":1},"restSeconds":90,
            "exercises":[{"id":"press","loadType":"dumbbell","repUnit":"ism.","sets":[
              {"setId":"set-a","done":false,"weight":10,"reps":"8"},
              {"setId":"set-b","done":false,"weight":10,"reps":"8"}]}]
        }"""))
    }

    @After fun close() {
        controller?.destroy()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
    }

    private fun first(snapshot: JSONObject) = snapshot.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0)
    private fun rawRecord(id: String = "raw-a", target: String = "set-a"): JSONObject {
        val execution = PhoneWorkoutExecution.store(context)
        val frame = execution.snapshot()!!
        return execution.record(JSONObject().put("commandId", id).put("action", "completeSet")
            .put("workoutId", "edit-me").put("generation", frame.getLong("generation"))
            .put("executionRevision", frame.getLong("executionRevision"))
            .put("exerciseId", "press").put("setId", target).put("reps", "9").put("weight", 12.5))
    }

    @Test fun editingThenNativeRecordThenUndoUsesOneSetStateAndRetainsUserFields() {
        armController()
        val edit = ActiveWorkoutStore.current(context)!!
        first(edit).put("weight", 12.5).put("reps", "9")
        NativeWorkoutEditing.commitEdit(context, edit)
        val native = PhoneWorkoutExecution.store(context).snapshot()!!
        assertEquals("9", first(native).getString("reps"))
        assertEquals(12.5, first(native).getDouble("weight"), 0.0)
        NativeWorkoutEditing.completeStrengthSet(context, "press", "set-a")
        val completed = ActiveWorkoutStore.current(context)!!
        assertTrue(first(completed).getBoolean("done"))
        assertEquals("keep my note", completed.getString("notes"))
        val recordId = completed.getJSONObject("lastRecord188").getString("recordId")
        NativeWorkoutEditing.undo(context, recordId)
        val undone = ActiveWorkoutStore.current(context)!!
        assertFalse(first(undone).getBoolean("done"))
        assertFalse(first(PhoneWorkoutExecution.store(context).snapshot()!!).getBoolean("done"))
        assertEquals("9", first(undone).getString("reps"))
        assertEquals(0, PhoneWorkoutExecution.store(context).pendingEvents("edit-me", undone.getLong("generation"), 0).length())
    }

    @Test fun staleUiEditCannotOverwritePendingNativeRecording() {
        val stale = ActiveWorkoutStore.current(context)!!
        first(stale).put("weight", 99)
        rawRecord()
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.commitEdit(context, stale) }
        val projected = NativeWorkoutEditing.project(context)
        assertTrue(first(projected).getBoolean("done"))
        assertEquals(12.5, first(projected).getDouble("weight"), 0.0)
        assertEquals("9", first(projected).getString("reps"))
    }

    @Test fun failedProjectionWriteCannotAcknowledgeRecordingAndRetryRestoresFullState() {
        rawRecord()
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.project(failingSnapshotContext()) }
        val store = PhoneWorkoutExecution.store(context)
        assertEquals(1, store.pendingEvents("edit-me", store.snapshot()!!.getLong("generation"), 0).length())
        assertFalse(first(ActiveWorkoutStore.current(context)!!).getBoolean("done"))
        val saved = NativeWorkoutEditing.project(context)
        assertTrue(first(saved).getBoolean("done"))
        assertEquals(0, store.pendingEvents("edit-me", saved.getLong("generation"), 0).length())
    }

    @Test fun nativeEditSurvivesFailedUiPublicationWithoutReapplyingOldUiValues() {
        val edit = ActiveWorkoutStore.current(context)!!
        first(edit).put("weight", 20).put("reps", "12")
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.commitEdit(failingSnapshotContext(), edit) }
        assertEquals(10.0, first(ActiveWorkoutStore.current(context)!!).getDouble("weight"), 0.0)
        val recovered = NativeWorkoutEditing.project(context)
        assertEquals(20.0, first(recovered).getDouble("weight"), 0.0)
        assertEquals("12", first(recovered).getString("reps"))
    }

    @Test fun oldProjectionCursorCannotAcknowledgeANewerRecording() {
        rawRecord()
        val store = PhoneWorkoutExecution.store(context)
        val old = store.projection()
        rawRecord("raw-b", "set-b")
        assertTrue(store.acknowledgeEvents("edit-me", old.getJSONObject("snapshot").getLong("generation"), old.getLong("sequence")))
        val pending = store.pendingEvents("edit-me", old.getJSONObject("snapshot").getLong("generation"), 0)
        assertEquals(1, pending.length())
        assertEquals("raw-b", pending.getJSONObject(0).getString("commandId"))
        val saved = NativeWorkoutEditing.project(context)
        assertEquals(0, store.pendingEvents("edit-me", saved.getLong("generation"), 0).length())
    }

    @Test fun frozenOrWatchOwnedDraftRejectsEditorChanges() {
        val edit = ActiveWorkoutStore.current(context)!!
        ActiveWorkoutStore.saveOwned(context, JSONObject(edit.toString()).put("timerOwner", "watch"))
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.commitEdit(context, edit) }
        ActiveWorkoutStore.saveOwned(context, JSONObject(edit.toString()).put("finalizationPending", true))
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.project(context) }
        assertFalse(first(PhoneWorkoutExecution.store(context).snapshot()!!).getBoolean("done"))
    }

    @Test fun alreadyClosedProjectionCannotResurrectAnUnretiredNativeFrame() {
        ActiveWorkoutStore.clear(context)
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.project(context) }
        assertFalse(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
    }

    @Test fun recordingAndUndoCarryTheInstalledOwnershipEpoch() {
        val store = PhoneWorkoutExecution.store(context)
        val frame = store.snapshot()!!.put("ownershipProtocol193", 1).put("ownerEpoch193", 7)
            .put("handoff193", JSONObject().put("id", "returned").put("state", "committed"))
        val installed = store.prepare(frame, frame.getLong("executionRevision"))
        ActiveWorkoutStore.saveOwned(context, installed)
        // Fixture of a durably committed return; handshake transitions have separate platform tests.
        File(context.noBackupFilesDir, "ownership193.json").writeText(JSONObject().put("snapshot", installed).toString())
        armController()
        NativeWorkoutEditing.completeStrengthSet(context, "press", "set-a")
        val current = ActiveWorkoutStore.current(context)!!
        assertEquals(7L, current.getLong("ownerEpoch193"))
        assertTrue(first(current).getBoolean("done"))
        NativeWorkoutEditing.undo(context, current.getJSONObject("lastRecord188").getString("recordId"))
        assertFalse(first(ActiveWorkoutStore.current(context)!!).getBoolean("done"))
    }

    private fun armController() {
        val frame = PhoneWorkoutExecution.store(context).snapshot()!!
        controller = Robolectric.buildService(IntervalTimerService::class.java).create()
        controller!!.get().onStartCommand(Intent(context, IntervalTimerService::class.java).apply {
            action = IntervalTimerService.ARM
            putExtra("workoutId", frame.getString("workoutId"))
            putExtra("generation", frame.getLong("generation"))
            putExtra("language", "hu")
        }, 0, 1)
        assertTrue(IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation")))
    }

    private fun failingSnapshotContext(): Context = object : ContextWrapper(context) {
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            val preferences = super.getSharedPreferences(name, mode)
            if (name != "trainpilot_wear_sync") return preferences
            return Proxy.newProxyInstance(SharedPreferences::class.java.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
                if (method.name != "edit") method.invoke(preferences, *(args ?: emptyArray()))
                else {
                    val editor = preferences.edit()
                    var proxy: SharedPreferences.Editor? = null
                    proxy = Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { _, editMethod, editArgs ->
                        when (editMethod.name) {
                            "commit" -> false
                            "apply" -> error("Unexpected asynchronous snapshot write")
                            else -> editMethod.invoke(editor, *(editArgs ?: emptyArray())).let { if (it is SharedPreferences.Editor) proxy else it }
                        }
                    } as SharedPreferences.Editor
                    proxy
                }
            } as SharedPreferences
        }
    }
}
