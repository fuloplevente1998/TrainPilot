package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearUnlockGesture189Test {
    @Test fun oneContinuousHoldUnlocksAtThreeSecondsWithoutTapOrRelease() {
        val g=WearUnlockGesture189()
        g.down(0);assertTrue(g.holding);assertEquals(.5f,g.progress(1500),.001f)
        assertFalse(g.tryUnlock(2999));assertTrue(g.tryUnlock(3000))
        assertFalse(g.holding);assertFalse(g.tryUnlock(3100));assertFalse(g.up(3200))
        // A delayed frame still completes the same hold, without an upper time limit.
        g.down(4000);assertTrue(g.tryUnlock(11000));assertFalse(g.up(11100))
    }
    @Test fun shortTouchesNeverAccumulateAndCancellationDiscardsTheWholeHold() {
        val g=WearUnlockGesture189()
        g.down(0);assertFalse(g.up(1500));g.down(1600);assertFalse(g.up(3100))
        assertFalse(g.tryUnlock(6000));assertEquals(0f,g.progress(6000),.001f)
        g.down(7000);g.cancel();assertFalse(g.tryUnlock(11000));assertFalse(g.up(11100))
        g.down(12000);assertFalse(g.tryUnlock(14999));assertTrue(g.up(15000));assertFalse(g.up(15100))
    }
    @Test fun allFourLanguageInstructionsDescribeTheDeliberateGesture() {
        for(lang in listOf("hu","en","de","ro")){
            val c=WearTouchCopy189.language(lang);assertTrue(c.lock.isNotBlank());assertTrue(c.hold.contains('3'))
        }
        assertEquals(WearTouchCopy189.language("hu"),WearTouchCopy189.language("hu-HU"))
    }
}
