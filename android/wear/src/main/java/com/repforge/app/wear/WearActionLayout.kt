package com.repforge.app.wear

import kotlin.math.min
import kotlin.math.sqrt

/** Positions the action row independently of title height, font scale or scrolling. */
internal data class WearActionLayout(
    val buttonSize: Float,
    val gap: Float,
    val footerWidth: Float,
    val footerTop: Float,
    val bodyTop: Float,
    val bodyHeight: Float
)

internal fun wearActionLayout(width: Float, height: Float, count: Int, preferredGap: Float): WearActionLayout {
    val diameter = min(width, height)
    val edge = min(8f, diameter * .05f)
    val gaps = (count - 1).coerceAtLeast(1)
    val gap = min(preferredGap, ((diameter - 2 * edge - count * 48f) / gaps).coerceAtLeast(0f))
    val button = min(48f, (diameter - 2 * edge - (count - 1) * gap) / count)
    val span = (count - 1) * (button + gap) / 2
    val circleRadius = diameter / 2 - edge
    val circleLimit = sqrt(((circleRadius - button / 2) * (circleRadius - button / 2) - span * span).coerceAtLeast(0f))
    // Captions occupy a rectangle inside each circular button. Keep their
    // outer bottom corners inside the physical display as well as the circles.
    val captionX = span + min(22f, button / 2 - 2)
    val captionBottom = min(20f, button / 2 - 4)
    val captionRadius = diameter / 2 - min(6f, edge)
    val captionLimit = (sqrt((captionRadius * captionRadius - captionX * captionX).coerceAtLeast(0f)) - captionBottom).coerceAtLeast(0f)
    val offset = min(diameter * .22f, min(circleLimit, captionLimit))
    val footerTop = height / 2 + offset - button / 2
    val bodyTop = (height - diameter) / 2 + diameter * .07f
    return WearActionLayout(button, gap, count * button + (count - 1) * gap,
        footerTop, bodyTop, (footerTop - bodyTop - 5f).coerceAtLeast(1f))
}
