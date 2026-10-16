package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearUnlockGesture189Test {
    @Test fun onlyASeparateArmedHoldAndReleaseUnlocks() {
        val g=WearUnlockGesture189()
        g.down(0);assertFalse(g.up(3100));assertEquals(0,g.phase(3100))
        g.down(4000);assertFalse(g.up(4100));assertEquals(1,g.phase(4100))
        g.down(4200);assertEquals(2,g.phase(4200));assertEquals(.5f,g.progress(5700),.001f)
        assertEquals(3,g.phase(7200));assertTrue(g.up(7300));assertEquals(0,g.phase(7300))
        assertFalse(g.up(7400))
    }
    @Test fun shortHoldsExpiryMovementMultitouchAndBackgroundCancelTheAttempt() {
        for(cancel in listOf(false,true)){
            val g=WearUnlockGesture189();g.down(0);g.up(100);g.down(200)
            if(cancel)g.cancel()
            assertFalse(g.up(if(cancel)3400 else 2500));assertEquals(0,g.phase(3500))
        }
        val expired=WearUnlockGesture189();expired.down(0);expired.up(100);expired.down(5200);assertFalse(expired.up(8500))
        val stuck=WearUnlockGesture189();stuck.down(0);stuck.up(100);stuck.down(200);assertEquals(0,stuck.phase(6500));assertFalse(stuck.up(6600))
    }
    @Test fun allFourLanguageInstructionsDescribeTheDeliberateGesture() {
        for(lang in listOf("hu","en","de","ro")){
            val c=WearTouchCopy189.language(lang);assertTrue(c.lock.isNotBlank());assertTrue(c.tap.isNotBlank());assertTrue(c.hold.contains('3'));assertTrue(c.release.isNotBlank())
        }
        assertEquals(WearTouchCopy189.language("hu"),WearTouchCopy189.language("hu-HU"))
    }
}
