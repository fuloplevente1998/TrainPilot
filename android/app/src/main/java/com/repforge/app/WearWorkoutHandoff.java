package com.repforge.app;

import org.json.JSONObject;

/** A bounded request can only refresh the workout already published by this phone. */
final class WearWorkoutHandoff {
    static final String REQUEST = "/trainpilot/workout-request";
    static final String REPLY = "/trainpilot/workout-reply";
    static JSONObject response(JSONObject current, JSONObject request, long now) {
        if (current == null || request == null || !current.optBoolean("active")) return null;
        String id = request.optString("workoutId");
        long revision = request.optLong("minimumRevision"), sentAt = request.optLong("sentAt");
        if (id.isEmpty() || id.length() > 512 || !id.equals(current.optString("workoutId"))
                || revision < 1 || current.optLong("revision") < revision
                || sentAt <= 0 || now - sentAt > 15_000L || sentAt - now > 15_000L) return null;
        return current;
    }
}
