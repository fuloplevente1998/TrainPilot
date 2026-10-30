package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.*
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.progress.NativeWeightJournalRepository
import com.repforge.shared.LegacySyncIdentity18
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeWeightDeletionReplayTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private lateinit var importer: LegacyWebViewMigrator
    private lateinit var repository: NativeWeightJournalRepository
    @Before fun open() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        settings = TrainPilotSettingsStore(context); settings.resetAllSettingsForEraseLocal()
        importer = LegacyWebViewMigrator(context, db, settings); repository = NativeWeightJournalRepository(context, db) { 1000L }
    }
    @After fun close() { db.close() }
    private fun weight(id: String? = "one", kg: Double = 72.0, date: String = "2026-10-25") =
        JSONObject().put("date", date).put("kg", kg).put("updatedAt", 1L).also { if (id != null) it.put("id", id) }
    private fun data(vararg rows: JSONObject) = JSONObject().put("history", JSONArray()).put("weights", JSONArray(rows.toList()))
    private suspend fun seed(vararg rows: JSONObject) { importer.importBackupJson(data(*rows), false) }
    private suspend fun delete(id: String = "one") { repository.delete(db.bodyWeightDao().getWeightById(id)!!) }
    private suspend fun backup() = importer.exportBackupJson(false)
    private fun merge(local: JSONObject, remote: JSONObject, base: JSONObject? = null,
        archives: List<JSONObject> = emptyList(), preferLocal: Boolean = true) = GoogleSyncRepository(context)
        .mergeSync18(local, listOf(remote), base, archives, emptySet(), emptySet(), preferLocal)
    private suspend fun rejected(operation: suspend () -> Unit) {
        var failure: Exception? = null
        try { operation() } catch (error: Exception) { failure = error }
        assertNotNull("Expected deletion/import to be rejected", failure)
    }

    @Test fun exactDeletionAndMarkerAreAtomicAndOtherSameDayMeasurementsRemain() = runBlocking {
        seed(weight(), weight("other", 73.0), weight("repeat", 72.0)); delete()
        assertEquals(setOf("other", "repeat"), db.bodyWeightDao().getAllWeights().map { it.id }.toSet())
        val marker = NativeWeightTombstones18.load(db).single()
        assertEquals("one", marker.id); assertEquals(1000L, marker.deletedAt)
        assertEquals(setOf(LegacySyncIdentity18.legacyWeightId(weight(null))), marker.legacyIds)
    }
    @Test fun localMarkerSuppressesALateRemoteRowUnderEitherConflictPreference() = runBlocking {
        seed(weight()); val stale = backup(); delete(); val local = backup()
        for (prefer in listOf(true, false)) {
            val merged = merge(local, stale, preferLocal = prefer)
            assertEquals(0, merged.getJSONArray("weights").length())
            assertEquals(1, merged.getJSONArray(NativeWeightTombstones18.KEY).length())
            importer.importBackupJson(merged, true); assertTrue(db.bodyWeightDao().getAllWeights().isEmpty())
        }
    }
    @Test fun remoteMarkerSuppressesAnOfflineLocalCopyWithoutNeedingABaseSnapshot() = runBlocking {
        seed(weight()); val offline = backup(); delete(); val deleted = backup()
        assertEquals(0, merge(offline, deleted).getJSONArray("weights").length())
    }
    @Test fun legacyIdlessAndSyntheticAliasesCannotResurrectFromCurrentOrArchiveSnapshots() = runBlocking {
        seed(weight()); delete(); val local = backup()
        val legacy = weight(null)
        val synthetic = weight(LegacySyncIdentity18.legacyWeightId(legacy))
        for (row in listOf(legacy, synthetic)) {
            assertEquals(0, merge(local, data(row), archives = listOf(data(legacy))).getJSONArray("weights").length())
        }
    }
    @Test fun newerEditedContentWithTheDeletedIdentityStillCannotResurrect() = runBlocking {
        seed(weight()); delete()
        val edited = weight(kg = 80.0).put("updatedAt", Long.MAX_VALUE)
        assertEquals(0, merge(backup(), data(edited)).getJSONArray("weights").length())
    }
    @Test fun genuinelyNewUuidCanRepeatTheDeletedMeasurementValueAndDate() = runBlocking {
        seed(weight()); delete()
        val newId = "11111111-2222-3333-4444-555555555555"
        val merged = merge(backup(), data(weight(newId)))
        assertEquals(newId, merged.getJSONArray("weights").getJSONObject(0).getString("id"))
        importer.importBackupJson(merged, true)
        assertEquals(newId, db.bodyWeightDao().getAllWeights().single().id)
    }
    @Test fun anotherTimestampOnTheSameCalendarDayIsNotALegacyAlias() = runBlocking {
        seed(weight()); delete()
        val other = weight(null, date = "2026-10-25T18:00:00Z")
        assertEquals(1, merge(backup(), data(other)).getJSONArray("weights").length())
    }
    @Test fun markerUnionRetainsAllLegacyAliasesAndTheLatestDeletionTime() {
        val a = LegacySyncIdentity18.legacyWeightId(weight(null, 72.0))
        val b = LegacySyncIdentity18.legacyWeightId(weight(null, 73.0))
        val merged = NativeWeightTombstones18.union(listOf(NativeWeightTombstones18.Marker("one", setOf(a), 20)),
            listOf(NativeWeightTombstones18.Marker("one", setOf(b), 10)))
        assertEquals(20L, merged.single().deletedAt); assertEquals(setOf(a, b), merged.single().legacyIds)
        val index = NativeWeightTombstones18.Index(merged)
        assertTrue(index.deleted(weight(null, 72.0))); assertTrue(index.deleted(weight(null, 73.0)))
    }
    @Test fun malformedMarkersAreRejectedBeforeReplacingWeightsOrSettings() = runBlocking {
        seed(weight())
        val bad = listOf<Any>(JSONObject(), JSONArray().put(JSONObject().put("id", "one").put("deletedAt", "yesterday")),
            JSONArray().put(JSONObject().put("id", "one").put("deletedAt", 20).put("legacyIds", JSONArray().put("../outside"))),
            JSONArray().put(JSONObject().put("version", 2).put("id", "one").put("deletedAt", 20)))
        for (invalid in bad) {
            rejected { importer.importBackupJson(data().put(NativeWeightTombstones18.KEY, invalid)
                .put("settings", JSONObject().put("language", "en")), true) }
            assertNotNull(db.bodyWeightDao().getWeightById("one")); assertEquals("hu", settings.current().language)
            assertTrue(NativeWeightTombstones18.load(db).isEmpty())
        }
    }
    @Test fun deleteFailureRollsBackItsMarkerAndKeepsTheOriginalMeasurement() = runBlocking {
        seed(weight())
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER weight_delete_full BEFORE DELETE ON body_weights BEGIN SELECT RAISE(ABORT,'disk full'); END")
        rejected { delete() }
        assertNotNull(db.bodyWeightDao().getWeightById("one")); assertTrue(NativeWeightTombstones18.load(db).isEmpty())
    }
    @Test fun markerWriteFailureCannotDeleteTheMeasurement() = runBlocking {
        seed(weight())
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER marker_full BEFORE INSERT ON kv_documents WHEN NEW.doc_key GLOB 'native:weightTombstone18:*' BEGIN SELECT RAISE(ABORT,'disk full'); END")
        rejected { delete() }
        assertNotNull(db.bodyWeightDao().getWeightById("one")); assertTrue(NativeWeightTombstones18.load(db).isEmpty())
    }
    @Test fun staleDeleteCannotWriteADeletionMarkerForTheChangedMeasurement() = runBlocking {
        seed(weight()); val old = db.bodyWeightDao().getWeightById("one")!!
        repository.edit(old, 73.0); rejected { repository.delete(old) }
        assertEquals(73.0, db.bodyWeightDao().getWeightById("one")!!.kg, .0001)
        assertTrue(NativeWeightTombstones18.load(db).isEmpty())
    }
    @Test fun incomingMarkerPurgesOnlyItsMatchingExistingRowAcrossBoundedPages() = runBlocking {
        db.bodyWeightDao().upsertWeights((0 until 1500).map { i ->
            val id = "w-%04d".format(i); BodyWeightEntryEntity(id, "2026-10-25", "2026-10-25", 72.0, 1,
                weight(id).toString())
        })
        val markers = NativeWeightTombstones18.encode(listOf(NativeWeightTombstones18.Marker("w-0800", emptySet(), 20)))
        importer.importBackupJson(JSONObject().put(NativeWeightTombstones18.KEY, markers), false)
        assertEquals(1499L, db.bodyWeightDao().getWeightJournalSummary().measurementCount)
        assertNull(db.bodyWeightDao().getWeightById("w-0800")); assertNotNull(db.bodyWeightDao().getWeightById("w-0801"))
    }
    @Test fun portableBackupRoundTripKeepsMarkersAndRejectsALaterOrdinaryImport() = runBlocking {
        seed(weight()); val old = backup(); delete(); val saved = backup()
        db.bodyWeightDao().deleteAllWeights(); db.keyValueDocumentDao().deleteAllDocuments()
        importer.importBackupJson(saved, true); importer.importBackupJson(old, false)
        assertTrue(db.bodyWeightDao().getAllWeights().isEmpty()); assertEquals(1, NativeWeightTombstones18.load(db).size)
    }
    @Test fun anExplicitOldBackupReplacementMayIntentionallyRestoreADeletedMeasurement() = runBlocking {
        seed(weight()); val old = backup().apply { remove(NativeWeightTombstones18.KEY) }; delete()
        db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("unrelated-owner-metadata", "{}", 1))
        importer.importBackupJson(old, true)
        assertNotNull(db.bodyWeightDao().getWeightById("one")); assertTrue(NativeWeightTombstones18.load(db).isEmpty())
        assertNotNull(db.keyValueDocumentDao().getDocument("unrelated-owner-metadata"))
    }
    @Test fun missingMarkerFieldInAnOrdinaryLegacyImportDoesNotClearCurrentDeletionIntent() = runBlocking {
        seed(weight()); delete()
        importer.importBackupJson(data(weight()).apply { remove(NativeWeightTombstones18.KEY) }, false)
        assertTrue(db.bodyWeightDao().getAllWeights().isEmpty()); assertEquals(1, NativeWeightTombstones18.load(db).size)
    }
    @Test fun aBaseSnapshotMarkerAlsoProtectsAgainstOldCurrentAndArchiveRows() = runBlocking {
        seed(weight()); delete(); val base = backup()
        assertEquals(0, merge(data(weight()), data(weight()), base, listOf(data(weight(null)))).getJSONArray("weights").length())
    }
    @Test fun sameCountDeleteAndReaddStillInvalidatesTheCloudMetadataSnapshot(): Unit = runBlocking {
        seed(weight()); val before = backup(); delete(); repository.add(73.0, "2026-10-25")
        assertEquals(1L, db.bodyWeightDao().getWeightJournalSummary().measurementCount)
        val after = backup()
        assertThrows(IllegalStateException::class.java) { NativeCloudMergeGuard.requireUnchanged(before, after) }
    }
}
