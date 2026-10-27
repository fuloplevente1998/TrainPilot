package com.repforge.app

import com.repforge.app.ui.progress.Exercise1RmPointUiModel
import com.repforge.app.ui.progress.strengthSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** UI estimates are derived from saved strength history, never fabricated. */
class NativeStrengthSnapshotTest {
    private fun point(date: String, value: Double) =
        Exercise1RmPointUiModel(date, value, 1, value)

    @Test fun latestAndBestAreSeparateFacts() {
        val first = point("2026-10-01", 62.0)
        val best = point("2026-10-12", 75.0)
        val latest = point("2026-10-24", 70.0)
        assertEquals(latest to best, strengthSnapshot(listOf(first, best, latest)))
    }

    @Test fun invalidEstimatesCannotAppearAsRealMetrics() {
        val real = point("2026-10-22", 65.0)
        assertEquals(real to real, strengthSnapshot(listOf(
            point("2026-10-03", Double.NaN),
            real, point("2026-10-24", Double.POSITIVE_INFINITY),
            point("2026-10-25", -50.0), point("2026-10-26", 0.0)
        )))
        assertNull(strengthSnapshot(emptyList()))
        assertNull(strengthSnapshot(listOf(point("2026-10-25", Double.NaN))))
    }
}
