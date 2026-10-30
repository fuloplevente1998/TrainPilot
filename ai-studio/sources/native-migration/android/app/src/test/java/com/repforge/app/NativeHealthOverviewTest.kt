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

    @Test fun overviewHasStableStudioMeaningsWithoutReplacingTheFullMetricList() {
        val full = listOf(
            metric("weightKg", "65.2", true),
            metric("averageHeartRate", "71", true),
            metric("energyScore", "50", true),
            metric("steps", "4250", true),
            metric("activeCalories", "220", true),
            metric("sleepScore", "80", true),
            metric("bodyMassIndex", "19.5", true)
        )
        val result = NativeHealthMetrics.studioOverview(
            full,
            metric("sleepMinutes", "7.5 h", true),
            metric("workouts", "1", true),
            metric("recovery", "82%", true)
        )
        assertEquals(
            listOf("sleepMinutes", "activeCalories", "averageHeartRate", "steps", "workouts", "recovery"),
            result.map { it.key }
        )
        assertEquals(7, full.size)
        assertTrue(full.any { it.key == "weightKg" })
        assertTrue(full.any { it.key == "energyScore" })
    }

    @Test fun overviewKeepsMissingSelectedDayValuesMissingInsteadOfBorrowingScores() {
        val sleep = metric("sleepMinutes")
        val workouts = metric("workouts", "0", true)
        val recovery = metric("recovery")
        val full = listOf(
            metric("activeCalories"),
            metric("averageHeartRate"),
            metric("steps", "412", true),
            metric("sleepScore", "95", true),
            metric("energyScore", "90", true)
        )
        val result = NativeHealthMetrics.studioOverview(full, sleep, workouts, recovery)
        assertEquals(6, result.size)
        assertFalse(result[0].isAvailable)
        assertEquals("—", result[0].valueText)
        assertTrue(result[3].isAvailable)
        assertEquals("0", result[4].valueText)
        assertFalse(result[5].isAvailable)
        assertEquals("—", result[5].valueText)
    }
}
