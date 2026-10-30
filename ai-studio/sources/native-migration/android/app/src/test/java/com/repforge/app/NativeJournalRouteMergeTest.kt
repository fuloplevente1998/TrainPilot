package com.repforge.app

import com.repforge.app.ui.journal.RouteCoordinatesUiModel
import com.repforge.app.ui.journal.WorkoutRouteCoordinates
import org.junit.Assert.*
import org.junit.Test

class NativeJournalRouteMergeTest {
    private fun part(key: String, start: Double) = RouteCoordinatesUiModel(key,
        listOf(listOf(start to 19.0, (start + .001) to 19.001)), 2, 10.0, null)
    @Test fun mixedWatchPhonePartsKeepSeparatePathsAcrossGaps() {
        val merged = WorkoutRouteCoordinates.merge(listOf(part("phone", 47.0), part("wear193:watch", 48.0)))!!
        assertEquals(2, merged.segments.size)
        assertEquals(4, merged.pointCount)
        assertEquals(20.0, merged.distanceMeters, .001)
        assertEquals(0, merged.missingRouteParts)
    }
    @Test fun partialTransferIsExplicitAndAllMissingIsNotAnEmptySuccessfulMap() {
        val merged = WorkoutRouteCoordinates.merge(listOf(part("phone", 47.0), null))!!
        assertEquals(1, merged.missingRouteParts)
        assertNull(WorkoutRouteCoordinates.merge(listOf(null, null)))
    }
    @Test fun duplicateRouteReferencesDoNotDuplicateSamplesOrInventAConnection() {
        val phone = part("phone", 47.0)
        val merged = WorkoutRouteCoordinates.merge(listOf(phone, phone))!!
        assertEquals(phone.segments, merged.segments)
        assertEquals(2, merged.pointCount)
        assertEquals(0, merged.missingRouteParts)
    }
}
