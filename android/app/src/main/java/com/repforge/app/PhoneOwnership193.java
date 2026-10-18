package com.repforge.app;

import android.content.Context;
import com.repforge.shared.OwnershipJournal193;
import com.repforge.shared.OwnershipTransport193;
import com.repforge.shared.WorkoutOwnership193;
import com.google.android.gms.wearable.Wearable;
import org.json.JSONObject;

/** Main-looper adapter. The WebView supplies its fresh, paused canonical draft before an offer. */
final class PhoneOwnership193 {
    private static OwnershipJournal193 journal;
    private static Context journalContext;
    private static long scannedAt;
    static synchronized OwnershipJournal193 journal(Context context) {
        if(journal==null||journalContext!=context.getApplicationContext()) {
            Context app=context.getApplicationContext();
            journalContext=app;
            journal=new OwnershipJournal193(app,new OwnershipJournal193.Host() {
                public JSONObject current() { return ActiveWorkoutStore.current(app); }
                public void install(JSONObject snapshot) throws Exception { ActiveWorkoutStore.saveOwned(app,snapshot);PhoneGpsRoute193.authorizeOwned(app,snapshot); }
            });
        }
        return journal;
    }
    static void receive(Context context,JSONObject message,String origin,String localNode) throws Exception {
        OwnershipJournal193 store=journal(context);
        store.recover();store.receive(message,origin,localNode,System.currentTimeMillis());store.recover();
        OwnershipTransport193.send(context,store);
    }
    static void prepare(Context context,JSONObject frozen,String requestId,String localNode) throws Exception {
        OwnershipJournal193 store=journal(context);JSONObject request=store.read().optJSONObject("request");
        if(request==null||!requestId.equals(request.optString("id"))||!localNode.equals(request.optString("target"))
                ||!request.optString("workoutId").equals(frozen.optString("workoutId"))||request.optLong("epoch")!=frozen.optLong("ownerEpoch193")
                ||!"phone".equals(frozen.optString("timerOwner","phone"))||!frozen.optBoolean("workoutPaused"))
            throw new IllegalStateException("The workout transfer changed");
        JSONObject active=ActiveWorkoutStore.current(context);
        long now=System.currentTimeMillis();
        if(active==null||!active.optBoolean("active",true)||!frozen.optString("workoutId").equals(active.optString("workoutId"))
                ||WorkoutOwnership193.pending(active)||!"phone".equals(active.optString("timerOwner","phone"))
                ||active.optLong("ownerEpoch193")!=request.optLong("epoch")
                ||now-request.optLong("createdAt")>30000||now<request.optLong("createdAt")-5000)
            throw new IllegalStateException("The workout transfer changed");
        if(frozen.optInt("ownershipProtocol193")!=1)throw new IllegalStateException("Update both apps before transferring a workout");
        JSONObject gps=DistanceTrackingService.snapshot(context);
        if(gps.optBoolean("active"))throw new IllegalStateException("Finish recording GPS values before transfer");
        JSONObject status=PhoneWorkoutExecution.status(context);
        PhoneTimerCheckpoint193.verifyCanonical(frozen,status);
        JSONObject prepared=new JSONObject(frozen.toString());
        prepared.remove("timerCheckpoint193");
        JSONObject checkpoint=PhoneTimerCheckpoint193.create(prepared,status.optJSONObject("timer"));
        if(checkpoint!=null) {
            JSONObject frame=status.optJSONObject("snapshot"),logs=frame==null?null:frame.optJSONObject("timeLogs162");
            logs=logs==null?new JSONObject():new JSONObject(logs.toString());
            JSONObject timer=status.getJSONObject("timer"),log=PhoneOwnershipResume193.localLog(timer,timer.getJSONObject("ledger"));
            logs.put(timer.getString("key"),log.put("key",timer.getString("key")));
            checkpoint.put("timeLogs193",logs);prepared.put("timerCheckpoint193",checkpoint);
        }
        PhoneWorkoutExecution.store(context).suspendCommands(true);
        IntervalTimerService.stopForOwnership(context,frozen.getString("workoutId"));
        store.prepare(prepared,localNode,System.currentTimeMillis());
        OwnershipTransport193.send(context,store);
    }
    static JSONObject status(Context context) throws Exception {
        OwnershipJournal193 store=journal(context);store.recover();OwnershipTransport193.send(context,store);
        long now=android.os.SystemClock.elapsedRealtime();
        if(now-scannedAt>5000) {
            scannedAt=now;
            Wearable.getDataClient(context).getDataItems().addOnSuccessListener(items->{
                try { for(var item:items)OwnershipTransport193.receive(context,item,(message,origin,local)->receive(context,message,origin,local)); }
                finally { items.release(); }
            });
        }
        return new JSONObject().put("state",store.read()).put("snapshot",ActiveWorkoutStore.current(context));
    }
    private static JSONObject returned(Context context,OwnershipJournal193 store)throws Exception {
        JSONObject proof=store.read().optJSONObject("snapshot"),current=ActiveWorkoutStore.current(context);
        JSONObject h=WorkoutOwnership193.transfer(proof),live=WorkoutOwnership193.transfer(current);
        if(!WorkoutOwnership193.owns(proof,"phone")||h==null||!"watch".equals(h.optString("source"))||!"phone".equals(h.optString("target"))
                ||!WorkoutOwnership193.owns(current,"phone")||!proof.optString("workoutId").equals(current.optString("workoutId"))
                ||WorkoutOwnership193.epoch(proof)!=WorkoutOwnership193.epoch(current)||live==null||!h.optString("id").equals(live.optString("id")))
            throw new IllegalStateException("The returned workout changed");
        return current;
    }
    static JSONObject prepareResume(Context context)throws Exception {
        OwnershipJournal193 journal=journal(context);
        synchronized(journal) {
            journal.recover();JSONObject current=returned(context,journal);
            JSONObject frame=PhoneWorkoutExecution.store(context).importOwnership193(current,System.currentTimeMillis());
            return new JSONObject().put("snapshot",frame);
        }
    }
    static JSONObject installResume(Context context,String requestId,long epoch)throws Exception {
        OwnershipJournal193 journal=journal(context);
        synchronized(journal) {
            JSONObject current=returned(context,journal),h=WorkoutOwnership193.transfer(current);
            if(!requestId.equals(h.optString("id"))||epoch!=h.optLong("epoch"))throw new IllegalStateException("The returned workout changed");
            PhoneWorkoutExecutionStore db=PhoneWorkoutExecution.store(context);JSONObject frame=db.snapshot();
            JSONObject marker=frame==null?null:frame.optJSONObject("ownershipImport193");
            if(marker==null||!requestId.equals(marker.optString("id"))||epoch!=marker.optLong("epoch")||!frame.optBoolean("workoutPaused"))
                throw new IllegalStateException("Prepare the returned workout before continuing");
            JSONObject transition=frame.optJSONObject("transition"),plan=transition==null?null:transition.optJSONObject("plan"),timer=null;
            if(plan!=null) {
                JSONObject latest=frame.optJSONObject("latestTimer162");
                if(latest!=null&&latest.optString("key").equals(plan.optString("key"))&&latest.optLong("generation")==frame.optLong("generation"))plan=latest;
                timer=IntervalTimerService.installOwnership193(plan);
                db.markTransition(plan.getString("key"),"started");
            }
            frame=db.ownershipReady193(requestId,epoch);
            return new JSONObject().put("snapshot",frame).put("timer",timer==null?JSONObject.NULL:timer);
        }
    }
    static boolean publicationAllowed(Context context,JSONObject incoming) throws Exception {
        JSONObject saved=journal(context).read().optJSONObject("snapshot");
        if(saved==null||!saved.optString("workoutId").equals(incoming.optString("workoutId")))return true;
        return saved.optBoolean("active",true)&&!WorkoutOwnership193.pending(saved)&&!WorkoutOwnership193.stale(saved,incoming);
    }
    static boolean phoneAllowed(Context context,String workoutId) {
        try {
            JSONObject saved=journal(context).read().optJSONObject("snapshot");
            return saved==null||!workoutId.equals(saved.optString("workoutId"))||WorkoutOwnership193.owns(saved,"phone");
        }catch(Exception error){return false;}
    }
    static boolean commandAllowed(Context context,JSONObject command) throws Exception {
            JSONObject current=ActiveWorkoutStore.current(context);
            if(current==null||!current.optString("workoutId").equals(command.optString("workoutId")))return true;
            if(java.util.Arrays.asList("healthSummary","saveWorkoutFeedback","requestHome").contains(command.optString("action")))return true;
            if(!current.optBoolean("active",true))return false;
            JSONObject saved=journal(context).read().optJSONObject("snapshot");
            if(saved==null||!current.optString("workoutId").equals(saved.optString("workoutId")))return command.optLong("ownerEpoch193")==0;
            return !WorkoutOwnership193.pending(saved)&&command.optLong("ownerEpoch193")==WorkoutOwnership193.epoch(saved);
    }
}
