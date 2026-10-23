package com.repforge.app

import android.content.Context
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

    fun commitEdit(context: Context, edit: JSONObject): JSONObject {
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
            "A natív művelet nem alkalmazható: ${result.optString("reason", result.optString("status"))}"
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
