package com.repforge.app;

import android.content.Context;
import android.content.Intent;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28, manifest=Config.NONE)
public class PhoneAutomaticBackgroundTest {
    private Context context;
    @Before public void reset() {
        context=RuntimeEnvironment.getApplication();
        context.getSharedPreferences("trainpilot_wear_sync",0).edit().clear().commit();
        context.getSharedPreferences("trainpilot_wear_commands",0).edit().clear().commit();
        context.getSharedPreferences("trainpilot-interval",0).edit().clear().commit();
    }
    private JSONObject set(String id)throws Exception{return new JSONObject().put("setId",id).put("set",1).put("reps","5").put("weight",5.5).put("done",false);}
    private JSONObject fixture()throws Exception{
        JSONObject preset=new JSONObject().put("work",2).put("rest",1).put("rounds",1).put("vibration",false).put("sound",false);
        return new JSONObject().put("active",true).put("workoutId","workout").put("started","2026-10-13T08:00:00Z")
            .put("timerOwner","phone").put("currentExercise",0).put("restSeconds",120).put("watchSequence",0)
            .put("automation",new JSONObject().put("owner","phone").put("enabled",true).put("key","").put("phaseNumber",0))
            .put("backgroundAutomatic162",new JSONObject().put("enabled",true).put("preset",preset))
            .put("exercises",new JSONArray().put(new JSONObject().put("id","press").put("name","Press").put("repUnit","ism.")
                .put("automaticRestSeconds",120).put("sets",new JSONArray().put(set("s1")).put(set("s2")))));
    }
    private JSONObject command()throws Exception{return new JSONObject().put("commandId","command-1").put("workoutId","workout")
        .put("action","completeSet").put("exerciseId","press").put("setId","s1").put("sequence",4).put("createdAt",System.currentTimeMillis())
        .put("reps","7").put("weight","12.5").put("automation",fixture().getJSONObject("automation"));}
    @Test public void storesOnlyActualRepetitionsAndStartsTheNextStableRest()throws Exception{
        JSONObject r=PhoneAutomaticReducer.reduce(fixture(),command(),new JSONObject()),snapshot=r.getJSONObject("snapshot"),plan=r.getJSONObject("plan");
        JSONArray sets=snapshot.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets");
        assertEquals("7",sets.getJSONObject(0).getString("reps"));assertEquals(12.5,sets.getJSONObject(0).getDouble("weight"),0);
        assertTrue(sets.getJSONObject(0).getBoolean("done"));assertFalse(sets.getJSONObject(1).getBoolean("done"));
        assertEquals("s2",plan.getJSONObject("target").getString("setId"));assertEquals(120,plan.getJSONArray("steps").getJSONObject(0).getInt("rest"));
        assertEquals(4,snapshot.getLong("watchSequence"));assertTrue(plan.getBoolean("restOnly"));
    }
    @Test public void wrongWorkoutSetOwnerAndInvalidValuesDoNothing()throws Exception{
        assertNull(PhoneAutomaticReducer.reduce(fixture(),command().put("workoutId","other"),new JSONObject()));
        assertNull(PhoneAutomaticReducer.reduce(fixture(),command().put("setId","s2"),new JSONObject()));
        assertNull(PhoneAutomaticReducer.reduce(fixture().put("timerOwner","watch"),command(),new JSONObject()));
        assertNull(PhoneAutomaticReducer.reduce(fixture(),command().put("reps","0"),new JSONObject()));
        assertNull(PhoneAutomaticReducer.reduce(fixture(),command().put("weight","NaN"),new JSONObject()));
        assertNull(PhoneAutomaticReducer.reduce(fixture(),command().put("reps","7.2"),new JSONObject()));
    }
    @Test public void completedSetCannotRecordTwiceEvenWithANewCommandId()throws Exception{
        JSONObject r=PhoneAutomaticReducer.reduce(fixture(),command(),new JSONObject());
        assertNull(PhoneAutomaticReducer.reduce(r.getJSONObject("snapshot"),command().put("commandId","duplicate"),new JSONObject()));
    }
    @Test public void finalSetAndZeroRestDoNotInventATimer()throws Exception{
        JSONObject input=fixture();input.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").remove(1);
        JSONObject r=PhoneAutomaticReducer.reduce(input,command(),new JSONObject());assertNull(r.optJSONObject("plan"));
        assertEquals("complete",r.getJSONObject("snapshot").getJSONObject("automation").getString("phase"));
        input=fixture();input.getJSONArray("exercises").getJSONObject(0).put("automaticRestSeconds",0);
        assertNull(PhoneAutomaticReducer.reduce(input,command(),new JSONObject()).optJSONObject("plan"));
    }
    @Test public void strengthToTimedPlanAndExplicitTimedStartNeedNoWebForm()throws Exception{
        JSONObject input=fixture();JSONArray exercises=input.getJSONArray("exercises");exercises.getJSONObject(0).getJSONArray("sets").remove(1);
        exercises.put(new JSONObject().put("id","plank").put("repUnit","mp").put("sets",new JSONArray().put(set("time-1").put("reps","0"))));
        JSONObject plan=PhoneAutomaticReducer.reduce(input,command(),new JSONObject()).getJSONObject("plan");
        assertTrue(plan.getBoolean("autoAdvance"));assertEquals(120,plan.getInt("initialRest"));assertEquals(2,plan.getJSONArray("steps").getJSONObject(0).getInt("work"));
        input.put("currentExercise",1);
        JSONObject start=command().put("action","automaticControl").put("value","start");
        assertEquals(0,PhoneAutomaticReducer.reduce(input,start,new JSONObject()).getJSONObject("plan").getInt("initialRest"));
        exercises.getJSONObject(1).put("measurementType","distance");assertNull(PhoneAutomaticReducer.reduce(input,start,new JSONObject()));
    }
    @Test public void phaseMismatchAndInterruptedTimersCannotAdvance()throws Exception{
        JSONObject input=fixture();input.getJSONObject("automation").put("key","timer");JSONObject c=command();c.getJSONObject("automation").put("key","timer").put("phaseNumber",2);
        JSONObject timer=new JSONObject().put("key","timer").put("phase","complete").put("phaseNumber",3);
        assertNull(PhoneAutomaticReducer.reduce(input,c,timer));timer.put("phaseNumber",2).put("interrupted",true);
        assertNull(PhoneAutomaticReducer.reduce(input,c,timer));
    }
    @Test public void receiptAndSnapshotSurviveReplayAndStaleWebPublishes()throws Exception{
        JSONObject stale=fixture();ActiveWorkoutStore.save(context,stale);
        JSONObject r=ActiveWorkoutStore.recordBackground(context,command(),new JSONObject());assertNotNull(r);
        assertNull(ActiveWorkoutStore.recordBackground(context,command(),new JSONObject()));
        JSONObject merged=ActiveWorkoutStore.save(context,stale);
        assertTrue(merged.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getBoolean("done"));
        assertNotNull(ActiveWorkoutStore.receipt(context,"command-1"));
        stale.put("backgroundApplied162",new JSONArray().put("command-1"));
        assertFalse(ActiveWorkoutStore.save(context,stale).getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getBoolean("done"));
        ActiveWorkoutStore.acknowledgeBackground(context,"command-1");assertNull(ActiveWorkoutStore.receipt(context,"command-1"));
    }
    @Test public void receiptNeverOverwritesAnotherDraft()throws Exception{
        ActiveWorkoutStore.save(context,fixture());ActiveWorkoutStore.recordBackground(context,command(),new JSONObject());
        JSONObject other=fixture().put("workoutId","other");assertFalse(ActiveWorkoutStore.save(context,other).getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getBoolean("done"));
        assertNotNull(ActiveWorkoutStore.receipt(context,"command-1"));
    }
    @Test public void foregroundClaimBlocksALateListenerButKeepsTheCommand()throws Exception{
        WearCommandQueueStore.enqueue(context,command(),null);WearCommandQueueStore.claim(context,"command-1");
        assertTrue(WearCommandQueueStore.claimed(context,"command-1"));assertEquals(1,WearCommandQueueStore.pending(context).length());
        assertFalse(WearCommandQueueStore.seen(context,"command-1"));
    }
    @Test public void armedNativeServiceRecordsWithoutAWebActivityAndDoesNotRestartOnReplay()throws Exception{
        ActiveWorkoutStore.save(context,fixture());WearCommandQueueStore.enqueue(context,command(),null);
        IntervalTimerService service=Robolectric.buildService(IntervalTimerService.class).create().get();
        try{
            service.onStartCommand(new Intent().setAction(IntervalTimerService.ARM).putExtra("workoutId","workout"),0,1);
            assertTrue(IntervalTimerService.armed("workout"));
            assertNotNull(IntervalTimerService.acceptWatchRecording(context,command()));
            JSONObject timer=IntervalTimerService.snapshot(context);assertTrue(timer.getBoolean("running"));assertEquals("bg|command-1",timer.getString("key"));
            assertNull(IntervalTimerService.acceptWatchRecording(context,command()));
            IntervalTimerService.command(context,timer.getString("key"),"pause");assertFalse(IntervalTimerService.snapshot(context).getBoolean("running"));
            IntervalTimerService.command(context,timer.getString("key"),"resume");assertTrue(IntervalTimerService.snapshot(context).getBoolean("running"));
            IntervalTimerService.command(context,timer.getString("key"),"skipRest");assertTrue(IntervalTimerService.armed("workout"));
            assertEquals("complete",IntervalTimerService.snapshot(context).getString("phase"));
        }finally{service.onDestroy();}
    }
    @Test public void unarmedOrExpiredRequestsStayForForegroundProcessing()throws Exception{
        ActiveWorkoutStore.save(context,fixture());assertNull(IntervalTimerService.acceptWatchRecording(context,command()));
        IntervalTimerService service=Robolectric.buildService(IntervalTimerService.class).create().get();
        try{service.onStartCommand(new Intent().setAction(IntervalTimerService.ARM).putExtra("workoutId","workout"),0,1);
            assertNull(IntervalTimerService.acceptWatchRecording(context,command().put("createdAt",System.currentTimeMillis()-31000)));
            assertFalse(ActiveWorkoutStore.current(context).getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getBoolean("done"));
        }finally{service.onDestroy();}
    }
    @Test public void serviceDestructionKeepsAnInterruptedRecordingWithoutInventingTheNextSet()throws Exception{
        ActiveWorkoutStore.save(context,fixture());IntervalTimerService service=Robolectric.buildService(IntervalTimerService.class).create().get();
        service.onStartCommand(new Intent().setAction(IntervalTimerService.ARM).putExtra("workoutId","workout"),0,1);
        IntervalTimerService.acceptWatchRecording(context,command());service.onDestroy();
        JSONObject state=IntervalTimerService.snapshot(context);assertTrue(state.getBoolean("interrupted"));assertFalse(state.getBoolean("running"));
        assertNotNull(ActiveWorkoutStore.receipt(context,"command-1"));
        assertFalse(ActiveWorkoutStore.current(context).getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(1).getBoolean("done"));
    }
    @Test public void timedCompletionStillUpdatesTheNativeSnapshotAfterForegroundAcknowledgement()throws Exception{
        JSONObject input=fixture();input.getJSONArray("exercises").getJSONObject(0).put("repUnit","mp");
        ActiveWorkoutStore.save(context,input);JSONObject start=command().put("action","automaticControl").put("value","start");
        JSONObject receipt=ActiveWorkoutStore.recordBackground(context,start,new JSONObject());JSONObject plan=receipt.getJSONObject("plan");
        ActiveWorkoutStore.acknowledgeBackground(context,"command-1");
        JSONObject clock=new JSONObject(plan.toString()).put("phase","complete").put("completed",2).put("stepIndex",1).put("phaseNumber",4);
        ActiveWorkoutStore.backgroundClock(context,clock);
        JSONArray sets=ActiveWorkoutStore.current(context).getJSONArray("exercises").getJSONObject(0).getJSONArray("sets");
        assertTrue(sets.getJSONObject(0).getBoolean("done"));assertTrue(sets.getJSONObject(1).getBoolean("done"));
        assertEquals("8",sets.getJSONObject(0).getString("reps"));assertEquals("7",sets.getJSONObject(1).getString("reps"));
    }
}
