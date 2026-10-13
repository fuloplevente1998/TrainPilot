package com.repforge.app;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class PhoneWorkoutExecutionStoreTest {
    private Context context;
    private PhoneWorkoutExecutionStore store;

    @Before public void open() throws Exception {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);
        store = new PhoneWorkoutExecutionStore(context);
        store.prepare(frame("workout", 1), 0);
    }
    @After public void close() {
        store.close(); context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);
    }

    private JSONObject frame(String id, long generation) throws Exception {
        JSONObject frame = new JSONObject("{\"active\":true,\"timerOwner\":\"phone\",\"executionRevision\":0,"
                + "\"currentExercise\":0,\"notes\":\"Keep my draft\",\"exercises\":["
                + "{\"id\":\"squat\",\"repUnit\":\"ism.\",\"effort\":\"pain\",\"sets\":["
                + "{\"setId\":\"a\",\"done\":false,\"reps\":\"\",\"weight\":5},"
                + "{\"setId\":\"b\",\"done\":false,\"reps\":\"\",\"weight\":5}]},"
                + "{\"id\":\"press\",\"repUnit\":\"ism.\",\"sets\":["
                + "{\"setId\":\"c\",\"done\":false,\"reps\":\"\",\"weight\":8}]}]}");
        return frame.put("workoutId", id).put("generation", generation);
    }
    private JSONObject command(String commandId, String setId) throws Exception {
        JSONObject current = store.snapshot();
        return new JSONObject().put("commandId", commandId).put("action", "completeSet")
                .put("workoutId", current.getString("workoutId")).put("generation", current.getLong("generation"))
                .put("executionRevision", current.getLong("executionRevision"))
                .put("exerciseId", "c".equals(setId) ? "press" : "squat").put("setId", setId)
                .put("reps", "5").put("weight", "5,5");
    }
    private JSONObject set(JSONObject frame, int exercise, int set) throws Exception {
        return frame.getJSONArray("exercises").getJSONObject(exercise).getJSONArray("sets").getJSONObject(set);
    }

    @Test public void stableIdWinsOverObsoleteIndexAndPreservesDraft() throws Exception {
        JSONObject command = command("record-b", "b").put("setIndex", 0).put("reps", "005");
        String input = command.toString();
        JSONObject result = store.record(command), current = store.snapshot();
        assertEquals("applied", result.getString("status"));
        assertEquals(input, command.toString());
        assertFalse(set(current, 0, 0).getBoolean("done"));
        assertTrue(set(current, 0, 1).getBoolean("done"));
        assertEquals("5", set(current, 0, 1).getString("reps"));
        assertEquals(5.5, result.getJSONObject("values").getDouble("weight"), 0);
        assertEquals("Keep my draft", current.getString("notes"));
        assertEquals("pain", current.getJSONArray("exercises").getJSONObject(0).getString("effort"));
        assertFalse(set(current, 1, 0).getBoolean("done"));
    }

    @Test public void receiptSurvivesReopenBeforeWearAcknowledgement() throws Exception {
        JSONObject command = command("replay", "a"), first = store.record(command);
        String snapshot = store.snapshot().toString();
        store.close(); store = new PhoneWorkoutExecutionStore(context);
        JSONObject replay = store.record(new JSONObject(command.toString()));
        assertEquals("duplicate", replay.getString("status"));
        assertEquals(first.getLong("sequence"), replay.getLong("sequence"));
        assertEquals(first.getLong("recordedAt"), replay.getLong("recordedAt"));
        assertEquals(snapshot, store.snapshot().toString());
        assertEquals(1, store.pendingEvents("workout", 1, 0).length());
    }

    @Test public void sqlFailureRollsBackMutationAndReplayReceiptTogether() throws Exception {
        JSONObject command = command("retry", "a"); String before = store.snapshot().toString();
        store.getWritableDatabase().execSQL("CREATE TRIGGER fail_execution BEFORE INSERT ON execution "
                + "BEGIN SELECT RAISE(ABORT,'injected write failure'); END");
        assertThrows(SQLiteException.class, () -> store.record(command));
        assertEquals(before, store.snapshot().toString());
        assertEquals(0, store.pendingEvents("workout", 1, 0).length());
        store.getWritableDatabase().execSQL("DROP TRIGGER fail_execution");
        assertEquals("applied", store.record(command).getString("status"));
        assertEquals(1, store.pendingEvents("workout", 1, 0).length());
    }

    @Test public void reusedCommandIdCannotCarryDifferentRecordedValues() throws Exception {
        JSONObject command = command("collision", "a"); store.record(command);
        String before = store.snapshot().toString();
        assertEquals("commandIdentityCollision", store.record(command.put("reps", "7")).getString("reason"));
        assertEquals(before, store.snapshot().toString());
    }

    @Test public void newCommandCannotRecordCompletedSetAgain() throws Exception {
        store.record(command("first", "a")); String before = store.snapshot().toString();
        assertEquals("alreadyRecorded", store.record(command("second", "a")).getString("reason"));
        assertEquals(before, store.snapshot().toString());
    }

    @Test public void stalePhoneCannotOverwriteNativeRecording() throws Exception {
        JSONObject stale = store.snapshot(); long revision = stale.getLong("executionRevision");
        store.record(command("native", "a")); String before = store.snapshot().toString();
        assertThrows(IllegalArgumentException.class, () -> store.prepare(stale, revision));
        assertThrows(IllegalStateException.class, () -> store.prepare(store.snapshot(), revision + 1));
        assertEquals(before, store.snapshot().toString());
    }

    @Test public void importingEventsDoesNotDeleteReplayReceipts() throws Exception {
        JSONObject command = command("kept-receipt", "a"), result = store.record(command);
        assertTrue(store.acknowledgeEvents("workout", 1, result.getLong("sequence")));
        assertEquals(0, store.pendingEvents("workout", 1, 0).length());
        assertEquals("duplicate", store.record(command).getString("status"));
        assertEquals(0, store.pendingEvents("workout", 1, 0).length());
    }

    @Test public void partialEventImportLeavesNewerRecordingPending() throws Exception {
        JSONObject first = store.record(command("one", "a")), second = store.record(command("two", "b"));
        assertFalse(store.acknowledgeEvents("other-workout", 1, second.getLong("sequence")));
        assertFalse(store.acknowledgeEvents("workout", 2, second.getLong("sequence")));
        assertTrue(store.acknowledgeEvents("workout", 1, first.getLong("sequence")));
        JSONArray pending = store.pendingEvents("workout", 1, first.getLong("sequence"));
        assertEquals(1, pending.length()); assertEquals("two", pending.getJSONObject(0).getString("commandId"));
        assertFalse(store.closeExecution("workout", 1, store.snapshot().getLong("executionRevision")));
    }

    @Test public void reconciledSnapshotCannotUndoOrDropRecordedSet() throws Exception {
        JSONObject result = store.record(command("record", "a"));
        store.acknowledgeEvents("workout", 1, result.getLong("sequence"));
        JSONObject incoming = store.snapshot(); long revision = incoming.getLong("executionRevision");
        set(incoming, 0, 0).put("done", false);
        assertThrows(IllegalArgumentException.class, () -> store.prepare(incoming, revision));
        incoming.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").remove(0);
        assertThrows(IllegalArgumentException.class, () -> store.prepare(incoming, revision));
        assertTrue(set(store.snapshot(), 0, 0).getBoolean("done"));
    }

    @Test public void validReconciliationPreservesEditingAndAdvancesRevision() throws Exception {
        JSONObject result = store.record(command("record", "a"));
        store.acknowledgeEvents("workout", 1, result.getLong("sequence"));
        JSONObject incoming = store.snapshot(); long revision = incoming.getLong("executionRevision");
        incoming.put("notes", "My new note"); set(incoming, 0, 1).put("weight", 9);
        JSONObject saved = store.prepare(incoming, revision);
        assertEquals(revision + 1, saved.getLong("executionRevision"));
        assertEquals("My new note", saved.getString("notes")); assertEquals(9, set(saved, 0, 1).getInt("weight"));
        assertTrue(set(saved, 0, 0).getBoolean("done"));
    }

    @Test public void restoreLockSurvivesReopenAndBlocksNewMutations() throws Exception {
        JSONObject command = command("locked", "a"); String before = store.snapshot().toString();
        store.setRestoreBlocked(true); store.close(); store = new PhoneWorkoutExecutionStore(context);
        assertEquals("restoreInProgress", store.record(command).getString("reason"));
        assertThrows(IllegalStateException.class, () -> store.prepare(store.snapshot(), 1));
        assertFalse(store.closeExecution("workout", 1, 1)); assertEquals(before, store.snapshot().toString());
        store.setRestoreBlocked(false); assertEquals("applied", store.record(command).getString("status"));
    }

    @Test public void closedExecutionCannotBeReopenedByOldSnapshot() throws Exception {
        JSONObject old = store.snapshot(), late = command("late", "a");
        assertTrue(store.closeExecution("workout", 1, 1));
        assertEquals("inactiveOrOtherOwner", store.record(late).getString("reason"));
        assertThrows(IllegalArgumentException.class, () -> store.prepare(old, 1));
        assertThrows(IllegalArgumentException.class, () -> store.prepare(frame("workout", 2), 0));
        assertThrows(IllegalArgumentException.class, () -> store.prepare(frame("new-workout", 1), 0));
        assertEquals("new-workout", store.prepare(frame("new-workout", 2), 0).getString("workoutId"));
        assertEquals("executionIdentityChanged", store.record(late).getString("reason"));
    }

    @Test public void malformedStaleAndUnsupportedCommandsHaveNoSideEffects() throws Exception {
        Object[][] invalid = {{"workoutId", "other"}, {"generation", 2}, {"executionRevision", 0},
                {"generation", "1"}, {"exerciseId", "press"}, {"setId", "missing"},
                {"reps", "0"}, {"reps", "8–12"}, {"reps", "999999999999999999999"},
                {"weight", "NaN"}, {"weight", -1}, {"action", "finishWorkout"}};
        String before = store.snapshot().toString();
        for (Object[] value : invalid) {
            JSONObject command = command("invalid", "a").put((String) value[0], value[1]);
            assertEquals(String.valueOf(value[0]), "rejected", store.record(command).getString("status"));
            assertEquals(before, store.snapshot().toString());
        }
        JSONObject legacy = command("legacy", "a"); legacy.remove("executionRevision");
        assertEquals("rejected", store.record(legacy).getString("status"));
        assertEquals(0, store.pendingEvents("workout", 1, 0).length());
    }

    @Test public void duplicateSetIdentityIsRejectedBeforePublishingFrame() throws Exception {
        JSONObject incoming = store.snapshot(); set(incoming, 1, 0).put("setId", "a");
        assertThrows(IllegalArgumentException.class, () -> store.prepare(incoming, 1));
        assertEquals("c", set(store.snapshot(), 1, 0).getString("setId"));
    }

    @Test public void timedAndDistanceOwnersRemainOnExistingPath() throws Exception {
        JSONObject incoming = store.snapshot(); incoming.getJSONArray("exercises").getJSONObject(0).put("repUnit", "mp");
        store.prepare(incoming, 1);
        assertEquals("unsupportedMeasurement", store.record(command("timed", "a")).getString("reason"));
        incoming = store.snapshot(); incoming.getJSONArray("exercises").getJSONObject(0)
                .put("repUnit", "ism.").put("measurementType", "distance");
        store.prepare(incoming, 2);
        assertEquals("unsupportedMeasurement", store.record(command("distance", "a")).getString("reason"));
        assertEquals(0, store.pendingEvents("workout", 1, 0).length());
    }

    @Test public void preparedSnapshotCannotChangeRecordedRepetitionsOrWeight() throws Exception {
        JSONObject result = store.record(command("actual-values", "a"));
        store.acknowledgeEvents("workout", 1, result.getLong("sequence"));
        JSONObject wrongReps = store.snapshot(), wrongWeight = store.snapshot();
        set(wrongReps, 0, 0).put("reps", "8"); set(wrongWeight, 0, 0).put("weight", 10);
        assertThrows(IllegalArgumentException.class, () -> store.prepare(wrongReps, 2));
        assertThrows(IllegalArgumentException.class, () -> store.prepare(wrongWeight, 2));
        assertEquals("5", set(store.snapshot(), 0, 0).getString("reps"));
        assertEquals(5.5, set(store.snapshot(), 0, 0).getDouble("weight"), 0);
    }

    @Test public void concurrentReplayRecordsExactlyOnce() throws Exception {
        JSONObject command = command("simultaneous", "a");
        CyclicBarrier start = new CyclicBarrier(2); ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            Future<JSONObject> first = threads.submit(() -> { start.await(); return store.record(new JSONObject(command.toString())); });
            Future<JSONObject> second = threads.submit(() -> { start.await(); return store.record(new JSONObject(command.toString())); });
            String a = first.get().getString("status"), b = second.get().getString("status");
            assertTrue(("applied".equals(a) && "duplicate".equals(b)) || ("duplicate".equals(a) && "applied".equals(b)));
            assertEquals(1, store.pendingEvents("workout", 1, 0).length());
            assertEquals(2, store.snapshot().getLong("executionRevision"));
        } finally { threads.shutdownNow(); }
    }

    @Test public void actualZeroWeightAndUnspecifiedWeightRemainDistinct() throws Exception {
        JSONObject zero = store.record(command("bodyweight", "a").put("weight", 0));
        assertEquals(0, zero.getJSONObject("values").getDouble("weight"), 0);
        JSONObject noWeight = command("no-change", "b"); noWeight.remove("weight");
        JSONObject second = store.record(noWeight);
        assertFalse(second.getJSONObject("values").has("weight"));
        assertEquals(5, set(store.snapshot(), 0, 1).getInt("weight"));
    }
}
