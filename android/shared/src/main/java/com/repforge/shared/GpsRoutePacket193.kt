package com.repforge.shared

import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Immutable, bounded route chunks. Coordinates never belong in a workout/Health snapshot. */
object GpsRoutePacket193 {
    const val MAX_POINTS: Int = 12000
    const val MAX_SEGMENTS: Int = 256
    const val CHUNK_POINTS: Int = 32
    const val MAX_PACKET_BYTES: Int = 12000

    private val ID_REGEX = Regex("[A-Za-z0-9._:-]{1,128}")

    private fun requireValid(value: Boolean) {
        if (!value) throw IllegalArgumentException("Invalid route packet")
    }

    @JvmStatic
    @Throws(Exception::class)
    fun digest(value: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
        val out = StringBuilder(hash.size * 2)
        for (b in hash) {
            out.append(String.format(Locale.ROOT, "%02x", b.toInt() and 255))
        }
        return out.toString()
    }

    @JvmStatic
    @Throws(Exception::class)
    fun key(workoutId: String?, setId: String?): String {
        requireValid(text(workoutId, 512) && text(setId, 200))
        return "wear193:" + digest(JSONArray().put(workoutId).put(setId).toString())
    }

    @JvmStatic
    fun id(value: String?): Boolean {
        return value != null && ID_REGEX.matches(value)
    }

    private fun text(value: String?, max: Int): Boolean {
        return !value.isNullOrEmpty() && value.length <= max
    }

    @Throws(Exception::class)
    private fun integer(value: JSONObject, name: String, min: Long, max: Long): Long {
        val raw = value.get(name)
        requireValid(raw is Number)
        val number = raw as Number
        val n = number.toDouble()
        val result = number.toLong()
        requireValid(n.isFinite() && n == result.toDouble() && result in min..max)
        return result
    }

    @Throws(Exception::class)
    private fun number(value: JSONObject, name: String, min: Double, max: Double): Double {
        val raw = value.get(name)
        requireValid(raw is Number)
        val result = (raw as Number).toDouble()
        requireValid(result.isFinite() && result in min..max)
        return result
    }

    /** A grant is local durable consent, never a field supplied by a remote packet. */
    @JvmStatic
    @Throws(Exception::class)
    fun validate(input: JSONObject?, grant: JSONObject?, origin: String?, localNode: String?): JSONObject {
        requireValid(
            input != null &&
                grant != null &&
                input.toString().toByteArray(StandardCharsets.UTF_8).size <= MAX_PACKET_BYTES
        )
        val nonNullInput = input!!
        val nonNullGrant = grant!!
        requireValid(
            integer(nonNullInput, "schema", 1L, 1L) == 1L &&
                id(origin) &&
                id(localNode) &&
                origin != localNode
        )
        val workout = nonNullInput.getString("workoutId")
        val set = nonNullInput.getString("setId")
        val route = nonNullInput.getString("routeKey")
        requireValid(
            text(workout, 512) &&
                text(set, 200) &&
                text(nonNullInput.getString("exerciseId"), 200) &&
                id(nonNullInput.getString("streamId"))
        )
        requireValid(
            route == key(workout, set) &&
                origin == nonNullInput.getString("sourceNode") &&
                localNode == nonNullInput.getString("targetNode")
        )
        requireValid(
            route == nonNullGrant.optString("routeKey") &&
                workout == nonNullGrant.optString("workoutId") &&
                set == nonNullGrant.optString("setId") &&
                nonNullInput.getString("exerciseId") == nonNullGrant.optString("exerciseId") &&
                origin == nonNullGrant.optString("sourceNode") &&
                localNode == nonNullGrant.optString("targetNode") &&
                integer(nonNullInput, "ownerEpoch193", 0L, Long.MAX_VALUE) == nonNullGrant.getLong("ownerEpoch193")
        )
        integer(nonNullInput, "sequence", 1L, MAX_POINTS.toLong())
        requireValid(nonNullInput.get("closed") is Boolean && nonNullInput.get("partial") is Boolean)

        val points = nonNullInput.getJSONArray("points")
        requireValid(points.length() <= CHUNK_POINTS && (points.length() > 0 || nonNullInput.getBoolean("closed")))

        var elapsed = -1L
        var epoch = -1L
        var segment = 0L
        var lat = 0.0
        var lon = 0.0
        for (i in 0 until points.length()) {
            val point = points.getJSONObject(i)
            val nextLat = number(point, "latitude", -90.0, 90.0)
            val nextLon = number(point, "longitude", -180.0, 180.0)
            number(point, "accuracyMeters", 0.0, 35.0)
            val nextElapsed = integer(point, "elapsedMillis", 0L, Long.MAX_VALUE)
            val nextEpoch = integer(point, "epochMillis", 1L, Long.MAX_VALUE)
            val nextSegment = integer(point, "segment", 1L, MAX_SEGMENTS.toLong())
            requireValid(nextElapsed > elapsed && nextEpoch > epoch && nextSegment >= segment)
            if (i > 0 && nextSegment == segment) {
                requireValid(
                    nextElapsed - elapsed <= 30_000L &&
                        distance(lat, lon, nextLat, nextLon) / ((nextElapsed - elapsed) / 1000.0) <= 20.0
                )
            }
            elapsed = nextElapsed
            epoch = nextEpoch
            segment = nextSegment
            lat = nextLat
            lon = nextLon
        }

        // Whitelist prevents arbitrary remote fields from becoming persistent or entering a receipt.
        val clean = JSONObject()
        for (field in arrayOf(
            "schema", "routeKey", "workoutId", "exerciseId", "setId",
            "sourceNode", "targetNode", "ownerEpoch193", "streamId", "sequence", "closed", "partial"
        )) {
            clean.put(field, nonNullInput.get(field))
        }
        val copied = JSONArray()
        for (i in 0 until points.length()) {
            val point = JSONObject()
            val srcPoint = points.getJSONObject(i)
            for (field in arrayOf("latitude", "longitude", "accuracyMeters", "elapsedMillis", "epochMillis", "segment")) {
                point.put(field, srcPoint.get(field))
            }
            copied.put(point)
        }
        return clean.put("points", copied)
    }

    @JvmStatic
    fun distance(lat: Double, lon: Double, otherLat: Double, otherLon: Double): Double {
        val a = Math.toRadians(otherLat - lat)
        val b = Math.toRadians(otherLon - lon)
        val x = sin(a / 2.0) * sin(a / 2.0) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(otherLat)) * sin(b / 2.0) * sin(b / 2.0)
        return 6_371_000.0 * 2.0 * atan2(sqrt(x), sqrt(max(0.0, 1.0 - x)))
    }
}
