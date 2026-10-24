package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.ui.progress.NativeWeightJournalRepository
import com.repforge.app.ui.progress.WeightJournalUiState
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeWeightJournalRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var repository: NativeWeightJournalRepository
    @Before fun open() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        repository = NativeWeightJournalRepository(context, db) { 100L }
    }
    @After fun close() {
        db.close()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun row(id: String = "original", date: String = "2026-10-24T12:34:56+02:00", kg: Double = 72.0) =
        BodyWeightEntryEntity(id, date, date.take(10), kg, 20,
            JSONObject().put("id", id).put("date", date).put("kg", kg).put("unknown", JSONObject().put("keep", true)).toString())
    private suspend fun reject(block: suspend () -> Unit) {
        var failure: Exception? = null
        try { block() } catch (error: Exception) { failure = error }
        assertNotNull("The mutation must be rejected", failure)
    }
    @Test fun addKeepsTheChosenDayAndRoundsLikeTheMainApp() = runBlocking {
        repository.add(72.25, "2026-02-28")
        val value = db.bodyWeightDao().getAllWeights().single()
        assertEquals("2026-02-28", value.date); assertEquals(value.date, value.dayKey)
        assertEquals(72.3, value.kg, .0001)
        val raw = JSONObject(value.rawJson)
        assertEquals(value.id, raw.getString("id")); assertEquals(value.date, raw.getString("date"))
        assertEquals(value.kg, raw.getDouble("kg"), .0001)
    }
    @Test fun editPreservesIdentityMeasurementTimestampAndUnknownPortableFields() = runBlocking {
        val old = row(); db.bodyWeightDao().upsertWeight(old)
        repository.edit(old, 73.25)
        val edited = db.bodyWeightDao().getWeightById(old.id)!!
        assertEquals(old.id, edited.id); assertEquals(old.date, edited.date); assertEquals(old.dayKey, edited.dayKey)
        assertEquals(73.3, edited.kg, .0001); assertTrue(edited.updatedAt > old.updatedAt)
        assertTrue(JSONObject(edited.rawJson).getJSONObject("unknown").getBoolean("keep"))
        assertEquals(old.date, JSONObject(edited.rawJson).getString("date"))
    }
    @Test fun exactDeletionLeavesOtherMeasurementsOnTheSameDayUntouched() = runBlocking {
        val first = row("one"); val second = row("two", kg = 73.0)
        db.bodyWeightDao().upsertWeights(listOf(first, second))
        repository.delete(first)
        assertNull(db.bodyWeightDao().getWeightById(first.id))
        assertEquals(second, db.bodyWeightDao().getWeightById(second.id))
    }
    @Test fun staleEditCannotOverwriteAConcurrentUpdate() = runBlocking {
        val old = row(); db.bodyWeightDao().upsertWeight(old)
        repository.edit(old, 74.0)
        val latest = db.bodyWeightDao().getWeightById(old.id)
        reject { repository.edit(old, 75.0) }
        assertEquals(latest, db.bodyWeightDao().getWeightById(old.id))
    }
    @Test fun deletedEntryCannotBeRecreatedByAnOpenEditor() = runBlocking {
        val old = row(); db.bodyWeightDao().upsertWeight(old); repository.delete(old)
        reject { repository.edit(old, 75.0) }
        assertTrue(db.bodyWeightDao().getAllWeights().isEmpty())
    }
    @Test fun staleDeleteCannotRemoveANewerMeasurementVersion() = runBlocking {
        val old = row(); db.bodyWeightDao().upsertWeight(old); repository.edit(old, 74.0)
        val latest = db.bodyWeightDao().getWeightById(old.id)
        reject { repository.delete(old) }
        assertEquals(latest, db.bodyWeightDao().getWeightById(old.id))
    }
    @Test fun invalidAndNonFiniteWeightsNeverMutateStoredData() = runBlocking {
        val old = row(); db.bodyWeightDao().upsertWeight(old)
        for (kg in listOf(0.0, 29.99, 300.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            reject { repository.add(kg, "2026-10-24") }; reject { repository.edit(old, kg) }
        }
        assertEquals(listOf(old), db.bodyWeightDao().getAllWeights())
    }
    @Test fun invalidCalendarDatesDoNotInsertRows() = runBlocking {
        for (date in listOf("", "2026-02-29", "2026-13-01", "2026-10-24garbage"))
            reject { repository.add(72.0, date) }
        assertTrue(db.bodyWeightDao().getAllWeights().isEmpty())
    }
    @Test fun restoreFenceRejectsAllThreeWritePaths() = runBlocking {
        val old = row(); db.bodyWeightDao().upsertWeight(old)
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().putBoolean("blocked", true).commit()
        reject { repository.add(73.0, "2026-10-24") }
        reject { repository.edit(old, 74.0) }; reject { repository.delete(old) }
        assertEquals(listOf(old), db.bodyWeightDao().getAllWeights())
    }
    @Test fun malformedPortableJsonRollsBackAnEditWithoutDroppingUnknownData() = runBlocking {
        val old = row().copy(rawJson = "invalid-json"); db.bodyWeightDao().upsertWeight(old)
        reject { repository.edit(old, 75.0) }
        assertEquals(old, db.bodyWeightDao().getWeightById(old.id))
    }
    @Test fun updatedTimeIsMonotonicEvenWhenTheDeviceClockMovesBackwards() = runBlocking {
        val old = row().copy(updatedAt = 10000); db.bodyWeightDao().upsertWeight(old)
        repository.edit(old, 74.0)
        assertEquals(10001L, db.bodyWeightDao().getWeightById(old.id)!!.updatedAt)
    }
    @Test fun chartRangeDoesNotRemoveOlderRowsFromTheEditableHistory() {
        val rows = listOf(row("old", "2025-01-01", 71.0), row("new", "2026-10-24", 72.0))
        val state = WeightJournalUiState(entries = rows, range = "7")
        assertEquals(listOf(rows.last()), state.chartEntries(LocalDate.parse("2026-10-24")))
        assertEquals(rows, state.entries); assertEquals(71.0, state.startKg!!, .0001)
        assertEquals(rows, state.copy(range = "all").chartEntries())
    }
    @Test fun editedMeasurementRoundTripsThroughTheExistingBackupMapper() = runBlocking {
        val original = row(); db.bodyWeightDao().upsertWeight(original)
        repository.edit(original, 73.0)
        val edited = db.bodyWeightDao().getWeightById(original.id)!!
        val mapper = com.repforge.app.data.migration.LegacyWebViewMigrator(context, db,
            com.repforge.app.data.store.TrainPilotSettingsStore(context))
        val backup = mapper.exportBackupJson(false)
        assertEquals(73.0, backup.getJSONArray("weights").getJSONObject(0).getDouble("kg"), .0001)
        db.bodyWeightDao().deleteAllWeights()
        mapper.importBackupJson(backup, replaceAll = false)
        val restored = db.bodyWeightDao().getWeightById(original.id)!!
        assertEquals(edited.date, restored.date); assertEquals(edited.updatedAt, restored.updatedAt)
        assertEquals(edited.kg, restored.kg, .0001)
        assertTrue(JSONObject(restored.rawJson).getJSONObject("unknown").getBoolean("keep"))
    }
}
