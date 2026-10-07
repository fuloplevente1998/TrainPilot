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
        String id = input.optString("commandId", "");
        if (id.isEmpty()) return;
        JSONArray pending = read(context, PENDING), seen = read(context, SEEN);
        if (containsId(pending, id) || containsId(seen, id)) return;
        JSONObject command = new JSONObject(input.toString());
        command.put("receivedAt", System.currentTimeMillis());
        if (uri != null) command.put("_uri", uri);
        pending.put(command);
        while (pending.length() > MAX_PENDING) pending.remove(0);
        prefs(context).edit().putString(PENDING, pending.toString()).apply();
    }

    static synchronized JSONArray pending(Context context) {
        try { return new JSONArray(read(context, PENDING).toString()); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    static synchronized void ack(Context context, String commandId) {
        if (commandId == null || commandId.isEmpty()) return;
        JSONArray pending = read(context, PENDING), next = new JSONArray();
        for (int i = 0; i < pending.length(); i++) {
            JSONObject command = pending.optJSONObject(i);
            if (command != null && !commandId.equals(command.optString("commandId"))) next.put(command);
        }
        JSONArray seen = read(context, SEEN);
        if (!containsId(seen, commandId)) seen.put(commandId);
        while (seen.length() > MAX_SEEN) seen.remove(0);
        prefs(context).edit().putString(PENDING, next.toString()).putString(SEEN, seen.toString()).apply();
    }
}
