package com.repforge.app;

import org.json.JSONArray;
import org.json.JSONObject;

/** A watch request changes only its stable target; it never invents repetition data. */
final class PhoneAutomaticReducer {
    private PhoneAutomaticReducer() {}

    static JSONObject reduce(JSONObject input, JSONObject command, JSONObject timer) throws Exception {
        if (input == null || !input.optBoolean("active") || !"phone".equals(input.optString("timerOwner")) ||
                !input.optString("workoutId").equals(command.optString("workoutId"))) return null;
        JSONObject config = input.optJSONObject("backgroundAutomatic162"), a = command.optJSONObject("automation");
        if (config == null || !config.optBoolean("enabled") || a == null || !a.optBoolean("enabled") ||
                !"phone".equals(a.optString("owner")) || !command.optString("commandId").matches("[A-Za-z0-9-]{1,80}")) return null;
        JSONObject currentAuto = input.optJSONObject("automation");
        if (currentAuto == null || !currentAuto.optBoolean("enabled")) return null;
        String expectedKey = a.optString("key");
        if (!expectedKey.equals(currentAuto.optString("key")) && !expectedKey.equals(timer.optString("key"))) return null;
        if (!expectedKey.isEmpty() && expectedKey.equals(timer.optString("key"))) {
            if (timer.optBoolean("interrupted") || timer.optBoolean("stopped") ||
                    !"complete".equals(timer.optString("phase")) || timer.optLong("phaseNumber") != a.optLong("phaseNumber")) return null;
        } else if (a.optLong("phaseNumber") != currentAuto.optLong("phaseNumber")) return null;

        JSONObject snapshot = new JSONObject(input.toString());
        snapshot.put("watchSequence", Math.max(snapshot.optLong("watchSequence"), command.optLong("sequence")));
        JSONArray exercises = snapshot.getJSONArray("exercises");
        int index = snapshot.optInt("currentExercise");
        int[] target = next(exercises, index);
        if (target == null) return null;
        JSONObject exercise = exercises.getJSONObject(target[0]), set = exercise.getJSONArray("sets").getJSONObject(target[1]);
        String action = command.optString("action");
        JSONObject recorded = null;
        if ("completeSet".equals(action)) {
            if (!exercise.optString("id").equals(command.optString("exerciseId")) ||
                    set.optString("setId").isEmpty() || !set.optString("setId").equals(command.optString("setId")) ||
                    "distance".equals(exercise.optString("measurementType")) || exercise.optString("repUnit").startsWith("mp")) return null;
            double reps = command.optDouble("reps", Double.NaN), weight = command.optDouble("weight", Double.NaN);
            if (!Double.isFinite(reps) || reps < 1 || reps > 86400 || reps != Math.rint(reps) ||
                    !Double.isFinite(weight) || weight < 0 || weight > 10000) return null;
            set.put("reps", String.valueOf((int) reps)).put("weight", weight).put("done", true);
            recorded = new JSONObject().put("exerciseId", exercise.getString("id")).put("setId", set.getString("setId"))
                    .put("reps", set.getString("reps")).put("weight", weight);
            target = next(exercises, target[0]);
        } else if (!"automaticControl".equals(action) || !"start".equals(command.optString("value")) ||
                !"mp".equals(exercise.optString("repUnit"))) return null;

        JSONObject plan = null;
        if (target != null) {
            exercise = exercises.getJSONObject(target[0]); set = exercise.getJSONArray("sets").getJSONObject(target[1]);
            if (set.optString("setId").isEmpty()) return null;
            int rest = "completeSet".equals(action) ? exercises.getJSONObject(index).optInt("automaticRestSeconds", snapshot.optInt("restSeconds", 90)) : 0;
            if (rest < 0 || rest > 3600) return null;
            boolean timed = "mp".equals(exercise.optString("repUnit"));
            JSONArray steps = new JSONArray();
            if (timed) {
                JSONObject preset = config.getJSONObject("preset");
                for (int ei = target[0]; ei < exercises.length(); ei++) {
                    JSONObject e = exercises.getJSONObject(ei);
                    if (!"mp".equals(e.optString("repUnit"))) break;
                    // Starting location tracking needs its own foreground permission flow.
                    if ("distance".equals(e.optString("measurementType"))) return null;
                    JSONArray sets = e.getJSONArray("sets");
                    for (int si = 0; si < sets.length(); si++) {
                        JSONObject s = sets.getJSONObject(si); if (s.optBoolean("done")) continue;
                        steps.put(new JSONObject().put("exerciseId", e.getString("id")).put("setId", s.getString("setId"))
                                .put("name", e.optString("name", "TrainPilot")).put("work", preset.getInt("work"))
                                .put("rest", preset.getInt("rest")).put("rounds", preset.getInt("rounds"))
                                .put("baseSeconds", s.optDouble("reps", 0)).put("baseMeters", 0).put("gpsKey", ""));
                    }
                }
            } else if (rest > 0) steps.put(new JSONObject().put("exerciseId", exercise.getString("id"))
                    .put("setId", set.getString("setId")).put("name", exercise.optString("name", "TrainPilot"))
                    .put("work", 1).put("rest", rest).put("rounds", 1).put("gpsKey", ""));
            if (steps.length() > 0) {
                plan = new JSONObject().put("key", "bg|" + command.getString("commandId"))
                        .put("workoutId", snapshot.getString("workoutId")).put("started", snapshot.optString("started"))
                        .put("native", true).put("steps", steps).put("elapsedMs", 0).put("running", false)
                        .put("autoAdvance", timed).put("restOnly", !timed).put("initialRest", timed ? rest : 0)
                        .put("sound", timed ? config.optJSONObject("preset").optBoolean("sound", true) : config.optBoolean("restSound"))
                        .put("vibration", config.optJSONObject("preset").optBoolean("vibration", true))
                        .put("language", snapshot.optString("language", "en"));
                if (!timed) plan.put("target", new JSONObject().put("exerciseId", exercise.getString("id")).put("setId", set.getString("setId")));
                IntervalTimerService.validate(plan);
            }
            snapshot.put("currentExercise", target[0]);
        }
        snapshot.put("restEndAt", 0).put("automation", new JSONObject(currentAuto.toString())
                .put("key", plan == null ? "" : plan.getString("key")).put("phaseNumber", 0)
                .put("phase", target == null ? "complete" : "ready").put("paused", false));
        return new JSONObject().put("commandId", command.getString("commandId")).put("workoutId", snapshot.getString("workoutId"))
                .put("snapshot", snapshot).put("recorded", recorded == null ? JSONObject.NULL : recorded)
                .put("plan", plan == null ? JSONObject.NULL : plan).put("currentExercise", snapshot.optInt("currentExercise"));
    }

    static int[] next(JSONArray exercises, int start) throws Exception {
        for (int ei = Math.max(0, start); ei < exercises.length(); ei++) {
            JSONArray sets = exercises.getJSONObject(ei).getJSONArray("sets");
            for (int si = 0; si < sets.length(); si++) if (!sets.getJSONObject(si).optBoolean("done")) return new int[]{ei, si};
        }
        return null;
    }

    static void overlay(JSONObject snapshot, JSONObject receipt) throws Exception {
        if (!snapshot.optString("workoutId").equals(receipt.optString("workoutId"))) return;
        JSONArray applied = snapshot.optJSONArray("backgroundApplied162");
        if (applied != null) for (int i = 0; i < applied.length(); i++) if (receipt.optString("commandId").equals(applied.optString(i))) return;
        JSONArray source = receipt.getJSONObject("snapshot").getJSONArray("exercises");
        for (int ei = 0; ei < snapshot.getJSONArray("exercises").length(); ei++) {
            JSONObject e = snapshot.getJSONArray("exercises").getJSONObject(ei);
            for (int se = 0; se < source.length(); se++) {
                JSONObject from = source.getJSONObject(se);if (!e.optString("id").equals(from.optString("id"))) continue;
                JSONArray sets = e.getJSONArray("sets"), old = from.getJSONArray("sets");
                for (int si = 0; si < sets.length(); si++) for (int sj = 0; sj < old.length(); sj++) {
                    JSONObject s = sets.getJSONObject(si), value = old.getJSONObject(sj);
                    if (!s.optString("setId").isEmpty() && s.optString("setId").equals(value.optString("setId")) && value.optBoolean("done") && !s.optBoolean("done"))
                        s.put("reps", value.optString("reps")).put("weight", value.opt("weight")).put("done", true);
                }
            }
        }
        snapshot.put("currentExercise", receipt.getInt("currentExercise"))
                .put("automation", receipt.getJSONObject("snapshot").getJSONObject("automation")).put("restEndAt", 0)
                .put("watchSequence", Math.max(snapshot.optLong("watchSequence"), receipt.getJSONObject("snapshot").optLong("watchSequence")));
    }
}
