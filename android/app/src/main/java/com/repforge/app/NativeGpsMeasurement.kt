package com.repforge.app

import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.round

/** Coordinates stay in GpsRouteStore. Only the bound capture summary enters the execution frame. */
internal object NativeGpsMeasurement {
    const val FIELD = "gpsMeasurement"
    private val openStates = setOf("prepared", "running", "explicitStopped")

    fun distance(exercise: JSONObject) = exercise.optString("measurementType") == "distance" ||
        exercise.optString("repUnit").trim().lowercase() in setOf("m", "km")

    fun busy(frame: JSONObject): Boolean {
        val binding = frame.optJSONObject(FIELD) ?: return false
        return binding.optString("state") in openStates || (binding.optString("state") == "interrupted" && !binding.optBoolean("applied"))
    }

    fun target(frame: JSONObject, binding: JSONObject = frame.getJSONObject(FIELD)) = NativeWorkoutTarget(
        binding.getString("workoutId"), binding.getLong("generation"), binding.getString("exerciseId"), binding.getString("setId"))

    fun prepare(frame: JSONObject, target: NativeWorkoutTarget) {
        val resolved = target.resolve(frame)
        check(!frame.optBoolean("workoutPaused") && !resolved.set.optBoolean("done") && distance(resolved.exercise)) {
            "Ehhez a sorozathoz nem indítható GPS-mérés."
        }
        check(!busy(frame)) { "Előbb fejezd be vagy szakítsd meg az előző GPS-mérést." }
        val oldKey = resolved.set.optString("gpsRouteKey163")
        val key = oldKey.takeIf { GpsRouteStore.validKey(it) } ?: ("phone|" + MessageDigest.getInstance("SHA-256")
            .digest((target.workoutId + "\u0000" + target.setId).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) })
        val meters = resolved.set.optDouble("distanceMeters", 0.0)
        val seconds = resolved.set.optString("reps").toDoubleOrNull() ?: 0.0
        require(meters.isFinite() && meters in 0.0..2_000_000.0 && seconds.isFinite() && seconds in 0.0..86_400.0)
        frame.put(FIELD, JSONObject().put("id", UUID.randomUUID().toString()).put("key", key)
            .put("workoutId", target.workoutId).put("generation", target.generation).put("ownerEpoch193", frame.optLong("ownerEpoch193"))
            .put("exerciseId", target.exerciseId).put("setId", target.setId).put("state", "prepared")
            .put("before", PhoneWorkoutExecutionStore.recordedBase(resolved.set)).put("baseMeters", meters).put("baseSeconds", seconds))
    }

    /** Reject old sessions even when a continued set reuses the same private route file. */
    fun capture(frame: JSONObject, status: JSONObject): Boolean {
        val binding = frame.optJSONObject(FIELD) ?: return false
        if (!busy(frame) || binding.optLong("ownerEpoch193") != frame.optLong("ownerEpoch193") || status.optString("key") != binding.optString("key") ||
            status.optString("measurementId") != binding.optString("id") ||
            status.optString("nativeWorkoutId") != binding.optString("workoutId") ||
            status.optLong("nativeGeneration") != binding.optLong("generation")) return false
        target(frame, binding).resolve(frame)
        val meters = status.optDouble("distanceMeters", Double.NaN)
        val seconds = status.optLong("elapsedSeconds", -1L)
        if (!meters.isFinite() || meters !in 0.0..2_000_000.0 || seconds !in 0L..86_400L) return false
        val previous = binding.optJSONObject("capture")
        if (previous != null && (meters < previous.optDouble("distanceMeters") || seconds < previous.optLong("elapsedSeconds") ||
            (!previous.optBoolean("active") && status.optBoolean("active")))) return false
        // Explicit stop is terminal for this measurement ID; delayed ticks cannot revoke it.
        if (previous?.optString("stopReason") == "explicit" && status.optString("stopReason") != "explicit") return false
        val safe = JSONObject()
        for (key in listOf("key", "measurementId", "nativeWorkoutId", "nativeGeneration", "active", "paused", "distanceMeters", "elapsedSeconds",
            "stopReason", "error", "routeSavedPoints", "routeSaveError", "routeTruncated")) if (status.has(key)) safe.put(key, status.get(key))
        binding.put("capture", safe).put("state", if (status.optBoolean("active")) "running"
            else if (status.optString("stopReason") == "explicit") "explicitStopped" else "interrupted")
        NativeMeasuredTime.gps(frame, safe)
        return true
    }

    fun apply(frame: JSONObject, complete: Boolean): JSONObject {
        val next = JSONObject(frame.toString())
        val binding = next.getJSONObject(FIELD)
        if (!complete && (binding.optString("state") == "recorded" || binding.optBoolean("applied"))) return next
        if (complete) check(binding.optString("state") == "explicitStopped" && !binding.optBoolean("applied")) { "A mérés nem fejezhető be automatikusan." }
        val target = target(next, binding).resolve(next)
        check(distance(target.exercise) && !target.set.optBoolean("done")) { "A GPS-mérés célpontja megváltozott." }
        check(PhoneWorkoutExecutionStore.recordedBase(target.set).toString() == binding.getJSONObject("before").toString()) {
            "A GPS-mérés sorozatát időközben módosították."
        }
        val capture = binding.optJSONObject("capture")
        if (complete) {
            check(capture != null && capture.opt("active") == false && capture.optString("stopReason") == "explicit" &&
                capture.optString("error").isEmpty() && capture.optDouble("distanceMeters") > 0.0 && capture.optLong("elapsedSeconds") > 0L) {
                "Csak sikeres, pozitív GPS-mérés fejezhető be automatikusan."
            }
        }
        if (capture != null) {
            val meters = (binding.getDouble("baseMeters") + capture.getDouble("distanceMeters")).coerceAtMost(2_000_000.0)
            val seconds = (binding.getDouble("baseSeconds") + capture.getLong("elapsedSeconds")).coerceAtMost(86_400.0)
            if (meters > 0.0 || seconds > 0.0) target.set.put("distanceMeters", round(meters * 10.0) / 10.0)
                .put("reps", seconds.toLong().toString()).put("distanceSource", "gps")
            if (capture.optInt("routeSavedPoints") > 0) target.set.put("gpsRouteKey163", binding.getString("key"))
        }
        binding.put("state", if (complete) "recorded" else "interrupted").put("applied", true)
        if (complete) target.set.put("done", true)
        return next
    }

    fun record(frame: JSONObject, command: JSONObject): JSONObject {
        val binding = frame.getJSONObject(FIELD)
        check(binding.optString("id") == command.optString("measurementId") &&
            binding.optString("exerciseId") == command.optString("exerciseId") && binding.optString("setId") == command.optString("setId")) {
            "A GPS-mérés azonosítója megváltozott."
        }
        val next = apply(frame, complete = true)
        next.put("currentExercise", target(next).resolve(next).exerciseIndex)
            .put("executionRevision", PhoneWorkoutRecording.advance(frame.getLong("executionRevision")))
        return next
    }
}
