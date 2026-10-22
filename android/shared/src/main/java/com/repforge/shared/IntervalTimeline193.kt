package com.repforge.shared

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

/** Finite monotonic workout timeline; no wall clock or Android dependency. */
open class IntervalTimeline193 @JvmOverloads constructor(
    steps: List<Step>?,
    private val advance: Boolean,
    initialRestSeconds: Int = 0,
    private val restOnly: Boolean = false
) {
    companion object {
        const val MAX_MS: Long = 86_400_000L
        const val MAX_PAUSE_MS: Long = 604_800_000L
        const val MAX_REVISION: Long = 9_007_199_254_740_991L

        @JvmStatic
        @Throws(JSONException::class)
        fun version(data: JSONObject, field: String): Long {
            val value = data.get(field)
            if (value !is Int && value !is Long) {
                throw IllegalArgumentException("invalidVersion")
            }
            val result = (value as Number).toLong()
            if (result < 0L || result > MAX_REVISION) {
                throw IllegalArgumentException("invalidVersion")
            }
            return result
        }
    }

    class Step(
        @JvmField val exerciseId: String?,
        @JvmField val setId: String?,
        @JvmField val name: String?,
        @JvmField val work: Int,
        @JvmField val rest: Int,
        @JvmField val rounds: Int,
        @JvmField var gpsKey: String?
    ) {
        init {
            if (exerciseId.isNullOrEmpty() ||
                setId.isNullOrEmpty() ||
                work < 1 || work > 3600 ||
                rest < 0 || rest > 3600 ||
                rounds < 1 || rounds > 100
            ) {
                throw IllegalArgumentException("Invalid interval step")
            }
        }

        internal fun duration(): Long {
            return (work.toLong() * rounds + rest.toLong() * (rounds - 1)) * 1000L
        }
    }

    class Phase internal constructor(
        @JvmField val step: Int,
        @JvmField val round: Int,
        @JvmField val completed: Int,
        @JvmField val elapsedMs: Long,
        @JvmField val stepElapsedMs: Long,
        @JvmField val remainingMs: Long,
        @JvmField val totalMs: Long,
        @JvmField val phase: String
    ) {
        @JvmField
        val token: String = "$step:$round:$phase"
    }

    private val steps: List<Step>
    private val initialRest: Long
    private val total: Long

    init {
        if (steps == null || steps.isEmpty() || steps.size > 100) {
            throw IllegalArgumentException("Invalid interval plan")
        }
        if (initialRestSeconds < 0 ||
            initialRestSeconds > 3600 ||
            (restOnly && (steps.size != 1 || advance || initialRestSeconds != 0))
        ) {
            throw IllegalArgumentException("Invalid rest plan")
        }
        this.steps = steps
        this.initialRest = initialRestSeconds * 1000L
        var sum = this.initialRest
        for (i in steps.indices) {
            sum += duration(i)
            if (sum > MAX_MS) {
                throw IllegalArgumentException("Interval plan exceeds 24 hours")
            }
        }
        this.total = sum
    }

    fun duration(i: Int): Long {
        return if (restOnly) {
            steps[i].rest * 1000L
        } else {
            steps[i].duration() + if (advance && i < steps.size - 1) steps[i].rest * 1000L else 0L
        }
    }

    fun phaseDurationMs(p: Phase): Long {
        if ("work" == p.phase) return steps[p.step].work * 1000L
        if ("rest" != p.phase) return 0L
        if (!restOnly && p.elapsedMs < initialRest) return initialRest
        return steps[p.step].rest * 1000L
    }

    fun at(milliseconds: Long): Phase {
        val elapsed = max(0L, min(total, milliseconds))
        var offset = elapsed
        if (restOnly) {
            return Phase(
                0,
                1,
                if (elapsed == total) 1 else 0,
                elapsed,
                0L,
                total - elapsed,
                total,
                if (elapsed == total) "complete" else "rest"
            )
        }
        if (elapsed == total) {
            val i = steps.size - 1
            return Phase(
                i,
                steps[i].rounds,
                steps.size,
                elapsed,
                duration(i),
                0L,
                total,
                "complete"
            )
        }
        if (elapsed < initialRest) {
            return Phase(0, 1, 0, elapsed, 0L, initialRest - elapsed, total, "rest")
        }
        offset -= initialRest
        for (i in steps.indices) {
            val length = duration(i)
            if (offset >= length) {
                offset -= length
                continue
            }
            val s = steps[i]
            var left = offset
            for (round in 1..s.rounds) {
                val work = s.work * 1000L
                if (left < work) {
                    return Phase(i, round, i, elapsed, offset, work - left, total, "work")
                }
                left -= work
                if (round < s.rounds || (advance && i < steps.size - 1)) {
                    val rest = s.rest * 1000L
                    if (left < rest) {
                        return Phase(i, round, i, elapsed, offset, rest - left, total, "rest")
                    }
                    left -= rest
                }
            }
        }
        throw IllegalStateException("Unreachable interval phase")
    }
}

/** Actual phase time, separate from the virtual timeline position changed by Skip Rest. */
class IntervalTimeLedger193(count: Int, now: Long) {
    private val work = LongArray(count)
    private val rest = LongArray(count)
    private val paused = LongArray(count)
    private var position: Long = 0L
    private var sampledAt: Long = now
    private var wasPaused: Boolean = false

    companion object {
        @JvmStatic
        @Throws(JSONException::class)
        fun restore(plan: JSONArray, saved: JSONObject, now: Long): IntervalTimeLedger193 {
            val rows = saved.getJSONArray("steps")
            if (rows.length() != plan.length()) {
                throw IllegalArgumentException("Transferred ledger targets changed")
            }
            val value = IntervalTimeLedger193(plan.length(), now)
            var w = 0L
            var r = 0L
            var p = 0L
            for (i in 0 until plan.length()) {
                val owner = plan.getJSONObject(i)
                val row = rows.getJSONObject(i)
                if (owner.getString("exerciseId") != row.getString("exerciseId") ||
                    owner.getString("setId") != row.getString("setId")
                ) {
                    throw IllegalArgumentException("Transferred ledger targets changed")
                }
                value.work[i] = IntervalTimeline193.version(row, "workMs")
                value.rest[i] = IntervalTimeline193.version(row, "restMs")
                value.paused[i] = IntervalTimeline193.version(row, "pausedMs")
                w += value.work[i]
                r += value.rest[i]
                p += value.paused[i]
            }
            value.position = IntervalTimeline193.version(saved, "positionMs")
            if (value.position > IntervalTimeline193.MAX_MS ||
                w + r > value.position ||
                w != saved.getLong("workMs") ||
                r != saved.getLong("restMs") ||
                p != saved.getLong("pausedMs") ||
                p > IntervalTimeline193.MAX_PAUSE_MS
            ) {
                throw IllegalArgumentException("Transferred ledger counters changed")
            }
            return value
        }
    }

    fun sample(
        timeline: IntervalTimeline193,
        progress: Long,
        now: Long,
        running: Boolean,
        stopped: Boolean,
        interrupted: Boolean
    ) {
        if (now < sampledAt || progress < position) {
            throw IllegalArgumentException("Clock moved backwards")
        }
        if (wasPaused) {
            paused[timeline.at(position).step] += now - sampledAt
        }
        while (position < progress) {
            val p = timeline.at(position)
            if ("complete" == p.phase) break
            val duration = min(p.remainingMs, progress - position)
            if (duration <= 0L) throw IllegalStateException("Empty timeline phase")
            if ("work" == p.phase) {
                work[p.step] += duration
            } else {
                rest[p.step] += duration
            }
            position += duration
        }
        sampledAt = now
        wasPaused = !running && !stopped && !interrupted && "complete" != timeline.at(position).phase
    }

    fun skipTo(progress: Long) {
        if (progress < position) throw IllegalArgumentException("Invalid skip")
        position = progress
    }

    @Throws(JSONException::class)
    fun json(plan: JSONArray): JSONObject {
        val rows = JSONArray()
        var workTotal = 0L
        var restTotal = 0L
        var pauseTotal = 0L
        for (i in work.indices) {
            val owner = plan.getJSONObject(i)
            rows.put(
                JSONObject()
                    .put("exerciseId", owner.getString("exerciseId"))
                    .put("setId", owner.getString("setId"))
                    .put("workMs", work[i])
                    .put("restMs", rest[i])
                    .put("pausedMs", paused[i])
            )
            workTotal += work[i]
            restTotal += rest[i]
            pauseTotal += paused[i]
        }
        return JSONObject()
            .put("schema", 1)
            .put("owner", "phone")
            .put("positionMs", position)
            .put("workMs", workTotal)
            .put("restMs", restTotal)
            .put("pausedMs", pauseTotal)
            .put("steps", rows)
    }
}

/** One cue per countdown second; delayed callbacks never replay missed cues. */
class IntervalHapticCues193 {
    private var token: String = ""
    private var second: Long = -1L
    private var quietUntil: Long = -1L

    fun next(p: IntervalTimeline193.Phase, running: Boolean): Int {
        if (!running) return 0
        val remaining = (p.remainingMs + 999L) / 1000L
        if (p.token != token) {
            val transition = token.isNotEmpty()
            token = p.token
            second = remaining
            val duration = if (transition || "complete" == p.phase) {
                1000
            } else if (remaining in 1L..3L) {
                80
            } else {
                100
            }
            quietUntil = p.elapsedMs + duration
            return duration
        }
        if ("complete" != p.phase && remaining in 1L..3L && remaining != second) {
            second = remaining
            if (p.elapsedMs >= quietUntil) {
                quietUntil = p.elapsedMs + 80L
                return 80
            }
        }
        return 0
    }
}
