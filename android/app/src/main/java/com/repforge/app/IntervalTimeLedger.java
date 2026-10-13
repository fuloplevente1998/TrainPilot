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
