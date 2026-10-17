package com.repforge.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.HashSet;

/** Actual phase time, separate from the virtual timeline position changed by Skip Rest. */
final class IntervalTimeLedger {
    private final long[] work, rest, paused;
    private final long[] left,right,sideRest,setRest;
    private final int[] skippedSides;
    private final LinkedHashMap<String,JSONObject> segments=new LinkedHashMap<>();
    private final HashSet<Integer> skipped=new HashSet<>(),compacted=new HashSet<>();
    private long position, sampledAt;
    private boolean wasPaused;
    IntervalTimeLedger(int count, long now) {
        work=new long[count];rest=new long[count];paused=new long[count];left=new long[count];right=new long[count];sideRest=new long[count];setRest=new long[count];skippedSides=new int[count];sampledAt=now;
    }
    void restore192(JSONObject ledger,JSONArray plan,long now)throws JSONException {
        if(ledger.getInt("schema")!=2||ledger.getJSONArray("steps").length()!=work.length)throw new IllegalArgumentException("Invalid restored side ledger");
        long w=0,r=0,p=0;JSONArray rows=ledger.getJSONArray("steps");
        for(int i=0;i<work.length;i++){
            JSONObject row=rows.getJSONObject(i),owner=plan.getJSONObject(i);
            if(!owner.getString("exerciseId").equals(row.getString("exerciseId"))||!owner.getString("setId").equals(row.getString("setId")))throw new IllegalArgumentException("Restored ledger owner changed");
            work[i]=counter192(row,"workMs");rest[i]=counter192(row,"restMs");paused[i]=counter192(row,"pausedMs");w+=work[i];r+=rest[i];p+=paused[i];
            if(owner.has("bilateral192")){
                left[i]=counter192(row,"leftWorkMs");right[i]=counter192(row,"rightWorkMs");sideRest[i]=counter192(row,"sideRestMs");setRest[i]=counter192(row,"setRestMs");
                if(left[i]+right[i]!=work[i]||sideRest[i]+setRest[i]!=rest[i])throw new IllegalArgumentException("Invalid restored side totals");
                skippedSides[i]=(row.optBoolean("skippedLeft192")?1:0)|(row.optBoolean("skippedRight192")?2:0);
                if(row.optBoolean("skippedWork192"))skipped.add(i);if(row.optBoolean("segmentsCompacted192"))compacted.add(i);
                JSONArray parts=row.optJSONArray("segments192");if(parts!=null)for(int j=0;j<parts.length();j++){JSONObject part=new JSONObject(parts.getJSONObject(j).toString());if(part.getInt("step")!=i)throw new IllegalArgumentException("Invalid restored segment");segments.put(part.getString("id"),part);}
            }
        }
        if(w!=ledger.getLong("workMs")||r!=ledger.getLong("restMs")||p!=ledger.getLong("pausedMs"))throw new IllegalArgumentException("Restored totals disagree");
        position=counter192(ledger,"positionMs");sampledAt=now;wasPaused=false;
    }
    private long counter192(JSONObject v,String key)throws JSONException{long n=PhoneWorkoutRecording.version(v,key);if(n>604800000)throw new IllegalArgumentException("Invalid side time");return n;}
    void sample(IntervalTimeline timeline, long progress, long now, boolean running, boolean stopped, boolean interrupted) {
        if(now<sampledAt||progress<position)throw new IllegalArgumentException("Clock moved backwards");
        if(wasPaused){IntervalTimeline.Phase p=timeline.at(position);paused[p.step]+=now-sampledAt;segment(p,0,now-sampledAt);}
        while(position<progress){
            IntervalTimeline.Phase p=timeline.at(position);
            if("complete".equals(p.phase))break;
            long duration=Math.min(p.remainingMs,progress-position);
            if(duration<=0)throw new IllegalStateException("Empty timeline phase");
            ("work".equals(p.phase)?work:rest)[p.step]+=duration;position+=duration;
            if(!p.side192.isEmpty()){
                if("work".equals(p.phase))(p.side192.equals("left")?left:right)[p.step]+=duration;
                else (p.restKind192.equals("side")?sideRest:setRest)[p.step]+=duration;
                segment(p,duration,0);
            }
        }
        sampledAt=now;wasPaused=!running&&!stopped&&!interrupted&&!"complete".equals(timeline.at(position).phase);
    }
    void skipTo(long progress){if(progress<position)throw new IllegalArgumentException("Invalid skip");position=progress;}
    void skipWork(IntervalTimeline.Phase p){if(!p.side192.isEmpty()&&"work".equals(p.phase)){skipped.add(p.step);skippedSides[p.step]|=p.side192.equals("left")?1:2;segment(p,0,0);try{segments.get(p.token).put("skipped",true);}catch(JSONException e){throw new IllegalStateException(e);}}}
    private void segment(IntervalTimeline.Phase p,long duration,long pause){
        if(p.side192.isEmpty())return;
        try{
            JSONObject s=segments.get(p.token);
            if(s==null){s=new JSONObject().put("id",p.token).put("step",p.step).put("round",p.round).put("side",p.side192)
                .put("kind",p.phase.equals("work")?"work":p.restKind192.equals("side")?"sideRest":"setRest")
                .put("durationMs",0).put("pausedMs",0).put("targetMs",p.phaseDuration192);segments.put(p.token,s);}
            s.put("targetMs",p.phaseDuration192).put("durationMs",s.getLong("durationMs")+duration).put("pausedMs",s.getLong("pausedMs")+pause);
            java.util.Iterator<java.util.Map.Entry<String,JSONObject>> it=segments.entrySet().iterator();
            while(it.hasNext()){java.util.Map.Entry<String,JSONObject> entry=it.next();if(entry.getValue().optInt("step")<p.step){compacted.add(entry.getValue().optInt("step"));it.remove();}}
            while(segments.size()>4){java.util.Iterator<java.util.Map.Entry<String,JSONObject>> first=segments.entrySet().iterator();java.util.Map.Entry<String,JSONObject> entry=first.next();compacted.add(entry.getValue().optInt("step"));first.remove();}

        }catch(JSONException e){throw new IllegalStateException(e);}
    }
    JSONObject json(JSONArray plan) throws JSONException {
        JSONArray rows=new JSONArray();long workTotal=0,restTotal=0,pauseTotal=0;boolean detailed=false;
        for(int i=0;i<work.length;i++){
            JSONObject owner=plan.getJSONObject(i);
            JSONObject row=new JSONObject().put("exerciseId",owner.getString("exerciseId")).put("setId",owner.getString("setId"))
                    .put("workMs",work[i]).put("restMs",rest[i]).put("pausedMs",paused[i]);
            if(owner.has("bilateral192")){
                detailed=true;JSONArray parts=new JSONArray();for(JSONObject s:segments.values())if(s.optInt("step")==i)parts.put(new JSONObject(s.toString()));
                row.put("leftWorkMs",left[i]).put("rightWorkMs",right[i]).put("sideRestMs",sideRest[i]).put("setRestMs",setRest[i])
                    .put("segments192",parts).put("segmentsCompacted192",compacted.contains(i)).put("skippedWork192",skipped.contains(i)).put("skippedLeft192",(skippedSides[i]&1)!=0).put("skippedRight192",(skippedSides[i]&2)!=0);
            }
            rows.put(row);
            workTotal+=work[i];restTotal+=rest[i];pauseTotal+=paused[i];
        }
        return new JSONObject().put("schema",detailed?2:1).put("owner","phone").put("positionMs",position)
                .put("workMs",workTotal).put("restMs",restTotal).put("pausedMs",pauseTotal).put("steps",rows);
    }
}
