package com.repforge.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.repforge.app.data.store.TrainPilotUserSettings

/** Central paint/contrast policy. Basic is matte; Vivid adds tinted selection surfaces. */
internal object NativeThemeTokens {
    fun parseHex(value: String, fallback: Color): Color =
        if (Regex("#[0-9a-fA-F]{6}").matches(value)) Color(0xFF000000L or value.drop(1).toLong(16)) else fallback

    fun contrast(a: Color, b: Color): Float {
        val first = a.luminance(); val second = b.luminance()
        return (maxOf(first, second) + .05f) / (minOf(first, second) + .05f)
    }
    fun onFill(fill: Color): Color =
        if (contrast(Color.Black, fill) >= contrast(Color.White, fill)) Color.Black else Color.White

    // Accent paint stays unchanged. Small text gets a readable version on both dark card surfaces.
    fun readableAccent(paint: Color): Color {
        for (step in 0..100) {
            val candidate = lerp(paint, Color.White, step / 100f)
            if (contrast(candidate, CardColor) >= 4.5f && contrast(candidate, Card2Color) >= 4.5f) return candidate
        }
        return Color.White
    }
    fun resolve(settings: TrainPilotUserSettings): TrainPilotExtendedColors {
        val accent = parseHex(settings.themeAccent, Color(0xFFF2BD45))
        val secondary = parseHex(settings.themeAccent2, Color(0xFFFFD97D))
        val vivid = settings.themeFamily == "vivid"
        return TrainPilotExtendedColors(accent = accent, accent2 = secondary,
            onAccent = onFill(accent), onAccent2 = onFill(secondary),
            accentText = readableAccent(accent),
            selectedSurface = if (vivid) lerp(Card2Color, accent, .16f) else Card2Color,
            selectedBorder = if (vivid) accent else LineColor,
            emphasisBorder = if (vivid) accent.copy(alpha = .5f) else LineColor)
    }
}
