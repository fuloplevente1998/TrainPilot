package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.KeyValueDocumentEntity
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
class NativeExerciseFavoritesTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase

    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
    }
    @After fun cleanup() {
        db.close()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun migrator() = LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context))

    @Test fun originalV144KeyIsRecognizedByLegacyReader() {
        assertTrue("exerciseFavorites" in LegacyWebViewMigrator.KNOWN_DB_KEYS)
        val decoded = LegacyWebViewMigrator.decodeSnapshot(mapOf("exerciseFavorites" to "[\"running\"]"))
        assertEquals("running", decoded.getJSONArray("exerciseFavorites").getString(0))
    }

    @Test fun parserKeepsSafeUnicodeAndFirstOccurrenceOrder() {
        val rows = JSONArray().put("db-squat").put("saját-gyakorlat").put("db-squat")
        assertEquals(listOf("db-squat", "saját-gyakorlat"), NativeExerciseFavorites.decode(rows))
    }

    @Test fun invalidIdsAndNonStringEntriesAreRejected() {
        for (bad in listOf("constructor", "__proto__", "bad\nname", "<script>", "bad\"quote", "bad\\slash")) {
            assertThrows(IllegalArgumentException::class.java) { NativeExerciseFavorites.decode(JSONArray().put(bad)) }
        }
        assertThrows(IllegalArgumentException::class.java) { NativeExerciseFavorites.decode(JSONArray().put(12)) }
        assertThrows(IllegalArgumentException::class.java) { NativeExerciseFavorites.decode(JSONObject()) }
    }

    @Test fun togglePersistsForBuiltinButUnknownIdCannotBeCreated() = runBlocking {
        db.exerciseLibraryDao().upsertExercise(TrainPilotDatabase.defaultExercises().first { it.id == "running" })
        assertEquals(listOf("running"), NativeExerciseFavorites.toggle(context, db, "running"))
        assertEquals(listOf("running"), NativeExerciseFavorites.load(db))
        assertThrows(IllegalArgumentException::class.java) { runBlocking { NativeExerciseFavorites.toggle(context, db, "missing") } }
        assertEquals(emptyList<String>(), NativeExerciseFavorites.toggle(context, db, "running"))
        assertEquals(emptyList<String>(), NativeExerciseFavorites.load(db))
    }

    @Test fun legacyAliasIsReadButCanonicalEditWins() = runBlocking {
        db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("legacy:exerciseFavorites",
            JSONObject().put("value", JSONArray().put("running")).toString(), 1))
        assertEquals(listOf("running"), NativeExerciseFavorites.load(db))
        db.exerciseLibraryDao().upsertExercise(TrainPilotDatabase.defaultExercises().first { it.id == "running" })
        NativeExerciseFavorites.toggle(context, db, "running")
        assertTrue(NativeExerciseFavorites.load(db).isEmpty())
    }

    @Test fun jsonExportAndRestorePreserveFavoritesWithoutChangingExerciseRows() = runBlocking {
        db.exerciseLibraryDao().upsertExercise(TrainPilotDatabase.defaultExercises().first { it.id == "running" })
        migrator().importBackupJson(JSONObject().put("exerciseFavorites", JSONArray().put("running")), false)
        assertEquals(listOf("running"), NativeExerciseFavorites.load(db))
        val backup = migrator().exportBackupJson(false)
        assertEquals("running", backup.getJSONArray("exerciseFavorites").getString(0))
        assertNotNull(db.exerciseLibraryDao().getExerciseById("running"))
        migrator().importBackupJson(JSONObject().put("exerciseFavorites", JSONArray()), true)
        assertTrue(NativeExerciseFavorites.load(db).isEmpty())
        migrator().importBackupJson(backup, true)
        assertEquals(listOf("running"), NativeExerciseFavorites.load(db))
    }

    @Test fun malformedImportDoesNotOverwriteExistingFavorite() = runBlocking {
        db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("exerciseFavorites",
            JSONArray().put("running").toString(), 1))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { migrator().importBackupJson(JSONObject().put("exerciseFavorites", JSONArray().put("bad<id>")), true) }
        }
        assertEquals(listOf("running"), NativeExerciseFavorites.load(db))
    }

    @Test fun restoreWithoutFavoritesDoesNotResurrectStaleAlias() = runBlocking {
        db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("legacy:exerciseFavorites",
            JSONObject().put("value", JSONArray().put("running")).toString(), 1))
        migrator().importBackupJson(JSONObject(), true)
        assertEquals(emptyList<String>(), NativeExerciseFavorites.load(db))
    }

    @Test fun driveMergeTreatsFavoritesAsAConflictAwarePortableSetting() {
        val local = JSONObject().put("exerciseFavorites", JSONArray().put("running"))
        val remote = JSONObject().put("exerciseFavorites", JSONArray().put("db-squat"))
        val base = JSONObject().put("exerciseFavorites", JSONArray())
        val sync = GoogleSyncRepository(context)
        val keepLocal = sync.mergeSync18(local, listOf(remote), base, emptyList(), emptySet(), emptySet(), true)
        val chooseRemote = sync.mergeSync18(local, listOf(remote), base, emptyList(), emptySet(), emptySet(), false)
        assertEquals(listOf("running"), NativeExerciseFavorites.decode(keepLocal.getJSONArray("exerciseFavorites")))
        assertEquals(listOf("db-squat"), NativeExerciseFavorites.decode(chooseRemote.getJSONArray("exerciseFavorites")))
        val remoteMatchesBase = sync.mergeSync18(local, listOf(base), base, emptyList(), emptySet(), emptySet(), false)
        assertEquals(listOf("running"), NativeExerciseFavorites.decode(remoteMatchesBase.getJSONArray("exerciseFavorites")))
    }

    @Test fun blockedReplacementRejectsFavoriteEditWithoutWriting() = runBlocking {
        db.exerciseLibraryDao().upsertExercise(TrainPilotDatabase.defaultExercises().first { it.id == "running" })
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().putBoolean("blocked", true).commit()
        try {
            assertThrows(IllegalStateException::class.java) {
                runBlocking { NativeExerciseFavorites.toggle(context, db, "running") }
            }
            assertNull(db.keyValueDocumentDao().getDocument(NativeExerciseFavorites.KEY))
        } finally {
            context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
