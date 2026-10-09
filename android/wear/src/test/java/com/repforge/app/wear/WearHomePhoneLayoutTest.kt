package com.repforge.app.wear

import kotlin.math.hypot
import kotlin.math.min
import org.junit.Assert.*
import org.junit.Test

class WearHomePhoneLayoutTest {
    @Test fun phoneShortcutFitsTheRoundHeaderAboveScrollableBody() {
        val screens = listOf(160f, 176f, 180f, 192f, 208f, 225f, 240f, 254f)
            .map { it to it } + listOf(225f to 192f, 192f to 225f)
        for ((width, height) in screens) {
            val layout = wearHomeLayout(width, height)
            val phone = layout.phone
            val radius = min(width, height) / 2f
            val endX = (phone.width - phone.height) / 2f
            val centreY = layout.phoneTop + phone.height / 2f - height / 2f
            assertTrue("Phone capsule clipped on $width x $height",
                hypot(endX, centreY) + phone.height / 2f <= radius - 5.99f)
            assertTrue(phone.width >= 48f)
            assertTrue("Phone header must remain compact", phone.height in 32f..36f)
            assertTrue(layout.phoneTop + phone.height < layout.bodyTop)
        }
    }
}
