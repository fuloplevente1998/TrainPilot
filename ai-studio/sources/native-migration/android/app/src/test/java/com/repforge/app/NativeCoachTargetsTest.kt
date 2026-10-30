package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.ui.coach.NativeCoachTargets
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeCoachTargetsTest {
    private lateinit var db: TrainPilotDatabase
    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val utc = ZoneId.of("UTC")
    @Before fun open() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        db.programDao().upsertPrograms(programs); db.programDao().upsertProgramDays(days)
    }
    @After fun close() { db.close() }
    private suspend fun plan(id: String, start: String, date: String = "2026-10-24", cancelled: Boolean = false,
        programId: String = "home-basic", dayId: String? = null, status: String = "planned") {
        val day = dayId ?: db.programDao().getProgramWithDaysById("home-basic")!!.days.minBy { it.sortOrder }.dayId
        db.calendarScheduleDao().upsertEntry(CalendarScheduleEntity(id, "", date, start, start, programId,
            "Program", day, "Day", status, 1L, "{\"cancelled\":$cancelled}"))
    }
    @Test fun ordersMixedOffsetsByActualInstantInsteadOfLexicalClockTime() = runBlocking {
        plan("later", "2026-10-24T08:00:00Z")
        plan("earlier", "2026-10-24T10:00:00+03:00")
        val target = NativeCoachTargets(db).select("home-basic", "en", now, utc)!!
        assertEquals("earlier", target.schedule!!.id)
        assertFalse(target.upcoming)
    }
    @Test fun previousUtcDateCanStillBeTodaysScheduledWorkoutInTheLocalZone() = runBlocking {
        plan("local-today", "2026-10-23T22:30:00Z", date = "2026-10-23")
        val target = NativeCoachTargets(db).select("home-basic", "hu", now, ZoneId.of("Europe/Budapest"))!!
        assertEquals("local-today", target.schedule!!.id)
        assertFalse(target.upcoming)
    }
    @Test fun daylightSavingTransitionDoesNotMisclassifyAnUpcomingLocalDay() = runBlocking {
        plan("dst", "2026-10-25T02:30:00+01:00", date = "2026-10-25")
        val target = NativeCoachTargets(db).select("home-basic", "de", now, ZoneId.of("Europe/Budapest"))!!
        assertEquals("dst", target.schedule!!.id)
        assertTrue(target.upcoming)
    }
    @Test fun skipsMoreThanOneBoundedPageOfCancelledAndUnavailablePrograms() = runBlocking {
        for (i in 0 until 40) plan("skip-$i", "2026-10-24T00:00:00Z", cancelled = i % 2 == 0,
            programId = if (i % 2 == 0) "home-basic" else "missing-program")
        plan("available", "2026-10-24T01:00:00Z")
        assertEquals("available", NativeCoachTargets(db).select("home-basic", "ro", now, utc)!!.schedule!!.id)
    }
    @Test fun historyCompletedScheduleDoesNotRemainAnOpenCoachTarget() = runBlocking {
        plan("completed-in-history", "2026-10-24T06:00:00Z")
        plan("next", "2026-10-25T06:00:00Z", date = "2026-10-25")
        db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(0).copy(scheduleId = "completed-in-history"))
        val target = NativeCoachTargets(db).select("home-basic", "en", now, utc)!!
        assertEquals("next", target.schedule!!.id)
        assertTrue(target.upcoming)
    }
    @Test fun missingOrCancelledCycleDaysDoNotResetAValidProgramCycle() = runBlocking {
        val days = db.programDao().getProgramWithDaysById("home-basic")!!.days.sortedBy { it.sortOrder }
        assertTrue(days.size >= 2)
        plan("last-valid", "2026-10-22T10:00:00Z", date = "2026-10-22", dayId = days[0].dayId, status = "completed")
        plan("invalid-day", "2026-10-23T10:00:00Z", date = "2026-10-23", dayId = "deleted-day", status = "completed")
        plan("cancelled", "2026-10-23T11:00:00Z", date = "2026-10-23", cancelled = true, dayId = days[1].dayId)
        val target = NativeCoachTargets(db).select("home-basic", "en", now, utc)!!
        assertNull(target.schedule)
        assertEquals(days[1].dayId, target.dayId)
        assertEquals(days[1].nameEn, target.title)
    }
}
