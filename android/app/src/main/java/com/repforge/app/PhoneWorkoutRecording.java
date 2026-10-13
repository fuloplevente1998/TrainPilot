package com.repforge.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

/** Validates prepared execution frames; does not touch the WebView or start timers. */
final class PhoneWorkoutRecording {
    private static final long MAX_REVISION = 9007199254740991L;
    private PhoneWorkoutRecording() {}

    static long version(JSONObject data, String field) throws JSONException {
        Object value = data.get(field);
        if (!(value instanceof Integer) && !(value instanceof Long))
            throw new IllegalArgumentException("invalidVersion");
        long result = ((Number) value).longValue();
        if (result < 0 || result > MAX_REVISION) throw new IllegalArgumentException("invalidVersion");
        return result;
    }

    static long advance(long revision) {
        if (revision < 0 || revision >= MAX_REVISION) throw new IllegalArgumentException("revisionExhausted");
        return revision + 1;
    }

    static String id(JSONObject data, String field) throws JSONException {
        Object value = data.get(field);
        if (!(value instanceof String) || ((String) value).isEmpty())
            throw new IllegalArgumentException("missingIdentity");
        return (String) value;
    }

    static void validateFrame(JSONObject frame) throws JSONException {
        id(frame, "workoutId");
        if (version(frame, "generation") == 0) throw new IllegalArgumentException("invalidGeneration");
        version(frame, "executionRevision");
        if (!Boolean.TRUE.equals(frame.opt("active")) || !"phone".equals(frame.optString("timerOwner")))
            throw new IllegalArgumentException("inactiveOrOtherOwner");
        JSONArray exercises = frame.getJSONArray("exercises");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < exercises.length(); i++) {
            JSONObject exercise = exercises.getJSONObject(i);
            id(exercise, "id");
            JSONArray sets = exercise.getJSONArray("sets");
            for (int j = 0; j < sets.length(); j++) {
                JSONObject set = sets.getJSONObject(j);
                if (!ids.add(id(set, "setId"))) throw new IllegalArgumentException("duplicateSetIdentity");
                if (!(set.opt("done") instanceof Boolean)) throw new IllegalArgumentException("invalidSetState");
            }
        }
    }

    static final class Target {
        final JSONObject exercise, set;
        final int exerciseIndex;
        Target(JSONObject exercise, JSONObject set, int index) {
            this.exercise = exercise; this.set = set; this.exerciseIndex = index;
        }
    }

    static Target target(JSONObject frame, JSONObject command) throws JSONException {
        validateFrame(frame);
        if (!"completeSet".equals(command.optString("action"))) throw new IllegalArgumentException("unsupportedAction");
        if (!id(frame, "workoutId").equals(id(command, "workoutId"))
                || version(frame, "generation") != version(command, "generation"))
            throw new IllegalArgumentException("executionIdentityChanged");
        if (version(frame, "executionRevision") != version(command, "executionRevision"))
            throw new IllegalArgumentException("revisionConflict");
        String exerciseId = id(command, "exerciseId"), setId = id(command, "setId");
        JSONArray exercises = frame.getJSONArray("exercises");
        for (int i = 0; i < exercises.length(); i++) {
            JSONObject exercise = exercises.getJSONObject(i);
            JSONArray sets = exercise.getJSONArray("sets");
            for (int j = 0; j < sets.length(); j++) {
                JSONObject set = sets.getJSONObject(j);
                if (!setId.equals(id(set, "setId"))) continue;
                if (!exerciseId.equals(id(exercise, "id"))) throw new IllegalArgumentException("targetChanged");
                if (set.getBoolean("done")) throw new IllegalArgumentException("alreadyRecorded");
                String unit = exercise.optString("repUnit");
                if (!(unit.isEmpty() || "ism.".equals(unit) || "reps".equals(unit))
                        || "distance".equals(exercise.optString("measurementType")))
                    throw new IllegalArgumentException("unsupportedMeasurement");
                return new Target(exercise, set, i);
            }
        }
        throw new IllegalArgumentException("unknownTarget");
    }

    static JSONObject record(JSONObject frame, JSONObject command) throws JSONException {
        JSONObject next = new JSONObject(frame.toString());
        Target target = target(next, command);
        Object reps = command.get("reps");
        String text = String.valueOf(reps);
        long repetitions;
        try { repetitions = text.matches("[0-9]+") ? Long.parseLong(text) : 0; }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException("invalidReps"); }
        if (repetitions < 1 || repetitions > Integer.MAX_VALUE) throw new IllegalArgumentException("invalidReps");
        target.set.put("reps", String.valueOf(repetitions));
        if (command.has("weight")) {
            double weight;
            try { weight = Double.parseDouble(String.valueOf(command.get("weight")).replace(',', '.')); }
            catch (NumberFormatException invalid) { throw new IllegalArgumentException("invalidWeight"); }
            if (!Double.isFinite(weight) || weight < 0) throw new IllegalArgumentException("invalidWeight");
            target.set.put("weight", weight);
        }
        target.set.put("done", true);
        next.put("currentExercise", target.exerciseIndex);
        next.put("executionRevision", advance(version(frame, "executionRevision")));
        return next;
    }

    static JSONObject recordedValues(JSONObject frame, JSONObject command) throws JSONException {
        JSONArray exercises = frame.getJSONArray("exercises");
        for (int i = 0; i < exercises.length(); i++) {
            JSONObject exercise = exercises.getJSONObject(i);
            if (!id(exercise, "id").equals(id(command, "exerciseId"))) continue;
            JSONArray sets = exercise.getJSONArray("sets");
            for (int j = 0; j < sets.length(); j++) {
                JSONObject set = sets.getJSONObject(j);
                if (!id(set, "setId").equals(id(command, "setId"))) continue;
                JSONObject values = new JSONObject().put("done", true).put("reps", set.getString("reps"));
                if (command.has("weight")) values.put("weight", set.get("weight"));
                return values;
            }
        }
        throw new IllegalArgumentException("unknownTarget");
    }

    static String fingerprint(JSONObject command) throws JSONException {
        JSONArray values = new JSONArray();
        for (String key : new String[]{"action", "workoutId", "generation", "executionRevision",
                "exerciseId", "setId", "reps", "weight", "value"})
            values.put(new JSONArray().put(command.has(key)).put(command.opt(key)));
        return values.toString();
    }

    static void retainCompleted(JSONObject oldFrame, JSONObject incoming) throws JSONException {
        JSONArray oldExercises = oldFrame.getJSONArray("exercises"), exercises = incoming.getJSONArray("exercises");
        for (int i = 0; i < oldExercises.length(); i++) {
            JSONObject oldExercise = oldExercises.getJSONObject(i);
            JSONArray oldSets = oldExercise.getJSONArray("sets");
            for (int j = 0; j < oldSets.length(); j++) {
                JSONObject oldSet = oldSets.getJSONObject(j);
                if (!oldSet.getBoolean("done")) continue;
                boolean retained = false;
                for (int e = 0; e < exercises.length(); e++) {
                    JSONObject exercise = exercises.getJSONObject(e);
                    if (!id(exercise, "id").equals(id(oldExercise, "id"))) continue;
                    JSONArray sets = exercise.getJSONArray("sets");
                    for (int s = 0; s < sets.length(); s++) {
                        JSONObject set = sets.getJSONObject(s);
                        if (id(set, "setId").equals(id(oldSet, "setId")) && set.getBoolean("done")) {
                            if (!set.optString("reps").equals(oldSet.optString("reps")))
                                throw new IllegalArgumentException("recordedValuesChanged");
                            for (String field : new String[]{"weight", "leftSeconds", "rightSeconds", "distanceMeters"})
                                if (!sameNumber(set.opt(field), oldSet.opt(field)))
                                    throw new IllegalArgumentException("recordedValuesChanged");
                            retained = true;
                        }
                    }
                }
                if (!retained) throw new IllegalArgumentException("completedSetRemoved");
            }
        }
    }

    private static boolean sameNumber(Object left, Object right) {
        if (left == null || left == JSONObject.NULL) return right == null || right == JSONObject.NULL;
        if (right == null || right == JSONObject.NULL) return false;
        try {
            double a = Double.parseDouble(left.toString().replace(',', '.'));
            double b = Double.parseDouble(right.toString().replace(',', '.'));
            return Double.isFinite(a) && Double.isFinite(b) && a == b;
        } catch (NumberFormatException invalid) { return left.equals(right); }
    }
}
