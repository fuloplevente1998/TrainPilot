package com.repforge.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeIntervalPresetTest {
    @Test fun rejectsOverflowStringNumbersAndNonBooleanModeBeforeBuildingAnyPlan() {
        val base = JSONObject().put("work", 30).put("rest", 10).put("rounds", 2)
        for (invalid in listOf(JSONObject(base.toString()).put("work", "30"),
            JSONObject(base.toString()).put("autoAdvance", "false"),
            JSONObject(base.toString()).put("work", 3600).put("rest", 3600).put("rounds", 100))) {
            assertThrows(IllegalArgumentException::class.java) { NativeIntervalPreset.normalized(invalid) }
        }
        assertEquals(24, NativeIntervalPreset.normalized(JSONObject().put("work", 3600).put("rest", 0)
            .put("rounds", 24)).getInt("rounds"))
    }
    @Test fun preservesOldNativeAutomaticDefaultButLegacyPresetUsesOriginalExplicitOptIn() {
        val old = JSONObject().put("work", 30).put("rest", 10).put("rounds", 2)
        assertTrue(NativeIntervalPreset.normalized(old).getBoolean("autoAdvance"))
        assertFalse(NativeIntervalPreset.normalized(old, defaultAutoAdvance = false).getBoolean("autoAdvance"))
        assertFalse(old.has("autoAdvance"))
        val explicit = JSONObject(old.toString()).put("autoAdvance", true)
        assertTrue(NativeIntervalPreset.normalized(explicit, defaultAutoAdvance = false).getBoolean("autoAdvance"))
    }
}
