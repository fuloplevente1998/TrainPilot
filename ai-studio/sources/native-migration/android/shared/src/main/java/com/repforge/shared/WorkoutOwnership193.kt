package com.repforge.shared

import org.json.JSONObject
import java.time.Instant
import kotlin.math.max

/** Pure, fenced ownership transitions. Persist each result before sending it to the peer. */
object WorkoutOwnership193 {
    const val PATH: String = "/trainpilot/ownership193/"
    const val PROTOCOL: Int = 1

    private val ID_REGEX = Regex("[A-Za-z0-9._:-]{1,128}")

    @JvmStatic
    @Throws(Exception::class)
    fun copy(value: JSONObject): JSONObject = JSONObject(value.toString())

    @JvmStatic
    fun epoch(value: JSONObject?): Long = value?.optLong("ownerEpoch193") ?: 0L

    @JvmStatic
    fun transfer(value: JSONObject?): JSONObject? = value?.optJSONObject("handoff193")

    @JvmStatic
    fun pending(value: JSONObject?): Boolean {
        val h = transfer(value)
        return h != null && "committed" != h.optString("state")
    }

    @JvmStatic
    fun owns(value: JSONObject?, device: String): Boolean {
        return value != null &&
            value.optBoolean("active", true) &&
            !pending(value) &&
            device == value.optString("timerOwner", "phone")
    }

    private fun requireOwnership(value: Boolean) {
        if (!value) throw IllegalStateException("Ownership changed; retry the same transfer.")
    }

    private fun id(value: String?): Boolean {
        return value != null && ID_REGEX.matches(value)
    }

    private fun same(a: JSONObject?, b: JSONObject?): Boolean {
        return a != null &&
            b != null &&
            a.optInt("protocol") == PROTOCOL &&
            b.optInt("protocol") == PROTOCOL &&
            a.optLong("epoch") == b.optLong("epoch") &&
            a.optString("id") == b.optString("id") &&
            a.optString("source") == b.optString("source") &&
            a.optString("target") == b.optString("target") &&
            a.optString("sourceNode") == b.optString("sourceNode") &&
            a.optString("targetNode") == b.optString("targetNode")
    }

    @JvmStatic
    @Throws(Exception::class)
    fun offer(
        current: JSONObject?,
        source: String,
        sourceNode: String?,
        targetNode: String?,
        transferId: String?,
        expectedEpoch: Long,
        now: Long
    ): JSONObject {
        requireOwnership(
            ("phone" == source || "watch" == source) &&
                owns(current, source) &&
                current != null &&
                current.optString("workoutId").isNotEmpty() &&
                current.optString("workoutId").length <= 512 &&
                epoch(current) == expectedEpoch &&
                expectedEpoch >= 0L &&
                id(sourceNode) &&
                id(targetNode) &&
                sourceNode != targetNode &&
                id(transferId) &&
                now > 0L &&
                expectedEpoch < Long.MAX_VALUE &&
                current.optLong("pauseRevision") >= 0L &&
                current.optLong("pauseRevision") < Long.MAX_VALUE &&
                (transfer(current) == null || transferId != transfer(current)!!.optString("id"))
        )
        val nonNullCurrent = current!!
        val target = if ("phone" == source) "watch" else "phone"
        val result = copy(nonNullCurrent)
        result.put("ownershipProtocol193", PROTOCOL)
            .put("ownerEpoch193", expectedEpoch)
            .put("workoutPaused", true)
            .put("pauseRevision", nonNullCurrent.optLong("pauseRevision") + 1L)
            .put(
                "pausedRestMillis",
                max(
                    nonNullCurrent.optLong("pausedRestMillis"),
                    max(0L, nonNullCurrent.optLong("restEndAt") - now)
                )
            )
            .put("restEndAt", 0)

        val intervals = result.optJSONArray("activeIntervals")
        if (intervals != null) {
            for (i in 0 until intervals.length()) {
                val interval = intervals.optJSONObject(i)
                if (interval != null && (interval.isNull("end") || interval.optString("end").isEmpty())) {
                    interval.put("end", Instant.ofEpochMilli(now).toString())
                }
            }
        }
        val automatic = result.optJSONObject("automation")
        if (automatic != null) {
            var remaining = automatic.optLong("remainingMs")
            if (!automatic.optBoolean("paused") && automatic.optLong("deadlineAt") > 0L) {
                remaining = max(0L, automatic.optLong("deadlineAt") - now)
            }
            automatic.put("paused", true).put("deadlineAt", 0).put("remainingMs", remaining)
        }
        result.put(
            "handoff193",
            JSONObject()
                .put("protocol", PROTOCOL)
                .put("id", transferId)
                .put("source", source)
                .put("target", target)
                .put("sourceNode", sourceNode)
                .put("targetNode", targetNode)
                .put("epoch", expectedEpoch + 1L)
                .put("state", "offered")
                .put("createdAt", now)
        )
        return result
    }

    /** The target persists this paused snapshot before acknowledging the offer. */
    @JvmStatic
    @Throws(Exception::class)
    fun accept(current: JSONObject?, offer: JSONObject?, localNode: String?, peerNode: String?): JSONObject {
        val h = transfer(offer)
        requireOwnership(
            offer != null &&
                h != null &&
                h.optInt("protocol") == PROTOCOL &&
                "offered" == h.optString("state") &&
                localNode == h.optString("targetNode") &&
                peerNode == h.optString("sourceNode") &&
                id(h.optString("id")) &&
                offer.optBoolean("active", true) &&
                offer.optBoolean("workoutPaused") &&
                offer.optString("workoutId").isNotEmpty() &&
                offer.optString("workoutId").length <= 512 &&
                epoch(offer) >= 0L &&
                h.optLong("epoch") > 0L &&
                h.optLong("epoch") == epoch(offer) + 1L &&
                ("phone" == h.optString("source") || "watch" == h.optString("source")) &&
                h.optString("source") == offer.optString("timerOwner") &&
                (if ("phone" == h.optString("source")) "watch" else "phone") == h.optString("target") &&
                id(h.optString("sourceNode")) &&
                id(h.optString("targetNode")) &&
                localNode != peerNode
        )
        val nonNullOffer = offer!!
        val nonNullH = h!!
        if (current != null) {
            requireOwnership(current.optString("workoutId") == nonNullOffer.optString("workoutId"))
            if (same(transfer(current), nonNullH) && "accepted" == transfer(current)!!.optString("state")) {
                return copy(current)
            }
            requireOwnership(
                !pending(current) &&
                    epoch(current) < nonNullH.optLong("epoch") &&
                    current.optString("timerOwner", "phone") == nonNullH.optString("source")
            )
        }
        val result = copy(nonNullOffer)
        transfer(result)!!.put("state", "accepted")
        return result
    }

    /** Only the frozen source can commit, and duplicate accepts replay the same commit. */
    @JvmStatic
    @Throws(Exception::class)
    fun commit(frozen: JSONObject?, accepted: JSONObject?, localNode: String?, peerNode: String?): JSONObject {
        val h = transfer(frozen)
        val a = transfer(accepted)
        requireOwnership(
            frozen != null &&
                accepted != null &&
                same(h, a) &&
                frozen.optString("workoutId") == accepted.optString("workoutId") &&
                "accepted" == a!!.optString("state") &&
                localNode == h!!.optString("sourceNode") &&
                peerNode == h.optString("targetNode") &&
                h.optInt("protocol") == PROTOCOL &&
                h.optLong("epoch") > 0L
        )
        val nonNullFrozen = frozen!!
        val nonNullH = h!!
        if ("committed" == nonNullH.optString("state")) {
            requireOwnership(
                epoch(nonNullFrozen) == nonNullH.optLong("epoch") &&
                    nonNullH.optString("target") == nonNullFrozen.optString("timerOwner")
            )
            return copy(nonNullFrozen)
        }
        requireOwnership(
            "offered" == nonNullH.optString("state") &&
                nonNullFrozen.optBoolean("workoutPaused") &&
                epoch(nonNullFrozen) == nonNullH.optLong("epoch") - 1L &&
                nonNullH.optString("source") == nonNullFrozen.optString("timerOwner")
        )
        val result = copy(nonNullFrozen)
        result.put("ownerEpoch193", nonNullH.getLong("epoch"))
            .put("timerOwner", nonNullH.getString("target"))
        result.remove("nativeExecutionProtocol")
        result.remove("generation")
        result.remove("executionRevision")
        val automatic = result.optJSONObject("automation")
        automatic?.put("owner", nonNullH.getString("target"))
        transfer(result)!!.put("state", "committed")
        return result
    }

    /** A commit is usable only by the target that already saved this exact offer. */
    @JvmStatic
    @Throws(Exception::class)
    fun finish(accepted: JSONObject?, committed: JSONObject?, localNode: String?, peerNode: String?): JSONObject {
        val h = transfer(accepted)
        val c = transfer(committed)
        requireOwnership(
            accepted != null &&
                committed != null &&
                same(h, c) &&
                accepted.optString("workoutId") == committed.optString("workoutId") &&
                ("accepted" == h!!.optString("state") || "committed" == h.optString("state")) &&
                "committed" == c!!.optString("state") &&
                localNode == c.optString("targetNode") &&
                peerNode == c.optString("sourceNode") &&
                epoch(committed) == c.optLong("epoch") &&
                epoch(accepted) == c.optLong("epoch") - (if ("accepted" == h.optString("state")) 1L else 0L) &&
                c.optString("target") == committed.optString("timerOwner") &&
                committed.optBoolean("active", true) &&
                committed.optBoolean("workoutPaused")
        )
        // A duplicate commit must never erase the target's subsequent offline edits.
        if ("committed" == h!!.optString("state")) {
            return copy(accepted!!)
        }
        return copy(committed!!)
    }

    @JvmStatic
    fun stale(current: JSONObject?, incoming: JSONObject): Boolean {
        if (current == null) return false
        if (current.optString("workoutId") != incoming.optString("workoutId")) {
            return current.optBoolean("active", true) && (pending(current) || epoch(current) > 0L)
        }
        return epoch(incoming) < epoch(current) ||
            (epoch(incoming) == epoch(current) &&
                current.optString("timerOwner", "phone") != incoming.optString("timerOwner", "phone")) ||
            (pending(current) && !same(transfer(current), transfer(incoming)))
    }
}
