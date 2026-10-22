package com.repforge.app

import org.json.JSONObject
import java.util.TreeMap
import java.util.TreeSet
import kotlin.math.max
import kotlin.math.min

/** A daily activity source is selected, never summed with another overlapping source. */
internal object HealthDailySource {
    const val SAMSUNG: String = "com.sec.android.app.shealth"

    @JvmStatic
    fun choose(requested: String, preference: String, available: Set<String>): String {
        if (requested.isNotEmpty()) return requested
        if ("auto" == preference || "priority" == preference) return ""
        return preference
    }

    @JvmStatic
    fun mostComplete(totals: Map<String?, Long?>): String {
        var best = ""
        var bestValue = -1L
        val sorted = TreeMap<String, Long>()
        for ((k, v) in totals) {
            if (!k.isNullOrEmpty() && v != null && v >= 0L) {
                sorted[k] = v
            }
        }
        for ((k, v) in sorted) {
            if (v > bestValue) {
                best = k
                bestValue = v
            }
        }
        return best
    }
}

/** Select values by provider, never sum two copies of the same health data. */
internal object HealthProviderProjection {
    const val SAMSUNG: String = "samsung_health"

    @JvmField
    val METRICS: Array<String> = arrayOf(
        "steps", "activeCalories", "totalCalories", "distanceMeters",
        "averageHeartRate", "minHeartRate", "maxHeartRate", "heartRateSamples", "restingHeartRate",
        "weightKg", "bodyFatPercent", "bodyFatMassKg", "skeletalMuscleMassKg", "fatFreeMassKg",
        "totalBodyWaterLiters", "basalMetabolicRateKcal", "bodyMassIndex", "oxygenSaturationPercent",
        "hrvRmssdMs", "vo2Max", "bloodPressureSystolic", "bloodPressureDiastolic",
        "bloodGlucoseMmolL", "respiratoryRate", "energyScore", "sleepScore"
    )

    @JvmStatic
    @Throws(Exception::class)
    fun day(
        hc: JSONObject?,
        samsung: JSONObject?,
        legacy: JSONObject?,
        preferred: String?
    ): JSONObject {
        val fallback = hc ?: legacy
        val direct = SAMSUNG == preferred && samsung != null
        val base = if (direct) samsung else fallback
        val out = if (base == null) JSONObject() else JSONObject(base.toString())
        val channel = when {
            direct -> SAMSUNG
            hc != null -> HealthJournalStore.HC
            else -> HealthJournalStore.LEGACY
        }
        val sources = JSONObject()
        for (key in METRICS) {
            if (!out.isNull(key)) {
                sources.put(key, channel)
            } else if (direct && fallback != null && !fallback.isNull(key)) {
                out.put(key, fallback.get(key))
                sources.put(key, if (hc != null) HealthJournalStore.HC else HealthJournalStore.LEGACY)
                val prefix = when (key) {
                    "weightKg" -> "weight"
                    "bodyFatPercent" -> "bodyFat"
                    "oxygenSaturationPercent" -> "oxygenSaturation"
                    "hrvRmssdMs" -> "hrv"
                    else -> key
                }
                for (suffix in arrayOf("Time", "Source")) {
                    if (fallback.has(prefix + suffix)) {
                        out.put(prefix + suffix, fallback.get(prefix + suffix))
                    }
                }
            }
        }
        val outSleep = out.optJSONArray("sleepSessions")
        val fallbackSleep = fallback?.optJSONArray("sleepSessions")
        if (direct && (outSleep == null || outSleep.length() == 0) && fallbackSleep != null) {
            out.put("sleepSessions", fallbackSleep)
            sources.put("sleepSessions", if (hc != null) HealthJournalStore.HC else HealthJournalStore.LEGACY)
        } else if (outSleep != null && outSleep.length() > 0) {
            sources.put("sleepSessions", channel)
        }
        out.put("channel", channel)
        out.put("metricProviders", sources)
        if (!out.isNull("steps")) {
            val stepsProvider = sources.optString("steps", channel)
            out.put("stepsSource", stepsProvider)
            val stepDay = if (stepsProvider == SAMSUNG) samsung else fallback
            out.put("stepsOrigin", stepDay?.optString("activityOrigin", "").orEmpty())
        }
        return out
    }
}

/** Raw energy tied to external exercise sessions, never the daily/BMR aggregate. */
internal object HealthWorkoutEnergy {
    const val SAMSUNG: String = "com.sec.android.app.shealth"
    const val EDGE_TOLERANCE_MS: Long = 180_000L

    class Session(
        @JvmField val source: String,
        @JvmField val start: Long,
        @JvmField val end: Long
    )

    class EnergyRow(
        @JvmField val id: String,
        @JvmField val source: String?,
        @JvmField val start: Long,
        @JvmField val end: Long,
        @JvmField val kcal: Double,
        @JvmField val total: Boolean,
        @JvmField val modified: Long
    )

    class Result {
        @JvmField var calories: Double? = null
        @JvmField var coverageMs: Long = 0L
        @JvmField var prorated: Boolean = false
        @JvmField val sources: MutableSet<String> = TreeSet()
        @JvmField val types: MutableSet<String> = TreeSet()
        @JvmField val recordIds: MutableSet<String> = TreeSet()
    }

    @JvmStatic
    fun match(
        sessions: List<Session>,
        rows: List<EnergyRow>,
        start: Long,
        end: Long,
        ownPackage: String
    ): Result {
        val result = Result()
        if (end <= start) return result
        val candidates = ArrayList<EnergyRow>()
        val unique = HashMap<String, EnergyRow>()
        for (row in rows) {
            if (row.source.isNullOrEmpty() ||
                row.source == ownPackage ||
                row.end <= row.start ||
                row.end <= start ||
                row.start >= end ||
                !row.kcal.isFinite() ||
                row.kcal < 0.0
            ) {
                continue
            }
            val key = "${row.source}|${row.total}|${row.id}"
            val previous = unique[key]
            if (previous == null || previous.modified < row.modified) {
                unique[key] = row
            }
        }
        for (row in unique.values) {
            for (session in sessions) {
                val overlap = max(0L, min(row.end, session.end) - max(row.start, session.start))
                if (row.source == session.source &&
                    session.end > session.start &&
                    row.start >= session.start - EDGE_TOLERANCE_MS &&
                    row.end <= session.end + EDGE_TOLERANCE_MS &&
                    overlap >= (row.end - row.start) * 0.9
                ) {
                    candidates.add(row)
                    break
                }
            }
        }
        val boundaries = TreeSet<Long>()
        boundaries.add(start)
        boundaries.add(end)
        for (row in candidates) {
            boundaries.add(max(start, row.start))
            boundaries.add(min(end, row.end))
        }
        for (session in sessions) {
            if (session.start < end && session.end > start) {
                boundaries.add(max(start, session.start))
                boundaries.add(min(end, session.end))
            }
        }
        val edges = ArrayList(boundaries)
        val used = HashMap<EnergyRow, Long>()
        val priority = Comparator<EnergyRow> { r1, r2 ->
            val s1 = r1.source != SAMSUNG
            val s2 = r2.source != SAMSUNG
            if (s1 != s2) return@Comparator s1.compareTo(s2)
            val srcCmp = (r1.source ?: "").compareTo(r2.source ?: "")
            if (srcCmp != 0) return@Comparator srcCmp
            val t1 = !r1.total
            val t2 = !r2.total
            if (t1 != t2) return@Comparator t1.compareTo(t2)
            val durCmp = (r1.end - r1.start).compareTo(r2.end - r2.start)
            if (durCmp != 0) return@Comparator durCmp
            val modCmp = r2.modified.compareTo(r1.modified)
            if (modCmp != 0) return@Comparator modCmp
            r1.id.compareTo(r2.id)
        }

        var calories = 0.0
        for (i in 1 until edges.size) {
            val a = edges[i - 1]
            val b = edges[i]
            var best: EnergyRow? = null
            for (row in candidates) {
                if (row.start > a || row.end < b) continue
                var inSession = false
                for (session in sessions) {
                    if (row.source == session.source && session.start <= a && session.end >= b) {
                        inSession = true
                        break
                    }
                }
                if (inSession && (best == null || priority.compare(row, best) < 0)) {
                    best = row
                }
            }
            if (best == null) continue
            calories += best.kcal * ((b - a).toDouble() / (best.end - best.start).toDouble())
            result.coverageMs += b - a
            result.sources.add(best.source!!)
            result.types.add(if (best.total) "total" else "active")
            result.recordIds.add("${best.source}|${best.id}")
            used[best] = (used[best] ?: 0L) + (b - a)
        }
        if (used.isNotEmpty()) {
            result.calories = calories
        }
        for ((row, usedMs) in used) {
            if (usedMs < row.end - row.start) {
                result.prorated = true
            }
        }
        return result
    }
}
