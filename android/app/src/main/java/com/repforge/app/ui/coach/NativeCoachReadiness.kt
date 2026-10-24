package com.repforge.app.ui.coach

import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Port of accepted main rf245RecentHistory/rf220EffortLoad/rf220Readiness/tp184EnergyToday.
 * Inputs must come from real provider readings or the persisted journal. No simulated defaults.
 * Samsung daily energy and TrainPilot's estimate remain separate calculations.
 */
internal object NativeCoachReadiness {
    data class Result(val score: Double?, val trainpilotScore: Int?, val samsungScore: Double?,
        val samsungTime: String?, val samsungSource: String?, val sleepMinutes: Double?, val hrvMs: Double?,
        val baselineMs: Double?, val load: Int?, val trainpilotObservations: Int, val pain: Boolean) {
        val mode: String get() = when { score == null -> "unknown"; score < 55 -> "light"; score < 75 -> "reduced"; else -> "normal" }
        val hasRecoverySignal: Boolean get() = samsungScore != null || sleepMinutes != null ||
            (hrvMs != null && hrvMs > 0 && baselineMs != null && baselineMs > 0)
    }

    fun calculate(recovery: JSONObject, daily: JSONObject, history: List<JSONObject>, now: Instant,
        zone: ZoneId = ZoneId.systemDefault(), externalEnergy: JSONObject? = null,
        externalEnergyAvailable: Boolean = false): Result {
        val recent = history.mapNotNull { row -> historyTime(row, zone)?.let { it to row } }
            .filter { (time, _) -> !time.isAfter(now) && Duration.between(time, now) <= Duration.ofDays(14) }
            .sortedByDescending { it.first }.take(3).map { it.second }
        var load = 100
        var loadParts = 0
        var pain = false
        for (workout in recent) {
            val exercises = workout.optJSONArray("exercises") ?: continue
            for (i in 0 until exercises.length()) {
                val exercise = exercises.optJSONObject(i) ?: continue
                if (exercise.optString("effort") == "pain") pain = true
                val effort = exercise.optString("effort").ifBlank { workout.optJSONObject("feedback")?.optString("effort").orEmpty() }
                if (effort.isBlank()) continue
                loadParts++
                load += when (effort) { "easy" -> 3; "light" -> 1; "challenging" -> -4; "hard" -> -8; "pain" -> -20; else -> 0 }
            }
        }
        load = load.coerceIn(20, 100)
        val sleep = if (fresh(recovery.optString("sleepEnd"), recovery.optString("day"), now, zone)) number(recovery, "sleepMinutes") else null
        val hrv = if (fresh(recovery.optString("hrvTime"), recovery.optString("day"), now, zone)) number(recovery, "hrvRmssdMs") else null
        val baseline = number(recovery, "hrvBaselineMs")
        var own = 70.0
        var parts = 0
        if (sleep != null) { parts++; own += when { sleep >= 450 -> 12; sleep >= 420 -> 8; sleep >= 360 -> 1; else -> -14 } }
        if (hrv != null && hrv > 0 && baseline != null && baseline > 0) {
            parts++
            val delta = (hrv - baseline) / baseline
            own += when { delta >= .15 -> 10; delta <= -.2 -> -14; delta <= -.1 -> -6; else -> 3 }
        }
        if (loadParts > 0) { parts++; own += (load - 70) * .35 }
        if (pain) own -= 12
        val ownScore = if (parts > 0) own.coerceIn(25.0, 100.0).roundToInt() else null
        val energy = if (externalEnergyAvailable) validEnergy(externalEnergy, now, zone)
            else validEnergy(daily, now, zone) ?: validEnergy(recovery, now, zone)
        var score = energy?.first ?: ownScore?.toDouble()
        if (energy != null && score != null) {
            if (pain || loadParts > 0 && load < 55) score = minOf(score, 54.0)
            else if (loadParts > 0 && load < 70) score = minOf(score, 74.0)
        }
        return Result(score, ownScore, energy?.first, energy?.second, energy?.third, sleep, hrv, baseline,
            if (loadParts > 0) load else null, parts, pain)
    }

    /** An explicit invalid timestamp never falls back to a different date. */
    private fun fresh(time: String, day: String, now: Instant, zone: ZoneId): Boolean {
        val instant = if (time.isNotBlank()) parse(time, zone) else runCatching { LocalDate.parse(day).atStartOfDay(zone).toInstant() }.getOrNull()
        return instant != null && !instant.isAfter(now) && Duration.between(instant, now) <= Duration.ofHours(36)
    }

    private fun validEnergy(reading: JSONObject?, now: Instant, zone: ZoneId): Triple<Double, String, String>? {
        reading ?: return null
        val value = number(reading, "energyScore") ?: return null
        if (value <= 0 || value > 100) return null
        val source = reading.optString("energyScoreSource").ifBlank { reading.optJSONObject("metricProviders")?.optString("energyScore").orEmpty() }
        if (source !in setOf("samsung_health", "com.sec.android.app.shealth")) return null
        val stamp = reading.optString("energyScoreTime")
        val time = parse(stamp, zone) ?: return null
        val today = now.atZone(zone).toLocalDate()
        val day = reading.optString("day")
        if (!fresh(stamp, "", now, zone) || time.atZone(zone).toLocalDate() != today || day.isNotBlank() && day != today.toString()) return null
        return Triple(value, stamp, source)
    }

    private fun historyTime(row: JSONObject, zone: ZoneId): Instant? {
        for (key in listOf("finished", "started", "endedAt", "finishedAt", "date", "startedAt", "start"))
            parse(row.optString(key), zone)?.let { return it }
        return null
    }
    private fun parse(value: String, zone: ZoneId): Instant? =
        runCatching { Instant.parse(value) }.getOrNull()
            ?: if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(value))
                runCatching { LocalDate.parse(value).atStartOfDay(ZoneId.of("UTC")).toInstant() }.getOrNull()
            else runCatching { java.time.LocalDateTime.parse(value).atZone(zone).toInstant() }.getOrNull()
    private fun number(row: JSONObject, key: String): Double? {
        if (row.isNull(key)) return null
        val raw = row.opt(key)
        val n = when (raw) { is Number -> raw.toDouble(); is String -> raw.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull(); else -> null }
        return n?.takeIf { it.isFinite() }
    }
}
