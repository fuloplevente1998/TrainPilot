package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.ui.calendar.NativeCalendarPlanner
import com.repforge.app.ui.calendar.NativeCalendarRepository
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeCalendarPlannerTest {
    private lateinit var db: TrainPilotDatabase
    private lateinit var repo: NativeCalendarRepository
    @Before fun open() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        runBlocking { TrainPilotDatabase.ensureBuiltinCatalogSeeded(db, context) }
        repo = NativeCalendarRepository(db)
    }
    @After fun close() { db.close() }
    private suspend fun settings(mode: String = "daily") = NativeCalendarPlanner.Settings(
        db.programDao().getProgramsWithDays().first().program.id, LocalDate.of(2026, 10, 1), LocalTime.of(18, 0), mode = mode)

    @Test fun fourWeeksRotateSeveralProgramDays() = runTest {
        val settings = settings()
        val preview = repo.preview(settings)
        val days = preview.program.days.sortedBy { it.sortOrder }.map { it.dayId }
        assertEquals(28, preview.plan.slots.size)
        assertEquals((0 until 28).map { days[it % days.size] }, preview.plan.slots.map { it.dayId })
    }
    @Test fun alternateAndWeeklyUseLocalDates() = runTest {
        assertEquals(14, repo.preview(settings("alternate")).plan.slots.size)
        val settings = settings("weekly").copy(weekdays = setOf(1, 3, 5))
        val slots = repo.preview(settings).plan.slots
        assertEquals(12, slots.size)
        assertTrue(slots.all { it.date.dayOfWeek.value % 7 in settings.weekdays })
    }
    @Test fun previewCancelWritesNothingAndSaveReopenRetryDoesNotDuplicate() = runTest {
        val settings = settings()
        val preview = repo.preview(settings)
        assertTrue(db.calendarScheduleDao().getAllEntries().isEmpty())
        assertNull(repo.plannerSettings())
        val first = repo.applyPreview(preview)
        assertEquals(first.map { it.id }, repo.applyPreview(preview).map { it.id })
        assertEquals(28, db.calendarScheduleDao().getAllEntries().size)
        assertEquals(settings, NativeCalendarRepository(db).plannerSettings())
    }
    @Test fun staleScheduleIsRejectedWithoutChangingSettings() = runTest {
        val settings = settings()
        val preview = repo.preview(settings)
        repo.save(NativeCalendarRepository.Edit(programId = settings.programId,
            dayId = preview.program.days.first().dayId, date = settings.start, time = settings.time, minutes = 45))
        assertTrue(runCatching { repo.applyPreview(preview) }.isFailure)
        assertNull(repo.plannerSettings())
        assertEquals(1, db.calendarScheduleDao().getAllEntries().size)
    }
    @Test fun rollbackDoesNotPersistSettingsOrPartialRows() = runTest {
        val preview = repo.preview(settings())
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER planner_fail BEFORE INSERT ON kv_documents BEGIN SELECT RAISE(ABORT,'test failure'); END")
        assertTrue(runCatching { repo.applyPreview(preview) }.isFailure)
        assertTrue(db.calendarScheduleDao().getAllEntries().isEmpty())
        assertNull(repo.plannerSettings())
    }
    @Test fun autumnDstKeepsLocalHourAndExactElapsedDuration() = runTest {
        val zone = ZoneId.of("Europe/Budapest")
        val settings = settings().copy(start = LocalDate.of(2026, 10, 24), weeks = 1)
        val slots = repo.preview(settings, zone).plan.slots
        assertTrue(slots.all { it.start.atZone(zone).hour == 18 && Duration.between(it.start, it.end).toMinutes() == 45L })
        assertEquals(25L, Duration.between(slots[0].start, slots[1].start).toHours())
    }
    @Test fun protectedAndCompletedDatesRemainOccupied() = runTest {
        val settings = settings()
        val preview = repo.preview(settings)
        val rows = repo.applyPreview(preview)
        val completed = rows.first().copy(status = "completed")
        db.calendarScheduleDao().upsertEntry(completed)
        val active = rows[1]
        val guarded = NativeCalendarRepository(db) { active.id }
        val next = guarded.preview(settings.copy(mode = "alternate"))
        assertTrue(next.plan.slots.none { it.date == settings.start || it.date == settings.start.plusDays(1) })
        guarded.applyPreview(next)
        assertEquals(completed, db.calendarScheduleDao().getEntry(completed.id))
        assertEquals(active, db.calendarScheduleDao().getEntry(active.id))
    }
}
