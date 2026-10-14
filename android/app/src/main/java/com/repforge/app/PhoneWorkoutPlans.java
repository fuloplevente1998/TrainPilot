package com.repforge.app;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Plans use the foreground-prepared snapshot, never a WebView form or guessed repetitions. */
final class PhoneWorkoutPlans {
    private PhoneWorkoutPlans() {}

    static void validateConfig(JSONObject frame) throws JSONException {
        if (!frame.optBoolean("automaticEnabled")) return;
        JSONObject c = frame.getJSONObject("timerConfig");
        integer(c, "work", 1, 3600); integer(c, "rest", 0, 3600); integer(c, "rounds", 1, 100);
        integer(frame, "restSeconds", 0, 3600);
    }
    private static int integer(JSONObject object, String field, int min, int max) throws JSONException {
        long value = PhoneWorkoutRecording.version(object, field);
        if (value < min || value > max) throw new IllegalArgumentException("invalidTimerConfig");
        return (int) value;
    }
    static JSONObject afterRecord(JSONObject frame, int from) throws JSONException {
        if (!frame.optBoolean("automaticEnabled")) return null;
        JSONObject exercise=frame.getJSONArray("exercises").getJSONObject(from);
        int rest=exercise.has("restSeconds162")?integer(exercise,"restSeconds162",0,3600):frame.getInt("restSeconds");
        return next(frame, from, rest, false);
    }
    static JSONObject startTimed(JSONObject frame) throws JSONException {
        if (!frame.optBoolean("automaticEnabled")) throw new IllegalArgumentException("automaticDisabled");
        return next(frame, frame.getInt("currentExercise"), 0, true);
    }
    private static JSONObject next(JSONObject frame, int from, int rest, boolean timedOnly) throws JSONException {
        validateConfig(frame);
        JSONArray exercises = frame.getJSONArray("exercises");
        int index = from; JSONObject target = null, exercise = null;
        for (; index < exercises.length(); index++) {
            exercise = exercises.getJSONObject(index); JSONArray sets = exercise.getJSONArray("sets");
            for (int j = 0; j < sets.length(); j++) if (!sets.getJSONObject(j).getBoolean("done")) { target = sets.getJSONObject(j); break; }
            if (target != null) break;
        }
        JSONObject transition = new JSONObject().put("state", "pending").put("currentExercise", index < exercises.length() ? index : from);
        if (target == null) {
            if (timedOnly) throw new IllegalArgumentException("noTimedTarget");
            return transition.put("phase", "complete");
        }
        boolean timed = "mp".equals(exercise.optString("repUnit"));
        if (timedOnly && !timed) throw new IllegalArgumentException("noTimedTarget");
        if ("distance".equals(exercise.optString("measurementType"))) throw new IllegalArgumentException("backgroundGpsNotPrepared");
        JSONObject config = frame.getJSONObject("timerConfig");
        JSONArray rows = new JSONArray(); ArrayList<IntervalTimeline.Step> steps = new ArrayList<>();
        String key = "native|" + frame.getLong("generation") + "|" + frame.getLong("executionRevision");
        if (timed) {
            for (int i = index; i < exercises.length(); i++) {
                JSONObject e = exercises.getJSONObject(i);
                if (!"mp".equals(e.optString("repUnit"))) break;
                if ("distance".equals(e.optString("measurementType"))) throw new IllegalArgumentException("backgroundGpsNotPrepared");
                JSONArray sets = e.getJSONArray("sets");
                for (int j = 0; j < sets.length(); j++) {
                    JSONObject set = sets.getJSONObject(j); if (set.getBoolean("done")) continue;
                    JSONObject row = row(e, set, config.getInt("work"), config.getInt("rest"), config.getInt("rounds"));
                    double base = set.optString("reps").isEmpty() ? 0 : Double.parseDouble(set.getString("reps"));
                    if (!Double.isFinite(base) || base < 0) throw new IllegalArgumentException("invalidTimedBaseline");
                    rows.put(row.put("baseSeconds", base)); steps.add(step(row));
                }
            }
        } else {
            JSONObject row = row(exercise, target, 1, rest, 1); rows.put(row); steps.add(step(row));
        }
        new IntervalTimeline(steps, timed, timed ? rest : 0, !timed);
        JSONObject plan = new JSONObject().put("key", key).put("workoutId", frame.getString("workoutId"))
                .put("started", frame.optString("started")).put("generation", frame.getLong("generation"))
                .put("native", true).put("steps", rows).put("autoAdvance", timed).put("restOnly", !timed)
                .put("initialRest", timed ? rest : 0).put("elapsedMs", 0).put("running", false).put("stopped", false)
                .put("sound", config.optBoolean("sound", true)).put("vibration", config.optBoolean("vibration", true))
                .put("language", frame.optString("language", "en")).put("target", new JSONObject()
                        .put("exerciseId", exercise.getString("id")).put("setId", target.getString("setId")))
                .put("work", config.getInt("work")).put("rest", config.getInt("rest")).put("rounds", config.getInt("rounds"));
        return transition.put("phase", rest > 0 ? "rest" : timed ? "work" : "ready").put("plan", plan);
    }
    private static JSONObject row(JSONObject e, JSONObject s, int work, int rest, int rounds) throws JSONException {
        return new JSONObject().put("exerciseId", e.getString("id")).put("setId", s.getString("setId"))
                .put("name", e.optString("name", e.getString("id"))).put("work", work).put("rest", rest)
                .put("rounds", rounds).put("gpsKey", "");
    }
    private static IntervalTimeline.Step step(JSONObject row) throws JSONException {
        return new IntervalTimeline.Step(row.getString("exerciseId"), row.getString("setId"), row.getString("name"),
                row.getInt("work"), row.getInt("rest"), row.getInt("rounds"), "");
    }
}
