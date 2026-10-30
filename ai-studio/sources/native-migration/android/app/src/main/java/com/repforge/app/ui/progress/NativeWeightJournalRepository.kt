package com.repforge.app.ui.progress

import android.content.Context
import androidx.room.withTransaction
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.repforge.app.NativeDataReplacement
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WeightJournalSummary
import com.repforge.app.data.db.WeightChartPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.math.ceil

data class WeightJournalOverviewData(val summary: WeightJournalSummary,
    val chart: List<WeightChartPoint>, val chartMeasurementCount: Long, val daysPerBucket: Int)

/** Uses the existing portable weight journal, independently of provider health measurements. */
internal class NativeWeightJournalRepository(
    private val context: Context,
    private val database: TrainPilotDatabase,
    private val now: () -> Long = System::currentTimeMillis
) {
    class ChangedEntry : IllegalStateException("The weight entry changed or was removed.")

    fun pages(): Flow<PagingData<BodyWeightEntryEntity>> = Pager(PagingConfig(pageSize = 32,
        initialLoadSize = 32, prefetchDistance = 8, maxSize = 160, enablePlaceholders = false)) {
        database.bodyWeightDao().weightJournalPages()
    }.flow

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeOverview(range: String, today: LocalDate): Flow<WeightJournalOverviewData> =
        database.invalidationTracker.createFlow("body_weights", emitInitialState = true)
            .mapLatest { overview(range, today) }

    /** One read transaction: aggregates and chart cover the same dataset, even during restore. */
    suspend fun overview(range: String, today: LocalDate): WeightJournalOverviewData = database.withTransaction {
        require(range in setOf("7", "30", "90", "365", "all"))
        val dao = database.bodyWeightDao()
        val summary = dao.getWeightJournalSummary()
        val fromDay = if (range == "all") "" else today.minusDays(range.toLong()).toString()
        val bounds = dao.getWeightChartBounds(fromDay)
        val span = if (bounds.firstDay == null || bounds.lastDay == null) 0.0 else bounds.lastDay - bounds.firstDay + 1.0
        val daysPerBucket = ceil(span / 180.0).toInt().coerceAtLeast(1)
        val chart = if (bounds.firstDay == null) emptyList() else dao.getWeightChart(fromDay, bounds.firstDay, daysPerBucket)
        check(chart.sumOf { it.measurementCount } == bounds.measurementCount) { "Incomplete weight chart" }
        WeightJournalOverviewData(summary, chart, bounds.measurementCount, daysPerBucket)
    }

    suspend fun add(kg: Double, date: String) {
        val value = normalizedWeight(kg)
        val day = LocalDate.parse(date).toString()
        NativeDataReplacement.mutate(context) {
            val id = UUID.randomUUID().toString()
            val time = now()
            val raw = JSONObject().put("id", id).put("date", day).put("kg", value).put("updatedAt", time)
            database.bodyWeightDao().upsertWeight(BodyWeightEntryEntity(id, day, day, value, time, raw.toString()))
        }
    }

    suspend fun edit(expected: BodyWeightEntryEntity, kg: Double) {
        val value = normalizedWeight(kg)
        // Lock order is metadata admission -> Room transaction, including restore races.
        NativeDataReplacement.mutate(context) {
            database.withTransaction {
                val current = database.bodyWeightDao().getWeightById(expected.id) ?: throw ChangedEntry()
                if (current != expected) throw ChangedEntry()
                // Preserve original measurement date, identity and unknown portable fields.
                val time = maxOf(now(), current.updatedAt + 1)
                val raw = JSONObject(current.rawJson).put("kg", value).put("updatedAt", time)
                database.bodyWeightDao().upsertWeight(current.copy(kg = value, updatedAt = time, rawJson = raw.toString()))
            }
        }
    }

    suspend fun delete(expected: BodyWeightEntryEntity) {
        NativeDataReplacement.mutate(context) {
            database.withTransaction {
                if (database.bodyWeightDao().getWeightById(expected.id) != expected) throw ChangedEntry()
                com.repforge.app.NativeWeightTombstones18.record(database, expected, now())
                // Day-based deletion would also remove distinct measurements from the same day.
                check(database.bodyWeightDao().deleteWeightById(expected.id) == 1)
            }
        }
    }

    private fun normalizedWeight(kg: Double): Double {
        require(kg.isFinite() && kg in 30.0..300.0)
        return (kg * 10.0).roundToInt() / 10.0
    }
}
