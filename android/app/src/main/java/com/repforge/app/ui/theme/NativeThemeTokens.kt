package com.repforge.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.repforge.app.data.store.TrainPilotUserSettings

/** Central paint/contrast policy. Stored accent colors are never replaced by contrast fixes. */
internal object NativeThemeTokens {
    fun parseHex(value: String, fallback: Color): Color =
        if (Regex("#[0-9a-fA-F]{6}").matches(value)) Color(0xFF000000L or value.drop(1).toLong(16)) else fallback

    /** WCAG luminance of the actual, composited paint; not an average of encoded RGB bytes. */
    fun contrast(a: Color, b: Color): Float {
        val background = if (b.alpha < 1f) b.compositeOver(BgColor) else b
        val foreground = if (a.alpha < 1f) a.compositeOver(background) else a
        val first = foreground.luminance()
        val second = background.luminance()
        return (maxOf(first, second) + .05f) / (minOf(first, second) + .05f)
    }

    fun onFill(fill: Color): Color =
        if (contrast(Color.Black, fill) >= contrast(Color.White, fill)) Color.Black else Color.White

    /**
     * Preserve the palette's paint accents. Only the derived small-text color may be lightened.
     * Compose Color.lerp interpolates in Oklab: deliberately use it here instead of RGB math.
     */
    private fun readableOnSurfaces(paint: Color, surfaces: List<Color>, minimum: Float): Color {
        for (step in 0..100) {
            val candidate = lerp(paint, Color.White, step / 100f)
            if (surfaces.all { contrast(candidate, it) >= minimum }) return candidate
        }
        return Color.White
    }

    fun readableAccent(paint: Color, additionalSurfaces: List<Color> = emptyList()): Color =
        readableOnSurfaces(paint, listOf(CardColor, Card2Color) + additionalSurfaces, 4.5f)

    /** The selection/focus outline is a functional control indicator, requiring 3:1. */
    fun readableControlAccent(paint: Color, selectedSurface: Color): Color =
        readableOnSurfaces(paint, listOf(CardColor, Card2Color, selectedSurface), 3f)

    fun resolve(settings: TrainPilotUserSettings): TrainPilotExtendedColors {
        val accent = parseHex(settings.themeAccent, Color(0xFF4D8FBE))
        val secondary = parseHex(settings.themeAccent2, Color(0xFF7CB4D6))
        val vivid = settings.themeFamily == "vivid"
        val selectedSurface = if (vivid) lerp(Card2Color, accent, .16f) else Card2Color
        return TrainPilotExtendedColors(
            accent = accent,
            accent2 = secondary,
            onAccent = onFill(accent),
            onAccent2 = onFill(secondary),
            accentText = readableAccent(accent, listOf(selectedSurface)),
            selectedSurface = selectedSurface,
            selectedBorder = readableControlAccent(accent, selectedSurface),
            emphasisBorder = if (vivid) accent.copy(alpha = .5f) else LineColor
        )
    }
}
