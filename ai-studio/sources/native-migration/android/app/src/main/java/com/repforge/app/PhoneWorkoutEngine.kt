package com.repforge.app
import com.repforge.shared.IntervalTimeline193

import android.content.Context
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.repforge.shared.WearDataLayerPaths
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Validates prepared execution frames; does not touch any WebView or start timers. */
internal object PhoneWorkoutRecording {
    private const val MAX_REVISION: Long = 9_007_199_254_740_991L
    private val DIGITS_REGEX = Regex("[0-9]+")

    @JvmStatic
    @Throws(JSONException::class)
    fun version(data: JSONObject, field: String): Long {
        val value = data.get(field)
        if (value !is Int && value !is Long) {
            throw IllegalArgumentException("invalidVersion")
        }
        val result = (value as Number).toLong()
        if (result < 0L || result > MAX_REVISION) {
            throw IllegalArgumentException("invalidVersion")
        }
        return result
    }

    @JvmStatic
    fun advance(revision: Long): Long {
        if (revision < 0L || revision >= MAX_REVISION) {
            throw IllegalArgumentException("revisionExhausted")
        }
        return revision + 1L
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun id(data: JSONObject, field: String): String {
        val value = data.get(field)
        if (value !is String || value.isEmpty()) {
            throw IllegalArgumentException("missingIdentity")
        }
        return value
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun validateFrame(frame: JSONObject) {
        id(frame, "workoutId")
        if (version(frame, "generation") == 0L) throw IllegalArgumentException("invalidGeneration")
        version(frame, "executionRevision")
        if (frame.opt("active") != true || "phone" != frame.optString("timerOwner")) {
            throw IllegalArgumentException("inactiveOrOtherOwner")
        }
        val exercises = frame.getJSONArray("exercises")
        val ids = HashSet<String>()
        for (i in 0 until exercises.length()) {
            val exercise = exercises.getJSONObject(i)
            id(exercise, "id")
            val sets = exercise.getJSONArray("sets")
            for (j in 0 until sets.length()) {
                val set = sets.getJSONObject(j)
                if (!ids.add(id(set, "setId"))) throw IllegalArgumentException("duplicateSetIdentity")
                if (set.opt("done") !is Boolean) throw IllegalArgumentException("invalidSetState")
                val rir = set.opt("actualRir")
                if (rir != null && rir !== JSONObject.NULL) {
                    if (rir !is Number ||
                        !rir.toDouble().isFinite() ||
                        rir.toDouble() < 0.0 ||
                        rir.toDouble() > 10.0 ||
                        rir.toDouble() != rir.toInt().toDouble()
                    ) {
                        throw IllegalArgumentException("invalidActualRir")
                    }
                }
            }
        }
    }

    class Target(
        @JvmField val exercise: JSONObject,
        @JvmField val set: JSONObject,
        @JvmField val exerciseIndex: Int
    )

    @JvmStatic
    @Throws(JSONException::class)
    fun target(frame: JSONObject, command: JSONObject): Target {
        validateFrame(frame)
        if (command.optString("action") !in setOf("completeSet", "completeTimedSet", "completeGpsSet", "completeMeasuredSet")) {
            throw IllegalArgumentException("unsupportedAction")
        }
        if (id(frame, "workoutId") != id(command, "workoutId") ||
            version(frame, "generation") != version(command, "generation")
        ) {
            throw IllegalArgumentException("executionIdentityChanged")
        }
        if (version(frame, "executionRevision") != version(command, "executionRevision")) {
            throw IllegalArgumentException("revisionConflict")
        }
        val exerciseId = id(command, "exerciseId")
        val setId = id(command, "setId")
        val exercises = frame.getJSONArray("exercises")
        for (i in 0 until exercises.length()) {
            val exercise = exercises.getJSONObject(i)
            val sets = exercise.getJSONArray("sets")
            for (j in 0 until sets.length()) {
                val set = sets.getJSONObject(j)
                if (setId != id(set, "setId")) continue
                if (exerciseId != id(exercise, "id")) throw IllegalArgumentException("targetChanged")
                if (set.getBoolean("done")) throw IllegalArgumentException("alreadyRecorded")
                if (command.optString("action") == "completeMeasuredSet") {
                    if (frame.optInt("nativeEditorProjection") != 1 || !NativeMeasuredSet.supported(exercise)) {
                        throw IllegalArgumentException("unsupportedMeasurement")
                    }
                } else if (command.optString("action") == "completeGpsSet") {
                    if (frame.optInt("nativeEditorProjection") != 1 || !NativeGpsMeasurement.distance(exercise)) {
                        throw IllegalArgumentException("unsupportedMeasurement")
                    }
                } else if (command.optString("action") == "completeTimedSet") {
                    if (frame.optInt("nativeEditorProjection") != 1 || !NativeSetStopwatch.timed(exercise)) {
                        throw IllegalArgumentException("unsupportedMeasurement")
                    }
                } else {
                    val unit = exercise.optString("repUnit")
                    if (unit !in listOf("", "ism.", "reps", "/kar", "/láb") ||
                        "distance" == exercise.optString("measurementType")) {
                        throw IllegalArgumentException("unsupportedMeasurement")
                    }
                }
                return Target(exercise, set, i)
            }
        }
        throw IllegalArgumentException("unknownTarget")
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun record(frame: JSONObject, command: JSONObject): JSONObject {
        if (command.optString("action") == "completeMeasuredSet") {
            target(frame, command)
            return NativeMeasuredSet.record(frame, command)
        }
        if (command.optString("action") == "completeGpsSet") {
            target(frame, command)
            return NativeGpsMeasurement.record(frame, command)
        }
        if (command.optString("action") == "completeTimedSet") {
            target(frame, command) // Same identity, revision, owner, and unfinished-target fences as strength.
            val measured = NativeSetStopwatch.stopped(frame, command)
            val target = NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"), command.getString("exerciseId"), command.getString("setId")).resolve(measured)
            require(target.set.optBoolean("done")) { "incompleteMeasurement" }
            measured.put("currentExercise", target.exerciseIndex)
                .put("executionRevision", advance(version(frame, "executionRevision")))
            return measured
        }
        val next = JSONObject(frame.toString())
        val target = target(next, command)
        val reps = command.get("reps")
        val text = reps.toString()
        val repetitions = try {
            if (DIGITS_REGEX.matches(text)) text.toLong() else 0L
        } catch (_: NumberFormatException) {
            throw IllegalArgumentException("invalidReps")
        }
        if (repetitions < 1L || repetitions > Int.MAX_VALUE.toLong()) {
            throw IllegalArgumentException("invalidReps")
        }
        target.set.put("reps", repetitions.toString())
        if (command.has("weight")) {
            val weight = try {
                command.get("weight").toString().replace(',', '.').toDouble()
            } catch (_: NumberFormatException) {
                throw IllegalArgumentException("invalidWeight")
            }
            if (!weight.isFinite() || weight < 0.0) throw IllegalArgumentException("invalidWeight")
            target.set.put("weight", weight)
        }
        target.set.put("done", true)
        if (next.optInt("nativeEditorProjection") == 1) NativeSetStopwatch.sealPause(next)
        next.put("currentExercise", target.exerciseIndex)
        next.put("executionRevision", advance(version(frame, "executionRevision")))
        return next
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun recordedValues(frame: JSONObject, command: JSONObject): JSONObject {
        val exercises = frame.getJSONArray("exercises")
        for (i in 0 until exercises.length()) {
            val exercise = exercises.getJSONObject(i)
            if (id(exercise, "id") != id(command, "exerciseId")) continue
            val sets = exercise.getJSONArray("sets")
            for (j in 0 until sets.length()) {
                val set = sets.getJSONObject(j)
                if (id(set, "setId") != id(command, "setId")) continue
                val values = JSONObject().put("done", true).put("reps", set.getString("reps"))
                if (command.has("weight")) values.put("weight", set.get("weight"))
                if (command.optString("action") in setOf("completeTimedSet", "completeMeasuredSet")) {
                    for (field in listOf("leftSeconds", "rightSeconds")) if (set.has(field)) values.put(field, set.get(field))
                }
                if (command.optString("action") in setOf("completeGpsSet", "completeMeasuredSet")) {
                    for (field in listOf("distanceMeters", "distanceSource", "gpsRouteKey163")) if (set.has(field)) values.put(field, set.get(field))
                }
                return values
            }
        }
        throw IllegalArgumentException("unknownTarget")
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun fingerprint(command: JSONObject): String {
        val values = JSONArray()
        for (key in arrayOf(
            "action", "workoutId", "generation", "executionRevision",
            "exerciseId", "setId", "reps", "weight", "value", "recordId", "expected", "undoDependency188"
        )) {
            values.put(JSONArray().put(command.has(key)).put(command.opt(key)))
        }
        // Existing durable receipts keep their exact original fingerprint format.
        if (command.optString("action") == "completeTimedSet") values.put(JSONArray().put(command.has("clockId")).put(command.opt("clockId")))
        if (command.optString("action") == "completeGpsSet") values.put(JSONArray().put(command.has("measurementId")).put(command.opt("measurementId")))
        if (command.optString("action") == "completeMeasuredSet") values.put(JSONArray().put(command.has("measuredValues")).put(command.opt("measuredValues")))
        return values.toString()
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun retainCompleted(oldFrame: JSONObject, incoming: JSONObject) {
        val oldExercises = oldFrame.getJSONArray("exercises")
        val exercises = incoming.getJSONArray("exercises")
        for (i in 0 until oldExercises.length()) {
            val oldExercise = oldExercises.getJSONObject(i)
            val oldSets = oldExercise.getJSONArray("sets")
            for (j in 0 until oldSets.length()) {
                val oldSet = oldSets.getJSONObject(j)
                if (!oldSet.getBoolean("done")) continue
                var retained = false
                for (e in 0 until exercises.length()) {
                    val exercise = exercises.getJSONObject(e)
                    if (id(exercise, "id") != id(oldExercise, "id")) continue
                    val sets = exercise.getJSONArray("sets")
                    for (s in 0 until sets.length()) {
                        val set = sets.getJSONObject(s)
                        if (id(set, "setId") == id(oldSet, "setId") && set.getBoolean("done")) {
                            if (set.optString("reps") != oldSet.optString("reps")) {
                                throw IllegalArgumentException("recordedValuesChanged")
                            }
                            for (field in arrayOf("weight", "leftSeconds", "rightSeconds", "distanceMeters")) {
                                if (!sameNumber(set.opt(field), oldSet.opt(field))) {
                                    throw IllegalArgumentException("recordedValuesChanged")
                                }
                            }
                            retained = true
                        }
                    }
                }
                if (!retained) throw IllegalArgumentException("completedSetRemoved")
            }
        }
    }

    private fun sameNumber(left: Any?, right: Any?): Boolean {
        if (left == null || left === JSONObject.NULL) return right == null || right === JSONObject.NULL
        if (right == null || right === JSONObject.NULL) return false
        return try {
            val a = left.toString().replace(',', '.').toDouble()
            val b = right.toString().replace(',', '.').toDouble()
            a.isFinite() && b.isFinite() && a == b
        } catch (_: NumberFormatException) {
            left == right
        }
    }
}

/** Plans use the foreground-prepared snapshot, never guessed repetitions. */
internal object PhoneWorkoutPlans {
    @JvmStatic
    @Throws(JSONException::class)
    fun validateConfig(frame: JSONObject) {
        if (!frame.optBoolean("automaticEnabled")) return
        val c = frame.getJSONObject("timerConfig")
        NativeIntervalPreset.normalized(c)
        integer(c, "work", 1, 3600)
        integer(c, "rest", 0, 3600)
        integer(c, "rounds", 1, 100)
        integer(frame, "restSeconds", 0, 3600)
    }

    @Throws(JSONException::class)
    private fun integer(obj: JSONObject, field: String, minVal: Int, maxVal: Int): Int {
        val value = PhoneWorkoutRecording.version(obj, field)
        if (value < minVal || value > maxVal) throw IllegalArgumentException("invalidTimerConfig")
        return value.toInt()
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun afterRecord(frame: JSONObject, from: Int): JSONObject? {
        if (!frame.optBoolean("automaticEnabled")) return null
        val exercise = frame.getJSONArray("exercises").getJSONObject(from)
        val rest = if (exercise.has("restSeconds162")) {
            integer(exercise, "restSeconds162", 0, 3600)
        } else {
            frame.getInt("restSeconds")
        }
        return next(frame, from, rest, false, manualMeasured = frame.optInt("nativeEditorProjection") == 1)
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun startTimed(frame: JSONObject): JSONObject {
        if (!frame.optBoolean("automaticEnabled")) throw IllegalArgumentException("automaticDisabled")
        return next(frame, frame.getInt("currentExercise"), 0, true)
    }

    @Throws(JSONException::class)
    private fun next(frame: JSONObject, from: Int, rest: Int, timedOnly: Boolean, manualMeasured: Boolean = false): JSONObject {
        validateConfig(frame)
        val exercises = frame.getJSONArray("exercises")
        var index = from
        var target: JSONObject? = null
        var exercise: JSONObject? = null
        while (index < exercises.length()) {
            exercise = exercises.getJSONObject(index)
            val sets = exercise.getJSONArray("sets")
            for (j in 0 until sets.length()) {
                if (!sets.getJSONObject(j).getBoolean("done")) {
                    target = sets.getJSONObject(j)
                    break
                }
            }
            if (target != null) break
            index++
        }
        val transition = JSONObject()
            .put("state", "pending")
            .put("currentExercise", if (index < exercises.length()) index else from)
        if (target == null) {
            if (timedOnly) throw IllegalArgumentException("noTimedTarget")
            return transition.put("phase", "complete")
        }
        val nonNullExercise = exercise!!
        fun timedExercise(ex: JSONObject) = if (frame.optInt("nativeEditorProjection") == 1)
            NativeSetStopwatch.timed(ex) && !NativeSetStopwatch.bilateral(ex) else "mp" == ex.optString("repUnit")
        val timed = !manualMeasured && timedExercise(nonNullExercise)
        if (timedOnly && !timed) throw IllegalArgumentException("noTimedTarget")
        if (!manualMeasured && "distance" == nonNullExercise.optString("measurementType")) {
            throw IllegalArgumentException("backgroundGpsNotPrepared")
        }
        val config = frame.getJSONObject("timerConfig")
        val advance = timed && config.optBoolean("autoAdvance", true)
        val rows = JSONArray()
        val steps = ArrayList<IntervalTimeline193.Step>()
        val key = "native|${frame.getLong("generation")}|${frame.getLong("executionRevision")}"
        if (timed) {
            timedRows@ for (i in index until exercises.length()) {
                val e = exercises.getJSONObject(i)
                if (!timedExercise(e)) break
                if ("distance" == e.optString("measurementType")) {
                    throw IllegalArgumentException("backgroundGpsNotPrepared")
                }
                val sets = e.getJSONArray("sets")
                for (j in 0 until sets.length()) {
                    val set = sets.getJSONObject(j)
                    if (set.getBoolean("done")) continue
                    val rowRest = config.getInt("rest")
                    val row = row(e, set, config.getInt("work"), rowRest, config.getInt("rounds"))
                    val base = if (set.optString("reps").isEmpty()) 0.0 else set.getString("reps").toDouble()
                    if (!base.isFinite() || base < 0.0) throw IllegalArgumentException("invalidTimedBaseline")
                    rows.put(row.put("baseSeconds", base))
                    steps.add(step(row))
                    if (!advance) break@timedRows
                }
            }
        } else {
            val row = row(nonNullExercise, target, 1, rest, 1)
            rows.put(row)
            steps.add(step(row))
        }
        IntervalTimeline(steps, advance, if (timed) rest else 0, !timed)
        val plan = JSONObject()
            .put("key", key)
            .put("workoutId", frame.getString("workoutId"))
            .put("started", frame.optString("started"))
            .put("generation", frame.getLong("generation"))
            .put("native", true)
            .put("steps", rows)
            .put("autoAdvance", advance)
            .put("restOnly", !timed)
            .put("initialRest", if (timed) rest else 0)
            .put("elapsedMs", 0)
            .put("running", false)
            .put("stopped", false)
            .put("sound", config.optBoolean("sound", true))
            .put("vibration", config.optBoolean("vibration", true))
            .put("language", frame.optString("language", "en"))
            .put(
                "target",
                JSONObject()
                    .put("exerciseId", nonNullExercise.getString("id"))
                    .put("setId", target.getString("setId"))
            )
            .put("work", config.getInt("work"))
            .put("rest", config.getInt("rest"))
            .put("rounds", config.getInt("rounds"))
        return transition
            .put("phase", if (rest > 0) "rest" else if (timed) "work" else "ready")
            .put("plan", plan)
    }

    @Throws(JSONException::class)
    private fun row(e: JSONObject, s: JSONObject, work: Int, rest: Int, rounds: Int): JSONObject {
        return JSONObject()
            .put("exerciseId", e.getString("id"))
            .put("setId", s.getString("setId"))
            .put("name", e.optString("name", e.getString("id")))
            .put("work", work)
            .put("rest", rest)
            .put("rounds", rounds)
            .put("gpsKey", "")
    }

    @Throws(JSONException::class)
    private fun step(row: JSONObject): IntervalTimeline193.Step {
        return IntervalTimeline193.Step(
            row.getString("exerciseId"),
            row.getString("setId"),
            row.getString("name"),
            row.getInt("work"),
            row.getInt("rest"),
            row.getInt("rounds"),
            ""
        )
    }
}

/** All callers serialize on the main looper: SQL receipt, timeline delivery, then Wear acknowledgement. */
internal object PhoneWorkoutExecution {
    // Kept as @JvmStatic private var store so PhoneWorkoutBridgeTest reflection works!
    @JvmStatic
    private var store: PhoneWorkoutExecutionStore? = null

    @JvmStatic
    fun store(context: Context): PhoneWorkoutExecutionStore {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalStateException("Execution requires main looper")
        }
        if (store == null) store = PhoneWorkoutExecutionStore(context)
        return store!!
    }

    @JvmStatic
    fun compatible(command: JSONObject): Boolean {
        return command.optInt("nativeExecutionProtocol") == 2 &&
            command.optLong("generation") > 0L &&
            (
                "completeSet" == command.optString("action") ||
                    "completeTimedSet" == command.optString("action") ||
                    "completeGpsSet" == command.optString("action") ||
                    "completeMeasuredSet" == command.optString("action") ||
                    "undoSet" == command.optString("action") ||
                    ("automaticControl" == command.optString("action") &&
                        ("start" == command.optString("value") || "resumeUndo" == command.optString("value")))
                )
    }

    @JvmStatic
    @Throws(Exception::class)
    fun execute(context: Context, command: JSONObject): JSONObject {
        if (!PhoneOwnership193.commandAllowed(context, command)) {
            return JSONObject().put("status", "rejected").put("reason", "ownershipChanged")
        }
        if (!compatible(command)) return JSONObject().put("status", "legacy")
        val db = store(context)
        val frame = db.snapshot()
        val receipt = db.receipt(command)
        if (receipt != null) {
            val requested = receipt.optJSONObject("transition")
            val current = frame?.optJSONObject("transition")
            if (frame != null &&
                frame.optBoolean("active") &&
                !db.restoreBlocked() &&
                !db.commandsSuspended() &&
                !frame.optBoolean("workoutPaused") &&
                "duplicate" == receipt.optString("status") &&
                requested != null &&
                requested.optJSONObject("plan") != null &&
                current != null &&
                current.optJSONObject("plan") != null &&
                requested.getJSONObject("plan").optString("key") == current.getJSONObject("plan").optString("key") &&
                "pending" == current.optString("state")
            ) {
                deliver(context)
                publish(context)
            }
            return receipt
        }
        if (frame == null ||
            !frame.optBoolean("active") ||
            !frame.optBoolean("automaticEnabled") ||
            db.commandsSuspended() ||
            frame.optBoolean("workoutPaused")
        ) {
            return JSONObject().put("status", "rejected").put("reason", "executionNotPrepared")
        }
        if (db.restoreBlocked()) return JSONObject().put("status", "rejected").put("reason", "restoreInProgress")
        val dependency = command.optString("undoDependency188")
        if (dependency.isNotEmpty() &&
            (frame.optJSONObject("undo188") == null || dependency != frame.getJSONObject("undo188").optString("commandId"))
        ) {
            return JSONObject().put("status", "rejected").put("reason", "undoConflict")
        }
        if (NativeGpsMeasurement.busy(frame) && command.optString("action") != "completeGpsSet") {
            return JSONObject().put("status", "rejected").put("reason", "gpsMeasurementStillActive")
        }
        val setClock = frame.optJSONObject(NativeSetStopwatch.FIELD)
        if (setClock != null && NativeSetStopwatch.running(setClock) && command.optString("action") != "completeTimedSet") {
            return JSONObject().put("status", "rejected").put("reason", "setStopwatchStillActive")
        }
        if ("undoSet" == command.optString("action")) {
            if (frame.getString("workoutId") != command.optString("workoutId") ||
                frame.getLong("generation") != command.optLong("generation")
            ) {
                return JSONObject().put("status", "rejected").put("reason", "undoConflict")
            }
            if (!db.recorded188(frame.getString("workoutId"), frame.getLong("generation"), command.optString("recordId"))) {
                return JSONObject()
                    .put("status", if (frame.getLong("executionRevision") <= command.optLong("executionRevision")) "deferred" else "rejected")
                    .put("reason", "recordingAwaitingDelivery")
            }
            val running = IntervalTimerService.snapshot(context)
            if (frame.getString("workoutId") == running.optString("workoutId") && !running.optBoolean("stopped")) {
                try {
                    IntervalTimerService.pauseForUndo(context, running.optString("key"))
                } catch (inactive: IllegalStateException) {
                    if (running.optBoolean("running")) throw inactive
                }
            }
            if (running.optBoolean("autoAdvance") &&
                !running.optBoolean("restOnly") &&
                running.optLong("stepElapsedMs") > 0L &&
                "work" == running.optString("phase")
            ) {
                val target = running.getJSONArray("steps").getJSONObject(running.optInt("stepIndex"))
                if (target.optString("setId") != command.optString("setId")) {
                    publish(context)
                    return JSONObject().put("status", "rejected").put("reason", "undoConflict")
                }
            }
            val undone = db.undo(command)
            publish(context)
            return undone
        }
        if (!IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation"))) {
            return JSONObject().put("status", "rejected").put("reason", "controllerNotArmed")
        }
        val timer = IntervalTimerService.snapshot(context)
        if (timer.optString("key").isNotEmpty() &&
            !timer.optBoolean("stopped") &&
            !timer.optBoolean("interrupted") &&
            "complete" != timer.optString("phase")
        ) {
            return JSONObject().put("status", "rejected").put("reason", "timerStillActive")
        }
        val transition = frame.optJSONObject("transition")
        val previousPlan = transition?.optJSONObject("plan")
        if (previousPlan != null && !previousPlan.optBoolean("restOnly")) {
            val owners = previousPlan.getJSONArray("steps")
            val exercises = frame.getJSONArray("exercises")
            for (i in 0 until owners.length()) {
                for (e in 0 until exercises.length()) {
                    val exercise = exercises.getJSONObject(e)
                    val sets = exercise.getJSONArray("sets")
                    for (j in 0 until sets.length()) {
                        if (owners.getJSONObject(i).getString("setId") == sets.getJSONObject(j).getString("setId") &&
                            !sets.getJSONObject(j).getBoolean("done")
                        ) {
                            return JSONObject().put("status", "rejected").put("reason", "timedResultsAwaitingReconciliation")
                        }
                    }
                }
            }
        }
        val result = if (command.optString("action") in setOf("completeSet", "completeTimedSet", "completeGpsSet", "completeMeasuredSet")) {
            db.record(command)
        } else {
            db.startTimeline(command)
        }
        if ("applied" == result.optString("status")) {
            deliver(context)
            publish(context)
        }
        return result
    }

    @JvmStatic
    @Throws(Exception::class)
    fun deliver(context: Context) {
        val db = store(context)
        val frame = db.snapshot()
        if (frame != null && !PhoneOwnership193.phoneAllowed(context, frame.optString("workoutId"))) return
        if (frame == null || !frame.optBoolean("active") || frame.optBoolean("workoutPaused")) return
        val transition = frame.optJSONObject("transition")
        val plan = transition?.optJSONObject("plan")
        if (plan == null || "pending" != transition.optString("state")) return
        var started = false
        try {
            started = IntervalTimerService.startPrepared(plan)
        } finally {
            db.markTransition(plan.getString("key"), if (started) "started" else "interrupted")
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun status(context: Context): JSONObject {
        val db = store(context)
        var frame = db.snapshot()
        var events = if (frame == null) JSONArray() else db.pendingEvents(frame.getString("workoutId"), frame.getLong("generation"), 0L)
        var timer = IntervalTimerService.snapshot(context)
        if (frame != null) {
            frame = db.snapshot()
            val latest = frame?.optJSONObject("latestTimer162")
            if (latest != null && (
                timer.optString("key") != latest.optString("key") ||
                    !IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation"))
                )
            ) {
                timer = JSONObject(latest.toString()).put("running", false).put("interrupted", true)
            }
        }
        val transition = frame?.optJSONObject("transition")
        val plan = transition?.optJSONObject("plan")
        if (frame != null &&
            frame.optBoolean("active") &&
            plan != null &&
            "interrupted" != transition.optString("state") &&
            (!IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation")) ||
                plan.getString("key") != timer.optString("key"))
        ) {
            db.markTransition(plan.getString("key"), "interrupted")
            frame = db.snapshot()
        }
        events = if (frame == null) JSONArray() else db.pendingEvents(frame.getString("workoutId"), frame.getLong("generation"), 0L)
        return JSONObject()
            .put("controllerArmed", frame != null && IntervalTimerService.armed(frame.optString("workoutId"), frame.optLong("generation")))
            .put("snapshot", frame ?: JSONObject.NULL)
            .put("events", events)
            .put("timer", timer)
    }

    @JvmStatic
    @Throws(Exception::class)
    fun display(context: Context): JSONObject? {
        val frame = store(context).snapshot()
        if (frame == null || !frame.optBoolean("active")) return null
        val value = JSONObject(frame.toString())
        value.remove("retiredTimers188")
        value.remove("timeLogs162")
        value.remove("latestTimer162")
        value.remove("manualAnchor162")
        value.remove("timerConfig")
        value.remove("automaticEnabled")
        value.remove("transition")
        value.put("nativeExecutionProtocol", 2)
        val transition = frame.optJSONObject("transition")
        val plan = transition?.optJSONObject("plan")
        if (plan != null) {
            val timer = IntervalTimerService.snapshot(context)
            if (plan.getString("key") == timer.optString("key")) {
                val interrupted = "interrupted" == transition.optString("state") || timer.optBoolean("interrupted")
                val running = timer.optBoolean("running") && !interrupted
                val remaining = timer.optLong("remainingMs")
                val restComplete = plan.optBoolean("restOnly") && "complete" == timer.optString("phase")
                if (restComplete && !interrupted) {
                    value.put("currentExercise", transition.getInt("currentExercise"))
                }
                val phaseStep = plan.getJSONArray("steps").getJSONObject(timer.optInt("stepIndex"))
                value.put(
                    "automation",
                    JSONObject()
                        .put("owner", "phone")
                        .put("enabled", true)
                        .put("key", plan.getString("key"))
                        .put("phase", if (interrupted) "ready" else if (restComplete) "ready" else timer.optString("phase"))
                        .put("paused", interrupted || (!running && "complete" != timer.optString("phase")))
                        .put("remainingMs", remaining)
                        .put("deadlineAt", if (running) System.currentTimeMillis() + remaining else 0L)
                        .put("workSeconds", phaseStep.getInt("work"))
                        .put(
                            "restSeconds",
                            if ("rest" == timer.optString("phase") &&
                                plan.optInt("initialRest") > 0 &&
                                timer.optLong("elapsedMs") < plan.optLong("initialRest") * 1000L
                            ) {
                                plan.getInt("initialRest")
                            } else {
                                phaseStep.getInt("rest")
                            }
                        )
                        .put("phaseNumber", timer.optLong("phaseNumber"))
                        .put("targetExercise", transition.getInt("currentExercise"))
                        .put("vibration", false)
                        .put("interrupted", interrupted)
                )
            }
        } else if (transition != null) {
            value.put(
                "automation",
                JSONObject()
                    .put("enabled", true)
                    .put("owner", "phone")
                    .put("phase", if ("paused" == transition.optString("phase")) "ready" else transition.optString("phase", "ready"))
                    .put("remainingMs", 0)
                    .put(
                        "key",
                        if ("paused" == transition.optString("state")) {
                            "undo188:" + (frame.optJSONObject("undo188")?.optString("commandId") ?: "")
                        } else {
                            ""
                        }
                    )
                    .put("paused", "paused" == transition.optString("state"))
            )
        }
        if (frame.optBoolean("workoutPaused")) {
            val a = value.optJSONObject("automation")
            a?.put("paused", true)?.put("deadlineAt", 0)
            value.put("restEndAt", 0)
        }
        return value
    }

    @JvmStatic
    @Throws(Exception::class)
    fun setWorkoutPause(context: Context, input: JSONObject?) {
        if (input == null || input.optString("workoutId").isEmpty()) {
            throw IllegalArgumentException("missingWorkoutPause")
        }
        if (!PhoneOwnership193.phoneAllowed(context, input.optString("workoutId"))) {
            throw IllegalStateException("Workout ownership changed")
        }
        val active = ActiveWorkoutStore.current(context)
        val db = store(context)
        val before = db.snapshot()
        val nativeOwner = before != null && before.optBoolean("active") && input.optString("workoutId") == before.optString("workoutId")
        if (!nativeOwner && active != null && active.optBoolean("active") && input.optString("workoutId") != active.optString("workoutId")) {
            throw IllegalStateException("Workout changed")
        }
        if (nativeOwner && before!!.optInt("nativeEditorProjection") == 1) {
            // All native pause callers (including remote control) must stop the bound GPS,
            // not just the Compose button. Reject stale input before touching that service.
            check(input.optLong("generation") == before.optLong("generation")) { "Workout generation changed" }
            val revision = input.optLong("pauseRevision")
            require(revision in 1L..9_007_199_254_740_991L) { "invalidWorkoutPause" }
            check(revision >= before.optLong("pauseRevision") &&
                (revision != before.optLong("pauseRevision") || input.optBoolean("workoutPaused") == before.optBoolean("workoutPaused"))) {
                "staleWorkoutPause"
            }
            if (input.optBoolean("workoutPaused") && revision > before.optLong("pauseRevision") && NativeGpsMeasurement.busy(before)) {
                NativeWorkoutEditing.interruptGps(context)
            }
        }
        val timer = IntervalTimerService.snapshot(context)
        val frame = db.setWorkoutPause(input, SystemClock.elapsedRealtime(), timer.optBoolean("running"))
        if (frame != null) {
            if (frame.optBoolean("workoutPaused") && timer.optBoolean("running")) {
                IntervalTimerService.command(context, timer.optString("key"), "pause")
            } else if (before != null &&
                input.optLong("pauseRevision") > before.optLong("pauseRevision") &&
                !frame.optBoolean("workoutPaused") &&
                frame.optBoolean("resumeTimerOnContinue201") &&
                !timer.optBoolean("running") &&
                !timer.optBoolean("interrupted") &&
                !timer.optBoolean("stopped") &&
                "complete" != timer.optString("phase")
            ) {
                IntervalTimerService.command(context, timer.optString("key"), "resume")
            }
            publish(context)
        } else {
            if (active != null &&
                input.optString("workoutId") == active.optString("workoutId") &&
                input.optLong("pauseRevision") < active.optLong("pauseRevision")
            ) {
                throw IllegalStateException("staleWorkoutPause")
            }
            val saved = ActiveWorkoutStore.save(context, input)
            val map = PutDataMapRequest.create(WearDataLayerPaths.ACTIVE_WORKOUT_PATH)
            map.dataMap.putString("snapshot", saved.toString())
            map.dataMap.putBoolean("active", true)
            map.dataMap.putLong("revision", saved.optLong("revision"))
            Wearable.getDataClient(context).putDataItem(map.asPutDataRequest().setUrgent())
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun publish(context: Context) {
        val value = display(context) ?: return
        // Original protocol frames keep their pending events until their explicit
        // consumer commits them. Only a complete native-editor frame opts into
        // the durable native projection; background transport alone is no consumer.
        val saved = if (value.optInt("nativeEditorProjection") == 1) NativeWorkoutEditing.project(context, value)
            else ActiveWorkoutStore.save(context, value)
        val map = PutDataMapRequest.create(WearDataLayerPaths.ACTIVE_WORKOUT_PATH)
        map.dataMap.putString("snapshot", saved.toString())
        map.dataMap.putBoolean("active", true)
        map.dataMap.putLong("revision", saved.getLong("revision"))
        // Local completion is durable even while Google Play Services / the watch
        // is unavailable. The saved snapshot remains available for later sync.
        runCatching { Wearable.getDataClient(context).putDataItem(map.asPutDataRequest().setUrgent()) }
    }
}
