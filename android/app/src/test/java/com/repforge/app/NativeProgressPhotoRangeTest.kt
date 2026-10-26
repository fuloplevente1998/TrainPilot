package com.repforge.app

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutPhotoEntity
import com.repforge.app.ui.progress.NativeProgressPhotoData
import com.repforge.app.ui.progress.NativeProgressPhotoDecode
import com.repforge.app.ui.progress.NativeStrengthRange
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
class NativeProgressPhotoRangeTest {
    private lateinit var db: TrainPilotDatabase
    private lateinit var context: Context

    @Before fun before() {
        context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
    }
    @After fun after() { db.close() }

    private fun photoId(index: Int) = "00000000-0000-0000-0000-${index.toString().padStart(12, '0')}"

    private suspend fun saveWorkout(index: Int) {
        val row = JournalTestFixtures.row(index).copy(
            startedEpochMs = 1_782_000_000_000L,
            finishedEpochMs = 1_782_000_060_000L
        )
        db.workoutHistoryDao().insertWorkout(row)
    }

    private suspend fun savePhoto(index: Int, workout: Int, deleted: String? = null) {
        db.workoutHistoryDao().insertPhotos(listOf(WorkoutPhotoEntity(
            id = photoId(index), workoutHistoryId = "w-${workout.toString().padStart(4, '0')}",
            label = "before", createdAt = "2026-06-01T12:00:00Z",
            updatedAt = 1L, width = 800, height = 600, bytes = 1024,
            driveFileId = null, deletedAt = deleted
        )))
    }

    @Test fun metadataOnlyExcludesDeletedOrphanAndTombstonedPhotos() = runBlocking(Dispatchers.IO) {
        saveWorkout(1)
        saveWorkout(2)
        savePhoto(1, 1) // missing file still legitimately has metadata
        savePhoto(2, 1, "2026-06-01T12:00:00Z")
        savePhoto(3, 2)
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(
            primaryId = "w-0002", workoutKey = "key-2",
            idsJson = "[\"w-0002\"]", deletedAt = 1L
        ))
        val orphan = WorkoutPhotoRepository(context).photoFile(photoId(9))
        orphan.writeText("orphan")
        try {
            val photos = NativeProgressPhotoData.load(db)
            assertEquals(1, photos.size)
            assertEquals(photoId(1), photos.single().id)
            assertEquals(1_782_000_000_000L, photos.single().workoutStartedEpochMs)
        } finally {
            orphan.delete()
        }
    }

    @Test fun missingCorruptOversizedAndBoundedDecodeAreRealStates() {
        val repository = WorkoutPhotoRepository(context)
        val missing = repository.photoFile(photoId(4))
        assertNull(NativeProgressPhotoDecode.decode(missing, 256))
        val corrupt = repository.photoFile(photoId(5))
        corrupt.writeText("not an image")
        val huge = repository.photoFile(photoId(6))
        RandomAccessFile(huge, "rw").use { it.setLength(41L * 1024L * 1024L) }
        val valid = repository.photoFile(photoId(7))
        try {
            assertNull(NativeProgressPhotoDecode.decode(corrupt, 256))
            assertNull(NativeProgressPhotoDecode.decode(huge, 256))
            val bitmap = Bitmap.createBitmap(1024, 768, Bitmap.Config.ARGB_8888)
            FileOutputStream(valid).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bitmap.recycle()
            val preview = NativeProgressPhotoDecode.decode(valid, 256)
            assertNotNull(preview)
            assertTrue(preview!!.width <= 256)
            assertTrue(preview.height <= 256)
        } finally {
            corrupt.delete()
            huge.delete()
            valid.delete()
        }
    }

    @Test fun calendarRangesHonorLocalMidnightDstAndRejectFuture() {
        val zone = ZoneId.of("Europe/Budapest")
        val today = LocalDate.of(2026, 10, 26)
        val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val lower = today.minusDays(29).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(lower, NativeStrengthRange.DAYS_30.startEpochMs(now, zone))
        assertTrue(NativeStrengthRange.DAYS_30.contains(lower, now, zone))
        assertFalse(NativeStrengthRange.DAYS_30.contains(lower - 1, now, zone))
        assertFalse(NativeStrengthRange.ALL.contains(now + 1, now, zone))
        // Budapest moves clocks back 2026-10-25. 30 local dates are not 30*24h.
        assertNotEquals(29L * 86_400_000L, now - lower - 12L * 3_600_000L)
        assertTrue(NativeStrengthRange.YEAR.contains(
            today.minusYears(1).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), now, zone))
        assertFalse(NativeStrengthRange.DAYS_90.contains(
            today.minusDays(90).atStartOfDay(zone).toInstant().toEpochMilli(), now, zone))
    }
}
