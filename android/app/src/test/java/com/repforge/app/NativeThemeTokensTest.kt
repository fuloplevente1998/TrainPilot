package com.repforge.app

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class NativeThemeTokensTest {
    @Test fun everyPaletteHasReadableButtonAndSmallTextContrast() {
        for (theme in TrainPilotSettingsStore.AVAILABLE_THEMES) {
            val colors = NativeThemeTokens.resolve(TrainPilotUserSettings(themeKey = theme.key, themeAccent = theme.accent,
                themeAccent2 = theme.accent2, themeFamily = theme.family))
            assertTrue(theme.key, NativeThemeTokens.contrast(colors.onAccent, colors.accent) >= 4.5f)
            assertTrue(theme.key, NativeThemeTokens.contrast(colors.onAccent2, colors.accent2) >= 4.5f)
            assertTrue(theme.key, NativeThemeTokens.contrast(colors.accentText, colors.card) >= 4.5f)
            assertTrue(theme.key, NativeThemeTokens.contrast(colors.accentText, colors.card2) >= 4.5f)
            assertEquals(theme.accent.lowercase(), "#%06x".format(colors.accent.toArgb() and 0xFFFFFF))
        }
    }
    @Test fun basicIsMatteAndVividHasAnAccentSelectionWithoutChangingSemanticDanger() {
        val basic = NativeThemeTokens.resolve(TrainPilotUserSettings(themeKey = "classicBlue", themeFamily = "basic"))
        val vivid = NativeThemeTokens.resolve(TrainPilotUserSettings(themeKey = "blue", themeFamily = "vivid"))
        assertEquals(Card2Color, basic.selectedSurface)
        assertEquals(LineColor, basic.emphasisBorder)
        assertNotEquals(basic.selectedSurface, vivid.selectedSurface)
        assertNotEquals(basic.emphasisBorder, vivid.emphasisBorder)
        // An already readable palette paint can be the same outline in both families.
        // Family distinction lives in the selected fill/emphasis, never lower contrast.
        for (colors in listOf(basic, vivid)) {
            assertTrue(NativeThemeTokens.contrast(colors.selectedBorder, colors.selectedSurface) >= 3f)
            assertTrue(NativeThemeTokens.contrast(colors.selectedBorder, colors.card) >= 3f)
            assertTrue(NativeThemeTokens.contrast(colors.selectedBorder, colors.card2) >= 3f)
        }
        assertEquals(basic.danger, vivid.danger)
        assertEquals(basic.dangerText, vivid.dangerText)
    }
    @Test fun materialInputsAndErrorsHaveReadableSemanticColorsAcrossEveryPalette() {
        for (theme in TrainPilotSettingsStore.AVAILABLE_THEMES) {
            val colors = NativeThemeTokens.resolve(TrainPilotUserSettings(themeKey = theme.key, themeAccent = theme.accent,
                themeAccent2 = theme.accent2, themeFamily = theme.family))
            val material = nativeMaterialColorScheme(colors)
            assertEquals(theme.key, colors.accent, material.primary)
            assertTrue(theme.key, NativeThemeTokens.contrast(material.onPrimary, material.primary) >= 4.5f)
            assertTrue(theme.key, NativeThemeTokens.contrast(material.error, material.surface) >= 4.5f)
            assertTrue(theme.key, NativeThemeTokens.contrast(material.onErrorContainer, material.errorContainer) >= 4.5f)
            assertTrue(theme.key, NativeThemeTokens.contrast(material.outline, material.surfaceVariant) >= 3f)
        }
    }

    @Test fun malformedHexDoesNotCrashAndDoesNotAcceptTransparentAccent() {
        for (value in listOf("", "#123", "#00123456", "blue", "#ggffff"))
            assertEquals(Color.Red, NativeThemeTokens.parseHex(value, Color.Red))
        assertEquals(Color(0xFF112233), NativeThemeTokens.parseHex("#112233", Color.Red))
    }
    @Test fun darkAndLightButtonFillsChooseDifferentForegrounds() {
        assertEquals(Color.White, NativeThemeTokens.onFill(Color.Black))
        assertEquals(Color.Black, NativeThemeTokens.onFill(Color.White))
    }
}
