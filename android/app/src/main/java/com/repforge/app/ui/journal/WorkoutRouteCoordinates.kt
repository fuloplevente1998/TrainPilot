package com.repforge.app.ui.journal

import android.content.Context
import com.repforge.app.GpsRouteStore
import com.repforge.shared.GpsRouteLedger193
import org.json.JSONArray

/** Shared private-file reader for active and saved workouts; loading/dismissing never controls GPS. */
internal object WorkoutRouteCoordinates {
    /** Each native segment stays separate: ownership gaps must never become straight lines. */
    fun loadAll(context: Context, keys: List<String>): RouteCoordinatesUiModel? = merge(keys.distinct().map { load(context, it) })

    internal fun merge(parts: List<RouteCoordinatesUiModel?>): RouteCoordinatesUiModel? {
        val available = parts.filterNotNull().filter { it.segments.any { points -> points.isNotEmpty() } }.distinctBy { it.routeKey }
        if (available.isEmpty()) return null
        val segments = available.flatMap { it.segments }
        return RouteCoordinatesUiModel(available.first().routeKey, segments, segments.sumOf { it.size },
            available.sumOf { it.distanceMeters }, available.mapNotNull { it.accuracyMeters }.maxOrNull(),
            parts.count { it == null || it.segments.none { points -> points.isNotEmpty() } })
    }

    fun load(context: Context, key: String): RouteCoordinatesUiModel? {
        return try {
            if (key.startsWith("wear193:")) {
                val ledger = GpsRouteLedger193.open(context.noBackupFilesDir)
                val viewObj = ledger.view(key)
                val segmentsArr = viewObj.optJSONArray("segments") ?: JSONArray()
                val segmentsList = ArrayList<List<Pair<Double, Double>>>()
                for (i in 0 until segmentsArr.length()) {
                    val seg = segmentsArr.getJSONObject(i)
                    val ptsArr = seg.optJSONArray("points") ?: continue
                    val ptsList = ArrayList<Pair<Double, Double>>()
                    for (j in 0 until ptsArr.length()) {
                        val p = ptsArr.getJSONObject(j)
                        ptsList.add(p.getDouble("latitude") to p.getDouble("longitude"))
                    }
                    if (ptsList.isNotEmpty()) segmentsList.add(ptsList)
                }
                RouteCoordinatesUiModel(
                    routeKey = key,
                    segments = segmentsList,
                    pointCount = viewObj.optInt("pointCount", 0),
                    distanceMeters = viewObj.optDouble("distanceMeters", 0.0),
                    accuracyMeters = null
                )
            } else {
                val store = GpsRouteStore(context.noBackupFilesDir)
                val track = store.read(key)
                val segmentsList = track.segments.map {
                    it.points.map { p -> p.latitude to p.longitude }
                }
                RouteCoordinatesUiModel(
                    routeKey = key,
                    segments = segmentsList,
                    pointCount = track.pointCount,
                    distanceMeters = 0.0,
                    accuracyMeters = null
                )
            }
        } catch (_: Exception) {
            null
        }
    }

}
