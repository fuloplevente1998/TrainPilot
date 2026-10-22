package com.repforge.app

import com.repforge.shared.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal class IntervalTimeline @JvmOverloads constructor(steps: List<Step>?, advance: Boolean, initialRestSeconds: Int = 0, restOnly: Boolean = false) :
    IntervalTimeline193(steps, advance, initialRestSeconds, restOnly) { companion object { const val MAX_MS = IntervalTimeline193.MAX_MS } }

internal class IntervalHapticCues {
    private val delegate = IntervalHapticCues193()
    fun next(p: IntervalTimeline193.Phase, running: Boolean): Int = delegate.next(p, running)
}

internal class IntervalTimeLedger private constructor(private val delegate: IntervalTimeLedger193) {
    constructor(count: Int, now: Long) : this(IntervalTimeLedger193(count, now))
    companion object {
        @JvmStatic @Throws(JSONException::class) fun restore(plan: JSONArray, saved: JSONObject, now: Long): IntervalTimeLedger =
            IntervalTimeLedger(IntervalTimeLedger193.restore(plan, saved, now))
    }
    fun sample(timeline: IntervalTimeline193, progress: Long, now: Long, running: Boolean, stopped: Boolean, interrupted: Boolean) =
        delegate.sample(timeline, progress, now, running, stopped, interrupted)
    fun skipTo(progress: Long) = delegate.skipTo(progress)
    @Throws(JSONException::class) fun json(plan: JSONArray): JSONObject = delegate.json(plan)
}

internal class DistanceAccumulator {
    private val delegate = DistanceAccumulator193()
    fun totalMeters(): Double = delegate.totalMeters()
    fun resetAnchor() = delegate.resetAnchor()
    fun add(lat: Double, lon: Double, acc: Double, elapsed: Long, speed: Double): Boolean = delegate.add(lat, lon, acc, elapsed, speed)
    companion object { @JvmStatic fun distance(a: Double, b: Double, c: Double, d: Double): Double = DistanceAccumulator193.distance(a, b, c, d) }
}

internal class GpsRouteTrace {
    companion object { const val MAX_POINTS = GpsRouteTrace193.MAX_POINTS; const val MAX_SEGMENTS = GpsRouteTrace193.MAX_SEGMENTS }
    class Point(@JvmField val latitude: Double, @JvmField val longitude: Double, @JvmField val accuracyMeters: Double, @JvmField val elapsedMillis: Long, @JvmField val epochMillis: Long)
    class Segment(@JvmField val ordinal: Int, @JvmField val points: List<Point>)
    private val delegate = GpsRouteTrace193()
    fun breakSegment() = delegate.breakSegment()
    fun record(lat: Double, lon: Double, acc: Double, elapsed: Long, epoch: Long, speed: Double): Boolean = delegate.record(lat, lon, acc, elapsed, epoch, speed)
    fun pointCount(): Int = delegate.pointCount()
    fun segmentCount(): Int = delegate.segmentCount()
    fun lastPoint(): Point? = delegate.lastPoint()?.let { Point(it.latitude, it.longitude, it.accuracyMeters, it.elapsedMillis, it.epochMillis) }
    fun isTruncated(): Boolean = delegate.isTruncated()
    fun snapshot(): List<Segment> = delegate.snapshot().map { s -> Segment(s.ordinal, s.points.map { Point(it.latitude, it.longitude, it.accuracyMeters, it.elapsedMillis, it.epochMillis) }) }
}

internal class GpsRouteCapture {
    private val distance = DistanceAccumulator(); private val route = GpsRouteTrace()
    fun addFix(lat: Double, lon: Double, acc: Double, elapsed: Long, epoch: Long, speed: Double): Boolean {
        if (!distance.add(lat, lon, acc, elapsed, speed)) return false
        route.record(lat, lon, acc, elapsed, epoch, speed); return true
    }
    fun breakSegment() { distance.resetAnchor(); route.breakSegment() }
    fun totalMeters(): Double = distance.totalMeters()
    fun routePointCount(): Int = route.pointCount()
    fun routeSegmentCount(): Int = route.segmentCount()
    fun routeTruncated(): Boolean = route.isTruncated()
    fun route(): GpsRouteTrace = route
}

internal object WorkoutUndo188 {
    @JvmStatic fun number(s: JSONObject, k: String): Double = WorkoutUndo188Shared.number(s, k)
    @JvmStatic @Throws(JSONException::class) fun guard(f: JSONObject): JSONArray = WorkoutUndo188Shared.guard(f)
    @JvmStatic @Throws(JSONException::class) fun same(l: JSONArray?, r: JSONArray?): Boolean = WorkoutUndo188Shared.same(l, r)
    @JvmStatic @Throws(JSONException::class) fun values(s: JSONObject): JSONObject = WorkoutUndo188Shared.values(s)
    @JvmStatic @Throws(JSONException::class) fun capture(b: JSONObject, a: JSONObject, t: JSONObject, id: String): JSONObject = WorkoutUndo188Shared.capture(b, a, t, id)
    @JvmStatic @Throws(JSONException::class) fun find(f: JSONObject, t: JSONObject): JSONObject = WorkoutUndo188Shared.find(f, t)
    @JvmStatic @Throws(JSONException::class) fun inverse(f: JSONObject, c: JSONObject): JSONObject {
        PhoneWorkoutRecording.validateFrame(f); val l = f.optJSONObject("lastRecord188")
        if (l == null || l.getString("recordId") != c.optString("recordId") || l.getString("exerciseId") != c.optString("exerciseId") || l.getString("setId") != c.optString("setId") || f.getString("workoutId") != c.optString("workoutId") || f.getLong("generation") != c.optLong("generation") || !same(l.getJSONArray("expected"), guard(f)) || !same(c.optJSONArray("expected"), guard(f))) throw IllegalArgumentException("undoConflict")
        val next = JSONObject(f.toString()); val set = find(next, l); val before = l.getJSONObject("before")
        for (k in arrayOf("done", "reps", "weight", "leftSeconds", "rightSeconds", "distanceMeters")) set.put(k, before.get(k))
        set.remove("actualRir")
        val exs = next.getJSONArray("exercises")
        for (e in 0 until exs.length()) if (exs.getJSONObject(e).getString("id") == l.getString("exerciseId")) next.put("currentExercise", e)
        next.put("restEndAt", 0).put("executionRevision", PhoneWorkoutRecording.advance(f.getLong("executionRevision")))
        next.remove("lastRecord188"); next.remove("manualAnchor162")
        val a = JSONObject().put("owner", "phone").put("enabled", f.optBoolean("automaticEnabled")).put("phase", "ready").put("paused", true).put("remainingMs", 0).put("deadlineAt", 0).put("key", "undo188:" + c.getString("commandId"))
        next.put("automation", a).put("transition", JSONObject().put("state", "paused").put("phase", "paused").put("currentExercise", next.getInt("currentExercise")))
        next.put("undo188", JSONObject().put("commandId", c.getString("commandId")).put("status", "applied"))
        return next
    }
}
