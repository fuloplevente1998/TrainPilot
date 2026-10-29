package com.repforge.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.repforge.app.R

// Original font bytes from the user-supplied Studio export. These are static regular
// faces: register their real weight and let Compose synthesize missing bold weights.
// Font licenses are bundled in assets/font-licenses. Never clamp system fontScale.
val TrainPilotDisplayFont = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))
val TrainPilotBodyFont = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val TrainPilotNumericFont = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))

private fun studioText(size: Float, height: Int, weight: FontWeight = FontWeight.Normal,
    family: FontFamily = TrainPilotBodyFont) = TextStyle(fontFamily = family,
    fontWeight = weight, fontSize = size.sp, lineHeight = height.sp, fontFeatureSettings = "tnum")

val TrainPilotTypography = Typography(
    displayLarge = studioText(34f, 40, FontWeight.Bold, TrainPilotDisplayFont),
    displayMedium = studioText(30f, 36, FontWeight.Bold, TrainPilotDisplayFont),
    displaySmall = studioText(28f, 34, FontWeight.Bold, TrainPilotDisplayFont),
    headlineLarge = studioText(28f, 34, FontWeight.Bold, TrainPilotDisplayFont),
    headlineMedium = studioText(22f, 28, FontWeight.Bold, TrainPilotDisplayFont),
    headlineSmall = studioText(22f, 28, FontWeight.SemiBold, TrainPilotDisplayFont),
    titleLarge = studioText(19f, 25, FontWeight.Bold),
    titleMedium = studioText(16f, 22, FontWeight.SemiBold),
    titleSmall = studioText(14f, 19, FontWeight.SemiBold),
    bodyLarge = studioText(15f, 22),
    bodyMedium = studioText(13.5f, 19),
    bodySmall = studioText(12f, 16),
    labelLarge = studioText(14f, 18, FontWeight.Bold),
    labelMedium = studioText(12f, 16, FontWeight.Medium),
    labelSmall = studioText(11f, 14, FontWeight.Medium, TrainPilotNumericFont)
)
