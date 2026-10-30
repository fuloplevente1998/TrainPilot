package com.repforge.app.ui.coach

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.*
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.security.MessageDigest
import java.time.*

/** Explicit, read-only variation preview, based on main 7473b30 tp128VariationPreview/tp128Rx. */
internal class NativeCoachVariation(private val context: Context, private val db: TrainPilotDatabase) {
    data class Preview(val target: CoachTarget, val exerciseId: String, val source: ProgramWithDays,
        val draft: NativeProgramEditor.Draft, val next: NativeCoachExerciseAdvice.Variation,
        val recipe: String, val proof: String, val language: String)

    suspend fun preview(target: CoachTarget, exerciseId: String, now: Instant = Instant.now()): Preview =
        NativeDataReplacement.mutate(context) {
            // Execution is main-looper confined, including when save() rechecks a preview
            // from within an IO/Room transaction. Only the short snapshot read hops to main.
            withContext(Dispatchers.Main.immediate) {
                check(ActiveWorkoutStore.current(context)?.optBoolean("active") != true &&
                    PhoneWorkoutExecution.store(context).snapshot()?.optBoolean("active") != true)
            }
            val settings = TrainPilotSettingsStore(context).current()
            require(settings.activeProgramId == target.programId)
            val profile = NativeTrainingProfile.verified(JSONObject(settings.profileJson))
            require(profile.has("gear") && profile.has("minutes"))
            val source = requireNotNull(db.programDao().getProgramWithDaysById(target.programId))
            val readiness = readiness(now)
            val repository = NativeCoachAdviceRepository(db)
            val advice = repository.load(target, settings, readiness, now).first { it.id == exerciseId }.advice
            require(advice.action == "variation" && advice.historyCount >= 3)
            val library = requireNotNull(db.exerciseLibraryDao().getExerciseById(exerciseId))
            require(NativeProfileProgramPreview.variationRejection(library, profile).isEmpty())
            var next = requireNotNull(advice.variation)
            var proposed = requireNotNull(db.exerciseLibraryDao().getExerciseById(next.exerciseId))
            val reason = NativeProfileProgramPreview.variationRejection(proposed, profile)
            require(reason != "avoid-area")
            if (reason.isNotEmpty()) {
                next = NativeCoachExerciseAdvice.Variation(exerciseId, "paused", 1)
                proposed = library
            }
            if (next.name == "weighted") {
                val gear = profile.getJSONArray("gear")
                require((0 until gear.length()).any { gear.optString(it) in setOf("dumbbell", "dumbbells", "barbell") })
            }
            val recipe = recipe(next, JSONObject(proposed.rawJson).put("id", proposed.id).put("repUnit", proposed.repUnit).put("loadType", proposed.loadType), profile)
            val original = NativeProgramEditor.from(source, settings.language)
            val draft = original.copy(days = original.days.map { day ->
                val replaced = day.entries.map { encoded ->
                    if (NativeProgramEditor.exerciseId(encoded) != exerciseId) encoded else {
                        val raw = JSONTokener(encoded).nextValue()
                        if (raw is JSONObject) {
                            raw.put("id", next.exerciseId)
                            for (key in recipe.keys()) raw.put(key, recipe.get(key))
                            raw.toString()
                        } else JSONObject.quote(next.exerciseId)
                    }
                }
                require(replaced.map(NativeProgramEditor::exerciseId).distinct().size == replaced.size)
                day.copy(entries = replaced)
            })
            val program = JSONObject(source.program.rawJson)
            val prescriptions = program.optJSONObject("prescriptions") ?: JSONObject()
            val rx = prescriptions.optJSONObject(exerciseId) ?: JSONObject()
            val lib = JSONObject(library.rawJson).put("id", exerciseId).put("loadType", library.loadType)
                .put("repUnit", library.repUnit).put("custom", library.custom)
            val dayItems = JSONArray(source.days.first { it.dayId == target.dayId }.exercisesJson)
            for (i in 0 until dayItems.length()) {
                val entry = dayItems.optJSONObject(i) ?: continue
                if (entry.optString("id") == exerciseId) for (key in entry.keys()) if (key != "id") rx.put(key, entry.get(key))
            }
            val evidence = repository.recent(exerciseId, target.programId, lib, rx, program, now)
            val token = listOf(source.toString(), settings.profileJson, settings.language,
                settings.progressionSteps.toSortedMap().toString(), library.toString(), proposed.toString(),
                readiness.toString(), evidence.joinToString { it.first.toString() }, recipe.toString()).joinToString("\n")
            val hash = MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
            Preview(target, exerciseId, source, draft, next, recipe.toString(), hash, settings.language)
        }

    /** Save an independent program. Activating it remains the existing explicit Programs action. */
    suspend fun save(opened: Preview): String = NativeDataReplacement.mutate(context) {
        db.withTransaction {
            val fresh = preview(opened.target, opened.exerciseId)
            check(fresh.proof == opened.proof && fresh.draft == opened.draft && fresh.next == opened.next)
            val id = NativeProgramEditor.save(context, db, fresh.source, fresh.draft, fresh.language, clone = true)
            val saved = requireNotNull(db.programDao().getProgramWithDaysById(id))
            val raw = JSONObject(saved.program.rawJson)
            val recipes = raw.optJSONObject("prescriptions") ?: JSONObject()
            recipes.put(fresh.next.exerciseId, JSONObject(fresh.recipe))
            raw.put("prescriptions", recipes).put("generated", true).put("sourceType", "coach")
                .put("parentProgramId", fresh.source.program.id)
            raw.optJSONObject("history128")?.optJSONObject("entries")?.remove(fresh.exerciseId)
            // REPLACE deletes the parent row and cascades to program_days. Republish the
            // existing days with the metadata in the same transaction instead of losing them.
            db.programDao().saveProgramWithDays(saved.program.copy(generated = true, sourceType = "coach", rawJson = raw.toString()), saved.days)
            id
        }
    }

    private suspend fun readiness(now: Instant): NativeCoachReadiness.Result {
        val zone = ZoneId.systemDefault(); val today = now.atZone(zone).toLocalDate()
        val response = HealthJournalStore.get(context).page(today.minusDays(14).toString(), today.toString(), "journal", "", 0, 0, 1)
        val array = response.getJSONArray("days")
        val signals = NativeCoachSignals.fromDays((0 until array.length()).map { array.getJSONObject(it) }, today, zone)
        val history = db.workoutHistoryDao().getCoachRecentHistory(now.minus(Duration.ofDays(14)).toEpochMilli(), now.toEpochMilli()).map { JSONObject(it) }
        return NativeCoachReadiness.calculate(signals.recovery, signals.daily, history, now, zone)
    }

    companion object {
        internal fun recipe(next: NativeCoachExerciseAdvice.Variation, library: JSONObject, profile: JSONObject): JSONObject {
            return NativeTrainingPrescription.create(JSONObject(library.toString()).put("id", next.exerciseId)
                .put("loadType", library.optString("loadType", "bodyweight")), profile)
                .put("variation", next.name).put("progressionLevel", next.level)
                .also { if (next.name == "weighted") it.put("loadType", "single_dumbbell") }
        }
    }
}
