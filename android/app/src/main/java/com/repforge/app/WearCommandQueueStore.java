package com.repforge.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

final class WearCommandQueueStore {
    private static final String PREFS = "trainpilot_wear_commands";
    private static final String PENDING = "pending";
    private static final String SEEN = "seen";
    private static final int MAX_PENDING = 64;
    private static final int MAX_SEEN = 256;

    private WearCommandQueueStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static JSONArray read(Context context, String key) {
        String raw = prefs(context).getString(key, "[]");
        try { return new JSONArray(raw == null ? "[]" : raw); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    private static boolean containsId(JSONArray array, String id) {
        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            if (value instanceof JSONObject) {
                if (id.equals(((JSONObject) value).optString("commandId"))) return true;
            } else if (id.equals(String.valueOf(value))) return true;
        }
        return false;
    }

    static synchronized void enqueue(Context context, JSONObject input, String uri) throws Exception {
        if(PhoneGpsRoute193.ACTION.equals(input.optString("action"))||input.has("gpsRoutePacket193"))return;
        String id = input.optString("commandId", "");
        if (id.isEmpty()) return;
        JSONArray pending = read(context, PENDING), seen = read(context, SEEN);
        if (containsId(pending, id) || containsId(seen, id)) return;
        if("watchAutomationState".equals(input.optString("action")))for(int i=pending.length()-1;i>=0;i--){
            JSONObject older=pending.optJSONObject(i);
            if(older!=null&&"watchAutomationState".equals(older.optString("action"))&&input.optString("workoutId").equals(older.optString("workoutId"))){
                if(older.optLong("sequence")>=input.optLong("sequence"))return;
                pending.remove(i);
            }
        }
        JSONObject command = new JSONObject(input.toString());
        command.put("receivedAt", System.currentTimeMillis());
        if (uri != null) command.put("_uri", uri);else command.remove("_uri");
        // Never evict an unacknowledged recording/finish command when the phone
        // WebView is sleeping. Its Wear DataItem remains until acknowledged and
        // can be re-scanned when capacity becomes available.
        if (pending.length() >= MAX_PENDING) return;
        pending.put(command);
        if (!prefs(context).edit().putString(PENDING, pending.toString()).commit())
            throw new IllegalStateException("Unable to persist watch command");
    }

    static synchronized JSONArray pending(Context context) {
        try { return new JSONArray(read(context, PENDING).toString()); }
        catch (Exception ignored) { return new JSONArray(); }
    }
    static synchronized boolean seen(Context context,String id){return containsId(read(context,SEEN),id);}

    static synchronized String sourceOrigin193(Context context,String workoutId) {
        java.util.Set<String> nodes=new java.util.HashSet<>();JSONArray pending=read(context,PENDING),seen=read(context,SEEN);
        for(int i=0;i<pending.length();i++) { JSONObject command=pending.optJSONObject(i);if(command==null||!workoutId.equals(command.optString("workoutId")))continue;
            String origin=PhoneGpsRoute193.startOrigin(command);if(!origin.isEmpty())nodes.add(origin); }
        for(int i=0;i<seen.length();i++) { JSONObject receipt=seen.optJSONObject(i);if(receipt!=null&&workoutId.equals(receipt.optString("workoutId"))&&com.repforge.shared.GpsRoutePacket193.id(receipt.optString("sourceNode193")))nodes.add(receipt.optString("sourceNode193")); }
        return nodes.size()==1?nodes.iterator().next():"";
    }

    static synchronized JSONObject receipt(Context context, String id) {
        JSONArray seen=read(context,SEEN);
        for(int i=0;i<seen.length();i++) {
            JSONObject row=seen.optJSONObject(i);
            if(row!=null&&id.equals(row.optString("commandId")))return row;
        }
        return null;
    }

    static synchronized boolean ack(Context context, JSONObject command, String status) {
        String id=command.optString("commandId");
        if(id.isEmpty())return false;
        JSONArray pending=read(context,PENDING),next=new JSONArray(),seen=read(context,SEEN);
        for(int i=0;i<pending.length();i++) {
            JSONObject row=pending.optJSONObject(i);
            if(row!=null&&!id.equals(row.optString("commandId")))next.put(row);
        }
        if(!containsId(seen,id))try {
            JSONObject receipt=new JSONObject().put("schema",1).put("commandId",id)
                .put("workoutId",command.optString("workoutId")).put("action",command.optString("action"))
                .put("sequence",command.optLong("sequence")).put("status",status)
                .put("receivedAt",System.currentTimeMillis());
            String origin=PhoneGpsRoute193.startOrigin(command);if(!origin.isEmpty())receipt.put("sourceNode193",origin);
            if(PhoneGpsRoute193.ACTION.equals(command.optString("action"))) {
                receipt.put("route193",command.getJSONObject("route193"));if(command.optBoolean("routeDeleted193"))receipt.put("routeDeleted193",true);
            }
            seen.put(receipt);
        } catch(Exception error) {return false;}
        while(seen.length()>MAX_SEEN)seen.remove(0);
        SharedPreferences p=prefs(context);
        String beforePending=p.getString(PENDING,"[]"),beforeSeen=p.getString(SEEN,"[]");
        if(p.edit().putString(PENDING,next.toString()).putString(SEEN,seen.toString()).commit())return true;
        // commit(false) may still change the in-memory preferences. Never replay
        // that volatile receipt as a durable phone acknowledgement.
        p.edit().putString(PENDING,beforePending).putString(SEEN,beforeSeen).commit();
        return false;
    }

    static synchronized boolean ack(Context context, String commandId) {
        if (commandId == null || commandId.isEmpty()) return false;
        JSONArray pending = read(context, PENDING), next = new JSONArray();
        for (int i = 0; i < pending.length(); i++) {
            JSONObject command = pending.optJSONObject(i);
            if (command != null && !commandId.equals(command.optString("commandId"))) next.put(command);
        }
        JSONArray seen = read(context, SEEN);
        if (!containsId(seen, commandId)) seen.put(commandId);
        while (seen.length() > MAX_SEEN) seen.remove(0);
        // Acknowledge only after both the pending queue and dedupe receipt
        // have reached durable storage; a process crash must not replay a set.
        return prefs(context).edit().putString(PENDING, next.toString()).putString(SEEN, seen.toString()).commit();
    }
}
