package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

val LocalTrainPilotPalette = staticCompositionLocalOf {
    TrainPilotPalettes.allPalettes.first()
}

@Composable
fun TrainPilotTheme(
    palette: TrainPilotPalette = TrainPilotPalettes.allPalettes.first(),
    fontScaleMultiplier: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.surfaceElevated,
            onPrimaryContainer = palette.primaryBright,
            secondary = palette.secondary,
            onSecondary = palette.background,
            secondaryContainer = palette.surfaceElevated,
            onSecondaryContainer = palette.textPrimary,
            tertiary = palette.tertiary,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.cardBorder,
            error = palette.danger
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.surfaceElevated,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.secondary,
            onSecondary = palette.surface,
            secondaryContainer = palette.surfaceElevated,
            onSecondaryContainer = palette.textPrimary,
            tertiary = palette.tertiary,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.cardBorder,
            error = palette.danger
        )
    }

    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = (currentDensity.fontScale * fontScaleMultiplier).coerceIn(0.85f, 1.6f)
    )

    CompositionLocalProvider(
        LocalTrainPilotPalette provides palette,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
