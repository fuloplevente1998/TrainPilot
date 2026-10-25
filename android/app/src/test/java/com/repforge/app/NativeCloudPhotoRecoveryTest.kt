package com.repforge.app

import android.content.Context
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.security.MessageDigest

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeCloudPhotoRecoveryTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private val dispatcher = StandardTestDispatcher()
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    private val active = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    private val deleted = "bbbbbbbb-bbbb-cccc-dddd-eeeeeeeeeeee"
    private val incoming = byteArrayOf(9, 8, 7, 6)

    @Before fun open() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        ActiveWorkoutStore.clear(context)
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        NativeReplacementJournal(context).clear()
        File(context.noBackupFilesDir, "archive_transactions").deleteRecursively()
        File(context.noBackupFilesDir, "cloud_photo_staging").deleteRecursively()
        File(context.filesDir, "workout_photos").deleteRecursively()
        db = TrainPilotDatabase.getInstance(context); settings = TrainPilotSettingsStore(context)
        runBlocking(Dispatchers.IO) { db.clearAllTables(); settings.resetAllSettingsForEraseLocal() }
    }
    @After fun close() {
        NativeReplacementJournal(context).clear()
        File(context.noBackupFilesDir, "archive_transactions").deleteRecursively()
        File(context.noBackupFilesDir, "cloud_photo_staging").deleteRecursively()
        File(context.filesDir, "workout_photos").deleteRecursively()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        Dispatchers.resetMain()
    }
    private fun photo(id: String) = File(context.filesDir, "workout_photos/$id.jpg").also { it.parentFile!!.mkdirs() }
    private fun payload() = JSONObject().put("history", JSONArray().put(JSONObject().put("id", "cloud-workout")
        .put("started", "2026-10-24T10:00:00Z").put("finished", "2026-10-24T11:00:00Z")
        .put("exercises", JSONArray()).put("photos", JSONArray().put(JSONObject().put("id", active)
            .put("phase", "before").put("driveFileId", "drive-active").put("bytes", incoming.size))
            .put(JSONObject().put("id", deleted).put("phase", "after").put("driveFileId", "drive-deleted").put("deletedAt", 123)))))
        .put("programs", JSONArray()).put("exercises", JSONArray()).put("weights", JSONArray()).put("scheduled", JSONArray())
        .put("settings", JSONObject().put("language", "en"))
    private fun prepared(data: JSONObject): NativeCloudPhotoStage = NativeCloudPhotoStage(context, data).apply {
        prepare({ _, _ -> "verified-local" }, { id, _, file ->
            val md5 = MessageDigest.getInstance("MD5").digest(incoming).joinToString("") { "%02x".format(it.toInt() and 255) }
            NativeCloudPhotoStage.installDownload(id, JSONObject().put("name", "trainpilot-photo-$id.jpg")
                .put("mimeType", "image/jpeg").put("size", incoming.size).put("md5Checksum", md5), incoming, file)
        })
    }
    private suspend fun install(manager: BackupRestoreManager, data: JSONObject, stage: NativeCloudPhotoStage) =
        NativeDataReplacement.exclusively(context) { manager.installCloudMerge(data, stage) }
    private fun receiptDir(): File = File(context.noBackupFilesDir, "archive_transactions/${NativeReplacementJournal(context).read().token}")
    private suspend fun reject(block: suspend () -> Unit) {
        var failure: Exception? = null
        try { block() } catch (error: Exception) { failure = error }
        assertNotNull("Expected staged cloud import to fail", failure)
    }

    @Test fun acceptedCloudPhotosAndDeletedFilesUseTheSameDurableLocalImport() = runTest(dispatcher) {
        photo(deleted).writeText("original")
        val data = payload()
        prepared(data).use { stage ->
            assertFalse(photo(active).exists()); assertEquals("original", photo(deleted).readText())
            install(BackupRestoreManager(context), data, stage)
            assertArrayEquals(incoming, photo(active).readBytes()); assertFalse(photo(deleted).exists())
            assertEquals("en", settings.current().language)
            val saved = db.workoutHistoryDao().getWorkoutWithDetails("cloud-workout")!!
            assertEquals(2, saved.photos.size)
            assertEquals("drive-deleted", saved.photos.first { it.id == deleted }.driveFileId)
            assertNotNull(saved.photos.first { it.id == deleted }.deletedAt)
            var removed = 0
            stage.finishRemoteDeletes { id, driveId -> assertEquals(deleted, id); assertEquals("drive-deleted", driveId); removed++ }
            assertEquals(1, removed)
        }
        assertFalse(NativeReplacementJournal(context).exists()); assertFalse(NativeDataReplacement.blocked(context))
    }
    @Test fun changedCanonicalMetadataRejectsWithoutTouchingLivePhotos() = runTest(dispatcher) {
        photo(deleted).writeText("original")
        val migrator = LegacyWebViewMigrator(context, db, settings)
        val original = migrator.exportBackupJson(false); val data = payload()
        prepared(data).use { stage ->
            settings.setLanguage("ro")
            reject { NativeDataReplacement.exclusively(context) {
                NativeCloudMergeGuard.requireUnchanged(original, migrator.exportBackupJson(false))
                BackupRestoreManager(context).installCloudMerge(data, stage)
            } }
            assertEquals("original", photo(deleted).readText()); assertFalse(photo(active).exists())
            assertThrows(IllegalStateException::class.java) { stage.finishRemoteDeletes { _, _ -> fail() } }
        }
        assertEquals("ro", settings.current().language)
        assertFalse(NativeReplacementJournal(context).exists()); assertFalse(NativeDataReplacement.blocked(context))
    }
    @Test fun sameSizeLocalPhotoEditRejectsEvenWhenCanonicalMetadataDidNotChange() = runTest(dispatcher) {
        photo(deleted).writeText("original"); val data = payload()
        prepared(data).use { stage ->
            photo(deleted).writeText("modified")
            reject { install(BackupRestoreManager(context), data, stage) }
            assertEquals("modified", photo(deleted).readText()); assertFalse(photo(active).exists())
        }
        assertFalse(NativeReplacementJournal(context).exists()); assertFalse(NativeDataReplacement.blocked(context))
    }
    @Test fun preparedInstalledImportedAndCommittedFaultsKeepAnOfflineReplayableArchive() = runTest(dispatcher) {
        for (phase in listOf("receipt-prepared", "photos-installed", "data-imported", "receipt-committed")) {
            withContext(Dispatchers.IO) { db.clearAllTables() }; settings.setLanguage("hu")
            photo(active).delete(); photo(deleted).writeText("original")
            val data = payload()
            prepared(data).use { stage ->
                val manager = BackupRestoreManager(context).apply {
                    restoreCheckpoint = { if (it == phase) throw IOException("injected $phase") }
                }
                reject { install(manager, data, stage) }
            } // Network stage is gone; replay must use archive_transactions only.
            assertTrue(NativeReplacementJournal(context).exists()); assertTrue(NativeDataReplacement.blocked(context))
            assertTrue(File(receiptDir(), "incoming/photos/$active.jpg").isFile)
            assertEquals(phase == "receipt-committed", NativeReplacementJournal(context).read().committed)
            assertTrue(BackupRestoreManager(context).resumeInterruptedReplacement().isSuccess)
            assertArrayEquals(incoming, photo(active).readBytes()); assertFalse(photo(deleted).exists())
            assertNotNull(db.workoutHistoryDao().getWorkoutWithDetails("cloud-workout"))
            assertFalse(NativeReplacementJournal(context).exists()); assertFalse(NativeDataReplacement.blocked(context))
        }
    }
    @Test fun failureAfterPhotoDeletionRollsBackOriginalsAndCanRetryRepeatedly() = runTest(dispatcher) {
        photo(deleted).writeText("original"); val data = payload()
        val manager = BackupRestoreManager(context).apply {
            restoreCheckpoint = { if (it == "photo-deleted:$deleted") throw IOException("delete fault") }
        }
        prepared(data).use { stage -> reject { install(manager, data, stage) } }
        assertEquals("original", photo(deleted).readText()); assertFalse(photo(active).exists())
        assertTrue(manager.resumeInterruptedReplacement().isFailure)
        assertEquals("original", photo(deleted).readText()); assertFalse(photo(active).exists())
        assertTrue(BackupRestoreManager(context).resumeInterruptedReplacement().isSuccess)
        assertFalse(photo(deleted).exists()); assertArrayEquals(incoming, photo(active).readBytes())
    }
    @Test fun corruptedArchivePhotoBlocksReplayBeforeChangingAnyLiveFile() = runTest(dispatcher) {
        photo(deleted).writeText("original"); val data = payload()
        prepared(data).use { stage ->
            val manager = BackupRestoreManager(context).apply { restoreCheckpoint = { if (it == "receipt-prepared") throw IOException("stop") } }
            reject { install(manager, data, stage) }
        }
        File(receiptDir(), "incoming/photos/$active.jpg").writeText("corrupt")
        assertTrue(BackupRestoreManager(context).resumeInterruptedReplacement().isFailure)
        assertEquals("original", photo(deleted).readText()); assertFalse(photo(active).exists())
        assertTrue(NativeReplacementJournal(context).exists()); assertTrue(NativeDataReplacement.blocked(context))
    }
    @Test fun deletionPlanMustMatchDeletedReferencesEvenWithAnUpdatedManifestHash() = runTest(dispatcher) {
        photo(deleted).writeText("original"); val data = payload()
        prepared(data).use { stage ->
            val manager = BackupRestoreManager(context).apply { restoreCheckpoint = { if (it == "receipt-prepared") throw IOException("stop") } }
            reject { install(manager, data, stage) }
        }
        val manifest = File(receiptDir(), "incoming/manifest.json")
        manifest.writeText(JSONObject(manifest.readText()).put("cloudPhotoDeletes", JSONArray().put("../outside")).toString())
        val journal = NativeReplacementJournal(context)
        journal.write(journal.read().copy(payloadSha256 = manifest.inputStream().use { ArchiveIO.hash(it) }))
        assertTrue(BackupRestoreManager(context).resumeInterruptedReplacement().isFailure)
        assertEquals("original", photo(deleted).readText()); assertFalse(photo(active).exists())
    }
}
