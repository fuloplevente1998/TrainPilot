package com.repforge.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PilotPrimary,
    secondary = PilotSecondary,
    background = PilotBackgroundDark,
    surface = PilotSurfaceDark,
    surfaceVariant = PilotSurfaceVariantDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = PilotError
)

private val AmoledColorScheme = darkColorScheme(
    primary = PilotPrimary,
    secondary = PilotSecondary,
    background = PilotAmoledBackground,
    surface = PilotAmoledSurface,
    surfaceVariant = PilotAmoledVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = PilotError
)

private val MidnightColorScheme = darkColorScheme(
    primary = PilotPrimary,
    secondary = PilotSecondary,
    background = PilotMidnightBackground,
    surface = PilotMidnightSurface,
    surfaceVariant = PilotMidnightVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = PilotError
)

private val EmeraldColorScheme = darkColorScheme(
    primary = PilotEmeraldPrimary,
    secondary = PilotEmeraldSecondary,
    background = PilotEmeraldBackground,
    surface = PilotEmeraldSurface,
    surfaceVariant = PilotEmeraldVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = PilotError
)

@Composable
fun TrainPilotTheme(
    selectedTheme: String = "DARK",
    content: @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme.uppercase()) {
        "AMOLED" -> AmoledColorScheme
        "MIDNIGHT" -> MidnightColorScheme
        "EMERALD" -> EmeraldColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
