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

    @Test fun homeArcTargetsAndCaptionsFitWithoutOverlapping() {
        for ((width, height) in listOf(160f to 160f, 176f to 176f, 192f to 192f,
                200f to 200f, 216f to 216f, 225f to 225f, 240f to 240f,
                254f to 254f, 225f to 192f, 192f to 225f)) {
            val layout = wearHomeLayout(width, height)
            val radius = min(width, height) / 2f
            val centers = listOf(-layout.sideOffset to (layout.sideTop + layout.buttonSize / 2f - height / 2f),
                0f to (layout.centerTop + layout.buttonSize / 2f - height / 2f),
                layout.sideOffset to (layout.sideTop + layout.buttonSize / 2f - height / 2f))
            assertEquals(48f, layout.buttonSize, .001f)
            assertTrue("Middle action must be lower", layout.centerTop > layout.sideTop)
            assertTrue("Body must not touch controls", layout.bodyTop + layout.bodyHeight + 4.99f <= layout.sideTop)
            for ((x, y) in centers) {
                assertTrue("Home control clipped at $width x $height", hypot(x, y) + layout.buttonSize / 2f <= radius - 7.99f)
                for (dx in listOf(-22f, 22f)) {
                    assertTrue("Home caption clipped at $width x $height", hypot(x + dx, y + 20f) <= radius - 5.99f)
                }
            }
            for (a in centers.indices) for (b in a + 1 until centers.size) {
                assertTrue("Home touch targets overlap", hypot(centers[a].first - centers[b].first,
                    centers[a].second - centers[b].second) >= layout.buttonSize + 3.99f)
            }
        }
    }

    @Test fun smallPhoneHeaderFitsTheRoundRimAndStaysAboveBody() {
        for (diameter in listOf(160f, 176f, 192f, 200f, 216f, 225f, 240f, 254f)) {
            val layout = wearHomeLayout(diameter, diameter)
            val pillRadius = layout.phone.height / 2f
            val endOffset = layout.phone.width / 2f - pillRadius
            val y = layout.phoneTop + pillRadius - diameter / 2f
            assertTrue("Phone capsule clipped", hypot(endOffset, y) + pillRadius <= diameter / 2f - 5.99f)
            assertTrue(layout.phoneTop + layout.phone.height < layout.bodyTop)
            assertTrue(layout.bodyHeight > 0f)
        }
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
