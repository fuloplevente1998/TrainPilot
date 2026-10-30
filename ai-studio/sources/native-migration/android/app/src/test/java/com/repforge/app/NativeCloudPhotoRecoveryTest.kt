package com.repforge.app

import android.content.Context
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
            assertThrows(IllegalStateException::class.java) { runBlocking { stage.finishRemoteDeletes { _, _ -> fail() } } }
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
    @Test fun weightDeletionIntentSurvivesTheVerifiedCloudZipRedoAndLaterStaleImport() = runTest(dispatcher) {
        val importer = LegacyWebViewMigrator(context, db, settings)
        val stale = JSONObject().put("history", JSONArray()).put("weights", JSONArray().put(
            JSONObject().put("id", "deleted-weight").put("date", "2026-10-25").put("kg", 72)))
        importer.importBackupJson(stale, false)
        com.repforge.app.ui.progress.NativeWeightJournalRepository(context, db)
            .delete(db.bodyWeightDao().getWeightById("deleted-weight")!!)
        val snapshot = importer.exportBackupJson(false)
        db.keyValueDocumentDao().deleteWeightTombstoneDocuments()
        prepared(snapshot).use { stage ->
            val manager = BackupRestoreManager(context).apply {
                restoreCheckpoint = { if (it == "receipt-prepared") throw IOException("stop") }
            }
            reject { install(manager, snapshot, stage) }
        }
        assertTrue(NativeWeightTombstones18.load(db).isEmpty())
        assertTrue(BackupRestoreManager(context).resumeInterruptedReplacement().isSuccess)
        assertEquals(1, NativeWeightTombstones18.load(db).size)
        importer.importBackupJson(stale, false)
        assertTrue(db.bodyWeightDao().getAllWeights().isEmpty())
    }

    @Test fun changedLocalMetadataCannotPublishARejectedCloudSnapshot() = runTest(dispatcher) {
        val migrator = LegacyWebViewMigrator(context, db, settings)
        val original = migrator.exportBackupJson(false)
        photo(deleted).writeText("original")
        var published = false
        val data = payload()
        prepared(data).use { stage ->
            settings.setLanguage("ro")
            reject { NativeCloudMergeGuard.installAndPublish(context, original, data, stage, "owner") { published = true } }
        }
        assertFalse(published); assertEquals("ro", settings.current().language)
        assertEquals("original", photo(deleted).readText()); assertFalse(photo(active).exists())
        assertNull(db.keyValueDocumentDao().getDocument("cloudBase:owner"))
    }

    @Test fun changedStagedPhotoCannotPublishEvenWhenCanonicalMetadataStillMatches() = runTest(dispatcher) {
        val original = LegacyWebViewMigrator(context, db, settings).exportBackupJson(false)
        var published = false
        val data = payload()
        prepared(data).use { stage ->
            File(stage.photos, "$active.jpg").writeText("tampered")
            reject { NativeCloudMergeGuard.installAndPublish(context, original, data, stage, "owner") { published = true } }
        }
        assertFalse(published); assertFalse(photo(active).exists())
        assertNull(db.keyValueDocumentDao().getDocument("cloudBase:owner"))
    }

    @Test fun publisherSeesDurableAcceptedPhotosAndMetadataOutsideTheReplacementLease() = runTest(dispatcher) {
        val original = LegacyWebViewMigrator(context, db, settings).exportBackupJson(false)
        val data = payload()
        prepared(data).use { stage ->
            val continuation = NativeCloudMergeGuard.installAndPublish(context, original, data, stage, "owner") { accepted ->
                assertFalse(NativeDataReplacement.blocked(context))
                assertFalse(PhoneWorkoutExecution.store(context).restoreBlocked())
                assertFalse(NativeReplacementJournal(context).exists())
                assertNotNull(db.workoutHistoryDao().getWorkoutWithDetails("cloud-workout"))
                assertArrayEquals(incoming, photo(active).readBytes())
                assertEquals("en", accepted.getJSONObject("settings").getString("language"))
                assertNull(db.keyValueDocumentDao().getDocument("cloudBase:owner"))
                // A publisher does not own the immutable local/base payload.
                accepted.getJSONObject("settings").put("language", "de")
            }
            continuation.requireCurrent(context)
        }
        val base = JSONObject(db.keyValueDocumentDao().getDocument("cloudBase:owner")!!.json)
        assertEquals("en", base.getJSONObject("settings").getString("language"))
        assertEquals("en", settings.current().language)
    }

    @Test fun failedPublicationKeepsAcceptedLocalDataAndPreviousBaseForTheNextSync() = runTest(dispatcher) {
        val migrator = LegacyWebViewMigrator(context, db, settings)
        val previous = "{\"history\":[]}"
        db.keyValueDocumentDao().putDocument(com.repforge.app.data.db.KeyValueDocumentEntity("cloudBase:owner", previous, 1))
        val original = migrator.exportBackupJson(false)
        val data = payload()
        prepared(data).use { stage -> reject {
            NativeCloudMergeGuard.installAndPublish(context, original, data, stage, "owner") { throw IOException("offline") }
        } }
        assertNotNull(db.workoutHistoryDao().getWorkoutWithDetails("cloud-workout"))
        assertArrayEquals(incoming, photo(active).readBytes())
        assertEquals(previous, db.keyValueDocumentDao().getDocument("cloudBase:owner")!!.json)
        assertNull(db.keyValueDocumentDao().getDocument("cloudStatus"))
        assertFalse(NativeDataReplacement.blocked(context)); assertFalse(NativeReplacementJournal(context).exists())
        // Retry starts from the actual accepted local data, not a lost in-memory outbox.
        val retry = migrator.exportBackupJson(false)
        val retryMerged = JSONObject(retry.toString())
        var published = 0
        prepared(retryMerged).use { stage ->
            NativeCloudMergeGuard.installAndPublish(context, retry, retryMerged, stage, "owner") { published++ }
        }
        assertEquals(1, published)
        assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
        assertNotEquals(previous, db.keyValueDocumentDao().getDocument("cloudBase:owner")!!.json)
    }

    @Test fun cancelledPublicationCannotMarkBaseOrStatusAsSuccessfullySynced() = runTest(dispatcher) {
        val original = LegacyWebViewMigrator(context, db, settings).exportBackupJson(false)
        val data = payload()
        val entered = CompletableDeferred<Unit>()
        prepared(data).use { stage ->
            val job = launch {
                NativeCloudMergeGuard.installAndPublish(context, original, data, stage, "owner") {
                    entered.complete(Unit); awaitCancellation()
                }
            }
            entered.await(); job.cancelAndJoin()
            assertTrue(job.isCancelled)
        }
        assertNotNull(db.workoutHistoryDao().getWorkoutWithDetails("cloud-workout"))
        assertNull(db.keyValueDocumentDao().getDocument("cloudBase:owner"))
        assertNull(db.keyValueDocumentDao().getDocument("cloudStatus"))
        assertFalse(NativeDataReplacement.blocked(context))
    }

    @Test fun laterExplicitRestorePreventsOldPublicationContinuationFromUpdatingTheNewDataset() = runTest(dispatcher) {
        val original = LegacyWebViewMigrator(context, db, settings).exportBackupJson(false)
        val data = payload()
        prepared(data).use { stage -> reject {
            NativeCloudMergeGuard.installAndPublish(context, original, data, stage, "owner") {
                NativeDataReplacement.exclusively(context) {
                    BackupRestoreManager(context).installCloudMerge(JSONObject().put("history", JSONArray())
                        .put("settings", JSONObject().put("language", "ro")))
                }
            }
        } }
        assertEquals("ro", settings.current().language)
        assertNull(db.workoutHistoryDao().getWorkoutWithDetails("cloud-workout"))
        assertNull(db.keyValueDocumentDao().getDocument("cloudBase:owner"))
        assertNull(db.keyValueDocumentDao().getDocument("cloudStatus"))
    }

    @Test fun ordinaryEditDuringPublicationRemainsLocalAndBaseStillDescribesThePublishedSnapshot() = runTest(dispatcher) {
        val original = LegacyWebViewMigrator(context, db, settings).exportBackupJson(false)
        val data = payload()
        prepared(data).use { stage ->
            NativeCloudMergeGuard.installAndPublish(context, original, data, stage, "owner") {
                NativeDataReplacement.mutate(context) { settings.setLanguage("ro") }
            }
        }
        assertEquals("ro", settings.current().language)
        assertEquals("en", JSONObject(db.keyValueDocumentDao().getDocument("cloudBase:owner")!!.json)
            .getJSONObject("settings").getString("language"))
    }

    private suspend fun remoteDelete(id: String = deleted, driveId: String = "drive-deleted", action: () -> Unit) =
        NativeCloudPhotoDeletion.deleteUnreferenced(context, id, driveId,
            NativeDataReplacement.continuationForCurrentData(context), action)

    private fun photoFrame(photoId: String = deleted, driveId: String = "drive-deleted") = JSONObject("""{
        "active":true,"workoutId":"photo-admission","generation":1,"executionRevision":0,"timerOwner":"phone",
        "started":"2026-10-24T10:00:00Z","currentExercise":0,"exercises":[{"id":"plank","repUnit":"mp",
        "sets":[{"setId":"one","reps":"30","done":false}]}]}""")
        .put("photos", JSONArray().put(JSONObject().put("id", photoId).put("driveFileId", driveId)))

    /** A blocked synchronous transport call runs only on IO; the test can still drive Main/Room. */
    private suspend fun whileRemoteDeleteIsInFlight(action: suspend () -> Unit) = coroutineScope {
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val job = async(Dispatchers.IO) {
            remoteDelete { entered.complete(Unit); check(release.await(15, TimeUnit.SECONDS)) }
        }
        entered.await()
        try { action() } finally { release.countDown(); job.await() }
    }

    @Test fun newLiveRoomPhotoVetoesStaleRemoteDeletionBeforeAnyTransportCall() = runTest(dispatcher) {
        LegacyWebViewMigrator(context, db, settings).importBackupJson(payload(), false)
        var called = false
        reject { remoteDelete(active, "drive-active") { called = true } }
        assertFalse(called)
        assertNotNull(db.workoutHistoryDao().getWorkoutWithDetails("cloud-workout"))
        NativeCloudPhotoDeletion.requireNoPendingDeletes()
    }

    @Test fun anotherPhotoIdUsingTheSameDriveObjectAlsoVetoesDeletion() = runTest(dispatcher) {
        val data = payload()
        data.getJSONArray("history").getJSONObject(0).getJSONArray("photos").getJSONObject(0)
            .put("driveFileId", "drive-deleted")
        LegacyWebViewMigrator(context, db, settings).importBackupJson(data, false)
        var called = false
        reject { remoteDelete { called = true } }
        assertFalse(called)
    }

    @Test fun zeroDeletedAtVariantsRemainLiveInRoomAndCannotBeRemotelyDeleted() = runTest(dispatcher) {
        val importer = LegacyWebViewMigrator(context, db, settings)
        for (marker in listOf<Any>(0, 0.0, false, "", JSONObject.NULL)) {
            val data = payload()
            data.getJSONArray("history").getJSONObject(0).getJSONArray("photos").getJSONObject(1).put("deletedAt", marker)
            importer.importBackupJson(data, false)
            assertNull(db.workoutHistoryDao().getWorkoutWithDetails("cloud-workout")!!.photos.first { it.id == deleted }.deletedAt)
            var called = false
            reject { remoteDelete { called = true } }
            assertFalse(called)
        }
    }

    @Test fun nestedImportedDraftPhotoRemainsProtectedEvenWithoutAJournalRow() = runTest(dispatcher) {
        val draft = JSONObject().put("session", photoFrame())
        db.keyValueDocumentDao().putDocument(com.repforge.app.data.db.KeyValueDocumentEntity("draft", draft.toString(), 1))
        var called = false
        reject { remoteDelete { called = true } }
        assertFalse(called); assertNotNull(db.keyValueDocumentDao().getDocument("draft"))
    }

    @Test fun canonicalOnlyUnpublishedPhotoReferenceVetoesDeletion() = runTest(dispatcher) {
        withContext(Dispatchers.Main) { PhoneWorkoutExecution.store(context).prepare(photoFrame(), 0) }
        assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
        var called = false
        reject { remoteDelete { called = true } }
        assertFalse(called)
        assertTrue(PhoneWorkoutExecution.store(context).snapshot()!!.getBoolean("active"))
    }

    @Test fun publicPhoneOrWatchMirrorPhotoReferenceAlsoVetoesDeletion() = runTest(dispatcher) {
        withContext(Dispatchers.Main) { ActiveWorkoutStore.saveOwned(context, photoFrame().put("timerOwner", "watch")) }
        var called = false
        reject { remoteDelete { called = true } }
        assertFalse(called); assertTrue(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
    }

    @Test fun inFlightDeletionRejectsReplacementButDoesNotHoldMetadataOrWorkoutFences() = runTest(dispatcher) {
        whileRemoteDeleteIsInFlight {
            assertFalse(NativeDataReplacement.blocked(context))
            assertFalse(PhoneWorkoutExecution.store(context).restoreBlocked())
            var replaced = false
            reject { NativeDataReplacement.exclusively(context) { replaced = true } }
            reject { NativeDataReplacement.recover(context) { replaced = true } }
            assertFalse(replaced); assertFalse(NativeDataReplacement.blocked(context))
            settings.setLanguage("ro")
            assertEquals("ro", settings.current().language)
        }
        NativeCloudPhotoDeletion.requireNoPendingDeletes()
        NativeDataReplacement.exclusively(context) { assertTrue(NativeDataReplacement.blocked(context)) }
    }

    @Test fun reservedReferenceCannotRebindThroughImportOrEitherSnapshotButNewUuidWorkoutsStillStart() = runTest(dispatcher) {
        whileRemoteDeleteIsInFlight {
            val data = payload()
            data.getJSONArray("history").getJSONObject(0).getJSONArray("photos").getJSONObject(1).remove("deletedAt")
            val importer = LegacyWebViewMigrator(context, db, settings)
            reject { importer.importBackupJson(data, false) }
            reject { importer.saveWorkoutJsonToRoom(data.getJSONArray("history").getJSONObject(0)) }
            assertEquals(0, db.workoutHistoryDao().getWorkoutCount())
            withContext(Dispatchers.Main) {
                assertThrows(IllegalStateException::class.java) { ActiveWorkoutStore.saveOwned(context, photoFrame()) }
                assertThrows(IllegalStateException::class.java) { PhoneWorkoutExecution.store(context).prepare(photoFrame(), 0) }
                assertNull(PhoneWorkoutExecution.store(context).snapshot())
                assertFalse(ActiveWorkoutStore.current(context)?.optBoolean("active") == true)
                NativeWorkoutSession.installPrepared(context, photoFrame(active, "new-drive"))
            }
            assertTrue(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
        }
        NativeCloudPhotoDeletion.requireNoPendingDeletes()
    }

    @Test fun coroutineCancellationDoesNotReleaseAdmissionWhileThePhysicalDeleteCallStillRuns() = runTest(dispatcher) {
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val job = launch(Dispatchers.IO) {
            remoteDelete { entered.complete(Unit); check(release.await(15, TimeUnit.SECONDS)) }
        }
        entered.await()
        try {
            job.cancel()
            assertThrows(IllegalStateException::class.java) { NativeCloudPhotoDeletion.requireNoPendingDeletes() }
            reject { NativeDataReplacement.exclusively(context) { fail("Physical request still running") } }
        } finally { release.countDown(); job.join() }
        assertTrue(job.isCancelled)
        NativeCloudPhotoDeletion.requireNoPendingDeletes()
    }

    @Test fun transportFailureReleasesReservationAndAllowsExplicitRetryOrRestore() = runTest(dispatcher) {
        var failure: IOException? = null
        try { remoteDelete { throw IOException("network uncertainty") } } catch (error: IOException) { failure = error }
        assertEquals("network uncertainty", failure?.message)
        NativeCloudPhotoDeletion.requireNoPendingDeletes()
        var called = 0
        remoteDelete { called++ }
        assertEquals(1, called)
        NativeDataReplacement.exclusively(context) { }
    }

    @Test fun malformedDraftFailsClosedBeforeDeletingAndDoesNotLeakAReservation() = runTest(dispatcher) {
        db.keyValueDocumentDao().putDocument(com.repforge.app.data.db.KeyValueDocumentEntity("draft", "{broken", 1))
        var called = false
        reject { remoteDelete { called = true } }
        assertFalse(called)
        NativeCloudPhotoDeletion.requireNoPendingDeletes()
    }

    @Test fun oldMergeContinuationCannotDeleteAfterACompletedReplacement() = runTest(dispatcher) {
        val stale = NativeDataReplacement.continuationForCurrentData(context)
        NativeDataReplacement.exclusively(context) { }
        var called = false
        reject { NativeCloudPhotoDeletion.deleteUnreferenced(context, deleted, "drive-deleted", stale) { called = true } }
        assertFalse(called)
        NativeCloudPhotoDeletion.requireNoPendingDeletes()
    }

    @Test fun draftPhotoBytesAreIncludedInTheVerifiedZipRedoAndSurviveItsHistoryTombstone() = runTest(dispatcher) {
        photo(deleted).writeText("draft-original")
        val data = payload().put("draft", JSONObject().put("session", photoFrame())
            .put("savedAt", "2026-10-24T10:30:00Z"))
        prepared(data).use { stage ->
            assertTrue(stage.deletedPhotoIds.isEmpty())
            val manager = BackupRestoreManager(context).apply {
                restoreCheckpoint = { if (it == "receipt-prepared") throw IOException("interrupt draft photo import") }
            }
            reject { install(manager, data, stage) }
        }
        assertEquals("draft-original", File(receiptDir(), "incoming/photos/$deleted.jpg").readText())
        photo(deleted).writeText("damaged-after-crash")
        assertTrue(BackupRestoreManager(context).resumeInterruptedReplacement().isSuccess)
        assertEquals("draft-original", photo(deleted).readText())
        assertArrayEquals(incoming, photo(active).readBytes())
        assertTrue(db.keyValueDocumentDao().getDocument("draft")!!.json.contains(deleted))
    }
}
