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
    init {
        // A cached active/paused summary is not proof that a recovered service owns
        // a running Health Services exercise. Keep totals, recheck ownership first.
        if(!stopRequested && data.optString("state") !in listOf("ended","error"))data.put("state","starting")
    }

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

/** Choose a session type that can supply movement metrics for cardio workouts. */
internal fun wearRecordingRequestForWorkout(workout: WearWorkout, gps: Boolean): WearRecordingRequest {
    fun unit(exercise: WearExercise) = exercise.repUnit.trim().lowercase(java.util.Locale.ROOT)
    fun steps(exercise: WearExercise) = exercise.measurementType == "steps" ||
        unit(exercise) in listOf("lépés", "lépések", "step", "steps", "schritte", "pași")
    val cardio = workout.exercises.firstOrNull {
        steps(it) || it.measurementType == "distance" || unit(it) in listOf("m", "km") ||
            it.id.lowercase(java.util.Locale.ROOT) in listOf("running", "walking", "cycling")
    } ?: return WearRecordingRequest("strength", false)
    val name = (cardio.id + " " + cardio.name).lowercase(java.util.Locale.ROOT)
    val type = when {
        steps(cardio) -> "walking"
        name.contains("kerék") || name.contains("cycl") || name.contains("bic") -> "cycling"
        name.contains("séta") || name.contains("walk") || name.contains("gyalog") -> "walking"
        else -> "running"
    }
    return WearRecordingRequest(type, gps)
}

/** Compact workout rows show measured session totals, separate from set targets. */
internal fun wearLiveWorkoutRows(data: JSONObject?, workoutId: String, now: Long): List<String> {
    if (data == null || workoutId.isBlank() || data.optString("workoutId") != workoutId ||
        data.optString("source") != "wear_health_services") return emptyList()
    fun value(key: String, unit: String, minimum: Double = 0.0): String? {
        val number = if (data.has(key) && !data.isNull(key)) data.optDouble(key) else Double.NaN
        return if (!number.isFinite() || number < minimum) null else "${trimWearNumber(number)} $unit"
    }
    val primary = listOfNotNull(value("heartRate", "bpm", 20.0), value("totalCalories", "kcal"))
    val movement = listOfNotNull(value("steps", "lépés"), value("distanceMeters", "m"))
    if (primary.isEmpty() && movement.isEmpty()) return emptyList()
    val status = when (data.optString("state")) {
        "paused" -> "Mérés szünetel"
        "active" -> if (now - data.optLong("updatedAt") > 20000L) "Legutóbbi adat" else null
        "starting" -> "Mérés indul"
        "ending" -> "Mérés lezárása"
        "error" -> "Mérés megszakadt"
        else -> "Legutóbbi adat"
    }
    return listOfNotNull(
        primary.takeIf { it.isNotEmpty() }?.joinToString(" · "),
        movement.takeIf { it.isNotEmpty() }?.joinToString(" · ")?.let { "Edzés: $it" },
        status
    )
}

/** Select only this workout; never borrow the previous workout's live values. */
internal fun wearHealthDisplaySummary(workoutId: String, stored: JSONObject?, published: JSONObject?): JSONObject? {
    fun matching(data: JSONObject?): JSONObject? = data?.takeIf {
        workoutId.isNotBlank() && it.optString("workoutId")==workoutId &&
            it.optString("source")=="wear_health_services" && it.optLong("revision")>0
    }
    val durable=matching(stored)
    val live=published?.takeIf { it.optString("workoutId")==workoutId }?.optJSONObject("summary")?.let(::matching)
    val selected=when {
        durable==null -> live
        live==null || live.optString("watchId")!=durable.optString("watchId") -> durable
        live.optLong("revision")>durable.optLong("revision") -> live
        else -> durable
    }
    return selected?.let { WearHealthAccumulator(it).snapshot() }
}

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

internal fun wearRecordingNeedsRetry(data: JSONObject?, enabled: Boolean, hasWorkout: Boolean): Boolean =
    enabled && hasWorkout && data?.optString("state") !in listOf("starting","active","paused","ending")

internal fun wearRecordingStatus(data: JSONObject?, enabled: Boolean, hasWorkout: Boolean=false): String = when (data?.optString("state")) {
    "starting" -> "Mérés indítása…"
    "active" -> "Mérés folyamatban · háttérben is"
    "paused" -> "Az órás mérés szünetel"
    "ending" -> "Mérés lezárása…"
    "ended" -> "Mérés befejezve"
    "error" -> "A mérés megszakadt"
    else -> if (!enabled) "Mérés kikapcsolva" else if(hasWorkout) "Az aktuális edzés mérése még nem indult" else "Mérés a következő edzésnél"
}
