package com.repforge.app.wear

import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.cos
import org.junit.Assert.*
import org.junit.Test

class WearActionLayoutTest {
    @Test fun scrollingListsAndEditorsKeepTheirWholeViewportInsideTheRoundDisplay() {
        for ((width, height) in viewports()) {
            val layout = wearRoundViewport(width, height)
            val radius = min(width, height) / 2f
            assertTrue("A 48dp control must be fully scrollable into view", layout.height >= 48f)
            assertTrue("Both 48dp adjustment buttons need 6dp separation", layout.width >= 102f)
            assertTrue(layout.top >= 0 && layout.top + layout.height <= height - 16f)
            for (y in listOf(layout.top, layout.top + layout.height)) {
                assertTrue("List corner clips the round rim at $width x $height",
                    hypot(layout.width / 2f, y - height / 2f) <= radius - 7.99f)
            }
            assertHeaderClear(width, height, layout.top)
        }
    }

    @Test fun confirmationAndSummaryKeepEveryActionAndCaptionVisibleOutsideScrollingContent() {
        for ((width, height) in viewports()) for (count in 1..2) {
            assertFits(width, height, count, 7f, curvedTitle = true)
            val layout = wearActionLayout(width, height, count, 7f, curvedTitle = true)
            assertTrue("Keep a useful scrolling text area", layout.bodyHeight >= 25f)
            assertHeaderClear(width, height, layout.bodyTop)
            val radius = min(width, height) / 2f
            for (y in listOf(layout.bodyTop, layout.bodyTop + layout.bodyHeight)) {
                assertTrue("Confirmation text rectangle clips the rim",
                    hypot(layout.bodyWidth / 2f, y - height / 2f) <= radius - 7.99f)
            }
        }
    }

    @Test fun homeContentCornersStayInsideTheRoundDisplay() {
        for ((width, height) in viewports()) {
            val layout = wearHomeLayout(width, height)
            val safeWidth = wearRoundBodyWidth(width, height, layout.bodyTop, layout.bodyTop + layout.bodyHeight)
            for (y in listOf(layout.bodyTop, layout.bodyTop + layout.bodyHeight)) {
                assertTrue(hypot(safeWidth / 2f, y - height / 2f) <= min(width, height) / 2f - 7.99f)
            }
        }
    }

    private fun viewports() = listOf(160f to 160f, 176f to 176f, 192f to 192f,
        200f to 200f, 216f to 216f, 225f to 225f, 240f to 240f, 254f to 254f,
        320f to 320f, 225f to 192f, 192f to 225f)

    private fun assertHeaderClear(width: Float, height: Float, bodyTop: Float) {
        // The capped 13.75sp physical title fits in an 18dp radial band.
        val innerRadius = min(width, height) / 2f - 26f
        val lowestHeaderPoint = height / 2f - innerRadius * cos(Math.toRadians(55.0)).toFloat()
        assertTrue("Curved title must end before the content starts", bodyTop >= lowestHeaderPoint + 3.99f)
    }

    @Test fun workoutArcKeepsTitlesBodyAndAllThreeTouchTargetsClear() {
        for ((width, height) in listOf(160f to 160f, 176f to 176f, 192f to 192f,
                200f to 200f, 216f to 216f, 225f to 225f, 240f to 240f,
                254f to 254f, 225f to 192f, 192f to 225f)) {
            val layout = wearWorkoutLayout(width, height)
            val radius = min(width, height) / 2f
            val centers = listOf(-layout.sideOffset to (layout.sideTop + 24f - height / 2f),
                0f to (layout.centerTop + 24f - height / 2f),
                layout.sideOffset to (layout.sideTop + 24f - height / 2f))
            assertEquals(48f, layout.buttonSize, .001f)
            assertTrue("Workout controls must follow an arc", layout.centerTop > layout.sideTop + 15f)
            assertTrue("Scrolling content must not touch actions", layout.bodyTop + layout.bodyHeight + 4.99f <= layout.sideTop)
            for ((x, y) in centers) {
                assertTrue("Workout control clipped at $width x $height", hypot(x, y) + 24f <= radius - 7.99f)
                for (dx in listOf(-22f, 22f)) {
                    assertTrue("Workout caption clipped at $width x $height", hypot(x + dx, y + 20f) <= radius - 5.99f)
                }
            }
            for (a in centers.indices) for (b in a + 1 until centers.size) {
                assertTrue("Workout touch targets overlap", hypot(centers[a].first - centers[b].first,
                    centers[a].second - centers[b].second) >= 51.99f)
            }
            for (y in listOf(layout.bodyTop, layout.bodyTop + layout.bodyHeight)) {
                assertTrue("Body overlaps curved title or physical rim", hypot(layout.bodyWidth / 2f,
                    y - height / 2f) <= radius - 25.99f)
            }
            assertTrue("Keep room below the middle action for pager dots", layout.centerTop + 48f <= height - 15f)
        }
    }

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

    private fun assertFits(width: Float, height: Float, count: Int, gap: Float, curvedTitle: Boolean = false) {
        val layout = wearActionLayout(width, height, count, gap, curvedTitle)
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
