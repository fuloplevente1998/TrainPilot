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
                public void install(JSONObject snapshot) throws Exception { ActiveWorkoutStore.saveOwned(app,snapshot); }
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
                ||!"phone".equals(frozen.optString("timerOwner","phone"))||!frozen.optBoolean("workoutPaused"))
            throw new IllegalStateException("The workout transfer changed");
        JSONObject gps=DistanceTrackingService.snapshot(context);
        if(gps.optBoolean("active"))throw new IllegalStateException("Finish recording GPS values before transfer");
        PhoneWorkoutExecution.store(context).suspendCommands(true);
        IntervalTimerService.stopForOwnership(context,frozen.getString("workoutId"));
        store.prepare(frozen,localNode,System.currentTimeMillis());
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
            JSONObject saved=journal(context).read().optJSONObject("snapshot");
            if(saved==null||!current.optString("workoutId").equals(saved.optString("workoutId")))return command.optLong("ownerEpoch193")==0;
            return !WorkoutOwnership193.pending(saved)&&command.optLong("ownerEpoch193")==WorkoutOwnership193.epoch(saved);
    }
}
