package com.repforge.app.ui.settings

import android.content.Context
import com.repforge.app.HealthJournalStore
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
        val exercise: String, val beforeSets: Int?, val afterSets: Int?,
        val beforeReps: String, val afterReps: String,
        val restSeconds: Int?, val beforeWeight: Double?, val afterWeight: Double?,
        val rir: String
    )
    data class Snapshot(val programName: String, val dayName: String, val rows: List<Row>)

    suspend fun read(context: Context, preview: NativeTrainingModeChange.Preview): Snapshot {
        val db = TrainPilotDatabase.getInstance(context)
        val settings = TrainPilotSettingsStore(context).current()
        check(settings.trainingModes197Json == preview.expectedRaw &&
            settings.activeProgramId == preview.expectedActiveProgramId) { "Stale training modes preview" }
        val pid = preview.programId ?: settings.activeProgramId
        val program = db.programDao().getProgramWithDaysById(pid) ?: return Snapshot("", "", emptyList())
        val day = program.days.sortedBy { it.sortOrder }.firstOrNull() ?: return Snapshot("", "", emptyList())
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val date = now.atZone(zone).toLocalDate()
        val days = HealthJournalStore.get(context).page(date.minusDays(14).toString(),
            date.toString(), "journal", "", 0L, 0L, 1).optJSONArray("days")
        val signals = NativeCoachSignals.fromDays((0 until (days?.length() ?: 0))
            .mapNotNull { days?.optJSONObject(it) }, date, zone)
        val history = db.workoutHistoryDao().getCoachRecentHistory(
            now.minus(Duration.ofDays(14)).toEpochMilli(), now.toEpochMilli()).map { JSONObject(it) }
        val readiness = NativeCoachReadiness.calculate(
            signals.recovery, signals.daily, history, now, zone)
        val target = CoachTarget(pid, day.dayId, day.nameHu)
        val repository = NativeCoachAdviceRepository(db)
        val before = repository.load(target, settings, readiness, now)
        val prospectiveJson = NativeTrainingModeChange.next(preview)
        val after = repository.load(target, settings.copy(trainingModes197Json = prospectiveJson.toString()),
            readiness, now)
        val afterById = after.associateBy { it.id }
        val raw = JSONObject(program.program.rawJson)
        val prescriptions = raw.optJSONObject("prescriptions")
        val rows = before.mapNotNull { old ->
            val updated = afterById[old.id] ?: return@mapNotNull null
            val rx = prescriptions?.optJSONObject(old.id)
            val rest = rx?.opt("rest")?.let { value ->
                val parsed = when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull()
                    else -> null
                }
                parsed?.takeIf { it in 0..3600 }
            }
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
            Row(old.name, old.modePreview?.workingSets ?: old.advice.target.sets,
                updated.modePreview?.workingSets ?: updated.advice.target.sets,
                repetitions(old.advice), repetitions(updated.advice),
                rest, weight(old.advice), weight(updated.advice), targetRir)
        }
        check(TrainPilotSettingsStore(context).current().trainingModes197Json == preview.expectedRaw) {
            "Training modes changed during preview"
        }
        val name = when (settings.language) {
            "en" -> program.program.nameEn; "de" -> program.program.nameDe
            "ro" -> program.program.nameRo; else -> program.program.nameHu
        }.ifBlank { program.program.nameHu }
        val dayName = when (settings.language) {
            "en" -> day.nameEn; "de" -> day.nameDe; "ro" -> day.nameRo; else -> day.nameHu
        }.ifBlank { day.nameHu }
        return Snapshot(name, dayName, rows)
    }
}
