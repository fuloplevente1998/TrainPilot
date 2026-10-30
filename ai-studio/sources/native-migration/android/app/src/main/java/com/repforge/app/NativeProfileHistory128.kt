package com.repforge.app

import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryEntity
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.ui.coach.NativeCoachAdviceRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.Duration
import java.security.MessageDigest
import kotlin.math.abs

/**
 * Explicit #128 history lineage. It is used for a new-program preview only;
 * journal sets and previous programs are never rewritten.
 */
internal object NativeProfileHistory128 {
    private const val PAGE = 32
    private const val MAX_PAGES = 20
    private const val MAX_SOURCES = 4

    /** Main-compatible backup guard; legacy programs without history128 remain valid. */
    fun validatePortable(program: JSONObject) {
        if (!program.has("history128")) return
        val history = program.optJSONObject("history128")
            ?: throw IllegalArgumentException("Invalid history128")
        val entries = history.optJSONObject("entries")
            ?: throw IllegalArgumentException("Missing history128 entries")
        require(entries.length() <= 200) { "Too many history128 entries" }
        NativeProfileDayHistory128.validate(history)
        history.opt("practiced")?.let { value ->
            val practiced = value as? JSONObject ?: throw IllegalArgumentException("Invalid practiced evidence")
            require(practiced.length() <= 200)
            for (id in practiced.keys()) {
                require(id.isNotBlank() && id.length <= 200)
                val proof = practiced.getJSONObject(id)
                for (field in listOf("programId", "key")) {
                    val identity = proof.opt(field) as? String ?: throw IllegalArgumentException("Invalid practiced identity")
                    require(identity.isNotBlank() && identity.length <= 200)
                }
                require(Regex("[0-9a-f]{64}").matches(proof.optString("evidenceHash")))
            }
        }
        for (id in entries.keys()) {
            require(id.isNotBlank() && id.length <= 200)
            val basis = entries.optJSONObject(id)
                ?: throw IllegalArgumentException("Invalid history128 exercise")
            if (basis.has("metric")) require(basis.optString("metric") in setOf("time", "reps"))
            val sources = basis.optJSONArray("sources")
                ?: throw IllegalArgumentException("Missing history128 sources")
            require(sources.length() <= MAX_SOURCES)
            val unique = HashSet<Pair<String, String>>()
            for (i in 0 until sources.length()) {
                val row = sources.optJSONObject(i)
                    ?: throw IllegalArgumentException("Invalid history128 source")
                val pid = row.optString("programId")
                val key = row.optString("key")
                require(pid.isNotBlank() && key.isNotBlank() &&
                    pid.length <= 200 && key.length <= 200 &&
                    unique.add(pid to key)) { "Duplicate or invalid history128 source" }
            }
        }
    }

    private fun metric(unit: String) =
        if (Regex("mp|sec|second", RegexOption.IGNORE_CASE).containsMatchIn(unit)) "time" else "reps"

    private fun exerciseIds(day: String): Set<String> {
        val a = JSONArray(day)
        return (0 until a.length()).mapNotNull { index ->
            when (val row = a.get(index)) {
                is String -> row
                is JSONObject -> row.optString("id")
                else -> ""
            }.takeIf(String::isNotBlank)
        }.toSet()
    }

    /** Do not infer compatible provenance from any unrelated program containing one exercise. */
    private suspend fun compatiblePrograms(db: TrainPilotDatabase, sourceId: String): Map<String, Map<String, Set<String>>> {
        val all = db.programDao().getProgramsWithDays()
        val source = all.firstOrNull { it.program.id == sourceId } ?: return emptyMap()
        val original = source.days.associate { it.dayId to exerciseIds(it.exercisesJson) }
        return all.mapNotNull { program ->
            val byDay = program.days.associate { it.dayId to exerciseIds(it.exercisesJson) }
            val ab = original.keys.containsAll(listOf("A", "B")) && byDay.keys.containsAll(listOf("A", "B"))
            val overlaps = listOf("A", "B").map { day -> original[day].orEmpty().intersect(byDay[day].orEmpty()).size }
            if (program.program.id == sourceId || (ab && overlaps.all { it >= 2 } && overlaps.sum() >= 7))
                program.program.id to byDay
            else null
        }.toMap()
    }

    private fun digest(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    internal data class Evidence(val programId: String, val key: String, val load: Double,
        val sets: Int, val topReps: Int, val raw: String)

    /** Full valid working sets only. Unknown/partial/pain/changed unit or variation fails closed. */
    internal fun evidence(row: WorkoutHistoryEntity, exerciseId: String,
        loadType: String, repUnit: String, variation: String): Evidence? {
        val raw = runCatching { JSONObject(row.rawJson) }.getOrNull() ?: return null
        if (raw.optString("programId") != row.programId ||
            raw.optString("id").isBlank() || row.finishedEpochMs <= row.startedEpochMs) return null
        if (raw.optJSONObject("feedback")?.optString("rating") == "pain" ||
            raw.optJSONObject("feedback")?.optString("effort") == "pain") return null
        val a = raw.optJSONArray("exercises") ?: return null
        for (i in 0 until a.length()) {
            val exercise = a.optJSONObject(i) ?: continue
            if (exercise.optString("id") != exerciseId) continue
            // Share the final native Coach's unit/variation/metric comparison,
            // rather than treating a similarly named movement as transferable.
            val expectedLib = JSONObject().put("id", exerciseId).put("loadType", loadType)
                .put("repUnit", repUnit).put("custom", false)
            val expectedRx = JSONObject().put("loadType", loadType).put("variation", variation)
            if (!NativeCoachAdviceRepository.compatible(exercise, expectedLib, expectedRx)) return null
            val sets = exercise.optJSONArray("sets") ?: return null
            if (sets.length() == 0 || sets.length() > 20 ||
                exercise.optString("effort") in setOf("pain", "hard", "challenging")) return null
            val observedLoadType = exercise.optString("loadType")
            if (observedLoadType != loadType && observedLoadType.isNotBlank()) return null
            val observedUnit = exercise.optString("repUnit").ifBlank { repUnit }
            if (metric(observedUnit) != metric(repUnit)) return null
            val observedVariation = exercise.optJSONObject("prescription")?.optString("variation").orEmpty()
            if (observedVariation != variation) return null
            val loads = ArrayList<Double>()
            val reps = ArrayList<Int>()
            for (j in 0 until sets.length()) {
                val set = sets.optJSONObject(j) ?: return null
                if (!set.optBoolean("done")) return null
                val count = set.optString("reps").trim().toIntOrNull() ?: return null
                if (count <= 0 || count > 500) return null
                val load = set.opt("weight") as? Number ?: return null
                val kg = load.toDouble()
                if (!kg.isFinite() || (loadType != "bodyweight" && kg <= 0.0) || kg < 0) return null
                loads.add(kg); reps.add(count)
            }
            if (loadType != "bodyweight" && loads.any { abs(it - loads[0]) >= .01 }) return null
            val key = raw.getString("id")
            return Evidence(row.programId, key, loads.first(), sets.length(), reps.max(), raw.toString())
        }
        return null
    }

    private fun ceiling(original: String, newReps: Int): String {
        val match = Regex("^(\\d+)\\s*[-–]\\s*(\\d+)").find(original.trim()) ?: return original
        val lower = match.groupValues[1].toInt()
        val old = match.groupValues[2].toInt()
        if (newReps <= old || newReps > 100) return original
        return "$lower–$newReps"
    }

    /** Keep the preview identity (asOf), but re-read through validationNow at acceptance. */
    suspend fun apply(db: TrainPilotDatabase, preview: NativeProfileProgramPreview.Preview,
        asOf: Instant, activeProgramId: String = preview.source.program.id,
        validationNow: Instant = asOf): NativeProfileProgramPreview.Preview = NativeProfileHistory128.preview(db,
            preview.source, preview.sourceProfileJson, preview.language, asOf, activeProgramId, validationNow)

    /** Resolve eligibility before min-three/capacity checks, using current Room evidence only. */
    suspend fun preview(db: TrainPilotDatabase, source: ProgramWithDays, rawProfile: String,
        language: String, asOf: Instant, activeProgramId: String = source.program.id,
        validationNow: Instant = asOf): NativeProfileProgramPreview.Preview {
        require(activeProgramId.isNotBlank() && activeProgramId.length <= 200 && !validationNow.isBefore(asOf))
        val catalog = db.exerciseLibraryDao().getAllExercises()
        val practiced = practiced(db, catalog, JSONObject(rawProfile), validationNow)
        val full = NativeProfileProgramPreview.preview(source, rawProfile,
            catalog, language, deferHistoryLimits = true, practicedIds = practiced.keys().asSequence().toSet())
        val allow = compatiblePrograms(db, source.program.id)
        val prescriptions = JSONObject(full.prescriptions.toString())
        val entries = JSONObject()
        val visited = HashSet<String>()
        for (day in full.draft.days) for (encoded in day.entries) {
            val id = NativeProgramEditor.exerciseId(encoded)
            if (!visited.add(id)) continue
            val lib = db.exerciseLibraryDao().getExerciseById(id) ?: continue
            val rx = prescriptions.getJSONObject(id)
            val variation = rx.optString("variation")
            val candidates = ArrayList<Evidence>()
            val keys = HashSet<Pair<String, String>>()
            var before: Long? = null
            var beforeId = ""
            var pageCount = 0
            var unsafeLatest = false
            while (pageCount++ < MAX_PAGES && candidates.size < MAX_SOURCES && !unsafeLatest) {
                val rows = db.workoutHistoryDao().getCoachExerciseCandidates(id,
                    validationNow.minus(Duration.ofDays(90)).toEpochMilli(), validationNow.toEpochMilli(),
                    before, beforeId, PAGE)
                for (row in rows) {
                    if (row.programId !in allow ||
                        id !in allow.getValue(row.programId)[row.dayId].orEmpty()) continue
                    val accepted = evidence(row, id, rx.optString("loadType", lib.loadType),
                        lib.repUnit, variation)
                    if (accepted == null) {
                        // If the most recent relevant workout is partial, painful or has
                        // mismatched units, never fall back to an older convenient weight.
                        if (candidates.isEmpty()) { unsafeLatest = true; break }
                        continue
                    }
                    if (keys.add(accepted.programId to accepted.key)) candidates += accepted
                    if (candidates.size >= MAX_SOURCES) break
                }
                if (rows.size < PAGE) break
                before = rows.last().finishedEpochMs
                beforeId = rows.last().id
            }
            if (candidates.isEmpty() || unsafeLatest) continue
            val first = candidates.first()
            val basis = JSONObject()
                .put("loadType", rx.optString("loadType", lib.loadType))
                .put("variation", variation).put("metric", metric(lib.repUnit))
                .put("sources", JSONArray().apply {
                    candidates.forEach { put(JSONObject().put("programId", it.programId).put("key", it.key)) }
                })
            // The freshest compatible complete session determines the starting load.
            // Older different weights are history, not a progression authorization.
            basis.put("evidenceHash", digest(candidates.joinToString("|") {
                "${it.programId}:${it.key}:${it.raw}"
            }))
            val effectiveLoad = rx.optString("loadType", lib.loadType)
            if (first.sets in 2..3 && (effectiveLoad == "bodyweight" || first.load > 0)) {
                if (effectiveLoad != "bodyweight") {
                    rx.put("weight", first.load).put("trial", false)
                }
                rx.put("sets", first.sets)
                rx.put("reps", ceiling(rx.optString("reps"), first.topReps))
                basis.put("status", "inherited")
            } else basis.put("status", "review")
            entries.put(id, basis)
        }
        val result = NativeProfileDayHistory128.adapt(db, full, prescriptions, entries, activeProgramId, asOf, validationNow)
        val selected = result.draft.days.flatMap { it.entries.map(NativeProgramEditor::exerciseId) }.toSet()
        for (id in practiced.keys().asSequence().toList()) if (id !in selected) practiced.remove(id)
        result.history128.put("practiced", practiced)
        return result
    }

    /** main tp128PracticedIds permits any program; this exception never authorizes load inheritance. */
    private suspend fun practiced(db: TrainPilotDatabase, catalog: List<ExerciseLibraryEntity>,
        profile: JSONObject, now: Instant): JSONObject {
        val proofs = JSONObject()
        if (profile.optString("experience") != "beginner") return proofs
        for (library in catalog) {
            if (NativeProfileProgramPreview.variationRejection(library, profile) != "experience") continue
            // The newest occurrence is decisive: no older success hides partial/pain/changed units.
            val row = db.workoutHistoryDao().getCoachExerciseCandidates(library.id,
                now.minus(Duration.ofDays(90)).toEpochMilli(), now.toEpochMilli(), null, "", 1).firstOrNull() ?: continue
            val raw = runCatching { JSONObject(row.rawJson) }.getOrNull() ?: continue
            if (row.startedEpochMs <= 0 || row.finishedEpochMs <= row.startedEpochMs ||
                raw.optString("programId") != row.programId || row.programId.isBlank() || row.programId.length > 200) continue
            val key = raw.optString("id")
            if (key.isBlank() || key.length > 200 || raw.optJSONObject("feedback")?.optString("rating") == "pain" ||
                raw.optJSONObject("feedback")?.optString("effort") == "pain") continue
            val exercises = raw.optJSONArray("exercises") ?: continue
            val matches = (0 until exercises.length()).mapNotNull { exercises.optJSONObject(it) }
                .filter { it.optString("id") == library.id }
            val ex = matches.singleOrNull() ?: continue
            val lib = JSONObject(library.rawJson).put("id", library.id).put("loadType", library.loadType)
                .put("repUnit", library.repUnit).put("custom", library.custom)
            if (ex.optString("effort") == "pain" || !NativeProfileDayHistory128.complete(ex) ||
                !NativeCoachAdviceRepository.compatible(ex, lib, JSONObject())) continue
            proofs.put(library.id, JSONObject().put("programId", row.programId).put("key", key)
                .put("evidenceHash", digest(row.toString())))
        }
        return proofs
    }
}
