package com.repforge.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.data.store.TrainPilotUserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Production AVAILABLE_THEMES -> production resolve(). No replicated test palette. */
class NativeThemeContrastParityTest {
    private val backgrounds = listOf(BgColor, CardColor, Card2Color)
    private val palettes get() = TrainPilotSettingsStore.AVAILABLE_THEMES

    private fun assertContrast(name: String, foreground: Color, background: Color, minimum: Float) {
        val measured = NativeThemeTokens.contrast(foreground, background)
        assertTrue("$name: $measured < $minimum", measured >= minimum)
    }

    @Test fun everyStoredPaletteKeepsItsPaintAndAllTextAndSelectionThresholds() {
        assertEquals("All ten final main Basic + sixteen main Vivid + six legacy native keys", 32, palettes.size)
        assertEquals(32, palettes.map { it.key }.toSet().size)
        palettes.forEach { palette ->
            val settings = TrainPilotUserSettings(
                themeKey = palette.key,
                themeAccent = palette.accent,
                themeAccent2 = palette.accent2,
                themeFamily = palette.family
            )
            val colors = NativeThemeTokens.resolve(settings)
            assertEquals("${palette.key}: accent changed", NativeThemeTokens.parseHex(palette.accent, Color.Black), colors.accent)
            assertEquals("${palette.key}: accent2 changed", NativeThemeTokens.parseHex(palette.accent2, Color.Black), colors.accent2)
            assertContrast("${palette.key}: onAccent", colors.onAccent, colors.accent, 4.5f)
            assertContrast("${palette.key}: onAccent2", colors.onAccent2, colors.accent2, 4.5f)
            backgrounds.forEachIndexed { index, background ->
                assertContrast("${palette.key}: text/$index", colors.text, background, 4.5f)
                assertContrast("${palette.key}: muted/$index", colors.muted, background, 4.5f)
                assertContrast("${palette.key}: accentText/$index", colors.accentText, background, 4.5f)
                assertContrast("${palette.key}: dangerText/$index", colors.dangerText, background, 4.5f)
            }
            assertContrast("${palette.key}: danger text on danger", colors.dangerText, colors.danger, 4.5f)
            assertContrast("${palette.key}: selected label", colors.accentText, colors.selectedSurface, 4.5f)
            assertContrast("${palette.key}: selected border on selected", colors.selectedBorder, colors.selectedSurface, 3f)
            assertContrast("${palette.key}: selected border on card", colors.selectedBorder, colors.card, 3f)
            assertContrast("${palette.key}: selected border on card2", colors.selectedBorder, colors.card2, 3f)
            assertContrast("${palette.key}: ordinary button", colors.onAccent, colors.accent, 4.5f)
            assertContrast("${palette.key}: secondary button", colors.onAccent2, colors.accent2, 4.5f)
        }
    }

    @Test fun invalidAndLegacyColorFormatsCannotReplaceSavedAccentsSilently() {
        val fallback = Color(0xFF4D8FBE)
        listOf("", "oops", "#123", "#12345678", "#zzzzzz", "  #112233").forEach {
            assertEquals(it, fallback, NativeThemeTokens.parseHex(it, fallback))
        }
        assertEquals(Color(0xFFABCDEF), NativeThemeTokens.parseHex("#AbCdEf", fallback))
        assertEquals(Color(0xFF000000), NativeThemeTokens.parseHex("#000000", fallback))
        assertEquals(Color.White, NativeThemeTokens.onFill(Color.Black))
        assertEquals(Color.Black, NativeThemeTokens.onFill(Color.White))
        listOf("#000000", "#111111", "#777777", "#bbbbbb", "#ffffff").forEach { hex ->
            val fill = NativeThemeTokens.parseHex(hex, fallback)
            assertContrast("onFill/$hex", NativeThemeTokens.onFill(fill), fill, 4.5f)
        }
    }

    @Test fun translucentPaintIsCompositedBeforeContrastRatherThanTreatedAsOpaque() {
        val halfWhite = Color.White.copy(alpha = .5f)
        val measured = NativeThemeTokens.contrast(halfWhite, Color.Black)
        val expected = NativeThemeTokens.contrast(Color(0xFF808080), Color.Black)
        assertEquals(expected.toDouble(), measured.toDouble(), .10)
    }

    @Test fun allFourLocalesKeepRealThemeNamesIncludingLongGermanAndRomanianNames() {
        palettes.forEach { option ->
            for (locale in listOf("hu", "en", "de", "ro")) {
                assertFalse("${option.key} / $locale", option.displayName(locale).isBlank())
            }
        }
        assertTrue(palettes.any { it.displayName("de").length >= 25 })
        assertTrue(palettes.any { it.displayName("ro").length >= 20 })
        assertEquals("classicBlue", TrainPilotUserSettings().themeKey)
    }
}
