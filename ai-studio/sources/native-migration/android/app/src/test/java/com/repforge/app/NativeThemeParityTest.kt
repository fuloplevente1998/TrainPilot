package com.repforge.app

import com.repforge.app.data.store.TrainPilotSettingsStore
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NativeThemeParityTest {
    @Test fun allMainThemesKeepTheirKeysColorsAndFamilies() {
        val expected = JSONObject(javaClass.getResource("/main-7473b30-themes.json")!!.readText())
        assertEquals(26, expected.length())
        for (key in expected.keys()) {
            val theme = TrainPilotSettingsStore.AVAILABLE_THEMES.single { it.key == key }
            assertEquals(expected.getJSONObject(key).getString("accent"), theme.accent)
            assertEquals(expected.getJSONObject(key).getString("accent2"), theme.accent2)
            assertEquals(expected.getJSONObject(key).getString("family"), theme.family)
            assertEquals(key, TrainPilotSettingsStore.importedTheme(JSONObject().put("themeAccent", key))!!.key)
        }
    }
    @Test fun previouslySavedNativeKeysAreStillSelectable() {
        for (key in listOf("classicBlue", "amberGold", "emerald", "slateCoral", "vividCyan", "vividGold", "vividEmerald"))
            assertEquals(key, TrainPilotSettingsStore.importedTheme(JSONObject().put("themeKey", key))!!.key)
    }
    @Test fun nativeBackupKeyWinsOverLegacyKeyAndHexDoesNotInventATheme() {
        val data = JSONObject().put("themeAccent", "yellow").put("settings", JSONObject().put("themeKey", "classicTeal"))
        assertEquals("classicTeal", TrainPilotSettingsStore.importedTheme(data)!!.key)
        assertNull(TrainPilotSettingsStore.importedTheme(JSONObject().put("themeAccent", "#abcdef")))
        assertNull(TrainPilotSettingsStore.importedTheme(JSONObject()))
        assertNull(TrainPilotSettingsStore.importedTheme(JSONObject().put("themeKey", "unknown")))
    }
    @Test fun everySelectablePaletteHasFourNamesAndUniqueStableKey() {
        val themes = TrainPilotSettingsStore.AVAILABLE_THEMES
        assertEquals(themes.size, themes.map { it.key }.toSet().size)
        for (theme in themes) for (language in listOf("hu", "en", "de", "ro"))
            assertTrue(theme.displayName(language).isNotBlank())
    }
}
