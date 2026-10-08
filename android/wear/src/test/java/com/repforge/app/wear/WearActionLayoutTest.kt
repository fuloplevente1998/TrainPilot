package com.repforge.app.wear

import kotlin.math.hypot
import kotlin.math.min
import org.junit.Assert.*
import org.junit.Test

class WearActionLayoutTest {
    @Test fun workoutCirclesAndCaptionCornersFitRoundDisplays() {
        for (diameter in listOf(160f, 176f, 192f, 200f, 216f, 225f, 240f, 254f)) {
            assertFits(diameter, diameter, 3, 7f)
        }
    }

    @Test fun editorActionsFitWithoutSquashingTheLastButton() {
        for (diameter in listOf(160f, 176f, 192f, 200f, 216f, 225f, 240f, 254f)) {
            assertFits(diameter, diameter, 2, 13f)
            assertEquals(48f, wearActionLayout(diameter, diameter, 2, 13f).buttonSize, .001f)
        }
    }

    @Test fun rectangularContentViewportUsesTheInscribedCircle() {
        assertFits(225f, 192f, 3, 7f)
        assertFits(192f, 225f, 3, 7f)
        assertFits(225f, 192f, 2, 13f)
    }

    private fun assertFits(width: Float, height: Float, count: Int, gap: Float) {
        val layout = wearActionLayout(width, height, count, gap)
        val radius = min(width, height) / 2
        val centerY = layout.footerTop + layout.buttonSize / 2 - height / 2
        assertEquals(48f, layout.buttonSize, .001f)
        assertTrue(layout.bodyTop >= 0)
        assertTrue(layout.bodyTop + layout.bodyHeight + 4.99f <= layout.footerTop)
        for (index in 0 until count) {
            val centerX = (index - (count - 1) / 2f) * (layout.buttonSize + layout.gap)
            assertTrue("Circle clipped: $width x $height, button $index",
                hypot(centerX, centerY) + layout.buttonSize / 2 <= radius - 7.99f)
            for (dx in listOf(-22f, 22f)) {
                assertTrue("Caption clipped: $width x $height, button $index",
                    hypot(centerX + dx, centerY + 20f) <= radius - 5.99f)
            }
        }
    }
}
