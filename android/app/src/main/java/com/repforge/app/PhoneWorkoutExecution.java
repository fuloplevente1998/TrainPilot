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
                && ("completeSet".equals(command.optString("action")) || "undoSet".equals(command.optString("action")) ||
                    "automaticControl".equals(command.optString("action")) && ("start".equals(command.optString("value"))||"resumeUndo".equals(command.optString("value"))));
    }
    static JSONObject execute(Context context, JSONObject command) throws Exception {
        if(!PhoneOwnership193.commandAllowed(context,command))return new JSONObject().put("status","rejected").put("reason","ownershipChanged");
        if (!compatible(command)) return new JSONObject().put("status", "legacy");
        PhoneWorkoutExecutionStore db = store(context); JSONObject frame = db.snapshot();
        // Receipts are consulted before dispatch; a replay never creates a second clock.
        JSONObject receipt = db.receipt(command);
        if (receipt != null) {
            JSONObject requested=receipt.optJSONObject("transition"),current=frame==null?null:frame.optJSONObject("transition");
            if(frame!=null&&frame.optBoolean("active")&&!db.restoreBlocked()&&!db.commandsSuspended()&&!frame.optBoolean("workoutPaused")&&"duplicate".equals(receipt.optString("status"))&&requested!=null&&requested.optJSONObject("plan")!=null&&current!=null&&current.optJSONObject("plan")!=null
                && requested.getJSONObject("plan").optString("key").equals(current.getJSONObject("plan").optString("key"))&&"pending".equals(current.optString("state"))){deliver(context);publish(context);}
            return receipt;
        }
        if (frame == null || !frame.optBoolean("active") || !frame.optBoolean("automaticEnabled") || db.commandsSuspended() || frame.optBoolean("workoutPaused"))
            return new JSONObject().put("status", "rejected").put("reason", "executionNotPrepared");
        if(db.restoreBlocked())return new JSONObject().put("status","rejected").put("reason","restoreInProgress");
        String dependency=command.optString("undoDependency188");
        if(!dependency.isEmpty()&&(frame.optJSONObject("undo188")==null||!dependency.equals(frame.getJSONObject("undo188").optString("commandId"))))return new JSONObject().put("status","rejected").put("reason","undoConflict");
        if("undoSet".equals(command.optString("action"))){
            if(!frame.getString("workoutId").equals(command.optString("workoutId"))||frame.getLong("generation")!=command.optLong("generation"))return new JSONObject().put("status","rejected").put("reason","undoConflict");
            if(!db.recorded188(frame.getString("workoutId"),frame.getLong("generation"),command.optString("recordId"))){
                return new JSONObject().put("status",frame.getLong("executionRevision")<=command.optLong("executionRevision")?"deferred":"rejected").put("reason","recordingAwaitingDelivery");
            }
            JSONObject running=IntervalTimerService.snapshot(context);
            if(frame.getString("workoutId").equals(running.optString("workoutId"))&&!running.optBoolean("stopped")){
                try{IntervalTimerService.pauseForUndo(context,running.optString("key"));}
                catch(IllegalStateException inactive){if(running.optBoolean("running"))throw inactive;}
            }
            if(running.optBoolean("autoAdvance")&&!running.optBoolean("restOnly")&&running.optLong("stepElapsedMs")>0&&"work".equals(running.optString("phase"))){
                JSONObject target=running.getJSONArray("steps").getJSONObject(running.optInt("stepIndex"));
                if(!target.optString("setId").equals(command.optString("setId"))){publish(context);return new JSONObject().put("status","rejected").put("reason","undoConflict");}
            }
            JSONObject undone=db.undo(command);publish(context);return undone;
        }
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
        if(frame!=null&&!PhoneOwnership193.phoneAllowed(context,frame.optString("workoutId")))return;
        if (frame == null || !frame.optBoolean("active") || frame.optBoolean("workoutPaused")) return;
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
        if(frame!=null){frame=db.snapshot();JSONObject latest=frame.optJSONObject("latestTimer162");
            if(latest!=null&&(!timer.optString("key").equals(latest.optString("key"))||!IntervalTimerService.armed(frame.getString("workoutId"),frame.getLong("generation")))){
                timer=new JSONObject(latest.toString()).put("running",false).put("interrupted",true);
            }
        }
        JSONObject transition = frame == null ? null : frame.optJSONObject("transition"), plan = transition == null ? null : transition.optJSONObject("plan");
        // A killed service is reported as interrupted; reopening the WebView never replays past work or cues.
        if (frame != null && frame.optBoolean("active") && plan != null && !"interrupted".equals(transition.optString("state"))
                && (!IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation"))
                    || !plan.getString("key").equals(timer.optString("key")))) {
            db.markTransition(plan.getString("key"), "interrupted"); frame = db.snapshot();
        }
        events=frame==null?new JSONArray():db.pendingEvents(frame.getString("workoutId"),frame.getLong("generation"),0);
        return new JSONObject().put("controllerArmed",frame!=null&&IntervalTimerService.armed(frame.optString("workoutId"),frame.optLong("generation"))).put("snapshot", frame == null ? JSONObject.NULL : frame).put("events", events).put("timer", timer);
    }
    static JSONObject display(Context context) throws Exception {
        JSONObject frame = store(context).snapshot(); if (frame == null || !frame.optBoolean("active")) return null;
        JSONObject value = new JSONObject(frame.toString());
        value.remove("retiredTimers188");value.remove("timeLogs162");value.remove("latestTimer162");value.remove("manualAnchor162");value.remove("timerConfig"); value.remove("automaticEnabled"); value.remove("transition");
        value.put("nativeExecutionProtocol", 2);
        JSONObject transition = frame.optJSONObject("transition"), plan = transition == null ? null : transition.optJSONObject("plan");
        if (plan != null) {
            JSONObject timer = IntervalTimerService.snapshot(context);
            if (plan.getString("key").equals(timer.optString("key"))) {
                boolean interrupted = "interrupted".equals(transition.optString("state")) || timer.optBoolean("interrupted");
                boolean running = timer.optBoolean("running") && !interrupted;
                long remaining = timer.optLong("remainingMs");
                boolean restComplete=plan.optBoolean("restOnly") && "complete".equals(timer.optString("phase"));
                // The last recorded exercise stays in the execution frame while
                // resting. Once rest ends, display the plan's actual next target.
                if(restComplete&&!interrupted)value.put("currentExercise",transition.getInt("currentExercise"));
                JSONObject phaseStep = plan.getJSONArray("steps").getJSONObject(timer.optInt("stepIndex"));
                value.put("automation", new JSONObject().put("owner", "phone").put("enabled", true).put("key", plan.getString("key"))
                        .put("phase", interrupted ? "ready" : plan.optBoolean("restOnly") && "complete".equals(timer.optString("phase")) ? "ready" : timer.optString("phase"))
                        .put("paused", interrupted || !running && !"complete".equals(timer.optString("phase"))).put("remainingMs", remaining).put("deadlineAt", running ? System.currentTimeMillis() + remaining : 0)
                        .put("workSeconds", phaseStep.getInt("work")).put("restSeconds", "rest".equals(timer.optString("phase")) && plan.optInt("initialRest") > 0 && timer.optLong("elapsedMs") < plan.optLong("initialRest") * 1000 ? plan.getInt("initialRest") : phaseStep.getInt("rest"))
                        .put("phaseNumber", timer.optLong("phaseNumber")).put("targetExercise", transition.getInt("currentExercise"))
                        .put("vibration", false).put("interrupted", interrupted));
            }
        } else if (transition != null) value.put("automation", new JSONObject().put("enabled", true).put("owner", "phone")
                .put("phase", "paused".equals(transition.optString("phase"))?"ready":transition.optString("phase", "ready")).put("remainingMs", 0)
                .put("key", "paused".equals(transition.optString("state"))?"undo188:"+frame.optJSONObject("undo188").optString("commandId"):"").put("paused", "paused".equals(transition.optString("state"))));
        if(frame.optBoolean("workoutPaused")){
            JSONObject a=value.optJSONObject("automation");if(a!=null)a.put("paused",true).put("deadlineAt",0);
            value.put("restEndAt",0);
        }
        return value;
    }
    static void setWorkoutPause(Context context,JSONObject input) throws Exception {

        if(input==null||input.optString("workoutId").isEmpty())throw new IllegalArgumentException("missingWorkoutPause");
        if(!PhoneOwnership193.phoneAllowed(context,input.optString("workoutId")))throw new IllegalStateException("Workout ownership changed");
        JSONObject active=ActiveWorkoutStore.current(context);
        PhoneWorkoutExecutionStore db=PhoneWorkoutExecution.store(context);JSONObject before=db.snapshot();
        boolean nativeOwner=before!=null&&before.optBoolean("active")&&input.optString("workoutId").equals(before.optString("workoutId"));
        if(!nativeOwner&&active!=null&&active.optBoolean("active")&&!input.optString("workoutId").equals(active.optString("workoutId")))throw new IllegalStateException("Workout changed");
        JSONObject timer=IntervalTimerService.snapshot(context);
        JSONObject frame=db.setWorkoutPause(input,android.os.SystemClock.elapsedRealtime(),timer.optBoolean("running"));
        if(frame!=null){
            if(frame.optBoolean("workoutPaused")&&timer.optBoolean("running"))IntervalTimerService.command(context,timer.optString("key"),"pause");
            else if(before!=null&&input.optLong("pauseRevision")>before.optLong("pauseRevision")&&!frame.optBoolean("workoutPaused")&&frame.optBoolean("resumeTimerOnContinue201")&&!timer.optBoolean("running")&&!timer.optBoolean("interrupted")&&!timer.optBoolean("stopped")&&!"complete".equals(timer.optString("phase")))
                IntervalTimerService.command(context,timer.optString("key"),"resume");
            PhoneWorkoutExecution.publish(context);
        }else{
            if(active!=null&&input.optString("workoutId").equals(active.optString("workoutId"))&&input.optLong("pauseRevision")<active.optLong("pauseRevision"))throw new IllegalStateException("staleWorkoutPause");
            JSONObject saved=ActiveWorkoutStore.save(context,input);
            PutDataMapRequest map=PutDataMapRequest.create(WearSyncPlugin.ACTIVE_WORKOUT_PATH);
            map.getDataMap().putString("snapshot",saved.toString());map.getDataMap().putBoolean("active",true);map.getDataMap().putLong("revision",saved.optLong("revision"));
            Wearable.getDataClient(context).putDataItem(map.asPutDataRequest().setUrgent());
        }
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
