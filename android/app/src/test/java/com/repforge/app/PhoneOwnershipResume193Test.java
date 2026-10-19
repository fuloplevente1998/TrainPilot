package com.repforge.app;

import android.content.Context;
import org.json.JSONArray;
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
@Config(sdk=33,manifest=Config.NONE)
public class PhoneOwnershipResume193Test {
    private Context context;private PhoneWorkoutExecutionStore store;
    @Before public void open(){context=RuntimeEnvironment.getApplication();context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);store=new PhoneWorkoutExecutionStore(context);}
    @After public void close(){store.close();context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);}
    private JSONObject returned()throws Exception {
        return new JSONObject("{\"active\":true,\"ownershipProtocol193\":1,\"ownerEpoch193\":2,\"workoutId\":\"w193\",\"timerOwner\":\"phone\",\"workoutPaused\":true,\"pauseRevision\":10,\"currentExercise\":0,\"restSeconds\":90,\"handoff193\":{\"protocol\":1,\"id\":\"return193\",\"source\":\"watch\",\"target\":\"phone\",\"state\":\"committed\",\"epoch\":2},\"exercises\":[{\"id\":\"press\",\"repUnit\":\"ism.\",\"sets\":[{\"setId\":\"one\",\"done\":false,\"reps\":\"25\",\"weight\":5}]}]}");
    }
    private JSONObject original()throws Exception {
        JSONObject frame=returned();frame.remove("handoff193");frame.put("ownerEpoch193",0).put("generation",4).put("executionRevision",0).put("automaticEnabled",false);
        frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).put("reps","12");return frame;
    }
    private JSONObject timed()throws Exception {
        JSONObject value=returned().put("started","2026-10-18T10:00:00Z");value.getJSONArray("exercises").getJSONObject(0).put("repUnit","mp");
        value.put("automation",new JSONObject().put("enabled",true).put("owner","phone").put("phase","work").put("workSeconds",10).put("restSeconds",5).put("rounds",2));
        JSONArray rows=new JSONArray().put(new JSONObject("{\"exerciseId\":\"press\",\"setId\":\"one\",\"work\":10,\"rest\":5,\"rounds\":2,\"baseSeconds\":7}"));
        JSONObject ledger=new JSONObject("{\"schema\":1,\"owner\":\"timeline193\",\"positionMs\":18000,\"workMs\":10000,\"restMs\":8000,\"pausedMs\":9000,\"steps\":[{\"exerciseId\":\"press\",\"setId\":\"one\",\"workMs\":10000,\"restMs\":8000,\"pausedMs\":9000}]}");
        JSONObject timer=new JSONObject().put("key","native|4|9").put("workoutId","w193").put("generation",4).put("autoAdvance",true).put("restOnly",false).put("initialRest",3).put("steps",rows)
            .put("elapsedMs",18000).put("phase","work").put("stepIndex",0).put("round",2).put("remainingMs",10000).put("stepElapsedMs",15000).put("totalMs",28000).put("completed",0)
            .put("ledger",ledger).put("running",false).put("stopped",false);
        JSONObject history=new JSONObject().put("phone-old",new JSONObject().put("owner","phone").put("workMs",5000)).put("watch-old",new JSONObject().put("owner","watch").put("workMs",5000));
        return value.put("resumeAutomaticOnContinue",true).put("timerCheckpoint193",new JSONObject().put("schema",1).put("timer",timer).put("timeLogs193",history));
    }
    private interface Attempt {void run()throws Exception;}
    private void rejected(Attempt action)throws Exception {try{action.run();fail("Unsafe phone return accepted");}catch(IllegalStateException expected){}}
    @Test public void newPausedGenerationKeepsTheWatchValuesAndCannotReuseRetiredPhoneFrame()throws Exception {
        JSONObject old=store.prepare(original(),0),frame=store.importOwnership193(returned(),1000);
        assertEquals(1000,frame.getLong("generation"));assertTrue(frame.getBoolean("workoutPaused"));assertEquals(2,frame.getLong("ownerEpoch193"));
        assertEquals("25",frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getString("reps"));
        assertEquals(4,old.getLong("generation"));assertTrue(store.commandsSuspended());assertFalse(frame.has("transition"));
    }
    @Test public void lostImportResponseCannotEraseNewerValuesOrCreateAnotherGeneration()throws Exception {
        JSONObject frame=store.importOwnership193(returned(),1000);frame.put("workoutPaused",false);
        frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).put("reps","29");
        JSONObject replay=PhoneOwnershipResume193.prepare(returned(),frame,9999);
        assertEquals(frame.toString(),replay.toString());
    }
    @Test public void offeredClosedWrongWorkoutAndOldEpochCannotImport()throws Exception {
        JSONObject offer=returned();offer.getJSONObject("handoff193").put("state","offered");rejected(()->PhoneOwnershipResume193.prepare(offer,null,1000));
        rejected(()->PhoneOwnershipResume193.prepare(returned(),original().put("active",false),1000));
        rejected(()->PhoneOwnershipResume193.prepare(returned(),original().put("workoutId","other"),1000));
        rejected(()->PhoneOwnershipResume193.prepare(returned(),original().put("ownerEpoch193",2),1000));
    }
    @Test public void readinessIsBoundToTransferAndRetainsTheExplicitPause()throws Exception {
        store.importOwnership193(returned(),1000);rejected(()->store.ownershipReady193("different",2));assertTrue(store.commandsSuspended());
        JSONObject ready=store.ownershipReady193("return193",2);
        assertTrue(ready.getBoolean("workoutPaused"));assertFalse(store.commandsSuspended());assertEquals("installed",ready.getJSONObject("ownershipImport193").getString("state"));
    }
    @Test public void restoredTimelineKeepsRoundBaselineAndSourceLogsWithoutCountingTheTransferWait()throws Exception {
        JSONObject frame=store.importOwnership193(timed(),1000),plan=frame.getJSONObject("transition").getJSONObject("plan");
        assertEquals(18000,plan.getLong("elapsedMs"));assertEquals(2,plan.getInt("round"));assertEquals(10000,plan.getLong("remainingMs"));
        assertEquals("2026-10-18T10:00:00Z",plan.getString("started"));
        assertEquals(7,plan.getJSONArray("steps").getJSONObject(0).getInt("baseSeconds"));
        assertTrue(frame.getBoolean("resumeTimerOnContinue201"));assertTrue(frame.getJSONObject("timeLogs162").has("watch-old"));
        IntervalTimeLedger restored=IntervalTimeLedger.restore(plan.getJSONArray("steps"),plan.getJSONObject("ledger"),50000);
        JSONObject initial=restored.json(plan.getJSONArray("steps"));assertEquals(9000,initial.getLong("pausedMs"));
        restored.sample(IntervalTimerService.validate(plan),19000,51000,true,false,false);
        JSONObject aggregate=restored.json(plan.getJSONArray("steps")),local=PhoneOwnershipResume193.localLog(plan,aggregate);
        assertEquals(11000,aggregate.getLong("workMs"));assertEquals(1000,local.getLong("workMs"));assertEquals(0,local.getLong("restMs"));assertEquals(0,local.getLong("pausedMs"));
        assertEquals(1000,local.getLong("positionMs"));assertEquals(9000,aggregate.getLong("pausedMs"));
    }
    @Test public void zeroLocalLedgerAndCounterRegressionCannotDuplicateInheritedTime()throws Exception {
        JSONObject frame=PhoneOwnershipResume193.prepare(timed(),null,1000),plan=frame.getJSONObject("transition").getJSONObject("plan");
        JSONObject ledger=plan.getJSONObject("ledger"),before=new JSONObject(ledger.toString()),local=PhoneOwnershipResume193.localLog(plan,ledger);
        assertEquals(0,local.getLong("positionMs"));assertEquals(before.toString(),ledger.toString());
        JSONObject invalid=new JSONObject(ledger.toString()).put("workMs",9999);rejected(()->PhoneOwnershipResume193.localLog(plan,invalid));
    }
    @Test public void pendingOldNativeRecordingMustReachCanonicalStorageBeforeReturnImport()throws Exception {
        JSONObject input=original().put("automaticEnabled",true).put("timerConfig",new JSONObject().put("work",10).put("rest",5).put("rounds",1));store.prepare(input,0);
        JSONObject command=new JSONObject().put("commandId","pending193").put("action","completeSet").put("workoutId","w193").put("generation",4).put("executionRevision",1)
            .put("exerciseId","press").put("setId","one").put("reps","12").put("weight",5);
        store.record(command);rejected(()->store.importOwnership193(returned(),1000));assertEquals(4,store.snapshot().getLong("generation"));
        long seq=store.pendingEvents("w193",4,0).getJSONObject(0).getLong("sequence");assertTrue(store.acknowledgeEvents("w193",4,seq));
        assertEquals(1000,store.importOwnership193(returned(),1000).getLong("generation"));
    }
    @Test public void blockedRestoreAndMalformedLedgerCannotReplaceTheExecution()throws Exception {
        store.prepare(original(),0);store.setRestoreBlocked(true);rejected(()->store.importOwnership193(returned(),1000));assertEquals(4,store.snapshot().getLong("generation"));
        JSONObject wrong=timed();wrong.getJSONObject("timerCheckpoint193").getJSONObject("timer").getJSONObject("ledger").put("restMs",1);
        rejected(()->PhoneOwnershipResume193.prepare(wrong,null,1000));
    }
}
