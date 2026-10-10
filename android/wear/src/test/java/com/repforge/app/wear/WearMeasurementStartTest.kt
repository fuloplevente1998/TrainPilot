package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearMeasurementStartTest {
    @Test fun remoteOpenBeforeDataDeliveryCannotStartTheCachedPreviousWorkout() {
        val request=WearMeasurementStart.parse("new-phone-workout","42")!!
        assertFalse(request.matches("previous-workout",100L))
        assertFalse(request.matches(null,0L))
        assertFalse(request.matches("new-phone-workout",41L))
        assertTrue(request.matches("new-phone-workout",42L))
        assertTrue(request.matches("new-phone-workout",43L))
        assertFalse(request.matches("next-workout",44L))
    }

    @Test fun invalidLaunchRequestsDoNotRelaxTheIdentityGuard() {
        for(id in listOf(null,"","   ","x".repeat(513)))assertNull(WearMeasurementStart.parse(id,"1"))
        for(revision in listOf(null,"","-1","0","NaN","9223372036854775808"))
            assertNull(WearMeasurementStart.parse("current",revision))
        val id="2026-10-10T08:00:00.123456789Z"
        assertEquals(id,WearMeasurementStart.parse(id,"1")!!.workoutId)
    }
}
