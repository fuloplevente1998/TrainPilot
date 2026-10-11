package com.repforge.app.wear

/** A remote open must wait for the published workout, never start a stale cache. */
data class WearMeasurementStart(val workoutId: String, val minimumRevision: Long) {
    fun matches(currentId: String?, currentRevision: Long): Boolean =
        currentId == workoutId && currentRevision >= minimumRevision

    companion object {
        fun parse(workoutId: String?, revision: String?): WearMeasurementStart? {
            val id = workoutId?.takeIf { it.isNotBlank() && it.length <= 512 } ?: return null
            val version = revision?.toLongOrNull()?.takeIf { it > 0L } ?: return null
            return WearMeasurementStart(id, version)
        }
    }
}
