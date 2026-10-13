package com.repforge.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import org.json.JSONArray;

final class ActiveWorkoutStore {
    private static final String PREFS = "trainpilot_wear_sync";
    private static final String SNAPSHOT = "snapshot";
    private static final String REVISION = "revision";
    private static final String BACKGROUND = "background162";

    private ActiveWorkoutStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static synchronized JSONObject save(Context context, JSONObject input) throws Exception {
        long revision = prefs(context).getLong(REVISION, 0L) + 1L;
        JSONObject snapshot = new JSONObject(input.toString());
        JSONArray receipts = receipts(context);
        for (int i = 0; i < receipts.length(); i++) PhoneAutomaticReducer.overlay(snapshot, receipts.getJSONObject(i));
        snapshot.put("schema", snapshot.optInt("schema", 1));
        snapshot.put("revision", revision);
        snapshot.put("active", true);
        snapshot.put("syncedAt", System.currentTimeMillis());
        prefs(context).edit()
                .putLong(REVISION, revision)
                .putString(SNAPSHOT, snapshot.toString())
                .apply();
        return snapshot;
    }

    static synchronized JSONObject clear(Context context) throws Exception {
        long revision = prefs(context).getLong(REVISION, 0L) + 1L;
        JSONObject snapshot = new JSONObject()
                .put("schema", 1)
                .put("revision", revision)
                .put("active", false)
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

    private static JSONArray receipts(Context context) {
        try { return new JSONArray(prefs(context).getString(BACKGROUND, "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    static synchronized JSONObject receipt(Context context, String id) {
        JSONArray rows = receipts(context);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null && id.equals(row.optString("commandId"))) return row;
        }
        return null;
    }

    static synchronized JSONObject recordBackground(Context context, JSONObject command, JSONObject timer) throws Exception {
        if (receipt(context, command.optString("commandId")) != null) return null;
        JSONArray rows = receipts(context);
        if (rows.length() >= 64) return null; // Never evict an unreconciled recording.
        JSONObject receipt = PhoneAutomaticReducer.reduce(current(context), command, timer);
        if (receipt == null) return null;
        JSONObject snapshot = receipt.getJSONObject("snapshot");
        long revision = prefs(context).getLong(REVISION, 0) + 1;
        snapshot.put("revision", revision).put("syncedAt", System.currentTimeMillis());
        rows.put(receipt);
        // The recording and replay receipt survive together, before starting the timer.
        if (!prefs(context).edit().putLong(REVISION, revision).putString(SNAPSHOT, snapshot.toString())
                .putString(BACKGROUND, rows.toString()).commit()) throw new IllegalStateException("Recording could not be saved");
        return receipt;
    }

    static synchronized void acknowledgeBackground(Context context, String id) {
        JSONArray rows = receipts(context), next = new JSONArray();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null && !id.equals(row.optString("commandId"))) next.put(row);
        }
        prefs(context).edit().putString(BACKGROUND, next.toString()).commit();
    }

    static synchronized void backgroundClock(Context context, JSONObject clock) throws Exception {
        JSONArray rows=receipts(context);JSONObject active=current(context);boolean changed=false;
        for(int i=0;i<rows.length();i++){
            JSONObject receipt=rows.getJSONObject(i),plan=receipt.optJSONObject("plan");
            if(plan==null||!plan.optString("key").equals(clock.optString("key")))continue;
            JSONObject snapshot=receipt.getJSONObject("snapshot");
            projectClock(snapshot,plan,clock);receipt.put("currentExercise",snapshot.optInt("currentExercise"));
            changed=true;
        }
        if(active!=null&&clock.optString("key").startsWith("bg|")&&active.optJSONObject("automation")!=null&&
            clock.optString("key").equals(active.optJSONObject("automation").optString("key"))){
            projectClock(active,clock,clock);changed=true;
        }
        if(changed){SharedPreferences.Editor edit=prefs(context).edit().putString(BACKGROUND,rows.toString());if(active!=null)edit.putString(SNAPSHOT,active.toString());edit.commit();}
    }
    private static void projectClock(JSONObject snapshot,JSONObject plan,JSONObject clock)throws Exception {
        JSONArray steps=plan.getJSONArray("steps"),exercises=snapshot.getJSONArray("exercises");
        int selected=snapshot.optInt("currentExercise");
        if(plan.optBoolean("autoAdvance"))for(int si=0;si<steps.length();si++){
            JSONObject step=steps.getJSONObject(si);boolean complete=si<clock.optInt("completed");
            for(int ei=0;ei<exercises.length();ei++){
                JSONObject e=exercises.getJSONObject(ei);if(!e.optString("id").equals(step.optString("exerciseId")))continue;
                JSONArray sets=e.getJSONArray("sets");
                for(int j=0;j<sets.length();j++)if(sets.getJSONObject(j).optString("setId").equals(step.optString("setId"))){
                    if(si==clock.optInt("stepIndex"))selected=ei;
                    if(complete){long full=(step.optLong("work")*step.optLong("rounds")+step.optLong("rest")*(step.optLong("rounds")-1)+(si<steps.length()-1?step.optLong("rest"):0))*1000;
                        sets.getJSONObject(j).put("done",true).put("reps",String.valueOf(Math.min(86400,step.optLong("baseSeconds")+full/1000)));}
                }
            }
        }
        JSONObject auto=new JSONObject(clock.toString());
        if("complete".equals(auto.optString("phase"))){int[] next=PhoneAutomaticReducer.next(exercises,selected);if(next!=null){selected=next[0];auto.put("phase","ready").put("paused",false);}}
        auto.put("targetExercise",selected).put("awaitingPhone",false);
        snapshot.put("automation",auto).put("currentExercise",selected);
    }
}
