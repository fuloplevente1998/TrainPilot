package com.repforge.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.repforge.app.data.store.TrainPilotUserSettings

val BgColor = Color(0xFF0D1015)
val SurfaceColor = Color(0xFF161B22)
val CardColor = Color(0xFF161B22)
val Card2Color = Color(0xFF1E252F)
val LineColor = Color(0xFF2A3341)
val TextColor = Color(0xFFF8FAFC)
val MutedColor = Color(0xFF94A3B8)
val DangerColor = Color(0xFF3B232A)
val DangerTextColor = Color(0xFFEFBCC3)
val SuccessColor = Color(0xFF3CA87D)

data class TrainPilotExtendedColors(
    val bg: Color = BgColor,
    val card: Color = CardColor,
    val card2: Color = Card2Color,
    val line: Color = LineColor,
    val text: Color = TextColor,
    val muted: Color = MutedColor,
    val accent: Color,
    val accent2: Color,
    val onAccent: Color = NativeThemeTokens.onFill(accent),
    val onAccent2: Color = NativeThemeTokens.onFill(accent2),
    val accentText: Color = NativeThemeTokens.readableAccent(accent),
    val selectedSurface: Color = Card2Color,
    val selectedBorder: Color = NativeThemeTokens.readableControlAccent(accent, selectedSurface),
    val emphasisBorder: Color = LineColor,
    val danger: Color = DangerColor,
    val dangerText: Color = DangerTextColor,
    // Main #177's relative-load encoding; semantic data colors, independent of accent selection.
    val loadLow: Color = Color(0xFFF0C83E),
    val loadMedium: Color = Color(0xFFF28A28),
    val loadHigh: Color = Color(0xFFE84E4E),
    val success: Color = SuccessColor
)

val LocalTrainPilotColors = staticCompositionLocalOf {
    TrainPilotExtendedColors(
        accent = Color(0xFF4D8FBE),
        accent2 = Color(0xFF7CB4D6)
    )
}

internal fun nativeMaterialColorScheme(colors: TrainPilotExtendedColors) = darkColorScheme(
    primary = colors.accent, secondary = colors.accent2,
    background = colors.bg, surface = colors.card, surfaceVariant = colors.card2,
    onPrimary = colors.onAccent, onSecondary = colors.onAccent2,
    primaryContainer = colors.selectedSurface, onPrimaryContainer = colors.text,
    onBackground = colors.text, onSurface = colors.text, onSurfaceVariant = colors.muted,
    error = colors.dangerText, onError = colors.danger,
    errorContainer = colors.danger, onErrorContainer = colors.dangerText,
    outline = NativeThemeTokens.readableControlAccent(colors.line, colors.selectedSurface),
    outlineVariant = colors.line
)

@Composable
fun TrainPilotTheme(
    settings: TrainPilotUserSettings,
    content: @Composable () -> Unit
) {
    val colors = androidx.compose.runtime.remember(settings.themeAccent, settings.themeAccent2, settings.themeFamily) {
        NativeThemeTokens.resolve(settings)
    }
    val colorScheme = androidx.compose.runtime.remember(colors) { nativeMaterialColorScheme(colors) }

    CompositionLocalProvider(LocalTrainPilotColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TrainPilotTypography,
            shapes = TrainPilotShapes,
            content = content
        )
    }
}
