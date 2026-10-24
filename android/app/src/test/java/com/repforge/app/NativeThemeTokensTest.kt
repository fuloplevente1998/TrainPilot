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
            val colors = NativeThemeTokens.resolve(TrainPilotUserSettings(themeAccent = theme.accent,
                themeAccent2 = theme.accent2, themeFamily = theme.family))
            assertTrue(theme.key, NativeThemeTokens.contrast(colors.onAccent, colors.accent) >= 4.5f)
            assertTrue(theme.key, NativeThemeTokens.contrast(colors.onAccent2, colors.accent2) >= 4.5f)
            assertTrue(theme.key, NativeThemeTokens.contrast(colors.accentText, colors.card) >= 4.5f)
            assertTrue(theme.key, NativeThemeTokens.contrast(colors.accentText, colors.card2) >= 4.5f)
            assertEquals(theme.accent.lowercase(), "#%06x".format(colors.accent.toArgb() and 0xFFFFFF))
        }
    }
    @Test fun basicIsMatteAndVividHasAnAccentSelectionWithoutChangingSemanticDanger() {
        val basic = NativeThemeTokens.resolve(TrainPilotUserSettings(themeFamily = "basic"))
        val vivid = NativeThemeTokens.resolve(TrainPilotUserSettings(themeFamily = "vivid"))
        assertEquals(Card2Color, basic.selectedSurface)
        assertEquals(LineColor, basic.emphasisBorder)
        assertNotEquals(basic.selectedSurface, vivid.selectedSurface)
        assertNotEquals(basic.selectedBorder, vivid.selectedBorder)
        assertEquals(basic.danger, vivid.danger)
        assertEquals(basic.dangerText, vivid.dangerText)
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
    @Test fun firstRunUsesThePinnedGoldPaletteAndKeepsExplicitSavedColors() {
        val default = NativeThemeTokens.resolve(TrainPilotUserSettings())
        assertEquals(Color(0xFFF2BD45), default.accent)
        assertEquals(Color(0xFFFFD97D), default.accent2)

        val saved = NativeThemeTokens.resolve(
            TrainPilotUserSettings(themeKey = "classicBlue", themeAccent = "#4d8fbe",
                themeAccent2 = "#7cb4d6", themeFamily = "basic")
        )
        assertEquals(Color(0xFF4D8FBE), saved.accent)
        assertEquals(Color(0xFF7CB4D6), saved.accent2)
    }
}
