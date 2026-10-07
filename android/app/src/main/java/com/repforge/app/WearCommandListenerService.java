package com.repforge.app;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import org.json.JSONObject;

public class WearCommandListenerService extends WearableListenerService {
    static final String COMMAND_PATH_PREFIX = "/trainpilot/workout-command/";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() != DataEvent.TYPE_CHANGED) continue;
            if (event.getDataItem() == null || event.getDataItem().getUri() == null) continue;
            String path = event.getDataItem().getUri().getPath();
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
