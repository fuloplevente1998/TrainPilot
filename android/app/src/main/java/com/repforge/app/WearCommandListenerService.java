package com.repforge.app;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class WearCommandListenerService extends WearableListenerService {
    static final String COMMAND_PATH_PREFIX = "/trainpilot/workout-command/";

    private void queue(JSONObject command, android.net.Uri uri) {
        try { WearCommandQueueStore.enqueue(this,command,uri.toString()); }
        catch(Exception ignored) { /* Retain the DataItem for scanning after storage recovers. */ }
    }
    @Override public void onMessageReceived(MessageEvent event) {
        if (!WearWorkoutHandoff.REQUEST.equals(event.getPath()) || event.getData().length > 4096) return;
        try {
            JSONObject request = new JSONObject(new String(event.getData(), StandardCharsets.UTF_8));
            JSONObject snapshot = WearWorkoutHandoff.response(ActiveWorkoutStore.current(this),request,System.currentTimeMillis());
            if (snapshot == null) return;
            // Reply directly as well as repairing the durable snapshot. An unchanged DataItem
            // alone does not produce a new event, so the refresh has its own timestamp.
            Wearable.getMessageClient(this).sendMessage(event.getSourceNodeId(),WearWorkoutHandoff.REPLY,snapshot.toString().getBytes(StandardCharsets.UTF_8));
            PutDataMapRequest map=PutDataMapRequest.create(WearSyncPlugin.ACTIVE_WORKOUT_PATH);
            map.getDataMap().putString("snapshot",snapshot.toString());
            map.getDataMap().putBoolean("active",true);
            map.getDataMap().putLong("revision",snapshot.optLong("revision"));
            map.getDataMap().putLong("refreshedAt",System.currentTimeMillis());
            Wearable.getDataClient(this).putDataItem(map.asPutDataRequest().setUrgent());
        } catch (Exception ignored) {}
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() != DataEvent.TYPE_CHANGED) continue;
            if (event.getDataItem() == null || event.getDataItem().getUri() == null) continue;
            String path = event.getDataItem().getUri().getPath();
            if (WearLiveHealthStore.PATH.equals(path)) {
                try { WearLiveHealthStore.save(this, DataMapItem.fromDataItem(event.getDataItem()).getDataMap().getString("health")); }
                catch (Exception ignored) {}
                continue;
            }
            if (path == null || !path.startsWith(COMMAND_PATH_PREFIX)&&!path.equals("/trainpilot/watch-automatic")) continue;
            try {
                String raw = DataMapItem.fromDataItem(event.getDataItem()).getDataMap().getString("command");
                if (raw == null || raw.isEmpty()) continue;
                JSONObject command=new JSONObject(raw);
                if(WearCommandQueueStore.seen(this,command.optString("commandId"))){Wearable.getDataClient(this).deleteDataItems(event.getDataItem().getUri());continue;}
                if(PhoneWorkoutExecution.compatible(command)){
                    android.net.Uri uri=event.getDataItem().getUri();
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
                        try{
                            JSONObject result=PhoneWorkoutExecution.execute(this,command);
                            if(java.util.Arrays.asList("applied","duplicate").contains(result.optString("status"))&&WearCommandQueueStore.ack(this,command.optString("commandId")))
                                Wearable.getDataClient(this).deleteDataItems(uri);
                            else queue(command,uri);
                        }catch(Exception error){queue(command,uri);}
                    });continue;
                }
                if("automaticControl".equals(command.optString("action"))){
                    android.net.Uri uri=event.getDataItem().getUri();
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
                    if(WearCommandQueueStore.seen(this,command.optString("commandId"))){
                        Wearable.getDataClient(this).deleteDataItems(uri);
                        return;
                    }
                    JSONObject timer=IntervalTimerService.snapshot(this),active=ActiveWorkoutStore.current(this);
                    String action=command.optString("value");
                    if(active!=null&&command.optJSONObject("automation")!=null&&active.optString("workoutId").equals(command.optString("workoutId"))&&
                        timer.optString("key").equals(command.optJSONObject("automation")==null?"":command.optJSONObject("automation").optString("key"))&&
                        java.util.Arrays.asList("pause","resume","skipRest").contains(action)){
                        try {
                            long expectedPhase=command.optJSONObject("automation").optLong("phaseNumber");
                            IntervalTimerService.command(this,timer.optString("key"),expectedPhase==timer.optLong("phaseNumber")?action:"status");
                            // Keep the DataItem available for replay if a durable receipt fails.
                            if(WearCommandQueueStore.ack(this,command.optString("commandId")))
                                Wearable.getDataClient(this).deleteDataItems(uri);
                            return;
                        }
                        catch(Exception ignored){}
                    }
                        queue(command,uri);
                    });continue;
                }
                WearCommandQueueStore.enqueue(this, command, event.getDataItem().getUri().toString());
            } catch (Exception ignored) {
                // Invalid or stale watch commands must never affect the phone workout.
            }
        }
    }
}
