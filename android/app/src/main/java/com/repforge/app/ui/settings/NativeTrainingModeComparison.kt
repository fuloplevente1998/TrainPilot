package com.repforge.app.ui.settings

import android.content.Context
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeDataReplacement
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.NativeTrainingModeChange
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.coach.CoachTarget
import com.repforge.app.ui.coach.NativeCoachAdviceRepository
import com.repforge.app.ui.coach.NativeCoachReadiness
import com.repforge.app.ui.coach.NativeCoachSignals
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Read-only actual-program preview: same Room history, prescription and readiness
 * evaluator as the real Coach screen. Does not save settings or alter a frame.
 */
internal object NativeTrainingModeComparison {
    data class Row(
        val day: String, val exercise: String, val beforeSets: Int?, val afterSets: Int?,
        val beforeReps: String, val afterReps: String,
        val restSeconds: Int?, val beforeWeight: Double?, val afterWeight: Double?,
        val rir: String, val reason: String
    )
    data class Snapshot(
        val programName: String,
        val rows: List<Row>,
        val programId: String,
        val programRaw: String,
        val dayRecipes: List<Pair<String, String>>,
        // Captured before any Room/DataStore read: identical restored bytes must not
        // make a comparison from a previous dataset generation valid again.
        val generation: NativeDataReplacement.Continuation
    )

    /** Read-only revalidation immediately before the existing settings admission/CAS. */
    suspend fun isCurrent(
        context: Context, preview: NativeTrainingModeChange.Preview, snapshot: Snapshot
    ): Boolean {
        snapshot.generation.requireCurrent(context)
        if (snapshot.rows.isEmpty()) return false
        val settings = TrainPilotSettingsStore(context).current()
        if (settings.trainingModes197Json != preview.expectedRaw ||
            settings.activeProgramId != preview.expectedActiveProgramId) return false
        val pid = preview.programId ?: settings.activeProgramId
        if (pid != snapshot.programId) return false
        val current = TrainPilotDatabase.getInstance(context).programDao()
            .getProgramWithDaysById(pid) ?: return false
        val sameRecipes = current.program.rawJson == snapshot.programRaw &&
            current.days.sortedBy { it.sortOrder }.map { it.dayId to it.exercisesJson } ==
                snapshot.dayRecipes
        snapshot.generation.requireCurrent(context)
        return sameRecipes
    }

    suspend fun read(context: Context, preview: NativeTrainingModeChange.Preview): Snapshot {
        val generation = NativeDataReplacement.continuationForCurrentData(context)
        val db = TrainPilotDatabase.getInstance(context)
        val settings = TrainPilotSettingsStore(context).current()
        check(settings.trainingModes197Json == preview.expectedRaw &&
            settings.activeProgramId == preview.expectedActiveProgramId) { "Stale training modes preview" }
        val pid = preview.programId ?: settings.activeProgramId
        require(pid.isNotBlank() && (preview.scope != "program" ||
            preview.programId == preview.expectedActiveProgramId))
        val program = checkNotNull(db.programDao().getProgramWithDaysById(pid)) {
            "Training mode comparison program missing"
        }
        val daysInProgram = program.days.sortedBy { it.sortOrder }
        check(daysInProgram.isNotEmpty()) { "Training mode comparison has no program days" }
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val date = now.atZone(zone).toLocalDate()
        val observedDays = HealthJournalStore.get(context).page(date.minusDays(14).toString(),
            date.toString(), "journal", "", 0L, 0L, 1).optJSONArray("days")
        val signals = NativeCoachSignals.fromDays((0 until (observedDays?.length() ?: 0))
            .mapNotNull { observedDays?.optJSONObject(it) }, date, zone)
        val history = db.workoutHistoryDao().getCoachRecentHistory(
            now.minus(Duration.ofDays(14)).toEpochMilli(), now.toEpochMilli()).map { JSONObject(it) }
        val readiness = NativeCoachReadiness.calculate(
            signals.recovery, signals.daily, history, now, zone)
        val repository = NativeCoachAdviceRepository(db)
        val prospectiveJson = NativeTrainingModeChange.next(preview)
        val raw = JSONObject(program.program.rawJson)
        val prescriptions = raw.optJSONObject("prescriptions")
        val rows = daysInProgram.flatMap { day ->
            val dayName = when (settings.language) {
                "en" -> day.nameEn; "de" -> day.nameDe; "ro" -> day.nameRo
                else -> day.nameHu
            }.ifBlank { day.nameHu }
            val target = CoachTarget(pid, day.dayId, dayName)
            val before = repository.load(target, settings, readiness, now)
            val after = repository.load(target,
                settings.copy(trainingModes197Json = prospectiveJson.toString()), readiness, now)
            // Do not silently drop a missing/mismatched exercise: a partial
            // comparison could otherwise enable approval with stale results.
            check(before.map { it.id } == after.map { it.id } &&
                before.map { it.id }.distinct().size == before.size) {
                "Training mode comparison changed exercise identity"
            }
            val afterById = after.associateBy { it.id }
            before.map { old ->
                val updated = afterById.getValue(old.id)
                val rx = prescriptions?.optJSONObject(old.id)
                // Program-day edits override the program-level Rx, just as the Coach repository does.
                val dayItems = org.json.JSONArray(day.exercisesJson)
                val specific = (0 until dayItems.length())
                    .mapNotNull { dayItems.optJSONObject(it) }
                    .firstOrNull { it.optString("id") == old.id }
                val restRaw = specific?.opt("rest") ?: rx?.opt("rest")
                val rest = when (restRaw) {
                    is Number -> restRaw.toInt()
                    is String -> restRaw.toIntOrNull()
                    else -> null
                }?.takeIf { it in 0..3600 } ?: settings.restSeconds
                val targetRir = old.advice.target.run {
                    when {
                        rirMin == null && rirMax == null -> "—"
                        rirMin == rirMax -> rirMin.toString()
                        else -> "${rirMin ?: "—"}–${rirMax ?: "—"}"
                    }
                }
                fun repetitions(advice: com.repforge.app.ui.coach.NativeCoachExerciseAdvice.Advice): String =
                    advice.nextReps.takeIf { it.isNotEmpty() }?.joinToString(" / ") { it.toString() }
                        ?: advice.target.text.ifBlank { "—" }
                fun weight(advice: com.repforge.app.ui.coach.NativeCoachExerciseAdvice.Advice): Double? =
                    advice.weight.takeIf { it.isFinite() && it > 0 }
                Row(dayName, old.name, old.modePreview?.workingSets ?: old.advice.target.sets,
                    updated.modePreview?.workingSets ?: updated.advice.target.sets,
                    repetitions(old.advice), repetitions(updated.advice),
                    rest, weight(old.advice), weight(updated.advice), targetRir,
                    updated.advice.reason)
            }
        }
        check(rows.isNotEmpty()) { "Training mode comparison has no exercise rows" }
        val snapshot = Snapshot(
            programName = when (settings.language) {
                "en" -> program.program.nameEn; "de" -> program.program.nameDe
                "ro" -> program.program.nameRo; else -> program.program.nameHu
            }.ifBlank { program.program.nameHu },
            rows = rows, programId = pid,
            programRaw = program.program.rawJson,
            dayRecipes = daysInProgram.map { it.dayId to it.exercisesJson },
            generation = generation
        )
        check(isCurrent(context, preview, snapshot)) {
            "Training mode comparison became stale"
        }
        return snapshot
    }
}
