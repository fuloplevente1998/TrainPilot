package com.repforge.app;

import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
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
    static final String AUTOMATIC_COMMAND_PREFIX = "/trainpilot/automatic-command/";

    private interface ExecutionCall { JSONObject run() throws Exception; }
    private void execution(PluginCall call, ExecutionCall work) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
            try { call.resolve(new JSObject(work.run().toString())); } catch(Exception error) { call.reject(error.getMessage(), error); }
        });
    }
    @PluginMethod public void executionStatus(PluginCall call) { execution(call,()->PhoneWorkoutExecution.status(getContext())); }
    @PluginMethod public void prepareExecution(PluginCall call) { execution(call,()->{
        PhoneWorkoutExecutionStore db=PhoneWorkoutExecution.store(getContext());
        db.suspendCommands(true); // An unsuccessful CAS cannot leave stale prepared controls available.
        JSONObject frame=db.prepare(call.getObject("snapshot"),call.getData().getLong("expectedRevision"));
        db.suspendCommands(false);return new JSONObject().put("snapshot",frame);
    }); }
    @PluginMethod public void amendExecution(PluginCall call){execution(call,()->{
        PhoneWorkoutExecutionStore db=PhoneWorkoutExecution.store(getContext());
        JSONObject frame=db.amend(call.getObject("snapshot"),call.getData().getLong("expectedRevision"),call.getObject("bases"));
        db.suspendCommands(false);db.beginManualWork(frame.getString("workoutId"),frame.getLong("generation"),android.os.SystemClock.elapsedRealtime());return new JSONObject().put("snapshot",frame);
    });}
    @PluginMethod public void executeCommand(PluginCall call) { execution(call,()->PhoneWorkoutExecution.execute(getContext(),call.getObject("command"))); }
    @PluginMethod public void acknowledgeExecution(PluginCall call) { execution(call,()->{
        boolean ok=PhoneWorkoutExecution.store(getContext()).acknowledgeEvents(call.getString("workoutId"),call.getData().getLong("generation"),call.getData().getLong("sequence"));
        if(!ok)throw new IllegalStateException("Native event acknowledgement changed owner");return new JSONObject();
    }); }
    @PluginMethod public void blockExecution(PluginCall call) { execution(call,()->{
        PhoneWorkoutExecution.store(getContext()).setRestoreBlocked(call.getBoolean("blocked",true));return new JSONObject();
    }); }
    @PluginMethod public void suspendExecution(PluginCall call) { execution(call,()->{
        PhoneWorkoutExecution.store(getContext()).suspendCommands(call.getBoolean("suspended",true));return new JSONObject();
    }); }
    @PluginMethod public void setWorkoutPause(PluginCall call) { execution(call,()->{
        PhoneWorkoutExecution.setWorkoutPause(getContext(),call.getObject("snapshot"));return new JSONObject();
    }); }
    @PluginMethod public void closeExecution(PluginCall call) { execution(call,()->{
        PhoneWorkoutExecutionStore db=PhoneWorkoutExecution.store(getContext());
        db.setRestoreBlocked(false); // Close and release the finish fence in this same main-looper operation.
        boolean ok=db.closeExecution(call.getString("workoutId"),call.getData().getLong("generation"),call.getData().getLong("executionRevision"));
        if(!ok)throw new IllegalStateException("Reconcile native events before closing execution");IntervalTimerService.closeWorkout(call.getString("workoutId"),call.getData().getLong("generation"));return new JSONObject();
    }); }
    @PluginMethod public void discardExecution(PluginCall call) { execution(call,()->{
        if (!call.getBoolean("confirmed",false)) throw new IllegalArgumentException("Discard must be confirmed");
        PhoneWorkoutExecutionStore db=PhoneWorkoutExecution.store(getContext());
        db.setRestoreBlocked(false);
        boolean ok=db.discardExecution(call.getString("workoutId"),call.getData().getLong("generation"),call.getData().getLong("executionRevision"));
        if(!ok)throw new IllegalStateException("Discard execution identity changed");
        IntervalTimerService.closeWorkout(call.getString("workoutId"),call.getData().getLong("generation"));return new JSONObject();
    }); }

    @PluginMethod public void automaticCommand(PluginCall call) {
        JSObject input=call.getObject("command");
        if(input==null||input.optString("commandId").isEmpty()||input.optString("workoutId").isEmpty()){call.reject("Missing automatic command identity.");return;}
        JSONObject current=ActiveWorkoutStore.current(getContext());
        if(current==null||!current.optString("workoutId").equals(input.optString("workoutId"))){call.reject("Workout changed.");return;}
        PutDataMapRequest map=PutDataMapRequest.create(AUTOMATIC_COMMAND_PREFIX+input.optString("commandId"));
        map.getDataMap().putString("command",input.toString());
        map.getDataMap().putLong("createdAt",System.currentTimeMillis());
        if("start".equals(input.optString("action"))||"resume".equals(input.optString("action"))||"record".equals(input.optString("action"))||
            "toggle".equals(input.optString("action"))&&input.optBoolean("enabled")){
            if(getActivity()==null||!getActivity().hasWindowFocus()){call.reject("The phone workout must be visible.");return;}
            Wearable.getDataClient(getContext()).putDataItem(map.asPutDataRequest().setUrgent());
            Wearable.getNodeClient(getContext()).getConnectedNodes().addOnSuccessListener(nodes->{
                if(nodes.size()!=1){call.reject("One connected watch is required.");return;}
                Intent intent=new Intent(Intent.ACTION_VIEW).setData(new Uri.Builder().scheme("trainpilot").authority("wear")
                    .path("/automatic-command").appendQueryParameter("command",input.toString()).build()).addCategory(Intent.CATEGORY_BROWSABLE);
                var task=new RemoteActivityHelper(getContext(),ContextCompat.getMainExecutor(getContext())).startRemoteActivity(intent,nodes.get(0).getId());
                task.addListener(()->{try{task.get();call.resolve();}catch(Exception error){call.reject("Unable to open the watch timer.",error);}},ContextCompat.getMainExecutor(getContext()));
            }).addOnFailureListener(error->call.reject("Unable to reach the watch.",error));
            return;
        }
        put(map,0,call);
    }

    @PluginMethod
    public void publish(PluginCall call) {
        JSObject input = call.getObject("snapshot");
        if (input == null) { call.reject("Missing workout snapshot."); return; }
        new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
            try {
                JSONObject value=input;
                JSONObject prior=ActiveWorkoutStore.current(getContext());
                if(prior!=null&&input.optString("workoutId").equals(prior.optString("workoutId"))&&input.optLong("pauseRevision")<prior.optLong("pauseRevision"))
                    for(String key:new String[]{"workoutPaused","pauseRevision","activeIntervals"})if(prior.has(key))value.put(key,prior.get(key));
                if(input.optInt("nativeExecutionProtocol")==2){
                    JSONObject frame=PhoneWorkoutExecution.store(getContext()).snapshot();
                    if(frame==null||!frame.optBoolean("active")||!input.optString("workoutId").equals(frame.optString("workoutId"))
                        || input.optLong("generation")!=frame.optLong("generation")||input.optLong("executionRevision")!=frame.optLong("executionRevision"))
                        throw new IllegalStateException("Reconcile native execution before publishing");
                    value=PhoneWorkoutExecution.display(getContext());
                }
                publishSnapshot(ActiveWorkoutStore.save(getContext(), value), call);
            } catch(Exception e){call.reject("Unable to prepare Wear workout snapshot.",e);}
        });
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
        long requestedAt = SystemClock.elapsedRealtime();
        Wearable.getNodeClient(getContext()).getConnectedNodes()
                .addOnSuccessListener(nodes -> ContextCompat.getMainExecutor(getContext()).execute(() -> {
                    JSONObject current = ActiveWorkoutStore.current(getContext());
                    if (SystemClock.elapsedRealtime() - requestedAt > 15_000L) {
                        call.reject("The watch start request expired."); return;
                    }
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

    @PluginMethod public void bilateralCapability192(PluginCall call) {
        Wearable.getNodeClient(getContext()).getConnectedNodes().addOnSuccessListener(nodes->{
            if(nodes.isEmpty()){JSObject result=new JSObject();result.put("supported",true);result.put("connectedWatches",0);call.resolve(result);return;}
            Wearable.getCapabilityClient(getContext()).getCapability("trainpilot_bilateral192",com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE)
                .addOnSuccessListener(capability->{boolean supported=true;for(com.google.android.gms.wearable.Node node:nodes){boolean found=false;for(com.google.android.gms.wearable.Node peer:capability.getNodes())if(peer.getId().equals(node.getId()))found=true;supported&=found;}
                    JSObject result=new JSObject();result.put("supported",supported);result.put("connectedWatches",nodes.size());call.resolve(result);})
                .addOnFailureListener(error->call.reject("Unable to check watch support"));
        }).addOnFailureListener(error->call.reject("Unable to check watch connection"));
    }
    @PluginMethod
    public void status(PluginCall call) {
        JSObject result = new JSObject();
        JSONObject current = ActiveWorkoutStore.current(getContext());
        result.put("supported", true);
        result.put("snapshot", current == null ? JSONObject.NULL : current);
        result.put("pendingCommands", WearCommandQueueStore.pending(getContext()).length());
        result.put("liveHealth", WearLiveHealthStore.current(getContext()));
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
                            if (WearLiveHealthStore.PATH.equals(path)) {
                                try { WearLiveHealthStore.save(getContext(), DataMapItem.fromDataItem(item).getDataMap().getString("health")); }
                                catch (Exception ignored) {}
                                continue;
                            }
                            if (path == null || !path.startsWith(WearCommandListenerService.COMMAND_PATH_PREFIX)&&!path.equals("/trainpilot/watch-automatic")) continue;
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
        JSONObject command=null;
        JSONArray pending=WearCommandQueueStore.pending(getContext());
        for(int i=0;i<pending.length();i++)if(pending.optJSONObject(i)!=null&&commandId.equals(pending.optJSONObject(i).optString("commandId")))command=pending.optJSONObject(i);
        if(command==null)command=WearCommandQueueStore.receipt(getContext(),commandId);
        if(command==null){
            if(!WearCommandQueueStore.ack(getContext(),commandId)){call.reject("Unable to persist the watch command receipt.");return;}
            deleteCommandDataItem(commandId);
        }else{
            String status=call.getString("status","processed");
            if(!java.util.Arrays.asList("applied","saved","discarded","error","superseded","processed").contains(status)){call.reject("Invalid command result.");return;}
            if(!WearCommandQueueStore.ack(getContext(),command,status)){call.reject("Unable to persist the watch command receipt.");return;}
            WearCommandReceipts.replay(getContext(),command,null);
            deleteCommandDataItem(commandId);
        }
        JSObject result = new JSObject();
        result.put("commandId", commandId);
        call.resolve(result);
    }

    private void resolvePendingCommands(PluginCall call, String scanError) {
        JSObject result = new JSObject();
        JSONArray commands = WearCommandQueueStore.pending(getContext());
        JSONObject current = ActiveWorkoutStore.current(getContext());
        result.put("commands", commands);
        result.put("liveHealth", WearLiveHealthStore.current(getContext()));
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
                            if (item != null && item.getUri() != null && (expectedPath.equals(item.getUri().getPath())||
                                "/trainpilot/watch-automatic".equals(item.getUri().getPath())&&
                                sameWatchStateCommand(item,commandId))) {
                                try { WearCommandReceipts.replay(getContext(), new JSONObject().put("commandId",commandId), item.getUri()); } catch(Exception ignored) {}
                            }
                        }
                    } finally {
                        items.release();
                    }
                });
    }
    private boolean sameWatchStateCommand(DataItem item,String id){
        try {return new JSONObject(DataMapItem.fromDataItem(item).getDataMap().getString("command")).optString("commandId").equals(id);}
        catch(Exception ignored){return false;}
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
