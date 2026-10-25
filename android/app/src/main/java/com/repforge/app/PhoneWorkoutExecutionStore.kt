package com.repforge.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Native execution state and receipts commit together before transport acknowledgement. */
internal class PhoneWorkoutExecutionStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE, null, 1) {

    companion object {
        private val PROCESS: String = UUID.randomUUID().toString()
        const val DATABASE: String = "trainpilot_phone_execution.db"

        @Throws(JSONException::class)
        private fun current(db: SQLiteDatabase): JSONObject? {
            db.rawQuery("SELECT snapshot FROM execution WHERE slot=1", null).use { c ->
                return if (c.moveToFirst()) JSONObject(c.getString(0)) else null
            }
        }

        private fun save(db: SQLiteDatabase, frame: JSONObject) {
            if (frame.optBoolean("active", true)) NativeCloudPhotoDeletion.requireUnreservedSnapshot(frame)
            db.execSQL("INSERT OR REPLACE INTO execution(slot,snapshot) VALUES(1,?)", arrayOf<Any>(frame.toString()))
        }

        private fun blocked(db: SQLiteDatabase): Boolean {
            db.rawQuery("SELECT value FROM flags WHERE name='restore'", null).use { c ->
                return c.moveToFirst() && c.getInt(0) != 0
            }
        }

        private fun pending(db: SQLiteDatabase, workoutId: String, generation: Long): Boolean {
            db.rawQuery(
                "SELECT seq FROM recordings WHERE workout_id=? AND generation=? AND imported=0 LIMIT 1",
                arrayOf(workoutId, generation.toString())
            ).use { c ->
                return c.moveToFirst()
            }
        }

        @Throws(JSONException::class)
        private fun rejected(reason: String): JSONObject {
            return JSONObject().put("status", "rejected").put("reason", reason)
        }

        @Throws(JSONException::class)
        private fun findSet(frame: JSONObject, target: JSONObject): JSONObject? {
            val exercises = frame.getJSONArray("exercises")
            val targetExId = target.getString("exerciseId")
            val targetSetId = target.getString("setId")
            for (e in 0 until exercises.length()) {
                val exercise = exercises.getJSONObject(e)
                if (exercise.getString("id") != targetExId) continue
                val sets = exercise.getJSONArray("sets")
                for (s in 0 until sets.length()) {
                    val set = sets.getJSONObject(s)
                    if (set.getString("setId") == targetSetId) return set
                }
            }
            return null
        }

        @Throws(JSONException::class)
        private fun anchorManual(frame: JSONObject, monotonic: Long) {
            val previous = frame.optJSONObject("manualAnchor162")
            frame.remove("manualAnchor162")
            if (!frame.optBoolean("automaticEnabled") || frame.optBoolean("workoutPaused")) {
                if (previous != null) frame.put("manualAnchor162", previous)
                return
            }
            val exercises = frame.getJSONArray("exercises")
            for (e in frame.optInt("currentExercise") until exercises.length()) {
                val exercise = exercises.getJSONObject(e)
                val sets = exercise.getJSONArray("sets")
                for (s in 0 until sets.length()) {
                    val set = sets.getJSONObject(s)
                    if (!set.getBoolean("done")) {
                        if (exercise.optString("repUnit") !in listOf("", "ism.", "reps", "/kar", "/láb")) return
                        if (previous != null &&
                            PROCESS == previous.optString("process") &&
                            exercise.getString("id") == previous.optString("exerciseId") &&
                            set.getString("setId") == previous.optString("setId")
                        ) {
                            frame.put("manualAnchor162", previous)
                            return
                        }
                        val anchor = JSONObject()
                            .put("exerciseId", exercise.getString("id"))
                            .put("setId", set.getString("setId"))
                            .put("began", monotonic)
                            .put("process", PROCESS)
                            .put(
                                "key",
                                "manual|${frame.getLong("generation")}|${frame.getLong("executionRevision")}|${set.getString("setId")}"
                            )
                        frame.put("manualAnchor162", anchor)
                        return
                    }
                }
            }
        }

        @Throws(JSONException::class)
        private fun finishManual(next: JSONObject, command: JSONObject, now: Long) {
            val anchor = next.optJSONObject("manualAnchor162")
            next.remove("manualAnchor162")
            if (anchor == null ||
                PROCESS != anchor.optString("process") ||
                anchor.optString("exerciseId") != command.optString("exerciseId") ||
                anchor.optString("setId") != command.optString("setId")
            ) {
                return
            }
            val work = min(
                IntervalTimeline.MAX_MS,
                anchor.optLong("priorWorkMs201") +
                    if (anchor.optBoolean("paused201")) 0L else max(0L, now - anchor.getLong("began"))
            )
            val log = JSONObject()
                .put("schema", 1)
                .put("owner", "phone")
                .put("key", anchor.getString("key"))
                .put("positionMs", work)
                .put("workMs", work)
                .put("restMs", 0)
                .put("pausedMs", 0)
                .put(
                    "steps",
                    JSONArray().put(
                        JSONObject()
                            .put("exerciseId", anchor.getString("exerciseId"))
                            .put("setId", anchor.getString("setId"))
                            .put("workMs", work)
                            .put("restMs", 0)
                            .put("pausedMs", 0)
                    )
                )
            var logs = next.optJSONObject("timeLogs162")
            if (logs == null) logs = JSONObject()
            logs.put(anchor.getString("key"), log)
            next.put("timeLogs162", logs)
        }

        @JvmStatic
        @Throws(JSONException::class)
        fun recordedBase(set: JSONObject): JSONObject {
            val result = JSONObject()
            for (key in arrayOf("done", "reps", "weight", "leftSeconds", "rightSeconds", "distanceMeters")) {
                if (set.has(key)) result.put(key, set.get(key))
            }
            return result
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE execution (slot INTEGER PRIMARY KEY CHECK(slot=1), snapshot TEXT NOT NULL)")
        db.execSQL(
            "CREATE TABLE recordings (seq INTEGER PRIMARY KEY AUTOINCREMENT, command_id TEXT NOT NULL UNIQUE," +
                "workout_id TEXT NOT NULL, generation INTEGER NOT NULL, fingerprint TEXT NOT NULL," +
                "event TEXT NOT NULL, imported INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL("CREATE TABLE flags (name TEXT PRIMARY KEY, value INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw IllegalStateException("Unsupported execution database migration")
    }

    @Synchronized
    @Throws(JSONException::class)
    fun snapshot(): JSONObject? = current(readableDatabase)

    /** One consistent frame/cursor pair; projection may acknowledge only this durable state. */
    @Synchronized
    fun projection(): JSONObject {
        val db = readableDatabase
        db.beginTransaction()
        try {
            val frame = current(db)
            var sequence = 0L
            if (frame != null) db.rawQuery(
                "SELECT MAX(seq) FROM recordings WHERE workout_id=? AND generation=?",
                arrayOf(frame.getString("workoutId"), frame.getLong("generation").toString())
            ).use { c -> if (c.moveToFirst() && !c.isNull(0)) sequence = c.getLong(0) }
            db.setTransactionSuccessful()
            return JSONObject().put("snapshot", frame ?: JSONObject.NULL).put("sequence", sequence)
        } finally {
            db.endTransaction()
        }
    }

    /** Only the ownership adapter supplies a locally acknowledged, committed reverse handoff. */
    @Synchronized
    @Throws(Exception::class)
    fun importOwnership193(consent: JSONObject, now: Long): JSONObject {
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (blocked(db)) throw IllegalStateException("restoreInProgress")
            val old = current(db)
            if (old?.optBoolean("active") == true && pending(db, old.getString("workoutId"), old.getLong("generation"))) {
                throw IllegalStateException("pendingRecordingEvents")
            }
            val next = PhoneOwnershipResume193.prepare(consent, old, now)
            if (old == null || next.getLong("generation") != old.getLong("generation")) {
                save(db, next)
                db.execSQL("INSERT OR REPLACE INTO flags(name,value) VALUES('suspended',1)")
            }
            db.setTransactionSuccessful()
            return next
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    @Throws(JSONException::class)
    fun ownershipReady193(id: String, epoch: Long): JSONObject {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val frame = current(db)
            val marker = frame?.optJSONObject("ownershipImport193")
            if (frame == null ||
                !frame.optBoolean("active") ||
                !frame.optBoolean("workoutPaused") ||
                marker == null ||
                id != marker.optString("id") ||
                epoch != marker.optLong("epoch")
            ) {
                throw IllegalStateException("The returned workout changed")
            }
            marker.put("state", "installed")
            save(db, frame)
            db.execSQL("INSERT OR REPLACE INTO flags(name,value) VALUES('suspended',0)")
            db.setTransactionSuccessful()
            return frame
        } finally {
            db.endTransaction()
        }
    }

    /** Called with the UI's last reconciled execution revision, not a transport snapshot revision. */
    @Synchronized
    @Throws(JSONException::class)
    fun prepare(input: JSONObject?, expectedRevision: Long): JSONObject {
        if (input == null) throw IllegalArgumentException("missingFrame")
        val incoming = JSONObject(input.toString())
        PhoneWorkoutRecording.validateFrame(incoming)
        PhoneWorkoutPlans.validateConfig(incoming)
        if (PhoneWorkoutRecording.version(incoming, "executionRevision") != expectedRevision) {
            throw IllegalArgumentException("revisionConflict")
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (blocked(db)) throw IllegalStateException("restoreInProgress")
            val old = current(db)
            if (old == null) {
                if (expectedRevision != 0L) throw IllegalArgumentException("revisionConflict")
            } else if (old.getBoolean("active")) {
                if (old.getString("workoutId") != incoming.getString("workoutId") ||
                    old.getLong("generation") != incoming.getLong("generation")
                ) {
                    throw IllegalArgumentException("executionIdentityChanged")
                }
                if (old.getLong("executionRevision") != expectedRevision) {
                    throw IllegalArgumentException("revisionConflict")
                }
                if (pending(db, old.getString("workoutId"), old.getLong("generation"))) {
                    throw IllegalStateException("pendingRecordingEvents")
                }
                PhoneWorkoutRecording.retainCompleted(old, incoming)
                if (old.has("undo188")) incoming.put("undo188", old.get("undo188"))
                val last = old.optJSONObject("lastRecord188")
                if (last != null && WorkoutUndo188.same(last.optJSONArray("expected"), WorkoutUndo188.guard(incoming))) {
                    incoming.put("lastRecord188", last)
                } else {
                    incoming.remove("lastRecord188")
                }
                if (incoming.optLong("pauseRevision") <= old.optLong("pauseRevision")) {
                    for (key in arrayOf("pauseRevision", "workoutPaused", "activeIntervals")) {
                        if (old.has(key)) incoming.put(key, old.get(key))
                    }
                }
                if (old.optInt("nativeEditorProjection") == 1) NativeMeasuredTime.stopwatch(old, SystemClock.elapsedRealtime())
                if (old.optInt("nativeEditorProjection") == 1 && !old.has("timeLogs162")) incoming.remove("timeLogs162")
                for (key in arrayOf("timeLogs162", "latestTimer162", "retiredTimers188", "manualAnchor162", "resumeTimerOnContinue201")) {
                    if (old.has(key)) incoming.put(key, old.get(key))
                }
                if (incoming.optInt("nativeEditorProjection") == 1) NativeMeasuredTime.stopwatch(incoming, SystemClock.elapsedRealtime())
                if (old.has("transition")) {
                    val transition = JSONObject(old.getJSONObject("transition").toString())
                    if (incoming.optInt("nativeEditorProjection") == 1 && transition.optJSONObject("plan")?.optBoolean("restOnly") == true &&
                        incoming.optLong("selectionRevision") > old.optLong("selectionRevision")) {
                        transition.put("currentExercise", incoming.getInt("currentExercise"))
                    }
                    val plan = transition.optJSONObject("plan")
                    val automation = incoming.optJSONObject("automation")
                    if (old.optInt("nativeEditorProjection") == 1 && incoming.optInt("nativeEditorProjection") == 1) {
                        // Native editor changes cannot retire the controller's durable plan.
                        incoming.put("transition", transition)
                    } else if (plan != null && automation != null && plan.optString("key") == automation.optString("key")) {
                        incoming.put("transition", transition)
                    } else if ("paused" == transition.optString("state") &&
                        automation != null &&
                        automation.optBoolean("paused") &&
                        automation.optString("key").startsWith("undo188:")
                    ) {
                        incoming.put("transition", transition)
                    } else {
                        incoming.remove("transition")
                    }
                }
            } else if (expectedRevision != 0L ||
                incoming.getLong("generation") <= old.getLong("generation") ||
                incoming.getString("workoutId") == old.getString("workoutId")
            ) {
                throw IllegalArgumentException("closedOrStaleExecution")
            }
            incoming.put("executionRevision", PhoneWorkoutRecording.advance(expectedRevision))
            save(db, incoming)
            db.setTransactionSuccessful()
            return incoming
        } finally {
            db.endTransaction()
        }
    }

    /** Mutation and replay receipt commit together, before the Wear queue may be acknowledged. */
    @Synchronized
    @Throws(JSONException::class)
    fun record(command: JSONObject?): JSONObject {
        if (command == null) return rejected("invalidCommand")
        val id = try {
            PhoneWorkoutRecording.id(command, "commandId")
        } catch (_: Exception) {
            return rejected("missingCommandIdentity")
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (blocked(db)) return rejected("restoreInProgress")
            val fingerprint = PhoneWorkoutRecording.fingerprint(command)
            db.rawQuery("SELECT seq, fingerprint, event FROM recordings WHERE command_id=?", arrayOf(id)).use { c ->
                if (c.moveToFirst()) {
                    if (fingerprint != c.getString(1)) return rejected("commandIdentityCollision")
                    val result = JSONObject(c.getString(2))
                    result.put("sequence", c.getLong(0)).put("status", "duplicate")
                    db.setTransactionSuccessful()
                    return result
                }
            }
            val frame = current(db) ?: return rejected("noExecution")
            val next: JSONObject
            try {
                next = PhoneWorkoutRecording.record(frame, command)
                finishManual(next, command, SystemClock.elapsedRealtime())
                val transition = PhoneWorkoutPlans.afterRecord(next, next.getInt("currentExercise"))
                if (transition != null) next.put("transition", transition)
            } catch (invalid: Exception) {
                return rejected(if (invalid is JSONException) "invalidCommand" else invalid.message ?: "invalidCommand")
            }
            next.put("lastRecord188", WorkoutUndo188.capture(frame, next, command, id))
            val target = PhoneWorkoutRecording.target(frame, command)
            val values = PhoneWorkoutRecording.recordedValues(next, command)
            val result = JSONObject()
                .put("status", "applied")
                .put("commandId", id)
                .put("workoutId", next.getString("workoutId"))
                .put("generation", next.getLong("generation"))
                .put("executionRevision", next.getLong("executionRevision"))
                .put("exerciseId", target.exercise.getString("id"))
                .put("setId", target.set.getString("setId"))
                .put("recordedAt", System.currentTimeMillis())
                .put("values", values)
                .put("before", if (command.optString("action") in setOf("completeTimedSet", "completeGpsSet", "completeMeasuredSet")) recordedBase(target.set)
                    else JSONObject().put("reps", target.set.opt("reps")).put("weight", target.set.opt("weight")))
            if (next.has("transition")) result.put("transition", next.getJSONObject("transition"))
            next.put("watchSequence", max(next.optLong("watchSequence"), command.optLong("sequence")))
            val row = ContentValues().apply {
                put("command_id", id)
                put("fingerprint", fingerprint)
                put("workout_id", next.getString("workoutId"))
                put("generation", next.getLong("generation"))
                put("event", result.toString())
            }
            val sequence = db.insertOrThrow("recordings", null, row)
            save(db, next)
            result.put("sequence", sequence)
            db.setTransactionSuccessful()
            return result
        } finally {
            db.endTransaction()
        }
    }

    /** Append a guarded inverse beside the original event; never delete recordings or measured time. */
    @Synchronized
    @Throws(JSONException::class)
    fun undo(command: JSONObject): JSONObject {
        val id = PhoneWorkoutRecording.id(command, "commandId")
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (blocked(db)) return rejected("restoreInProgress")
            val replay = receipt(command)
            if (replay != null) return replay
            val old = current(db) ?: return rejected("noExecution")
            val next: JSONObject
            try {
                val accounted = JSONObject(old.toString())
                val anchor = accounted.optJSONObject("manualAnchor162")
                if (anchor != null) finishManual(accounted, anchor, SystemClock.elapsedRealtime())
                next = WorkoutUndo188.inverse(accounted, command)
                val timer = old.optJSONObject("latestTimer162")
                if (timer != null && timer.optString("key").isNotEmpty()) {
                    val retired = next.optJSONArray("retiredTimers188") ?: JSONArray()
                    retired.put(timer.getString("key"))
                    next.put("retiredTimers188", retired)
                }
            } catch (_: Exception) {
                return rejected("undoConflict")
            }
            val event = JSONObject()
                .put("kind", "undoSet")
                .put("status", "applied")
                .put("commandId", id)
                .put("recordId", command.getString("recordId"))
                .put("workoutId", next.getString("workoutId"))
                .put("generation", next.getLong("generation"))
                .put("executionRevision", next.getLong("executionRevision"))
                .put("exerciseId", command.getString("exerciseId"))
                .put("setId", command.getString("setId"))
                .put("before", WorkoutUndo188.values(WorkoutUndo188.find(old, command)))
                .put("values", WorkoutUndo188.values(WorkoutUndo188.find(next, command)))
                .put("transition", next.getJSONObject("transition"))
            next.put("watchSequence", max(next.optLong("watchSequence"), command.optLong("sequence")))
            val row = ContentValues().apply {
                put("command_id", id)
                put("workout_id", next.getString("workoutId"))
                put("generation", next.getLong("generation"))
                put("fingerprint", PhoneWorkoutRecording.fingerprint(command))
                put("event", event.toString())
            }
            val seq = db.insertOrThrow("recordings", null, row)
            save(db, next)
            event.put("sequence", seq)
            db.setTransactionSuccessful()
            return event
        } finally {
            db.endTransaction()
        }
    }

    /** Cumulative checkpoints stay in the snapshot; only completed sets create journal events. */
    @Synchronized
    @Throws(JSONException::class)
    fun checkpointTimer(timer: JSONObject, monotonic: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val frame = current(db)
            if (frame == null ||
                !frame.optBoolean("active") ||
                frame.optString("workoutId") != timer.optString("workoutId") ||
                frame.optLong("generation") != timer.optLong("generation")
            ) {
                return false
            }
            val retired = frame.optJSONArray("retiredTimers188")
            if (retired != null) {
                for (r in 0 until retired.length()) {
                    if (retired.getString(r) == timer.optString("key")) return false
                }
            }
            val ledger = timer.getJSONObject("ledger")
            val logs = frame.optJSONObject("timeLogs162") ?: JSONObject()
            val key = PhoneWorkoutRecording.id(timer, "key")
            val previous = logs.optJSONObject(key)
            val local = PhoneOwnershipResume193.localLog(timer, ledger)
            if (previous != null && (
                local.getLong("positionMs") < previous.getLong("positionMs") ||
                    local.getLong("workMs") < previous.getLong("workMs") ||
                    local.getLong("restMs") < previous.getLong("restMs") ||
                    local.getLong("pausedMs") < previous.getLong("pausedMs")
                )
            ) {
                return false
            }
            val log = JSONObject(local.toString())
                .put("key", key)
                .put("phase", timer.optString("phase"))
                .put("interrupted", timer.optBoolean("interrupted"))
                .put("stopped", timer.optBoolean("stopped"))
            logs.put(key, log)
            frame.put("timeLogs162", logs).put("latestTimer162", timer)
            val restPlan = frame.optJSONObject("transition")?.optJSONObject("plan")
            if (frame.optInt("nativeEditorProjection") == 1 && timer.optBoolean("restOnly") &&
                restPlan != null && restPlan.optString("key") == key && restPlan.optBoolean("restOnly")) {
                val rows = timer.getJSONArray("steps")
                val priorRows = restPlan.getJSONArray("steps")
                check(rows.length() == 1 && priorRows.length() == 1 && rows.getJSONObject(0).optString("exerciseId") == priorRows.getJSONObject(0).optString("exerciseId") &&
                    rows.getJSONObject(0).optString("setId") == priorRows.getJSONObject(0).optString("setId")) { "Rest target changed" }
                val rest = rows.getJSONObject(0).getInt("rest")
                check(rest in 0..3600)
                restPlan.put("steps", JSONArray(rows.toString())).put("rest", rest)
            }
            val beforeUndo188 = JSONObject(frame.toString())
            val changes = JSONArray()
            val rows = timer.getJSONArray("steps")
            val parts = ledger.getJSONArray("steps")
            if (timer.optBoolean("autoAdvance") && !timer.optBoolean("restOnly")) {
                for (i in 0 until timer.optInt("completed")) {
                    val row = rows.getJSONObject(i)
                    val part = parts.getJSONObject(i)
                    val set = findSet(frame, row) ?: throw IllegalArgumentException("timerTargetRemoved")
                    if (!set.getBoolean("done")) {
                        val before = JSONObject().put("reps", set.opt("reps"))
                        val reps = floor(
                            row.optDouble("baseSeconds", 0.0) +
                                (part.getLong("workMs") + if (frame.optInt("nativeEditorProjection") == 1) 0L else part.getLong("restMs")) / 1000.0
                        ).toLong().toString()
                        set.put("done", true).put("reps", reps)
                        changes.put(
                            JSONObject()
                                .put("exerciseId", row.getString("exerciseId"))
                                .put("setId", row.getString("setId"))
                                .put("values", JSONObject().put("done", true).put("reps", reps))
                                .put("before", before)
                        )
                    }
                }
            }
            if (changes.length() > 0) {
                frame.put("executionRevision", PhoneWorkoutRecording.advance(frame.getLong("executionRevision")))
                val event = JSONObject()
                    .put("kind", "timedSets")
                    .put("workoutId", frame.getString("workoutId"))
                    .put("generation", frame.getLong("generation"))
                    .put("executionRevision", frame.getLong("executionRevision"))
                    .put("key", key)
                    .put("changes", changes)
                val row = ContentValues().apply {
                    put("command_id", "timer:$key:${timer.getInt("completed")}")
                    put("workout_id", frame.getString("workoutId"))
                    put("generation", frame.getLong("generation"))
                    put("fingerprint", event.toString())
                    put("event", event.toString())
                }
                db.insertOrThrow("recordings", null, row)
                val lastChange = changes.getJSONObject(changes.length() - 1)
                frame.put(
                    "lastRecord188",
                    WorkoutUndo188.capture(beforeUndo188, frame, lastChange, "timer:$key:${timer.getInt("completed")}")
                )
            }
            if (frame.optInt("nativeEditorProjection") == 1 && !timer.optBoolean("restOnly") &&
                (timer.optBoolean("stopped") || timer.optBoolean("interrupted") ||
                    (!timer.optBoolean("autoAdvance") && timer.optString("phase") == "complete"))) {
                val from = if (timer.optBoolean("autoAdvance")) timer.optInt("completed") else 0
                for (i in from until rows.length()) {
                    val row = rows.getJSONObject(i); val part = parts.getJSONObject(i)
                    val set = findSet(frame, row) ?: throw IllegalArgumentException("timerTargetRemoved")
                    if (!set.optBoolean("done") && part.getLong("workMs") > 0L) {
                        set.put("reps", floor(row.optDouble("baseSeconds", 0.0) + part.getLong("workMs") / 1000.0).toLong().toString())
                    }
                }
            }
            // A finished single-set timer has no automatic completion event to reconcile.
            // Retire its plan so the user's manual OK/restart can use the existing command path.
            if (frame.optInt("nativeEditorProjection") == 1 && !timer.optBoolean("restOnly") &&
                !timer.optBoolean("autoAdvance") && timer.optString("phase") == "complete") {
                retireIntervalPlan(frame, key)
            }
            if (timer.optBoolean("autoAdvance")) {
                val step = rows.getJSONObject(timer.optInt("stepIndex"))
                val exercises = frame.getJSONArray("exercises")
                for (e in 0 until exercises.length()) {
                    if (findSet(JSONObject().put("exercises", JSONArray().put(exercises.getJSONObject(e))), step) != null) {
                        var selected = e
                        if ("complete" == timer.optString("phase") && e + 1 < exercises.length()) {
                            selected = e + 1
                        }
                        frame.put("currentExercise", selected)
                        break
                    }
                }
            }
            if ("complete" == timer.optString("phase") && !timer.optBoolean("stopped")) {
                anchorManual(frame, monotonic)
            }
            save(db, frame)
            db.setTransactionSuccessful()
            return changes.length() > 0
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    @Throws(JSONException::class)
    fun beginManualWork(workoutId: String, generation: Long, monotonic: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val frame = current(db)
            if (frame != null &&
                frame.optBoolean("active") &&
                workoutId == frame.optString("workoutId") &&
                generation == frame.optLong("generation")
            ) {
                if (frame.optJSONObject("transition") != null &&
                    "paused" == frame.getJSONObject("transition").optString("state")
                ) {
                    return
                }
                val timer = frame.optJSONObject("latestTimer162")
                if (timer != null &&
                    !timer.optBoolean("stopped") &&
                    !timer.optBoolean("interrupted") &&
                    "complete" != timer.optString("phase")
                ) {
                    return
                }
                anchorManual(frame, monotonic)
                save(db, frame)
                db.setTransactionSuccessful()
            }
        } finally {
            db.endTransaction()
        }
    }

    /** Native GPS summaries are recoverable metadata; only an explicit command completes a set. */
    @Synchronized
    fun checkpointGps(status: JSONObject): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val frame = current(db) ?: return false
            if (blocked(db) || commandsSuspended() || !frame.optBoolean("active") || frame.optString("timerOwner") != "phone") return false
            if (!NativeGpsMeasurement.capture(frame, status)) return false
            save(db, frame)
            db.setTransactionSuccessful()
            return true
        } finally { db.endTransaction() }
    }

    /** Controller checkpoints never complete a measured set or advance its command revision. */
    @Synchronized
    fun checkpointStopwatch(workoutId: String, generation: Long, monotonic: Long, pause: Boolean = false): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val frame = current(db) ?: return false
            if (blocked(db) || !frame.optBoolean("active") || frame.optString("timerOwner") != "phone" ||
                workoutId != frame.optString("workoutId") || generation != frame.optLong("generation") ||
                frame.optJSONObject(NativeSetStopwatch.FIELD)?.optBoolean("running") != true) return false
            if (pause || frame.optBoolean("workoutPaused")) NativeSetStopwatch.pause(frame, monotonic)
            else NativeSetStopwatch.checkpoint(frame, monotonic)
            save(db, frame)
            db.setTransactionSuccessful()
            return NativeSetStopwatch.running(frame.getJSONObject(NativeSetStopwatch.FIELD))
        } finally { db.endTransaction() }
    }

    /** Explicit phone edits use a CAS and a matching base; ordinary snapshots still cannot undo recordings. */
    @Synchronized
    @Throws(JSONException::class)
    fun amend(input: JSONObject, expectedRevision: Long, bases: JSONObject): JSONObject {
        val incoming = JSONObject(input.toString())
        PhoneWorkoutRecording.validateFrame(incoming)
        PhoneWorkoutPlans.validateConfig(incoming)
        val db = writableDatabase
        db.beginTransaction()
        try {
            val old = current(db)
            if (blocked(db) ||
                old == null ||
                !old.optBoolean("active") ||
                old.getString("workoutId") != incoming.getString("workoutId") ||
                old.getLong("generation") != incoming.getLong("generation") ||
                old.getLong("executionRevision") != expectedRevision ||
                incoming.getLong("executionRevision") != expectedRevision ||
                pending(db, old.getString("workoutId"), old.getLong("generation"))
            ) {
                throw IllegalStateException("amendmentConflict")
            }
            val exercises = old.getJSONArray("exercises")
            for (e in 0 until exercises.length()) {
                val exercise = exercises.getJSONObject(e)
                val sets = exercise.getJSONArray("sets")
                for (s in 0 until sets.length()) {
                    val set = sets.getJSONObject(s)
                    if (!set.getBoolean("done")) continue
                    val target = JSONObject()
                        .put("exerciseId", exercise.getString("id"))
                        .put("setId", set.getString("setId"))
                    findSet(incoming, target) ?: throw IllegalStateException("amendmentTargetRemoved")
                    val base = bases.optJSONObject(set.getString("setId"))
                    if (base == null || base.toString() != recordedBase(set).toString()) {
                        throw IllegalStateException("amendmentConflict")
                    }
                }
            }
            incoming.remove("lastRecord188")
            if (old.has("undo188")) incoming.put("undo188", old.get("undo188"))
            for (key in arrayOf("timeLogs162", "latestTimer162", "retiredTimers188")) {
                if (old.has(key)) incoming.put(key, old.get(key))
            }
            incoming.remove("manualAnchor162")
            val anchor = old.optJSONObject("manualAnchor162")
            if (anchor != null && old.optInt("currentExercise") == incoming.optInt("currentExercise")) {
                val target = findSet(incoming, anchor)
                if (target != null && !target.getBoolean("done")) {
                    incoming.put("manualAnchor162", anchor)
                }
            }
            val previousTransition = old.optJSONObject("transition")
            val automation = incoming.optJSONObject("automation")
            if (previousTransition != null &&
                previousTransition.optJSONObject("plan") != null &&
                automation != null &&
                previousTransition.getJSONObject("plan").optString("key") == automation.optString("key")
            ) {
                incoming.put("transition", previousTransition)
            } else {
                incoming.remove("transition")
            }
            incoming.put("executionRevision", PhoneWorkoutRecording.advance(expectedRevision))
            save(db, incoming)
            db.setTransactionSuccessful()
            return incoming
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun recorded188(workoutId: String, generation: Long, recordId: String): Boolean {
        readableDatabase.rawQuery(
            "SELECT seq FROM recordings WHERE command_id=? AND workout_id=? AND generation=?",
            arrayOf(recordId, workoutId, generation.toString())
        ).use { c ->
            return c.moveToFirst()
        }
    }

    @Synchronized
    @Throws(JSONException::class)
    fun receipt(command: JSONObject): JSONObject? {
        val id = PhoneWorkoutRecording.id(command, "commandId")
        readableDatabase.rawQuery(
            "SELECT seq, fingerprint, event FROM recordings WHERE command_id=?",
            arrayOf(id)
        ).use { c ->
            if (!c.moveToFirst()) return null
            if (PhoneWorkoutRecording.fingerprint(command) != c.getString(1)) {
                return rejected("commandIdentityCollision")
            }
            return JSONObject(c.getString(2)).put("sequence", c.getLong(0)).put("status", "duplicate")
        }
    }

    /** Explicit watch start uses the same durable outbox as a recorded repetition set. */
    @Synchronized
    @Throws(JSONException::class)
    fun startTimeline(command: JSONObject): JSONObject {
        val id = PhoneWorkoutRecording.id(command, "commandId")
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (blocked(db)) return rejected("restoreInProgress")
            val fingerprint = PhoneWorkoutRecording.fingerprint(command)
            db.rawQuery("SELECT seq, fingerprint, event FROM recordings WHERE command_id=?", arrayOf(id)).use { c ->
                if (c.moveToFirst()) {
                    if (fingerprint != c.getString(1)) return rejected("commandIdentityCollision")
                    return JSONObject(c.getString(2)).put("sequence", c.getLong(0)).put("status", "duplicate")
                }
            }
            val old = current(db) ?: return rejected("noExecution")
            val next = JSONObject(old.toString())
            try {
                PhoneWorkoutRecording.validateFrame(next)
                if ("automaticControl" != command.optString("action") ||
                    !("start" == command.optString("value") || "resumeUndo" == command.optString("value"))
                ) {
                    throw IllegalArgumentException("unsupportedAction")
                }
                if (next.getString("workoutId") != PhoneWorkoutRecording.id(command, "workoutId") ||
                    next.getLong("generation") != PhoneWorkoutRecording.version(command, "generation") ||
                    next.getLong("executionRevision") != PhoneWorkoutRecording.version(command, "executionRevision")
                ) {
                    throw IllegalArgumentException("revisionConflict")
                }
                next.put("executionRevision", PhoneWorkoutRecording.advance(next.getLong("executionRevision")))
                if ("resumeUndo" == command.optString("value")) {
                    if ("paused" != (next.optJSONObject("transition")?.optString("state") ?: "")) {
                        throw IllegalArgumentException("notUndoPaused")
                    }
                    next.put(
                        "transition",
                        JSONObject()
                            .put("state", "ready")
                            .put("phase", "ready")
                            .put("currentExercise", next.optInt("currentExercise"))
                    )
                    val a = next.optJSONObject("automation")
                    a?.put("paused", false)
                    anchorManual(next, SystemClock.elapsedRealtime())
                } else {
                    val transition = PhoneWorkoutPlans.startTimed(next)
                    val first = transition.getJSONObject("plan").getJSONArray("steps").getJSONObject(0)
                    if (first.getString("exerciseId") != PhoneWorkoutRecording.id(command, "exerciseId") ||
                        first.getString("setId") != PhoneWorkoutRecording.id(command, "setId")
                    ) {
                        throw IllegalArgumentException("targetChanged")
                    }
                    next.put("transition", transition)
                }
            } catch (_: Exception) {
                return rejected("invalidTimelineStart")
            }
            next.put("watchSequence", max(next.optLong("watchSequence"), command.optLong("sequence")))
            val event = JSONObject()
                .put("status", "applied")
                .put("kind", "startTimeline")
                .put("commandId", id)
                .put("workoutId", next.getString("workoutId"))
                .put("generation", next.getLong("generation"))
                .put("executionRevision", next.getLong("executionRevision"))
                .put("transition", next.getJSONObject("transition"))
            val row = ContentValues().apply {
                put("command_id", id)
                put("fingerprint", fingerprint)
                put("workout_id", next.getString("workoutId"))
                put("generation", next.getLong("generation"))
                put("event", event.toString())
            }
            val sequence = db.insertOrThrow("recordings", null, row)
            save(db, next)
            event.put("sequence", sequence)
            db.setTransactionSuccessful()
            return event
        } finally {
            db.endTransaction()
        }
    }

    /** Delivery progress changes no user data and cannot consume a newer timeline request. */
    @Synchronized
    @Throws(JSONException::class)
    fun markTransition(key: String, state: String): Boolean {
        if (state !in listOf("started", "interrupted")) {
            throw IllegalArgumentException("invalidTransitionState")
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val frame = current(db)
            val t = frame?.optJSONObject("transition")
            val plan = t?.optJSONObject("plan")
            if (frame == null || !frame.optBoolean("active") || plan == null || key != plan.optString("key")) {
                return false
            }
            t.put("state", state)
            save(db, frame)
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    /** A separate revision fences stale transport snapshots without invalidating pending set receipts. */
    @Synchronized
    @Throws(JSONException::class)
    fun setWorkoutPause(input: JSONObject, monotonic: Long, running: Boolean): JSONObject? {
        val id = input.optString("workoutId")
        val revision = input.optLong("pauseRevision")
        if (id.isEmpty() || revision < 1L || revision > 9_007_199_254_740_991L) {
            throw IllegalArgumentException("invalidWorkoutPause")
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val frame = current(db)
            if (frame == null || !frame.optBoolean("active") || id != frame.optString("workoutId")) {
                return null
            }
            if (revision < frame.optLong("pauseRevision")) throw IllegalStateException("staleWorkoutPause")
            val paused = input.optBoolean("workoutPaused")
            if (revision == frame.optLong("pauseRevision") && paused != frame.optBoolean("workoutPaused")) {
                throw IllegalStateException("staleWorkoutPause")
            }
            if (revision > frame.optLong("pauseRevision")) {
                if (paused && !frame.optBoolean("workoutPaused")) {
                    frame.put("resumeTimerOnContinue201", running)
                }
                val anchor = frame.optJSONObject("manualAnchor162")
                if (anchor != null && PROCESS == anchor.optString("process")) {
                    if (paused && !anchor.optBoolean("paused201")) {
                        anchor.put("priorWorkMs201", anchor.optLong("priorWorkMs201") + max(0L, monotonic - anchor.getLong("began")))
                    }
                    anchor.put("paused201", paused).put("began", monotonic)
                }
                if (paused) NativeSetStopwatch.pause(frame, monotonic)
                frame.put("workoutPaused", paused).put("pauseRevision", revision)
                if (input.optJSONArray("activeIntervals") != null) {
                    frame.put("activeIntervals", JSONArray(input.getJSONArray("activeIntervals").toString()))
                }
                save(db, frame)
            }
            db.setTransactionSuccessful()
            return frame
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun suspendCommands(value: Boolean) {
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO flags(name,value) VALUES('suspended',?)",
            arrayOf<Any>(if (value) 1 else 0)
        )
    }

    private fun retireIntervalPlan(frame: JSONObject, key: String) {
        val retired = frame.optJSONArray("retiredTimers188") ?: JSONArray()
        if ((0 until retired.length()).none { retired.optString(it) == key }) retired.put(key)
        frame.put("retiredTimers188", retired).put("transition", JSONObject().put("state", "ready").put("phase", "ready")
            .put("currentExercise", frame.optInt("currentExercise")))
    }

    /** Explicitly abandon a stopped native interval; retain measurements and reject late ticks. */
    @Synchronized
    fun retireNativeInterval(target: NativeTimerTarget) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            check(!blocked(db) && !commandsSuspended()) { "A vezérlés foglalt." }
            val frame = current(db) ?: error("Nincs aktív edzés.")
            val timer = frame.getJSONObject("latestTimer162")
            check(frame.optInt("nativeEditorProjection") == 1 && frame.optBoolean("active") && frame.optString("timerOwner") == "phone" &&
                frame.optString("workoutId") == target.workoutId && frame.optLong("generation") == target.generation &&
                timer.optString("key") == target.key && !timer.optBoolean("restOnly") &&
                (timer.optBoolean("stopped") || timer.optBoolean("interrupted") || timer.optString("phase") == "complete")) {
                "Az intervallum még aktív vagy megváltozott."
            }
            retireIntervalPlan(frame, target.key)
            save(db, frame); db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    @Synchronized
    fun commandsSuspended(): Boolean {
        readableDatabase.rawQuery("SELECT value FROM flags WHERE name='suspended'", null).use { c ->
            return c.moveToFirst() && c.getInt(0) != 0
        }
    }

    @Synchronized
    @Throws(JSONException::class)
    fun pendingEvents(workoutId: String, generation: Long, afterSequence: Long): JSONArray {
        if (afterSequence < 0L) throw IllegalArgumentException("invalidEventCursor")
        val events = JSONArray()
        readableDatabase.rawQuery(
            "SELECT seq, event FROM recordings WHERE workout_id=? AND generation=? AND imported=0 AND seq>? ORDER BY seq LIMIT 128",
            arrayOf(workoutId, generation.toString(), afterSequence.toString())
        ).use { c ->
            while (c.moveToNext()) {
                events.put(JSONObject(c.getString(1)).put("sequence", c.getLong(0)))
            }
        }
        return events
    }

    /** Called only after the UI has durably saved its draft and last-applied sequence together. */
    @Synchronized
    fun acknowledgeEvents(workoutId: String, generation: Long, throughSequence: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.rawQuery(
                "SELECT seq FROM recordings WHERE seq=? AND workout_id=? AND generation=?",
                arrayOf(throughSequence.toString(), workoutId, generation.toString())
            ).use { c ->
                if (!c.moveToFirst()) return false
            }
            val values = ContentValues().apply { put("imported", 1) }
            db.update(
                "recordings",
                values,
                "workout_id=? AND generation=? AND seq<=?",
                arrayOf(workoutId, generation.toString(), throughSequence.toString())
            )
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun restoreBlocked(): Boolean = blocked(readableDatabase)

    @Synchronized
    fun setRestoreBlocked(value: Boolean) {
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO flags(name,value) VALUES('restore',?)",
            arrayOf<Any>(if (value) 1 else 0)
        )
    }

    @Synchronized
    @Throws(JSONException::class)
    fun closeExecution(workoutId: String, generation: Long, expectedRevision: Long): Boolean {
        return endExecution(workoutId, generation, expectedRevision, false)
    }

    /** Explicit confirmed discard may abandon pending events; ordinary close still requires reconciliation. */
    @Synchronized
    @Throws(JSONException::class)
    fun discardExecution(workoutId: String, generation: Long, expectedRevision: Long): Boolean {
        return endExecution(workoutId, generation, expectedRevision, true)
    }

    @Throws(JSONException::class)
    private fun endExecution(
        workoutId: String,
        generation: Long,
        expectedRevision: Long,
        discard: Boolean
    ): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val frame = current(db)
            if (blocked(db) ||
                frame == null ||
                workoutId != frame.getString("workoutId") ||
                generation != frame.getLong("generation") ||
                expectedRevision != frame.getLong("executionRevision") ||
                (!discard && pending(db, workoutId, generation))
            ) {
                return false
            }
            if (frame.getBoolean("active")) {
                frame.put("active", false).put("executionRevision", PhoneWorkoutRecording.advance(expectedRevision))
                save(db, frame)
            }
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }
}
