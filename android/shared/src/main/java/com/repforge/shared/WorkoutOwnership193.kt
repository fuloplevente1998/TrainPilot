package com.repforge.shared

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import kotlin.math.max

object WorkoutOwnership193 {
    const val PATH: String = "/trainpilot/ownership193/"
    const val PROTOCOL: Int = 1
    private val ID_REGEX = Regex("[A-Za-z0-9._:-]{1,128}")

    @JvmStatic @Throws(Exception::class) fun copy(v: JSONObject): JSONObject = JSONObject(v.toString())
    @JvmStatic fun epoch(v: JSONObject?): Long = v?.optLong("ownerEpoch193") ?: 0L
    @JvmStatic fun transfer(v: JSONObject?): JSONObject? = v?.optJSONObject("handoff193")
    @JvmStatic fun pending(v: JSONObject?): Boolean { val h = transfer(v); return h != null && "committed" != h.optString("state") }
    @JvmStatic fun owns(v: JSONObject?, device: String): Boolean = v != null && v.optBoolean("active", true) && !pending(v) && device == v.optString("timerOwner", "phone")
    private fun requireOwnership(v: Boolean) { if (!v) throw IllegalStateException("Ownership changed; retry the same transfer.") }
    private fun id(v: String?): Boolean = v != null && ID_REGEX.matches(v)
    private fun same(a: JSONObject?, b: JSONObject?): Boolean = a != null && b != null && a.optInt("protocol") == PROTOCOL && b.optInt("protocol") == PROTOCOL && a.optLong("epoch") == b.optLong("epoch") && a.optString("id") == b.optString("id") && a.optString("source") == b.optString("source") && a.optString("target") == b.optString("target") && a.optString("sourceNode") == b.optString("sourceNode") && a.optString("targetNode") == b.optString("targetNode")

    @JvmStatic @Throws(Exception::class) fun offer(current: JSONObject?, source: String, sourceNode: String?, targetNode: String?, transferId: String?, expectedEpoch: Long, now: Long): JSONObject {
        requireOwnership(("phone" == source || "watch" == source) && owns(current, source) && current != null && current.optString("workoutId").isNotEmpty() && current.optString("workoutId").length <= 512 && epoch(current) == expectedEpoch && expectedEpoch >= 0L && id(sourceNode) && id(targetNode) && sourceNode != targetNode && id(transferId) && now > 0L && expectedEpoch < Long.MAX_VALUE && current.optLong("pauseRevision") >= 0L && current.optLong("pauseRevision") < Long.MAX_VALUE && (transfer(current) == null || transferId != transfer(current)!!.optString("id")))
        val target = if ("phone" == source) "watch" else "phone"; val result = copy(current!!)
        result.put("ownershipProtocol193", PROTOCOL).put("ownerEpoch193", expectedEpoch).put("workoutPaused", true).put("pauseRevision", current.optLong("pauseRevision") + 1L).put("pausedRestMillis", max(current.optLong("pausedRestMillis"), max(0L, current.optLong("restEndAt") - now))).put("restEndAt", 0)
        val intervals = result.optJSONArray("activeIntervals")
        if (intervals != null) for (i in 0 until intervals.length()) { val itv = intervals.optJSONObject(i); if (itv != null && (itv.isNull("end") || itv.optString("end").isEmpty())) itv.put("end", Instant.ofEpochMilli(now).toString()) }
        val automatic = result.optJSONObject("automation")
        if (automatic != null) {
            var remaining = automatic.optLong("remainingMs")
            if (!automatic.optBoolean("paused") && automatic.optLong("deadlineAt") > 0L) remaining = max(0L, automatic.optLong("deadlineAt") - now)
            automatic.put("paused", true).put("deadlineAt", 0).put("remainingMs", remaining)
        }
        result.put("handoff193", JSONObject().put("protocol", PROTOCOL).put("id", transferId).put("source", source).put("target", target).put("sourceNode", sourceNode).put("targetNode", targetNode).put("epoch", expectedEpoch + 1L).put("state", "offered").put("createdAt", now))
        return result
    }

    @JvmStatic @Throws(Exception::class) fun accept(current: JSONObject?, offer: JSONObject?, localNode: String?, peerNode: String?): JSONObject {
        val h = transfer(offer)
        requireOwnership(offer != null && h != null && h.optInt("protocol") == PROTOCOL && "offered" == h.optString("state") && localNode == h.optString("targetNode") && peerNode == h.optString("sourceNode") && id(h.optString("id")) && offer.optBoolean("active", true) && offer.optBoolean("workoutPaused") && offer.optString("workoutId").isNotEmpty() && offer.optString("workoutId").length <= 512 && epoch(offer) >= 0L && h.optLong("epoch") > 0L && h.optLong("epoch") == epoch(offer) + 1L && ("phone" == h.optString("source") || "watch" == h.optString("source")) && h.optString("source") == offer.optString("timerOwner") && (if ("phone" == h.optString("source")) "watch" else "phone") == h.optString("target") && id(h.optString("sourceNode")) && id(h.optString("targetNode")) && localNode != peerNode)
        if (current != null) {
            requireOwnership(current.optString("workoutId") == offer!!.optString("workoutId"))
            if (same(transfer(current), h) && "accepted" == transfer(current)!!.optString("state")) return copy(current)
            requireOwnership(!pending(current) && epoch(current) < h!!.optLong("epoch") && current.optString("timerOwner", "phone") == h.optString("source"))
        }
        val result = copy(offer!!); transfer(result)!!.put("state", "accepted"); return result
    }

    @JvmStatic @Throws(Exception::class) fun commit(frozen: JSONObject?, accepted: JSONObject?, localNode: String?, peerNode: String?): JSONObject {
        val h = transfer(frozen); val a = transfer(accepted)
        requireOwnership(frozen != null && accepted != null && same(h, a) && frozen.optString("workoutId") == accepted.optString("workoutId") && "accepted" == a!!.optString("state") && localNode == h!!.optString("sourceNode") && peerNode == h.optString("targetNode") && h.optInt("protocol") == PROTOCOL && h.optLong("epoch") > 0L)
        if ("committed" == h!!.optString("state")) {
            requireOwnership(epoch(frozen!!) == h.optLong("epoch") && h.optString("target") == frozen.optString("timerOwner"))
            return copy(frozen)
        }
        requireOwnership("offered" == h.optString("state") && frozen!!.optBoolean("workoutPaused") && epoch(frozen) == h.optLong("epoch") - 1L && h.optString("source") == frozen.optString("timerOwner"))
        val result = copy(frozen); result.put("ownerEpoch193", h.getLong("epoch")).put("timerOwner", h.getString("target"))
        result.remove("nativeExecutionProtocol"); result.remove("generation"); result.remove("executionRevision")
        result.optJSONObject("automation")?.put("owner", h.getString("target"))
        transfer(result)!!.put("state", "committed"); return result
    }

    @JvmStatic @Throws(Exception::class) fun finish(accepted: JSONObject?, committed: JSONObject?, localNode: String?, peerNode: String?): JSONObject {
        val h = transfer(accepted); val c = transfer(committed)
        requireOwnership(accepted != null && committed != null && same(h, c) && accepted.optString("workoutId") == committed.optString("workoutId") && ("accepted" == h!!.optString("state") || "committed" == h.optString("state")) && "committed" == c!!.optString("state") && localNode == c.optString("targetNode") && peerNode == c.optString("sourceNode") && epoch(committed) == c.optLong("epoch") && epoch(accepted) == c.optLong("epoch") - (if ("accepted" == h.optString("state")) 1L else 0L) && c.optString("target") == committed.optString("timerOwner") && committed.optBoolean("active", true) && committed.optBoolean("workoutPaused"))
        if ("committed" == h!!.optString("state")) return copy(accepted!!)
        return copy(committed!!)
    }

    @JvmStatic fun stale(current: JSONObject?, incoming: JSONObject): Boolean {
        if (current == null) return false
        if (current.optString("workoutId") != incoming.optString("workoutId")) return current.optBoolean("active", true) && (pending(current) || epoch(current) > 0L)
        return epoch(incoming) < epoch(current) || (epoch(incoming) == epoch(current) && current.optString("timerOwner", "phone") != incoming.optString("timerOwner", "phone")) || (pending(current) && !same(transfer(current), transfer(incoming)))
    }
}
