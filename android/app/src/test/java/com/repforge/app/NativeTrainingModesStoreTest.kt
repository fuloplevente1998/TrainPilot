package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.NativeTrainingModes
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
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
class NativeTrainingModesStoreTest {
    private lateinit var context: Context
    private lateinit var database: TrainPilotDatabase
    private lateinit var store: TrainPilotSettingsStore

    @Before fun setup() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        store = TrainPilotSettingsStore(context)
        store.resetAllSettingsForEraseLocal()
    }

    @After fun teardown() {
        database.close()
    }

    private fun chosen() = JSONObject()
        .put("schemaVersion", 1)
        .put("defaults", JSONObject().put("trainingStyle", "hit").put("progressionMode", "rir"))
        .put("explicitChoice", true)
        .put("revision", 5)
        .put("byProgram", JSONObject().put("p", JSONObject()
            .put("explicitChoice", true).put("trainingStyle", "normal").put("progressionMode", "linear")
            .put("updatedAt", "2026-10-20T10:00:00Z")
            .put("futureProgramExtension", JSONObject().put("keep", "program"))))
        .put("undo", JSONObject().put("previous", JSONObject().put("opaque", "undo")))
        .put("futureExtension", JSONObject().put("keep", "global"))

    /** Equivalent nested objects remain opaque when storing, exporting and importing. */
    @Test fun fullPortableRoundTripPreservesModesRevisionUndoAndUnknownFields() = runBlocking {
        val initial = chosen()
        store.importPortableSettings(JSONObject().put("settings", JSONObject().put("trainingModes197", initial)))
        assertEquals("program", NativeTrainingModes.resolveSavedJson(store.current().trainingModes197Json, "p").source)
        assertEquals("global", NativeTrainingModes.resolveSavedJson(store.current().trainingModes197Json, "other").source)

        val migrator = LegacyWebViewMigrator(context, database, store)
        val portable = migrator.exportBackupJson(includeHealthJournal = false)
        val onWire = portable.getJSONObject("settings").getJSONObject("trainingModes197")
        assertEquals(5, onWire.getInt("revision"))
        assertEquals("undo", onWire.getJSONObject("undo").getJSONObject("previous").getString("opaque"))
        assertEquals("global", onWire.getJSONObject("futureExtension").getString("keep"))
        assertEquals("program", onWire.getJSONObject("byProgram").getJSONObject("p")
            .getJSONObject("futureProgramExtension").getString("keep"))

        store.resetAllSettingsForEraseLocal()
        assertNull(store.current().trainingModes197Json)
        migrator.importBackupJson(portable, replaceAll = true)
        val restored = JSONObject(store.current().trainingModes197Json!!)
        assertEquals(com.repforge.shared.LegacySyncIdentity18.canonical(onWire),
            com.repforge.shared.LegacySyncIdentity18.canonical(restored))
        assertEquals("program", NativeTrainingModes.resolve(restored, "p").source)
    }

    @Test fun fullLegacyReplacementClearsEarlierExplicitMode() = runBlocking {
        assertTrue(store.saveApprovedTrainingModes197(null, chosen()))
        val migrator = LegacyWebViewMigrator(context, database, store)
        val legacy = migrator.exportBackupJson(false)
        legacy.getJSONObject("settings").remove("trainingModes197")
        migrator.importBackupJson(legacy, replaceAll = true)
        assertNull(store.current().trainingModes197Json)
        assertEquals("legacy", NativeTrainingModes.resolveSavedJson(store.current().trainingModes197Json, "p").source)
    }

    @Test fun incrementalPortableImportWithoutModesDoesNotEraseLocalSelection() = runBlocking {
        val value = chosen()
        assertTrue(store.saveApprovedTrainingModes197(null, value))
        val snapshot = store.current().trainingModes197Json
        store.importPortableSettings(JSONObject().put("settings", JSONObject().put("language", "de")))
        assertEquals(snapshot, store.current().trainingModes197Json)
        assertEquals("de", store.current().language)
    }

    @Test fun staleModeCasIsRejectedAndOtherSettingsAreNotChanged() = runBlocking {
        store.setLanguage("ro")
        val accepted = chosen()
        assertTrue(store.saveApprovedTrainingModes197(null, accepted))
        assertFalse(store.saveApprovedTrainingModes197(null, chosen().put("revision", 6)))
        val old = store.current().trainingModes197Json
        assertFalse(store.saveApprovedTrainingModes197("obsolete", null))
        assertEquals(old, store.current().trainingModes197Json)
        assertTrue(store.saveApprovedTrainingModes197(old, chosen().put("revision", 6)))
        assertEquals(6, JSONObject(store.current().trainingModes197Json!!).getInt("revision"))
        assertEquals("ro", store.current().language)
        assertTrue(store.saveApprovedTrainingModes197(store.current().trainingModes197Json, null))
        assertNull(store.current().trainingModes197Json)
        assertEquals("ro", store.current().language)
    }

    @Test fun malformedImportedModeIsPreservedButCannotActivateOrBeApproved() = runBlocking {
        val invalid = chosen().put("schemaVersion", 99)
        store.importPortableSettings(JSONObject().put("settings", JSONObject().put("trainingModes197", invalid)))
        assertEquals("legacy", NativeTrainingModes.resolveSavedJson(store.current().trainingModes197Json, "p").source)
        val portable = LegacyWebViewMigrator(context, database, store).exportBackupJson(false)
        assertEquals(99, portable.getJSONObject("settings").getJSONObject("trainingModes197").getInt("schemaVersion"))
        assertFalse(NativeTrainingModes.validForWrite(invalid))
        assertFalse(NativeTrainingModes.validForWrite(chosen().put("byProgram", "invalid")))
        var rejected = false
        try { store.saveApprovedTrainingModes197(store.current().trainingModes197Json, invalid) }
        catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        assertEquals(99, JSONObject(store.current().trainingModes197Json!!).getInt("schemaVersion"))
    }

}
