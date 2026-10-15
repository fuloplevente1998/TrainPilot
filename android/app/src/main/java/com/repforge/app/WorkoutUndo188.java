package com.repforge.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Ordered stable identities and all editable set values; clocks/health are deliberately outside the guard. */
final class WorkoutUndo188 {
    private WorkoutUndo188() {}
    static double number(JSONObject set,String key) {
        String text=set.optString(key,"").replace(',','.');
        return text.isEmpty()?0:Double.parseDouble(text);
    }
    static JSONArray guard(JSONObject frame)throws JSONException {
        JSONArray rows=new JSONArray(),exercises=frame.getJSONArray("exercises");
        for(int e=0;e<exercises.length();e++){
            JSONObject exercise=exercises.getJSONObject(e);JSONArray sets=exercise.getJSONArray("sets");
            for(int s=0;s<sets.length();s++){
                JSONObject set=sets.getJSONObject(s);JSONArray row=new JSONArray().put(exercise.getString("id")).put(set.getString("setId")).put(set.getBoolean("done"));
                for(String key:new String[]{"reps","weight","leftSeconds","rightSeconds","distanceMeters"})row.put(number(set,key));
                rows.put(row);
            }
        }return rows;
    }
    static boolean same(JSONArray left,JSONArray right)throws JSONException {
        if(left==null||right==null||left.length()!=right.length())return false;
        for(int i=0;i<left.length();i++){
            JSONArray a=left.getJSONArray(i),b=right.getJSONArray(i);if(a.length()!=8||b.length()!=8)return false;
            for(int j=0;j<3;j++)if(!a.get(j).equals(b.get(j)))return false;
            for(int j=3;j<8;j++)if(a.getDouble(j)!=b.getDouble(j))return false;
        }return true;
    }
    static JSONObject values(JSONObject set)throws JSONException {
        JSONObject out=new JSONObject().put("done",set.getBoolean("done"));
        for(String key:new String[]{"reps","weight","leftSeconds","rightSeconds","distanceMeters"})out.put(key,set.opt(key)==null? ("reps".equals(key)?"":0):set.opt(key));
        return out;
    }
    static JSONObject capture(JSONObject before,JSONObject after,JSONObject target,String recordId)throws JSONException {
        return new JSONObject().put("recordId",recordId).put("workoutId",before.getString("workoutId"))
            .put("exerciseId",target.getString("exerciseId")).put("setId",target.getString("setId"))
            .put("before",values(find(before,target))).put("expected",guard(after)).put("recordedAt",System.currentTimeMillis());
    }
    static JSONObject find(JSONObject frame,JSONObject target)throws JSONException {
        JSONArray exercises=frame.getJSONArray("exercises");
        for(int e=0;e<exercises.length();e++){
            JSONObject exercise=exercises.getJSONObject(e);if(!exercise.getString("id").equals(target.getString("exerciseId")))continue;
            JSONArray sets=exercise.getJSONArray("sets");for(int s=0;s<sets.length();s++)if(sets.getJSONObject(s).getString("setId").equals(target.getString("setId")))return sets.getJSONObject(s);
        }throw new IllegalArgumentException("undoTargetChanged");
    }
    static JSONObject inverse(JSONObject frame,JSONObject command)throws JSONException {
        PhoneWorkoutRecording.validateFrame(frame);
        JSONObject last=frame.optJSONObject("lastRecord188");
        if(last==null||!last.getString("recordId").equals(command.optString("recordId"))
            ||!last.getString("exerciseId").equals(command.optString("exerciseId"))||!last.getString("setId").equals(command.optString("setId"))
            ||!frame.getString("workoutId").equals(command.optString("workoutId"))||frame.getLong("generation")!=command.optLong("generation")
            ||!same(last.getJSONArray("expected"),guard(frame))||!same(command.optJSONArray("expected"),guard(frame)))throw new IllegalArgumentException("undoConflict");
        JSONObject next=new JSONObject(frame.toString()),set=find(next,last),before=last.getJSONObject("before");
        for(String key:new String[]{"done","reps","weight","leftSeconds","rightSeconds","distanceMeters"})set.put(key,before.get(key));
        JSONArray exercises=next.getJSONArray("exercises");for(int e=0;e<exercises.length();e++)if(exercises.getJSONObject(e).getString("id").equals(last.getString("exerciseId")))next.put("currentExercise",e);
        next.put("restEndAt",0).put("executionRevision",PhoneWorkoutRecording.advance(frame.getLong("executionRevision")));
        next.remove("lastRecord188");next.remove("manualAnchor162");
        JSONObject a=new JSONObject().put("owner","phone").put("enabled",frame.optBoolean("automaticEnabled")).put("phase","ready")
            .put("paused",true).put("remainingMs",0).put("deadlineAt",0).put("key","undo188:"+command.getString("commandId"));
        next.put("automation",a).put("transition",new JSONObject().put("state","paused").put("phase","paused").put("currentExercise",next.getInt("currentExercise")));
        next.put("undo188",new JSONObject().put("commandId",command.getString("commandId")).put("status","applied"));
        return next;
    }
}
