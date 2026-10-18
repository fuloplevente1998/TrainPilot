package com.repforge.app;

import android.content.Context;
import android.net.Uri;
import com.google.android.gms.wearable.Wearable;
import com.repforge.shared.GpsRouteLedger193;
import com.repforge.shared.GpsRoutePacket193;
import com.repforge.shared.GpsRouteSpool193;
import org.json.JSONArray;
import org.json.JSONObject;

/** GPS chunks bypass the generic command queue and WebView; receipts contain references only. */
final class PhoneGpsRoute193 {
    static final String ACTION="gpsRouteChunk193";
    private PhoneGpsRoute193() {}
    private static void require(boolean value) { if(!value)throw new IllegalArgumentException("Invalid watch route delivery"); }
    static String startOrigin(JSONObject command) {
        JSONObject root=command.optJSONObject("finalSnapshot");
        if(root==null||root.optInt("ownershipProtocol193")!=1||root.optLong("ownerEpoch193")!=0||root.has("handoff193")
            ||!"watch".equals(root.optString("timerOwner"))||!root.optString("workoutId").equals(command.optString("workoutId")))return "";
        if(!java.util.Arrays.asList("startWorkout","startQuickWorkout","addQuickExercise","finishWorkout","discardWorkout","watchAutomationState").contains(command.optString("action")))return "";
        Uri uri=Uri.parse(command.optString("_uri"));String node=uri.getHost();
        return "wear".equals(uri.getScheme())&&GpsRoutePacket193.id(node)&&
            ((WearCommandListenerService.COMMAND_PATH_PREFIX+command.optString("commandId")).equals(uri.getPath())
                ||"watchAutomationState".equals(command.optString("action"))&&"/trainpilot/watch-automatic".equals(uri.getPath()))?node:"";
    }
    static void captureOrigin(Context context,JSONObject snapshot) throws Exception {
        snapshot.remove("watchSourceNode193");
        if(snapshot.optInt("ownershipProtocol193")!=1||snapshot.optLong("ownerEpoch193")!=0||snapshot.has("handoff193")||!"watch".equals(snapshot.optString("timerOwner")))return;
        // A WebView-supplied origin is never authority. Retain only this phone's prior native proof.
        String node="";JSONObject prior=ActiveWorkoutStore.current(context);
        if(prior!=null&&prior.optString("workoutId").equals(snapshot.optString("workoutId"))&&prior.optInt("ownershipProtocol193")==1&&"watch".equals(prior.optString("timerOwner"))&&prior.optLong("ownerEpoch193")==0&&!prior.has("handoff193"))node=prior.optString("watchSourceNode193");
        if(!GpsRoutePacket193.id(node))node=WearCommandQueueStore.sourceOrigin193(context,snapshot.optString("workoutId"));
        if(GpsRoutePacket193.id(node))snapshot.put("watchSourceNode193",node);
    }
    static void authorize(Context context,JSONObject snapshot) throws Exception {
        if(snapshot.optInt("ownershipProtocol193")!=1)return;
        String source="",target="";long epoch=snapshot.optLong("ownerEpoch193");JSONObject h=snapshot.optJSONObject("handoff193");
        boolean pendingStart=epoch==0&&h==null&&"watch".equals(snapshot.optString("timerOwner"))&&GpsRoutePacket193.id(snapshot.optString("watchSourceNode193"));
        if(pendingStart)source=snapshot.getString("watchSourceNode193");
        else if(h!=null&&h.optInt("protocol")==1&&h.optLong("epoch")>=1) {
            if("committed".equals(h.optString("state"))&&h.optLong("epoch")==epoch) {
                if("watch".equals(h.optString("target"))&&"watch".equals(snapshot.optString("timerOwner"))) { source=h.getString("targetNode");target=h.getString("sourceNode"); }
                else if("watch".equals(h.optString("source"))&&"phone".equals(snapshot.optString("timerOwner"))) { source=h.getString("sourceNode");target=h.getString("targetNode");epoch--; }
            }else if(java.util.Arrays.asList("offered","accepted").contains(h.optString("state"))&&h.optLong("epoch")==epoch+1&&"watch".equals(h.optString("source"))&&"watch".equals(snapshot.optString("timerOwner"))) { source=h.getString("sourceNode");target=h.getString("targetNode"); }
        }
        if(!GpsRoutePacket193.id(source)||!pendingStart&&!GpsRoutePacket193.id(target))return;
        JSONArray exercises=snapshot.optJSONArray("exercises");if(exercises==null)return;GpsRouteLedger193 ledger=GpsRouteLedger193.open(context.getNoBackupFilesDir());
        for(int i=0;i<exercises.length();i++) {
            JSONObject ex=exercises.getJSONObject(i);String unit=ex.optString("repUnit").trim().toLowerCase(java.util.Locale.ROOT);
            if(!"distance".equals(ex.optString("measurementType"))&&!"km".equals(unit)&&!"m".equals(unit))continue;
            JSONArray sets=ex.optJSONArray("sets");if(sets==null)continue;
            for(int j=0;j<sets.length();j++) {
                String set=sets.getJSONObject(j).optString("setId");if(set.isEmpty())continue;
                JSONObject grant=new JSONObject().put("routeKey",GpsRoutePacket193.key(snapshot.getString("workoutId"),set))
                    .put("workoutId",snapshot.getString("workoutId")).put("setId",set).put("exerciseId",ex.getString("id")).put("sourceNode",source).put("ownerEpoch193",epoch);
                if(pendingStart)ledger.rememberSource(grant);else ledger.authorize(grant.put("targetNode",target));
            }
        }
    }
    /** Called with a native ownership-journal snapshot, never with an unverified remote route. */
    static void authorizeOwned(Context context,JSONObject snapshot) { try { authorize(context,snapshot); }catch(Exception ignored) { /* GPS storage failure cannot stop the workout. Source retains unacknowledged chunks. */ } }
    static JSONObject accept(Context context,JSONObject command,String origin,String localNode) throws Exception {
        require(ACTION.equals(command.optString("action"))&&command.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=16000);
        require(command.get("schema") instanceof Number&&command.getDouble("schema")==1&&command.get("createdAt") instanceof Number&&command.getDouble("createdAt")==command.getLong("createdAt")&&command.getLong("createdAt")>0);
        JSONObject packet=command.getJSONObject("gpsRoutePacket193"),expected=GpsRouteSpool193.pointer(packet),pointer=command.getJSONObject("route193");
        require(expected.getString("commandId").equals(command.getString("commandId"))&&expected.getString("workoutId").equals(command.getString("workoutId"))
            &&command.get("ownerEpoch193") instanceof Number&&command.getDouble("ownerEpoch193")==command.getLong("ownerEpoch193")&&command.getLong("ownerEpoch193")==packet.getLong("ownerEpoch193")
            &&command.get("sequence") instanceof Number&&command.getDouble("sequence")==command.getLong("sequence")&&command.getLong("sequence")>0);
        for(String key:new String[]{"routeKey","streamId","sequence","sourceNode","targetNode","hash"})require(String.valueOf(expected.getJSONObject("route193").get(key)).equals(String.valueOf(pointer.get(key))));
        JSONObject proof=GpsRouteLedger193.open(context.getNoBackupFilesDir()).accept(packet,origin,localNode);if(proof==null)return null;
        JSONObject clean=new JSONObject();for(String key:new String[]{"schema","action","commandId","workoutId","ownerEpoch193","sequence","createdAt"})clean.put(key,command.get(key));
        clean.put("route193",expected.getJSONObject("route193"));if("deleted".equals(proof.getString("status")))clean.put("routeDeleted193",true);return clean;
    }
    /** Capture URI and JSON before the caller releases its DataEvent buffer. */
    static boolean receive(Context context,JSONObject command,Uri uri) {
        if(!ACTION.equals(command.optString("action")))return command.has("gpsRoutePacket193");
        try {
            String origin=uri.getHost();if(!GpsRoutePacket193.id(origin)||!(WearCommandListenerService.COMMAND_PATH_PREFIX+command.optString("commandId")).equals(uri.getPath()))return true;
            JSONObject copy=new JSONObject(command.toString());
            Wearable.getNodeClient(context).getLocalNode().addOnSuccessListener(local->{
                if(local.getId().equals(origin))return;
                try { JSONObject receipt=accept(context,copy,origin,local.getId());if(receipt!=null)WearCommandReceipts.complete(context,receipt,"applied",uri); }
                catch(Exception ignored) { /* Missing consent/storage retains the DataItem; no generic queue or WebView delivery. */ }
            });
        }catch(Exception ignored) { /* Invalid GPS commands are never generic workout commands. */ }
        return true;
    }
}
