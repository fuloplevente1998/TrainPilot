package com.repforge.app.wear

import kotlin.math.hypot
import kotlin.math.min
import org.junit.Assert.*
import org.junit.Test

class WearHomePhoneLayoutTest {
    @Test fun phoneShortcutFitsTheCircularTopWithoutMovingTheExistingFooter() {
        val screens = listOf(160f, 176f, 180f, 192f, 208f, 225f, 240f, 254f)
            .map { it to it } + listOf(225f to 192f, 192f to 225f)
        for ((width, height) in screens) {
            val layout = wearActionLayout(width, height, 3, 7f)
            val phone = wearHomePhoneLayout(width, height)
            val radius = min(width, height) / 2f
            val endX = (phone.width - phone.height) / 2f
            val centreY = layout.bodyTop + phone.height / 2f - height / 2f
            assertTrue("Phone capsule clipped on $width x $height",
                hypot(endX, centreY) + phone.height / 2f <= radius - 5.99f)
            assertTrue(phone.width >= 48f)
            assertEquals(if (radius < 90f) 36f else 48f, phone.height, .001f)
            assertTrue(phone.height <= layout.bodyHeight)
        }
    }
}
