package com.repforge.app

import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.ui.coach.NativeCoachAdviceRepository
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

/** Bounded, same-program/day proof for main tp128AdaptHistory's practiced-day exception. */
internal object NativeProfileDayHistory128 {
    private const val PAGE = 32
    private const val MAX_PAGES = 20
    private data class Evidence(val prior: JSONObject?, val practiced: JSONObject?, val hash: String, val sampled: Int, val practicedKey: String?)

    /** Optional extensions: original main history128 without day proofs remains valid. */
    fun validate(history: JSONObject) {
        fun id(row: JSONObject, field: String, max: Int = 200): String {
            val value = row.opt(field) as? String ?: throw IllegalArgumentException("Invalid day identity")
            require(value.isNotBlank() && value.length <= max)
            return value
        }
        fun count(row: JSONObject, field: String): Int {
            val number = row.opt(field) as? Number ?: throw IllegalArgumentException("Invalid day count")
            require(number.toDouble().isFinite() && number.toDouble() == number.toInt().toDouble() && number.toInt() >= 0)
            return number.toInt()
        }
        if (history.has("activeProgramId")) id(history, "activeProgramId")
        if (history.has("days")) {
            val days = history.optJSONObject("days") ?: throw IllegalArgumentException("Invalid day evidence")
            require(days.length() <= 31)
            for (day in days.keys()) {
                require(day.isNotBlank() && day.length <= 120)
                val row = days.getJSONObject(day)
                id(row, "programId")
                require(row.opt("retained") is Boolean && Regex("[0-9a-f]{64}").matches(row.optString("evidenceHash")))
                require(count(row, "sampled") <= 640)
                if (row.has("key") || row.getBoolean("retained")) id(row, "key")
            }
        }
        if (history.has("reductions")) {
            val rows = history.optJSONArray("reductions") ?: throw IllegalArgumentException("Invalid reductions")
            require(rows.length() <= 31)
            val seen = HashSet<String>()
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                require(seen.add(id(row, "dayId", 120)))
                require(row.optString("reason") in setOf("time", "equipment"))
                val fromExercises = count(row, "fromExercises"); val toExercises = count(row, "toExercises")
                val fromSets = count(row, "fromSets"); val toSets = count(row, "toSets")
                require(toExercises < fromExercises || toSets < fromSets)
            }
        }
    }

    internal fun complete(exercise: JSONObject): Boolean {
        val sets = exercise.optJSONArray("sets") ?: return false
        if (sets.length() !in 1..10) return false
        fun value(set: JSONObject, key: String): Double? {
            val raw = set.opt(key)
            return when (raw) {
                is Number -> raw.toDouble(); is String -> raw.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
                else -> null
            }?.takeIf { it.isFinite() }
        }
        return (0 until sets.length()).all { index ->
            val set = sets.optJSONObject(index) ?: return@all false
            val reps = value(set, "reps"); val weight = value(set, "weight")
            set.opt("done") == true && reps != null && reps > 0 && reps <= 1000 && weight != null && weight in 0.0..1000.0
        }
    }

    private suspend fun evidence(db: TrainPilotDatabase, active: String, day: NativeProgramEditor.DayDraft,
        recipes: JSONObject, now: Instant, budget: Int): Evidence {
        val ids = day.entries.map(NativeProgramEditor::exerciseId)
        val libraries = ids.associateWith { id -> db.exerciseLibraryDao().getExerciseById(id)?.let { library ->
            JSONObject(library.rawJson).put("id", id).put("loadType", library.loadType)
                .put("repUnit", library.repUnit).put("custom", library.custom)
        } }
        val digest = MessageDigest.getInstance("SHA-256")
        var prior: JSONObject? = null; var practiced: JSONObject? = null
        var practicedKey: String? = null
        var decided = false; var sampled = 0; var before: Long? = null; var beforeId = ""
        repeat(MAX_PAGES) {
            val rows = db.workoutHistoryDao().getProfileDayCandidates(active, day.id,
                now.minus(Duration.ofDays(90)).toEpochMilli(), now.toEpochMilli(), before, beforeId, PAGE)
            for (row in rows) {
                sampled++; digest.update(row.toString().toByteArray(Charsets.UTF_8))
                val raw = runCatching { JSONObject(row.rawJson) }.getOrNull()
                if (raw == null) { decided = true; continue }
                if (row.startedEpochMs <= 0 || row.finishedEpochMs <= row.startedEpochMs || raw.optString("programId", active) != active) { decided = true; continue }
                val array = raw.optJSONArray("exercises")
                if (array == null) { decided = true; continue }
                val exercises = (0 until array.length()).mapNotNull { array.optJSONObject(it) }
                val unique = exercises.map { it.optString("id") }.toSet().size == exercises.size
                val all = exercises.isNotEmpty() && exercises.size == array.length() && unique && exercises.all(::complete)
                if (prior == null && all) prior = raw
                if (!decided) {
                    val painful = raw.optJSONObject("feedback")?.optString("rating") == "pain" ||
                        raw.optJSONObject("feedback")?.optString("effort") == "pain" ||
                        exercises.any { it.optString("effort") == "pain" }
                    // A newer partial/painful day must not be hidden behind an older convenient success.
                    if (!all || painful) decided = true
                    else {
                        val byId = exercises.associateBy { it.optString("id") }
                        val matches = ids.all { id ->
                            val exercise = byId[id]; val lib = libraries[id]
                            exercise != null && lib != null && NativeCoachAdviceRepository.compatible(exercise, lib, recipes.getJSONObject(id))
                        }
                        decided = true
                        if (matches && row.finishedEpochMs - row.startedEpochMs <= budget * 60_000L) {
                            practiced = raw
                            practicedKey = raw.optString("id").ifBlank { row.workoutKey.ifBlank { row.id } }
                        }
                    }
                }
                if (decided && prior != null) break
            }
            if (rows.size < PAGE || (decided && prior != null))
                return Evidence(prior, practiced, digest.digest().joinToString("") { "%02x".format(it) }, sampled, practicedKey)
            before = rows.last().finishedEpochMs; beforeId = rows.last().id
        }
        return Evidence(prior, practiced, digest.digest().joinToString("") { "%02x".format(it) }, sampled, practicedKey)
    }

    suspend fun adapt(db: TrainPilotDatabase, full: NativeProfileProgramPreview.Preview,
        recipes: JSONObject, entries: JSONObject, active: String, asOf: Instant, now: Instant): NativeProfileProgramPreview.Preview {
        val budget = full.profileSnapshot.getInt("minutes")
        val capacity = NativePlannerPolicy.capacity(budget)
        val proofs = JSONObject()
        val dayEvidence = mutableMapOf<String, Evidence>()
        val before = db.programDao().getProgramWithDaysById(active)
        val reductions = full.reductions.toMutableList()
        val days = full.draft.days.map { day ->
            val proof = (if (before?.days?.any { it.dayId == day.id } == true) evidence(db, active, day, recipes, now, budget)
                else Evidence(null, null, MessageDigest.getInstance("SHA-256").digest().joinToString("") { "%02x".format(it) }, 0, null))
                .also { dayEvidence[day.id] = it }
            val fits = proof.practiced != null && NativeProfileProgramPreview.estimatedMinutes(listOf(day), recipes) <= budget
            val retained = day.entries.size > capacity && fits
            val chosen = if (fits) day.entries else day.entries.take(capacity)
            for (item in day.entries.drop(chosen.size)) reductions += NativeProfileProgramPreview.Reduction(day.id,
                NativeProgramEditor.exerciseId(item), "time")
            proofs.put(day.id, JSONObject().put("programId", active).put("evidenceHash", proof.hash)
                .put("sampled", proof.sampled).put("retained", retained)
                .also { if (retained) it.put("key", proof.practicedKey) })
            day.copy(entries = chosen)
        }
        val used = days.flatMap { it.entries.map(NativeProgramEditor::exerciseId) }.toSet()
        for (id in recipes.keys().asSequence().toList()) if (id !in used) recipes.remove(id)
        for (id in entries.keys().asSequence().toList()) if (id !in used) entries.remove(id)
        val history = JSONObject().put("entries", entries).put("days", proofs).put("activeProgramId", active).put("asOf", asOf.toEpochMilli())
        val fitted = NativeProfileProgramPreview.fitTime(full.copy(draft = full.draft.copy(days = days),
            prescriptions = recipes, history128 = history, reductions = reductions))
        val changes = JSONArray()
        for (day in fitted.draft.days) {
            val oldDay = before?.days?.firstOrNull { it.dayId == day.id } ?: continue
            val old = JSONArray(oldDay.exercisesJson)
            val oldIds = (0 until old.length()).map { ix ->
                val value = old.get(ix)
                NativeProgramEditor.exerciseId(if (value is String) JSONObject.quote(value) else value.toString())
            }
            val prior = dayEvidence[day.id]?.prior?.optJSONArray("exercises")
            val fromExercises = prior?.length() ?: oldIds.size
            val fromSets = if (prior != null) (0 until prior.length()).sumOf { prior.getJSONObject(it).getJSONArray("sets").length() }
                else oldIds.sumOf { id -> if (entries.optJSONObject(id)?.optString("status") == "inherited") recipes.optJSONObject(id)?.optInt("sets") ?: 0 else 0 }
            val newIds = day.entries.map(NativeProgramEditor::exerciseId)
            val toSets = newIds.sumOf { fitted.prescriptions.getJSONObject(it).getInt("sets") }
            val inherited = newIds.any { id -> entries.optJSONObject(id)?.optJSONArray("sources")?.let { sources ->
                (0 until sources.length()).any { sources.getJSONObject(it).optString("programId") == active }
            } == true }
            if (inherited && (newIds.size < fromExercises || toSets < fromSets)) {
                val changedEquipment = newIds.any { it !in oldIds } || oldIds.any { id ->
                    val library = db.exerciseLibraryDao().getExerciseById(id)
                    library == null || NativeProfileProgramPreview.variationRejection(library, full.profileSnapshot).isNotEmpty()
                }
                changes.put(JSONObject().put("dayId", day.id).put("reason", if (changedEquipment) "equipment" else "time")
                    .put("fromExercises", fromExercises).put("toExercises", newIds.size).put("fromSets", fromSets).put("toSets", toSets))
            }
        }
        history.put("reductions", changes)
        return fitted
    }
}
