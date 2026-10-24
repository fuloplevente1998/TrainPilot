package com.repforge.app.ui.journal

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.InvalidationTracker
import com.repforge.app.data.db.JournalSearchRow
import com.repforge.app.data.db.JournalWorkoutSummary
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onCompletion
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

internal data class JournalFilter(val query: String = "", val month: YearMonth? = null,
    val zone: ZoneId = ZoneId.systemDefault()) {
    val fromEpoch: Long get() = month?.atDay(1)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: Long.MIN_VALUE
    val toEpoch: Long get() = month?.plusMonths(1)?.atDay(1)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: Long.MAX_VALUE
    val terms: List<String> get() = searchKey(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
    companion object {
        fun searchKey(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
    }
}
internal data class JournalCursor(val epoch: Long, val id: String)

/** Room is still the only journal. Paging keeps raw JSON and relations out of list loading. */
internal class NativeJournalRepository(private val database: TrainPilotDatabase) {
    fun pages(filter: JournalFilter): kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<JournalWorkoutSummary>> {
        val active = AtomicReference<JournalSource?>(null)
        return Pager(PagingConfig(pageSize = 32, initialLoadSize = 32, prefetchDistance = 8, enablePlaceholders = false)) {
            source(filter).also { active.getAndSet(it)?.invalidate() }
        }.flow.onCompletion { active.getAndSet(null)?.invalidate() }
    }
    fun source(filter: JournalFilter) = JournalSource(database, filter)
}

/** Unicode-aware global search scans bounded light projections, never only the visible page. */
internal class JournalSource(private val database: TrainPilotDatabase, private val filter: JournalFilter) :
    PagingSource<JournalCursor, JournalWorkoutSummary>() {
    private val watching = AtomicBoolean(false)
    private val observer = object : InvalidationTracker.Observer("workout_history", "workout_history_exercises",
        "workout_history_sets", "workout_photos", "exercise_library") {
        override fun onInvalidated(tables: Set<String>) { invalidate() }
    }
    init { registerInvalidatedCallback { if (watching.get()) database.invalidationTracker.removeObserver(observer) } }
    // Rebuild from the beginning on mutation; do not reuse stale offsets or a count-only cache.
    override fun getRefreshKey(state: PagingState<JournalCursor, JournalWorkoutSummary>): JournalCursor? = null

    override suspend fun load(params: LoadParams<JournalCursor>): LoadResult<JournalCursor, JournalWorkoutSummary> = withContext(Dispatchers.IO) {
        try {
            if (watching.compareAndSet(false, true)) {
                database.invalidationTracker.addObserver(observer)
                if (invalid) { database.invalidationTracker.removeObserver(observer); return@withContext LoadResult.Invalid() }
            }
            if (params is LoadParams.Prepend) return@withContext LoadResult.Page(emptyList(), null, null)
            val wanted = params.loadSize.coerceIn(1, 128)
            val terms = filter.terms
            val result = ArrayList<JournalWorkoutSummary>(wanted)
            var cursor = params.key
            var end = false
            while (result.size < wanted && !end) {
                coroutineContext.ensureActive()
                if (invalid) return@withContext LoadResult.Invalid()
                val batchLimit = if (terms.isEmpty()) wanted else 128
                val dao = database.workoutHistoryDao()
                val rows = if (terms.isEmpty()) dao.scanJournalSummaries(filter.fromEpoch, filter.toEpoch,
                    cursor?.epoch, cursor?.id.orEmpty(), batchLimit).map { JournalSearchRow(it, "") }
                    else dao.scanJournal(filter.fromEpoch, filter.toEpoch, cursor?.epoch, cursor?.id.orEmpty(), batchLimit)
                if (rows.isEmpty()) { end = true; break }
                for ((index, row) in rows.withIndex()) {
                    cursor = JournalCursor(row.summary.startedEpochMs, row.summary.id)
                    val text = if (terms.isEmpty()) "" else JournalFilter.searchKey(row.searchText)
                    if (terms.all { it in text }) result.add(row.summary)
                    if (result.size == wanted) {
                        end = rows.size < batchLimit && index == rows.lastIndex
                        break
                    }
                }
                if (result.size < wanted && rows.size < batchLimit) end = true
            }
            if (invalid) LoadResult.Invalid() else LoadResult.Page(result, prevKey = null, nextKey = if (end) null else cursor)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { LoadResult.Error(error) }
    }
}
