package com.repforge.app.wear

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

/** Optional phone palette metadata; old phone snapshots remain readable. */
data class WearHomeTheme(
    val key: String,
    val accent: String,
    val accent2: String,
    val family: String,
) {
    companion object {
        val DEFAULT = WearHomeTheme("classicBlue", "#4d8fbe", "#7cb4d6", "basic")

        fun parse(value: JSONObject?): WearHomeTheme {
            val accent = value?.optString("accent").orEmpty()
            val accent2 = value?.optString("accent2").orEmpty()
            val hex = Regex("#[0-9a-fA-F]{6}")
            if (!hex.matches(accent) || !hex.matches(accent2)) return DEFAULT
            return WearHomeTheme(
                value!!.optString("key").take(32),
                accent,
                accent2,
                if (value.optString("family") == "vivid") "vivid" else "basic",
            )
        }
    }

    val accentColor: Color
        get() = Color(0xFF000000L or accent.substring(1).toLong(16))

    val secondaryColor: Color
        get() = Color(0xFF000000L or accent2.substring(1).toLong(16))

    val frame: Color
        get() = mix(Color(0xFF2C3440), accentColor, if (family == "vivid") .76f else .66f)

    val button: Color
        get() = mix(Color(0xFF232933), accentColor, if (family == "vivid") .24f else .16f)

    // Same gamma-encoded sRGB interpolation as the phone's CSS color-mix(in srgb).
    private fun mix(a: Color, b: Color, amount: Float): Color =
        Color(
            red = a.red * (1f - amount) + b.red * amount,
            green = a.green * (1f - amount) + b.green * amount,
            blue = a.blue * (1f - amount) + b.blue * amount,
        )
}

internal val LocalWearHomeTheme = staticCompositionLocalOf { WearHomeTheme.DEFAULT }
