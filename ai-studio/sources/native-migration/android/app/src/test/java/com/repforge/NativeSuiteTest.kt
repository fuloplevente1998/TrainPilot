package com.repforge

import org.junit.Assert.*
import org.junit.Test

class NativeSuiteTest {

    @Test
    fun testDoubleProgressionAdvancement() {
        val plans = PhoneWorkoutPlans()
        val sets = listOf(
            EngineSetRecord(1, 1, "Fekvenyomás", 1, EngineSetType.NORMAL, 80.0, 12, 1, true, 100L),
            EngineSetRecord(2, 1, "Fekvenyomás", 2, EngineSetType.NORMAL, 80.0, 12, 2, true, 200L),
            EngineSetRecord(3, 1, "Fekvenyomás", 3, EngineSetType.NORMAL, 80.0, 12, 1, true, 300L)
        )

        val advice = plans.evaluateProgression(
            exerciseName = "Fekvenyomás",
            model = EngineProgressionModel.DOUBLE_PROGRESSION,
            completedSets = sets,
            targetRepMin = 8,
            targetRepMax = 12,
            weightIncrementKg = 2.5
        )

        // Mivel minden sorozat elérte a 12 ismétlést és az RIR <= 2, súlynövelést kell javasolnia: 82.5 kg-ra
        assertEquals(82.5, advice.suggestedWeightKg, 0.01)
        assertEquals(8, advice.suggestedReps)
    }

    @Test
    fun testDoubleProgressionMaintainLoad() {
        val plans = PhoneWorkoutPlans()
        val sets = listOf(
            EngineSetRecord(1, 1, "Fekvenyomás", 1, EngineSetType.NORMAL, 80.0, 12, 1, true, 100L),
            EngineSetRecord(2, 1, "Fekvenyomás", 2, EngineSetType.NORMAL, 80.0, 10, 2, true, 200L),
            EngineSetRecord(3, 1, "Fekvenyomás", 3, EngineSetType.NORMAL, 80.0, 9, 0, true, 300L)
        )

        val advice = plans.evaluateProgression(
            exerciseName = "Fekvenyomás",
            model = EngineProgressionModel.DOUBLE_PROGRESSION,
            completedSets = sets,
            targetRepMin = 8,
            targetRepMax = 12
        )

        // Mivel nem minden sorozat érte el a 12-t, a súly marad 80 kg, és +1 ismétlést javasol (9+1 = 10)
        assertEquals(80.0, advice.suggestedWeightKg, 0.01)
        assertEquals(10, advice.suggestedReps)
    }

    @Test
    fun testRestoredCancelBehavior() {
        val execution = PhoneWorkoutExecution()
        execution.startWorkout(101L, "Mellnap", EngineTrainingStyle.NORMAL)
        assertEquals(EngineWorkoutState.WORK, execution.currentState.value)

        // v1.3.71 Restored cancel megerősítés leállítja az időzítőket és reseteli az állapotot
        val result = execution.cancelWorkout()
        assertTrue(result)
        assertEquals(EngineWorkoutState.IDLE, execution.currentState.value)
        assertNull(execution.currentWorkout.value)
        assertEquals(0, execution.restSecondsRemaining.value)
        assertFalse(execution.isRestActive.value)
    }

    @Test
    fun testRestTimerAdjustments() {
        val execution = PhoneWorkoutExecution()
        execution.startWorkout(102L, "Lábnap")
        execution.startRestTimer(90)

        assertTrue(execution.isRestActive.value)
        assertEquals(90, execution.restSecondsRemaining.value)

        // +30 mp hozzáadása
        execution.adjustRestTimer(30)
        assertEquals(120, execution.restSecondsRemaining.value)

        // -15 mp levonása
        execution.adjustRestTimer(-15)
        assertEquals(105, execution.restSecondsRemaining.value)

        // Kihagyás (Skip)
        execution.skipRest()
        assertFalse(execution.isRestActive.value)
        assertEquals(0, execution.restSecondsRemaining.value)
        assertEquals(EngineWorkoutState.WORK, execution.currentState.value)
    }
}
