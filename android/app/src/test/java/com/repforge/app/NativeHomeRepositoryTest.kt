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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeHomeRepositoryTest {
    private lateinit var db: TrainPilotDatabase
    private lateinit var repository: NativeHomeRepository
    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val zone = ZoneId.of("Europe/Budapest")
    private val reads = ConcurrentLinkedQueue<String>()
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TrainPilotDatabase::class.java)
            .setQueryCallback({ sql, _ -> reads.add(sql) }, Executor { it.run() }).build()
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
            reads.clear()
            val result = repository.read("home-basic", "hu", now, zone)
            assertNotNull(result.target)
            val queries = reads.filter { it.trimStart().startsWith("SELECT", true) }
            assertEquals(1, queries.count { Regex("SELECT\\s+\\*\\s+FROM\\s+programs\\b", RegexOption.IGNORE_CASE).containsMatchIn(it) })
            assertTrue(queries.none { Regex("SELECT\\s+\\*\\s+FROM\\s+workout_history\\b", RegexOption.IGNORE_CASE).containsMatchIn(it) })
            assertTrue(queries.none { "workout_history_sets" in it || "workout_photos" in it })
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
