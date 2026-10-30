package com.repforge.app.wear

import kotlin.math.hypot
import kotlin.math.min
import org.junit.Assert.*
import org.junit.Test

class WearActionLayoutTest {
    private fun viewports() =
        listOf(
            160f to 160f,
            176f to 176f,
            192f to 192f,
            200f to 200f,
            216f to 216f,
            225f to 225f,
            240f to 240f,
            254f to 254f,
            320f to 320f,
            225f to 192f,
            192f to 225f,
        )

    @Test
    fun listViewportUsesTheFullHeightAndAtLeastNinetyPercentOfTheRoundWidth() {
        for ((width, height) in viewports()) {
            val viewport = wearRoundViewport(width, height)
            assertEquals(0f, viewport.top, .001f)
            assertEquals(height, viewport.height, .001f)
            assertTrue(
                "A central strip must never return",
                viewport.width >= min(width, height) * .9f,
            )
            assertTrue(viewport.width <= width)
        }
    }

    @Test
    fun centeredArcKeepsThreeSeparate48dpTargetsAndCaptionsInsideTheCircle() {
        for ((width, height) in viewports()) {
            val layout = wearArcLayout(width, height)
            assertEquals(48f, layout.buttonSize, .001f)
            assertTrue(layout.centerTop >= 25f)
            assertTrue(layout.width <= wearRoundViewport(width, height).width)
            val centers =
                listOf(
                    -layout.sideOffset to (-layout.centerTop / 2),
                    0f to (layout.centerTop / 2),
                    layout.sideOffset to (-layout.centerTop / 2),
                )
            val radius = min(width, height) / 2f
            for ((x, y) in centers) {
                assertTrue(hypot(x, y) + 24f <= radius - 7.99f)
                for (dx in listOf(-22f, 22f)) {
                    assertTrue(
                        "Caption fits when scrolled to center",
                        hypot(x + dx, y + 20f) <= radius - 5.99f,
                    )
                }
            }
            for (a in centers.indices) for (b in a + 1 until centers.size) {
                assertTrue(
                    "Touch targets must not overlap",
                    hypot(
                        centers[a].first - centers[b].first,
                        centers[a].second - centers[b].second,
                    ) >= 51.99f,
                )
            }
        }
    }

    @Test
    fun centeredConfirmationsAndEditorsKeepFullSizeControls() {
        for ((width, height) in viewports()) for (gap in listOf(6f, 7f, 13f)) {
            val layout = wearActionLayout(width, height, 2, gap)
            assertEquals(48f, layout.buttonSize, .001f)
            assertEquals(gap, layout.gap, .001f)
            assertTrue(2 * layout.buttonSize + layout.gap <= wearRoundViewport(width, height).width)
            val centerX = (layout.buttonSize + layout.gap) / 2
            assertTrue(hypot(centerX, 0f) + 24f <= min(width, height) / 2 - 7.99f)
        }
    }

    @Test
    fun fixedFootersStayInsideTheCircleAtEverySupportedSize() {
        for ((width, height) in viewports()) for (arc in listOf(false, true)) {
            val count = if (arc) 3 else 2
            val page = wearFixedPageLayout(width, height, arc, count)
            val diameter = min(width, height)
            val origin = (height - diameter) / 2f
            val centers =
                if (arc) {
                    val layout = wearArcLayout(width, height)
                    listOf(
                        -layout.sideOffset to (page.footerTop + 24f),
                        0f to (page.footerTop + layout.centerTop + 24f),
                        layout.sideOffset to (page.footerTop + 24f),
                    )
                } else {
                    val layout = wearActionLayout(width, height, count, 7f)
                    val x = (layout.buttonSize + layout.gap) / 2f
                    listOf(-x to (page.footerTop + 24f), x to (page.footerTop + 24f))
                }
            for ((x, y) in centers) {
                assertTrue(
                    "A fixed footer target must fit the physical round display",
                    hypot(x, y - origin - diameter / 2f) + 24f <= diameter / 2f - 3.9f,
                )
                for (dx in listOf(-22f, 22f)) {
                    assertTrue(
                        "Its caption must fit the physical round display",
                        hypot(x + dx, y + 20f - origin - diameter / 2f) <= diameter / 2f,
                    )
                }
            }
            assertTrue(page.bodyTop + page.bodyHeight < page.footerTop)
        }
    }
}
