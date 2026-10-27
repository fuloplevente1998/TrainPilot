package com.repforge.app

import com.repforge.app.ui.health.HealthMetricItemUiModel
import com.repforge.app.ui.health.NativeHealthMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Studio Health dashboard is a projection of real data, not an independent source. */
class NativeHealthOverviewTest {
    private fun metric(key: String, value: String = "—", available: Boolean = false) =
        HealthMetricItemUiModel(
            key = key, label = key, valueText = value, unit = "unit",
            sourceText = if (available) "samsung_health" else "No data",
            isAvailable = available
        )

    @Test fun overviewHasStableStudioOrderRegardlessOfProviderRowOrder() {
        val full = listOf(metric("weightKg", "65.2", true), metric("distanceMeters", "2000", true),
            metric("averageHeartRate", "71", true), metric("energyScore", "50", true),
            metric("steps", "4250", true), metric("activeCalories", "220", true),
            metric("sleepScore", "80", true), metric("bodyMassIndex", "19.5", true))
        val result = NativeHealthMetrics.overview(full)
        assertEquals(listOf("sleepScore", "activeCalories", "averageHeartRate",
            "steps", "weightKg", "energyScore"), result.map { it.key })
        assertEquals("samsung_health", result.first().sourceText)
        assertEquals(8, full.size) // The full 26-reading model is never overwritten or truncated.
    }

    @Test fun overviewDoesNotInventValuesOrCopyAnotherDaysReadings() {
        val missing = metric("sleepScore")
        val available = metric("steps", "412", true)
        val result = NativeHealthMetrics.overview(listOf(available, missing))
        assertEquals(listOf(missing, available), result)
        assertFalse(result.first().isAvailable)
        assertEquals("—", result.first().valueText)
        assertTrue(result.last().isAvailable)
        assertTrue(NativeHealthMetrics.overview(emptyList()).isEmpty())
    }
}
