package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.shared.SetEditorRules212
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/** An isolated edit of one existing journal row, never an active-workout replay. */
internal object NativeJournalEditor {
    data class Opened(val detail: WorkoutHistoryWithDetails, val ticket: NativeDataReplacement.Continuation)
    data class Draft(val started: String, val finished: String, val exercises: List<Exercise>,
        val feedbackEffort: String? = null)
    data class Units(val loadType: String, val repUnit: String, val measurementType: String)
    data class Exercise(val originalIndex: Int?, val id: String, val sets: List<Set>, val catalogUnits: Units? = null)
    data class Set(val originalIndex: Int?, val weight: String, val reps: String, val done: Boolean,
        val left: String = "", val right: String = "", val distance: String = "", val rir: String = "")

    fun open(context: Context, detail: WorkoutHistoryWithDetails) =
        Opened(detail, NativeDataReplacement.continuationForCurrentData(context))

    fun from(detail: WorkoutHistoryWithDetails): Draft {
        // Legacy import can retain malformed dates; surface an operation error before composing date fields.
        Instant.parse(detail.workout.started)
        Instant.parse(detail.workout.finished)
        val raw = LegacyWebViewMigrator.workoutJson(detail)
        val exercises = raw.getJSONArray("exercises")
        return Draft(detail.workout.started, detail.workout.finished, (0 until exercises.length()).map { e ->
            val ex = exercises.getJSONObject(e)
            val sets = ex.getJSONArray("sets")
            Exercise(e, ex.getString("id"), (0 until sets.length()).map { i ->
                val set = sets.getJSONObject(i)
                fun value(key: String) = if (set.isNull(key)) "" else set.optString(key)
                Set(i, value("weight"), value("reps"), set.optBoolean("done"),
                    value("leftSeconds"), value("rightSeconds"), value("distanceMeters"), value("actualRir"))
            })
        }, detail.workout.effortFeedback)
    }

    fun addExercise(draft: Draft, source: ExerciseLibraryEntity): Draft {
        require(draft.exercises.none { it.id == source.id })
        return draft.copy(exercises = draft.exercises + Exercise(null, source.id,
            List(source.defaultSets.coerceIn(1, 500)) { Set(null,
                if (source.loadType == "bodyweight") "0" else source.defaultWeight.toString(), "", false) },
            Units(source.loadType, source.repUnit, source.measurementType)))
    }

    private fun sorted(detail: WorkoutHistoryWithDetails) = detail.copy(
        exercises = detail.exercises.sortedBy { it.exercise.rowId }.map { it.copy(sets = it.sets.sortedBy { s -> s.rowId }) },
        photos = detail.photos.sortedBy { it.id })

    internal suspend fun requireNotActive(context: Context, aliases: kotlin.collections.Set<String>) = withContext(Dispatchers.Main.immediate) {
        val snapshots = listOfNotNull(ActiveWorkoutStore.current(context),
            PhoneWorkoutExecution.store(context).snapshot(), PhoneOwnership193.journal(context).read().optJSONObject("snapshot"))
        check(snapshots.none { frame ->
            listOf(frame.optString("workoutId"), frame.optString("id"), frame.optString("syncId")).any { it in aliases } &&
                (frame.optBoolean("active") || frame.optBoolean("finalizationPending") || frame.optBoolean("discardPending"))
        }) { "Workout is still active or being finalized" }
    }

    suspend fun save(context: Context, db: TrainPilotDatabase, opened: Opened, draft: Draft): String =
        NativeDataReplacement.mutate(context) {
            opened.ticket.requireCurrent(context)
            val w = opened.detail.workout
            val aliases = setOf(w.id, w.syncId, w.workoutKey).filter { it.isNotBlank() }.toSet()
            requireNotActive(context, aliases)
            db.withTransaction {
                val latest = db.workoutHistoryDao().getWorkoutWithDetailsById(w.id)
                check(latest != null && sorted(latest) == sorted(opened.detail)) { "Workout changed; reopen editor" }
                for (id in aliases) check(!db.workoutHistoryDao().isTombstoned(id, JSONObject.quote(id))) {
                    "Deleted workout cannot be edited"
                }
                val catalog = draft.exercises.filter { it.originalIndex == null }.associate { ex ->
                    ex.id to (db.exerciseLibraryDao().getExerciseById(ex.id) ?: error("Exercise no longer exists"))
                }
                val result = encode(opened.detail, draft, catalog)
                // Reuse bounded PR evaluation and the canonical relation/summary mapper in this transaction.
                result.remove("prRecords226")
                PersonalRecordCalculator226.evaluateAndAttachPrs(result, db)
                LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context)).saveWorkoutJsonToRoom(result)
                check(db.workoutHistoryDao().getWorkoutWithDetailsById(w.id)?.workout?.rawJson == result.toString()) {
                    "Journal edit was not stored"
                }
                w.id
            }
        }

    internal fun encode(detail: WorkoutHistoryWithDetails, draft: Draft,
        catalog: Map<String, ExerciseLibraryEntity>, now: Long = System.currentTimeMillis()): JSONObject {
        val start = Instant.parse(draft.started)
        val finish = Instant.parse(draft.finished)
        val duration = java.time.Duration.between(start, finish)
        require(!duration.isNegative && !duration.isZero && duration <= java.time.Duration.ofHours(24))
        require(finish.toEpochMilli() <= now + 60_000L)
        require(draft.exercises.size in 1..200)
        val raw = LegacyWebViewMigrator.workoutJson(detail)
        val original = raw.getJSONArray("exercises")
        val usedExercises = HashSet<Int>()
        val newIds = HashSet<String>()
        val exercises = JSONArray()
        for (edited in draft.exercises) {
            val ex = if (edited.originalIndex != null) {
                require(edited.originalIndex in 0 until original.length() && usedExercises.add(edited.originalIndex))
                JSONObject(original.getJSONObject(edited.originalIndex).toString()).also { require(it.getString("id") == edited.id) }
            } else {
                require(newIds.add(edited.id) && (0 until original.length()).none { original.getJSONObject(it).getString("id") == edited.id })
                val source = catalog.getValue(edited.id)
                check(edited.catalogUnits == Units(source.loadType, source.repUnit, source.measurementType)) {
                    "Exercise units changed; select the exercise again"
                }
                JSONObject(source.rawJson).put("id", source.id).put("hu", source.nameHu).put("en", source.nameEn)
                    .put("de", source.nameDe).put("ro", source.nameRo)
                    .put("loadType", source.loadType).put("repUnit", source.repUnit).put("measurementType", source.measurementType)
            }
            require(edited.sets.size in 1..500)
            val oldSets = ex.optJSONArray("sets") ?: JSONArray()
            val usedSets = HashSet<Int>()
            val sets = JSONArray()
            val bilateral = SetEditorRules212.normalizeUnit(ex.optString("repUnit")) == "mp/oldal" || edited.id == "side-plank"
            val distance = ex.optString("measurementType") == "distance" || ex.optString("repUnit") in setOf("km", "m")
            for ((index, edit) in edited.sets.withIndex()) {
                val set = if (edit.originalIndex != null) {
                    require(edit.originalIndex in 0 until oldSets.length() && usedSets.add(edit.originalIndex))
                    JSONObject(oldSets.getJSONObject(edit.originalIndex).toString())
                } else JSONObject().put("setId", UUID.randomUUID().toString())
                val weight = number(edit.weight.ifBlank { "0" }, 0.0, 10000.0)
                val reps = edit.reps.trim()
                require(reps.length <= 40 && reps.none(Char::isISOControl))
                val rir = optionalInteger(edit.rir, 0, 10)
                set.put("set", index + 1).put("weight", if (ex.optString("loadType") == "bodyweight") 0.0 else weight)
                    .put("reps", reps).put("done", edit.done)
                putOptional(set, "actualRir", rir)
                if (bilateral) {
                    val left = optionalInteger(edit.left, 1, 86400)
                    val right = optionalInteger(edit.right, 1, 86400)
                    require(!edit.done || (left != null && right != null))
                    putOptional(set, "leftSeconds", left); putOptional(set, "rightSeconds", right)
                    set.put("reps", if (left != null && right != null) minOf(left, right).toString() else "")
                } else if (distance) {
                    val meters = edit.distance.takeIf { it.isNotBlank() }?.let { number(it, 0.0, 100_000_000.0) }
                    require(!edit.done || (meters != null && meters > 0 && reps.toIntOrNull()?.let { it > 0 } == true))
                    putOptional(set, "distanceMeters", meters)
                } else require(!edit.done || reps.toIntOrNull()?.let { it > 0 } == true)
                sets.put(set)
            }
            ex.put("sets", sets); exercises.put(ex)
        }
        val timesChanged = start.toEpochMilli() != detail.workout.startedEpochMs || finish.toEpochMilli() != detail.workout.finishedEpochMs
        raw.put("exercises", exercises).put("started", start.toString()).put("finished", finish.toString())
            .put("durationSeconds", if (timesChanged) duration.seconds.toInt() else detail.workout.durationSeconds)
            .put("editedAt", Instant.ofEpochMilli(now).toString())
        if (draft.feedbackEffort != detail.workout.effortFeedback) {
            val effort = draft.feedbackEffort
            val scores = listOf("easy", "light", "good", "challenging", "hard")
            require(effort in scores || effort == "pain") { "Invalid workout feedback" }
            val feedback = raw.optJSONObject("feedback")?.let { JSONObject(it.toString()) } ?: JSONObject()
            check(!feedback.optBoolean("applied")) { "Feedback already applied to a program" }
            feedback.put("effort", effort).put("effortScore",
                if (effort == "pain") JSONObject.NULL else scores.indexOf(effort) + 1)
                .put("rating", when (effort) { "pain" -> "pain"; "challenging", "hard" -> "hard"; "good" -> "right"; else -> "easy" })
                .put("source", "phone").put("updatedAt", now)
            feedback.remove("skipped")
            // Old translated suggestions describe the previous answer. Coach reads this saved
            // evidence afresh; editing a journal never applies progression a second time.
            feedback.remove("suggestion")
            raw.put("feedback", feedback)
            if (raw.has("effortFeedback")) raw.put("effortFeedback", effort)
        }
        if (timesChanged) {
            // Measured telemetry retains its real measurement window; it is never silently relabelled.
            raw.put("nativeHealthWindowStale", true)
            if (!raw.has("nativeOriginalHealthWindow")) raw.put("nativeOriginalHealthWindow", JSONObject()
                .put("started", detail.workout.started).put("finished", detail.workout.finished))
        }
        return raw
    }

    private fun number(value: String, min: Double, max: Double): Double =
        (value.trim().replace(',', '.').toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number"))
            .also { require(it.isFinite() && it in min..max) }
    private fun optionalInteger(value: String, min: Int, max: Int): Int? =
        if (value.isBlank()) null else (value.toIntOrNull() ?: throw IllegalArgumentException("Invalid integer"))
            .also { require(it in min..max) }
    private fun putOptional(target: JSONObject, key: String, value: Any?) {
        if (value == null) target.remove(key) else target.put(key, value)
    }
}
