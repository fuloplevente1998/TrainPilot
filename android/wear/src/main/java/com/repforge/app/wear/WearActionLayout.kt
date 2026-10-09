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

internal data class WearHomePhoneLayout(val width: Float, val height: Float)

internal fun wearHomePhoneLayout(width: Float, height: Float): WearHomePhoneLayout {
    val diameter = min(width, height)
    return WearHomePhoneLayout((diameter * .39f).coerceIn(72f, 96f), 32f)
}

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

/** Home has a fixed small phone header and three separate controls on a lower arc. */
internal data class WearHomeLayout(
    val phone: WearHomePhoneLayout,
    val phoneTop: Float,
    val buttonSize: Float,
    val sideOffset: Float,
    val sideTop: Float,
    val centerTop: Float,
    val bodyTop: Float,
    val bodyHeight: Float
)

internal fun wearHomeLayout(width: Float, height: Float): WearHomeLayout {
    val diameter = min(width, height)
    val insetTop = (height - diameter) / 2f
    val phone = wearHomePhoneLayout(width, height)
    val phoneTop = insetTop + diameter * .065f
    val button = 48f
    val sideOffset = min(45f, diameter * .245f)
    // Neighboring circular touch targets keep at least 4dp of separation.
    val drop = sqrt(((button + 4f) * (button + 4f) - sideOffset * sideOffset).coerceAtLeast(0f))
    val centerOffset = min(diameter * .32f, diameter / 2f - 8f - button / 2f - 3f)
    val centerTop = height / 2f + centerOffset - button / 2f
    val sideTop = centerTop - drop
    val bodyTop = phoneTop + phone.height + 5f
    return WearHomeLayout(phone, phoneTop, button, sideOffset, sideTop, centerTop,
        bodyTop, (sideTop - bodyTop - 5f).coerceAtLeast(1f))
}
