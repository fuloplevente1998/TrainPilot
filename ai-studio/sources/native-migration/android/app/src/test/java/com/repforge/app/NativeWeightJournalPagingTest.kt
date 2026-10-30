package com.repforge.app

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.room.withTransaction
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.ui.progress.NativeWeightJournalRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeWeightJournalPagingTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var repository: NativeWeightJournalRepository
    private val queries = CopyOnWriteArrayList<String>()
    private val sources = mutableListOf<PagingSource<Int, BodyWeightEntryEntity>>()
    private val today = LocalDate.parse("2026-10-25")

    @Before fun open() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java)
            .setQueryCallback({ sql, _ -> queries.add(sql) }, Executor { it.run() }).build()
        repository = NativeWeightJournalRepository(context, db)
    }
    @After fun close() { sources.forEach { it.invalidate() }; db.close() }
    private fun row(i: Int, day: LocalDate = today.minusDays((1499 - i).toLong()), kg: Double = 70.0 + i % 100 / 10.0) =
        BodyWeightEntryEntity("weight-%04d".format(i), day.toString(), day.toString(), kg, i.toLong(),
            JSONObject().put("id", "weight-%04d".format(i)).put("date", day.toString()).put("kg", kg).toString())
    private suspend fun seed(count: Int, sameDay: Boolean = false) = db.withTransaction {
        db.bodyWeightDao().upsertWeights((0 until count).map { row(it, if (sameDay) today else today.minusDays((count - 1 - it).toLong())) })
    }
    private fun source() = db.bodyWeightDao().weightJournalPages().also { sources += it }
    private suspend fun page(source: PagingSource<Int, BodyWeightEntryEntity>, key: Int? = null,
        refresh: Boolean = key == null): PagingSource.LoadResult.Page<Int, BodyWeightEntryEntity> {
        val result = source.load(if (refresh) PagingSource.LoadParams.Refresh(key, 32, false)
            else PagingSource.LoadParams.Append(key!!, 32, false))
        assertTrue(result.toString(), result is PagingSource.LoadResult.Page)
        return result as PagingSource.LoadResult.Page<Int, BodyWeightEntryEntity>
    }

    @Test fun zeroTwoHundredAndFifteenHundredKeepListAndChartBoundedWithFullCoverage() = runBlocking {
        for (count in listOf(0, 200, 1500)) {
            db.bodyWeightDao().deleteAllWeights(); seed(count); queries.clear()
            val s = source(); val first = page(s)
            assertEquals(minOf(32, count), first.data.size)
            val overview = repository.overview("all", today)
            assertEquals(count.toLong(), overview.summary.measurementCount)
            assertEquals(count.toLong(), overview.chartMeasurementCount)
            assertEquals(count.toLong(), overview.chart.sumOf { it.measurementCount })
            assertTrue(overview.chart.size <= 180)
            val reads = queries.filter { it.trimStart().startsWith("SELECT", true) }
            assertTrue(reads.none { "workout_history" in it || "workout_photos" in it })
            assertTrue(reads.filter { "SELECT * FROM body_weights" in it && !it.contains("COUNT", true) }
                .all { it.contains("LIMIT", true) })
            s.invalidate()
        }
    }
    @Test fun equalDatesPageEveryIdentityExactlyOnce() = runBlocking {
        seed(200, sameDay = true)
        val s = source(); val ids = mutableListOf<String>(); var key: Int? = null
        do { val p = page(s, key); ids += p.data.map { it.id }; key = p.nextKey } while (key != null)
        assertEquals(200, ids.size); assertEquals(200, ids.toSet().size)
        assertEquals((0 until 200).reversed().map { "weight-%04d".format(it) }, ids)
    }
    @Test fun refreshAtAnOlderOffsetStillAllowsReturningTowardNewerRows() = runBlocking {
        seed(200)
        val s = source(); val older = page(s, 96, refresh = true)
        val previous = older.prevKey
        assertNotNull(previous)
        val back = s.load(PagingSource.LoadParams.Prepend(previous!!, 32, false))
            as PagingSource.LoadResult.Page<Int, BodyWeightEntryEntity>
        assertEquals(32, back.data.size)
        assertTrue(back.data.map { it.id }.intersect(older.data.map { it.id }.toSet()).isEmpty())
    }
    @Test fun extremaAndInitialWeightRemainGlobalWhenTheyAreOutsideTheLoadedPage() = runBlocking {
        seed(1500)
        db.bodyWeightDao().upsertWeight(row(0, kg = 140.0)); db.bodyWeightDao().upsertWeight(row(700, kg = 35.0))
        val visible = page(source()).data
        assertFalse(visible.any { it.id in setOf("weight-0000", "weight-0700") })
        val summary = repository.overview("7", today).summary
        assertEquals(1500L, summary.measurementCount); assertEquals(140.0, summary.firstKg!!, .0001)
        assertEquals(35.0, summary.minimumKg!!, .0001); assertEquals(140.0, summary.maximumKg!!, .0001)
        assertEquals(visible.first().kg, summary.currentKg!!, .0001)
    }
    @Test fun longHistoryAggregationIncludesOldestAndNewestMeasurements() = runBlocking {
        db.withTransaction { db.bodyWeightDao().upsertWeights((0 until 1500).map { row(it, today.minusDays((1499 - it) * 5L)) }) }
        val data = repository.overview("all", today)
        assertTrue(data.daysPerBucket > 1); assertTrue(data.chart.size <= 180)
        assertEquals(1500L, data.chart.sumOf { it.measurementCount })
        assertEquals(70.0, data.chart.first().minimumKg, .0001)
        assertEquals(79.9, data.chart.last().maximumKg, .0001)
    }
    @Test fun manyMeasurementsOnOneDayBecomeOneExplicitAverageWithoutDroppingExtrema() = runBlocking {
        seed(200, sameDay = true)
        val data = repository.overview("all", today); val point = data.chart.single()
        assertEquals(200L, point.measurementCount); assertEquals(74.95, point.averageKg, .0001)
        assertEquals(70.0, point.minimumKg, .0001); assertEquals(79.9, point.maximumKg, .0001)
        assertEquals(1, data.daysPerBucket)
    }
    @Test fun allRangesUseTheExistingInclusiveCutoffAndKeepGlobalStatistics() = runBlocking {
        seed(1500)
        for (range in listOf("7", "30", "90", "365")) {
            val data = repository.overview(range, today)
            assertEquals(range.toLong() + 1L, data.chartMeasurementCount)
            assertEquals(1500L, data.summary.measurementCount)
        }
    }
    @Test fun emptySelectedRangeDoesNotEraseGlobalCurrentAndInitialValues() = runBlocking {
        db.bodyWeightDao().upsertWeight(row(0, today.minusDays(500), 85.0))
        val data = repository.overview("7", today)
        assertTrue(data.chart.isEmpty()); assertEquals(0L, data.chartMeasurementCount)
        assertEquals(1L, data.summary.measurementCount); assertEquals(85.0, data.summary.currentKg!!, .0001)
    }
    @Test fun invalidImportedDatesStayEditableAndAreNotFabricatedIntoChartPoints() = runBlocking {
        db.bodyWeightDao().upsertWeight(row(0, today, 72.0))
        db.bodyWeightDao().upsertWeight(row(1, today, 73.0).copy(date = "unknown-date", dayKey = "unknown-date"))
        val data = repository.overview("all", today)
        assertEquals(2L, data.summary.measurementCount); assertEquals(1L, data.chartMeasurementCount)
        assertEquals(2, page(source()).data.size)
    }
    @Test fun editingAMiddleValueInvalidatesChartEvenWhenCountAndSummaryDoNotChange() = runBlocking {
        val rows = listOf(row(0, today, 70.0), row(1, today, 75.0), row(2, today, 80.0))
        db.bodyWeightDao().upsertWeights(rows)
        val initial = repository.overview("all", today)
        coroutineScope {
            val ready = CompletableDeferred<Unit>(); val changed = CompletableDeferred<Unit>()
            val job = launch(Dispatchers.IO) {
                repository.observeOverview("all", today).collect { value ->
                    if (value.chart.single().averageKg == 75.0) ready.complete(Unit)
                    if (value.chart.single().averageKg < 75.0) { assertEquals(initial.summary, value.summary); changed.complete(Unit) }
                }
            }
            try { withTimeout(5000) { ready.await() }; repository.edit(rows[1], 72.0); withTimeout(5000) { changed.await() } }
            finally { job.cancelAndJoin() }
        }
    }
    @Test fun sameCountReplacementInvalidatesThePagingGeneration() = runBlocking {
        seed(200)
        val s = source(); val first = page(s)
        val invalidated = CompletableDeferred<Unit>(); s.registerInvalidatedCallback { invalidated.complete(Unit) }
        db.withTransaction {
            db.bodyWeightDao().deleteAllWeights()
            db.bodyWeightDao().upsertWeights((0 until 200).map { row(it).copy(id = "replacement-$it") })
        }
        withTimeout(5000) { invalidated.await() }
        val fresh = page(source()).data
        assertTrue(fresh.map { it.id }.intersect(first.data.map { it.id }.toSet()).isEmpty())
        assertEquals(200L, repository.overview("all", today).summary.measurementCount)
    }
    @Test fun deletingLatestMeasurementRefreshesCurrentWeightAndRemainingPage() = runBlocking {
        seed(200)
        val s = source(); val first = page(s); val newest = first.data.first()
        val invalidated = CompletableDeferred<Unit>(); s.registerInvalidatedCallback { invalidated.complete(Unit) }
        repository.delete(newest); withTimeout(5000) { invalidated.await() }
        assertEquals(first.data[1].kg, repository.overview("all", today).summary.currentKg!!, .0001)
        assertFalse(page(source()).data.any { it.id == newest.id })
        assertEquals(199L, repository.overview("all", today).summary.measurementCount)
    }
    @Test fun invalidRangeIsRejectedWithoutChangingStoredData() = runBlocking {
        seed(200)
        var failure: Exception? = null
        try { repository.overview("invalid", today) } catch (error: Exception) { failure = error }
        assertNotNull(failure); assertEquals(200L, db.bodyWeightDao().getWeightJournalSummary().measurementCount)
    }
}
