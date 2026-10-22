package com.repforge.shared

import org.json.JSONObject
import java.util.Collections
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** GNSS fixes only. Rejected signal/ownership gaps never contribute distance or a connecting line. */
class GpsRouteSampler193(private val writer: Writer) {
    fun interface Writer {
        @Throws(Exception::class)
        fun append(point: JSONObject)
    }

    private var anchor: JSONObject? = null
    private var points: Int = 0
    private var segment: Int = 0
    private var lastElapsed: Long = -1L
    private var lastEpoch: Long = -1L
    private var meters: Double = 0.0
    private var partial: Boolean = false

    fun breakSegment() {
        anchor = null
    }

    fun pointCount(): Int = points
    fun segmentCount(): Int = segment
    fun distanceMeters(): Double = meters
    fun partial(): Boolean = partial

    @Throws(Exception::class)
    fun add(
        latitude: Double,
        longitude: Double,
        accuracy: Double,
        elapsed: Long,
        epoch: Long,
        speed: Double
    ): Boolean {
        if (!latitude.isFinite() || !longitude.isFinite() ||
            abs(latitude) > 90.0 || abs(longitude) > 180.0 ||
            !accuracy.isFinite() || accuracy < 0.0 || accuracy > 35.0 ||
            elapsed < 0L || epoch <= 0L ||
            (speed.isFinite() && (speed < 0.0 || speed > 20.0))
        ) {
            breakSegment()
            return false
        }
        if (elapsed <= lastElapsed || epoch <= lastEpoch) return false
        val currentAnchor = anchor
        if (currentAnchor != null && elapsed - currentAnchor.getLong("elapsedMillis") > 30_000L) {
            breakSegment()
        }
        var delta = 0.0
        val activeAnchor = anchor
        if (activeAnchor != null) {
            delta = GpsRoutePacket193.distance(
                activeAnchor.getDouble("latitude"),
                activeAnchor.getDouble("longitude"),
                latitude,
                longitude
            )
            if (delta / ((elapsed - activeAnchor.getLong("elapsedMillis")) / 1000.0) > 20.0) {
                breakSegment()
                return false
            }
            val minimum = max(3.0, min(15.0, (activeAnchor.getDouble("accuracyMeters") + accuracy) * 0.25))
            if (delta < minimum || (speed.isFinite() && speed < 0.5)) {
                return false
            }
        }
        if (points >= GpsRoutePacket193.MAX_POINTS ||
            (anchor == null && segment >= GpsRoutePacket193.MAX_SEGMENTS)
        ) {
            partial = true
            return false
        }
        val nextSegment = if (anchor == null) segment + 1 else segment
        val point = JSONObject()
            .put("latitude", latitude)
            .put("longitude", longitude)
            .put("accuracyMeters", accuracy)
            .put("elapsedMillis", elapsed)
            .put("epochMillis", epoch)
            .put("segment", nextSegment)
        try {
            writer.append(JSONObject(point.toString()))
        } catch (error: Exception) {
            partial = true
            breakSegment()
            throw error
        }
        anchor = point
        segment = nextSegment
        points++
        meters += delta
        lastElapsed = elapsed
        lastEpoch = epoch
        return true
    }
}

/** GNSS distance accumulator shared by phone and watch. */
class DistanceAccumulator193 {
    private var lat: Double = 0.0
    private var lon: Double = 0.0
    private var accuracy: Double = 0.0
    private var total: Double = 0.0
    private var at: Long = 0L
    private var anchor: Boolean = false

    fun totalMeters(): Double = total

    fun resetAnchor() {
        anchor = false
    }

    fun add(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        elapsedMillis: Long,
        speedMps: Double
    ): Boolean {
        if (!latitude.isFinite() || !longitude.isFinite() ||
            abs(latitude) > 90.0 || abs(longitude) > 180.0 ||
            !accuracyMeters.isFinite() || accuracyMeters < 0.0 || accuracyMeters > 35.0 ||
            elapsedMillis < 0L
        ) {
            return false
        }
        if (!anchor) {
            remember(latitude, longitude, accuracyMeters, elapsedMillis)
            return true
        }
        val dt = elapsedMillis - at
        if (dt <= 0L) return false
        if (dt > 30_000L) {
            remember(latitude, longitude, accuracyMeters, elapsedMillis)
            return true
        }
        val meters = distance(lat, lon, latitude, longitude)
        if (meters / (dt / 1000.0) > 20.0) return false
        val threshold = max(3.0, min(15.0, (accuracy + accuracyMeters) * 0.25))
        if (meters < threshold || (speedMps.isFinite() && speedMps < 0.5)) return true
        total += meters
        remember(latitude, longitude, accuracyMeters, elapsedMillis)
        return true
    }

    private fun remember(latitude: Double, longitude: Double, acc: Double, millis: Long) {
        lat = latitude
        lon = longitude
        accuracy = acc
        at = millis
        anchor = true
    }

    companion object {
        @JvmStatic
        fun distance(a: Double, b: Double, c: Double, d: Double): Double {
            return GpsRoutePacket193.distance(a, b, c, d)
        }
    }
}

/** #163: bounded, in-memory polyline segments. */
class GpsRouteTrace193 {
    companion object {
        const val MAX_POINTS: Int = GpsRoutePacket193.MAX_POINTS
        const val MAX_SEGMENTS: Int = GpsRoutePacket193.MAX_SEGMENTS
        private const val MAX_GAP_MS: Long = 30_000L
        private const val MAX_ACCURACY_METERS: Double = 35.0
        private const val MAX_SPEED_MPS: Double = 20.0
    }

    class Point(
        @JvmField val latitude: Double,
        @JvmField val longitude: Double,
        @JvmField val accuracyMeters: Double,
        @JvmField val elapsedMillis: Long,
        @JvmField val epochMillis: Long
    )

    class Segment(
        @JvmField val ordinal: Int,
        points: List<Point>
    ) {
        @JvmField
        val points: List<Point> = Collections.unmodifiableList(ArrayList(points))
    }

    private val segments = ArrayList<ArrayList<Point>>()
    private var anchor: Point? = null
    private var pointCount: Int = 0
    private var truncated: Boolean = false

    fun breakSegment() {
        anchor = null
    }

    fun record(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        elapsedMillis: Long,
        epochMillis: Long,
        speedMps: Double
    ): Boolean {
        if (truncated || !latitude.isFinite() || !longitude.isFinite() ||
            abs(latitude) > 90.0 || abs(longitude) > 180.0 ||
            !accuracyMeters.isFinite() || accuracyMeters < 0.0 ||
            accuracyMeters > MAX_ACCURACY_METERS || elapsedMillis < 0L || epochMillis <= 0L ||
            (speedMps.isFinite() && (speedMps < 0.0 || speedMps > MAX_SPEED_MPS))
        ) {
            return false
        }
        val currentAnchor = anchor
        if (currentAnchor != null && elapsedMillis <= currentAnchor.elapsedMillis) return false
        if (currentAnchor != null && elapsedMillis - currentAnchor.elapsedMillis > MAX_GAP_MS) {
            breakSegment()
        }
        val activeAnchor = anchor
        if (activeAnchor != null) {
            val meters = DistanceAccumulator193.distance(
                activeAnchor.latitude,
                activeAnchor.longitude,
                latitude,
                longitude
            )
            val seconds = (elapsedMillis - activeAnchor.elapsedMillis) / 1000.0
            if (meters / seconds > MAX_SPEED_MPS) {
                breakSegment()
                return false
            }
            val minimum = max(3.0, min(15.0, (activeAnchor.accuracyMeters + accuracyMeters) * 0.25))
            if (meters < minimum || (speedMps.isFinite() && speedMps < 0.5)) return false
        }
        if (pointCount >= MAX_POINTS || (anchor == null && segments.size >= MAX_SEGMENTS)) {
            truncated = true
            return false
        }
        if (anchor == null) segments.add(ArrayList())
        val point = Point(latitude, longitude, accuracyMeters, elapsedMillis, epochMillis)
        segments[segments.size - 1].add(point)
        anchor = point
        pointCount++
        return true
    }

    fun pointCount(): Int = pointCount
    fun segmentCount(): Int = segments.size
    fun lastPoint(): Point? = anchor
    fun isTruncated(): Boolean = truncated

    fun snapshot(): List<Segment> {
        val copy = ArrayList<Segment>(segments.size)
        for (i in segments.indices) {
            copy.add(Segment(i + 1, segments[i]))
        }
        return Collections.unmodifiableList(copy)
    }
}
