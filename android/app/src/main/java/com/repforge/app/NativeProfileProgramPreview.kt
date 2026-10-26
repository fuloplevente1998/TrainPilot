package com.repforge.app

import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.ProgramWithDays
import org.json.JSONArray
import org.json.JSONObject

/** Read-only #128 A/B preview. No invented weights, history evidence or automatic increases. */
internal object NativeProfileProgramPreview {
    data class Reduction(val dayId: String, val exerciseId: String, val reason: String, val cause: String = "")
    data class Preview(
        val source: ProgramWithDays,
        val draft: NativeProgramEditor.Draft,
        val prescriptions: JSONObject,
        val profileSnapshot: JSONObject,
        val reductions: List<Reduction>,
        val estimateMinutes: Int,
        val workSets: Int,
        val sourceProfileJson: String,
        val history128: JSONObject = JSONObject(),
        val language: String = "hu"
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

    private fun available(ex: ExerciseLibraryEntity, gear: Set<String>) = NativePlannerPolicy.available(ex, gear)

    internal fun variationRejection(ex: ExerciseLibraryEntity, profile: JSONObject): String = when {
        ex.id in keys(profile, "excluded") -> "excluded"
        NativePlannerPolicy.avoided(ex, avoidedMuscles(keys(profile, "avoidAreas"))) -> "avoid-area"
        profile.optString("experience") == "beginner" && !NativePlannerPolicy.metadata(ex).optBoolean("beginnerSafe", true) -> "experience"
        !available(ex, keys(profile, "gear")) -> "equipment"
        else -> ""
    }

    fun preview(source: ProgramWithDays, rawProfile: String,
        catalog: List<ExerciseLibraryEntity>, language: String, deferHistoryLimits: Boolean = false,
        practicedIds: Set<String> = emptySet()): Preview {
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
        // Final main validateProfile rejects unknown exclusion IDs before generating a plan.
        // Never silently ignore a typo/removed exercise and prescribe a substitute instead.
        profile.optJSONArray("excluded")?.let { entries ->
            require((0 until entries.length()).all { catalogById.containsKey(entries.getString(it)) }) {
                "Invalid exercise exclusion"
            }
        }
        fun exclusionReason(id: String, ex: ExerciseLibraryEntity?): String = when {
            ex == null -> "missing"
            id in excluded -> "excluded"
            NativePlannerPolicy.avoided(ex, areas) -> "avoid-area"
            beginner && !NativePlannerPolicy.metadata(ex).optBoolean("beginnerSafe", true) && id !in practicedIds -> "experience"
            !available(ex, gear) -> "equipment"
            else -> ""
        }
        val count = NativePlannerPolicy.capacity(minutes)
        val level2 = source.program.id == "home-level2" && profile.optString("experience") == "intermediate"
        val repetitions = JSONObject()
        val removed = ArrayList<Reduction>()
        var totalSets = 0
        val eligible = catalog.filter { exclusionReason(it.id, it).isBlank() }
        val pools = mutableMapOf<String, List<ExerciseLibraryEntity>>()
        val draftDays = NativePlannerPolicy.slots(source.program.id).map { (dayId, slotIds) ->
            val day = source.days.firstOrNull { it.dayId == dayId }
            val filtered = ArrayList<ExerciseLibraryEntity>()
            for (id in slotIds) {
                val original = catalogById[id]
                var chosen = eligible.firstOrNull { it.id == id && filtered.none { old -> old.id == id } }
                val reason = exclusionReason(id, original)
                if (chosen == null) {
                    val role = original?.let(NativePlannerPolicy::role)
                    chosen = role?.let { pools.getOrPut(it) { NativePlannerPolicy.pool(it, eligible, profile) } }
                        ?.firstOrNull { candidate -> filtered.none { it.id == candidate.id } }
                }
                if (chosen?.id != id) removed += Reduction(dayId, id,
                    if (chosen == null) reason.ifBlank { "missing" } else "substitution:${chosen.id}", reason)
                if (chosen != null) filtered += chosen
            }
            require(filtered.size >= 3) { "Fewer than three eligible exercises for day $dayId" }
            val finalRows = filtered.sortedByDescending { NativePlannerPolicy.metadata(it).optBoolean("compound") }
                .map { it.id to it }
            val selected = if (deferHistoryLimits) finalRows else finalRows.take(count)
            if (!deferHistoryLimits) for ((id, _) in finalRows.drop(count)) removed += Reduction(dayId, id, "time")
            for ((id, exercise) in selected) {
                val original = NativePlannerPolicy.metadata(exercise)
                original.put("id", id).put("repUnit", exercise.repUnit).put("loadType", exercise.loadType)
                val rx = if (level2) NativeTrainingPrescription.level2(original, profile)
                    else NativeTrainingPrescription.create(original, profile)
                totalSets += rx.getInt("sets")
                repetitions.put(id, rx)
            }
            NativeProgramEditor.DayDraft(dayId,
                when (language) {
                    "en" -> day?.nameEn; "de" -> day?.nameDe
                    "ro" -> day?.nameRo; else -> day?.nameHu
                }.orEmpty().ifBlank { dayId }, selected.map { JSONObject.quote(it.first) })
        }
        require(draftDays.isNotEmpty() && draftDays.all { it.entries.isNotEmpty() }) {
            "No safe A/B day for the selected equipment and exclusions"
        }
        val name = when (language) {
            "en" -> source.program.nameEn; "de" -> source.program.nameDe
            "ro" -> source.program.nameRo; else -> source.program.nameHu
        }.ifBlank { source.program.nameHu }
        val result = Preview(source,
            NativeProgramEditor.Draft(name, source.program.description, draftDays),
            repetitions, profile, removed, minutes, totalSets, rawProfile, language = language)
        return if (deferHistoryLimits) result else fitTime(result)
    }

    private fun dayMinutes(day: NativeProgramEditor.DayDraft, recipes: JSONObject): Double =
        4.0 + day.entries.sumOf { encoded ->
            val rx = recipes.getJSONObject(NativeProgramEditor.exerciseId(encoded))
            val sets = rx.getInt("sets")
            sets * .65 + (sets - 1) * rx.getDouble("rest") / 60.0 + 1.0
        }

    fun estimatedMinutes(days: List<NativeProgramEditor.DayDraft>, recipes: JSONObject): Int =
        kotlin.math.ceil(days.maxOf { dayMinutes(it, recipes) }).toInt()

    /** Main tp128AdaptHistory reduces working-set count, never the already earned load. */
    fun fitTime(preview: Preview): Preview {
        val recipes = JSONObject(preview.prescriptions.toString())
        val budget = preview.profileSnapshot.getInt("minutes")
        val reductions = preview.reductions.toMutableList()
        for (day in preview.draft.days) {
            for (encoded in day.entries.asReversed()) {
                val id = NativeProgramEditor.exerciseId(encoded)
                val rx = recipes.getJSONObject(id)
                if (dayMinutes(day, recipes) > budget && rx.getInt("sets") > 2) {
                    rx.put("sets", 2)
                    reductions += Reduction(day.id, id, "time-sets")
                }
            }
            require(dayMinutes(day, recipes) <= budget) { "The minimum safe workout does not fit the requested time" }
        }
        return preview.copy(prescriptions = recipes, reductions = reductions.distinct(),
            estimateMinutes = estimatedMinutes(preview.draft.days, recipes),
            workSets = preview.draft.days.sumOf { day -> day.entries.sumOf { recipes.getJSONObject(NativeProgramEditor.exerciseId(it)).getInt("sets") } })
    }

    fun metadata(preview: Preview): JSONObject = JSONObject()
        .put("profileSnapshot", JSONObject(preview.profileSnapshot.toString()))
        .put("prescriptions", JSONObject(preview.prescriptions.toString()))
        .put("estimatedMinutes", preview.estimateMinutes)
        .put("templateVersion", 2)
        .put("sourceTemplateId", preview.source.program.id)
        .also { if (preview.history128.has("entries")) it.put("history128", JSONObject(preview.history128.toString())) }
}
