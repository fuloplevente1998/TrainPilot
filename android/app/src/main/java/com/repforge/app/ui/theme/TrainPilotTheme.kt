package com.repforge.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.repforge.app.data.store.TrainPilotUserSettings

val BgColor = Color(0xFF0E1015)
val SurfaceColor = Color(0xFF161920)
val CardColor = Color(0xFF1A1E25)
val Card2Color = Color(0xFF232933)
val LineColor = Color(0xFF2C3440)
val TextColor = Color(0xFFF6F8FB)
val MutedColor = Color(0xFF9AA5B6)
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
    val success: Color = SuccessColor
)

val LocalTrainPilotColors = staticCompositionLocalOf {
    TrainPilotExtendedColors(
        accent = Color(0xFF4D8FBE),
        accent2 = Color(0xFF7CB4D6)
    )
}

val TrainPilotTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        color = TextColor
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = TextColor
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = TextColor
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = TextColor
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = TextColor
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = TextColor
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = MutedColor
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        color = MutedColor
    )
)

@Composable
fun TrainPilotTheme(
    settings: TrainPilotUserSettings,
    content: @Composable () -> Unit
) {
    val colors = androidx.compose.runtime.remember(settings.themeAccent, settings.themeAccent2, settings.themeFamily) {
        NativeThemeTokens.resolve(settings)
    }
    val colorScheme = darkColorScheme(
        primary = colors.accent, secondary = colors.accent2,
        background = colors.bg, surface = colors.card, surfaceVariant = colors.card2,
        onPrimary = colors.onAccent, onSecondary = colors.onAccent2,
        primaryContainer = colors.selectedSurface, onPrimaryContainer = colors.text,
        onBackground = colors.text, onSurface = colors.text, onSurfaceVariant = colors.muted,
        outline = colors.line
    )

    CompositionLocalProvider(LocalTrainPilotColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TrainPilotTypography,
            content = content
        )
    }
}
