package com.repforge.app;

import android.content.Context;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33, manifest=Config.NONE)
public class PhoneWorkoutPlansTest {
    private Context context;
    private PhoneWorkoutExecutionStore store;
    @Before public void open() throws Exception {
        context=RuntimeEnvironment.getApplication();context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);
        store=new PhoneWorkoutExecutionStore(context);store.prepare(frame(),0);
    }
    @After public void close(){store.close();context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);}
    private JSONObject frame() throws Exception {
        return new JSONObject("{\"active\":true,\"automaticEnabled\":true,\"timerOwner\":\"phone\",\"workoutId\":\"workout\",\"started\":\"workout\",\"generation\":1,\"executionRevision\":0,\"currentExercise\":0,\"restSeconds\":120,\"timerConfig\":{\"work\":30,\"rest\":15,\"rounds\":2,\"sound\":true,\"vibration\":true},\"exercises\":[{\"id\":\"press\",\"repUnit\":\"ism.\",\"sets\":[{\"setId\":\"a\",\"done\":false,\"reps\":\"\"},{\"setId\":\"b\",\"done\":false,\"reps\":\"\"}]}]}");
    }
    private JSONObject command(String id) throws Exception {
        return new JSONObject().put("commandId",id).put("action","completeSet").put("workoutId","workout")
                .put("generation",1).put("executionRevision",store.snapshot().getLong("executionRevision"))
                .put("exerciseId","press").put("setId","a").put("reps","7").put("sequence",4);
    }
    @Test public void recordingAndTwoMinuteRestCommitAndReplayTogether() throws Exception {
        JSONObject command=command("one"),event=store.record(command);
        JSONObject plan=event.getJSONObject("transition").getJSONObject("plan");
        assertTrue(plan.getBoolean("restOnly"));assertEquals(120,plan.getJSONArray("steps").getJSONObject(0).getInt("rest"));
        assertEquals("b",plan.getJSONObject("target").getString("setId"));
        assertEquals(4,store.snapshot().getLong("watchSequence"));
        store.close();store=new PhoneWorkoutExecutionStore(context);
        assertEquals("pending",store.snapshot().getJSONObject("transition").getString("state"));
        assertEquals("duplicate",store.record(command).getString("status"));
        assertEquals(plan.toString(),store.record(command).getJSONObject("transition").getJSONObject("plan").toString());
        assertEquals(1,store.pendingEvents("workout",1,0).length());
        assertTrue(store.markTransition(plan.getString("key"),"started"));
        assertFalse(store.markTransition("other-key","interrupted"));
        assertEquals("started",store.snapshot().getJSONObject("transition").getString("state"));
    }
    @Test public void repetitionToTimedExerciseKeepsLeadingRestAndBaseline() throws Exception {
        JSONObject frame=store.snapshot();frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").remove(1);
        frame.getJSONArray("exercises").put(new JSONObject("{\"id\":\"plank\",\"repUnit\":\"mp\",\"sets\":[{\"setId\":\"t\",\"done\":false,\"reps\":\"3\"}]}"));
        store.prepare(frame,1);JSONObject plan=store.record(command("timed-next")).getJSONObject("transition").getJSONObject("plan");
        assertTrue(plan.getBoolean("autoAdvance"));assertFalse(plan.getBoolean("restOnly"));assertEquals(120,plan.getInt("initialRest"));
        assertEquals(3,plan.getJSONArray("steps").getJSONObject(0).getInt("baseSeconds"));
        assertEquals(1,store.snapshot().getJSONObject("transition").getInt("currentExercise"));
        assertEquals("3",store.snapshot().getJSONArray("exercises").getJSONObject(1).getJSONArray("sets").getJSONObject(0).getString("reps"));
    }
    @Test public void lastRecordedSetDoesNotAutomaticallySaveWorkout() throws Exception {
        JSONObject frame=store.snapshot();frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").remove(1);store.prepare(frame,1);
        JSONObject transition=store.record(command("last")).getJSONObject("transition");
        assertEquals("complete",transition.getString("phase"));assertFalse(transition.has("plan"));assertTrue(store.snapshot().getBoolean("active"));
    }
    @Test public void zeroRestKeepsManualTargetAndNoInventedWork() throws Exception {
        JSONObject frame=store.snapshot();frame.put("restSeconds",0);store.prepare(frame,1);
        JSONObject transition=store.record(command("zero")).getJSONObject("transition"),plan=transition.getJSONObject("plan");
        assertEquals("ready",transition.getString("phase"));assertTrue(plan.getBoolean("restOnly"));
        assertEquals(0,plan.getJSONArray("steps").getJSONObject(0).getInt("rest"));
        assertFalse(store.snapshot().getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(1).getBoolean("done"));
    }
    @Test public void exerciseRestOverridesGlobalRest() throws Exception {
        JSONObject frame=store.snapshot();frame.getJSONArray("exercises").getJSONObject(0).put("restSeconds162",75);store.prepare(frame,1);
        JSONObject plan=store.record(command("prescribed-rest")).getJSONObject("transition").getJSONObject("plan");
        assertEquals(75,plan.getJSONArray("steps").getJSONObject(0).getInt("rest"));
    }
    @Test public void invalidPresetCannotReplacePreparedFrame() throws Exception {
        JSONObject frame=store.snapshot();String before=frame.toString();frame.getJSONObject("timerConfig").put("work",0);
        assertThrows(IllegalArgumentException.class,()->store.prepare(frame,1));assertEquals(before,store.snapshot().toString());
    }
    @Test public void explicitTimedStartHasDurableReceiptAndRejectsStaleOwner() throws Exception {
        JSONObject frame=store.snapshot();frame.getJSONArray("exercises").getJSONObject(0).put("repUnit","mp");store.prepare(frame,1);
        JSONObject start=command("start").put("action","automaticControl").put("value","start");
        JSONObject first=store.startTimeline(start);assertEquals("applied",first.getString("status"));assertEquals("startTimeline",first.getString("kind"));
        assertEquals("duplicate",store.startTimeline(start).getString("status"));
        assertEquals("rejected",store.startTimeline(new JSONObject(start.toString()).put("commandId","stale")).getString("status"));
        assertEquals(1,store.pendingEvents("workout",1,0).length());
        assertFalse(store.snapshot().getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getBoolean("done"));
    }
    @Test public void missingGpsOwnerDoesNotPartiallyRecordSet() throws Exception {
        JSONObject frame=store.snapshot();frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").remove(1);
        frame.getJSONArray("exercises").put(new JSONObject("{\"id\":\"run\",\"repUnit\":\"mp\",\"measurementType\":\"distance\",\"sets\":[{\"setId\":\"gps\",\"done\":false,\"reps\":\"\"}]}"));
        store.prepare(frame,1);String before=store.snapshot().toString();assertEquals("rejected",store.record(command("gps")).getString("status"));
        assertEquals(before,store.snapshot().toString());assertEquals(0,store.pendingEvents("workout",1,0).length());
    }
    @Test public void transactionFailureKeepsRecordingAndTimelineRetryable() throws Exception {
        String before=store.snapshot().toString();JSONObject command=command("failure");
        store.getWritableDatabase().execSQL("CREATE TRIGGER fail BEFORE INSERT ON execution BEGIN SELECT RAISE(ABORT, 'injected'); END");
        assertThrows(android.database.sqlite.SQLiteException.class,()->store.record(command));
        assertEquals(before,store.snapshot().toString());assertEquals(0,store.pendingEvents("workout",1,0).length());
        store.getWritableDatabase().execSQL("DROP TRIGGER fail");assertEquals("applied",store.record(command).getString("status"));
    }
}
