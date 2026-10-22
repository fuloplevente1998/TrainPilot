package com.repforge.shared

import org.json.JSONArray
import org.json.JSONObject
import java.util.TreeSet

/**
 * #18: Deterministic legacy ID generator (tp18-h-... and tp18-w-...) and
 * Google Calendar rf[0-9a-f]{64} event ID generator with 100% JS parity.
 */
object LegacySyncIdentity18 {
    private const val SEED_1: Long = 2166136261L
    private const val SEED_2: Long = 2246822519L
    private const val SEED_3: Long = 3266489917L

    /**
     * Exact port of JS:
     * const hash32=(s,seed)=>{let h=seed>>>0;for(let i=0;i<s.length;i++){h^=s.charCodeAt(i);h=Math.imul(h,16777619)>>>0;h^=h>>>13;}return h.toString(16).padStart(8,'0')};
     */
    @JvmStatic
    fun hash32(s: String, seed: Long): String {
        var h = (seed and 0xFFFFFFFFL).toInt()
        for (i in 0 until s.length) {
            h = h xor s[i].code
            h = h * 16777619 // 32-bit signed/unsigned wraparound is identical to Math.imul(h, 16777619) >>> 0
            h = h xor (h ushr 13)
        }
        return Integer.toUnsignedString(h, 16).padStart(8, '0')
    }

    /** Deterministic key-sorted JSON serialization matching JS canonical(). */
    @JvmStatic
    fun canonical(value: Any?): String {
        return when (value) {
            null, JSONObject.NULL -> "null"
            is Boolean -> value.toString()
            is Number -> {
                val d = value.toDouble()
                if (!d.isFinite()) "null"
                else if (d == d.toLong().toDouble()) d.toLong().toString()
                else d.toString()
            }
            is String -> JSONObject.quote(value)
            is JSONArray -> {
                val sb = StringBuilder("[")
                for (i in 0 until value.length()) {
                    if (i > 0) sb.append(',')
                    sb.append(canonical(value.opt(i)))
                }
                sb.append(']').toString()
            }
            is JSONObject -> {
                val keys = TreeSet<String>()
                val iter = value.keys()
                while (iter.hasNext()) {
                    val k = iter.next()
                    val v = value.opt(k)
                    if (v != null) keys.add(k)
                }
                val sb = StringBuilder("{")
                var first = true
                for (k in keys) {
                    if (!first) sb.append(',')
                    first = false
                    sb.append(JSONObject.quote(k)).append(':').append(canonical(value.opt(k)))
                }
                sb.append('}').toString()
            }
            else -> JSONObject.quote(value.toString())
        }
    }

    /**
     * Exact port of JS workoutIdentity(x):
     * canonical({
     *   started: x?.started || '',
     *   workout: x?.workout || '',
     *   exercises: (x?.exercises || []).map(e => ({
     *     id: e?.id || '',
     *     sets: (e?.sets || []).map(s => ({
     *       set: s?.set ?? null, weight: s?.weight ?? null, reps: s?.reps ?? null,
     *       leftSeconds: s?.leftSeconds ?? null, rightSeconds: s?.rightSeconds ?? null,
     *       done: !!s?.done
     *     }))
     *   }))
     * })
     */
    @JvmStatic
    fun workoutIdentity(workout: JSONObject?): String {
        val root = JSONObject()
        root.put("started", workout?.optString("started", "") ?: "")
        root.put("workout", workout?.optString("workout", workout.optString("dayId", "")) ?: "")
        val exArray = JSONArray()
        val srcExercises = workout?.optJSONArray("exercises")
        if (srcExercises != null) {
            for (i in 0 until srcExercises.length()) {
                val e = srcExercises.optJSONObject(i)
                val exObj = JSONObject()
                exObj.put("id", e?.optString("id", "") ?: "")
                val setsArray = JSONArray()
                val srcSets = e?.optJSONArray("sets")
                if (srcSets != null) {
                    for (j in 0 until srcSets.length()) {
                        val s = srcSets.optJSONObject(j)
                        val setObj = JSONObject()
                        setObj.put("set", if (s != null && s.has("set") && !s.isNull("set")) s.get("set") else JSONObject.NULL)
                        setObj.put("weight", if (s != null && s.has("weight") && !s.isNull("weight")) s.get("weight") else JSONObject.NULL)
                        setObj.put("reps", if (s != null && s.has("reps") && !s.isNull("reps")) s.get("reps") else JSONObject.NULL)
                        setObj.put("leftSeconds", if (s != null && s.has("leftSeconds") && !s.isNull("leftSeconds")) s.get("leftSeconds") else JSONObject.NULL)
                        setObj.put("rightSeconds", if (s != null && s.has("rightSeconds") && !s.isNull("rightSeconds")) s.get("rightSeconds") else JSONObject.NULL)
                        setObj.put("done", s?.optBoolean("done", false) ?: false)
                        setsArray.put(setObj)
                    }
                }
                exObj.put("sets", setsArray)
                exArray.put(exObj)
            }
        }
        root.put("exercises", exArray)
        return canonical(root)
    }

    /**
     * Exact port of JS weightIdentity(x):
     * canonical({ date: x?.date || '', kg: x?.kg ?? null })
     */
    @JvmStatic
    fun weightIdentity(weight: JSONObject?): String {
        val root = JSONObject()
        root.put("date", weight?.optString("date", "") ?: "")
        root.put("kg", if (weight != null && weight.has("kg") && !weight.isNull("kg")) weight.get("kg") else JSONObject.NULL)
        return canonical(root)
    }

    @JvmStatic
    fun legacyHistoryId(workout: JSONObject): String {
        val s = workoutIdentity(workout)
        return "tp18-h-" + hash32(s, SEED_1) + hash32(s, SEED_2) + hash32(s, SEED_3)
    }

    @JvmStatic
    fun legacyWeightId(weight: JSONObject): String {
        val s = weightIdentity(weight)
        return "tp18-w-" + hash32(s, SEED_1) + hash32(s, SEED_2) + hash32(s, SEED_3)
    }

    @JvmStatic
    fun isSyntheticId(kind: String, id: String?): Boolean {
        if (id.isNullOrEmpty()) return false
        val prefix = if (kind == "history") "tp18-h-" else "tp18-w-"
        return id.startsWith(prefix)
    }

    @JvmStatic
    fun isLegacyProtectedId(id: String?): Boolean {
        return id.isNullOrEmpty() || id.startsWith("tp18-")
    }

    @JvmStatic
    fun calendarEventId(scheduleId: String, date: String, programId: String, dayId: String): String {
        return "rf" + GpsRoutePacket193.digest("$scheduleId|$date|$programId|$dayId")
    }
}
