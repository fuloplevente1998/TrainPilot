package com.repforge.app;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class WearWorkoutHandoffTest {
    private final long now=1_800_000_000_000L;
    private JSONObject current() throws Exception { return new JSONObject().put("active",true).put("workoutId","new").put("revision",42); }
    private JSONObject request() throws Exception { return new JSONObject().put("workoutId","new").put("minimumRevision",42).put("sentAt",now); }
    @Test public void repairsOnlyTheAlreadyPublishedMatchingWorkout() throws Exception {
        JSONObject current=current();
        assertSame(current,WearWorkoutHandoff.response(current,request(),now));
        assertSame(current,WearWorkoutHandoff.response(current.put("revision",43),request(),now));
        assertNull(WearWorkoutHandoff.response(current,request().put("workoutId","old"),now));
        assertNull(WearWorkoutHandoff.response(current.put("revision",41),request(),now));
    }
    @Test public void closedAndDelayedRequestsCannotReviveAWorkout() throws Exception {
        assertNull(WearWorkoutHandoff.response(current().put("active",false),request(),now));
        assertNull(WearWorkoutHandoff.response(null,request(),now));
        assertNull(WearWorkoutHandoff.response(current(),request().put("sentAt",now-16000),now));
        assertNull(WearWorkoutHandoff.response(current(),request().put("sentAt",now+16000),now));
        assertNull(WearWorkoutHandoff.response(current(),request().put("minimumRevision",0),now));
    }
}
