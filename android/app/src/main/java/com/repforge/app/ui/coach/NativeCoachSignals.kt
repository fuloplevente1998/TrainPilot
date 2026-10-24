package com.repforge.app.ui.coach

import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Accepted rf245Recovery/rf250SleepSpans/rf229ApplyLatestRecovery, from real journal days. */
internal object NativeCoachSignals {
    data class Snapshot(val recovery: JSONObject, val daily: JSONObject)
    fun fromDays(days: List<JSONObject>, today: LocalDate, zone: ZoneId): Snapshot {
        val valid = days.mapNotNull { day -> runCatching { LocalDate.parse(day.getString("day")) to day }.getOrNull() }
            .filter { it.first <= today }.sortedByDescending { it.first }
        val sessions = valid.flatMap { (_, day) ->
            val rows = day.optJSONArray("sleepSessions")
            (0 until (rows?.length() ?: 0)).mapNotNull { rows?.optJSONObject(it) }
        }.distinctBy { listOf(it.optString("source"), it.optString("start"), it.optString("end")) }
            .groupBy { parse(it.optString("end"))?.atZone(zone)?.toLocalDate() }
        val recoveryRows = valid.map { (date, input) ->
            val row = JSONObject(input.toString())
            val sleep = sessions[date].orEmpty()
            val source = sleep.firstOrNull { it.optString("source") == "com.sec.android.app.shealth" }?.optString("source")
                ?: sleep.maxByOrNull { parse(it.optString("end")) ?: Instant.MIN }?.optString("source")
            val spans = sleep.filter { it.optString("source") == source }.flatMap { session ->
                val stages = session.optJSONArray("stages")
                val asleep = (0 until (stages?.length() ?: 0)).mapNotNull { i ->
                    stages?.optJSONObject(i)?.takeIf { it.optBoolean("isAsleep") }?.let { span(it) }
                }
                if (asleep.isNotEmpty() || session.optString("provider") == "samsung_health" && (stages?.length() ?: 0) > 0) asleep
                else listOfNotNull(span(session))
            }.sortedBy { it.first }
            val merged = ArrayList<Pair<Instant, Instant>>()
            for (span in spans) {
                val last = merged.lastOrNull()
                if (last != null && span.first <= last.second) merged[merged.lastIndex] = last.first to maxOf(last.second, span.second)
                else merged.add(span)
            }
            row.put("sleepMinutes", if (merged.isEmpty()) JSONObject.NULL else merged.sumOf { Duration.between(it.first, it.second).toMillis() / 60000.0 })
                .put("sleepStart", merged.firstOrNull()?.first?.toString() ?: JSONObject.NULL)
                .put("sleepEnd", merged.lastOrNull()?.second?.toString() ?: JSONObject.NULL)
                .put("sleepSource", source ?: JSONObject.NULL)
            date to row
        }
        val latest = recoveryRows.firstOrNull { (_, row) -> listOf("sleepMinutes", "hrvRmssdMs", "energyScore").any { number(row, it) != null } }
        val recovery = latest?.let { (date, row) ->
            val baseline = recoveryRows.filter { (d, previous) -> d < date && d >= date.minusDays(7) &&
                previous.optString("hrvSource") == row.optString("hrvSource") }
                .mapNotNull { number(it.second, "hrvRmssdMs")?.takeIf { n -> n > 0 } }.sorted()
            val median = if (baseline.size < 3) null else if (baseline.size % 2 == 1) baseline[baseline.size / 2]
                else (baseline[baseline.size / 2 - 1] + baseline[baseline.size / 2]) / 2
            row.put("hrvBaselineMs", median ?: JSONObject.NULL).put("baselineSamples", baseline.size)
        } ?: JSONObject()
        return Snapshot(recovery, valid.firstOrNull { it.first == today }?.second?.let { JSONObject(it.toString()) } ?: JSONObject())
    }
    private fun parse(value: String) = runCatching { Instant.parse(value) }.getOrNull()
    private fun span(row: JSONObject): Pair<Instant, Instant>? {
        val start = parse(row.optString("start")) ?: return null
        val end = parse(row.optString("end")) ?: return null
        return (start to end).takeIf { end > start }
    }
    private fun number(row: JSONObject, key: String): Double? = if (row.isNull(key)) null else row.optDouble(key, Double.NaN).takeIf { it.isFinite() }
}
