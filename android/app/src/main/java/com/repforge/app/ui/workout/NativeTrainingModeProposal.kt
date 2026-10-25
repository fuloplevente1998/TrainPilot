package com.repforge.app.ui.workout

import org.json.JSONObject
import com.repforge.app.NativeWorkoutStartValues

/** Pure admission policy. The actual write uses NativeWorkoutEditing.commitEdit CAS. */
internal object NativeTrainingModeProposal {
    data class Ticket(val workoutId: String, val generation: Long,
        val executionRevision: Long, val ownerEpoch: Long, val exerciseId: String,
        val sourceFingerprint: String, val settingsRaw: String?, val approvedWeight: Double,
        val originalFrame: String)

    private fun editableSet(set: JSONObject): Boolean {
        if (set.optBoolean("done") || set.has("completedAt") || set.has("gpsRouteKey193") ||
            set.has("gpsRouteKey163") || set.has("distanceMeters") ||
            set.optInt("leftSeconds") > 0 || set.optInt("rightSeconds") > 0 ||
            set.has("actualRir")) return false
        val original = set.optJSONObject("previousValues228")
        val reps = set.opt("reps")?.toString().orEmpty()
        val weight = set.opt("weight")?.toString().orEmpty()
        return if (original != null)
            original.opt("reps")?.toString().orEmpty() == reps &&
                original.opt("weight")?.toString().orEmpty() == weight
        else reps.isBlank() // do not silently overwrite a manually entered rep target
    }

    fun eligible(frame: JSONObject, exerciseId: String, proposedWeight: Double): Boolean {
        if (!frame.optBoolean("active") || frame.optBoolean("workoutPaused") ||
            frame.optBoolean("finalizationPending") || frame.optBoolean("ownershipAwaitingResume193") ||
            frame.optBoolean("automatic162") || frame.optString("timerOwner") != "phone" ||
            frame.optInt("nativeEditorProjection") != 1 || frame.optString("workoutId").isBlank() ||
            frame.optString("programId").isBlank() || !proposedWeight.isFinite() ||
            proposedWeight <= 0 || proposedWeight > 10000) return false
        val selected = frame.optJSONArray("exercises") ?: return false
        val matching = (0 until selected.length()).mapNotNull { selected.optJSONObject(it) }
            .filter { it.optString("id") == exerciseId }
        if (matching.size != 1) return false
        val ex = matching.single()
        if (!NativeWorkoutStartValues.strength(ex) || ex.optString("loadType") == "bodyweight") return false
        val sets = ex.optJSONArray("sets") ?: return false
        if (sets.length() == 0) return false
        if ((0 until sets.length()).any { index -> sets.optJSONObject(index)?.let(::editableSet) != true }) return false
        // This proposal changes weight only, never sets/reps/done/timers/GPS ownership.
        return true
    }

    fun ticket(frame: JSONObject, exerciseId: String, weight: Double,
        fingerprint: String, settingsRaw: String?): Ticket? {
        if (!eligible(frame, exerciseId, weight)) return null
        return Ticket(frame.optString("workoutId"), frame.optLong("generation"),
            frame.optLong("executionRevision"), frame.optLong("ownerEpoch193"),
            exerciseId, fingerprint, settingsRaw, weight, frame.toString())
    }

    fun permitted(current: JSONObject, ticket: Ticket): Boolean =
        current.optString("workoutId") == ticket.workoutId &&
            current.optLong("generation") == ticket.generation &&
            current.optLong("executionRevision") == ticket.executionRevision &&
            current.optLong("ownerEpoch193") == ticket.ownerEpoch &&
            current.toString() == ticket.originalFrame &&
            eligible(current, ticket.exerciseId, ticket.approvedWeight)

    fun editedFrame(current: JSONObject, ticket: Ticket): JSONObject {
        check(permitted(current, ticket)) { "Stale Coach proposal" }
        val next = JSONObject(current.toString())
        val rows = next.getJSONArray("exercises")
        val exercise = (0 until rows.length()).map { rows.getJSONObject(it) }
            .single { it.optString("id") == ticket.exerciseId }
        val sets = exercise.getJSONArray("sets")
        for (index in 0 until sets.length()) sets.getJSONObject(index).put("weight", ticket.approvedWeight)
        return next
    }
}
