package com.repforge.app.wear

import org.json.JSONObject

/** Persist the request separately from the device's fallback ExerciseType. */
internal data class WearRecordingRequest(val exerciseType: String, val gps: Boolean) {
    fun save(data: JSONObject) {
        data.put("recordingExerciseType", exerciseType).put("recordingGps", gps)
    }

    companion object {
        fun restore(data: JSONObject, exerciseType: String?, gps: Boolean?): WearRecordingRequest {
            val legacyType = when (data.optString("exerciseType")) {
                "RUNNING" -> "running"
                "WALKING" -> "walking"
                "BIKING" -> "cycling"
                else -> "strength"
            }
            return WearRecordingRequest(
                data.optString("recordingExerciseType").ifBlank { if(data.has("exerciseType"))legacyType else exerciseType ?: legacyType },
                if (data.has("recordingGps")) data.optBoolean("recordingGps") else if(data.has("gps"))data.optBoolean("gps") else gps ?: false
            )
        }
    }
}

/** A stop survives process death; only an owned exercise may be ended. */
internal class WearRecordingLifecycle(private val data: JSONObject) {
    var owned = false
        private set
    var ending = false
        private set
    var finished = false
        private set
    val stopRequested: Boolean get() = data.optBoolean("stopRequested") || data.optString("state") == "ending"

    fun requestStop() {
        if (!finished) data.put("stopRequested", true).put("state", "ending")
    }

    fun confirmOwnership() { owned = true }

    fun beginEnding(): Boolean {
        if (!owned || ending || finished) return false
        requestStop()
        ending = true
        return true
    }

    fun finish(): Boolean {
        if (finished) return false
        finished = true
        data.put("state", "ended")
        return true
    }
}

internal data class WearMeasuredMetric(val label: String, val value: String)

internal fun wearMeasuredTotals(data: JSONObject?, workoutId: String): List<WearMeasuredMetric> {
    if (data == null || data.optString("workoutId") != workoutId) return emptyList()
    return listOf(
        Triple("Átlagpulzus", "averageHeartRate", "bpm"),
        Triple("Max. pulzus", "maxHeartRate", "bpm"),
        Triple("Összes energia", "totalCalories", "kcal"),
        Triple("Lépések", "steps", "lépés"),
        Triple("Távolság", "distanceMeters", "m")
    ).mapNotNull { (label, key, unit) ->
        val number = if (data.has(key) && !data.isNull(key)) data.optDouble(key) else Double.NaN
        if (!number.isFinite() || number < 0) null
        else WearMeasuredMetric(label, "${trimWearNumber(number)} $unit")
    }
}

internal fun trimWearNumber(number: Double): String =
    if (number >= 100 || number % 1.0 == 0.0) number.toLong().toString()
    else String.format(java.util.Locale.forLanguageTag("hu"), "%.1f", number)

internal fun wearRecordingStatus(data: JSONObject?, enabled: Boolean): String = when (data?.optString("state")) {
    "starting" -> "Mérés indítása…"
    "active" -> "Mérés folyamatban · háttérben is"
    "ending" -> "Mérés lezárása…"
    "ended" -> "Mérés befejezve"
    "error" -> "A mérés megszakadt"
    else -> if (enabled) "Mérés a következő edzésnél" else "Mérés kikapcsolva"
}
