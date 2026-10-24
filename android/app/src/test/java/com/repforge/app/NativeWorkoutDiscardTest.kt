package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeWorkoutDiscardTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private val dispatcher = StandardTestDispatcher()
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    private fun store() = PhoneWorkoutExecution.store(context)
    @Before fun open() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        for (name in listOf("trainpilot_wear_sync", "trainpilot_data_replacement"))
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        NativeReplacementJournal(context).clear()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
    }
    @After fun close() {
        db.close()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        Dispatchers.resetMain()
    }
    private fun prepare(): JSONObject {
        val draft = JSONObject("""{"active":true,"timerOwner":"phone","workoutId":"discard-me",
            "id":"discard-me","syncId":"discard-me","generation":1,"executionRevision":0,
            "started":"2026-10-24T10:00:00Z","currentExercise":0,
            "exercises":[{"id":"press","repUnit":"ism.","loadType":"dumbbell",
                "sets":[{"setId":"set-a","done":false,"reps":"8","weight":10}]}]}""")
        store().prepare(draft, 0)
        return ActiveWorkoutStore.saveOwned(context, draft)
    }
    private suspend fun reject(operation: suspend () -> Unit) {
        var error: Exception? = null
        try { operation() } catch (e: Exception) { error = e }
        assertNotNull("Expected rejection without success acknowledgement", error)
    }
    private fun finalizer() = TrainPilotWorkoutFinalization(context, db)
    private fun assertRetryable() {
        val current = ActiveWorkoutStore.current(context)!!
        assertTrue(current.getBoolean("active"))
        assertTrue(current.getBoolean("workoutPaused"))
        assertTrue(current.getBoolean("discardPending"))
        assertTrue(store().commandsSuspended())
    }
    @Test fun explicitDiscardAbandonsPendingRecordingOnlyAfterDurableTombstoneAndNativeClose() = runTest(dispatcher) {
        val input = prepare()
        store().record(JSONObject().put("commandId", "record").put("action", "completeSet")
            .put("workoutId", "discard-me").put("generation", 1).put("executionRevision", 0)
            .put("exerciseId", "press").put("setId", "set-a").put("weight", 12).put("reps", "9"))
        assertEquals(1, store().pendingEvents("discard-me", 1, 0).length())
        val closed = finalizer().discardWorkout(input)
        assertFalse(closed.getBoolean("active"))
        assertFalse(store().snapshot()!!.getBoolean("active"))
        assertFalse(store().commandsSuspended())
        assertEquals(0, db.workoutHistoryDao().getWorkoutCount())
        assertTrue(db.workoutHistoryDao().isTombstoned("discard-me", JSONObject.quote("discard-me")))
    }
    @Test fun roomFailureLeavesDurablePausedDiscardAndFinishCannotSaveItBeforeRetry() = runTest(dispatcher) {
        val input = prepare()
        db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER disk_full BEFORE INSERT ON history_tombstones
            BEGIN SELECT RAISE(ABORT, 'injected discard failure'); END""")
        reject { finalizer().discardWorkout(input) }
        assertRetryable()
        assertTrue(store().snapshot()!!.getBoolean("active"))
        assertFalse(db.workoutHistoryDao().isTombstoned("discard-me", JSONObject.quote("discard-me")))
        reject { finalizer().finishWorkout(input) }
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER disk_full")
        finalizer().discardWorkout(input)
        assertFalse(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
        assertEquals(1, db.workoutHistoryDao().getAllTombstones().size)
        assertEquals(0, db.workoutHistoryDao().getWorkoutCount())
    }
    @Test fun nativeCloseFailureSurvivesExecutionStoreReopenAndRetryClosesBeforeSuccess() = runTest(dispatcher) {
        val input = prepare()
        store().writableDatabase.execSQL("""CREATE TRIGGER close_full BEFORE INSERT ON execution
            WHEN instr(NEW.snapshot, '"active":false') > 0
            BEGIN SELECT RAISE(ABORT, 'injected native close failure'); END""")
        reject { finalizer().discardWorkout(input) }
        assertRetryable()
        assertTrue(db.workoutHistoryDao().isTombstoned("discard-me", JSONObject.quote("discard-me")))
        store().close(); field.set(null, null)
        assertRetryable()
        store().writableDatabase.execSQL("DROP TRIGGER close_full")
        finalizer().discardWorkout(ActiveWorkoutStore.current(context)!!)
        assertFalse(store().snapshot()!!.getBoolean("active"))
        assertFalse(store().commandsSuspended())
    }
    @Test fun exactImportedDraftRetiresTogetherWithTombstoneAndKeepsArchive() = runTest(dispatcher) {
        val raw = "{\"source\":\"imported draft\"}"
        db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("draft", raw, 1))
        val input = prepare().put("legacyDraftSha256", NativeLegacyDraft.hash(raw))
        ActiveWorkoutStore.saveOwned(context, input)
        finalizer().discardWorkout(input)
        assertNull(db.keyValueDocumentDao().getDocument("draft"))
        assertEquals(raw, db.keyValueDocumentDao().getDocument("legacyDraftArchive|" + NativeLegacyDraft.hash(raw))!!.json)
    }
    @Test fun retirementFailureRollsBackTombstoneAndKeepsSourceForExplicitRetry() = runTest(dispatcher) {
        val raw = "{\"source\":\"imported draft\"}"
        db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("draft", raw, 1))
        val input = prepare().put("legacyDraftSha256", NativeLegacyDraft.hash(raw))
        ActiveWorkoutStore.saveOwned(context, input)
        // The table name comes from the actual Room entity, not a mocked retirement callback.
        db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER archive_full BEFORE DELETE ON kv_documents
            WHEN OLD.doc_key = 'draft' BEGIN SELECT RAISE(ABORT, 'injected archive failure'); END""")
        reject { finalizer().discardWorkout(input) }
        assertRetryable()
        assertFalse(db.workoutHistoryDao().isTombstoned("discard-me", JSONObject.quote("discard-me")))
        assertEquals(raw, db.keyValueDocumentDao().getDocument("draft")!!.json)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER archive_full")
        finalizer().discardWorkout(input)
        assertNull(db.keyValueDocumentDao().getDocument("draft"))
    }
    @Test fun staleGenerationAndWatchOwnerAreRejectedWithoutDeletingAnything() = runTest(dispatcher) {
        val input = prepare()
        reject { finalizer().discardWorkout(JSONObject(input.toString()).put("generation", 2)) }
        ActiveWorkoutStore.saveOwned(context, JSONObject(input.toString()).put("timerOwner", "watch"))
        reject { finalizer().discardWorkout(input) }
        assertFalse(ActiveWorkoutStore.current(context)!!.optBoolean("discardPending"))
        assertEquals(0, db.workoutHistoryDao().getAllTombstones().size)
        assertTrue(store().snapshot()!!.getBoolean("active"))
    }
    @Test fun pendingSaveCannotBeConvertedIntoDiscard() = runTest(dispatcher) {
        val input = prepare()
        ActiveWorkoutStore.saveFinalizing(context, JSONObject(input.toString()).put("finalizationPending", true))
        reject { finalizer().discardWorkout(input) }
        assertEquals(0, db.workoutHistoryDao().getAllTombstones().size)
        assertFalse(ActiveWorkoutStore.current(context)!!.optBoolean("discardPending"))
    }
    @Test fun replacementFenceRejectsDiscardWithoutPublishingOrWriting() = runTest(dispatcher) {
        val input = prepare()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().putBoolean("blocked", true).commit()
        reject { finalizer().discardWorkout(input) }
        assertFalse(ActiveWorkoutStore.current(context)!!.optBoolean("discardPending"))
        assertEquals(0, db.workoutHistoryDao().getAllTombstones().size)
    }
}
