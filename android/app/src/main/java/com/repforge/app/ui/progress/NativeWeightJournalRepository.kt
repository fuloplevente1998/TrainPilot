package com.repforge.app.ui.progress

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.NativeDataReplacement
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.TrainPilotDatabase
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

/** Uses the existing portable weight journal, independently of provider health measurements. */
internal class NativeWeightJournalRepository(
    private val context: Context,
    private val database: TrainPilotDatabase,
    private val now: () -> Long = System::currentTimeMillis
) {
    class ChangedEntry : IllegalStateException("The weight entry changed or was removed.")

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
