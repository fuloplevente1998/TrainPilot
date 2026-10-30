package com.repforge.app

import androidx.room.Room
import androidx.room.withTransaction
import com.repforge.app.data.db.*
import com.repforge.app.ui.home.NativeHomeRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeHomeRepositoryTest {
    private lateinit var db: TrainPilotDatabase
    private lateinit var repository: NativeHomeRepository
    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val zone = ZoneId.of("Europe/Budapest")
    // Only the measured repository.read is part of this query sample, not fixture setup.
    private val activeTrace = AtomicReference<ConcurrentLinkedQueue<String>?>(null)
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TrainPilotDatabase::class.java)
            .setQueryCallback({ sql, _ -> activeTrace.get()?.add(sql) }, Executor { it.run() }).build()
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        db.programDao().upsertPrograms(programs); db.programDao().upsertProgramDays(days)
        val source = db.programDao().getProgramWithDaysById("home-basic")!!
        db.programDao().saveProgramWithDays(source.program.copy(id = "calendar-program", nameHu = "Naptári program", builtin = false),
            listOf(source.days.first().copy(compositeId = "calendar-program:other-day", programId = "calendar-program",
                dayId = "other-day", nameHu = "Másik nap", exercisesJson = "[\"plank\",{\"id\":\"goblet-squat\"}]")))
        repository = NativeHomeRepository(db)
    }
    @After fun close() { db.close() }

    private fun plan(id: String = "home-plan") = CalendarScheduleEntity(id, "", "2026-10-24",
        "2026-10-24T10:00:00Z", "2026-10-24T10:30:00Z", "calendar-program", "Naptári program",
        "other-day", "Másik nap", "planned", 1, "{}")

    @Test fun anotherProgramsScheduleCarriesAllStartIdsAndCompletingItChangesTheRecommendation() = runBlocking {
        val row = plan(); db.calendarScheduleDao().upsertEntry(row)
        val before = repository.read("home-basic", "hu", now, zone)
        val selected = before.target!!
        assertEquals("calendar-program", before.program!!.program.id)
        assertEquals("calendar-program", selected.programId)
        assertEquals("other-day", selected.dayId); assertEquals(row.id, selected.scheduleId)
        assertEquals(2, before.exerciseCount); assertEquals(0, before.workoutCount)
        db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(0).copy(scheduleId = row.id,
            startedEpochMs = now.minusSeconds(60).toEpochMilli(), finishedEpochMs = now.toEpochMilli()))
        val after = repository.read("home-basic", "hu", now, zone)
        val next = after.target!!
        assertEquals(1, after.workoutCount); assertEquals("home-basic", next.programId)
        assertNull(next.scheduleId)
        // A history write alone removes the consumed plan from selection; no fake calendar UI replay.
        assertEquals("planned", db.calendarScheduleDao().getEntry(row.id)!!.status)
    }

    @Test fun sameCountProgramAndCalendarChangesAreNotHiddenByACountCache() = runBlocking {
        db.calendarScheduleDao().upsertEntry(plan())
        val before = repository.read("home-basic", "hu", now, zone)
        val source = before.program!!
        db.programDao().saveProgramWithDays(source.program.copy(nameHu = "Friss név"), source.days.map {
            it.copy(nameHu = "Friss nap", exercisesJson = JSONArray().put("plank").toString())
        })
        val changed = repository.read("home-basic", "hu", now, zone)
        assertEquals(before.workoutCount, changed.workoutCount)
        assertEquals("Friss név", changed.program!!.program.nameHu); assertEquals(1, changed.exerciseCount)
        db.calendarScheduleDao().upsertEntry(plan().copy(status = "skipped"))
        val active = repository.read("home-basic", "hu", now, zone)
        val fallback = active.target!!
        assertEquals("home-basic", fallback.programId); assertNull(fallback.scheduleId)
        assertEquals(before.workoutCount, active.workoutCount)
    }

    @Test fun unavailableActiveProgramDoesNotInventADefaultWorkoutButARealPlanCanStillBeOffered() = runBlocking {
        val missing = repository.read("deleted-program", "en", now, zone)
        assertNull(missing.program); assertNull(missing.target); assertEquals(0, missing.exerciseCount)
        db.calendarScheduleDao().upsertEntry(plan())
        assertEquals("calendar-program", repository.read("deleted-program", "en", now, zone).target!!.programId)
        db.calendarScheduleDao().upsertEntry(plan().copy(rawJson = "{\"cancelled\":true}"))
        assertNull(repository.read("deleted-program", "en", now, zone).target)
    }

    @Test fun snapshotsReadTheProgramCatalogOnceAndNeverLoadAllHistoryDetails() = runBlocking {
        for (count in listOf(0, 200, 1500)) {
            db.workoutHistoryDao().deleteAllWorkouts()
            db.withTransaction { repeat(count) { db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(it)) } }
            val captured = ConcurrentLinkedQueue<String>()
            check(activeTrace.compareAndSet(null, captured)) { "A previous SQL probe was not closed" }
            val result = try {
                repository.read("home-basic", "hu", now, zone)
            } finally {
                activeTrace.compareAndSet(captured, null)
            }
            assertNotNull(result.target)
            val queries = captured.filter { it.trimStart().startsWith("SELECT", ignoreCase = true) }
            val trace = buildString {
                append("history fixture=").append(count).append(" rows; scoped repository.read SQL (")
                    .append(captured.size).append(" statements):")
                captured.forEachIndexed { index, sql ->
                    append("\n").append(index + 1).append(": ").append(sql)
                }
            }
            val catalogReads = queries.filter {
                Regex("""SELECT\s+\*\s+FROM\s+programs\b""", RegexOption.IGNORE_CASE).containsMatchIn(it)
            }
            // @Relation may query program_days separately; one root catalog query is still required.
            // Do not mask a genuine duplicate by blindly changing the expected count to two.
            assertEquals(trace, 1, catalogReads.size)
            assertTrue(trace, queries.none {
                Regex("""SELECT\s+\*\s+FROM\s+workout_history\b""", RegexOption.IGNORE_CASE).containsMatchIn(it)
            })
            assertTrue(trace, queries.none {
                it.contains("workout_history_sets", ignoreCase = true) ||
                    it.contains("workout_photos", ignoreCase = true)
            })
        }
    }

    @Test fun realRoomInvalidationPublishesASameCountProgramRename() = runBlocking {
        val ready = CompletableDeferred<Unit>()
        val pending = async(Dispatchers.IO) {
            repository.invalidations().map { repository.read("home-basic", "hu", now, zone) }
                .onEach { ready.complete(Unit) }.first { it.program?.program?.nameHu == "Megváltozott" }
        }
        withTimeout(5000) { ready.await() }
        val source = db.programDao().getProgramWithDaysById("home-basic")!!
        db.programDao().saveProgramWithDays(source.program.copy(nameHu = "Megváltozott"), source.days)
        db.invalidationTracker.refreshVersionsAsync()
        val result = withTimeout(5000) { pending.await() }
        assertEquals("Megváltozott", result.program!!.program.nameHu)
        assertEquals(0, result.workoutCount)
    }
}
