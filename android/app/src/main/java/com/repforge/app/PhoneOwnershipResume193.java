package com.repforge.app;

import com.repforge.shared.WorkoutOwnership193;
import org.json.JSONArray;
import org.json.JSONObject;

/** Builds a fresh paused phone execution from the locally acknowledged reverse handoff. */
final class PhoneOwnershipResume193 {
    private static void require(boolean value) {
        if(!value)throw new IllegalStateException("The returned workout changed; reconcile before continuing");
    }
    static JSONObject prepare(JSONObject consent,JSONObject old,long now)throws Exception {
        JSONObject h=WorkoutOwnership193.transfer(consent);
        require(WorkoutOwnership193.owns(consent,"phone")&&consent.optBoolean("workoutPaused")&&consent.optInt("ownershipProtocol193")==1
                &&h!=null&&h.optInt("protocol")==1&&"committed".equals(h.optString("state"))&&"phone".equals(h.optString("target"))&&"watch".equals(h.optString("source"))
                &&h.optLong("epoch")==WorkoutOwnership193.epoch(consent));
        JSONObject prior=old==null?null:old.optJSONObject("ownershipImport193");
        if(prior!=null&&prior.optString("id").equals(h.optString("id"))&&prior.optLong("epoch")==h.optLong("epoch")) {
            require(old.optBoolean("active")&&old.optString("workoutId").equals(consent.optString("workoutId")));
            return new JSONObject(old.toString()); // A lost bridge response cannot reinstall the frozen values.
        }
        if(old!=null) {
            require(!old.optBoolean("active")||old.optString("workoutId").equals(consent.optString("workoutId")));
            if(old.optString("workoutId").equals(consent.optString("workoutId")))require(old.optBoolean("active")&&old.optLong("ownerEpoch193")<WorkoutOwnership193.epoch(consent));
        }
        long generation=Math.max(now,old==null?1:PhoneWorkoutRecording.advance(old.getLong("generation")));
        require(generation>0&&generation<=9007199254740991L);
        JSONObject frame=new JSONObject(consent.toString()),a=consent.optJSONObject("automation");
        frame.put("generation",generation).put("executionRevision",1).put("nativeExecutionProtocol",2)
                .put("automaticEnabled",a!=null&&a.optBoolean("enabled"))
                .put("ownershipImport193",new JSONObject().put("id",h.getString("id")).put("epoch",h.getLong("epoch")).put("state","prepared"));
        frame.remove("transition");frame.remove("latestTimer162");frame.remove("manualAnchor162");frame.remove("retiredTimers188");
        JSONObject config=new JSONObject().put("work",a==null?30:a.optInt("workSeconds",30))
                .put("rest",a==null?90:a.optInt("restSeconds",90)).put("rounds",a==null?1:a.optInt("rounds",1))
                .put("sound",true).put("vibration",a==null||a.optBoolean("vibration",true));
        frame.put("timerConfig",config);
        JSONObject checkpoint=consent.optJSONObject("timerCheckpoint193");
        if(checkpoint!=null) {
            require(checkpoint.optInt("schema")==1);
            JSONObject timer=new JSONObject(checkpoint.getJSONObject("timer").toString());
            JSONObject checked=new JSONObject(timer.toString());checked.getJSONObject("ledger").put("owner","phone");
            require(PhoneTimerCheckpoint193.create(consent,checked)!=null);
            String key="ownership193:phone:"+h.getLong("epoch")+":"+h.getString("id");
            timer.put("key",key).put("started",frame.optString("started")).put("generation",generation).put("running",false).put("stopped",false).put("interrupted",false)
                    .put("ownershipBaseLedger193",new JSONObject(timer.getJSONObject("ledger").toString()));
            JSONObject target=timer.getJSONArray("steps").getJSONObject(timer.getInt("stepIndex"));
            timer.put("target",new JSONObject().put("exerciseId",target.getString("exerciseId")).put("setId",target.getString("setId")));
            frame.put("transition",new JSONObject().put("state","ownership193").put("phase",timer.getString("phase"))
                    .put("currentExercise",frame.getInt("currentExercise")).put("plan",timer));
            frame.put("resumeTimerOnContinue201",consent.optBoolean("resumeAutomaticOnContinue"));
            JSONObject logs=checkpoint.optJSONObject("timeLogs193");if(logs!=null)frame.put("timeLogs162",new JSONObject(logs.toString()));
        }
        PhoneWorkoutRecording.validateFrame(frame);PhoneWorkoutPlans.validateConfig(frame);return frame;
    }
    /** Record only this phone epoch's measurements; inherited counters still drive exact set values. */
    static JSONObject localLog(JSONObject timer,JSONObject aggregate)throws org.json.JSONException {
        JSONObject base=timer.optJSONObject("ownershipBaseLedger193"),log=new JSONObject(aggregate.toString());
        if(base==null)return log;
        JSONArray rows=log.getJSONArray("steps"),before=base.getJSONArray("steps");require(rows.length()==before.length());
        long position=0;
        for(String key:new String[]{"workMs","restMs","pausedMs"}) {
            long delta=log.getLong(key)-base.getLong(key);require(delta>=0);log.put(key,delta);position+=delta;
        }
        for(int i=0;i<rows.length();i++) {
            JSONObject row=rows.getJSONObject(i),old=before.getJSONObject(i);
            require(row.getString("setId").equals(old.getString("setId"))&&row.getString("exerciseId").equals(old.getString("exerciseId")));
            for(String key:new String[]{"workMs","restMs","pausedMs"}) {long delta=row.getLong(key)-old.getLong(key);require(delta>=0);row.put(key,delta);}
        }
        return log.put("positionMs",position).put("owner","phone");
    }
}
