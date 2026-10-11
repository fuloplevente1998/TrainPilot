package com.repforge.app.wear

import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.cos

/** Positions the action row independently of title height, font scale or scrolling. */
internal data class WearActionLayout(
    val buttonSize: Float,
    val gap: Float,
    val footerWidth: Float,
    val footerTop: Float,
    val bodyTop: Float,
    val bodyHeight: Float,
    val bodyWidth: Float
)

internal const val WEAR_HEADER_SWEEP = 110f

internal data class WearRoundViewport(val top: Float, val height: Float, val width: Float)

/** Keep the whole scroll viewport inside the circle, including its corners. */
internal fun wearRoundBodyWidth(width: Float, height: Float, top: Float, bottom: Float): Float {
    val radius = min(width, height) / 2f - 8f
    val offset = maxOf(kotlin.math.abs(top - height / 2f), kotlin.math.abs(bottom - height / 2f))
    return (2f * sqrt((radius * radius - offset * offset).coerceAtLeast(0f)) - 4f).coerceAtLeast(1f)
}

/** The curved header has an 8dp rim inset and at most 18dp physical text height. */
internal fun wearRoundBodyTop(width: Float, height: Float): Float {
    val diameter = min(width, height)
    val headerBottom = height / 2f - (diameter / 2f - 26f) *
        cos(Math.toRadians((WEAR_HEADER_SWEEP / 2f).toDouble())).toFloat()
    return maxOf((height - diameter) / 2f + diameter * .30f, headerBottom + 4f)
}

internal fun wearRoundViewport(width: Float, height: Float): WearRoundViewport {
    val diameter = min(width, height)
    val top = wearRoundBodyTop(width, height)
    val bottom = (height - diameter) / 2f + diameter * .80f
    return WearRoundViewport(top, bottom - top, wearRoundBodyWidth(width, height, top, bottom))
}

internal data class WearHomePhoneLayout(val width: Float, val height: Float)

internal fun wearHomePhoneLayout(width: Float, height: Float): WearHomePhoneLayout {
    val diameter = min(width, height)
    return WearHomePhoneLayout((diameter * .39f).coerceIn(72f, 96f), 32f)
}

internal fun wearActionLayout(width: Float, height: Float, count: Int, preferredGap: Float,
                              curvedTitle: Boolean = false): WearActionLayout {
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
    val bodyTop = if (curvedTitle) wearRoundBodyTop(width, height) else (height - diameter) / 2f + diameter * .07f
    val bodyBottom = footerTop - 5f
    return WearActionLayout(button, gap, count * button + (count - 1) * gap,
        footerTop, bodyTop, (bodyBottom - bodyTop).coerceAtLeast(1f),
        wearRoundBodyWidth(width, height, bodyTop, bodyBottom))
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

/** Workout title follows the top rim; the controls follow the lower arc. */
internal data class WearWorkoutLayout(
    val buttonSize: Float,
    val sideOffset: Float,
    val sideTop: Float,
    val centerTop: Float,
    val bodyTop: Float,
    val bodyHeight: Float,
    val bodyWidth: Float
)

internal fun wearWorkoutLayout(width: Float, height: Float): WearWorkoutLayout {
    val diameter = min(width, height)
    val button = 48f
    val sideOffset = min(45f + 3.8f * ((diameter - 200f) / 25f).coerceIn(0f, 1f), diameter * .245f)
    val drop = sqrt((52f * 52f - sideOffset * sideOffset).coerceAtLeast(0f))
    val centerOffset = min(diameter * .32f, diameter / 2f - 42f)
    val centerTop = height / 2f + centerOffset - button / 2f
    val sideTop = centerTop - drop
    val bodyTop = (height - diameter) / 2f + diameter * .20f
    // The whole scrolling rectangle stays inside the curved title's inner
    // edge. Its top corners must fit too, not just its centered first line.
    val innerRadius = diameter / 2f - 26f
    val topOffset = height / 2f - bodyTop
    val bodyWidth = min(132f, 2f * sqrt((innerRadius * innerRadius - topOffset * topOffset).coerceAtLeast(0f)) - 4f)
    return WearWorkoutLayout(button, sideOffset, sideTop, centerTop, bodyTop,
        (sideTop - bodyTop - 5f).coerceAtLeast(1f), bodyWidth.coerceAtLeast(40f))
}
