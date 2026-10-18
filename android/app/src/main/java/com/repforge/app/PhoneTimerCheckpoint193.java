package com.repforge.app;

import org.json.JSONArray;
import org.json.JSONObject;

/** Immutable source clock checkpoint. Device-local monotonic anchors are never transferred. */
final class PhoneTimerCheckpoint193 {
    private static void require(boolean condition) {
        if(!condition)throw new IllegalStateException("Reconcile the paused timer before transferring the workout");
    }
    private static long integer(JSONObject value,String field,long max)throws Exception {
        long n=PhoneWorkoutRecording.version(value,field);require(n<=max);return n;
    }
    static JSONObject create(JSONObject frozen,JSONObject timer)throws Exception {
        JSONObject a=frozen.optJSONObject("automation");
        if(timer==null||timer.optString("key").isEmpty()||!frozen.optString("workoutId").equals(timer.optString("workoutId"))
                ||timer.optBoolean("stopped")||"complete".equals(timer.optString("phase"))) {
            require(a==null||!a.optBoolean("enabled")||!("work".equals(a.optString("phase"))||"rest".equals(a.optString("phase"))));
            return null;
        }
        require(frozen.optBoolean("workoutPaused")&&!timer.optBoolean("running")&&!timer.optBoolean("interrupted"));
        IntervalTimeline timeline=IntervalTimerService.validate(timer);
        long elapsed=integer(timer,"elapsedMs",IntervalTimeline.MAX_MS);
        IntervalTimeline.Phase phase=timeline.at(elapsed);
        require(elapsed==phase.elapsedMs&&timer.getString("phase").equals(phase.phase)
                &&timer.getInt("stepIndex")==phase.step&&timer.getInt("round")==phase.round
                &&timer.getLong("remainingMs")==phase.remainingMs&&timer.getLong("stepElapsedMs")==phase.stepElapsedMs
                &&timer.getLong("totalMs")==phase.totalMs&&timer.getInt("completed")==phase.completed);
        JSONObject ledger=timer.getJSONObject("ledger");JSONArray rows=timer.getJSONArray("steps"),parts=ledger.getJSONArray("steps");
        require(parts.length()==rows.length()&&ledger.getInt("schema")==1&&"phone".equals(ledger.getString("owner"))
                &&integer(ledger,"positionMs",IntervalTimeline.MAX_MS)==elapsed);
        long work=0,rest=0,paused=0;
        for(int i=0;i<rows.length();i++) {
            JSONObject row=rows.getJSONObject(i),part=parts.getJSONObject(i);
            require(row.getString("exerciseId").equals(part.getString("exerciseId"))&&row.getString("setId").equals(part.getString("setId"))
                    &&row.optString("gpsKey").isEmpty()&&contains(frozen,row));
            double base=row.optDouble("baseSeconds",0);require(Double.isFinite(base)&&base>=0&&base<=86400);
            work+=integer(part,"workMs",IntervalTimeline.MAX_MS);rest+=integer(part,"restMs",IntervalTimeline.MAX_MS);
            paused+=integer(part,"pausedMs",604800000L);
        }
        require(work==integer(ledger,"workMs",IntervalTimeline.MAX_MS)&&rest==integer(ledger,"restMs",IntervalTimeline.MAX_MS)
                &&paused==integer(ledger,"pausedMs",604800000L)&&work+rest<=elapsed);
        JSONObject exact=new JSONObject(timer.toString());
        // Only position and measured counters cross devices. Resume supplies a fresh local anchor.
        exact.remove("anchorMs");exact.remove("began");exact.remove("boot");exact.remove("deadlineAt");
        return new JSONObject().put("schema",1).put("source","phone").put("sourceEpoch",frozen.optLong("ownerEpoch193"))
                .put("timer",exact).put("resumeRequested",false);
    }
    private static boolean contains(JSONObject workout,JSONObject row)throws Exception {
        JSONArray exercises=workout.getJSONArray("exercises");
        for(int i=0;i<exercises.length();i++) {
            JSONObject e=exercises.getJSONObject(i);if(!row.getString("exerciseId").equals(e.optString("id")))continue;
            JSONArray sets=e.getJSONArray("sets");for(int j=0;j<sets.length();j++)if(row.getString("setId").equals(sets.getJSONObject(j).optString("setId")))return true;
        }return false;
    }
    static void verifyCanonical(JSONObject frozen,JSONObject status)throws Exception {
        JSONObject frame=status.optJSONObject("snapshot");
        if(frame==null||!frame.optBoolean("active")||!frozen.optString("workoutId").equals(frame.optString("workoutId")))return;
        require(frame.optBoolean("workoutPaused")&&frozen.optLong("generation")==frame.optLong("generation")
                &&frozen.optLong("executionRevision")==frame.optLong("executionRevision")&&status.getJSONArray("events").length()==0);
        JSONArray own=frozen.getJSONArray("exercises"),nativeRows=frame.getJSONArray("exercises");require(own.length()==nativeRows.length());
        for(int i=0;i<own.length();i++) {
            JSONObject a=own.getJSONObject(i),b=nativeRows.getJSONObject(i);require(a.getString("id").equals(b.getString("id")));
            JSONArray aa=a.getJSONArray("sets"),bb=b.getJSONArray("sets");require(aa.length()==bb.length());
            for(int j=0;j<aa.length();j++) {
                JSONObject x=aa.getJSONObject(j),y=bb.getJSONObject(j);
                require(x.getString("setId").equals(y.getString("setId"))&&x.getBoolean("done")==y.getBoolean("done"));
                for(String field:new String[]{"reps","weight","leftSeconds","rightSeconds","distanceMeters"}) {
                    String first=x.optString(field),second=y.optString(field);
                    if(first.equals(second))continue;
                    require(!first.isEmpty()&&!second.isEmpty()&&Double.parseDouble(first)==Double.parseDouble(second));
                }
            }
        }
    }
}
