package com.repforge.app

import androidx.room.Room
import androidx.room.withTransaction
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeJournalQueriesTest {
    private fun row(i: Int) = WorkoutHistoryEntity(id = "w-%04d".format(i), workoutKey = "key-$i", syncId = "sync-$i",
        started = "2026-10-24T12:00:00Z", finished = "2026-10-24T12:01:00Z", startedEpochMs = i.toLong(),
        finishedEpochMs = i + 60000L, durationSeconds = 60, programId = "program", programName = "Program",
        dayId = "day", dayName = "Nap", scheduleId = "", quickWorkout = false, originDevice = "phone",
        ownershipProtocol193 = 0, ownerEpoch193 = 0, effortFeedback = null, feedbackSkipped = false,
        feedbackUpdatedAt = 0, totalVolumeKg = 0.0, completedSetsCount = 0, totalSetsCount = 0,
        totalRepsCount = 0, hasPr226 = false, prRecordsJson226 = "[]", wearMetricsJson = null,
        health240Json = null, timeLogs162Json = null, activeIntervalsJson = null, editedAt = null,
        rawJson = "{\"large\":\"${"x".repeat(4096)}\"}")

    @Test fun emptyAndLargeJournalsReturnOnlyEightWatchParentsAndBoundedSummaryPages() = runBlocking {
        for (count in listOf(0, 200, 1500)) {
            val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TrainPilotDatabase::class.java).build()
            try {
                val dao = db.workoutHistoryDao()
                db.withTransaction { repeat(count) { dao.insertWorkout(row(it)) } }
                val mini = dao.getWearMiniJournal()
                assertEquals(minOf(8, count), mini.size)
                if (count > 0) assertEquals("w-%04d".format(count - 1), mini.first().workout.id)
                val first = dao.getJournalSummaryPage(32, 0)
                val next = dao.getJournalSummaryPage(32, 32)
                assertEquals(minOf(32, count), first.size)
                assertEquals(minOf(32, (count - 32).coerceAtLeast(0)), next.size)
                assertTrue(first.map { it.id }.intersect(next.map { it.id }.toSet()).isEmpty())
                assertEquals(count, dao.getWorkoutCount())
            } finally { db.close() }
        }
    }
    @Test fun equalTimestampsHaveStablePageBoundariesAndUpdatesAreNotCachedByCount() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TrainPilotDatabase::class.java).build()
        try {
            val dao = db.workoutHistoryDao()
            db.withTransaction { repeat(40) { dao.insertWorkout(row(it).copy(startedEpochMs = 1000)) } }
            val first = dao.getJournalSummaryPage(32, 0)
            val last = dao.getJournalSummaryPage(32, 32)
            assertEquals(40, (first + last).map { it.id }.toSet().size)
            dao.insertWorkout(row(39).copy(startedEpochMs = 1000, dayName = "Updated"))
            assertEquals("Updated", dao.getJournalSummaryPage(32, 0).first().dayName)
            assertEquals(40, dao.getWorkoutCount())
        } finally { db.close() }
    }
}
