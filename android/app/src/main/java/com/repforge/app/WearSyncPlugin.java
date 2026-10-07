package com.repforge.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import org.json.JSONArray;
import org.json.JSONObject;

@CapacitorPlugin(name = "WearSync")
public class WearSyncPlugin extends Plugin {
    static final String ACTIVE_WORKOUT_PATH = "/trainpilot/active-workout";

    @PluginMethod
    public void publish(PluginCall call) {
        JSObject input = call.getObject("snapshot");
        if (input == null) { call.reject("Missing workout snapshot."); return; }
        try { publishSnapshot(ActiveWorkoutStore.save(getContext(), input), call); }
        catch (Exception e) { call.reject("Unable to prepare Wear workout snapshot.", e); }
    }

    @PluginMethod
    public void clear(PluginCall call) {
        try { publishSnapshot(ActiveWorkoutStore.clear(getContext()), call); }
        catch (Exception e) { call.reject("Unable to clear Wear workout snapshot.", e); }
    }

    @PluginMethod
    public void status(PluginCall call) {
        JSObject result = new JSObject();
        JSONObject current = ActiveWorkoutStore.current(getContext());
        result.put("supported", true);
        result.put("snapshot", current == null ? JSONObject.NULL : current);
        result.put("pendingCommands", WearCommandQueueStore.pending(getContext()).length());
        call.resolve(result);
    }

    @PluginMethod
    public void pendingCommands(PluginCall call) {
        // Do not rely only on WearableListenerService delivery. DataItems are
        // persistent, so scan the local Data Layer view as well before draining
        // the native queue. This makes foreground phone sync self-healing.
        Wearable.getDataClient(getContext()).getDataItems()
                .addOnSuccessListener(items -> {
                    try {
                        for (DataItem item : items) {
                            if (item == null || item.getUri() == null) continue;
                            String path = item.getUri().getPath();
                            if (path == null || !path.startsWith(WearCommandListenerService.COMMAND_PATH_PREFIX)) continue;
                            try {
                                String raw = DataMapItem.fromDataItem(item).getDataMap().getString("command");
                                if (raw == null || raw.isEmpty()) continue;
                                WearCommandQueueStore.enqueue(
                                        getContext(),
                                        new JSONObject(raw),
                                        item.getUri().toString()
                                );
                            } catch (Exception ignored) {
                                // One malformed/stale DataItem must not block the rest.
                            }
                        }
                        resolvePendingCommands(call, null);
                    } finally {
                        items.release();
                    }
                })
                .addOnFailureListener(error -> {
                    // Listener-fed queued commands can still be processed even if
                    // an explicit Data Layer scan is temporarily unavailable.
                    resolvePendingCommands(call, error == null ? "scan failed" : error.getMessage());
                });
    }

    @PluginMethod
    public void ackCommand(PluginCall call) {
        String commandId = call.getString("commandId", "");
        if (commandId == null || commandId.isEmpty()) { call.reject("Missing commandId."); return; }
        WearCommandQueueStore.ack(getContext(), commandId);
        deleteCommandDataItem(commandId);
        JSObject result = new JSObject();
        result.put("commandId", commandId);
        call.resolve(result);
    }

    private void resolvePendingCommands(PluginCall call, String scanError) {
        JSObject result = new JSObject();
        JSONArray commands = WearCommandQueueStore.pending(getContext());
        JSONObject current = ActiveWorkoutStore.current(getContext());
        result.put("commands", commands);
        result.put("revision", current == null ? 0L : current.optLong("revision", 0L));
        if (scanError != null && !scanError.isEmpty()) result.put("scanError", scanError);
        call.resolve(result);
    }

    private void deleteCommandDataItem(String commandId) {
        String expectedPath = WearCommandListenerService.COMMAND_PATH_PREFIX + commandId;
        Wearable.getDataClient(getContext()).getDataItems()
                .addOnSuccessListener(items -> {
                    try {
                        for (DataItem item : items) {
                            if (item != null && item.getUri() != null
                                    && expectedPath.equals(item.getUri().getPath())) {
                                Wearable.getDataClient(getContext()).deleteDataItems(item.getUri());
                            }
                        }
                    } finally {
                        items.release();
                    }
                });
    }

    private void publishSnapshot(JSONObject snapshot, PluginCall call) {
        PutDataMapRequest mapRequest = PutDataMapRequest.create(ACTIVE_WORKOUT_PATH);
        mapRequest.getDataMap().putBoolean("active", snapshot.optBoolean("active", true));
        mapRequest.getDataMap().putLong("revision", snapshot.optLong("revision", 0L));
        mapRequest.getDataMap().putString("snapshot", snapshot.toString());
        PutDataRequest request = mapRequest.asPutDataRequest().setUrgent();
        Wearable.getDataClient(getContext()).putDataItem(request)
                .addOnSuccessListener(item -> {
                    JSObject result = new JSObject();
                    result.put("revision", snapshot.optLong("revision", 0L));
                    result.put("uri", item.getUri().toString());
                    call.resolve(result);
                })
                .addOnFailureListener(error -> call.reject("Wear Data Layer sync failed.", error));
    }
}
