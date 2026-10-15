package com.repforge.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Native execution state and receipts commit together before transport acknowledgement. */
final class PhoneWorkoutExecutionStore extends SQLiteOpenHelper {
    private static final String PROCESS=java.util.UUID.randomUUID().toString();
    static final String DATABASE = "trainpilot_phone_execution.db";
    PhoneWorkoutExecutionStore(Context context) { super(context.getApplicationContext(), DATABASE, null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE execution (slot INTEGER PRIMARY KEY CHECK(slot=1), snapshot TEXT NOT NULL)");
        db.execSQL("CREATE TABLE recordings (seq INTEGER PRIMARY KEY AUTOINCREMENT, command_id TEXT NOT NULL UNIQUE,"
                + "workout_id TEXT NOT NULL, generation INTEGER NOT NULL, fingerprint TEXT NOT NULL,"
                + "event TEXT NOT NULL, imported INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE flags (name TEXT PRIMARY KEY, value INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Unsupported execution database migration");
    }

    private static JSONObject current(SQLiteDatabase db) throws JSONException {
        try (Cursor c = db.rawQuery("SELECT snapshot FROM execution WHERE slot=1", null)) {
            return c.moveToFirst() ? new JSONObject(c.getString(0)) : null;
        }
    }
    synchronized JSONObject snapshot() throws JSONException { return current(getReadableDatabase()); }

    private static void save(SQLiteDatabase db, JSONObject frame) {
        db.execSQL("INSERT OR REPLACE INTO execution(slot,snapshot) VALUES(1,?)", new Object[]{frame.toString()});
    }
    private static boolean blocked(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT value FROM flags WHERE name='restore'", null)) {
            return c.moveToFirst() && c.getInt(0) != 0;
        }
    }
    private static boolean pending(SQLiteDatabase db, String workoutId, long generation) {
        try (Cursor c = db.rawQuery("SELECT seq FROM recordings WHERE workout_id=? AND generation=? AND imported=0 LIMIT 1",
                new String[]{workoutId, Long.toString(generation)})) { return c.moveToFirst(); }
    }
    private static JSONObject rejected(String reason) throws JSONException {
        return new JSONObject().put("status", "rejected").put("reason", reason);
    }

    /** Called with the UI's last reconciled execution revision, not a transport snapshot revision. */
    synchronized JSONObject prepare(JSONObject input, long expectedRevision) throws JSONException {
        if (input == null) throw new IllegalArgumentException("missingFrame");
        JSONObject incoming = new JSONObject(input.toString());
        PhoneWorkoutRecording.validateFrame(incoming);
        PhoneWorkoutPlans.validateConfig(incoming);
        if (PhoneWorkoutRecording.version(incoming, "executionRevision") != expectedRevision)
            throw new IllegalArgumentException("revisionConflict");
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            if (blocked(db)) throw new IllegalStateException("restoreInProgress");
            JSONObject old = current(db);
            if (old == null) {
                if (expectedRevision != 0) throw new IllegalArgumentException("revisionConflict");
            } else if (old.getBoolean("active")) {
                if (!old.getString("workoutId").equals(incoming.getString("workoutId"))
                        || old.getLong("generation") != incoming.getLong("generation"))
                    throw new IllegalArgumentException("executionIdentityChanged");
                if (old.getLong("executionRevision") != expectedRevision)
                    throw new IllegalArgumentException("revisionConflict");
                if (pending(db, old.getString("workoutId"), old.getLong("generation")))
                    throw new IllegalStateException("pendingRecordingEvents");
                PhoneWorkoutRecording.retainCompleted(old, incoming);
                if(old.has("undo188"))incoming.put("undo188",old.get("undo188"));
                JSONObject last=old.optJSONObject("lastRecord188");
                if(last!=null&&WorkoutUndo188.same(last.optJSONArray("expected"),WorkoutUndo188.guard(incoming)))incoming.put("lastRecord188",last);
                else incoming.remove("lastRecord188");
                if(incoming.optLong("pauseRevision")<=old.optLong("pauseRevision"))
                    for(String key:new String[]{"pauseRevision","workoutPaused","activeIntervals"})if(old.has(key))incoming.put(key,old.get(key));
                for(String key:new String[]{"timeLogs162","latestTimer162","retiredTimers188","manualAnchor162","resumeTimerOnContinue201"})if(old.has(key))incoming.put(key,old.get(key));
                if (old.has("transition")) {
                    JSONObject transition=old.getJSONObject("transition"),plan=transition.optJSONObject("plan"),automation=incoming.optJSONObject("automation");
                    if (plan!=null&&automation!=null&&plan.optString("key").equals(automation.optString("key"))) incoming.put("transition",transition);
                    else if("paused".equals(transition.optString("state"))&&automation!=null&&automation.optBoolean("paused")&&automation.optString("key").startsWith("undo188:"))incoming.put("transition",transition);
                    else incoming.remove("transition");
                }
            } else if (expectedRevision != 0 || incoming.getLong("generation") <= old.getLong("generation")
                    || incoming.getString("workoutId").equals(old.getString("workoutId"))) {
                throw new IllegalArgumentException("closedOrStaleExecution");
            }
            incoming.put("executionRevision", PhoneWorkoutRecording.advance(expectedRevision));
            save(db, incoming); db.setTransactionSuccessful(); return incoming;
        } finally { db.endTransaction(); }
    }

    /** Mutation and replay receipt commit together, before the Wear queue may be acknowledged. */
    synchronized JSONObject record(JSONObject command) throws JSONException {
        if (command == null) return rejected("invalidCommand");
        String id;
        try { id = PhoneWorkoutRecording.id(command, "commandId"); }
        catch (JSONException | IllegalArgumentException invalid) { return rejected("missingCommandIdentity"); }
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            if (blocked(db)) return rejected("restoreInProgress");
            String fingerprint = PhoneWorkoutRecording.fingerprint(command);
            try (Cursor c = db.rawQuery("SELECT seq, fingerprint, event FROM recordings WHERE command_id=?", new String[]{id})) {
                if (c.moveToFirst()) {
                    if (!fingerprint.equals(c.getString(1))) return rejected("commandIdentityCollision");
                    JSONObject result = new JSONObject(c.getString(2));
                    result.put("sequence", c.getLong(0)).put("status", "duplicate");
                    db.setTransactionSuccessful(); return result;
                }
            }
            JSONObject frame = current(db);
            if (frame == null) return rejected("noExecution");
            JSONObject next;
            try {
                next = PhoneWorkoutRecording.record(frame, command);
                finishManual(next,command,android.os.SystemClock.elapsedRealtime());
                JSONObject transition = PhoneWorkoutPlans.afterRecord(next, next.getInt("currentExercise"));
                if (transition != null) next.put("transition", transition);
            }
            catch (JSONException | IllegalArgumentException invalid) {
                return rejected(invalid instanceof JSONException ? "invalidCommand" : invalid.getMessage());
            }
            next.put("lastRecord188",WorkoutUndo188.capture(frame,next,command,id));
            PhoneWorkoutRecording.Target target = PhoneWorkoutRecording.target(frame, command);
            JSONObject values = PhoneWorkoutRecording.recordedValues(next, command);
            JSONObject result = new JSONObject().put("status", "applied").put("commandId", id)
                    .put("workoutId", next.getString("workoutId")).put("generation", next.getLong("generation"))
                    .put("executionRevision", next.getLong("executionRevision"))
                    .put("exerciseId", target.exercise.getString("id")).put("setId", target.set.getString("setId"))
                    .put("recordedAt", System.currentTimeMillis()).put("values", values)
                    .put("before",new JSONObject().put("reps",target.set.opt("reps")).put("weight",target.set.opt("weight")));
            if (next.has("transition")) result.put("transition", next.getJSONObject("transition"));
            next.put("watchSequence", Math.max(next.optLong("watchSequence"), command.optLong("sequence")));
            ContentValues row = new ContentValues(); row.put("command_id", id); row.put("fingerprint", fingerprint);
            row.put("workout_id", next.getString("workoutId")); row.put("generation", next.getLong("generation"));
            row.put("event", result.toString());
            long sequence = db.insertOrThrow("recordings", null, row);
            save(db, next); result.put("sequence", sequence);
            db.setTransactionSuccessful(); return result;
        } finally { db.endTransaction(); }
    }


    /** Append a guarded inverse beside the original event; never delete recordings or measured time. */
    synchronized JSONObject undo(JSONObject command)throws JSONException {
        String id=PhoneWorkoutRecording.id(command,"commandId");SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            if(blocked(db))return rejected("restoreInProgress");
            JSONObject replay=receipt(command);if(replay!=null)return replay;
            JSONObject old=current(db);if(old==null)return rejected("noExecution");
            JSONObject next;
            try{
                JSONObject accounted=new JSONObject(old.toString()),anchor=accounted.optJSONObject("manualAnchor162");
                if(anchor!=null)finishManual(accounted,anchor,android.os.SystemClock.elapsedRealtime());
                next=WorkoutUndo188.inverse(accounted,command);
                JSONObject timer=old.optJSONObject("latestTimer162");
                if(timer!=null&&!timer.optString("key").isEmpty()){
                    JSONArray retired=next.optJSONArray("retiredTimers188");if(retired==null)retired=new JSONArray();
                    retired.put(timer.getString("key"));next.put("retiredTimers188",retired);
                }
            }catch(JSONException|IllegalArgumentException conflict){return rejected("undoConflict");}
            JSONObject event=new JSONObject().put("kind","undoSet").put("status","applied").put("commandId",id)
                .put("recordId",command.getString("recordId")).put("workoutId",next.getString("workoutId")).put("generation",next.getLong("generation"))
                .put("executionRevision",next.getLong("executionRevision")).put("exerciseId",command.getString("exerciseId")).put("setId",command.getString("setId"))
                .put("before",WorkoutUndo188.values(WorkoutUndo188.find(old,command))).put("values",WorkoutUndo188.values(WorkoutUndo188.find(next,command)))
                .put("transition",next.getJSONObject("transition"));
            next.put("watchSequence",Math.max(next.optLong("watchSequence"),command.optLong("sequence")));
            ContentValues row=new ContentValues();row.put("command_id",id);row.put("workout_id",next.getString("workoutId"));row.put("generation",next.getLong("generation"));
            row.put("fingerprint",PhoneWorkoutRecording.fingerprint(command));row.put("event",event.toString());
            long seq=db.insertOrThrow("recordings",null,row);save(db,next);event.put("sequence",seq);db.setTransactionSuccessful();return event;
        }finally{db.endTransaction();}
    }

    /** Cumulative checkpoints stay in the snapshot; only completed sets create journal events. */
    synchronized boolean checkpointTimer(JSONObject timer, long monotonic) throws JSONException {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            JSONObject frame=current(db);if(frame==null||!frame.optBoolean("active")||!frame.optString("workoutId").equals(timer.optString("workoutId"))
                    ||frame.optLong("generation")!=timer.optLong("generation"))return false;
            JSONArray retired=frame.optJSONArray("retiredTimers188");
            if(retired!=null)for(int r=0;r<retired.length();r++)if(retired.getString(r).equals(timer.optString("key")))return false;
            JSONObject ledger=timer.getJSONObject("ledger"),logs=frame.optJSONObject("timeLogs162");if(logs==null)logs=new JSONObject();
            String key=PhoneWorkoutRecording.id(timer,"key");JSONObject previous=logs.optJSONObject(key);
            if(previous!=null&&(ledger.getLong("positionMs")<previous.getLong("positionMs")||ledger.getLong("workMs")<previous.getLong("workMs")
                    ||ledger.getLong("restMs")<previous.getLong("restMs")||ledger.getLong("pausedMs")<previous.getLong("pausedMs")))return false;
            JSONObject log=new JSONObject(ledger.toString()).put("key",key).put("phase",timer.optString("phase"))
                    .put("interrupted",timer.optBoolean("interrupted")).put("stopped",timer.optBoolean("stopped"));
            logs.put(key,log);frame.put("timeLogs162",logs).put("latestTimer162",timer);
            JSONObject beforeUndo188=new JSONObject(frame.toString());
            JSONArray changes=new JSONArray(),rows=timer.getJSONArray("steps"),parts=ledger.getJSONArray("steps");
            if(timer.optBoolean("autoAdvance")&&!timer.optBoolean("restOnly"))for(int i=0;i<timer.optInt("completed");i++){
                JSONObject row=rows.getJSONObject(i),part=parts.getJSONObject(i),set=findSet(frame,row);
                if(set==null)throw new IllegalArgumentException("timerTargetRemoved");
                if(!set.getBoolean("done")){
                    JSONObject before=new JSONObject().put("reps",set.opt("reps"));
                    String reps=String.valueOf((long)Math.floor(row.optDouble("baseSeconds",0)+(part.getLong("workMs")+part.getLong("restMs"))/1000.0));
                    set.put("done",true).put("reps",reps);
                    changes.put(new JSONObject().put("exerciseId",row.getString("exerciseId")).put("setId",row.getString("setId"))
                            .put("values",new JSONObject().put("done",true).put("reps",reps)).put("before",before));
                }
            }
            if(changes.length()>0){
                frame.put("executionRevision",PhoneWorkoutRecording.advance(frame.getLong("executionRevision")));
                JSONObject event=new JSONObject().put("kind","timedSets").put("workoutId",frame.getString("workoutId"))
                        .put("generation",frame.getLong("generation")).put("executionRevision",frame.getLong("executionRevision"))
                        .put("key",key).put("changes",changes);
                ContentValues row=new ContentValues();row.put("command_id","timer:"+key+":"+timer.getInt("completed"));
                row.put("workout_id",frame.getString("workoutId"));row.put("generation",frame.getLong("generation"));row.put("fingerprint",event.toString());row.put("event",event.toString());
                db.insertOrThrow("recordings",null,row);
                JSONObject lastChange=changes.getJSONObject(changes.length()-1);
                frame.put("lastRecord188",WorkoutUndo188.capture(beforeUndo188,frame,lastChange,"timer:"+key+":"+timer.getInt("completed")));
            }
            if(timer.optBoolean("autoAdvance")){
                JSONObject step=rows.getJSONObject(timer.optInt("stepIndex"));JSONArray exercises=frame.getJSONArray("exercises");
                for(int e=0;e<exercises.length();e++)if(findSet(new JSONObject().put("exercises",new JSONArray().put(exercises.getJSONObject(e))),step)!=null){
                    int selected=e;
                    if("complete".equals(timer.optString("phase"))&&e+1<exercises.length())selected=e+1;
                    frame.put("currentExercise",selected);break;
                }
            }
            if("complete".equals(timer.optString("phase"))&&!timer.optBoolean("stopped"))anchorManual(frame,monotonic);
            save(db,frame);db.setTransactionSuccessful();return changes.length()>0;
        }finally{db.endTransaction();}
    }
    private static JSONObject findSet(JSONObject frame,JSONObject target)throws JSONException{
        JSONArray exercises=frame.getJSONArray("exercises");
        for(int e=0;e<exercises.length();e++){
            JSONObject exercise=exercises.getJSONObject(e);if(!exercise.getString("id").equals(target.getString("exerciseId")))continue;
            JSONArray sets=exercise.getJSONArray("sets");for(int s=0;s<sets.length();s++)if(sets.getJSONObject(s).getString("setId").equals(target.getString("setId")))return sets.getJSONObject(s);
        }return null;
    }
    private static void anchorManual(JSONObject frame,long monotonic)throws JSONException{
        JSONObject previous=frame.optJSONObject("manualAnchor162");
        frame.remove("manualAnchor162");if(!frame.optBoolean("automaticEnabled")||frame.optBoolean("workoutPaused")){if(previous!=null)frame.put("manualAnchor162",previous);return;}
        JSONArray exercises=frame.getJSONArray("exercises");
        for(int e=frame.optInt("currentExercise");e<exercises.length();e++){
            JSONObject exercise=exercises.getJSONObject(e);JSONArray sets=exercise.getJSONArray("sets");
            for(int s=0;s<sets.length();s++)if(!sets.getJSONObject(s).getBoolean("done")){
                if(!java.util.Arrays.asList("","ism.","reps","/kar","/láb").contains(exercise.optString("repUnit")))return;
                if(previous!=null&&PROCESS.equals(previous.optString("process"))&&exercise.getString("id").equals(previous.optString("exerciseId"))
                    &&sets.getJSONObject(s).getString("setId").equals(previous.optString("setId"))){frame.put("manualAnchor162",previous);return;}
                JSONObject anchor=new JSONObject().put("exerciseId",exercise.getString("id")).put("setId",sets.getJSONObject(s).getString("setId"))
                        .put("began",monotonic).put("process",PROCESS).put("key","manual|"+frame.getLong("generation")+"|"+frame.getLong("executionRevision")+"|"+sets.getJSONObject(s).getString("setId"));
                frame.put("manualAnchor162",anchor);return;
            }
        }
    }
    synchronized void beginManualWork(String workoutId,long generation,long monotonic)throws JSONException{
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{JSONObject frame=current(db);if(frame!=null&&frame.optBoolean("active")&&workoutId.equals(frame.optString("workoutId"))&&generation==frame.optLong("generation")){
            if(frame.optJSONObject("transition")!=null&&"paused".equals(frame.getJSONObject("transition").optString("state")))return;
            JSONObject timer=frame.optJSONObject("latestTimer162");
            if(timer!=null&&!timer.optBoolean("stopped")&&!timer.optBoolean("interrupted")&&!"complete".equals(timer.optString("phase")))return;
            anchorManual(frame,monotonic);save(db,frame);db.setTransactionSuccessful();
        }}finally{db.endTransaction();}
    }
    private static void finishManual(JSONObject next,JSONObject command,long now)throws JSONException{
        JSONObject anchor=next.optJSONObject("manualAnchor162");next.remove("manualAnchor162");
        if(anchor==null||!PROCESS.equals(anchor.optString("process"))||!anchor.optString("exerciseId").equals(command.optString("exerciseId"))||!anchor.optString("setId").equals(command.optString("setId")))return;
        long work=Math.min(IntervalTimeline.MAX_MS,anchor.optLong("priorWorkMs201")+(anchor.optBoolean("paused201")?0:Math.max(0,now-anchor.getLong("began"))));
        JSONObject log=new JSONObject().put("schema",1).put("owner","phone").put("key",anchor.getString("key"))
                .put("positionMs",work).put("workMs",work).put("restMs",0).put("pausedMs",0)
                .put("steps",new JSONArray().put(new JSONObject().put("exerciseId",anchor.getString("exerciseId")).put("setId",anchor.getString("setId"))
                        .put("workMs",work).put("restMs",0).put("pausedMs",0)));
        JSONObject logs=next.optJSONObject("timeLogs162");if(logs==null)logs=new JSONObject();logs.put(anchor.getString("key"),log);next.put("timeLogs162",logs);
    }

    /** Explicit phone edits use a CAS and a matching base; ordinary snapshots still cannot undo recordings. */
    synchronized JSONObject amend(JSONObject input,long expectedRevision,JSONObject bases)throws JSONException{
        JSONObject incoming=new JSONObject(input.toString());PhoneWorkoutRecording.validateFrame(incoming);PhoneWorkoutPlans.validateConfig(incoming);
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            JSONObject old=current(db);
            if(blocked(db)||old==null||!old.optBoolean("active")||!old.getString("workoutId").equals(incoming.getString("workoutId"))
                ||old.getLong("generation")!=incoming.getLong("generation")||old.getLong("executionRevision")!=expectedRevision
                ||incoming.getLong("executionRevision")!=expectedRevision||pending(db,old.getString("workoutId"),old.getLong("generation")))throw new IllegalStateException("amendmentConflict");
            JSONArray exercises=old.getJSONArray("exercises");
            for(int e=0;e<exercises.length();e++){
                JSONObject exercise=exercises.getJSONObject(e);JSONArray sets=exercise.getJSONArray("sets");
                for(int s=0;s<sets.length();s++){
                    JSONObject set=sets.getJSONObject(s);if(!set.getBoolean("done"))continue;
                    JSONObject target=new JSONObject().put("exerciseId",exercise.getString("id")).put("setId",set.getString("setId"));
                    JSONObject edited=findSet(incoming,target);if(edited==null)throw new IllegalStateException("amendmentTargetRemoved");
                    JSONObject base=bases.optJSONObject(set.getString("setId"));
                    if(base==null||!base.toString().equals(recordedBase(set).toString()))throw new IllegalStateException("amendmentConflict");
                }
            }
            incoming.remove("lastRecord188");
            if(old.has("undo188"))incoming.put("undo188",old.get("undo188"));
            for(String key:new String[]{"timeLogs162","latestTimer162","retiredTimers188"})if(old.has(key))incoming.put(key,old.get(key));
            incoming.remove("manualAnchor162");
            JSONObject anchor=old.optJSONObject("manualAnchor162");
            if(anchor!=null&&old.optInt("currentExercise")==incoming.optInt("currentExercise")){
                JSONObject target=findSet(incoming,anchor);
                if(target!=null&&!target.getBoolean("done"))incoming.put("manualAnchor162",anchor);
            }
            JSONObject previousTransition=old.optJSONObject("transition"),automation=incoming.optJSONObject("automation");
            if(previousTransition!=null&&previousTransition.optJSONObject("plan")!=null&&automation!=null&&previousTransition.getJSONObject("plan").optString("key").equals(automation.optString("key")))incoming.put("transition",previousTransition);
            else incoming.remove("transition");
            incoming.put("executionRevision",PhoneWorkoutRecording.advance(expectedRevision));save(db,incoming);db.setTransactionSuccessful();return incoming;
        }finally{db.endTransaction();}
    }
    static JSONObject recordedBase(JSONObject set)throws JSONException{
        JSONObject result=new JSONObject();for(String key:new String[]{"done","reps","weight","leftSeconds","rightSeconds","distanceMeters"})if(set.has(key))result.put(key,set.get(key));return result;
    }

    synchronized boolean recorded188(String workoutId,long generation,String recordId) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT seq FROM recordings WHERE command_id=? AND workout_id=? AND generation=?",
            new String[]{recordId,workoutId,Long.toString(generation)})){return c.moveToFirst();}
    }

    synchronized JSONObject receipt(JSONObject command) throws JSONException {
        String id = PhoneWorkoutRecording.id(command, "commandId");
        try (Cursor c = getReadableDatabase().rawQuery("SELECT seq, fingerprint, event FROM recordings WHERE command_id=?", new String[]{id})) {
            if (!c.moveToFirst()) return null;
            if (!PhoneWorkoutRecording.fingerprint(command).equals(c.getString(1))) return rejected("commandIdentityCollision");
            return new JSONObject(c.getString(2)).put("sequence", c.getLong(0)).put("status", "duplicate");
        }
    }

    /** Explicit watch start uses the same durable outbox as a recorded repetition set. */
    synchronized JSONObject startTimeline(JSONObject command) throws JSONException {
        String id = PhoneWorkoutRecording.id(command, "commandId");
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            if (blocked(db)) return rejected("restoreInProgress");
            String fingerprint = PhoneWorkoutRecording.fingerprint(command);
            try (Cursor c = db.rawQuery("SELECT seq, fingerprint, event FROM recordings WHERE command_id=?", new String[]{id})) {
                if (c.moveToFirst()) {
                    if (!fingerprint.equals(c.getString(1))) return rejected("commandIdentityCollision");
                    return new JSONObject(c.getString(2)).put("sequence", c.getLong(0)).put("status", "duplicate");
                }
            }
            JSONObject old = current(db);
            if (old == null) return rejected("noExecution");
            JSONObject next = new JSONObject(old.toString());
            try {
                PhoneWorkoutRecording.validateFrame(next);
                if (!"automaticControl".equals(command.optString("action")) || !("start".equals(command.optString("value"))||"resumeUndo".equals(command.optString("value"))))
                    throw new IllegalArgumentException("unsupportedAction");
                if (!next.getString("workoutId").equals(PhoneWorkoutRecording.id(command, "workoutId"))
                        || next.getLong("generation") != PhoneWorkoutRecording.version(command, "generation")
                        || next.getLong("executionRevision") != PhoneWorkoutRecording.version(command, "executionRevision"))
                    throw new IllegalArgumentException("revisionConflict");
                next.put("executionRevision", PhoneWorkoutRecording.advance(next.getLong("executionRevision")));
                if("resumeUndo".equals(command.optString("value"))){
                    if(!"paused".equals(next.optJSONObject("transition")==null?"":next.getJSONObject("transition").optString("state")))throw new IllegalArgumentException("notUndoPaused");
                    next.put("transition",new JSONObject().put("state","ready").put("phase","ready").put("currentExercise",next.optInt("currentExercise")));
                    JSONObject a=next.optJSONObject("automation");if(a!=null)a.put("paused",false);
                    anchorManual(next,android.os.SystemClock.elapsedRealtime());
                }else{
                JSONObject transition=PhoneWorkoutPlans.startTimed(next),first=transition.getJSONObject("plan").getJSONArray("steps").getJSONObject(0);
                if (!first.getString("exerciseId").equals(PhoneWorkoutRecording.id(command,"exerciseId"))
                        || !first.getString("setId").equals(PhoneWorkoutRecording.id(command,"setId")))
                    throw new IllegalArgumentException("targetChanged");
                next.put("transition",transition);
                }
            } catch (JSONException | IllegalArgumentException invalid) { return rejected("invalidTimelineStart"); }
            next.put("watchSequence", Math.max(next.optLong("watchSequence"), command.optLong("sequence")));
            JSONObject event = new JSONObject().put("status", "applied").put("kind", "startTimeline")
                    .put("commandId", id).put("workoutId", next.getString("workoutId"))
                    .put("generation", next.getLong("generation")).put("executionRevision", next.getLong("executionRevision"))
                    .put("transition", next.getJSONObject("transition"));
            ContentValues row = new ContentValues(); row.put("command_id", id); row.put("fingerprint", fingerprint);
            row.put("workout_id", next.getString("workoutId")); row.put("generation", next.getLong("generation"));
            row.put("event", event.toString()); long sequence = db.insertOrThrow("recordings", null, row);
            save(db, next); event.put("sequence", sequence); db.setTransactionSuccessful(); return event;
        } finally { db.endTransaction(); }
    }

    /** Delivery progress changes no user data and cannot consume a newer timeline request. */
    synchronized boolean markTransition(String key, String state) throws JSONException {
        if (!java.util.Arrays.asList("started", "interrupted").contains(state)) throw new IllegalArgumentException("invalidTransitionState");
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            JSONObject frame = current(db), t = frame == null ? null : frame.optJSONObject("transition");
            JSONObject plan = t == null ? null : t.optJSONObject("plan");
            if (frame == null || !frame.optBoolean("active") || plan == null || !key.equals(plan.optString("key"))) return false;
            t.put("state", state); save(db, frame); db.setTransactionSuccessful(); return true;
        } finally { db.endTransaction(); }
    }

    /** A separate revision fences stale transport snapshots without invalidating pending set receipts. */
    synchronized JSONObject setWorkoutPause(JSONObject input,long monotonic,boolean running)throws JSONException {
        String id=input.optString("workoutId");long revision=input.optLong("pauseRevision");
        if(id.isEmpty()||revision<1||revision>9007199254740991L)throw new IllegalArgumentException("invalidWorkoutPause");
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            JSONObject frame=current(db);
            if(frame==null||!frame.optBoolean("active")||!id.equals(frame.optString("workoutId")))return null;
            if(revision<frame.optLong("pauseRevision"))throw new IllegalStateException("staleWorkoutPause");
            boolean paused=input.optBoolean("workoutPaused");
            if(revision==frame.optLong("pauseRevision")&&paused!=frame.optBoolean("workoutPaused"))throw new IllegalStateException("staleWorkoutPause");
            if(revision>frame.optLong("pauseRevision")){
                if(paused&&!frame.optBoolean("workoutPaused"))frame.put("resumeTimerOnContinue201",running);
                JSONObject anchor=frame.optJSONObject("manualAnchor162");
                if(anchor!=null&&PROCESS.equals(anchor.optString("process"))){
                    if(paused&&!anchor.optBoolean("paused201"))anchor.put("priorWorkMs201",anchor.optLong("priorWorkMs201")+Math.max(0,monotonic-anchor.optLong("began")));
                    anchor.put("paused201",paused).put("began",monotonic);
                }
                frame.put("workoutPaused",paused).put("pauseRevision",revision);
                if(input.optJSONArray("activeIntervals")!=null)frame.put("activeIntervals",new JSONArray(input.getJSONArray("activeIntervals").toString()));
                save(db,frame);
            }
            db.setTransactionSuccessful();return frame;
        }finally{db.endTransaction();}
    }

    synchronized void suspendCommands(boolean value) {
        getWritableDatabase().execSQL("INSERT OR REPLACE INTO flags(name,value) VALUES('suspended',?)", new Object[]{value ? 1 : 0});
    }
    synchronized boolean commandsSuspended() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT value FROM flags WHERE name='suspended'", null)) {
            return c.moveToFirst() && c.getInt(0) != 0;
        }
    }

    synchronized JSONArray pendingEvents(String workoutId, long generation, long afterSequence) throws JSONException {
        if (afterSequence < 0) throw new IllegalArgumentException("invalidEventCursor");
        JSONArray events = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT seq, event FROM recordings WHERE workout_id=?"
                + " AND generation=? AND imported=0 AND seq>? ORDER BY seq LIMIT 128",
                new String[]{workoutId, Long.toString(generation), Long.toString(afterSequence)})) {
            while (c.moveToNext()) events.put(new JSONObject(c.getString(1)).put("sequence", c.getLong(0)));
        }
        return events;
    }

    /** Called only after the UI has durably saved its draft and last-applied sequence together. */
    synchronized boolean acknowledgeEvents(String workoutId, long generation, long throughSequence) {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            try (Cursor c = db.rawQuery("SELECT seq FROM recordings WHERE seq=? AND workout_id=? AND generation=?",
                    new String[]{Long.toString(throughSequence), workoutId, Long.toString(generation)})) {
                if (!c.moveToFirst()) return false;
            }
            ContentValues values = new ContentValues(); values.put("imported", 1);
            db.update("recordings", values, "workout_id=? AND generation=? AND seq<=?",
                    new String[]{workoutId, Long.toString(generation), Long.toString(throughSequence)});
            db.setTransactionSuccessful(); return true;
        } finally { db.endTransaction(); }
    }

    synchronized boolean restoreBlocked() { return blocked(getReadableDatabase()); }

    synchronized void setRestoreBlocked(boolean value) {
        getWritableDatabase().execSQL("INSERT OR REPLACE INTO flags(name,value) VALUES('restore',?)", new Object[]{value ? 1 : 0});
    }

    synchronized boolean closeExecution(String workoutId, long generation, long expectedRevision) throws JSONException {
        return endExecution(workoutId, generation, expectedRevision, false);
    }
    /** Explicit confirmed discard may abandon pending events; ordinary close still requires reconciliation. */
    synchronized boolean discardExecution(String workoutId, long generation, long expectedRevision) throws JSONException {
        return endExecution(workoutId, generation, expectedRevision, true);
    }
    private boolean endExecution(String workoutId, long generation, long expectedRevision, boolean discard) throws JSONException {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            JSONObject frame = current(db);
            if (blocked(db) || frame == null || !workoutId.equals(frame.getString("workoutId"))
                    || generation != frame.getLong("generation") || expectedRevision != frame.getLong("executionRevision")
                    || !discard && pending(db, workoutId, generation)) return false;
            if (frame.getBoolean("active")) {
                frame.put("active", false).put("executionRevision", PhoneWorkoutRecording.advance(expectedRevision)); save(db, frame);
            }
            db.setTransactionSuccessful(); return true;
        } finally { db.endTransaction(); }
    }
}
