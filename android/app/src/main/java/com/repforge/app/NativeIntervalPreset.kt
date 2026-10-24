package com.repforge.app

import com.repforge.shared.IntervalTimeline193
import org.json.JSONObject

/** One portable configuration shape; execution remains in the existing native interval engine. */
internal object NativeIntervalPreset {
    fun normalized(source: JSONObject, defaultAutoAdvance: Boolean = true): JSONObject {
        fun integer(key: String, range: IntRange): Int {
            val value = PhoneWorkoutRecording.version(source, key)
            require(value in range.first.toLong()..range.last.toLong()) { "invalidTimerConfig" }
            return value.toInt()
        }
        val work = integer("work", 1..3600)
        val rest = integer("rest", 0..3600)
        val rounds = integer("rounds", 1..100)
        require((work.toLong() * rounds + rest.toLong() * (rounds - 1)) * 1000 <= IntervalTimeline193.MAX_MS) {
            "Interval plan exceeds 24 hours"
        }
        fun boolean(key: String, default: Boolean): Boolean {
            require(!source.has(key) || source.opt(key) is Boolean) { "invalidTimerConfig" }
            return source.optBoolean(key, default)
        }
        return JSONObject().put("work", work).put("rest", rest).put("rounds", rounds)
            .put("sound", boolean("sound", true)).put("vibration", boolean("vibration", true))
            .put("autoAdvance", boolean("autoAdvance", defaultAutoAdvance))
    }
}
