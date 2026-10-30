package com.repforge.app

import android.os.SystemClock

import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.repforge.shared.WorkoutOwnership193
import org.json.JSONObject
import java.util.UUID

/**
 * PhoneWorkoutExecutionStore is the phone editor's durable authority. ActiveWorkoutStore
 * is its UI/transport projection, committed before event acknowledgements. No second editor state.
 */
internal object NativeWorkoutEditing {
    fun project(context: Context, display: JSONObject? = null): JSONObject {
        val store = PhoneWorkoutExecution.store(context)
        check(!store.restoreBlocked() && !store.commandsSuspended()) { "A natív edzésvezérlés átmenetileg foglalt." }
        val projection = store.projection()
        val frame = projection.optJSONObject("snapshot") ?: error("Nincs előkészített natív edzés.")
        val current = ActiveWorkoutStore.current(context)
        requireEditable(frame, current)
        if (display != null && display.optString("workoutId") == frame.optString("workoutId") &&
            display.optLong("generation") == frame.optLong("generation") &&
            display.optLong("executionRevision") == frame.optLong("executionRevision")) {
            for (key in listOf("automation", "nativeExecutionProtocol", "restEndAt")) if (display.has(key)) frame.put(key, display.get(key))
        }
        val sequence = projection.getLong("sequence")
        frame.put("phoneExecution162", JSONObject().put("generation", frame.getLong("generation"))
            .put("revision", frame.getLong("executionRevision")).put("sequence", sequence))
        NativeWorkoutEventMerge.mergeTime(frame, frame.optJSONObject("timeLogs162") ?: JSONObject())
        val saved = ActiveWorkoutStore.save(context, frame)
        if (sequence > 0) {
            val latest = store.snapshot()!!
            check(latest.optString("workoutId") == frame.optString("workoutId") && latest.optLong("generation") == frame.optLong("generation")) {
                "A natív esemény nyugtázása közben megváltozott az edzés."
            }
            check(store.acknowledgeEvents(frame.getString("workoutId"), frame.getLong("generation"), sequence)) {
                "A natív esemény nyugtázása nem sikerült."
            }
        }
        return saved
    }

    fun commitEdit(context: Context, edit: JSONObject, clockEdit: Boolean = false, gpsEdit: Boolean = false): JSONObject {
        val store = PhoneWorkoutExecution.store(context)
        check(!store.restoreBlocked() && !store.commandsSuspended()) { "A natív edzésvezérlés átmenetileg foglalt." }
        val frame = store.snapshot() ?: error("Nincs előkészített natív edzés.")
        requireEditable(frame, ActiveWorkoutStore.current(context))
        check(edit.optString("workoutId") == frame.optString("workoutId") &&
            edit.optLong("generation") == frame.optLong("generation") &&
            edit.optLong("executionRevision") == frame.optLong("executionRevision")) {
            "Az edzés időközben frissült. Próbáld újra a módosítást."
        }
        val incoming = JSONObject(edit.toString())
        check(incoming.optInt("nativeEditorProjection") == 1) { "A natív edzés állapota megváltozott." }
        if (!clockEdit) {
            if (frame.has(NativeSetStopwatch.FIELD)) incoming.put(NativeSetStopwatch.FIELD, frame.get(NativeSetStopwatch.FIELD))
            else incoming.remove(NativeSetStopwatch.FIELD)
        }
        if (!gpsEdit) {
            if (frame.has(NativeGpsMeasurement.FIELD)) incoming.put(NativeGpsMeasurement.FIELD, frame.get(NativeGpsMeasurement.FIELD))
            else incoming.remove(NativeGpsMeasurement.FIELD)
            if (NativeGpsMeasurement.busy(frame)) {
                val target = NativeGpsMeasurement.target(frame)
                check(PhoneWorkoutExecutionStore.recordedBase(target.resolve(frame).set).toString() ==
                    PhoneWorkoutExecutionStore.recordedBase(target.resolve(incoming).set).toString()) {
                    "Előbb fejezd be a sorozat GPS-mérését."
                }
            }
        }
        val activeTimer = frame.optJSONObject("latestTimer162")
        if (activeTimer != null && !activeTimer.optBoolean("restOnly") && !activeTimer.optBoolean("stopped") &&
            !activeTimer.optBoolean("interrupted") && activeTimer.optString("phase") != "complete") {
            val rows = activeTimer.getJSONArray("steps")
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val target = NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"), row.getString("exerciseId"), row.getString("setId"))
                check(PhoneWorkoutExecutionStore.recordedBase(target.resolve(frame).set).toString() ==
                    PhoneWorkoutExecutionStore.recordedBase(target.resolve(incoming).set).toString()) { "Előbb állítsd le az intervallummérést." }
            }
        }
        val bases = JSONObject()
        var completedChanged = false
        val oldExercises = frame.getJSONArray("exercises")
        val nextExercises = incoming.getJSONArray("exercises")
        for (i in 0 until oldExercises.length()) {
            val exercise = oldExercises.getJSONObject(i)
            val sets = exercise.getJSONArray("sets")
            for (j in 0 until sets.length()) {
                val old = sets.getJSONObject(j)
                if (!old.getBoolean("done")) continue
                val id = old.getString("setId")
                val base = PhoneWorkoutExecutionStore.recordedBase(old)
                bases.put(id, base)
                val target = (0 until nextExercises.length()).map { nextExercises.getJSONObject(it) }
                    .filter { it.optString("id") == exercise.optString("id") }.flatMap { e ->
                        val rows = e.getJSONArray("sets")
                        (0 until rows.length()).map { rows.getJSONObject(it) }.filter { it.optString("setId") == id }
                    }.singleOrNull() ?: error("A rögzített sorozat nem távolítható el.")
                if (!target.optBoolean("done")) target.remove("actualRir")
                if (PhoneWorkoutExecutionStore.recordedBase(target).toString() != base.toString()) completedChanged = true
            }
        }
        if (completedChanged) store.amend(incoming, frame.getLong("executionRevision"), bases)
        else store.prepare(incoming, frame.getLong("executionRevision"))
        // If publication fails the edited native frame remains durable. A later
        // projection restores it, instead of replacing it with stale UI fields.
        return project(context)
    }

    fun completeStrengthSet(context: Context, exerciseId: String, setId: String): JSONObject {
        val current = project(context)
        val sets = current.getJSONArray("exercises")
        val target = (0 until sets.length()).map { sets.getJSONObject(it) }
            .filter { it.optString("id") == exerciseId }.flatMap { exercise ->
                val rows = exercise.getJSONArray("sets")
                (0 until rows.length()).map { rows.getJSONObject(it) }.filter { it.optString("setId") == setId }
            }.singleOrNull() ?: error("A sorozat célpontja megváltozott.")
        val command = JSONObject().put("nativeExecutionProtocol", 2).put("action", "completeSet")
            .put("commandId", UUID.randomUUID().toString()).put("workoutId", current.getString("workoutId"))
            .put("ownerEpoch193", current.optLong("ownerEpoch193"))
            .put("generation", current.getLong("generation")).put("executionRevision", current.getLong("executionRevision"))
            .put("exerciseId", exerciseId).put("setId", setId).put("reps", target.getString("reps"))
        if (target.has("weight") && target.optString("weight").isNotBlank()) command.put("weight", target.get("weight"))
        return requireApplied(PhoneWorkoutExecution.execute(context, command))
    }

    fun startStopwatch(context: Context, target: NativeWorkoutTarget, side: String): JSONObject {
        val frame = project(context)
        check(IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation"))) { "A háttéridőzítő még nem áll készen." }
        val timer = IntervalTimerService.snapshot(context)
        check(!timer.optBoolean("running") && (timer.optBoolean("stopped") || timer.optBoolean("interrupted") ||
            timer.optString("key").isEmpty() || timer.optString("phase") == "complete")) { "Előbb fejezd be a futó pihenőt vagy intervallumot." }
        check(!NativeGpsMeasurement.busy(frame)) { "Előbb fejezd be a GPS-mérést." }
        NativeSetStopwatch.start(frame, target, side)
        return try { commitEdit(context, frame, clockEdit = true) }
        finally { IntervalTimerService.trackSetStopwatch(target.workoutId, target.generation) }
    }

    fun completeMeasuredSet(context: Context, target: NativeWorkoutTarget): JSONObject {
        val frame = project(context)
        val set = target.resolve(frame).set
        val command = JSONObject().put("nativeExecutionProtocol", 2).put("action", "completeMeasuredSet")
            .put("commandId", UUID.randomUUID().toString()).put("workoutId", target.workoutId).put("generation", target.generation)
            .put("ownerEpoch193", frame.optLong("ownerEpoch193")).put("executionRevision", frame.getLong("executionRevision"))
            .put("exerciseId", target.exerciseId).put("setId", target.setId)
            .put("measuredValues", PhoneWorkoutExecutionStore.recordedBase(set).toString())
        requireApplied(PhoneWorkoutExecution.execute(context, command))
        return ActiveWorkoutStore.current(context)!!
    }

    fun stopStopwatch(context: Context, target: NativeWorkoutTarget): JSONObject {
        val frame = project(context)
        target.resolve(frame)
        check(!frame.optBoolean("workoutPaused")) { "Az edzés szünetel." }
        val clock = frame.getJSONObject(NativeSetStopwatch.FIELD)
        val command = JSONObject().put("action", "completeTimedSet").put("nativeExecutionProtocol", 2)
            .put("commandId", UUID.randomUUID().toString()).put("workoutId", target.workoutId).put("generation", target.generation)
            .put("executionRevision", frame.getLong("executionRevision")).put("ownerEpoch193", frame.optLong("ownerEpoch193"))
            .put("exerciseId", target.exerciseId).put("setId", target.setId).put("clockId", clock.getString("id"))
        val stopped = NativeSetStopwatch.stopped(frame, command)
        return if (target.resolve(stopped).set.optBoolean("done")) {
            requireApplied(PhoneWorkoutExecution.execute(context, command))
            ActiveWorkoutStore.current(context)!!
        } else {
            // A zero stop or the first positive side is durable, but it is not a completed-set event.
            commitEdit(context, stopped, clockEdit = true)
        }
    }

    fun resetStopwatch(context: Context, target: NativeWorkoutTarget): JSONObject {
        val frame = project(context)
        check(!target.resolve(frame).set.optBoolean("done")) { "A rögzített sorozat nem nullázható." }
        val clock = frame.optJSONObject(NativeSetStopwatch.FIELD)
        if (clock != null) {
            check(clock.optString("setId") == target.setId && clock.optString("exerciseId") == target.exerciseId) { "Másik sorozat mérése van előkészítve." }
            NativeSetStopwatch.pause(frame)
            frame.remove(NativeSetStopwatch.FIELD)
        }
        return commitEdit(context, frame, clockEdit = true)
    }

    fun startGps(context: Context, target: NativeWorkoutTarget): JSONObject {
        var frame = project(context)
        target.resolve(frame)
        if (frame.optJSONObject(NativeGpsMeasurement.FIELD)?.optString("state") == "interrupted") frame = interruptGps(context)
        check(!NativeGpsMeasurement.busy(frame)) { "Már van befejezetlen GPS-mérés." }
        check(IntervalTimerService.armed(target.workoutId, target.generation)) { "A háttéridőzítő még nem áll készen." }
        val timer = IntervalTimerService.snapshot(context)
        check(timer.optString("key").isEmpty() || timer.optBoolean("stopped") || timer.optBoolean("interrupted") ||
            timer.optString("phase") == "complete") { "Előbb fejezd be a pihenőt vagy intervallumot." }
        check(frame.optJSONObject(NativeSetStopwatch.FIELD)?.let { NativeSetStopwatch.running(it) } != true) { "Már fut egy sorozatstopper." }
        check(!DistanceTrackingService.snapshot(context).optBoolean("active")) { "Már fut GPS-mérés." }
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            "A GPS-méréshez engedélyezd a pontos helyadatokat."
        }
        check(context.getSystemService(LocationManager::class.java)?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
            "Kapcsold be a telefon helyszolgáltatását."
        }
        NativeGpsMeasurement.prepare(frame, target)
        NativeSetStopwatch.sealPause(frame)
        frame.remove(NativeSetStopwatch.FIELD)
        val saved = commitEdit(context, frame, gpsEdit = true, clockEdit = true)
        val binding = saved.getJSONObject(NativeGpsMeasurement.FIELD)
        ContextCompat.startForegroundService(context, Intent(context, DistanceTrackingService::class.java).apply {
            action = DistanceTrackingService.START
            putExtra("key", binding.getString("key")); putExtra("measurementId", binding.getString("id"))
            putExtra("name", target.resolve(saved).exercise.optString("name", "TrainPilot")); putExtra("language", saved.optString("language", "hu"))
        })
        return saved
    }

    fun finishGps(context: Context, target: NativeWorkoutTarget, expectedMeasurementId: String): JSONObject {
        var frame = project(context)
        val resolved = target.resolve(frame)
        val binding = frame.getJSONObject(NativeGpsMeasurement.FIELD)
        check(binding.optString("id") == expectedMeasurementId && expectedMeasurementId.isNotEmpty() &&
            binding.optString("setId") == target.setId && binding.optString("exerciseId") == target.exerciseId) { "Másik sorozat GPS-mérése van előkészítve." }
        if (binding.optString("state") == "recorded" && resolved.set.optBoolean("done")) return frame
        check(!frame.optBoolean("workoutPaused")) { "Az edzés szünetel." }
        val live = DistanceTrackingService.snapshot(context)
        if (binding.optString("state") == "prepared" && !(live.optBoolean("active") && live.optString("measurementId") == expectedMeasurementId)) {
            return interruptGps(context) // Cancel a queued foreground-service start before it can run.
        }
        val status = DistanceTrackingService.finishOwned(context, binding.getString("key"), binding.getString("id"))
        check(PhoneWorkoutExecution.store(context).checkpointGps(status)) { "A GPS-mérés állapota időközben megváltozott." }
        frame = project(context)
        if (status.optDouble("distanceMeters") <= 0.0 || status.optLong("elapsedSeconds") <= 0L || status.optString("error").isNotEmpty()) {
            return commitEdit(context, NativeGpsMeasurement.apply(frame, complete = false), gpsEdit = true)
        }
        val command = JSONObject().put("action", "completeGpsSet").put("nativeExecutionProtocol", 2)
            .put("commandId", "gps-finish:" + binding.getString("id")).put("measurementId", binding.getString("id"))
            .put("workoutId", target.workoutId).put("generation", target.generation).put("ownerEpoch193", frame.optLong("ownerEpoch193"))
            .put("executionRevision", frame.getLong("executionRevision")).put("exerciseId", target.exerciseId).put("setId", target.setId)
        requireApplied(PhoneWorkoutExecution.execute(context, command))
        return ActiveWorkoutStore.current(context)!!
    }

    /** Pause/navigation/finish preserve the measured values and private route reference, never completion. */
    fun interruptGps(context: Context): JSONObject {
        var frame = project(context)
        val binding = frame.optJSONObject(NativeGpsMeasurement.FIELD) ?: return frame
        if (binding.optString("state") == "recorded" || binding.optBoolean("applied")) return frame
        val live = DistanceTrackingService.snapshot(context)
        if (live.optString("key") == binding.optString("key") && live.optString("measurementId") == binding.optString("id")) {
            val stopped = DistanceTrackingService.stopOwned(context, binding.getString("key"))
            if (stopped != null) PhoneWorkoutExecution.store(context).checkpointGps(stopped)
            frame = project(context)
        }
        return commitEdit(context, NativeGpsMeasurement.apply(frame, complete = false), gpsEdit = true)
    }

    fun controlRest(context: Context, target: NativeTimerTarget, deltaSeconds: Int? = null): JSONObject {
        val frame = project(context)
        check(target.key.isNotEmpty() && target.workoutId == frame.optString("workoutId") &&
            target.generation == frame.optLong("generation") && !frame.optBoolean("workoutPaused")) { "A pihenő már másik edzéshez tartozik." }
        val timer = IntervalTimerService.snapshot(context)
        check(timer.optString("key") == target.key && timer.optString("workoutId") == target.workoutId &&
            timer.optLong("generation") == target.generation && timer.optString("phase") == "rest" && timer.optBoolean("running") &&
            !timer.optBoolean("stopped") && !timer.optBoolean("interrupted")) { "A pihenő időközben megváltozott." }
        if (deltaSeconds == null) IntervalTimerService.command(context, target.key, "skipRest")
        else IntervalTimerService.adjustRest(context, target, deltaSeconds)
        PhoneWorkoutExecution.publish(context)
        return project(context, PhoneWorkoutExecution.display(context))
    }

    /** Pause only the rendered automatic-rest clock, never the whole workout. */
    fun setRestPaused(context: Context, target: NativeTimerTarget, paused: Boolean): JSONObject {
        val frame = project(context)
        check(target.key.isNotEmpty() && target.workoutId == frame.optString("workoutId") &&
            target.generation == frame.optLong("generation") && !frame.optBoolean("workoutPaused")) {
            "A pihenő már másik edzéshez tartozik."
        }
        val timer = IntervalTimerService.snapshot(context)
        check(timer.optString("key") == target.key && timer.optString("workoutId") == target.workoutId &&
            timer.optLong("generation") == target.generation && timer.optBoolean("restOnly") &&
            timer.optString("phase") == "rest" && !timer.optBoolean("stopped") &&
            !timer.optBoolean("interrupted")) { "A pihenő időközben megváltozott." }
        // Explicit desired state makes repeated/late delivery idempotent.
        if (timer.optBoolean("running") == paused)
            IntervalTimerService.command(context, target.key, if (paused) "pause" else "resume")
        PhoneWorkoutExecution.publish(context)
        return project(context, PhoneWorkoutExecution.display(context))
    }

    fun configureIntervals(context: Context, target: NativeWorkoutTarget, configuration: JSONObject): JSONObject {
        val frame = project(context)
        val resolved = target.resolve(frame)
        val config = NativeIntervalPreset.normalized(configuration)
        frame.put("timerConfig", config).put("restSeconds", config.getInt("rest"))
        resolved.exercise.put("restSeconds162", config.getInt("rest"))
        // Validate even if automatic rest is currently disabled.
        PhoneWorkoutPlans.validateConfig(JSONObject(frame.toString()).put("automaticEnabled", true))
        return commitEdit(context, frame)
    }

    fun startIntervals(context: Context, target: NativeWorkoutTarget): JSONObject {
        var frame = project(context)
        val resolved = target.resolve(frame)
        check(!frame.optBoolean("workoutPaused") && !resolved.set.optBoolean("done") &&
            resolved.exerciseIndex == frame.optInt("currentExercise") && NativeSetStopwatch.timed(resolved.exercise) &&
            !NativeSetStopwatch.bilateral(resolved.exercise)) { "Az intervallummérés egyoldali időalapú sorozathoz indítható." }
        check(!NativeGpsMeasurement.busy(frame) && frame.optJSONObject(NativeSetStopwatch.FIELD)?.let { NativeSetStopwatch.running(it) } != true) {
            "Előbb állítsd le a másik mérést."
        }
        if (frame.has(NativeSetStopwatch.FIELD)) {
            frame.remove(NativeSetStopwatch.FIELD)
            frame = commitEdit(context, frame, clockEdit = true)
        }
        requireApplied(PhoneWorkoutExecution.execute(context, JSONObject().put("nativeExecutionProtocol", 2)
            .put("action", "automaticControl").put("value", "start").put("commandId", UUID.randomUUID().toString())
            .put("workoutId", target.workoutId).put("generation", target.generation).put("executionRevision", frame.getLong("executionRevision"))
            .put("ownerEpoch193", frame.optLong("ownerEpoch193")).put("exerciseId", target.exerciseId).put("setId", target.setId)))
        return project(context, PhoneWorkoutExecution.display(context))
    }

    fun stopIntervals(context: Context, target: NativeTimerTarget): JSONObject {
        val frame = project(context)
        check(target.workoutId == frame.optString("workoutId") && target.generation == frame.optLong("generation") && target.key.isNotEmpty()) { "Az időzítő megváltozott." }
        var status = IntervalTimerService.snapshot(context)
        check(status.optString("key") == target.key && status.optString("workoutId") == target.workoutId &&
            status.optLong("generation") == target.generation && !status.optBoolean("restOnly")) { "Az intervallum már másik méréshez tartozik." }
        if (!status.optBoolean("stopped") && !status.optBoolean("interrupted")) status = IntervalTimerService.command(context, target.key, "stop")
        PhoneWorkoutExecution.store(context).checkpointTimer(status, SystemClock.elapsedRealtime())
        PhoneWorkoutExecution.store(context).retireNativeInterval(target)
        PhoneWorkoutExecution.publish(context)
        return project(context)
    }

    fun selectExercise(context: Context, target: NativeWorkoutTarget): JSONObject {
        var frame = project(context)
        target.resolve(frame)
        if (frame.optJSONObject(NativeGpsMeasurement.FIELD)?.let { it.optString("state") != "recorded" && !it.optBoolean("applied") } == true) {
            frame = interruptGps(context)
        }
        NativeSetStopwatch.sealPause(frame)
        NativeSetStopwatch.pause(frame, trackPause = false)
        frame.put("currentExercise", target.resolve(frame).exerciseIndex)
            .put("selectionRevision", frame.optLong("selectionRevision") + 1L)
        // The native rest plan and its ledger remain owned by the foreground controller.
        return commitEdit(context, frame, clockEdit = true)
    }

    fun undo(context: Context, recordId: String): JSONObject {
        val current = project(context)
        val record = current.optJSONObject("lastRecord188") ?: error("Nincs visszavonható sorozat.")
        check(record.optString("recordId") == recordId) { "A visszavonható sorozat megváltozott." }
        val command = JSONObject(record.toString()).put("schema", 1).put("nativeExecutionProtocol", 2)
            .put("action", "undoSet").put("commandId", UUID.randomUUID().toString())
            .put("workoutId", current.getString("workoutId")).put("generation", current.getLong("generation"))
            .put("ownerEpoch193", current.optLong("ownerEpoch193"))
            .put("executionRevision", current.getLong("executionRevision")).put("createdAt", System.currentTimeMillis())
        return requireApplied(PhoneWorkoutExecution.execute(context, command))
    }

    private fun requireApplied(result: JSONObject): JSONObject {
        check(result.optString("status") in setOf("applied", "duplicate")) {
            when (val reason = result.optString("reason", result.optString("status"))) {
                "invalidMeasuredTime" -> "Pozitív egész másodpercet adj meg (legfeljebb 24 óra); kétoldali sorozatnál mindkét oldal szükséges."
                "invalidDistance" -> "A távolsági sorozathoz pozitív idő és távolság is szükséges."
                "measurementChanged" -> "A mért értékek időközben megváltoztak. Ellenőrizd és próbáld újra."
                else -> "A natív művelet nem alkalmazható: $reason"
            }
        }
        return result
    }

    private fun requireEditable(frame: JSONObject, current: JSONObject?) {
        check(frame.optInt("nativeEditorProjection") == 1) { "A natív szerkesztő állapota még nincs előkészítve." }
        check(current == null || current.optBoolean("active") || current.optString("workoutId") != frame.optString("workoutId")) {
            "A lezárt edzés nem indítható újra."
        }
        check(frame.optBoolean("active") && frame.optString("timerOwner") == "phone" && !WorkoutOwnership193.pending(frame)) {
            "A telefon már nem vezérli ezt az edzést."
        }
        if (current?.optBoolean("active") == true) check(current.optString("workoutId") == frame.optString("workoutId") &&
            current.optString("timerOwner", "phone") == "phone" && !current.optBoolean("finalizationPending") && !WorkoutOwnership193.pending(current)) {
            "Az edzés tulajdonosa vagy lezárási állapota megváltozott."
        }
    }
}
