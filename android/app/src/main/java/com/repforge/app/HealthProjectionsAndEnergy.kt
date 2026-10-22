package com.repforge.app

import org.json.JSONObject
import java.util.TreeMap
import java.util.TreeSet
import kotlin.math.max
import kotlin.math.min

internal object HealthDailySource {
    const val SAMSUNG: String = "com.sec.android.app.shealth"
    @JvmStatic fun choose(requested: String, preference: String, available: Set<String>): String =
        if (requested.isNotEmpty()) requested else if ("auto" == preference || "priority" == preference) "" else preference
    @JvmStatic fun mostComplete(totals: Map<String?, Long?>): String {
        var best = ""; var maxVal = -1L
        val sorted = TreeMap<String, Long>()
        for ((k, v) in totals) if (!k.isNullOrEmpty() && v != null && v >= 0L) sorted[k] = v
        for ((k, v) in sorted) if (v > maxVal) { best = k; maxVal = v }
        return best
    }
}

internal object HealthProviderProjection {
    const val SAMSUNG: String = "samsung_health"
    @JvmField val METRICS = arrayOf("steps", "activeCalories", "totalCalories", "distanceMeters", "averageHeartRate", "minHeartRate", "maxHeartRate", "heartRateSamples", "restingHeartRate", "weightKg", "bodyFatPercent", "bodyFatMassKg", "skeletalMuscleMassKg", "fatFreeMassKg", "totalBodyWaterLiters", "basalMetabolicRateKcal", "bodyMassIndex", "oxygenSaturationPercent", "hrvRmssdMs", "vo2Max", "bloodPressureSystolic", "bloodPressureDiastolic", "bloodGlucoseMmolL", "respiratoryRate", "energyScore", "sleepScore")
    @JvmStatic @Throws(Exception::class) fun day(hc: JSONObject?, samsung: JSONObject?, legacy: JSONObject?, preferred: String?): JSONObject {
        val fallback = hc ?: legacy; val direct = SAMSUNG == preferred && samsung != null
        val base = if (direct) samsung else fallback; val out = if (base == null) JSONObject() else JSONObject(base.toString())
        val channel = if (direct) SAMSUNG else if (hc != null) HealthJournalStore.HC else HealthJournalStore.LEGACY
        val sources = JSONObject()
        for (k in METRICS) {
            if (!out.isNull(k)) sources.put(k, channel)
            else if (direct && fallback != null && !fallback.isNull(k)) {
                out.put(k, fallback.get(k)); sources.put(k, if (hc != null) HealthJournalStore.HC else HealthJournalStore.LEGACY)
                val p = when (k) { "weightKg" -> "weight"; "bodyFatPercent" -> "bodyFat"; "oxygenSaturationPercent" -> "oxygenSaturation"; "hrvRmssdMs" -> "hrv"; else -> k }
                for (s in arrayOf("Time", "Source")) if (fallback.has(p + s)) out.put(p + s, fallback.get(p + s))
            }
        }
        val outS = out.optJSONArray("sleepSessions"); val fallS = fallback?.optJSONArray("sleepSessions")
        if (direct && (outS == null || outS.length() == 0) && fallS != null) { out.put("sleepSessions", fallS); sources.put("sleepSessions", if (hc != null) HealthJournalStore.HC else HealthJournalStore.LEGACY) }
        else if (outS != null && outS.length() > 0) sources.put("sleepSessions", channel)
        out.put("channel", channel).put("metricProviders", sources)
        if (!out.isNull("steps")) { val prov = sources.optString("steps", channel); out.put("stepsSource", prov); val sd = if (prov == SAMSUNG) samsung else fallback; out.put("stepsOrigin", sd?.optString("activityOrigin", "").orEmpty()) }
        return out
    }
}

internal object HealthWorkoutEnergy {
    const val SAMSUNG: String = "com.sec.android.app.shealth"; const val EDGE_TOLERANCE_MS: Long = 180_000L
    class Session(@JvmField val source: String, @JvmField val start: Long, @JvmField val end: Long)
    class EnergyRow(@JvmField val id: String, @JvmField val source: String?, @JvmField val start: Long, @JvmField val end: Long, @JvmField val kcal: Double, @JvmField val total: Boolean, @JvmField val modified: Long)
    class Result { @JvmField var calories: Double? = null; @JvmField var coverageMs: Long = 0L; @JvmField var prorated: Boolean = false; @JvmField val sources: MutableSet<String> = TreeSet(); @JvmField val types: MutableSet<String> = TreeSet(); @JvmField val recordIds: MutableSet<String> = TreeSet() }
    @JvmStatic fun match(sessions: List<Session>, rows: List<EnergyRow>, start: Long, end: Long, ownPkg: String): Result {
        val res = Result(); if (end <= start) return res
        val cands = ArrayList<EnergyRow>(); val uniq = HashMap<String, EnergyRow>()
        for (r in rows) { if (r.source.isNullOrEmpty() || r.source == ownPkg || r.end <= r.start || r.end <= start || r.start >= end || !r.kcal.isFinite() || r.kcal < 0) continue; val k = "${r.source}|${r.total}|${r.id}"; val prev = uniq[k]; if (prev == null || prev.modified < r.modified) uniq[k] = r }
        for (r in uniq.values) for (s in sessions) { val ov = max(0L, min(r.end, s.end) - max(r.start, s.start)); if (r.source == s.source && s.end > s.start && r.start >= s.start - EDGE_TOLERANCE_MS && r.end <= s.end + EDGE_TOLERANCE_MS && ov >= (r.end - r.start) * 0.9) { cands.add(r); break } }
        val bounds = TreeSet<Long>().apply { add(start); add(end); cands.forEach { add(max(start, it.start)); add(min(end, it.end)) }; sessions.forEach { if (it.start < end && it.end > start) { add(max(start, it.start)); add(min(end, it.end)) } } }
        val edges = ArrayList(bounds); val used = HashMap<EnergyRow, Long>()
        val prio = Comparator<EnergyRow> { a, b -> if ((a.source != SAMSUNG) != (b.source != SAMSUNG)) (a.source != SAMSUNG).compareTo(b.source != SAMSUNG) else if (a.source != b.source) (a.source ?: "").compareTo(b.source ?: "") else if (!a.total != !b.total) (!a.total).compareTo(!b.total) else if ((a.end - a.start) != (b.end - b.start)) (a.end - a.start).compareTo(b.end - b.start) else if (a.modified != b.modified) b.modified.compareTo(a.modified) else a.id.compareTo(b.id) }
        var cal = 0.0
        for (i in 1 until edges.size) {
            val a = edges[i - 1]; val b = edges[i]; var best: EnergyRow? = null
            for (r in cands) { if (r.start > a || r.end < b) continue; if (sessions.any { r.source == it.source && it.start <= a && it.end >= b } && (best == null || prio.compare(r, best) < 0)) best = r }
            if (best != null) { cal += best.kcal * ((b - a).toDouble() / (best.end - best.start)); res.coverageMs += b - a; res.sources.add(best.source!!); res.types.add(if (best.total) "total" else "active"); res.recordIds.add("${best.source}|${best.id}"); used[best] = (used[best] ?: 0L) + (b - a) }
        }
        if (used.isNotEmpty()) res.calories = cal
        for ((r, u) in used) if (u < r.end - r.start) res.prorated = true
        return res
    }
}
