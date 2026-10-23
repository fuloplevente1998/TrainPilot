package com.repforge.app

import com.repforge.shared.DistanceAccumulator193
import com.repforge.shared.GpsRouteTrace193
import com.repforge.shared.IntervalHapticCues193
import com.repforge.shared.IntervalTimeLedger193
import com.repforge.shared.IntervalTimeline193
import com.repforge.shared.WorkoutUndo188Shared
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections

/** Phone compatibility alias; both devices use the same finite timeline calculation from :shared. */
class IntervalTimeline @JvmOverloads constructor(
    steps: List<Step>?,
    advance: Boolean,
    initialRestSeconds: Int = 0,
    restOnly: Boolean = false
) : IntervalTimeline193(steps, advance, initialRestSeconds, restOnly) {
    companion object {
        const val MAX_MS: Long = IntervalTimeline193.MAX_MS
    }
}

/** Delegates to IntervalHapticCues193 in :shared. */
internal class IntervalHapticCues {
    private val delegate = IntervalHapticCues193()

    fun next(p: IntervalTimeline193.Phase, running: Boolean): Int {
        return delegate.next(p, running)
    }
}

/** Delegates to IntervalTimeLedger193 in :shared. */
internal class IntervalTimeLedger private constructor(
    private val delegate: IntervalTimeLedger193
) {
    constructor(count: Int, now: Long) : this(IntervalTimeLedger193(count, now))

    companion object {
        @JvmStatic
        @Throws(JSONException::class)
        fun restore(plan: JSONArray, saved: JSONObject, now: Long): IntervalTimeLedger {
            return IntervalTimeLedger(IntervalTimeLedger193.restore(plan, saved, now))
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
        delegate.sample(timeline, progress, now, running, stopped, interrupted)
    }

    fun skipTo(progress: Long) {
        delegate.skipTo(progress)
    }

    @Throws(JSONException::class)
    fun json(plan: JSONArray): JSONObject {
        return delegate.json(plan)
    }
}

/** GNSS distance accumulator delegating to DistanceAccumulator193 in :shared. */
internal class DistanceAccumulator {
    private val delegate = DistanceAccumulator193()

    fun totalMeters(): Double = delegate.totalMeters()

    fun resetAnchor() = delegate.resetAnchor()

    fun add(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        elapsedMillis: Long,
        speedMps: Double
    ): Boolean {
        return delegate.add(latitude, longitude, accuracyMeters, elapsedMillis, speedMps)
    }

    companion object {
        @JvmStatic
        fun distance(a: Double, b: Double, c: Double, d: Double): Double {
            return DistanceAccumulator193.distance(a, b, c, d)
        }
    }
}

/** Bounded in-memory polyline trace delegating to GpsRouteTrace193 in :shared. */
internal class GpsRouteTrace {
    companion object {
        const val MAX_POINTS: Int = GpsRouteTrace193.MAX_POINTS
        const val MAX_SEGMENTS: Int = GpsRouteTrace193.MAX_SEGMENTS
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
        @JvmField val points: List<Point> = Collections.unmodifiableList(ArrayList(points))
    }

    private val delegate = GpsRouteTrace193()

    fun breakSegment() = delegate.breakSegment()

    fun record(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        elapsedMillis: Long,
        epochMillis: Long,
        speedMps: Double
    ): Boolean {
        return delegate.record(latitude, longitude, accuracyMeters, elapsedMillis, epochMillis, speedMps)
    }

    fun pointCount(): Int = delegate.pointCount()
    fun segmentCount(): Int = delegate.segmentCount()

    fun lastPoint(): Point? {
        val p = delegate.lastPoint() ?: return null
        return Point(p.latitude, p.longitude, p.accuracyMeters, p.elapsedMillis, p.epochMillis)
    }

    fun isTruncated(): Boolean = delegate.isTruncated()

    fun snapshot(): List<Segment> {
        return Collections.unmodifiableList(delegate.snapshot().map { seg ->
            Segment(
                seg.ordinal,
                seg.points.map { p ->
                    Point(p.latitude, p.longitude, p.accuracyMeters, p.elapsedMillis, p.epochMillis)
                }
            )
        })
    }
}

/** #163: delegates each GPS fix to DistanceAccumulator and GpsRouteTrace. */
internal class GpsRouteCapture {
    private val distance = DistanceAccumulator()
    private val route = GpsRouteTrace()

    fun addFix(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        elapsedMillis: Long,
        epochMillis: Long,
        speedMps: Double
    ): Boolean {
        if (!distance.add(latitude, longitude, accuracyMeters, elapsedMillis, speedMps)) return false
        route.record(latitude, longitude, accuracyMeters, elapsedMillis, epochMillis, speedMps)
        return true
    }

    fun breakSegment() {
        distance.resetAnchor()
        route.breakSegment()
    }

    fun totalMeters(): Double = distance.totalMeters()
    fun routePointCount(): Int = route.pointCount()
    fun routeSegmentCount(): Int = route.segmentCount()
    fun routeTruncated(): Boolean = route.isTruncated()
    fun route(): GpsRouteTrace = route
}

/** Ordered stable identities and all editable set values; uses shared guard logic from :shared. */
internal object WorkoutUndo188 {
    const val QUICK_MILLIS: Long = WorkoutUndo188Shared.QUICK_MILLIS
    @JvmStatic
    fun number(set: JSONObject, key: String): Double = WorkoutUndo188Shared.number(set, key)

    @JvmStatic
    @Throws(JSONException::class)
    fun guard(frame: JSONObject): JSONArray = WorkoutUndo188Shared.guard(frame)

    @JvmStatic
    @Throws(JSONException::class)
    fun same(left: JSONArray?, right: JSONArray?): Boolean = WorkoutUndo188Shared.same(left, right)

    @JvmStatic
    @Throws(JSONException::class)
    fun values(set: JSONObject): JSONObject = WorkoutUndo188Shared.values(set)

    @JvmStatic
    @Throws(JSONException::class)
    fun capture(before: JSONObject, after: JSONObject, target: JSONObject, recordId: String): JSONObject =
        WorkoutUndo188Shared.capture(before, after, target, recordId)

    @JvmStatic
    @Throws(JSONException::class)
    fun find(frame: JSONObject, target: JSONObject): JSONObject =
        WorkoutUndo188Shared.find(frame, target)

    @JvmStatic
    @Throws(JSONException::class)
    fun inverse(frame: JSONObject, command: JSONObject): JSONObject {
        PhoneWorkoutRecording.validateFrame(frame)
        val last = frame.optJSONObject("lastRecord188")
        if (last == null ||
            last.getString("recordId") != command.optString("recordId") ||
            last.getString("exerciseId") != command.optString("exerciseId") ||
            last.getString("setId") != command.optString("setId") ||
            frame.getString("workoutId") != command.optString("workoutId") ||
            frame.getLong("generation") != command.optLong("generation") ||
            !same(last.getJSONArray("expected"), guard(frame)) ||
            !same(command.optJSONArray("expected"), guard(frame))
        ) {
            throw IllegalArgumentException("undoConflict")
        }
        val next = JSONObject(frame.toString())
        val set = find(next, last)
        val before = last.getJSONObject("before")
        for (key in arrayOf("done", "reps", "weight", "leftSeconds", "rightSeconds", "distanceMeters")) {
            set.put(key, before.get(key))
        }
        set.remove("actualRir")
        val exercises = next.getJSONArray("exercises")
        for (e in 0 until exercises.length()) {
            val exercise = exercises.getJSONObject(e)
            val sets = exercise.getJSONArray("sets")
            if (exercise.getString("id") == last.getString("exerciseId") &&
                (0 until sets.length()).any { sets.getJSONObject(it).getString("setId") == last.getString("setId") }) {
                next.put("currentExercise", e)
                break
            }
        }
        next.put("restEndAt", 0)
            .put("executionRevision", PhoneWorkoutRecording.advance(frame.getLong("executionRevision")))
        next.remove("lastRecord188")
        next.remove("manualAnchor162")
        val a = JSONObject()
            .put("owner", "phone")
            .put("enabled", frame.optBoolean("automaticEnabled"))
            .put("phase", "ready")
            .put("paused", true)
            .put("remainingMs", 0)
            .put("deadlineAt", 0)
            .put("key", "undo188:" + command.getString("commandId"))
        next.put("automation", a)
            .put(
                "transition",
                JSONObject()
                    .put("state", "paused")
                    .put("phase", "paused")
                    .put("currentExercise", next.getInt("currentExercise"))
            )
        next.put(
            "undo188",
            JSONObject()
                .put("commandId", command.getString("commandId"))
                .put("status", "applied")
        )
        return next
    }
}
