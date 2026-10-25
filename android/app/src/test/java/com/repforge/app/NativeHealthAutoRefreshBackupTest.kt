package com.repforge.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeHealthAutoRefreshBackupTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test fun nestedBackupScopesPreventAnyNewHealthPublicationUntilLastRelease() {
        var commits = 0
        NativeHealthBackupGate.begin()
        try {
            assertFalse(NativeHealthBackupGate.available())
            assertThrows(IllegalStateException::class.java) { NativeHealthBackupGate.publish { commits++ } }
            NativeHealthBackupGate.begin()
            try {
                assertFalse(NativeHealthBackupGate.available())
            } finally { NativeHealthBackupGate.end() }
            assertFalse(NativeHealthBackupGate.available())
            assertEquals(0, commits)
        } finally { NativeHealthBackupGate.end() }
        assertTrue(NativeHealthBackupGate.available())
        NativeHealthBackupGate.publish { commits++ }
        assertEquals(1, commits)
    }

    @Test fun initialPartialHealthDayKeepsActualFieldsAndMarksMissingOnesUnavailable() {
        val store = HealthJournalStore(context)
        try {
            val date = LocalDate.now().minusDays(2).toString()
            val result = JSONObject()
                .put("source", "@aggregate")
                .put("steps", 1525)
                .put("weightKg", JSONObject.NULL)
                .put("warnings", JSONArray().put("READ_WEIGHT unavailable"))
            store.healthDay(date, result, store.generation())
            val projected = store.projection().getJSONObject("days").getJSONObject(date)
            assertEquals(1525, projected.getInt("steps"))
            assertTrue(projected.optBoolean("partial"))
            assertTrue(projected.isNull("weightKg") || !projected.has("weightKg"))
        } finally {
            store.close()
        }
    }
}
