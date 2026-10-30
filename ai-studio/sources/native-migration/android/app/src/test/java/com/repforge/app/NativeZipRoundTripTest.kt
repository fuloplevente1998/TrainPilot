package com.repforge.app

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.repforge.app.data.db.*
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** Archive byte/hash and metadata tests; image rendering remains a separate emulator UI assertion. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeZipRoundTripTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    private val photoId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    private val photoBytes = ByteArray(128) { it.toByte() }
    @Before fun open() = runBlocking {
        Dispatchers.setMain(StandardTestDispatcher())
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        NativeReplacementJournal(context).clear()
        File(context.noBackupFilesDir, "archive_transactions").deleteRecursively()
        db = TrainPilotDatabase.getInstance(context)
        withContext(Dispatchers.IO) { db.clearAllTables() }
        settings = TrainPilotSettingsStore(context); settings.resetAllSettingsForEraseLocal(); settings.setLanguage("en")
        db.withTransaction {
            db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(0))
            db.workoutHistoryDao().insertPhotos(listOf(WorkoutPhotoEntity(photoId, "w-0000", "before", "2026-10-24T10:00:00Z",
                1, 1, 1, photoBytes.size.toLong(), null, null)))
            db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("draft", "{\"savedAt\":\"2026-10-24\",\"custom\":17}", 1))
        }
        WorkoutPhotoRepository(context).photoFile(photoId).writeBytes(photoBytes)
    }
    @After fun close() {
        NativeReplacementJournal(context).clear()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        Dispatchers.resetMain()
    }
    private suspend fun archive(): Uri {
        val uri = Uri.fromFile(File(context.cacheDir, "roundtrip.zip"))
        val result = BackupRestoreManager(context).exportZipArchiveToUri(uri)
        assertTrue(result.exceptionOrNull()?.message, result.isSuccess)
        return uri
    }
    private suspend fun removeLocalFixture() {
        db.workoutHistoryDao().deleteAllWorkouts(); db.keyValueDocumentDao().deleteDocument("draft")
        WorkoutPhotoRepository(context).photoFile(photoId).delete()
        settings.setLanguage("ro")
    }
    @Test fun actualZipRoundTripRestoresPhotoBytesHistoryDraftAndPortableSettings() = runTest {
        val uri = archive(); removeLocalFixture()
        val result = BackupRestoreManager(context).importZipArchiveFromUri(uri)
        assertTrue(result.exceptionOrNull()?.message, result.isSuccess)
        assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
        assertEquals(photoId, db.workoutHistoryDao().getWorkoutWithDetails("w-0000")!!.photos.single().id)
        assertArrayEquals(photoBytes, WorkoutPhotoRepository(context).photoFile(photoId).readBytes())
        assertEquals(17, JSONObject(db.keyValueDocumentDao().getDocument("draft")!!.json).getInt("custom"))
        assertEquals("en", settings.current().language)
        assertFalse(NativeReplacementJournal(context).exists()); assertFalse(NativeDataReplacement.blocked(context))
    }
    @Test fun interruptedZipReplayKeepsNewPhotosDistinctFromOriginalsAndFinishesIdempotently() = runTest {
        val uri = archive(); removeLocalFixture()
        val manager = BackupRestoreManager(context)
        manager.restoreCheckpoint = { if (it == "data-imported") throw java.io.IOException("injected interruption") }
        assertTrue(manager.importZipArchiveFromUri(uri).isFailure)
        val entry = NativeReplacementJournal(context).read()
        val original = File(context.noBackupFilesDir, "archive_transactions/${entry.token}/originals/$photoId.jpg")
        assertFalse(original.exists()) // There was no local photo before the chosen archive was installed.
        assertTrue(BackupRestoreManager(context).resumeInterruptedReplacement().isSuccess)
        assertArrayEquals(photoBytes, WorkoutPhotoRepository(context).photoFile(photoId).readBytes())
        assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
        assertFalse(NativeDataReplacement.blocked(context))
    }
}
