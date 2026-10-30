package com.repforge.app

import android.content.Context
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeCloudPhotoStageTest {
    private lateinit var context: Context
    private val one = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    private val two = "bbbbbbbb-bbbb-cccc-dddd-eeeeeeeeeeee"
    private val bytes = byteArrayOf(1, 2, 3, 4, 5)
    @Before fun open() {
        context = RuntimeEnvironment.getApplication()
        File(context.filesDir, "workout_photos").deleteRecursively()
        File(context.noBackupFilesDir, "cloud_photo_staging").deleteRecursively()
    }
    @After fun close() { open() }
    private fun local(id: String = one) = File(context.filesDir, "workout_photos/$id.jpg").also { it.parentFile!!.mkdirs() }
    private fun photo(id: String = one, deleted: Any? = null) = JSONObject().put("id", id).put("driveFileId", "drive-$id")
        .also { if (deleted != null) it.put("deletedAt", deleted) }
    private fun data(vararg photos: JSONObject) = JSONObject().put("history", JSONArray().put(
        JSONObject().put("id", "workout").put("photos", JSONArray(photos.toList()))))

    @Test fun liveDraftPhotoOverridesDeletedHistoryReferenceAndNeverEntersTheDeletionPlan() = runBlocking {
        local().writeBytes(bytes)
        val activeDraftPhoto = photo()
        val payload = data(photo(deleted = 123L)).put("draft", JSONObject().put("session",
            JSONObject().put("photos", JSONArray().put(activeDraftPhoto))))
        NativeCloudPhotoStage(context, payload).use { stage ->
            stage.prepare({ _, _ -> "new-draft-drive" }, { _, _, _ -> fail("Local draft bytes exist") })
            assertTrue(stage.deletedPhotoIds.isEmpty())
            assertEquals("new-draft-drive", activeDraftPhoto.getString("driveFileId"))
            assertArrayEquals(bytes, File(stage.photos, "$one.jpg").readBytes())
            stage.markInstalled()
            stage.finishRemoteDeletes { _, _ -> fail("A resumable draft is still using this photo") }
        }
        assertArrayEquals(bytes, local().readBytes())
    }
    private fun meta(id: String = one, content: ByteArray = bytes) = JSONObject().put("name", "trainpilot-photo-$id.jpg")
        .put("mimeType", "image/jpeg").put("size", content.size.toString()).put("md5Checksum",
            MessageDigest.getInstance("MD5").digest(content).joinToString("") { "%02x".format(it.toInt() and 255) })
    private fun prepare(stage: NativeCloudPhotoStage) = stage.prepare({ id, _ -> "verified-$id" },
        { id, _, file -> NativeCloudPhotoStage.installDownload(id, meta(id), bytes, file) })

    @Test fun downloadedPhotosStayPrivateUntilTheLocalCommitAndAreCleanedOnClose() {
        val live = local()
        lateinit var staged: File
        NativeCloudPhotoStage(context, data(photo())).use { stage ->
            prepare(stage); stage.requireUnchanged(); staged = File(stage.photos, "$one.jpg")
            assertArrayEquals(bytes, staged.readBytes()); assertFalse(live.exists())
        }
        assertFalse(staged.exists()); assertFalse(live.exists())
    }
    @Test fun uploadsReadAnImmutableCopyAndUpdateOnlyTheMergedMetadata() {
        val live = local().apply { writeBytes(bytes) }; val row = photo().put("driveFileId", "stale-drive")
        NativeCloudPhotoStage(context, data(row)).use { stage ->
            stage.prepare({ _, copy ->
                assertNotEquals(live.canonicalFile, copy.canonicalFile); assertArrayEquals(bytes, copy.readBytes()); "new-drive"
            }, { _, _, _ -> fail("Local bytes must not be replaced from Drive") })
            assertEquals("new-drive", row.getString("driveFileId")); assertArrayEquals(bytes, live.readBytes())
            stage.requireUnchanged()
        }
    }
    @Test fun remoteDeletionCannotRunBeforeAConfirmedLocalPhotoCommit() {
        val live = local().apply { writeBytes(bytes) }; val row = photo(deleted = 123L)
        NativeCloudPhotoStage(context, data(row)).use { stage ->
            prepare(stage)
            assertThrows(IllegalStateException::class.java) { runBlocking { stage.finishRemoteDeletes { _, _ -> fail() } } }
            assertArrayEquals(bytes, live.readBytes()); assertEquals("drive-$one", row.getString("driveFileId"))
        }
    }
    @Test fun failedDownloadDoesNotTouchAnyLivePhotoOrPretendTheStageIsReady() {
        val live = local().apply { writeBytes(bytes) }
        NativeCloudPhotoStage(context, data(photo(), photo(two))).use { stage ->
            assertThrows(IOException::class.java) { stage.prepare({ _, _ -> "drive" }, { _, _, _ -> throw IOException("network") }) }
            assertThrows(IllegalStateException::class.java) { stage.requireUnchanged() }
            assertArrayEquals(bytes, live.readBytes()); assertFalse(local(two).exists())
        }
    }
    @Test fun failedUploadLeavesLiveBytesAndOriginalDriveIdentityIntact() {
        val live = local().apply { writeBytes(bytes) }; val row = photo()
        NativeCloudPhotoStage(context, data(row)).use { stage ->
            assertThrows(IOException::class.java) { stage.prepare({ _, _ -> throw IOException("network") }, { _, _, _ -> fail() }) }
            assertArrayEquals(bytes, live.readBytes()); assertEquals("drive-$one", row.getString("driveFileId"))
        }
    }
    @Test fun sameLengthLocalEditAfterStagingIsAConflict() {
        val live = local().apply { writeBytes(bytes) }
        NativeCloudPhotoStage(context, data(photo())).use { stage ->
            prepare(stage); live.writeBytes(byteArrayOf(5, 4, 3, 2, 1))
            assertThrows(IllegalStateException::class.java) { stage.requireUnchanged() }
        }
    }
    @Test fun mutationOfTheUploadCopyCannotBecomeAnAcceptedLocalReplacement() {
        val live = local().apply { writeBytes(bytes) }
        NativeCloudPhotoStage(context, data(photo())).use { stage ->
            assertThrows(IllegalStateException::class.java) { stage.prepare({ _, copy ->
                copy.writeBytes(bytes.reversedArray()); "drive"
            }, { _, _, _ -> fail() }) }
            assertArrayEquals(bytes, live.readBytes())
        }
    }
    @Test fun aNewlyAppearingLiveFileCannotBeOverwrittenByAnOlderDownload() {
        NativeCloudPhotoStage(context, data(photo())).use { stage ->
            prepare(stage); val live = local().apply { writeText("new local photo") }
            assertThrows(IllegalStateException::class.java) { stage.requireUnchanged() }
            assertEquals("new local photo", live.readText())
        }
    }
    @Test fun deletingALiveFileAfterStagingDoesNotAllowResurrection() {
        val live = local().apply { writeBytes(bytes) }
        NativeCloudPhotoStage(context, data(photo())).use { stage ->
            prepare(stage); live.delete()
            assertThrows(IllegalStateException::class.java) { stage.requireUnchanged() }
            assertFalse(live.exists())
        }
    }
    @Test fun stagedOrArchiveCopyCorruptionIsRejected() {
        NativeCloudPhotoStage(context, data(photo())).use { stage ->
            prepare(stage)
            val bad = File(context.cacheDir, "bad-photo-copy").apply { writeText("bad") }
            try { assertThrows(IllegalStateException::class.java) { stage.verifiedCopyHash(one, bad) } } finally { bad.delete() }
            File(stage.photos, "$one.jpg").writeText("tampered")
            assertThrows(IllegalStateException::class.java) { stage.requireUnchanged() }
        }
    }
    @Test fun corruptedBytesCannotPassWithTheRightLengthAndWrongMd5() {
        val target = local()
        assertThrows(IOException::class.java) { NativeCloudPhotoStage.installDownload(one, meta(), bytes.reversedArray(), target) }
        assertFalse(target.exists())
    }
    @Test fun downloadMetadataMustBindNameTypeLengthAndChecksum() {
        for (metadata in listOf(meta().put("name", "other.jpg"), meta().put("mimeType", "text/plain"),
            meta().put("size", 100), meta().apply { remove("md5Checksum") }, meta().put("md5Checksum", "invalid"))) {
            assertThrows(IOException::class.java) { NativeCloudPhotoStage.installDownload(one, metadata, bytes, local()) }
            assertFalse(local().exists())
        }
    }
    @Test fun validUppercaseMd5AndStringSizeRemainSupported() {
        val metadata = meta().put("md5Checksum", meta().getString("md5Checksum").uppercase())
        NativeCloudPhotoStage.installDownload(one, metadata, bytes, local())
        assertArrayEquals(bytes, local().readBytes())
    }
    @Test fun activePathTraversalReferencesFailBeforeCreatingAStage() {
        assertThrows(IOException::class.java) { NativeCloudPhotoStage(context, data(photo("../outside"))) }
        assertFalse(File(context.noBackupFilesDir, "cloud_photo_staging").exists())
    }
    @Test fun conflictingActiveDriveIdentitiesDoNotPickAnArbitraryRemoteFile() {
        assertThrows(IOException::class.java) { NativeCloudPhotoStage(context, data(photo(), photo().put("driveFileId", "another-drive"))) }
    }
    @Test fun duplicateActiveReferencesUseOneUploadAndReceiveTheSameVerifiedIdentity() {
        local().writeBytes(bytes); val a = photo(); val b = photo(); var uploads = 0
        NativeCloudPhotoStage(context, data(a, b)).use { stage ->
            stage.prepare({ _, _ -> uploads++; "verified" }, { _, _, _ -> fail() })
            assertEquals(1, uploads); assertEquals("verified", a.getString("driveFileId")); assertEquals("verified", b.getString("driveFileId"))
        }
    }
    @Test fun anActiveReferenceProtectsTheSameIdFromAConflictingDeletedReference() {
        local().writeBytes(bytes)
        NativeCloudPhotoStage(context, data(photo(), photo(deleted = 123))).use { stage ->
            prepare(stage); assertTrue(stage.deletedPhotoIds.isEmpty()); stage.markInstalled()
            runBlocking { stage.finishRemoteDeletes { _, _ -> fail("Active photo cannot be deleted") } }
        }
    }
    @Test fun zeroFalseEmptyAndNullDeletionMarkersStayActive() {
        for (value in listOf(0, 0.0, "0", "0.0", false, "", JSONObject.NULL))
            assertFalse(NativeCloudPhotoStage.references(data(photo(deleted = value))).single().deleted)
        assertTrue(NativeCloudPhotoStage.references(data(photo(deleted = "2026-10-25T00:00:00Z"))).single().deleted)
    }
    @Test fun failedRemoteDeletionKeepsADurableIdentityForIdempotentRetry() {
        val row = photo(deleted = 123)
        NativeCloudPhotoStage(context, data(row)).use { stage ->
            prepare(stage); stage.markInstalled()
            assertThrows(IOException::class.java) { runBlocking { stage.finishRemoteDeletes { _, _ -> throw IOException("network") } } }
            assertEquals("drive-$one", row.getString("driveFileId"))
            var called = 0; runBlocking { stage.finishRemoteDeletes { _, id -> assertEquals("drive-$one", id); called++ } }
            assertEquals(1, called); assertEquals("drive-$one", row.getString("driveFileId"))
        }
    }
    @Test fun missingPhotoWithoutAnyLocalOrRemoteBytesIsNotASuccessfulSync() {
        NativeCloudPhotoStage(context, data(photo().put("driveFileId", ""))).use { stage ->
            assertThrows(IOException::class.java) { prepare(stage) }
            assertFalse(local().exists())
        }
    }
}
