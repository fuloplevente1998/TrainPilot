package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.migration.NativeBackupPrivacy
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
class NativeBackupImportTest {
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private lateinit var importer: LegacyWebViewMigrator
    @Before fun open() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        settings = TrainPilotSettingsStore(context)
        importer = LegacyWebViewMigrator(context, db, settings)
        runBlocking { settings.resetAllSettingsForEraseLocal() }
    }
    @After fun close() { db.close() }
    @Test fun sqlFailureRollsBackOldHistoryAndAuxiliaryDataBeforeChangingSettings() = runBlocking {
        db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(0))
        db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("draft", "{\"old\":true}", 1))
        withContext(Dispatchers.IO) {
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_import BEFORE INSERT ON workout_history BEGIN SELECT RAISE(ABORT, 'injected'); END")
        }
        val data = JSONObject().put("history", JSONArray().put(JSONObject().put("id", "new").put("started", "2026-10-24T10:00:00Z")))
            .put("settings", JSONObject().put("language", "ro"))
        var failed = false
        try { importer.importBackupJson(data, true) } catch (_: Exception) { failed = true }
        assertTrue(failed)
        assertNotNull(db.workoutHistoryDao().getWorkoutWithDetails("w-0000"))
        assertEquals("{\"old\":true}", db.keyValueDocumentDao().getDocument("draft")!!.json)
        assertEquals("hu", settings.current().language)
    }
    @Test fun portableSettingsRoundTripIncludesGpsAndTimerButPreservesLocalDeviceIdentity() = runBlocking {
        val identity = settings.ensureCloudDeviceId()
        val data = JSONObject().put("history", JSONArray()).put("cloudDevice", "foreign-device")
            .put("settings", JSONObject().put("language", "de").put("themeKey", "classicRose")
                .put("restSeconds", 45).put("workSeconds", 17).put("rounds", 4).put("sound", false)
                .put("vibration", false).put("weightRange", "60").put("autoWriteWorkoutToHealthConnect", true)
                .put("gpsStartChoices216", JSONObject().put("running", true).put("plank", true)))
        importer.importBackupJson(data, true)
        val first = settings.current()
        assertEquals(identity, first.cloudDeviceId)
        assertEquals(mapOf("running" to true), first.gpsStartChoices216)
        val backup = importer.exportBackupJson(false)
        settings.setLanguage("hu"); settings.setTimerDefaults(30, 90, 1, true, true)
        importer.importBackupJson(backup, true)
        val roundTrip = settings.current()
        assertEquals("de", roundTrip.language); assertEquals("classicRose", roundTrip.themeKey)
        assertEquals(45, roundTrip.restSeconds); assertEquals(17, roundTrip.workSeconds); assertEquals(4, roundTrip.rounds)
        assertFalse(roundTrip.soundEnabled); assertFalse(roundTrip.vibrationEnabled)
        assertTrue(roundTrip.autoWriteWorkoutToHealthConnect); assertEquals("60", roundTrip.weightRange)
        assertEquals(first.gpsStartChoices216, roundTrip.gpsStartChoices216); assertEquals(identity, roundTrip.cloudDeviceId)
    }
    @Test fun explicitPortableDocumentsRoundTripAndAbsentDraftDoesNotResurrectAfterReplace() = runBlocking {
        val data = JSONObject().put("history", JSONArray()).put("draft", JSONObject().put("savedAt", "2026-10-24"))
            .put("plannerSettings", JSONObject().put("unknownFutureOption", 7))
        importer.importBackupJson(data, true)
        val exported = importer.exportBackupJson(false)
        assertEquals("2026-10-24", exported.getJSONObject("draft").getString("savedAt"))
        assertEquals(7, exported.getJSONObject("plannerSettings").getInt("unknownFutureOption"))
        importer.importBackupJson(JSONObject().put("history", JSONArray()), true)
        assertNull(db.keyValueDocumentDao().getDocument("draft"))
        assertNull(db.keyValueDocumentDao().getDocument("plannerSettings"))
    }
    @Test fun backupAndRestoreDoNotDropDeletionMarkersOlderThanTheRecentWindow() = runBlocking {
        val tombstones = (0..2100).map { i -> com.repforge.app.data.db.HistoryTombstoneEntity(
            "deleted-$i", "key-$i", JSONArray().put("deleted-$i").toString(), i.toLong()) }
        db.workoutHistoryDao().upsertTombstones(tombstones)
        val backup = importer.exportBackupJson(false)
        assertEquals(2101, backup.getJSONArray("phase3HistoryTombstones").length())
        backup.getJSONArray("history").put(JSONObject().put("id", "deleted-0").put("started", "2026-10-24T10:00:00Z"))
        importer.importBackupJson(backup, true)
        assertNull(db.workoutHistoryDao().getWorkoutWithDetails("deleted-0"))
        assertEquals(2101, db.workoutHistoryDao().getAllTombstones().size)
    }
    @Test fun backupPrivacyKeepsMeasurementAndRouteKeysWithoutCoordinateSamplesOrSourceMutation() {
        val source = JSONObject("""{"distanceMeters":123,"gpsRouteKey193":"private-reference","unknown":9,
            "draft":{"points":[{"latitude":47.0,"longitude":19.0}],"coordinates":[[19,47]]}}""")
        val original = source.toString()
        val exported = NativeBackupPrivacy.sanitize(source)
        assertEquals(original, source.toString())
        assertEquals(123, exported.getInt("distanceMeters")); assertEquals("private-reference", exported.getString("gpsRouteKey193"))
        assertEquals(9, exported.getInt("unknown")); assertFalse(exported.getJSONObject("draft").has("coordinates"))
        assertEquals(0, exported.getJSONObject("draft").getJSONArray("points").getJSONObject(0).length())
    }
    @Test fun legacyGlobalPresetOverridesFallbackFieldsAndRoundTripsIntoNewSessionConfiguration() = runBlocking {
        val preset = JSONObject().put("work", 12).put("rest", 8).put("rounds", 3)
            .put("sound", false).put("vibration", true).put("autoAdvance", false)
        settings.importPortableSettings(JSONObject().put("settings", JSONObject().put("workSeconds", 99)
            .put("restSeconds", 99).put("rounds", 99).put("intervalPreset110", preset)))
        val original = settings.current()
        assertEquals(12, original.workSeconds); assertEquals(8, original.restSeconds)
        assertFalse(original.intervalAutoAdvance)
        val exported = importer.exportBackupJson(false)
        assertFalse(exported.getJSONObject("settings").getJSONObject("intervalPreset110").getBoolean("autoAdvance"))
        settings.setTimerDefaults(5, 5, 1, true, false, true)
        importer.importBackupJson(exported, true)
        val restored = settings.current()
        assertEquals(original.timerConfiguration().toString(), restored.timerConfiguration().toString())
        val candidate = NativeWorkoutSession.quickCandidate(JSONObject().put("id", "plank"), restored)
        assertEquals(12, candidate.getJSONObject("timerConfig").getInt("work"))
        assertFalse(candidate.getJSONObject("timerConfig").getBoolean("autoAdvance"))
    }
    @Test fun malformedPresetCannotPartiallyOverwriteExistingPortableSettings() = runBlocking {
        settings.setTimerDefaults(3, 4, 2, false, true, false)
        val before = settings.current()
        val invalid = JSONObject().put("work", 3600).put("rest", 3600).put("rounds", 100)
        var rejected = false
        try { settings.importPortableSettings(JSONObject().put("settings", JSONObject()
            .put("language", "ro").put("intervalPreset110", invalid))) }
        catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        assertEquals(before, settings.current())
    }

    @Test fun portableProgressionStepsKeepValidOverridesAcrossExportAndImport() = runBlocking {
        val steps = JSONObject().put("per_hand", .5).put("total", 2.5).put("isolation", "0.5").put("invalid", -1).put("oversized", 99)
        settings.importPortableSettings(JSONObject().put("settings", JSONObject().put("progressionSteps", steps)))
        assertEquals(mapOf("per_hand" to .5, "total" to 2.5, "isolation" to .5), settings.current().progressionSteps)
        val exported = importer.exportBackupJson(false)
        settings.resetAllSettingsForEraseLocal()
        assertTrue(settings.current().progressionSteps.isEmpty())
        importer.importBackupJson(exported, true)
        assertEquals(mapOf("per_hand" to .5, "total" to 2.5, "isolation" to .5), settings.current().progressionSteps)
    }

}
