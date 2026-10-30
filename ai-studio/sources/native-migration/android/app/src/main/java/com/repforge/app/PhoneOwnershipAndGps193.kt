package com.repforge.app

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import com.google.android.gms.wearable.Wearable
import com.repforge.shared.GpsRouteLedger193
import com.repforge.shared.GpsRoutePacket193
import com.repforge.shared.GpsRouteSpool193
import com.repforge.shared.OwnershipJournal193
import com.repforge.shared.OwnershipTransport193
import com.repforge.shared.WorkoutOwnership193
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.max

/** GPS chunks bypass the generic command queue; receipts contain references only. */
internal object PhoneGpsRoute193 {
    const val ACTION: String = "gpsRouteChunk193"

    private fun requireRoute(value: Boolean) {
        if (!value) throw IllegalArgumentException("Invalid watch route delivery")
    }

    @JvmStatic
    fun startOrigin(command: JSONObject): String {
        val root = command.optJSONObject("finalSnapshot")
        if (root == null ||
            root.optInt("ownershipProtocol193") != 1 ||
            root.optLong("ownerEpoch193") != 0L ||
            root.has("handoff193") ||
            "watch" != root.optString("timerOwner") ||
            root.optString("workoutId") != command.optString("workoutId")
        ) {
            return ""
        }
        if (command.optString("action") !in listOf(
                "startWorkout", "startQuickWorkout", "addQuickExercise",
                "finishWorkout", "discardWorkout", "watchAutomationState"
            )
        ) {
            return ""
        }
        val uri = Uri.parse(command.optString("_uri"))
        val node = uri.host
        val validPath = (WearCommandListenerService.COMMAND_PATH_PREFIX + command.optString("commandId")) == uri.path ||
            ("watchAutomationState" == command.optString("action") && "/trainpilot/watch-automatic" == uri.path)
        return if ("wear" == uri.scheme && GpsRoutePacket193.id(node) && validPath) node!! else ""
    }

    @JvmStatic
    @Throws(Exception::class)
    fun captureOrigin(context: Context, snapshot: JSONObject) {
        snapshot.remove("watchSourceNode193")
        if (snapshot.optInt("ownershipProtocol193") != 1 ||
            snapshot.optLong("ownerEpoch193") != 0L ||
            snapshot.has("handoff193") ||
            "watch" != snapshot.optString("timerOwner")
        ) {
            return
        }
        var node = ""
        val prior = ActiveWorkoutStore.current(context)
        if (prior != null &&
            prior.optString("workoutId") == snapshot.optString("workoutId") &&
            prior.optInt("ownershipProtocol193") == 1 &&
            "watch" == prior.optString("timerOwner") &&
            prior.optLong("ownerEpoch193") == 0L &&
            !prior.has("handoff193")
        ) {
            node = prior.optString("watchSourceNode193")
        }
        if (!GpsRoutePacket193.id(node)) {
            node = WearCommandQueueStore.sourceOrigin193(context, snapshot.optString("workoutId"))
        }
        if (GpsRoutePacket193.id(node)) {
            snapshot.put("watchSourceNode193", node)
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun authorize(context: Context, snapshot: JSONObject) {
        if (snapshot.optInt("ownershipProtocol193") != 1) return
        var source = ""
        var target = ""
        var epoch = snapshot.optLong("ownerEpoch193")
        val h = snapshot.optJSONObject("handoff193")
        val pendingStart = epoch == 0L &&
            h == null &&
            "watch" == snapshot.optString("timerOwner") &&
            GpsRoutePacket193.id(snapshot.optString("watchSourceNode193"))
        if (pendingStart) {
            source = snapshot.getString("watchSourceNode193")
        } else if (h != null && h.optInt("protocol") == 1 && h.optLong("epoch") >= 1L) {
            if ("committed" == h.optString("state") && h.optLong("epoch") == epoch) {
                if ("watch" == h.optString("target") && "watch" == snapshot.optString("timerOwner")) {
                    source = h.getString("targetNode")
                    target = h.getString("sourceNode")
                } else if ("watch" == h.optString("source") && "phone" == snapshot.optString("timerOwner")) {
                    source = h.getString("sourceNode")
                    target = h.getString("targetNode")
                    epoch--
                }
            } else if (h.optString("state") in listOf("offered", "accepted") &&
                h.optLong("epoch") == epoch + 1L &&
                "watch" == h.optString("source") &&
                "watch" == snapshot.optString("timerOwner")
            ) {
                source = h.getString("sourceNode")
                target = h.getString("targetNode")
            }
        }
        if (!GpsRoutePacket193.id(source) || (!pendingStart && !GpsRoutePacket193.id(target))) return
        val exercises = snapshot.optJSONArray("exercises") ?: return
        val ledger = GpsRouteLedger193.open(context.noBackupFilesDir)
        for (i in 0 until exercises.length()) {
            val ex = exercises.getJSONObject(i)
            val unit = ex.optString("repUnit").trim().lowercase(Locale.ROOT)
            if ("distance" != ex.optString("measurementType") && "km" != unit && "m" != unit) continue
            val sets = ex.optJSONArray("sets") ?: continue
            for (j in 0 until sets.length()) {
                val set = sets.getJSONObject(j).optString("setId")
                if (set.isEmpty()) continue
                val grant = JSONObject()
                    .put("routeKey", GpsRoutePacket193.key(snapshot.getString("workoutId"), set))
                    .put("workoutId", snapshot.getString("workoutId"))
                    .put("setId", set)
                    .put("exerciseId", ex.getString("id"))
                    .put("sourceNode", source)
                    .put("ownerEpoch193", epoch)
                if (pendingStart) {
                    ledger.rememberSource(grant)
                } else {
                    ledger.authorize(grant.put("targetNode", target))
                }
            }
        }
    }

    @JvmStatic
    fun authorizeOwned(context: Context, snapshot: JSONObject) {
        try {
            authorize(context, snapshot)
        } catch (_: Exception) {
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun accept(context: Context, command: JSONObject, origin: String, localNode: String): JSONObject? {
        requireRoute(
            ACTION == command.optString("action") &&
                command.toString().toByteArray(StandardCharsets.UTF_8).size <= 16_000
        )
        requireRoute(
            command.get("schema") is Number &&
                command.getDouble("schema") == 1.0 &&
                command.get("createdAt") is Number &&
                command.getDouble("createdAt") == command.getLong("createdAt").toDouble() &&
                command.getLong("createdAt") > 0L
        )
        val packet = command.getJSONObject("gpsRoutePacket193")
        val expected = GpsRouteSpool193.pointer(packet)
        val pointer = command.getJSONObject("route193")
        requireRoute(
            expected.getString("commandId") == command.getString("commandId") &&
                expected.getString("workoutId") == command.getString("workoutId") &&
                command.get("ownerEpoch193") is Number &&
                command.getDouble("ownerEpoch193") == command.getLong("ownerEpoch193").toDouble() &&
                command.getLong("ownerEpoch193") == packet.getLong("ownerEpoch193") &&
                command.get("sequence") is Number &&
                command.getDouble("sequence") == command.getLong("sequence").toDouble() &&
                command.getLong("sequence") > 0L
        )
        for (key in arrayOf("routeKey", "streamId", "sequence", "sourceNode", "targetNode", "hash")) {
            requireRoute(expected.getJSONObject("route193").get(key).toString() == pointer.get(key).toString())
        }
        val proof = GpsRouteLedger193.open(context.noBackupFilesDir).accept(packet, origin, localNode) ?: return null
        val clean = JSONObject()
        for (key in arrayOf("schema", "action", "commandId", "workoutId", "ownerEpoch193", "sequence", "createdAt")) {
            clean.put(key, command.get(key))
        }
        clean.put("route193", expected.getJSONObject("route193"))
        if ("deleted" == proof.getString("status")) clean.put("routeDeleted193", true)
        return clean
    }

    @JvmStatic
    fun receive(context: Context, command: JSONObject, uri: Uri): Boolean {
        if (ACTION != command.optString("action")) return command.has("gpsRoutePacket193")
        try {
            val origin = uri.host
            if (!GpsRoutePacket193.id(origin) ||
                (WearCommandListenerService.COMMAND_PATH_PREFIX + command.optString("commandId")) != uri.path
            ) {
                return true
            }
            val copy = JSONObject(command.toString())
            Wearable.getNodeClient(context).localNode.addOnSuccessListener { local ->
                if (local.id == origin) return@addOnSuccessListener
                try {
                    val receipt = accept(context, copy, origin!!, local.id)
                    if (receipt != null) {
                        WearCommandReceipts.complete(context, receipt, "applied", uri)
                    }
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
        return true
    }
}

/** Immutable source clock checkpoint. Device-local monotonic anchors are never transferred. */
internal object PhoneTimerCheckpoint193 {
    private fun requireCheckpoint(condition: Boolean) {
        if (!condition) throw IllegalStateException("Reconcile the paused timer before transferring the workout")
    }

    @Throws(Exception::class)
    private fun integer(value: JSONObject, field: String, maxVal: Long): Long {
        val n = PhoneWorkoutRecording.version(value, field)
        requireCheckpoint(n <= maxVal)
        return n
    }

    @JvmStatic
    @Throws(Exception::class)
    fun create(frozen: JSONObject, timer: JSONObject?): JSONObject? {
        val a = frozen.optJSONObject("automation")
        if (timer == null ||
            timer.optString("key").isEmpty() ||
            frozen.optString("workoutId") != timer.optString("workoutId") ||
            timer.optBoolean("stopped") ||
            "complete" == timer.optString("phase")
        ) {
            requireCheckpoint(
                a == null ||
                    !a.optBoolean("enabled") ||
                    !("work" == a.optString("phase") || "rest" == a.optString("phase"))
            )
            return null
        }
        requireCheckpoint(
            frozen.optBoolean("workoutPaused") &&
                !timer.optBoolean("running") &&
                !timer.optBoolean("interrupted")
        )
        val timeline = IntervalTimerService.validate(timer)
        val elapsed = integer(timer, "elapsedMs", IntervalTimeline.MAX_MS)
        val phase = timeline.at(elapsed)
        requireCheckpoint(
            elapsed == phase.elapsedMs &&
                timer.getString("phase") == phase.phase &&
                timer.getInt("stepIndex") == phase.step &&
                timer.getInt("round") == phase.round &&
                timer.getLong("remainingMs") == phase.remainingMs &&
                timer.getLong("stepElapsedMs") == phase.stepElapsedMs &&
                timer.getLong("totalMs") == phase.totalMs &&
                timer.getInt("completed") == phase.completed
        )
        val ledger = timer.getJSONObject("ledger")
        val rows = timer.getJSONArray("steps")
        val parts = ledger.getJSONArray("steps")
        requireCheckpoint(
            parts.length() == rows.length() &&
                ledger.getInt("schema") == 1 &&
                "phone" == ledger.getString("owner") &&
                integer(ledger, "positionMs", IntervalTimeline.MAX_MS) == elapsed
        )
        var work = 0L
        var rest = 0L
        var paused = 0L
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val part = parts.getJSONObject(i)
            requireCheckpoint(
                row.getString("exerciseId") == part.getString("exerciseId") &&
                    row.getString("setId") == part.getString("setId") &&
                    row.optString("gpsKey").isEmpty() &&
                    contains(frozen, row)
            )
            val base = row.optDouble("baseSeconds", 0.0)
            requireCheckpoint(base.isFinite() && base in 0.0..86400.0)
            work += integer(part, "workMs", IntervalTimeline.MAX_MS)
            rest += integer(part, "restMs", IntervalTimeline.MAX_MS)
            paused += integer(part, "pausedMs", 604_800_000L)
        }
        requireCheckpoint(
            work == integer(ledger, "workMs", IntervalTimeline.MAX_MS) &&
                rest == integer(ledger, "restMs", IntervalTimeline.MAX_MS) &&
                paused == integer(ledger, "pausedMs", 604_800_000L) &&
                work + rest <= elapsed
        )
        val exact = JSONObject(timer.toString())
        exact.remove("anchorMs")
        exact.remove("began")
        exact.remove("boot")
        exact.remove("deadlineAt")
        return JSONObject()
            .put("schema", 1)
            .put("source", "phone")
            .put("sourceEpoch", frozen.optLong("ownerEpoch193"))
            .put("timer", exact)
            .put("resumeRequested", false)
    }

    @Throws(Exception::class)
    private fun contains(workout: JSONObject, row: JSONObject): Boolean {
        val exercises = workout.getJSONArray("exercises")
        for (i in 0 until exercises.length()) {
            val e = exercises.getJSONObject(i)
            if (row.getString("exerciseId") != e.optString("id")) continue
            val sets = e.getJSONArray("sets")
            for (j in 0 until sets.length()) {
                if (row.getString("setId") == sets.getJSONObject(j).optString("setId")) return true
            }
        }
        return false
    }

    @JvmStatic
    @Throws(Exception::class)
    fun verifyCanonical(frozen: JSONObject, status: JSONObject) {
        val frame = status.optJSONObject("snapshot")
        if (frame == null || !frame.optBoolean("active") || frozen.optString("workoutId") != frame.optString("workoutId")) {
            return
        }
        requireCheckpoint(
            frame.optBoolean("workoutPaused") &&
                frozen.optLong("generation") == frame.optLong("generation") &&
                frozen.optLong("executionRevision") == frame.optLong("executionRevision") &&
                status.getJSONArray("events").length() == 0
        )
        val own = frozen.getJSONArray("exercises")
        val nativeRows = frame.getJSONArray("exercises")
        requireCheckpoint(own.length() == nativeRows.length())
        for (i in 0 until own.length()) {
            val a = own.getJSONObject(i)
            val b = nativeRows.getJSONObject(i)
            requireCheckpoint(a.getString("id") == b.getString("id"))
            val aa = a.getJSONArray("sets")
            val bb = b.getJSONArray("sets")
            requireCheckpoint(aa.length() == bb.length())
            for (j in 0 until aa.length()) {
                val x = aa.getJSONObject(j)
                val y = bb.getJSONObject(j)
                requireCheckpoint(x.getString("setId") == y.getString("setId") && x.getBoolean("done") == y.getBoolean("done"))
                for (field in arrayOf("reps", "weight", "leftSeconds", "rightSeconds", "distanceMeters")) {
                    val first = x.optString(field)
                    val second = y.optString(field)
                    if (first == second) continue
                    requireCheckpoint(
                        first.isNotEmpty() &&
                            second.isNotEmpty() &&
                            first.toDouble() == second.toDouble()
                    )
                }
            }
        }
    }
}

/** Builds a fresh paused phone execution from the locally acknowledged reverse handoff. */
internal object PhoneOwnershipResume193 {
    private fun requireResume(value: Boolean) {
        if (!value) throw IllegalStateException("The returned workout changed; reconcile before continuing")
    }

    @JvmStatic
    @Throws(Exception::class)
    fun prepare(consent: JSONObject, old: JSONObject?, now: Long): JSONObject {
        val h = WorkoutOwnership193.transfer(consent)
        requireResume(
            WorkoutOwnership193.owns(consent, "phone") &&
                consent.optBoolean("workoutPaused") &&
                consent.optInt("ownershipProtocol193") == 1 &&
                h != null &&
                h.optInt("protocol") == 1 &&
                "committed" == h.optString("state") &&
                "phone" == h.optString("target") &&
                "watch" == h.optString("source") &&
                h.optLong("epoch") == WorkoutOwnership193.epoch(consent)
        )
        val nonNullH = h!!
        val prior = old?.optJSONObject("ownershipImport193")
        if (prior != null &&
            prior.optString("id") == nonNullH.optString("id") &&
            prior.optLong("epoch") == nonNullH.optLong("epoch")
        ) {
            requireResume(old.optBoolean("active") && old.optString("workoutId") == consent.optString("workoutId"))
            return JSONObject(old.toString())
        }
        if (old != null) {
            requireResume(!old.optBoolean("active") || old.optString("workoutId") == consent.optString("workoutId"))
            if (old.optString("workoutId") == consent.optString("workoutId")) {
                requireResume(old.optBoolean("active") && old.optLong("ownerEpoch193") < WorkoutOwnership193.epoch(consent))
            }
        }
        val generation = max(now, if (old == null) 1L else PhoneWorkoutRecording.advance(old.getLong("generation")))
        requireResume(generation in 1L..9_007_199_254_740_991L)
        val frame = JSONObject(consent.toString())
        val a = consent.optJSONObject("automation")
        frame.put("generation", generation)
            .put("executionRevision", 1)
            .put("nativeExecutionProtocol", 2)
            .put("automaticEnabled", a != null && a.optBoolean("enabled"))
            .put(
                "ownershipImport193",
                JSONObject()
                    .put("id", nonNullH.getString("id"))
                    .put("epoch", nonNullH.getLong("epoch"))
                    .put("state", "prepared")
            )
        frame.remove("transition")
        frame.remove("latestTimer162")
        frame.remove("manualAnchor162")
        frame.remove("retiredTimers188")
        val config = JSONObject()
            .put("work", a?.optInt("workSeconds", 30) ?: 30)
            .put("rest", a?.optInt("restSeconds", 90) ?: 90)
            .put("rounds", a?.optInt("rounds", 1) ?: 1)
            .put("sound", true)
            .put("vibration", a == null || a.optBoolean("vibration", true))
        frame.put("timerConfig", config)
        val checkpoint = consent.optJSONObject("timerCheckpoint193")
        if (checkpoint != null) {
            requireResume(checkpoint.optInt("schema") == 1)
            val timer = JSONObject(checkpoint.getJSONObject("timer").toString())
            val checked = JSONObject(timer.toString())
            checked.getJSONObject("ledger").put("owner", "phone")
            requireResume(PhoneTimerCheckpoint193.create(consent, checked) != null)
            val key = "ownership193:phone:${nonNullH.getLong("epoch")}:${nonNullH.getString("id")}"
            timer.put("key", key)
                .put("started", frame.optString("started"))
                .put("generation", generation)
                .put("running", false)
                .put("stopped", false)
                .put("interrupted", false)
                .put("ownershipBaseLedger193", JSONObject(timer.getJSONObject("ledger").toString()))
            val target = timer.getJSONArray("steps").getJSONObject(timer.getInt("stepIndex"))
            timer.put(
                "target",
                JSONObject()
                    .put("exerciseId", target.getString("exerciseId"))
                    .put("setId", target.getString("setId"))
            )
            frame.put(
                "transition",
                JSONObject()
                    .put("state", "ownership193")
                    .put("phase", timer.getString("phase"))
                    .put("currentExercise", frame.getInt("currentExercise"))
                    .put("plan", timer)
            )
            frame.put("resumeTimerOnContinue201", consent.optBoolean("resumeAutomaticOnContinue"))
            val logs = checkpoint.optJSONObject("timeLogs193")
            if (logs != null) frame.put("timeLogs162", JSONObject(logs.toString()))
        }
        PhoneWorkoutRecording.validateFrame(frame)
        PhoneWorkoutPlans.validateConfig(frame)
        return frame
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun localLog(timer: JSONObject, aggregate: JSONObject): JSONObject {
        val base = timer.optJSONObject("ownershipBaseLedger193")
        val log = JSONObject(aggregate.toString())
        if (base == null) return log
        val rows = log.getJSONArray("steps")
        val before = base.getJSONArray("steps")
        requireResume(rows.length() == before.length())
        var position = 0L
        for (key in arrayOf("workMs", "restMs", "pausedMs")) {
            val delta = log.getLong(key) - base.getLong(key)
            requireResume(delta >= 0L)
            log.put(key, delta)
            position += delta
        }
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val old = before.getJSONObject(i)
            requireResume(
                row.getString("setId") == old.getString("setId") &&
                    row.getString("exerciseId") == old.getString("exerciseId")
            )
            for (key in arrayOf("workMs", "restMs", "pausedMs")) {
                val delta = row.getLong(key) - old.getLong(key)
                requireResume(delta >= 0L)
                row.put(key, delta)
            }
        }
        return log.put("positionMs", position).put("owner", "phone")
    }
}

/** Main-looper adapter for workout ownership transitions. */
internal object PhoneOwnership193 {
    private var journal: OwnershipJournal193? = null
    private var journalContext: Context? = null
    private var scannedAt: Long = 0L

    @JvmStatic
    @Synchronized
    fun journal(context: Context): OwnershipJournal193 {
        val app = context.applicationContext
        if (journal == null || journalContext !== app) {
            journalContext = app
            journal = OwnershipJournal193(
                app,
                object : OwnershipJournal193.Host {
                    override fun current(): JSONObject? = ActiveWorkoutStore.current(app)
                    override fun install(value: JSONObject) {
                        ActiveWorkoutStore.saveOwned(app, value)
                        PhoneGpsRoute193.authorizeOwned(app, value)
                    }
                }
            )
        }
        return journal!!
    }

    @JvmStatic
    @Throws(Exception::class)
    fun receive(context: Context, message: JSONObject, origin: String, localNode: String) {
        val store = journal(context)
        synchronized(store) {
            check(!NativeDataReplacement.blocked(context)) { "Adat-visszaállítás vagy törlés van folyamatban." }
            store.recover()
            val state = store.receive(message, origin, localNode, System.currentTimeMillis())
            store.recover()
            // Native auto-prepare when the watch requests takeover and the phone workout is ready/paused!
            if ("incoming" == state.optString("stage")) {
                try {
                    WearSyncRepository.autoPrepareIncomingOwnershipIfEligible(context, localNode)
                } catch (_: Exception) {
                }
            }
            OwnershipTransport193.send(context, store)
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun prepare(context: Context, frozen: JSONObject, requestId: String, localNode: String) {
        val store = journal(context)
        val request = store.read().optJSONObject("request")
        if (request == null ||
            requestId != request.optString("id") ||
            localNode != request.optString("target") ||
            request.optString("workoutId") != frozen.optString("workoutId") ||
            request.optLong("epoch") != frozen.optLong("ownerEpoch193") ||
            "phone" != frozen.optString("timerOwner", "phone") ||
            !frozen.optBoolean("workoutPaused")
        ) {
            throw IllegalStateException("The workout transfer changed")
        }
        val active = ActiveWorkoutStore.current(context)
        val now = System.currentTimeMillis()
        if (active == null ||
            !active.optBoolean("active", true) ||
            frozen.optString("workoutId") != active.optString("workoutId") ||
            WorkoutOwnership193.pending(active) ||
            "phone" != active.optString("timerOwner", "phone") ||
            active.optLong("ownerEpoch193") != request.optLong("epoch") ||
            now - request.optLong("createdAt") > 30_000L ||
            now < request.optLong("createdAt") - 5_000L
        ) {
            throw IllegalStateException("The workout transfer changed")
        }
        if (frozen.optInt("ownershipProtocol193") != 1) {
            throw IllegalStateException("Update both apps before transferring a workout")
        }
        val gps = DistanceTrackingService.snapshot(context)
        if (gps.optBoolean("active")) {
            throw IllegalStateException("Finish recording GPS values before transfer")
        }
        val status = PhoneWorkoutExecution.status(context)
        if (frozen.optJSONObject(NativeSetStopwatch.FIELD)?.optBoolean("running") == true ||
            status.optJSONObject("snapshot")?.optJSONObject(NativeSetStopwatch.FIELD)?.optBoolean("running") == true) {
            throw IllegalStateException("Pause the set stopwatch before transfer")
        }
        PhoneTimerCheckpoint193.verifyCanonical(frozen, status)
        val prepared = JSONObject(frozen.toString())
        prepared.remove("timerCheckpoint193")
        val checkpoint = PhoneTimerCheckpoint193.create(prepared, status.optJSONObject("timer"))
        if (checkpoint != null) {
            val frame = status.optJSONObject("snapshot")
            var logs = frame?.optJSONObject("timeLogs162")
            logs = if (logs == null) JSONObject() else JSONObject(logs.toString())
            val timer = status.getJSONObject("timer")
            val log = PhoneOwnershipResume193.localLog(timer, timer.getJSONObject("ledger"))
            logs.put(timer.getString("key"), log.put("key", timer.getString("key")))
            checkpoint.put("timeLogs193", logs)
            prepared.put("timerCheckpoint193", checkpoint)
        }
        PhoneWorkoutExecution.store(context).suspendCommands(true)
        IntervalTimerService.stopForOwnership(context, frozen.getString("workoutId"))
        store.prepare(prepared, localNode, System.currentTimeMillis())
        OwnershipTransport193.send(context, store)
    }

    @JvmStatic
    @Throws(Exception::class)
    fun status(context: Context): JSONObject {
        val store = journal(context)
        store.recover()
        OwnershipTransport193.send(context, store)
        val now = SystemClock.elapsedRealtime()
        if (now - scannedAt > 5_000L) {
            scannedAt = now
            Wearable.getDataClient(context).dataItems.addOnSuccessListener { items ->
                try {
                    for (item in items) {
                        OwnershipTransport193.receive(context, item) { message, origin, local ->
                            receive(context, message, origin, local)
                        }
                    }
                } finally {
                    items.release()
                }
            }
        }
        return JSONObject()
            .put("ownershipProtocol193", 1)
            .put("state", store.read())
            .put("snapshot", ActiveWorkoutStore.current(context))
    }

    @Throws(Exception::class)
    private fun returned(context: Context, store: OwnershipJournal193): JSONObject {
        val proof = store.read().optJSONObject("snapshot")
        val current = ActiveWorkoutStore.current(context)
        val h = WorkoutOwnership193.transfer(proof)
        val live = WorkoutOwnership193.transfer(current)
        if (!WorkoutOwnership193.owns(proof, "phone") ||
            h == null ||
            "watch" != h.optString("source") ||
            "phone" != h.optString("target") ||
            !WorkoutOwnership193.owns(current, "phone") ||
            proof!!.optString("workoutId") != current!!.optString("workoutId") ||
            WorkoutOwnership193.epoch(proof) != WorkoutOwnership193.epoch(current) ||
            live == null ||
            h.optString("id") != live.optString("id")
        ) {
            throw IllegalStateException("The returned workout changed")
        }
        return current
    }

    @JvmStatic
    @Throws(Exception::class)
    fun prepareResume(context: Context): JSONObject {
        val store = journal(context)
        synchronized(store) {
            store.recover()
            val current = returned(context, store)
            val frame = PhoneWorkoutExecution.store(context).importOwnership193(current, System.currentTimeMillis())
            return JSONObject().put("snapshot", frame)
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun installResume(context: Context, requestId: String, epoch: Long): JSONObject {
        val store = journal(context)
        synchronized(store) {
            val current = returned(context, store)
            val h = WorkoutOwnership193.transfer(current)!!
            if (requestId != h.optString("id") || epoch != h.optLong("epoch")) {
                throw IllegalStateException("The returned workout changed")
            }
            val db = PhoneWorkoutExecution.store(context)
            var frame = db.snapshot()
            val marker = frame?.optJSONObject("ownershipImport193")
            if (marker == null ||
                requestId != marker.optString("id") ||
                epoch != marker.optLong("epoch") ||
                !frame.optBoolean("workoutPaused")
            ) {
                throw IllegalStateException("Prepare the returned workout before continuing")
            }
            val transition = frame.optJSONObject("transition")
            var plan = transition?.optJSONObject("plan")
            var timer: JSONObject? = null
            if (plan != null) {
                val latest = frame.optJSONObject("latestTimer162")
                if (latest != null &&
                    latest.optString("key") == plan.optString("key") &&
                    latest.optLong("generation") == frame.optLong("generation")
                ) {
                    plan = latest
                }
                timer = IntervalTimerService.installOwnership193(plan)
                db.markTransition(plan.getString("key"), "started")
            }
            frame = db.ownershipReady193(requestId, epoch)
            return JSONObject().put("snapshot", frame).put("timer", timer ?: JSONObject.NULL)
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun publicationAllowed(context: Context, incoming: JSONObject): Boolean {
        val saved = journal(context).read().optJSONObject("snapshot")
        if (saved == null || saved.optString("workoutId") != incoming.optString("workoutId")) {
            return true
        }
        return saved.optBoolean("active", true) &&
            !WorkoutOwnership193.pending(saved) &&
            !WorkoutOwnership193.stale(saved, incoming)
    }

    @JvmStatic
    fun phoneAllowed(context: Context, workoutId: String): Boolean {
        return try {
            val saved = journal(context).read().optJSONObject("snapshot")
            saved == null || workoutId != saved.optString("workoutId") || WorkoutOwnership193.owns(saved, "phone")
        } catch (_: Exception) {
            false
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun commandAllowed(context: Context, command: JSONObject): Boolean {
        val current = ActiveWorkoutStore.current(context)
        if (current == null || current.optString("workoutId") != command.optString("workoutId")) {
            return true
        }
        if (command.optString("action") in listOf("healthSummary", "saveWorkoutFeedback", "requestHome")) {
            return true
        }
        if (!current.optBoolean("active", true)) return false
        val saved = journal(context).read().optJSONObject("snapshot")
        if (saved == null || current.optString("workoutId") != saved.optString("workoutId")) {
            return command.optLong("ownerEpoch193") == 0L
        }
        return !WorkoutOwnership193.pending(saved) &&
            command.optLong("ownerEpoch193") == WorkoutOwnership193.epoch(saved)
    }
}
