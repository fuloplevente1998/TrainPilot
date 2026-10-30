package com.repforge.app.wear

import kotlin.math.min
import kotlin.math.sqrt

internal const val WEAR_HEADER_SWEEP = 110f

internal data class WearRoundViewport(val top: Float, val height: Float, val width: Float)

/** The list scrolls over the whole display; individual items scale at the round rim. */
internal fun wearRoundViewport(width: Float, height: Float): WearRoundViewport =
    WearRoundViewport(0f, height, (min(width, height) - 16f).coerceAtLeast(1f))

internal data class WearHomePhoneLayout(val width: Float, val height: Float)

internal fun wearHomePhoneLayout(width: Float, height: Float): WearHomePhoneLayout =
    WearHomePhoneLayout((min(width, height) * .39f).coerceIn(72f, 96f), 32f)

internal data class WearActionLayout(val buttonSize: Float, val gap: Float)

internal fun wearActionLayout(
    width: Float,
    height: Float,
    count: Int,
    preferredGap: Float,
): WearActionLayout {
    val available = wearRoundViewport(width, height).width
    val gap =
        min(
            preferredGap,
            ((available - count * 48f) / (count - 1).coerceAtLeast(1)).coerceAtLeast(0f),
        )
    return WearActionLayout(48f, gap)
}

/** A scrollable arc item keeps three separate 48dp controls and 4dp target separation. */
internal data class WearArcLayout(
    val buttonSize: Float,
    val sideOffset: Float,
    val centerTop: Float,
) {
    val width: Float
        get() = 2f * sideOffset + buttonSize

    val height: Float
        get() = centerTop + buttonSize
}

internal fun wearArcLayout(width: Float, height: Float): WearArcLayout {
    val button = 48f
    val sideOffset = min(45f, (wearRoundViewport(width, height).width - button) / 2f)
    val drop = sqrt((52f * 52f - sideOffset * sideOffset).coerceAtLeast(0f))
    return WearArcLayout(button, sideOffset, drop)
}

internal data class WearFixedPageLayout(
    val bodyTop: Float,
    val bodyHeight: Float,
    val footerTop: Float,
    val footerWidth: Float,
    val footerHeight: Float,
)

/** Locate the controls themselves in the circle, without shrinking a whole rectangular page. */
internal fun wearFixedPageLayout(
    width: Float,
    height: Float,
    arc: Boolean,
    count: Int = 2,
    gap: Float = 7f,
): WearFixedPageLayout {
    val diameter = min(width, height)
    val top = (height - diameter) / 2f
    val footerWidth: Float
    val footerHeight: Float
    val footerTop: Float
    if (arc) {
        val layout = wearArcLayout(width, height)
        footerWidth = layout.width
        footerHeight = layout.height
        footerTop = top + diameter - 10f - footerHeight
    } else {
        val layout = wearActionLayout(width, height, count, gap)
        footerWidth = count * layout.buttonSize + (count - 1) * layout.gap
        footerHeight = layout.buttonSize
        val outerCenterX = (footerWidth - layout.buttonSize) / 2f
        val radius = diameter / 2f - layout.buttonSize / 2f - 6f
        val centerY = sqrt((radius * radius - outerCenterX * outerCenterX).coerceAtLeast(0f))
        footerTop = top + diameter / 2f + centerY - layout.buttonSize / 2f
    }
    val bodyTop = top + 32f
    return WearFixedPageLayout(
        bodyTop,
        (footerTop - bodyTop - 4f).coerceAtLeast(1f),
        footerTop,
        footerWidth,
        footerHeight,
    )
}
