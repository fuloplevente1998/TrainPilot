package com.repforge.app;

import android.content.Context;
import android.os.Looper;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import org.json.JSONArray;
import org.json.JSONObject;

/** All callers serialize on the main looper: SQL receipt, timeline delivery, then Wear acknowledgement. */
final class PhoneWorkoutExecution {
    private PhoneWorkoutExecution() {}
    private static PhoneWorkoutExecutionStore store;
    static PhoneWorkoutExecutionStore store(Context context) {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Execution requires main looper");
        if (store == null) store = new PhoneWorkoutExecutionStore(context);
        return store;
    }
    static boolean compatible(JSONObject command) {
        return command.optInt("nativeExecutionProtocol") == 2 && command.optLong("generation") > 0
                && ("completeSet".equals(command.optString("action")) ||
                    "automaticControl".equals(command.optString("action")) && "start".equals(command.optString("value")));
    }
    static JSONObject execute(Context context, JSONObject command) throws Exception {
        if (!compatible(command)) return new JSONObject().put("status", "legacy");
        PhoneWorkoutExecutionStore db = store(context); JSONObject frame = db.snapshot();
        // Receipts are consulted before dispatch; a replay never creates a second clock.
        JSONObject receipt = db.receipt(command);
        if (receipt != null) {
            JSONObject requested=receipt.optJSONObject("transition"),current=frame==null?null:frame.optJSONObject("transition");
            if(frame!=null&&frame.optBoolean("active")&&!db.restoreBlocked()&&!db.commandsSuspended()&&"duplicate".equals(receipt.optString("status"))&&requested!=null&&requested.optJSONObject("plan")!=null&&current!=null&&current.optJSONObject("plan")!=null
                && requested.getJSONObject("plan").optString("key").equals(current.getJSONObject("plan").optString("key"))&&"pending".equals(current.optString("state"))){deliver(context);publish(context);}
            return receipt;
        }
        if (frame == null || !frame.optBoolean("active") || !frame.optBoolean("automaticEnabled") || db.commandsSuspended())
            return new JSONObject().put("status", "rejected").put("reason", "executionNotPrepared");
        if(db.restoreBlocked())return new JSONObject().put("status","rejected").put("reason","restoreInProgress");
        if (!IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation")))
            return new JSONObject().put("status", "rejected").put("reason", "controllerNotArmed");
        JSONObject timer = IntervalTimerService.snapshot(context);
        if (!timer.optString("key").isEmpty() && !timer.optBoolean("stopped") && !timer.optBoolean("interrupted") && !"complete".equals(timer.optString("phase")))
            return new JSONObject().put("status", "rejected").put("reason", "timerStillActive");
        JSONObject transition=frame.optJSONObject("transition"),previousPlan=transition==null?null:transition.optJSONObject("plan");
        if(previousPlan!=null&&!previousPlan.optBoolean("restOnly")){
            JSONArray owners=previousPlan.getJSONArray("steps"),exercises=frame.getJSONArray("exercises");
            for(int i=0;i<owners.length();i++)for(int e=0;e<exercises.length();e++){
                JSONObject exercise=exercises.getJSONObject(e);JSONArray sets=exercise.getJSONArray("sets");
                for(int j=0;j<sets.length();j++)if(owners.getJSONObject(i).getString("setId").equals(sets.getJSONObject(j).getString("setId"))&&!sets.getJSONObject(j).getBoolean("done"))
                    return new JSONObject().put("status","rejected").put("reason","timedResultsAwaitingReconciliation");
            }
        }
        JSONObject result = "completeSet".equals(command.optString("action")) ? db.record(command) : db.startTimeline(command);
        if ("applied".equals(result.optString("status"))) { deliver(context); publish(context); }
        return result;
    }
    static void deliver(Context context) throws Exception {
        PhoneWorkoutExecutionStore db = store(context); JSONObject frame = db.snapshot();
        if (frame == null || !frame.optBoolean("active")) return;
        JSONObject transition = frame.optJSONObject("transition"), plan = transition == null ? null : transition.optJSONObject("plan");
        if (plan == null || !"pending".equals(transition.optString("state"))) return;
        boolean started = false;
        try { started = IntervalTimerService.startPrepared(plan); }
        finally { db.markTransition(plan.getString("key"), started ? "started" : "interrupted"); }
    }
    static JSONObject status(Context context) throws Exception {
        PhoneWorkoutExecutionStore db = store(context); JSONObject frame = db.snapshot();
        JSONArray events = frame == null ? new JSONArray() : db.pendingEvents(frame.getString("workoutId"), frame.getLong("generation"), 0);
        JSONObject timer = IntervalTimerService.snapshot(context);
        JSONObject transition = frame == null ? null : frame.optJSONObject("transition"), plan = transition == null ? null : transition.optJSONObject("plan");
        // A killed service is reported as interrupted; reopening the WebView never replays past work or cues.
        if (frame != null && frame.optBoolean("active") && plan != null && !"interrupted".equals(transition.optString("state"))
                && (!IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation"))
                    || !plan.getString("key").equals(timer.optString("key")))) {
            db.markTransition(plan.getString("key"), "interrupted"); frame = db.snapshot();
        }
        return new JSONObject().put("snapshot", frame == null ? JSONObject.NULL : frame).put("events", events).put("timer", timer);
    }
    static JSONObject display(Context context) throws Exception {
        JSONObject frame = store(context).snapshot(); if (frame == null || !frame.optBoolean("active")) return null;
        JSONObject value = new JSONObject(frame.toString());
        value.remove("timerConfig"); value.remove("automaticEnabled"); value.remove("transition");
        value.put("nativeExecutionProtocol", 2);
        JSONObject transition = frame.optJSONObject("transition"), plan = transition == null ? null : transition.optJSONObject("plan");
        if (plan != null) {
            JSONObject timer = IntervalTimerService.snapshot(context);
            if (plan.getString("key").equals(timer.optString("key"))) {
                boolean interrupted = "interrupted".equals(transition.optString("state")) || timer.optBoolean("interrupted");
                boolean running = timer.optBoolean("running") && !interrupted;
                long remaining = timer.optLong("remainingMs");
                JSONObject phaseStep = plan.getJSONArray("steps").getJSONObject(timer.optInt("stepIndex"));
                value.put("automation", new JSONObject().put("owner", "phone").put("enabled", true).put("key", plan.getString("key"))
                        .put("phase", interrupted ? "ready" : plan.optBoolean("restOnly") && "complete".equals(timer.optString("phase")) ? "ready" : timer.optString("phase"))
                        .put("paused", !running).put("remainingMs", remaining).put("deadlineAt", running ? System.currentTimeMillis() + remaining : 0)
                        .put("workSeconds", phaseStep.getInt("work")).put("restSeconds", "rest".equals(timer.optString("phase")) && plan.optInt("initialRest") > 0 && timer.optLong("elapsedMs") < plan.optLong("initialRest") * 1000 ? plan.getInt("initialRest") : phaseStep.getInt("rest"))
                        .put("phaseNumber", timer.optLong("phaseNumber")).put("targetExercise", transition.getInt("currentExercise"))
                        .put("vibration", false).put("interrupted", interrupted));
            }
        } else if (transition != null) value.put("automation", new JSONObject().put("enabled", true).put("owner", "phone")
                .put("phase", transition.optString("phase", "ready")).put("remainingMs", 0).put("key", "").put("paused", false));
        return value;
    }
    static void publish(Context context) throws Exception {
        JSONObject value = display(context); if (value == null) return;
        JSONObject saved = ActiveWorkoutStore.save(context, value);
        PutDataMapRequest map = PutDataMapRequest.create(WearSyncPlugin.ACTIVE_WORKOUT_PATH);
        map.getDataMap().putString("snapshot", saved.toString()); map.getDataMap().putBoolean("active", true);
        map.getDataMap().putLong("revision", saved.getLong("revision"));
        Wearable.getDataClient(context).putDataItem(map.asPutDataRequest().setUrgent());
    }
}
