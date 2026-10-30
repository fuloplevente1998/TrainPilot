package com.repforge.app

import android.content.Context
import android.os.SystemClock
import androidx.room.withTransaction
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.shared.WorkoutOwnership193
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** One finalization path for the phone UI and durable watch finish commands. */
internal class TrainPilotWorkoutFinalization(
    private val context: Context,
    private val database: TrainPilotDatabase = TrainPilotDatabase.getInstance(context)
) {
    companion object { private val mutex = Mutex() }

    data class Saved(val history: JSONObject, val closedSnapshot: JSONObject?)

    /** Confirmed phone discard shares the finish lock; never acknowledge before native close. */
    suspend fun discardWorkout(input: JSONObject): JSONObject = withContext(NonCancellable) {
        NativeDataReplacement.mutate(context) {
            mutex.withLock {
                val id = input.getString("workoutId").also { require(it.isNotBlank()) }
                val frozen = withContext(Dispatchers.Main.immediate) {
                    val current = requireNotNull(ActiveWorkoutStore.current(context))
                    check(current.optBoolean("active") && current.optString("workoutId") == id &&
                        current.optLong("generation") == input.optLong("generation") &&
                        current.optString("timerOwner", "phone") == "phone" && !WorkoutOwnership193.pending(current)) {
                        "Az edzés tulajdonosa vagy állapota megváltozott."
                    }
                    check(!current.optBoolean("finalizationPending") || current.optBoolean("discardPending")) {
                        "A mentés alatt álló edzés nem vethető el."
                    }
                    val execution = PhoneWorkoutExecution.store(context)
                    check(!execution.restoreBlocked()) { "Adat-visszaállítás van folyamatban." }
                    val frame = execution.snapshot()
                    check(frame == null || (frame.optString("workoutId") == id &&
                        frame.optLong("generation") == current.optLong("generation"))) { "A natív edzés megváltozott." }
                    check(!execution.commandsSuspended() || current.optBoolean("discardPending")) { "Az edzésvezérlés foglalt." }
                    // The existing durable finish fence already blocks edits/resume/handoff. The
                    // discriminator permits only explicit discard retry after a storage/process failure.
                    val pending = JSONObject(current.toString()).put("finalizationPending", true)
                        .put("discardPending", true).put("workoutPaused", true).put("restEndAt", 0L)
                    ActiveWorkoutStore.saveFinalizing(context, pending)
                    execution.suspendCommands(true)
                    if (frame?.optBoolean("active") == true) {
                        execution.setWorkoutPause(JSONObject(frame.toString()).put("workoutPaused", true)
                            .put("pauseRevision", maxOf(System.currentTimeMillis(), frame.optLong("pauseRevision") + 1L)),
                            SystemClock.elapsedRealtime(), IntervalTimerService.snapshot(context).optBoolean("running"))
                    }
                    IntervalTimerService.stopForOwnership(context, id)
                    DistanceTrackingService.stop(context)
                    pending
                }
                withContext(Dispatchers.IO) {
                    database.withTransaction {
                        check(database.workoutHistoryDao().getWorkoutWithDetails(id) == null) {
                            "A már naplózott edzés nem vethető el aktív edzésként."
                        }
                        database.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(
                            primaryId = id, workoutKey = id, idsJson = JSONArray().put(id).toString(),
                            deletedAt = System.currentTimeMillis()))
                        NativeLegacyDraft.retire(database, frozen.optString("legacyDraftSha256"))
                    }
                }
                val closed = withContext(Dispatchers.Main.immediate) {
                    val execution = PhoneWorkoutExecution.store(context)
                    val frame = execution.snapshot()
                    if (frame != null) {
                        check(frame.optString("workoutId") == id && frame.optLong("generation") == frozen.optLong("generation")) {
                            "A natív edzés megváltozott."
                        }
                        check(execution.discardExecution(id, frame.getLong("generation"), frame.getLong("executionRevision"))) {
                            "A natív elvetés nem sikerült. Próbáld újra."
                        }
                        IntervalTimerService.closeWorkout(id, frame.getLong("generation"))
                    }
                    check(ActiveWorkoutStore.current(context)?.let {
                        it.optString("workoutId") == id && it.optBoolean("discardPending")
                    } == true) { "Az elvetendő edzés megváltozott." }
                    // Clear the command flag while the durable discard draft still permits retry.
                    // The native frame is already inactive, so it cannot accept another recording.
                    execution.suspendCommands(false)
                    ActiveWorkoutStore.clear(context)
                }
                // Route cleanup is after durable deletion. Failure must not resurrect the workout.
                withContext(Dispatchers.IO) {
                    runCatching { com.repforge.shared.GpsRouteLedger193.open(context.noBackupFilesDir).deleteWorkout(id) }
                }
                closed
            }
        }
    }

    suspend fun finishWorkout(input: JSONObject, fromWatch: Boolean = false): Saved =
        withContext(NonCancellable) {
            NativeDataReplacement.mutate(context) {
            mutex.withLock {
                val id = input.getString("workoutId").also { require(it.isNotBlank()) }
                val history = withContext(Dispatchers.Main.immediate) { freezeAndReconcile(input, fromWatch) }
                // History and calendar are one Room transaction. A calendar/storage failure
                // leaves the durable pending draft available, and emits no saved receipt.
                withContext(Dispatchers.IO) {
                    database.withTransaction {
                        check(!database.workoutHistoryDao().isTombstoned(id, JSONObject.quote(id))) {
                            "A törölt edzés nem menthető újra."
                        }
                        val existing = database.workoutHistoryDao().getWorkoutWithDetails(id)
                        if (existing != null) history.put("id", existing.workout.id).put("finished", existing.workout.finished)
                        PersonalRecordCalculator226.evaluateAndAttachPrs(history, database)
                        LegacyWebViewMigrator(context, database, TrainPilotSettingsStore(context)).saveWorkoutJsonToRoom(history)
                        check(database.workoutHistoryDao().getWorkoutWithDetails(id) != null) { "A törölt vagy hibás edzés nem menthető újra." }
                        val scheduleId = history.optString("scheduleId")
                        if (scheduleId.isNotBlank()) database.calendarScheduleDao().getEntry(scheduleId)?.let {
                            database.calendarScheduleDao().upsertEntry(it.copy(status = "completed", updatedAt = System.currentTimeMillis()))
                        }
                    }
                }
                // Only remove the exact imported source after history is durable; native pending frame remains retryable.
                val importedHash = history.optString("legacyDraftSha256")
                if (importedHash.isNotEmpty()) withContext(Dispatchers.IO) {
                    NativeLegacyDraft.retire(database, importedHash)
                }
                val closed = withContext(Dispatchers.Main.immediate) {
                    val execution = PhoneWorkoutExecution.store(context)
                    val frame = execution.snapshot()
                    if (frame != null && frame.optString("workoutId") == id) {
                        check(execution.pendingEvents(id, frame.getLong("generation"), 0L).length() == 0) { "Függő sorozatrögzítés maradt." }
                        check(execution.closeExecution(id, frame.getLong("generation"), frame.getLong("executionRevision"))) {
                            "A natív edzéslezárás nem sikerült. Próbáld újra."
                        }
                        IntervalTimerService.closeWorkout(id, frame.getLong("generation"))
                    }
                    val current = ActiveWorkoutStore.current(context)
                    val result = if (current?.optBoolean("active") == true && current.optString("workoutId") == id) {
                        ActiveWorkoutStore.clear(context)
                    } else null
                    if (frame?.optString("workoutId") == id) execution.suspendCommands(false)
                    result
                }
                Saved(history, closed)
            }
            }
        }

    private fun freezeAndReconcile(input: JSONObject, fromWatch: Boolean): JSONObject {
        val id = input.getString("workoutId")
        var current = ActiveWorkoutStore.current(context)
        check(!input.optBoolean("discardPending") && current?.takeIf { it.optString("workoutId") == id }
            ?.optBoolean("discardPending") != true) { "Az elvetést kell befejezni; az edzés már nem menthető. Próbáld újra az elvetést." }
        val ownsDraft = current?.optBoolean("active") == true && current.optString("workoutId") == id
        check(!WorkoutOwnership193.pending(input) && (!ownsDraft || !WorkoutOwnership193.pending(current))) {
            "Előbb fejezd be az edzés átadását vagy visszavételét."
        }
        if (!fromWatch) {
            check(ownsDraft && current!!.optString("timerOwner", "phone") == "phone") { "A telefonos edzés tulajdonosa megváltozott." }
        } else if (ownsDraft) {
            check(current!!.optString("timerOwner") == "watch") { "Az órás edzés tulajdonosa megváltozott." }
        }
        if (ownsDraft && !fromWatch && current?.optBoolean("finalizationPending") != true &&
            PhoneWorkoutExecution.store(context).snapshot()?.optInt("nativeEditorProjection") == 1) {
            NativeWorkoutEditing.interruptGps(context)
            current = ActiveWorkoutStore.current(context)
        }
        var draft = JSONObject(if (ownsDraft && !fromWatch) current.toString() else input.toString())
        if (!fromWatch && input.has("feedback")) draft.put("feedback", input.get("feedback"))
        // Retain the first finish timestamp across failed retries.
        val finished = current?.takeIf { ownsDraft && it.optBoolean("finalizationPending") }?.optString("finished")
            ?.takeIf { it.isNotBlank() } ?: draft.optString("finished").takeIf { it.isNotBlank() } ?: Instant.now().toString()
        draft.put("id", draft.optString("id").ifBlank { id }).put("syncId", id).put("finished", finished)
            .put("finalizationPending", true).put("workoutPaused", true).put("restEndAt", 0L)
        val intervals = draft.optJSONArray("activeIntervals")
        if (intervals != null) for (i in 0 until intervals.length()) intervals.optJSONObject(i)?.let {
            if (it.isNull("end") || it.optString("end").isBlank()) it.put("end", finished)
        }
        val execution = PhoneWorkoutExecution.store(context)
        check(!execution.restoreBlocked()) { "Adat-visszaállítás van folyamatban." }
        var frame = execution.snapshot()?.takeIf { it.optString("workoutId") == id }
        if (frame != null) {
            check(!execution.commandsSuspended() || current?.optBoolean("finalizationPending") == true) { "Az edzésvezérlés foglalt." }
            if (draft.has("generation")) check(draft.getLong("generation") == frame.getLong("generation")) { "Az edzés generációja megváltozott." }
        }
        if (ownsDraft) ActiveWorkoutStore.saveFinalizing(context, draft)
        if (frame != null) {
            execution.suspendCommands(true)
            if (frame.optBoolean("active")) {
                val paused = JSONObject(frame.toString()).put("workoutPaused", true)
                    .put("pauseRevision", maxOf(System.currentTimeMillis(), frame.optLong("pauseRevision") + 1L))
                if (intervals != null) paused.put("activeIntervals", intervals)
                execution.setWorkoutPause(paused, SystemClock.elapsedRealtime(), IntervalTimerService.snapshot(context).optBoolean("running"))
            }
            IntervalTimerService.stopForOwnership(context, id)
            frame = execution.snapshot()!!
            while (true) {
                val events = execution.pendingEvents(id, frame.getLong("generation"), 0L)
                draft = NativeWorkoutEventMerge.merge(draft, frame, events)
                // The event cursor and values are committed together before acknowledgement.
                // A detached, old watch finish may not replace a newer active workout.
                if (ownsDraft) ActiveWorkoutStore.saveFinalizing(context, draft)
                else check(events.length() == 0) { "A függő natív edzéshez hiányzik a helyreállítható draft." }
                if (events.length() == 0) break
                check(execution.acknowledgeEvents(id, frame.getLong("generation"), events.getJSONObject(events.length() - 1).getLong("sequence"))) {
                    "A sorozatrögzítés nyugtázása nem sikerült."
                }
            }
        }
        if (ownsDraft && frame?.optInt("nativeEditorProjection") != 1) DistanceTrackingService.stop(context)
        val history = JSONObject(draft.toString())
        history.remove("finalizationPending")
        history.put("active", false)
        return history
    }
}

/** Stable-ID event reconciliation, based on accepted main TrainPilotPhoneExecution162Core.merge. */
internal object NativeWorkoutEventMerge {
    fun merge(input: JSONObject, frame: JSONObject, events: JSONArray): JSONObject {
        val next = JSONObject(input.toString())
        val id = next.getString("workoutId")
        check(frame.getString("workoutId") == id) { "Native event identity mismatch" }
        val generation = frame.getLong("generation")
        val meta = next.optJSONObject("phoneExecution162") ?: JSONObject().put("generation", generation).put("sequence", 0)
        check(meta.getLong("generation") == generation) { "Native event generation mismatch" }
        var cursor = meta.optLong("sequence")
        for (i in 0 until events.length()) {
            val event = events.getJSONObject(i)
            check(event.getString("workoutId") == id && event.getLong("generation") == generation && event.getLong("sequence") > 0) {
                "Native event identity mismatch"
            }
            if (event.getLong("sequence") <= cursor) continue
            when (event.optString("kind")) {
                "startTimeline" -> Unit
                "timedSets" -> {
                    val changes = event.getJSONArray("changes")
                    for (j in 0 until changes.length()) applyChange(next, changes.getJSONObject(j), false)
                }
                "undoSet" -> applyChange(next, event, true)
                else -> applyChange(next, event, false)
            }
            cursor = event.getLong("sequence")
        }
        meta.put("sequence", cursor).put("revision", frame.getLong("executionRevision"))
        next.put("phoneExecution162", meta).put("executionRevision", frame.getLong("executionRevision"))
        for (key in listOf("undo188", "lastRecord188", "timeLogs162", "latestTimer162")) {
            if (frame.has(key)) next.put(key, frame.get(key)) else if (key == "lastRecord188") next.remove(key)
        }
        mergeTime(next, frame.optJSONObject("timeLogs162") ?: JSONObject())
        return next
    }

    private fun applyChange(next: JSONObject, event: JSONObject, undo: Boolean) {
        val exercises = next.getJSONArray("exercises")
        val targets = (0 until exercises.length()).map { exercises.getJSONObject(it) }
            .filter { it.optString("id") == event.getString("exerciseId") }.flatMap { exercise ->
                val sets = exercise.getJSONArray("sets")
                (0 until sets.length()).map { sets.getJSONObject(it) }.filter { it.optString("setId") == event.getString("setId") }
            }
        check(targets.size == 1) { "Native recording target changed" }
        val set = targets.single()
        val values = event.getJSONObject("values")
        check(values.opt("done") == !undo) { "Invalid native completion" }
        val before = event.optJSONObject("before")
        if (before != null) for (field in listOf("reps", "weight", "leftSeconds", "rightSeconds", "distanceMeters")) {
            if (!values.has(field)) continue
            val local = normalized(set.opt(field), field)
            check(local == normalized(before.opt(field), field) || local == normalized(values.opt(field), field)) {
                "Recorded set has conflicting edits"
            }
        }
        for (key in values.keys()) set.put(key, values.get(key))
        if (undo) set.remove("actualRir")
    }

    private fun normalized(value: Any?, field: String): String {
        val text = if (value == null || value === JSONObject.NULL) "" else value.toString()
        return if (field != "reps") (text.replace(',', '.').toDoubleOrNull() ?: 0.0).toString() else text
    }

    internal fun mergeTime(session: JSONObject, native: JSONObject) {
        val fields = listOf("workMs", "restMs", "pausedMs", "positionMs")
        val automatic = session.optJSONObject("automaticTime162") ?: JSONObject().put("schema", 1)
        val logs = automatic.optJSONObject("logs") ?: JSONObject()
        for (key in native.keys()) {
            val log = native.getJSONObject(key)
            check(log.optInt("schema") == 1 && log.optString("key") == key && log.optString("owner") in setOf("phone", "watch", "browser")) { "Invalid automatic time log" }
            for (field in fields) {
                val value = log.opt(field)
                check(value is Number && value.toDouble() == value.toLong().toDouble() && value.toLong() in 0..604800000L) { "Invalid automatic time log" }
            }
            val old = logs.optJSONObject(key)
            if (old != null) {
                check(old.optString("owner") == log.optString("owner")) { "Automatic time owner changed" }
                if (fields.any { log.getLong(it) < old.getLong(it) }) {
                    check(fields.all { log.getLong(it) <= old.getLong(it) }) { "Automatic time moved backwards" }
                    continue
                }
            }
            logs.put(key, log)
        }
        automatic.put("logs", logs)
        for (field in fields.take(3)) automatic.put(field, logs.keys().asSequence().sumOf { logs.getJSONObject(it).getLong(field) })
        session.put("automaticTime162", automatic)
    }
}
