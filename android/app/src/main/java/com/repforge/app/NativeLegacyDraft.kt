package com.repforge.app

import com.repforge.shared.WorkoutOwnership193
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/** Pure draft preparation plus exact-source transactional retirement after durable save/discard. */
internal object NativeLegacyDraft {
    suspend fun retire(database: com.repforge.app.data.db.TrainPilotDatabase, expectedHash: String) {
        if (expectedHash.isEmpty()) return
        database.withTransaction {
            val original = database.keyValueDocumentDao().getDocument("draft")
            if (original != null && hash(original.json) == expectedHash) {
                database.keyValueDocumentDao().putDocument(com.repforge.app.data.db.KeyValueDocumentEntity(
                    "legacyDraftArchive|" + expectedHash, original.json, System.currentTimeMillis()))
                database.keyValueDocumentDao().deleteDocument("draft")
            }
        }
    }

    fun hash(raw: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    fun prepare(raw: String, knownExercises: Set<String>, now: Instant): JSONObject {
        val document = JSONObject(raw)
        val source = document.getJSONObject("session")
        check(!source.optBoolean("finalizationPending") && !source.optBoolean("deleted") &&
            !source.optBoolean("cancelled") && source.optString("finished").isBlank()) { "A draft már lezárás alatt áll vagy lezárt." }
        check(source.optString("timerOwner", "phone") == "phone" && !WorkoutOwnership193.pending(source) &&
            source.optLong("ownerEpoch193") == 0L) { "A draft tulajdonosát előbb egyeztetni kell." }
        val started = Instant.parse(source.getString("started"))
        val saved = Instant.parse(document.getString("savedAt"))
        check(started <= saved && saved <= now) { "A draft időadata nem egyértelmű." }
        val ids = listOf("workoutId", "syncId", "id").mapNotNull { key ->
            if (source.isNull(key)) null else source.optString(key).takeIf { it.isNotBlank() }
        }.distinct()
        check(ids.size <= 1) { "Ellentmondó edzésazonosítók." }
        // Accepted legacy phone execution uses started as identity when syncId is absent.
        val id = ids.singleOrNull() ?: source.getString("started")
        val next = JSONObject(source.toString())
        val exercises = next.getJSONArray("exercises")
        check(exercises.length() > 0) { "A draft nem tartalmaz gyakorlatot." }
        val setIds = HashSet<String>()
        for (e in 0 until exercises.length()) {
            val exercise = exercises.getJSONObject(e)
            val exerciseId = exercise.getString("id")
            check(exerciseId in knownExercises) { "Hiányzó gyakorlat: $exerciseId" }
            val sets = exercise.getJSONArray("sets")
            check(sets.length() > 0) { "A gyakorlatnak nincs visszaállítható sorozata." }
            for (s in 0 until sets.length()) {
                val set = sets.getJSONObject(s)
                val setId = if (!set.isNull("setId") && set.optString("setId").isNotBlank()) set.getString("setId")
                    else UUID.nameUUIDFromBytes("legacy-draft\u0000$id\u0000$e\u0000$s".toByteArray(Charsets.UTF_8)).toString()
                check(setIds.add(setId)) { "Ismétlődő sorozatazonosító." }
                set.put("setId", setId)
            }
        }
        val intervals = next.optJSONArray("activeIntervals") ?: JSONArray().put(JSONObject()
            .put("start", started.toString()).put("end", saved.toString()))
        var previousEnd = started
        for (i in 0 until intervals.length()) {
            val interval = intervals.getJSONObject(i)
            val begin = Instant.parse(interval.getString("start"))
            val end = if (interval.isNull("end")) saved else Instant.parse(interval.getString("end"))
            check(begin >= previousEnd && end >= begin && end <= saved) { "Hibás aktív időszakasz." }
            check(!interval.isNull("end") || i == intervals.length() - 1) { "Lezáratlan köztes időszakasz." }
            interval.put("end", end.toString()); previousEnd = end
        }
        // Keep measurement values, route references, photos and time ledgers. Old runtime clocks
        // cannot become live native controllers. The immutable original remains the audit source.
        for (key in listOf("generation", "executionRevision", "phoneExecution162", "nativeEditorProjection",
            "latestTimer162", NativeSetStopwatch.FIELD, NativeGpsMeasurement.FIELD, "undo188", "lastRecord188")) next.remove(key)
        return next.put("workoutId", id).put("id", id).put("syncId", id).put("active", true)
            .put("timerOwner", "phone").put("originDevice", "phone").put("workoutPaused", true)
            .put("activeIntervals", intervals).put("restEndAt", 0)
            .put("currentExercise", document.optInt("current").coerceIn(0, exercises.length() - 1))
            .put("legacyDraftSavedAt", saved.toString())
    }
}
