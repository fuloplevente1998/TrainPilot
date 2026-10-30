package com.repforge.app.ui.progress

import android.util.JsonReader
import android.util.JsonToken
import com.repforge.app.data.db.CardioSetProjection
import com.repforge.app.data.db.WorkoutHistoryDao
import java.io.StringReader
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Bounded Room -> pure model adapter. Full workout graphs, photo lists and GPS points are
 * never loaded. The legacy source is read from at most one parent raw JSON at a time.
 */
internal object NativeCardioProgressAdapter {
    private const val PAGE_SIZE = NativeCardioModel.DEFAULT_PAGE_SIZE

    suspend fun load(
        dao: WorkoutHistoryDao,
        nowEpochMs: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        activityFilter: String? = null
    ): CardioModelResult = withContext(Dispatchers.IO) {
        val acc = NativeCardioModel.Accumulator(nowEpochMs, zoneId, activityFilter)
        var beforeFinished: Long? = null
        var beforeWorkoutId = ""
        var beforeSetRowId = Long.MAX_VALUE
        var cachedWorkoutId: String? = null
        var cachedSources = SourceIndex.EMPTY
        val observedNames = HashMap<String, LocalizedNames>()

        do {
            currentCoroutineContext().ensureActive()
            val page = dao.pageCardioSets(beforeFinished, beforeWorkoutId, beforeSetRowId, PAGE_SIZE)
            if (page.isEmpty()) break

            val mapped = ArrayList<CardioSetRow>(page.size)
            for (projection in page) {
                currentCoroutineContext().ensureActive()
                val row = projection.toCardioRow()
                if (row.exerciseId.isNotBlank() && NativeCardioModel.isDistance(row)) {
                    observedNames.getOrPut(row.exerciseId) { LocalizedNames() }.remember(row)
                }
                // Source affects records for the chosen activity, not the selector's
                // cross-activity counts. Skip other parent JSON reads when filtered.
                if (NativeCardioModel.isDistance(row) &&
                    (activityFilter.isNullOrEmpty() || row.exerciseId == activityFilter)) {
                    if (cachedWorkoutId != projection.workoutId) {
                        cachedWorkoutId = projection.workoutId
                        cachedSources = SourceIndex.read(
                            dao.getCardioWorkoutRawJson(projection.workoutId)
                        )
                    }
                    mapped.add(row.copy(distanceSource = cachedSources.sourceFor(projection)))
                } else {
                    mapped.add(row)
                }
            }
            acc.addPage(mapped)

            // The cursor is the LAST RAW DAO ROW, not the last accepted model row.
            // It must advance even when all 256 raw rows were discarded by the model.
            val last = page.last()
            if (beforeFinished != null && (
                last.finishedEpochMs > beforeFinished ||
                (last.finishedEpochMs == beforeFinished && last.workoutId > beforeWorkoutId) ||
                (last.finishedEpochMs == beforeFinished && last.workoutId == beforeWorkoutId &&
                    last.setRowId >= beforeSetRowId)
            )) {
                error("Cardio Room keyset did not advance")
            }
            beforeFinished = last.finishedEpochMs
            beforeWorkoutId = last.workoutId
            beforeSetRowId = last.setRowId
        } while (page.size == PAGE_SIZE)

        val built = acc.build()
        // The pure model intentionally keeps its first activity label. Historic newer
        // rows may contain only the exercise ID, whereas older rows have real names.
        // Repair labels here without altering totals, records or the canonical math.
        built.copy(activities = built.activities.map { activity ->
            val observed = observedNames[activity.exerciseId]
            if (observed == null) activity else activity.copy(
                nameHu = observed.resolve(activity.nameHu, observed.hu, activity.exerciseId),
                nameEn = observed.resolve(activity.nameEn, observed.en, activity.exerciseId),
                nameDe = observed.resolve(activity.nameDe, observed.de, activity.exerciseId),
                nameRo = observed.resolve(activity.nameRo, observed.ro, activity.exerciseId)
            )
        })
    }

    private class LocalizedNames {
        var hu: String? = null
        var en: String? = null
        var de: String? = null
        var ro: String? = null

        fun remember(row: CardioSetRow) {
            hu = hu ?: actual(row.exerciseNameHu, row.exerciseId)
            en = en ?: actual(row.exerciseNameEn, row.exerciseId)
            de = de ?: actual(row.exerciseNameDe, row.exerciseId)
            ro = ro ?: actual(row.exerciseNameRo, row.exerciseId)
        }

        fun resolve(original: String, saved: String?, id: String): String =
            actual(original, id) ?: saved ?: original

        private fun actual(name: String, id: String): String? =
            name.trim().takeIf { it.isNotEmpty() && it != id }
    }

    private fun CardioSetProjection.toCardioRow(): CardioSetRow = CardioSetRow(
        workoutId = workoutId,
        setId = setId,
        setRowId = setRowId,
        setNumber = setNumber,
        startedEpochMs = startedEpochMs,
        finishedEpochMs = finishedEpochMs.takeIf { it > 0L },
        startedIso = startedIso.takeIf { it.isNotBlank() },
        finishedIso = finishedIso.takeIf { it.isNotBlank() },
        exerciseId = exerciseId,
        exerciseNameHu = exerciseNameHu,
        exerciseNameEn = exerciseNameEn,
        exerciseNameDe = exerciseNameDe,
        exerciseNameRo = exerciseNameRo,
        measurementType = measurementType,
        libraryMeasurementType = libraryMeasurementType,
        isKnownLibraryExercise = isKnownLibraryExercise,
        repUnit = repUnit,
        done = done,
        distanceMeters = distanceMeters,
        // Decimal seconds come ONLY from the actual persisted set.reps.
        reps = reps,
        leftSeconds = leftSeconds,
        rightSeconds = rightSeconds,
        gpsRouteKey163 = gpsRouteKey163,
        gpsRouteKey193 = gpsRouteKey193,
        exerciseHasDistance = exerciseHasDistance,
        exerciseHasPositiveDistance = exerciseHasPositiveDistance
    )

    private data class SetIdKey(val order: Int, val setId: String)
    private data class SetNumberKey(val order: Int, val number: Int)

    private class SourceIndex(
        private val bySetId: Map<SetIdKey, String>,
        private val byLegacyNumber: Map<SetNumberKey, String>
    ) {
        fun sourceFor(row: CardioSetProjection): String? =
            bySetId[SetIdKey(row.exerciseOrderIndex, row.setId)]
                ?: byLegacyNumber[SetNumberKey(row.exerciseOrderIndex, row.setNumber)]

        companion object {
            val EMPTY = SourceIndex(emptyMap(), emptyMap())

            /**
             * Streaming JSON reader skips photos, GPS coordinates and unrelated fields without
             * hydrating a parent JSON object graph. Only explicit per-set distanceSource is kept.
             */
            fun read(raw: String?): SourceIndex {
                if (raw.isNullOrBlank()) return EMPTY
                val ids = HashMap<SetIdKey, String>()
                val legacyNumbers = HashMap<SetNumberKey, String>()
                val allNumbers = HashSet<SetNumberKey>()
                val ambiguousNumbers = HashSet<SetNumberKey>()
                return try {
                    JsonReader(StringReader(raw)).use { json ->
                        json.beginObject()
                        while (json.hasNext()) {
                            if (json.nextName() != "exercises" || json.peek() != JsonToken.BEGIN_ARRAY) {
                                json.skipValue()
                                continue
                            }
                            json.beginArray()
                            var order = 0
                            while (json.hasNext()) {
                                if (json.peek() == JsonToken.BEGIN_OBJECT) {
                                    json.beginObject()
                                    while (json.hasNext()) {
                                        if (json.nextName() != "sets" || json.peek() != JsonToken.BEGIN_ARRAY) {
                                            json.skipValue()
                                            continue
                                        }
                                        json.beginArray()
                                        var index = 0
                                        while (json.hasNext()) {
                                            if (json.peek() != JsonToken.BEGIN_OBJECT) {
                                                json.skipValue()
                                                index++
                                                continue
                                            }
                                            json.beginObject()
                                            var id: String? = null
                                            var explicitNumber: Int? = null
                                            var fallbackNumber: Int? = null
                                            var source: String? = null
                                            while (json.hasNext()) {
                                                when (json.nextName()) {
                                                    "setId" -> id = json.readStringOrNull()
                                                    "set" -> explicitNumber = json.readIntOrNull()
                                                    "number" -> fallbackNumber = json.readIntOrNull()
                                                    "distanceSource" -> source = json.readStringOrNull()
                                                    else -> json.skipValue()
                                                }
                                            }
                                            json.endObject()
                                            val number = explicitNumber ?: fallbackNumber ?: index + 1
                                            val numberKey = SetNumberKey(order, number)
                                            if (!allNumbers.add(numberKey)) {
                                                ambiguousNumbers.add(numberKey)
                                                legacyNumbers.remove(numberKey)
                                            }
                                            val normalized = source?.takeIf { it.isNotBlank() && it.length <= 64 }
                                            if (normalized != null) {
                                                if (!id.isNullOrBlank()) {
                                                    ids[SetIdKey(order, id)] = normalized
                                                } else if (numberKey !in ambiguousNumbers) {
                                                    legacyNumbers[numberKey] = normalized
                                                }
                                            }
                                            index++
                                        }
                                        json.endArray()
                                    }
                                    json.endObject()
                                } else {
                                    json.skipValue()
                                }
                                order++
                            }
                            json.endArray()
                        }
                        json.endObject()
                    }
                    SourceIndex(ids, legacyNumbers)
                } catch (_: Exception) {
                    EMPTY
                }
            }

            private fun JsonReader.readStringOrNull(): String? =
                if (peek() == JsonToken.STRING) nextString() else {
                    skipValue()
                    null
                }

            private fun JsonReader.readIntOrNull(): Int? =
                if (peek() == JsonToken.NUMBER || peek() == JsonToken.STRING)
                    nextString().toIntOrNull()
                else {
                    skipValue()
                    null
                }
        }
    }
}
