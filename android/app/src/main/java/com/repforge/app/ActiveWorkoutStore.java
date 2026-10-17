package com.repforge.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

final class ActiveWorkoutStore {
    private static final String PREFS = "trainpilot_wear_sync";
    private static final String SNAPSHOT = "snapshot";
    private static final String REVISION = "revision";

    private ActiveWorkoutStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static synchronized JSONObject save(Context context, JSONObject input) throws Exception {
        if(!PhoneOwnership193.publicationAllowed(context,input))throw new IllegalStateException("Reconcile workout ownership before publishing");
        return saveOwned(context,input);
    }

    static synchronized JSONObject saveOwned(Context context, JSONObject input) throws Exception {
        long revision = prefs(context).getLong(REVISION, 0L) + 1L;
        JSONObject snapshot = new JSONObject(input.toString());
        snapshot.put("schema", snapshot.optInt("schema", 1));
        snapshot.put("revision", revision);
        snapshot.put("active", true);
        snapshot.put("syncedAt", System.currentTimeMillis());
        boolean saved=prefs(context).edit()
                .putLong(REVISION, revision)
                .putString(SNAPSHOT, snapshot.toString())
                .commit();
        if(!saved)throw new IllegalStateException("Workout snapshot could not be saved");
        return snapshot;
    }

    static synchronized JSONObject clear(Context context) throws Exception {
        JSONObject previous=current(context);
        if(previous!=null)PhoneOwnership193.journal(context).close(previous.optString("workoutId"));
        long revision = prefs(context).getLong(REVISION, 0L) + 1L;
        JSONObject snapshot = new JSONObject()
                .put("schema", 1)
                .put("revision", revision)
                .put("active", false)
                .put("workoutId",previous==null?"":previous.optString("workoutId"))
                .put("ownerEpoch193",previous==null?0:previous.optLong("ownerEpoch193"))
                .put("syncedAt", System.currentTimeMillis());
        prefs(context).edit()
                .putLong(REVISION, revision)
                .putString(SNAPSHOT, snapshot.toString())
                .apply();
        return snapshot;
    }

    static synchronized JSONObject current(Context context) {
        String raw = prefs(context).getString(SNAPSHOT, "");
        if (raw == null || raw.isEmpty()) return null;
        try {
            return new JSONObject(raw);
        } catch (Exception ignored) {
            return null;
        }
    }
}
