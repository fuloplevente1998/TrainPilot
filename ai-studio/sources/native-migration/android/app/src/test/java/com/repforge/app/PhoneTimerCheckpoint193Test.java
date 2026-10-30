package com.repforge.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE)
public class PhoneTimerCheckpoint193Test {
    private JSONObject frozen()throws Exception {
        return new JSONObject("{\"active\":true,\"workoutId\":\"w193\",\"timerOwner\":\"phone\",\"workoutPaused\":true,\"generation\":4,\"executionRevision\":9,\"ownerEpoch193\":2,\"exercises\":[{\"id\":\"plank\",\"sets\":[{\"setId\":\"set193\",\"done\":false,\"reps\":\"7\",\"weight\":0}]}]}");
    }
    private JSONObject timer(long elapsed,long work,long rest)throws Exception {
        JSONObject row=new JSONObject("{\"exerciseId\":\"plank\",\"setId\":\"set193\",\"work\":30,\"rest\":15,\"rounds\":3,\"baseSeconds\":7}");
        JSONArray steps=new JSONArray().put(row);
        JSONObject t=new JSONObject().put("key","native|4|9").put("workoutId","w193").put("generation",4)
            .put("steps",steps).put("autoAdvance",true).put("initialRest",20).put("restOnly",false)
            .put("running",false).put("stopped",false).put("elapsedMs",elapsed).put("phaseNumber",5)
            .put("anchorMs",999999).put("boot",3).put("deadlineAt",123456);
        IntervalTimeline.Phase p=IntervalTimerService.validate(t).at(elapsed);
        t.put("phase",p.phase).put("round",p.round).put("stepIndex",p.step).put("stepElapsedMs",p.stepElapsedMs)
            .put("remainingMs",p.remainingMs).put("totalMs",p.totalMs).put("completed",p.completed);
        JSONObject part=new JSONObject().put("exerciseId","plank").put("setId","set193").put("workMs",work).put("restMs",rest).put("pausedMs",24000);
        t.put("ledger",new JSONObject().put("schema",1).put("owner","phone").put("positionMs",elapsed)
            .put("workMs",work).put("restMs",rest).put("pausedMs",24000).put("steps",new JSONArray().put(part)));
        return t;
    }
    private void rejected(JSONObject workout,JSONObject timer)throws Exception {
        try{PhoneTimerCheckpoint193.create(workout,timer);fail("Unsafe timer accepted");}catch(IllegalStateException expected){}
    }
    @Test public void exactSecondRoundRestAndBaselineSurviveWithoutDeviceAnchor()throws Exception {
        JSONObject t=timer(96000,60000,36000),text=new JSONObject(t.toString());
        JSONObject checkpoint=PhoneTimerCheckpoint193.create(frozen(),t),exact=checkpoint.getJSONObject("timer");
        assertEquals(2,exact.getInt("round"));assertEquals("rest",exact.getString("phase"));assertEquals(14000,exact.getLong("remainingMs"));
        assertEquals(7,exact.getJSONArray("steps").getJSONObject(0).getInt("baseSeconds"));
        assertEquals(36000,exact.getJSONObject("ledger").getLong("restMs"));assertEquals(24000,exact.getJSONObject("ledger").getLong("pausedMs"));
        assertEquals(2,checkpoint.getLong("sourceEpoch"));assertFalse(checkpoint.getBoolean("resumeRequested"));
        assertFalse(exact.has("anchorMs"));assertFalse(exact.has("boot"));assertFalse(exact.has("deadlineAt"));assertEquals(text.toString(),t.toString());
    }
    @Test public void leadingRestAndSkippedRestKeepSeparateMeasuredTime()throws Exception {
        JSONObject leading=PhoneTimerCheckpoint193.create(frozen(),timer(8500,0,8500)).getJSONObject("timer");
        assertEquals(11500,leading.getLong("remainingMs"));assertEquals(0,leading.getLong("stepElapsedMs"));
        JSONObject skipped=PhoneTimerCheckpoint193.create(frozen(),timer(70000,35000,20000)).getJSONObject("timer");
        assertEquals(70000,skipped.getLong("elapsedMs"));assertEquals(20000,skipped.getJSONObject("ledger").getLong("restMs"));
    }
    @Test public void activeInterruptedAndMismatchedPhaseCannotBeTransferred()throws Exception {
        rejected(frozen(),timer(10000,0,10000).put("running",true));
        rejected(frozen(),timer(10000,0,10000).put("interrupted",true));
        rejected(frozen(),timer(10000,0,10000).put("remainingMs",999));
        rejected(frozen().put("workoutPaused",false),timer(10000,0,10000));
    }
    @Test public void wrongSetAndInventedMeasuredTimeCannotBeTransferred()throws Exception {
        JSONObject t=timer(10000,0,10000);t.getJSONArray("steps").getJSONObject(0).put("setId","other");rejected(frozen(),t);
        t=timer(10000,0,10000);t.getJSONObject("ledger").put("workMs",1);rejected(frozen(),t);
        rejected(frozen(),timer(10000,5000,10000));
    }
    @Test public void absentClockIsAllowedOnlyWithoutAnActiveProjectedPhase()throws Exception {
        assertNull(PhoneTimerCheckpoint193.create(frozen(),new JSONObject()));
        rejected(frozen().put("automation",new JSONObject().put("enabled",true).put("phase","rest")),new JSONObject());
    }
    @Test public void nativeEventsAndSetConflictsMustBeReconciledBeforeOffer()throws Exception {
        JSONObject frame=frozen(),status=new JSONObject().put("snapshot",frame).put("events",new JSONArray());
        PhoneTimerCheckpoint193.verifyCanonical(frozen(),status);
        status.getJSONArray("events").put(new JSONObject());
        try{PhoneTimerCheckpoint193.verifyCanonical(frozen(),status);fail();}catch(IllegalStateException expected){}
        status.put("events",new JSONArray());frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).put("reps","8");
        try{PhoneTimerCheckpoint193.verifyCanonical(frozen(),status);fail();}catch(IllegalStateException expected){}
    }
}
