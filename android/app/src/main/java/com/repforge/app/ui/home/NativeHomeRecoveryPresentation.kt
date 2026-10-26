package com.repforge.app.ui.home

import com.repforge.app.ui.coach.NativeCoachReadiness
import com.repforge.app.ui.coach.NativeCoachSignals
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Only a display projection. Readiness recommendations may be capped by training
 * load or pain; the Home energy tile must never display that capped Coach score.
 * No persisted data is changed and no synthetic provider reading is produced.
 */
object NativeHomeRecoveryPresentation {
    enum class Source { SAMSUNG, TRAINPILOT_ESTIMATE, NONE }

    data class Model(
        val source: Source,
        val value: Double?,
        val timestamp: String?,
        val calculatedAt: Instant?,
        val partial: Boolean,
        val stale: Boolean,
        val refreshFailed: Boolean = false
    ) {
        val displayValue: String get() = value?.let {
            if (it.isFinite() && it >= 0.0 && it <= 100.0) "${kotlin.math.round(it).toInt()}%" else "—"
        } ?: "—"
    }

    fun select(
        days: List<JSONObject>,
        history: List<JSONObject>,
        now: Instant,
        zone: ZoneId,
        refreshFailed: Boolean = false
    ): Model {
        val today = now.atZone(zone).toLocalDate()
        val signals = NativeCoachSignals.fromDays(days, today, zone)
        val readiness = NativeCoachReadiness.calculate(signals.recovery, signals.daily, history, now, zone)
        val partial = signals.daily.optBoolean("partial", false) ||
            signals.recovery.optBoolean("partial", false)
        val older = signals.recovery.optString("day").let { stamp ->
            runCatching { LocalDate.parse(stamp) }.getOrNull()?.let { it < today } == true
        }
        val unverifiedEnergy = (days.any { row ->
            row.has("energyScore") && !row.isNull("energyScore")
        } && readiness.samsungScore == null)

        if (readiness.samsungScore != null) {
            return Model(Source.SAMSUNG, readiness.samsungScore, readiness.samsungTime,
                null, partial, false, refreshFailed)
        }

        // NativeCoachReadiness currently rejects exactly zero (<= 0). Preserve
        // its *existing* source, time, today, and 36h validators by invoking it
        // with a positive validation probe on a defensive copy. Only a genuine
        // finite zero is subsequently presented as zero; never as a Coach score.
        // Remove this compatibility shim if the shared validator accepts zero.
        for (record in listOf(signals.daily, signals.recovery)) {
            val raw = record.opt("energyScore")
            val numeric = when (raw) {
                is Number -> raw.toDouble()
                is String -> raw.trim().toDoubleOrNull()
                else -> null
            }
            if (numeric == null || !numeric.isFinite() || numeric != 0.0) continue
            val probe = JSONObject(record.toString()).put("energyScore", 1.0)
            val verification = NativeCoachReadiness.calculate(
                signals.recovery, signals.daily, history, now, zone,
                externalEnergy = probe, externalEnergyAvailable = true)
            if (verification.samsungScore == 1.0) {
                return Model(Source.SAMSUNG, 0.0, verification.samsungTime,
                    null, partial, false, refreshFailed)
            }
        }

        if (readiness.trainpilotScore != null && readiness.trainpilotObservations > 0) {
            return Model(Source.TRAINPILOT_ESTIMATE, readiness.trainpilotScore.toDouble(),
                null, now, partial, older || unverifiedEnergy, refreshFailed)
        }
        return Model(Source.NONE, null, null, null, partial, older || unverifiedEnergy, refreshFailed)
    }
}

/** Exact-result admission across cancel/reload/locale/date/zone and restore invalidations. */
internal class NativeHomeRecoveryLoadGate {
    private val generation = java.util.concurrent.atomic.AtomicLong()
    fun next(): Long = generation.incrementAndGet()
    fun isCurrent(ticket: Long): Boolean = ticket == generation.get()
}
