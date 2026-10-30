package com.repforge.app.wear

import kotlin.math.min
import org.junit.Assert.*
import org.junit.Test

class WearHomePhoneLayoutTest {
    @Test
    fun phoneShortcutRemainsCompactAndCanBeCenteredOnEveryWatchSize() {
        val screens =
            listOf(160f, 176f, 180f, 192f, 208f, 225f, 240f, 254f).map { it to it } +
                listOf(225f to 192f, 192f to 225f)
        for ((width, height) in screens) {
            val phone = wearHomePhoneLayout(width, height)
            assertTrue(phone.width >= 48f)
            assertTrue(phone.width <= wearRoundViewport(width, height).width)
            assertTrue("Phone shortcut must remain compact", phone.height in 32f..36f)
            assertTrue(
                "Centered capsule fits the round display",
                phone.width / 2 <= min(width, height) / 2 - 6f,
            )
        }
    }
}
