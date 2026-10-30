package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeTrainingProfileTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var store: TrainPilotSettingsStore

    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        store = TrainPilotSettingsStore(context)
    }
    @After fun cleanup() { db.close() }

    private fun original(): JSONObject = JSONObject()
        .put("age", 30).put("height", 175).put("weight", 70)
        .put("minutes", 60).put("goal", "fitness").put("experience", "beginner")
        .put("activity", "mixed").put("location", "home")
        .put("gear", JSONArray().put("dumbbell").put("mat"))
        .put("excluded", JSONArray().put("barbell-row"))
        .put("avoidAreas", JSONArray().put("left-shoulder"))
        .put("future128Metadata", JSONObject().put("approvedPreview", "preserve"))

    @Test fun validatesMainProfileWithoutDiscardingUnknownFields() {
        val checked = NativeTrainingProfile.verified(original())
        assertEquals(30, checked.getInt("age"))
        assertEquals("home", checked.getString("location"))
        assertEquals("mat", checked.getJSONArray("gear").getString(1))
        assertEquals("preserve", checked.getJSONObject("future128Metadata").getString("approvedPreview"))
    }

    @Test fun editingCoreFieldsPreservesGearExclusionsAndFutureExtensions() {
        val edited = NativeTrainingProfile.edit(original(),
            mapOf("minutes" to "90", "weight" to "65,5", "goal" to "muscle"))
        assertEquals(90, edited.getInt("minutes"))
        assertEquals(65.5, edited.getDouble("weight"), 1e-6)
        assertEquals("muscle", edited.getString("goal"))
        assertEquals("barbell-row", edited.getJSONArray("excluded").getString(0))
        assertEquals("left-shoulder", edited.getJSONArray("avoidAreas").getString(0))
        assertTrue(edited.has("future128Metadata"))
    }

    @Test fun editableGearAndExcludedItemsRoundTripWithoutChangingOtherProfileMetadata() = runBlocking {
        val edited = NativeTrainingProfile.edit(original(), mapOf(
            "gear" to "dumbbells, barbell, dumbbells",
            "excluded" to "barbell-row, db-ohp", "avoidAreas" to "shoulders"))
        assertEquals(listOf("dumbbells", "barbell"), edited.getJSONArray("gear").let {
            rows -> (0 until rows.length()).map(rows::getString)
        })
        assertEquals(2, edited.getJSONArray("excluded").length())
        assertEquals("preserve", edited.getJSONObject("future128Metadata").getString("approvedPreview"))
        store.saveTrainingProfile(edited)
        val saved = JSONObject(store.current().profileJson)
        assertEquals("shoulders", saved.getJSONArray("avoidAreas").getString(0))
    }

    @Test fun blankInputDoesNotInventPersonalMeasurements() {
        val result = NativeTrainingProfile.edit(original(), mapOf("weight" to "", "height" to ""))
        assertFalse(result.has("weight"))
        assertFalse(result.has("height"))
        assertTrue(result.has("gear"))
    }

    @Test fun rejectsInvalidMeasurementsAndDisallowedProfileTypes() {
        for (bad in listOf("NaN", "Infinity", "-3", "99999", "invalid")) {
            assertThrows(IllegalArgumentException::class.java) {
                NativeTrainingProfile.edit(original(), mapOf("weight" to bad))
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            NativeTrainingProfile.verified(original().put("age", "not-a-number"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            NativeTrainingProfile.verified(original().put("gear", JSONObject().put("bad", true)))
        }
    }

    @Test fun roundTripsCompleteProfileViaExistingSettingsAndPortableBackup() = runBlocking {
        store.saveTrainingProfile(original())
        val saved = JSONObject(store.current().profileJson)
        assertEquals("fitness", saved.getString("goal"))
        val snapshot = LegacyWebViewMigrator(context, db, store).exportBackupJson(false)
        val portable = snapshot.getJSONObject("settings").getJSONObject("profile")
        assertEquals("mat", portable.getJSONArray("gear").getString(1))
        assertEquals("preserve", portable.getJSONObject("future128Metadata").getString("approvedPreview"))
        store.saveTrainingProfile(JSONObject())
        assertFalse(JSONObject(store.current().profileJson).has("gear"))
        store.importPortableSettings(snapshot)
        val reloaded = JSONObject(store.current().profileJson)
        assertEquals(60, reloaded.getInt("minutes"))
        assertEquals("barbell-row", reloaded.getJSONArray("excluded").getString(0))
    }

    @Test fun invalidPortableProfileCannotPartiallyOverwriteExistingSettings() = runBlocking {
        store.saveTrainingProfile(original())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { store.importPortableSettings(JSONObject().put("settings",
                JSONObject().put("profile", JSONArray().put("invalid")))) }
        }
        assertEquals(30, JSONObject(store.current().profileJson).getInt("age"))
    }

    @Test fun malformedProfileBackupFailsBeforeAnyRoomProgramReplacement() = runBlocking {
        val originalProgram = TrainPilotDatabase.defaultPrograms().first.first()
        db.programDao().upsertProgram(originalProgram)
        val malformed = JSONObject().put("settings", JSONObject().put("profile",
            JSONObject().put("age", -1)))
            .put("programs", JSONArray())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { LegacyWebViewMigrator(context, db, store).importBackupJson(malformed, true) }
        }
        assertNotNull(db.programDao().getProgramWithDaysById(originalProgram.id))
    }

    @Test fun restoreFenceRejectsProfileWrites(): Unit = runBlocking {
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().putBoolean("blocked", true).commit()
        try {
            assertThrows(IllegalStateException::class.java) { runBlocking {
                store.saveTrainingProfile(original())
            } }
        } finally {
            context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
                .edit().clear().commit()
        }
    }
}
