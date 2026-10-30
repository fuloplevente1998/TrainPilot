package com.repforge

import org.junit.Assert.*
import org.junit.Test

class PersonalRecordCalculator226Test {

    @Test
    fun testOneRepMaxFormulas() {
        val weight = 100.0
        val reps = 10

        // Epley: 100 * (1 + 10/30) = 133.333 kg
        val epley = PersonalRecordCalculator226.calculateEpley(weight, reps)
        assertEquals(133.333, epley, 0.01)

        // Brzycki: 100 * (36 / (37 - 10)) = 100 * (36 / 27) = 133.333 kg
        val brzycki = PersonalRecordCalculator226.calculateBrzycki(weight, reps)
        assertEquals(133.333, brzycki, 0.01)

        // 1 rep esetén mindegyik formula pontosan a súlyt adja vissza
        assertEquals(120.0, PersonalRecordCalculator226.calculateEpley(120.0, 1), 0.001)
        assertEquals(120.0, PersonalRecordCalculator226.calculateBrzycki(120.0, 1), 0.001)
        assertEquals(120.0, PersonalRecordCalculator226.calculateLombardi(120.0, 1), 0.001)
    }

    @Test
    fun testPrEvaluationNewRecordsDetected() {
        val exercise = "Fekvenyomás"
        val historical = listOf(
            PrEntry226(
                exerciseName = exercise,
                category = PrCategory226.MAX_WEIGHT,
                value = 100.0,
                reps = 10,
                weightKg = 100.0,
                achievedAt = 1000L,
                formulaUsed = "Raw"
            ),
            PrEntry226(
                exerciseName = exercise,
                category = PrCategory226.ESTIMATED_1RM,
                value = 133.3,
                reps = 10,
                weightKg = 100.0,
                achievedAt = 1000L,
                formulaUsed = "Epley"
            ),
            PrEntry226(
                exerciseName = exercise,
                category = PrCategory226.MAX_VOLUME,
                value = 1000.0,
                reps = 10,
                weightKg = 100.0,
                achievedAt = 1000L,
                formulaUsed = "Volume"
            )
        )

        // Új sorozat: 105.0 kg x 10 ismétlés -> Mindhárom kategóriában rekorddöntés!
        val breakdowns = PersonalRecordCalculator226.evaluateSet(
            exerciseName = exercise,
            weightKg = 105.0,
            reps = 10,
            historicalPrs = historical
        )

        assertEquals(3, breakdowns.size)

        val weightPr = breakdowns.first { it.category == PrCategory226.MAX_WEIGHT }
        assertEquals(105.0, weightPr.newValue, 0.01)
        assertEquals(5.0, weightPr.deltaKg, 0.01)
        assertEquals(5.0, weightPr.deltaPercent, 0.01)

        val volumePr = breakdowns.first { it.category == PrCategory226.MAX_VOLUME }
        assertEquals(1050.0, volumePr.newValue, 0.01)
        assertEquals(50.0, volumePr.deltaKg, 0.01)
    }

    @Test
    fun testPrEvaluationNoRecordWhenBelowThreshold() {
        val exercise = "Guggolás"
        val historical = listOf(
            PrEntry226(
                exerciseName = exercise,
                category = PrCategory226.MAX_WEIGHT,
                value = 140.0,
                reps = 5,
                weightKg = 140.0,
                achievedAt = 1000L,
                formulaUsed = "Raw"
            )
        )

        // Gyengébb sorozat: 130 kg x 5 -> Nem dönthet rekordot
        val breakdowns = PersonalRecordCalculator226.evaluateSet(
            exerciseName = exercise,
            weightKg = 130.0,
            reps = 5,
            historicalPrs = historical
        )

        assertTrue(breakdowns.none { it.category == PrCategory226.MAX_WEIGHT })
    }
}
