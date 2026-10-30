package com.repforge.app

import androidx.room.Room
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeHomeDailyCountTest {
    private lateinit var db: TrainPilotDatabase
    private val zone = ZoneId.of("Europe/Budapest")
    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TrainPilotDatabase::class.java).build()
    }
    @After fun close() { db.close() }
    private suspend fun insert(id: Int, instant: Instant) {
        db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(id).copy(
            started = instant.toString(), finished = instant.plusSeconds(60).toString(),
            startedEpochMs = instant.toEpochMilli(), finishedEpochMs = instant.plusSeconds(60).toEpochMilli()))
    }
    private suspend fun count(date: LocalDate) = db.workoutHistoryDao().getWorkoutCountBetween(
        date.atStartOfDay(zone).toInstant().toEpochMilli(),
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())

    @Test fun localMidnightIsIncludedAndNextMidnightExcludedRegardlessOfUtcDate() = runBlocking {
        val day = LocalDate.parse("2026-10-24")
        assertEquals(0, count(day))
        insert(0, Instant.parse("2026-10-23T21:59:59.999Z"))
        insert(1, Instant.parse("2026-10-23T22:00:00Z"))
        insert(2, Instant.parse("2026-10-24T21:59:59.999Z"))
        insert(3, Instant.parse("2026-10-24T22:00:00Z"))
        assertEquals(4, db.workoutHistoryDao().getWorkoutCount())
        assertEquals(2, count(day))
    }

    @Test fun springAndAutumnDaysUse23And25HoursAndIncludeBothRepeatedLocalTimes() = runBlocking {
        val spring = LocalDate.parse("2026-03-29")
        val autumn = LocalDate.parse("2026-10-25")
        for ((date, hours) in listOf(spring to 23L, autumn to 25L)) {
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            assertEquals(hours, Duration.between(start, end).toHours())
            insert(hours.toInt(), start); insert(hours.toInt() + 100, end.minusMillis(1))
            insert(hours.toInt() + 200, end)
            assertEquals(2, count(date))
        }
        insert(500, Instant.parse("2026-10-25T02:30:00+02:00"))
        insert(501, Instant.parse("2026-10-25T02:30:00+01:00"))
        assertEquals(4, count(autumn))
    }

    @Test fun tombstonedWorkoutIdentitiesDoNotInflateTodaysCount() = runBlocking {
        val day = LocalDate.parse("2026-10-24")
        for (id in 0..3) insert(id, Instant.parse("2026-10-24T10:00:00Z"))
        val rows = (0..2).map(JournalTestFixtures::row)
        db.workoutHistoryDao().upsertTombstones(listOf(
            HistoryTombstoneEntity(rows[0].id, "", "[]", 1),
            HistoryTombstoneEntity(rows[1].syncId, "", "[]", 1),
            HistoryTombstoneEntity("other-id", rows[2].workoutKey, "[]", 1)))
        assertEquals(1, count(day))
    }
}
