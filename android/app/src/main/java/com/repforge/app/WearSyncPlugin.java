package com.repforge.app;

import android.content.Intent;
import android.net.Uri;
import androidx.core.content.ContextCompat;
import androidx.wear.remote.interactions.RemoteActivityHelper;
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
    static final String WATCH_HOME_PATH = "/trainpilot/watch-home";

    @PluginMethod
    public void publish(PluginCall call) {
        JSObject input = call.getObject("snapshot");
        if (input == null) { call.reject("Missing workout snapshot."); return; }
        try { publishSnapshot(ActiveWorkoutStore.save(getContext(), input), call); }
        catch (Exception e) { call.reject("Unable to prepare Wear workout snapshot.", e); }
    }

    @PluginMethod
    public void publishHome(PluginCall call) {
        JSObject input = call.getObject("snapshot");
        if (input == null) { call.reject("Missing Wear home snapshot."); return; }
        try { publishJson(WATCH_HOME_PATH, new JSONObject(input.toString()), call); }
        catch (Exception e) { call.reject("Unable to prepare Wear home snapshot.", e); }
    }

    @PluginMethod
    public void clear(PluginCall call) {
        try { publishSnapshot(ActiveWorkoutStore.clear(getContext()), call); }
        catch (Exception e) { call.reject("Unable to clear Wear workout snapshot.", e); }
    }

    /** Called once after an explicit phone workout start, after its urgent publish. */
    @PluginMethod
    public void startWatchMeasurement(PluginCall call) {
        String requestedId = call.getString("workoutId", "");
        Wearable.getNodeClient(getContext()).getConnectedNodes()
                .addOnSuccessListener(nodes -> ContextCompat.getMainExecutor(getContext()).execute(() -> {
                    JSONObject current = ActiveWorkoutStore.current(getContext());
                    if (getActivity() == null || !getActivity().hasWindowFocus()) {
                        call.reject("The phone workout must be visible to open the watch."); return;
                    }
                    if (current == null || requestedId == null || requestedId.isEmpty()
                            || !current.optBoolean("active", false)
                            || !requestedId.equals(current.optString("workoutId"))) {
                        call.reject("The requested workout is no longer active."); return;
                    }
                    // Avoid starting duplicate measurements on several paired watches.
                    if (nodes.size() != 1) {
                        call.reject(nodes.isEmpty() ? "No watch is connected." : "More than one watch is connected."); return;
                    }
                    Uri uri = new Uri.Builder().scheme("trainpilot").authority("wear")
                            .path("/start-measurement")
                            .appendQueryParameter("workoutId", requestedId)
                            .appendQueryParameter("revision", Long.toString(current.optLong("revision", 0L)))
                            .build();
                    Intent intent = new Intent(Intent.ACTION_VIEW).setData(uri).addCategory(Intent.CATEGORY_BROWSABLE);
                    try {
                        var task = new RemoteActivityHelper(getContext(), ContextCompat.getMainExecutor(getContext()))
                                .startRemoteActivity(intent, nodes.get(0).getId());
                        task.addListener(() -> {
                            try {
                                task.get();
                                JSObject result = new JSObject();
                                // Opening the Activity is not proof that the sensors are recording.
                                result.put("opened", true);
                                result.put("workoutId", requestedId);
                                call.resolve(result);
                            } catch (Exception error) { call.reject("Unable to open TrainPilot on the watch.", error); }
                        }, ContextCompat.getMainExecutor(getContext()));
                    } catch (Exception error) { call.reject("Unable to open TrainPilot on the watch.", error); }
                }))
                .addOnFailureListener(error -> call.reject("Unable to reach the watch.", error));
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
                                WearCommandQueueStore.enqueue(getContext(), new JSONObject(raw), item.getUri().toString());
                            } catch (Exception ignored) {
                                // One malformed/stale DataItem must not block the rest.
                            }
                        }
                        resolvePendingCommands(call, null);
                    } finally {
                        items.release();
                    }
                })
                .addOnFailureListener(error -> resolvePendingCommands(call, error == null ? "scan failed" : error.getMessage()));
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
                            if (item != null && item.getUri() != null && expectedPath.equals(item.getUri().getPath())) {
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
        put(mapRequest, snapshot.optLong("revision", 0L), call);
    }

    private void publishJson(String path, JSONObject snapshot, PluginCall call) {
        PutDataMapRequest mapRequest = PutDataMapRequest.create(path);
        mapRequest.getDataMap().putString("snapshot", snapshot.toString());
        mapRequest.getDataMap().putLong("publishedAt", System.currentTimeMillis());
        put(mapRequest, 0L, call);
    }

    private void put(PutDataMapRequest mapRequest, long revision, PluginCall call) {
        PutDataRequest request = mapRequest.asPutDataRequest().setUrgent();
        Wearable.getDataClient(getContext()).putDataItem(request)
                .addOnSuccessListener(item -> {
                    JSObject result = new JSObject();
                    result.put("revision", revision);
                    result.put("uri", item.getUri().toString());
                    call.resolve(result);
                })
                .addOnFailureListener(error -> call.reject("Wear Data Layer sync failed.", error));
    }
}
