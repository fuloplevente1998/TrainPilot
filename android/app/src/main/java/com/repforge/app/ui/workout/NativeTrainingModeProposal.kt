package com.repforge.app.ui.workout

import android.content.Context
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.NativeWorkoutEditing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.repforge.app.NativeWorkoutStartValues
import com.repforge.app.NativeGpsMeasurement
import com.repforge.app.NativeSetStopwatch
import com.repforge.shared.WorkoutOwnership193

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
        else reps.isBlank() && weight.toDoubleOrNull() == 0.0
        // An unmarked nonzero prescription/prefill cannot prove the user left weight untouched.
    }

    fun eligible(frame: JSONObject, exerciseId: String, proposedWeight: Double): Boolean {
        if (!frame.optBoolean("active") || frame.optBoolean("workoutPaused") ||
            frame.optBoolean("finalizationPending") || frame.optBoolean("ownershipAwaitingResume193") ||
            WorkoutOwnership193.pending(frame) ||
            frame.optJSONObject("automatic162")?.optBoolean("pending") == true ||
            NativeGpsMeasurement.busy(frame) ||
            frame.optJSONObject(NativeSetStopwatch.FIELD)?.let(NativeSetStopwatch::running) == true ||
            frame.optJSONObject("latestTimer162")?.let {
                !it.optBoolean("stopped", false) && !it.optBoolean("interrupted", false) &&
                    it.optString("phase") != "complete"
            } == true ||
            frame.optString("timerOwner") != "phone" ||
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

    /**
     * The VM completes its history/readiness checks under an admitted metadata
     * lease before calling this. Main-only canonical execution is deliberately
     * centralized so a real IO-entry instrumentation test can exercise it.
     * No suspensions occur between the final frame admission and commitEdit.
     */
    suspend fun commitApprovedOnMain(context: Context, ticket: Ticket,
        currentSettingsRaw: suspend () -> String?): JSONObject? =
        withContext(Dispatchers.Main.immediate) {
            val freshSettings = currentSettingsRaw()
            val frame = ActiveWorkoutStore.current(context)
            if (freshSettings != ticket.settingsRaw || frame == null || !permitted(frame, ticket)) null
            else NativeWorkoutEditing.commitEdit(context, editedFrame(frame, ticket))
        }

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
