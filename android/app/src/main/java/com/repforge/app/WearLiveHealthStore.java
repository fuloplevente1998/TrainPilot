package com.repforge.app;

import android.content.Context;
import org.json.JSONObject;

/** Keep the last watch acknowledgement without creating an unbounded command backlog. */
final class WearLiveHealthStore {
    static final String PATH = "/trainpilot/watch-health";
    private static final String PREFS = "trainpilot_wear_live_health";
    static synchronized void save(Context context, String raw) {
        if (raw == null || raw.length() > 32768) return;
        try {
            JSONObject next = new JSONObject(raw);
            if (!valid(next)) return;
            JSONObject old = current(context);
            if (old != null && (old.optString("watchId").equals(next.optString("watchId"))
                    ? old.optLong("revision") >= next.optLong("revision")
                    : old.optLong("updatedAt") > next.optLong("updatedAt"))) return;
            context.getSharedPreferences(PREFS,0).edit().putString("health",raw).apply();
        } catch (Exception ignored) {}
    }
    static boolean valid(JSONObject value) {
        return value.optInt("schema") == 1 && value.optLong("revision") > 0
                && !value.optString("workoutId").isEmpty() && value.optString("workoutId").length() <= 512
                && !value.optString("watchId").isEmpty() && value.optString("watchId").length() <= 80
                && value.optLong("updatedAt") > 0;
    }
    static synchronized JSONObject current(Context context) {
        try { return new JSONObject(context.getSharedPreferences(PREFS,0).getString("health","")); }
        catch (Exception ignored) { return null; }
    }
}
