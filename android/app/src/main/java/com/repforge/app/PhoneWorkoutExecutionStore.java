package com.repforge.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Native recording state and durable receipts. Integration is deliberately a separate step. */
final class PhoneWorkoutExecutionStore extends SQLiteOpenHelper {
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
            try { next = PhoneWorkoutRecording.record(frame, command); }
            catch (JSONException | IllegalArgumentException invalid) {
                return rejected(invalid instanceof JSONException ? "invalidCommand" : invalid.getMessage());
            }
            PhoneWorkoutRecording.Target target = PhoneWorkoutRecording.target(frame, command);
            JSONObject values = PhoneWorkoutRecording.recordedValues(next, command);
            JSONObject result = new JSONObject().put("status", "applied").put("commandId", id)
                    .put("workoutId", next.getString("workoutId")).put("generation", next.getLong("generation"))
                    .put("executionRevision", next.getLong("executionRevision"))
                    .put("exerciseId", target.exercise.getString("id")).put("setId", target.set.getString("setId"))
                    .put("recordedAt", System.currentTimeMillis()).put("values", values);
            ContentValues row = new ContentValues(); row.put("command_id", id); row.put("fingerprint", fingerprint);
            row.put("workout_id", next.getString("workoutId")); row.put("generation", next.getLong("generation"));
            row.put("event", result.toString());
            long sequence = db.insertOrThrow("recordings", null, row);
            save(db, next); result.put("sequence", sequence);
            db.setTransactionSuccessful(); return result;
        } finally { db.endTransaction(); }
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

    synchronized void setRestoreBlocked(boolean value) {
        getWritableDatabase().execSQL("INSERT OR REPLACE INTO flags(name,value) VALUES('restore',?)", new Object[]{value ? 1 : 0});
    }

    synchronized boolean closeExecution(String workoutId, long generation, long expectedRevision) throws JSONException {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            JSONObject frame = current(db);
            if (blocked(db) || frame == null || !workoutId.equals(frame.getString("workoutId"))
                    || generation != frame.getLong("generation") || expectedRevision != frame.getLong("executionRevision")
                    || pending(db, workoutId, generation)) return false;
            if (frame.getBoolean("active")) {
                frame.put("active", false).put("executionRevision", PhoneWorkoutRecording.advance(expectedRevision)); save(db, frame);
            }
            db.setTransactionSuccessful(); return true;
        } finally { db.endTransaction(); }
    }
}
