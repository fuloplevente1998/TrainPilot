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
    private fun row(i: Int) = JournalTestFixtures.row(i)

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
