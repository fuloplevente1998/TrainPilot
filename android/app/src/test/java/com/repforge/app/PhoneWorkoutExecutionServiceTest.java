package com.repforge.app;

import android.content.Context;
import android.content.Intent;
import java.lang.reflect.Field;
import java.time.Duration;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33, manifest=Config.NONE)
public class PhoneWorkoutExecutionServiceTest {
    private Context context;
    private PhoneWorkoutExecutionStore store;
    private ServiceController<IntervalTimerService> controller;
    @Before public void open() throws Exception {
        context=RuntimeEnvironment.getApplication();context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);
        Field field=PhoneWorkoutExecution.class.getDeclaredField("store");field.setAccessible(true);field.set(null,null);
        store=PhoneWorkoutExecution.store(context);
        store.prepare(new JSONObject("{\"active\":true,\"automaticEnabled\":true,\"timerOwner\":\"phone\",\"workoutId\":\"workout\",\"started\":\"workout\",\"generation\":1,\"executionRevision\":0,\"currentExercise\":0,\"restSeconds\":120,\"timerConfig\":{\"work\":5,\"rest\":3,\"rounds\":2,\"sound\":false,\"vibration\":false},\"exercises\":[{\"id\":\"press\",\"repUnit\":\"ism.\",\"sets\":[{\"setId\":\"a\",\"done\":false,\"reps\":\"\"},{\"setId\":\"b\",\"done\":false,\"reps\":\"\"}]}]}"),0);
    }
    @After public void close() throws Exception {
        if(controller!=null)controller.destroy();store.close();context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);
        Field field=PhoneWorkoutExecution.class.getDeclaredField("store");field.setAccessible(true);field.set(null,null);
    }
    private void arm(){
        controller=Robolectric.buildService(IntervalTimerService.class).create();
        controller.get().onStartCommand(new Intent(context,IntervalTimerService.class).setAction(IntervalTimerService.ARM)
                .putExtra("workoutId","workout").putExtra("generation",1L).putExtra("language","hu"),0,1);
        assertTrue(IntervalTimerService.armed("workout",1));
    }
    private JSONObject command() throws Exception {
        return new JSONObject().put("nativeExecutionProtocol",2).put("commandId","record-a").put("action","completeSet")
                .put("workoutId","workout").put("generation",1).put("executionRevision",store.snapshot().getLong("executionRevision"))
                .put("exerciseId","press").put("setId","a").put("reps","7");
    }
    @Test public void sleepingWebViewRecordingStartsRestAndReplayCannotResetClock() throws Exception {
        arm();JSONObject command=command();assertEquals("applied",PhoneWorkoutExecution.execute(context,command).getString("status"));
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(30));JSONObject timer=IntervalTimerService.snapshot(context);
        assertTrue(timer.getBoolean("running"));assertEquals("rest",timer.getString("phase"));assertEquals(90000,timer.getLong("remainingMs"));
        assertEquals("duplicate",PhoneWorkoutExecution.execute(context,command).getString("status"));
        assertEquals(90000,IntervalTimerService.snapshot(context).getLong("remainingMs"));
        assertEquals(1,store.pendingEvents("workout",1,0).length());
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(90));assertEquals("complete",IntervalTimerService.snapshot(context).getString("phase"));
        assertTrue(IntervalTimerService.armed("workout",1)); // Ready for the next explicit repetition recording.
        assertFalse("Natural rest completion also returns to recording",PhoneWorkoutExecution.display(context).getJSONObject("automation").getBoolean("paused"));
    }
    @Test public void skippedRepetitionRestUnlocksWatchRecordingWithoutPhoneConfirmation() throws Exception { skippedRest(false); }
    @Test public void skippedRestBetweenExercisesUnlocksTheNextExerciseWithoutPhoneConfirmation() throws Exception { skippedRest(true); }
    private void skippedRest(boolean nextExercise) throws Exception {
        if(nextExercise) {
            JSONObject frame=store.snapshot(),first=frame.getJSONArray("exercises").getJSONObject(0);
            JSONObject second=new JSONObject(first.toString()).put("id","row")
                .put("sets",new org.json.JSONArray().put(first.getJSONArray("sets").getJSONObject(1)));
            first.getJSONArray("sets").remove(1);frame.getJSONArray("exercises").put(second);
            store.prepare(frame,frame.getLong("executionRevision"));
        }
        arm();PhoneWorkoutExecution.execute(context,command());
        ShadowLooper.idleMainLooper(30000);
        JSONObject before=IntervalTimerService.snapshot(context);
        assertEquals("rest",before.getString("phase"));
        IntervalTimerService.command(context,before.getString("key"),"skipRest");
        assertEquals("complete",IntervalTimerService.snapshot(context).getString("phase"));
        JSONObject display=PhoneWorkoutExecution.display(context).getJSONObject("automation");
        assertEquals("ready",display.getString("phase"));
        assertFalse("A completed rest must expose recording, not an unusable Resume action",display.getBoolean("paused"));
        assertEquals(nextExercise?1:0,PhoneWorkoutExecution.display(context).getInt("currentExercise"));
        JSONObject next=command().put("commandId","record-b-after-skip").put("setId","b").put("exerciseId",nextExercise?"row":"press");
        assertEquals("applied",PhoneWorkoutExecution.execute(context,next).getString("status"));
        assertEquals(2,store.pendingEvents("workout",1,0).length());
    }
    @Test public void skippedTimedRestContinuesTheWorkPhaseWithoutInventingRepetitions() throws Exception {
        JSONObject frame=store.snapshot();frame.getJSONArray("exercises").getJSONObject(0).put("repUnit","mp");store.prepare(frame,1);
        arm();PhoneWorkoutExecution.execute(context,command().put("action","automaticControl").put("value","start"));
        ShadowLooper.idleMainLooper(5000);JSONObject timer=IntervalTimerService.snapshot(context);
        assertEquals("rest",timer.getString("phase"));
        IntervalTimerService.command(context,timer.getString("key"),"skipRest");
        JSONObject display=PhoneWorkoutExecution.display(context);
        assertEquals("work",display.getJSONObject("automation").getString("phase"));
        assertFalse(display.getJSONObject("automation").getBoolean("paused"));
        assertFalse(store.snapshot().getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getBoolean("done"));
    }
    private void unilateralRest(String unit) throws Exception {
        JSONObject frame=store.snapshot();frame.getJSONArray("exercises").getJSONObject(0).put("repUnit",unit);store.prepare(frame,1);
        arm();assertTrue("Per-side repetitions retain the manual work-time anchor",store.snapshot().has("manualAnchor162"));JSONObject command=command();
        assertEquals("applied",PhoneWorkoutExecution.execute(context,command).getString("status"));
        assertEquals("7",store.snapshot().getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getString("reps"));
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(30));
        assertEquals("rest",IntervalTimerService.snapshot(context).getString("phase"));
        assertEquals(90000,IntervalTimerService.snapshot(context).getLong("remainingMs"));
        assertEquals("duplicate",PhoneWorkoutExecution.execute(context,command).getString("status"));
        assertEquals(90000,IntervalTimerService.snapshot(context).getLong("remainingMs"));
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(90));
        assertEquals("complete",IntervalTimerService.snapshot(context).getString("phase"));
        assertTrue(IntervalTimerService.armed("workout",1));
    }
    @Test public void perArmRecordingStartsBackgroundRestWithoutDoublingRepetitions() throws Exception {unilateralRest("/kar");}
    @Test public void perLegRecordingStartsBackgroundRestWithoutDoublingRepetitions() throws Exception {unilateralRest("/láb");}
    @Test public void unarmedOrSuspendedControllerCannotMutateExecution() throws Exception {
        assertEquals("controllerNotArmed",PhoneWorkoutExecution.execute(context,command()).getString("reason"));
        arm();store.suspendCommands(true);assertEquals("executionNotPrepared",PhoneWorkoutExecution.execute(context,command()).getString("reason"));
        assertEquals(0,store.pendingEvents("workout",1,0).length());
    }
    @Test public void amendmentAndRearmDuringRestCannotCountRestAsManualWork() throws Exception {
        arm();JSONObject event=PhoneWorkoutExecution.execute(context,command());
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(30));
        assertTrue(store.acknowledgeEvents("workout",1,event.getLong("sequence")));
        JSONObject frame=store.snapshot(),first=frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0);
        JSONObject bases=new JSONObject().put("a",PhoneWorkoutExecutionStore.recordedBase(first));first.put("reps","8");
        store.amend(frame,frame.getLong("executionRevision"),bases);
        store.beginManualWork("workout",1,android.os.SystemClock.elapsedRealtime());
        controller.get().onStartCommand(new Intent(context,IntervalTimerService.class).setAction(IntervalTimerService.ARM)
            .putExtra("workoutId","workout").putExtra("generation",1L).putExtra("language","hu"),0,2);
        assertFalse(store.snapshot().has("manualAnchor162"));
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(92));
        JSONObject next=command().put("commandId","record-b").put("setId","b");
        assertEquals("applied",PhoneWorkoutExecution.execute(context,next).getString("status"));
        JSONObject logs=store.snapshot().getJSONObject("timeLogs162");long work=0,rest=0;
        for(java.util.Iterator<String> i=logs.keys();i.hasNext();){JSONObject log=logs.getJSONObject(i.next());work+=log.getLong("workMs");rest+=log.getLong("restMs");}
        assertEquals(2000,work);assertEquals(120000,rest);
    }
    @Test public void failedRecordingCommitCannotStartTimeline() throws Exception {
        arm();store.getWritableDatabase().execSQL("CREATE TRIGGER fail BEFORE INSERT ON execution BEGIN SELECT RAISE(ABORT, 'injected'); END");
        assertThrows(android.database.sqlite.SQLiteException.class,()->PhoneWorkoutExecution.execute(context,command()));
        assertEquals("",IntervalTimerService.snapshot(context).optString("key"));assertEquals(0,store.pendingEvents("workout",1,0).length());
    }
    @Test public void destroyedServiceReportsInterruptionWithoutRestartingPastRest() throws Exception {
        arm();PhoneWorkoutExecution.execute(context,command());org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(15));
        controller.destroy();controller=null;
        JSONObject status=PhoneWorkoutExecution.status(context);
        assertEquals("interrupted",status.getJSONObject("snapshot").getJSONObject("transition").getString("state"));
        assertFalse(status.getJSONObject("timer").getBoolean("running"));assertTrue(status.getJSONObject("timer").getBoolean("interrupted"));
        assertEquals(1,status.getJSONArray("events").length());
    }
    @Test public void explicitWatchStartBuildsTimedPlanWithoutAnyPluginOrForm() throws Exception {
        JSONObject frame=store.snapshot();frame.getJSONArray("exercises").getJSONObject(0).put("repUnit","mp");store.prepare(frame,1);
        arm();JSONObject command=command().put("action","automaticControl").put("value","start");
        assertEquals("applied",PhoneWorkoutExecution.execute(context,command).getString("status"));
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));JSONObject timer=IntervalTimerService.snapshot(context);
        assertEquals("rest",timer.getString("phase"));assertEquals(3000,timer.getLong("remainingMs"));
        assertEquals("duplicate",PhoneWorkoutExecution.execute(context,command).getString("status"));
        assertEquals(3000,IntervalTimerService.snapshot(context).getLong("remainingMs"));
    }
    @Test public void receiptBeforeDispatchIsDeliveredOnceWhileControllerIsStillArmed() throws Exception {
        arm();JSONObject command=command();store.record(command);
        assertEquals("",IntervalTimerService.snapshot(context).optString("key"));
        assertEquals("duplicate",PhoneWorkoutExecution.execute(context,command).getString("status"));
        assertTrue(IntervalTimerService.snapshot(context).getBoolean("running"));
        assertEquals("started",store.snapshot().getJSONObject("transition").getString("state"));
        ShadowLooper.idleMainLooper(4000);
        assertEquals("duplicate",PhoneWorkoutExecution.execute(context,command).getString("status"));
        assertEquals(116000,IntervalTimerService.snapshot(context).getLong("remainingMs"));
    }
    @Test public void closedWorkoutReplayAcknowledgesReceiptWithoutReopeningExecution() throws Exception {
        arm();JSONObject command=command(),event=PhoneWorkoutExecution.execute(context,command);
        assertTrue(store.acknowledgeEvents("workout",1,event.getLong("sequence")));
        assertTrue(store.closeExecution("workout",1,store.snapshot().getLong("executionRevision")));
        IntervalTimerService.closeWorkout("workout",1);assertFalse(IntervalTimerService.snapshot(context).getBoolean("running"));
        assertEquals("duplicate",PhoneWorkoutExecution.execute(context,command).getString("status"));
        assertFalse(store.snapshot().getBoolean("active"));
    }
    private JSONObject pauseInput(boolean paused,long revision)throws Exception {
        return new JSONObject(store.snapshot().toString()).put("workoutPaused",paused).put("pauseRevision",revision)
            .put("activeIntervals",new org.json.JSONArray().put(new JSONObject().put("start","2026-10-14T10:00:00Z").put("end",paused?"2026-10-14T10:00:30Z":JSONObject.NULL)));
    }
    @Test public void explicitWorkoutPauseFreezesNativeRestForTwentyMinutesAndOnlyExplicitlyResumes() throws Exception {
        arm();PhoneWorkoutExecution.execute(context,command());ShadowLooper.idleMainLooper(30000);
        long remaining=IntervalTimerService.snapshot(context).getLong("remainingMs");
        PhoneWorkoutExecution.setWorkoutPause(context,pauseInput(true,1));
        assertTrue(store.snapshot().getBoolean("workoutPaused"));assertFalse(IntervalTimerService.snapshot(context).getBoolean("running"));
        ShadowLooper.idleMainLooper(1200000);
        assertEquals(remaining,IntervalTimerService.snapshot(context).getLong("remainingMs"));
        assertFalse(store.snapshot().getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(1).getBoolean("done"));
        assertTrue(PhoneWorkoutExecution.display(context).getBoolean("workoutPaused"));
        try {IntervalTimerService.command(context,IntervalTimerService.snapshot(context).getString("key"),"skipRest");fail("Suspended workout accepted skip");}catch(IllegalStateException expected){}
        PhoneWorkoutExecution.setWorkoutPause(context,pauseInput(false,2));
        assertTrue(IntervalTimerService.snapshot(context).getBoolean("running"));
        assertEquals(remaining,IntervalTimerService.snapshot(context).getLong("remainingMs"));
    }
    @Test public void pollingAndStalePreparationCannotClearAnExplicitWorkoutPause() throws Exception {
        arm();JSONObject stale=new JSONObject(store.snapshot().toString());
        PhoneWorkoutExecution.setWorkoutPause(context,pauseInput(true,3));
        JSONObject result=store.prepare(stale,store.snapshot().getLong("executionRevision"));
        assertTrue(result.getBoolean("workoutPaused"));assertEquals(3,result.getLong("pauseRevision"));
        assertEquals("rejected",PhoneWorkoutExecution.execute(context,command()).getString("status"));
        try{PhoneWorkoutExecution.setWorkoutPause(context,stale.put("pauseRevision",2).put("workoutPaused",false));fail("Stale continuation accepted");}catch(IllegalStateException expected){}
    }
    @Test public void manualWorkExcludesTheLongSuspension() throws Exception {
        arm();store.beginManualWork("workout",1,android.os.SystemClock.elapsedRealtime());ShadowLooper.idleMainLooper(5000);
        PhoneWorkoutExecution.setWorkoutPause(context,pauseInput(true,1));ShadowLooper.idleMainLooper(1200000);
        PhoneWorkoutExecution.setWorkoutPause(context,pauseInput(false,2));ShadowLooper.idleMainLooper(2000);
        assertEquals("applied",PhoneWorkoutExecution.execute(context,command()).getString("status"));
        JSONObject logs=store.snapshot().getJSONObject("timeLogs162");long work=0;
        for(java.util.Iterator<String> keys=logs.keys();keys.hasNext();)work+=logs.getJSONObject(keys.next()).optLong("workMs");
        assertEquals(7000,work);
    }

}
