package com.repforge.app.ui.coach

import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.data.store.NativeTrainingModes
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

internal data class CoachExerciseAdvice(val id: String, val name: String, val advice: NativeCoachExerciseAdvice.Advice,
    val loadType: String = "", val modePreview: NativeTrainingModeProgression.Preview? = null,
    val sourceFingerprint: String = "")

/** Bounded canonical Room reads; no full-history relations/cache or second workout state. */
internal class NativeCoachAdviceRepository(private val db: TrainPilotDatabase) {
    suspend fun load(target: CoachTarget?, settings: TrainPilotUserSettings,
        readiness: NativeCoachReadiness.Result, now: Instant): List<CoachExerciseAdvice> {
        if (target == null) return emptyList()
        val program = db.programDao().getProgramWithDaysById(target.programId) ?: return emptyList()
        val day = program.days.firstOrNull { it.dayId == target.dayId } ?: return emptyList()
        val raw = JSONObject(program.program.rawJson)
        val recipes = raw.optJSONObject("prescriptions") ?: JSONObject()
        val selectedMode = NativeTrainingModes.resolveSavedJson(settings.trainingModes197Json, target.programId)
        val ids = JSONArray(day.exercisesJson)
        return (0 until ids.length()).map { index ->
            val item = ids.get(index)
            val id = if (item is JSONObject) item.getString("id") else item as String
            val library = requireNotNull(db.exerciseLibraryDao().getExerciseById(id)) { "Missing exercise: $id" }
            val rx = JSONObject(recipes.optJSONObject(id)?.toString() ?: "{}")
            if (item is JSONObject) for (key in item.keys()) if (key != "id") rx.put(key, item.get(key))
            val lib = JSONObject(library.rawJson).put("id", id).put("loadType", library.loadType)
                .put("repUnit", library.repUnit).put("measurementType", library.measurementType)
                .put("reps", library.defaultReps).put("sets", library.defaultSets).put("weight", library.defaultWeight)
                .put("custom", library.custom)
            val prescription = NativeCoachExerciseAdvice.target(lib, rx)
            val rows = recent(id, target.programId, lib, rx, raw, now)
            val occurrences = rows.map { NativeCoachExerciseAdvice.occurrence(it.first, it.second, prescription) }
            val base = occurrences.firstOrNull()?.weight ?: 0.0
            val load = rx.optString("loadType", library.loadType)
            val merged = JSONObject(lib.toString()).put("loadType", load)
            val observed = if (load == "bodyweight") null else observedStep(id, load)
            val step = NativeCoachExerciseAdvice.step(merged, base, settings.progressionSteps, observed)
            // Explicit #197 mode proposals use the same canonical history/target/step.
            // Unselected legacy keeps its existing unmodified recommendation motor.
            val preview = NativeTrainingModeProgression.compute(selectedMode, merged,
                prescription, occurrences, readiness, step)
            val recommendation = if (preview == null) NativeCoachExerciseAdvice.recommend(
                lib, rx, occurrences, prescription, readiness, step)
            else NativeCoachExerciseAdvice.Advice(
                action = if (preview.action == "variation") "hold" else preview.action,
                reason = "mode197:${preview.trainingStyle}:${preview.reason}",
                weight = preview.proposedWeight, baseWeight = preview.baselineWeight,
                autoApply = false, historyCount = preview.historyCount, target = prescription,
                nextReps = preview.nextReps
            )
            val lastRaw = db.workoutHistoryDao().getCoachExerciseCandidates(id, Long.MIN_VALUE, Long.MAX_VALUE, null, "", 1).firstOrNull()
                ?.let { JSONObject(it.rawJson).optJSONArray("exercises") }
            val lastEffort = (0 until (lastRaw?.length() ?: 0)).mapNotNull { lastRaw?.optJSONObject(it) }
                .firstOrNull { it.optString("id") == id }?.optString("effort")
            val name = when (settings.language) {
                "en" -> library.nameEn; "de" -> library.nameDe; "ro" -> library.nameRo; else -> library.nameHu
            }.ifBlank { library.nameHu }
            CoachExerciseAdvice(id, name, NativeCoachExerciseAdvice.forCoach(recommendation, lastEffort, readiness),
                load, preview, buildString {
                    append(raw.toString()); append("\\u001f")
                    append(lib.toString()); append("\\u001f")
                    append(rx.toString()); append("\\u001f")
                    append(step); append("\\u001f")
                    append(settings.progressionSteps.toSortedMap().toString()); append("\\u001f")
                    rows.forEach { (workout, exercise) ->
                        append(workout.toString()); append("\\u001e"); append(exercise.toString()); append("\\u001f")
                    }
                    append(readiness.toString()); append("\\u001f"); append(settings.trainingModes197Json)
                })
        }
    }

    internal suspend fun recent(id: String, programId: String, lib: JSONObject, rx: JSONObject,
        program: JSONObject, now: Instant): List<Pair<JSONObject, JSONObject>> {
        val selection = Selection(programId, lib, rx, program, now)
        val found = ArrayList<Pair<JSONObject, JSONObject>>()
        var before: Long? = null; var beforeId = ""
        while (found.size < 4) {
            val page = db.workoutHistoryDao().getCoachExerciseCandidates(id, now.minus(Duration.ofDays(90)).toEpochMilli(),
                now.toEpochMilli(), before, beforeId, 32)
            for (row in page) {
                selection.accept(JSONObject(row.rawJson))?.let { found.add(it) }
                if (found.size == 4) break
            }
            if (page.size < 32) break
            before = page.last().finishedEpochMs; beforeId = page.last().id
        }
        return found
    }
    private suspend fun observedStep(id: String, load: String): Double? {
        var after = -1.0; var previous: Double? = null; var smallest: Double? = null
        while (true) {
            val page = db.workoutHistoryDao().getCoachObservedWeights(id, load, after, 128)
            for (weight in page) {
                if (!weight.isFinite()) continue
                previous?.let {
                    val diff = kotlin.math.round((weight - it) * 100) / 100
                    if (diff in .25..10.0) smallest = minOf(smallest ?: diff, diff)
                }
                previous = weight
            }
            if (page.size < 128) break
            after = page.last()
        }
        return smallest
    }
    /** Streaming selection is shared with reference tests and bounded database pages. */
    internal class Selection(private val programId: String, private val lib: JSONObject, private val rx: JSONObject,
        program: JSONObject, private val now: Instant) {
        private val inherited = inheritedSources(lib, rx, program.optJSONObject("history128")
            ?.optJSONObject("entries")?.optJSONObject(lib.optString("id")))
        private val seen = HashSet<Pair<String, String>>()
        fun accept(workout: JSONObject): Pair<JSONObject, JSONObject>? {
            val stamp = workout.optString("finished")
            val finished = runCatching { Instant.parse(stamp) }.getOrNull()
                ?: runCatching { java.time.LocalDateTime.parse(stamp).atZone(java.time.ZoneId.systemDefault()).toInstant() }.getOrNull()
                ?: runCatching { java.time.LocalDate.parse(stamp).atStartOfDay(java.time.ZoneId.of("UTC")).toInstant() }.getOrNull()
                ?: return null
            if (finished > now || finished < now.minus(Duration.ofDays(90))) return null
            val pid = workout.optString("programId").ifBlank { "home-basic" }
            val key = workout.optString("id").ifBlank { workout.optString("started") }
            val identity = pid to key
            if (identity in seen || pid != programId && identity !in inherited) return null
            val exercises = workout.optJSONArray("exercises") ?: return null
            val exercise = (0 until exercises.length()).mapNotNull { exercises.optJSONObject(it) }
                .firstOrNull { compatible(it, lib, rx) } ?: return null
            seen.add(identity)
            return workout to exercise
        }
    }
    companion object {
        private fun metric(lib: JSONObject, unit: String = lib.optString("repUnit")) =
            if (Regex("mp|sec|second").containsMatchIn(unit)) "time" else "reps"
        internal fun compatible(exercise: JSONObject, lib: JSONObject, rx: JSONObject): Boolean {
            if (exercise.optString("id") != lib.optString("id") || (exercise.optJSONArray("sets")?.length() ?: 0) == 0) return false
            val load = exercise.optString("loadType").ifBlank { if (lib.optBoolean("custom")) "" else lib.optString("loadType") }
            return load == rx.optString("loadType", lib.optString("loadType")) &&
                exercise.optJSONObject("prescription")?.optString("variation").orEmpty() == rx.optString("variation") &&
                metric(lib, exercise.optString("repUnit").ifBlank { lib.optString("repUnit") }) == metric(lib)
        }
        private fun inheritedSources(lib: JSONObject, rx: JSONObject, basis: JSONObject?): Set<Pair<String, String>> {
            if (basis == null || basis.optString("loadType") != rx.optString("loadType", lib.optString("loadType")) ||
                basis.optString("variation") != rx.optString("variation") || basis.optString("metric") != metric(lib)) return emptySet()
            val sources = basis.optJSONArray("sources") ?: return emptySet()
            return (0 until sources.length()).mapNotNull { sources.optJSONObject(it)?.let { source ->
                source.optString("programId") to source.optString("key") } }.toSet()
        }
    }
}
