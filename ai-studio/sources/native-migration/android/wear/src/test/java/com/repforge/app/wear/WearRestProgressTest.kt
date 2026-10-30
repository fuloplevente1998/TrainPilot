package com.repforge.app.wear

import org.junit.Assert.assertEquals
import org.junit.Test

class WearRestProgressTest {
    private fun workout(automatic: WearAutomatic?=null)=WearWorkout(1,"p","Program","a","",
        "2026-10-13T08:00:00Z","rest",0,60,0,emptyList(),automation=automatic)

    @Test fun twoMinuteWatchRestShrinksFromFirstSecondDespiteOneMinuteWorkoutDefault() {
        val w=workout(WearAutomatic(enabled=true,phase="rest",restSeconds=120))
        assertEquals(1f,wearRestProgress(w,120),.00001f)
        assertEquals(119f/120,wearRestProgress(w,119),.00001f)
        assertEquals(.75f,wearRestProgress(w,90),.00001f)
        assertEquals(.5f,wearRestProgress(w,60),.00001f)
        assertEquals(0f,wearRestProgress(w,0),.00001f)
    }
    @Test fun phoneOwnedAndPausedRestUseTheCurrentPhaseDuration() {
        val w=workout(WearAutomatic(enabled=true,owner="phone",phase="rest",restSeconds=180,paused=true))
        assertEquals(150f/180,wearRestProgress(w,150),.00001f)
    }
    @Test fun manualRestAndDisabledAutomationKeepWorkoutDuration() {
        assertEquals(.5f,wearRestProgress(workout(),30),.00001f)
        assertEquals(.5f,wearRestProgress(workout(WearAutomatic(enabled=false,phase="rest",restSeconds=120)),30),.00001f)
    }
    @Test fun zeroDurationAndInvalidRemainingNeverCreateAnInvalidArc() {
        val w=workout(WearAutomatic(enabled=true,phase="rest",restSeconds=0))
        assertEquals(0f,wearRestProgress(w,0),.00001f)
        assertEquals(0f,wearRestProgress(w,-1),.00001f)
        assertEquals(1f,wearRestProgress(w,100),.00001f)
    }
}
