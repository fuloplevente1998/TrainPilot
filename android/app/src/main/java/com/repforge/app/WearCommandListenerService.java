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
            if (path == null || !path.startsWith(COMMAND_PATH_PREFIX)) continue;
            try {
                String raw = DataMapItem.fromDataItem(event.getDataItem()).getDataMap().getString("command");
                if (raw == null || raw.isEmpty()) continue;
                WearCommandQueueStore.enqueue(this, new JSONObject(raw), event.getDataItem().getUri().toString());
            } catch (Exception ignored) {
                // Invalid or stale watch commands must never affect the phone workout.
            }
        }
    }
}
