package com.repforge.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Actual phase time, separate from the virtual timeline position changed by Skip Rest. */
final class IntervalTimeLedger {
    private final long[] work, rest, paused;
    private long position, sampledAt;
    private boolean wasPaused;
    IntervalTimeLedger(int count, long now) {
        work=new long[count];rest=new long[count];paused=new long[count];sampledAt=now;
    }
    static IntervalTimeLedger restore(JSONArray plan,JSONObject saved,long now)throws JSONException {
        JSONArray rows=saved.getJSONArray("steps");
        if(rows.length()!=plan.length())throw new IllegalArgumentException("Transferred ledger targets changed");
        IntervalTimeLedger value=new IntervalTimeLedger(plan.length(),now);
        long w=0,r=0,p=0;
        for(int i=0;i<plan.length();i++) {
            JSONObject owner=plan.getJSONObject(i),row=rows.getJSONObject(i);
            if(!owner.getString("exerciseId").equals(row.getString("exerciseId"))||!owner.getString("setId").equals(row.getString("setId")))
                throw new IllegalArgumentException("Transferred ledger targets changed");
            value.work[i]=PhoneWorkoutRecording.version(row,"workMs");value.rest[i]=PhoneWorkoutRecording.version(row,"restMs");value.paused[i]=PhoneWorkoutRecording.version(row,"pausedMs");
            w+=value.work[i];r+=value.rest[i];p+=value.paused[i];
        }
        value.position=PhoneWorkoutRecording.version(saved,"positionMs");
        if(value.position>IntervalTimeline.MAX_MS||w+r>value.position||w!=saved.getLong("workMs")||r!=saved.getLong("restMs")||p!=saved.getLong("pausedMs")||p>604800000)
            throw new IllegalArgumentException("Transferred ledger counters changed");
        return value; // Time spent waiting for explicit Resume belongs to no new clock anchor.
    }
    void sample(IntervalTimeline timeline, long progress, long now, boolean running, boolean stopped, boolean interrupted) {
        if(now<sampledAt||progress<position)throw new IllegalArgumentException("Clock moved backwards");
        if(wasPaused)paused[timeline.at(position).step]+=now-sampledAt;
        while(position<progress){
            IntervalTimeline.Phase p=timeline.at(position);
            if("complete".equals(p.phase))break;
            long duration=Math.min(p.remainingMs,progress-position);
            if(duration<=0)throw new IllegalStateException("Empty timeline phase");
            ("work".equals(p.phase)?work:rest)[p.step]+=duration;position+=duration;
        }
        sampledAt=now;wasPaused=!running&&!stopped&&!interrupted&&!"complete".equals(timeline.at(position).phase);
    }
    void skipTo(long progress){if(progress<position)throw new IllegalArgumentException("Invalid skip");position=progress;}
    JSONObject json(JSONArray plan) throws JSONException {
        JSONArray rows=new JSONArray();long workTotal=0,restTotal=0,pauseTotal=0;
        for(int i=0;i<work.length;i++){
            JSONObject owner=plan.getJSONObject(i);
            rows.put(new JSONObject().put("exerciseId",owner.getString("exerciseId")).put("setId",owner.getString("setId"))
                    .put("workMs",work[i]).put("restMs",rest[i]).put("pausedMs",paused[i]));
            workTotal+=work[i];restTotal+=rest[i];pauseTotal+=paused[i];
        }
        return new JSONObject().put("schema",1).put("owner","phone").put("positionMs",position)
                .put("workMs",workTotal).put("restMs",restTotal).put("pausedMs",pauseTotal).put("steps",rows);
    }
}
