package com.repforge.app

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.room.withTransaction
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryExerciseEntity
import com.repforge.app.data.db.JournalWorkoutSummary
import com.repforge.app.ui.journal.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeJournalPagingTest {
    private lateinit var db: TrainPilotDatabase
    private val queries = CopyOnWriteArrayList<String>()
    private val sources = ArrayList<JournalSource>()
    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TrainPilotDatabase::class.java)
            .setQueryCallback({ sql, _ -> queries.add(sql) }, Executor { it.run() }).build()
    }
    @After fun close() { sources.forEach { it.invalidate() }; db.close() }
    private fun source(filter: JournalFilter = JournalFilter()) = JournalSource(db, filter).also { sources.add(it) }
    private suspend fun seed(count: Int) = db.withTransaction {
        repeat(count) { db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(it)) }
    }
    private suspend fun page(source: JournalSource, key: JournalCursor? = null): PagingSource.LoadResult.Page<JournalCursor, JournalWorkoutSummary> {
        val result = source.load(if (key == null) PagingSource.LoadParams.Refresh(null, 32, false)
            else PagingSource.LoadParams.Append(key, 32, false))
        assertTrue(result.toString(), result is PagingSource.LoadResult.Page)
        return result as PagingSource.LoadResult.Page<JournalCursor, JournalWorkoutSummary>
    }
    @Test fun zeroTwoHundredAndFifteenHundredLoadBoundedListsWithoutRawOrRelations() = runBlocking {
        for (count in listOf(0, 200, 1500)) {
            db.workoutHistoryDao().deleteAllWorkouts()
            seed(count)
            queries.clear()
            val source = source()
            val first = page(source)
            assertEquals(minOf(32, count), first.data.size)
            if (first.nextKey != null) {
                val second = page(source, first.nextKey)
                assertTrue(first.data.map { it.id }.intersect(second.data.map { it.id }.toSet()).isEmpty())
            }
            val reads = queries.filter { it.trimStart().startsWith("SELECT", true) }
            assertTrue(reads.none { "rawJson" in it || "SELECT * FROM workout_history_sets" in it || "SELECT * FROM workout_photos" in it })
            assertTrue(reads.filter { "AS searchText" in it }.all { "LIMIT" in it })
            source.invalidate()
        }
    }
    @Test fun unicodeGlobalSearchFindsOldRowsOutsideFirstPageAndSqlScanBatch() = runBlocking {
        seed(1500)
        db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(0).copy(dayName = "ŐSZI ÁLLÓKÉP"))
        val result = page(source(JournalFilter(query = "oszi allokep")))
        assertEquals(listOf("w-0000"), result.data.map { it.id })
        assertNull(result.nextKey)
    }
    @Test fun searchContinuesAfterTheLastMatchedRowWithoutSkippingScanRemainder() = runBlocking {
        db.withTransaction { repeat(200) { db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(it).copy(dayName = if (it % 2 == 0) "plank" else "other")) } }
        val source = source(JournalFilter(query = "plank"))
        val ids = ArrayList<String>()
        var cursor: JournalCursor? = null
        do {
            val loaded = page(source, cursor)
            ids.addAll(loaded.data.map { it.id })
            cursor = loaded.nextKey
        } while (cursor != null)
        assertEquals((0 until 200).reversed().filter { it % 2 == 0 }.map { "w-%04d".format(it) }, ids)
    }
    @Test fun exerciseNamesParticipateInGlobalSearchWithoutFetchingSetRelations() = runBlocking {
        seed(200)
        db.workoutHistoryDao().insertExercise(WorkoutHistoryExerciseEntity(workoutHistoryId = "w-0001", exerciseId = "plank",
            nameHu = "Oldalsó plank", nameEn = "Side plank", loadType = "bodyweight", repUnit = "mp", measurementType = "time",
            orderIndex = 0, actualRir = null, setRir229 = false))
        assertEquals(listOf("w-0001"), page(source(JournalFilter(query = "oldalso plank"))).data.map { it.id })
        assertEquals(listOf("w-0001"), page(source(JournalFilter(query = "side plank"))).data.map { it.id })
        assertTrue(page(source(JournalFilter(query = "%_"))).data.isEmpty())
    }
    @Test fun monthUsesLocalMidnightsWithDstAndExclusiveNextMonthBoundary() = runBlocking {
        val filter = JournalFilter(month = YearMonth.of(2026, 3), zone = ZoneId.of("Europe/Budapest"))
        assertEquals(31L * 24 * 3600000 - 3600000, filter.toEpoch - filter.fromEpoch)
        db.withTransaction {
            listOf(filter.fromEpoch - 1, filter.fromEpoch, filter.toEpoch - 1, filter.toEpoch).forEachIndexed { i, epoch ->
                db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(i).copy(startedEpochMs = epoch))
            }
        }
        assertEquals(listOf("w-0002", "w-0001"), page(source(filter)).data.map { it.id })
    }
    @Test fun editingWithoutChangingCountInvalidatesTheVisibleSource() = runBlocking {
        seed(40)
        val source = source()
        page(source)
        val invalidated = CompletableDeferred<Unit>()
        source.registerInvalidatedCallback { invalidated.complete(Unit) }
        db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(39).copy(dayName = "Changed"))
        withTimeout(5000) { invalidated.await() }
        assertTrue(source.invalid)
        assertEquals(40, db.workoutHistoryDao().getWorkoutCount())
        assertEquals("Changed", page(source()).data.first().dayName)
    }
    @Test fun deletedDetailCannotRemainCachedAndInvalidSourcesNeverPublishAPage() = runBlocking {
        seed(1)
        val source = source()
        page(source)
        source.invalidate()
        assertTrue(source.load(PagingSource.LoadParams.Refresh(null, 32, false)) is PagingSource.LoadResult.Invalid)
        db.workoutHistoryDao().deleteWorkoutByIdOrKey("w-0000")
        assertNull(db.workoutHistoryDao().getWorkoutWithDetails("w-0000"))
        assertTrue(page(source()).data.isEmpty())
    }
    @Test fun tiedEpochsArePagedInStableIdOrder() = runBlocking {
        db.withTransaction { repeat(80) { db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(it).copy(startedEpochMs = 1)) } }
        val source = source()
        val first = page(source); val second = page(source, first.nextKey); val third = page(source, second.nextKey)
        assertEquals(80, (first.data + second.data + third.data).map { it.id }.toSet().size)
        assertNull(third.nextKey)
    }
}
