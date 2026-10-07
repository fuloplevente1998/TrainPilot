package com.repforge.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import org.json.JSONObject;

@CapacitorPlugin(name = "WearSync")
public class WearSyncPlugin extends Plugin {
    static final String ACTIVE_WORKOUT_PATH = "/trainpilot/active-workout";

    @PluginMethod
    public void publish(PluginCall call) {
        JSObject input = call.getObject("snapshot");
        if (input == null) {
            call.reject("Missing workout snapshot.");
            return;
        }
        try {
            JSONObject snapshot = ActiveWorkoutStore.save(getContext(), input);
            publishSnapshot(snapshot, call);
        } catch (Exception e) {
            call.reject("Unable to prepare Wear workout snapshot.", e);
        }
    }

    @PluginMethod
    public void clear(PluginCall call) {
        try {
            publishSnapshot(ActiveWorkoutStore.clear(getContext()), call);
        } catch (Exception e) {
            call.reject("Unable to clear Wear workout snapshot.", e);
        }
    }

    @PluginMethod
    public void status(PluginCall call) {
        JSObject result = new JSObject();
        JSONObject current = ActiveWorkoutStore.current(getContext());
        result.put("supported", true);
        result.put("snapshot", current == null ? JSONObject.NULL : current);
        call.resolve(result);
    }

    private void publishSnapshot(JSONObject snapshot, PluginCall call) {
        PutDataMapRequest mapRequest = PutDataMapRequest.create(ACTIVE_WORKOUT_PATH);
        mapRequest.getDataMap().putBoolean("active", snapshot.optBoolean("active", true));
        mapRequest.getDataMap().putLong("revision", snapshot.optLong("revision", 0L));
        mapRequest.getDataMap().putString("snapshot", snapshot.toString());

        PutDataRequest request = mapRequest.asPutDataRequest().setUrgent();
        Wearable.getDataClient(getContext())
                .putDataItem(request)
                .addOnSuccessListener(item -> {
                    JSObject result = new JSObject();
                    result.put("revision", snapshot.optLong("revision", 0L));
                    result.put("uri", item.getUri().toString());
                    call.resolve(result);
                })
                .addOnFailureListener(error -> call.reject("Wear Data Layer sync failed.", error));
    }
}
