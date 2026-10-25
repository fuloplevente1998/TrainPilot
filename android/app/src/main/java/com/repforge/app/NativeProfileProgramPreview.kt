package com.repforge.app

import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.ProgramWithDays
import org.json.JSONArray
import org.json.JSONObject

/** Read-only #128 A/B preview. No invented weights, history evidence or automatic increases. */
internal object NativeProfileProgramPreview {
    data class Reduction(val dayId: String, val exerciseId: String, val reason: String)
    data class Preview(
        val source: ProgramWithDays,
        val draft: NativeProgramEditor.Draft,
        val prescriptions: JSONObject,
        val profileSnapshot: JSONObject,
        val reductions: List<Reduction>,
        val estimateMinutes: Int,
        val workSets: Int,
        val sourceProfileJson: String,
        val history128: JSONObject = JSONObject()
    )

    private fun keys(json: JSONObject, name: String): Set<String> {
        val rows = json.optJSONArray(name) ?: return emptySet()
        return (0 until rows.length()).mapNotNull { rows.optString(it).takeIf(String::isNotBlank) }.toSet()
    }

    private fun avoidedMuscles(areas: Set<String>): Set<String> {
        val muscles = setOf("chest", "back", "shoulders", "arms", "core", "legs")
        return areas.map { raw ->
            val normalized = raw.lowercase().replace('_', '-')
            when {
                normalized in muscles -> normalized
                "shoulder" in normalized || "váll" in normalized -> "shoulders"
                "knee" in normalized || "térd" in normalized -> "legs"
                "elbow" in normalized || "könyök" in normalized -> "arms"
                "lumbar" in normalized || "derék" in normalized -> "back"
                else -> throw IllegalArgumentException("Unrecognized area: $raw")
            }
        }.toSet()
    }

    private fun available(ex: ExerciseLibraryEntity, gear: Set<String>): Boolean {
        val raw = JSONObject(ex.rawJson)
        if (ex.loadType == "bodyweight") {
            val equipment = raw.optString("equipment").lowercase()
            return !("pad" in equipment || "támasz" in equipment) ||
                "bench" in gear || "support" in gear
        }
        val required = raw.optString("gear")
        if (required.isNotBlank()) return when (required) {
            "bodyweight" -> true
            "dumbbells", "dumbbell" -> "dumbbells" in gear
            "barbell" -> "barbell" in gear
            "kettlebell" -> "kettlebell" in gear
            "bands" -> "bands" in gear
            else -> required in gear
        }
        val eq = raw.optString("equipment").lowercase()
        return when {
            "kézisúlyzó" in eq -> "dumbbells" in gear
            "kétkezes rúd" in eq -> "barbell" in gear
            "kettlebell" in eq -> "kettlebell" in gear
            "gumiszalag" in eq -> "bands" in gear
            "testsúly" in eq -> true
            else -> false // Unknown equipment is never silently assumed available.
        }
    }

    internal fun variationRejection(ex: ExerciseLibraryEntity, profile: JSONObject): String = when {
        ex.id in keys(profile, "excluded") -> "excluded"
        ex.primaryMuscle in avoidedMuscles(keys(profile, "avoidAreas")) -> "avoid-area"
        profile.optString("experience") == "beginner" && !JSONObject(ex.rawJson).optBoolean("beginnerSafe", true) -> "experience"
        !available(ex, keys(profile, "gear")) -> "equipment"
        else -> ""
    }

    fun preview(source: ProgramWithDays, rawProfile: String,
        catalog: List<ExerciseLibraryEntity>, language: String): Preview {
        require(source.program.builtin && source.program.id in setOf("home-basic", "home-level2")) {
            "Only an accepted A/B template can produce this preview"
        }
        val profile = NativeTrainingProfile.verified(JSONObject(rawProfile))
        require(profile.has("minutes") && profile.optInt("minutes") in 5..300) {
            "Set workout duration in Training profile first"
        }
        require(profile.optJSONArray("gear") != null) {
            "Set available equipment in Training profile first"
        }
        val minutes = profile.getInt("minutes")
        val gear = keys(profile, "gear")
        val excluded = keys(profile, "excluded")
        val areas = avoidedMuscles(keys(profile, "avoidAreas"))
        val beginner = profile.optString("experience") == "beginner"
        val catalogById = catalog.associateBy { it.id }
        fun exclusionReason(id: String, ex: ExerciseLibraryEntity?): String = when {
            ex == null -> "missing"
            id in excluded -> "excluded"
            ex.primaryMuscle in areas -> "avoid-area"
            beginner && !JSONObject(ex.rawJson).optBoolean("beginnerSafe", true) -> "experience"
            !available(ex, gear) -> "equipment"
            else -> ""
        }
        val count = (minutes / 6).coerceIn(1, 7)
        val level2 = source.program.id == "home-level2" && profile.optString("experience") == "intermediate"
        val repetitions = JSONObject()
        val removed = ArrayList<Reduction>()
        var totalSets = 0
        val draftDays = source.days.sortedBy { it.sortOrder }.map { day ->
            val ids = JSONArray(day.exercisesJson)
            val filtered = ArrayList<Pair<String, ExerciseLibraryEntity>>()
            for (index in 0 until ids.length()) {
                val row = ids.get(index)
                val id = if (row is JSONObject) row.optString("id") else row as? String ?: ""
                val ex = catalogById[id]
                val reason = exclusionReason(id, ex)
                if (reason.isNotBlank()) removed += Reduction(day.dayId, id, reason)
                else filtered += id to ex!!
            }
            // Main #128 B day: compound order before isolation, push-up third.
            // Level 2 may use decline push-up only when the user explicitly has a bench.
            val finalRows = if (day.dayId == "B") {
                val variation = catalogById["decline-pushup"]
                val index = filtered.indexOfFirst { it.first == "pushup" }
                if (source.program.id == "home-level2" && index >= 0 &&
                    "pushup" !in excluded && "decline-pushup" !in excluded &&
                    variation != null && exclusionReason("decline-pushup", variation).isEmpty()) {
                    filtered[index] = "decline-pushup" to variation
                }
                val order = listOf("bulgarian-split-squat", "barbell-row",
                    "decline-pushup", "pushup", "db-pullover",
                    "lateral-raise", "oh-triceps", "dead-bug")
                filtered.sortedWith(compareBy<Pair<String, ExerciseLibraryEntity>> {
                    order.indexOf(it.first).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE
                })
            } else filtered
            val selected = finalRows.take(count)
            for ((id, _) in finalRows.drop(count)) removed += Reduction(day.dayId, id, "time")
            for ((id, exercise) in selected) {
                val original = JSONObject(exercise.rawJson)
                original.put("id", id).put("repUnit", exercise.repUnit).put("loadType", exercise.loadType)
                val rx = if (level2) NativeTrainingPrescription.level2(original, profile)
                    else NativeTrainingPrescription.create(original, profile)
                totalSets += rx.getInt("sets")
                repetitions.put(id, rx)
            }
            NativeProgramEditor.DayDraft(day.dayId,
                when (language) {
                    "en" -> day.nameEn; "de" -> day.nameDe
                    "ro" -> day.nameRo; else -> day.nameHu
                }.ifBlank { day.nameHu }, selected.map { JSONObject.quote(it.first) })
        }
        require(draftDays.isNotEmpty() && draftDays.all { it.entries.isNotEmpty() }) {
            "No safe A/B day for the selected equipment and exclusions"
        }
        val name = when (language) {
            "en" -> source.program.nameEn; "de" -> source.program.nameDe
            "ro" -> source.program.nameRo; else -> source.program.nameHu
        }.ifBlank { source.program.nameHu }
        return Preview(source,
            NativeProgramEditor.Draft(name, source.program.description, draftDays),
            repetitions, profile, removed, minutes, totalSets, rawProfile)
    }

    fun metadata(preview: Preview): JSONObject = JSONObject()
        .put("profileSnapshot", JSONObject(preview.profileSnapshot.toString()))
        .put("prescriptions", JSONObject(preview.prescriptions.toString()))
        .put("templateVersion", 2)
        .put("sourceTemplateId", preview.source.program.id)
        .also { if (preview.history128.has("entries")) it.put("history128", JSONObject(preview.history128.toString())) }
}
