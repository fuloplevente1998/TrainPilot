package com.repforge.app.ui.progress

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.floor

/**
 * Flat, immutable row projection for native cardio/distance statistics.
 * Designed for keyset-paged Room queries (`ORDER BY startedEpochMs DESC, workoutId DESC, setRowId DESC`)
 * without loading full `WorkoutHistoryWithDetails` graphs, JSON blobs, or GPS point arrays.
 */
data class CardioSetRow(
    val workoutId: String,
    val setId: String = "",
    val setRowId: Long = 0L,
    val setNumber: Int = 1,
    val startedEpochMs: Long = 0L,
    val finishedEpochMs: Long? = null,
    val startedIso: String? = null,
    val finishedIso: String? = null,
    val exerciseId: String,
    val exerciseNameHu: String = "",
    val exerciseNameEn: String = "",
    val exerciseNameDe: String = "",
    val exerciseNameRo: String = "",
    val measurementType: String? = null,
    val libraryMeasurementType: String? = null,
    val isKnownLibraryExercise: Boolean = false,
    val repUnit: String = "mp",
    val done: Boolean = true,
    val distanceMeters: Double? = null,
    val durationSeconds: Double? = null,
    val reps: String = "",
    val leftSeconds: Int? = null,
    val rightSeconds: Int? = null,
    val distanceSource: String? = null,
    val gpsRouteKey163: String? = null,
    val gpsRouteKey193: String? = null,
    // Main classifies the whole exercise before iterating its sets, including undone siblings.
    // A paged adapter must project these two exercise-wide flags when siblings are on other pages.
    val exerciseHasDistance: Boolean? = null,
    val exerciseHasPositiveDistance: Boolean? = null
)

data class CardioActivityOption(
    val exerciseId: String, val nameHu: String, val nameEn: String,
    val nameDe: String, val nameRo: String, val setCount: Int, val workoutCount: Int
)

data class CardioTotals(
    val meters: Double,
    val seconds: Double,
    val pace: Double?,
    val timeOnlySeconds: Double,
    val pacedMeters: Double,
    val pacedSeconds: Double
) {
    val distanceMeters: Double get() = meters
    val durationSeconds: Double get() = seconds
    val paceSecondsPerKm: Double? get() = pace
    val timeOnlyDurationSeconds: Double get() = timeOnlySeconds
}

data class CardioWeekStats(
    val weekStartDate: LocalDate,
    val weekStartIso: String,
    val weekStartEpochMs: Long,
    val weekEndExclusiveEpochMs: Long,
    val weeksAgo: Int,
    val meters: Double,
    val seconds: Double,
    val pace: Double?,
    val timeOnlySeconds: Double,
    val pacedMeters: Double,
    val pacedSeconds: Double,
    val setCount: Int,
    val workoutCount: Int
) {
    val start: LocalDate get() = weekStartDate
    val distanceMeters: Double get() = meters
    val durationSeconds: Double get() = seconds
    val paceSecondsPerKm: Double? get() = pace
    val timeOnlyDurationSeconds: Double get() = timeOnlySeconds
}

data class CardioRecordEntry(
    val exerciseId: String,
    val workoutId: String,
    val setId: String,
    val setRowId: Long,
    val setNumber: Int,
    val epochMs: Long,
    val meters: Double,
    val roundedDistanceMeters: Int,
    val seconds: Double,
    val pace: Double?,
    val source: String,
    val distanceSource: String?,
    val gpsRouteKey163: String?,
    val gpsRouteKey193: String?
) {
    val id: String get() = exerciseId
    val dateEpochMs: Long get() = epochMs
    val distanceMeters: Double get() = meters
    val durationSeconds: Double get() = seconds
    val paceSecondsPerKm: Double? get() = pace
    val measurementSource: String get() = source
}

data class CardioDistanceRecordGroup(
    val exerciseId: String,
    val roundedDistanceMeters: Int,
    val count: Int,
    val best: CardioRecordEntry,
    val second: CardioRecordEntry?
) {
    val id: String get() = exerciseId
    val meters: Int get() = roundedDistanceMeters
    val previous: CardioRecordEntry? get() = second
}

data class CardioModelResult(
    val selectedExerciseId: String?,
    val totals: CardioTotals,
    val currentWeek: CardioWeekStats,
    val previousWeek: CardioWeekStats,
    val weeks: List<CardioWeekStats>,
    val records: List<CardioDistanceRecordGroup>,
    val activities: List<CardioActivityOption>,
    val totalSetCount: Int,
    val totalWorkoutCount: Int
) {
    val week: CardioWeekStats get() = currentWeek
    val distanceRecords: List<CardioDistanceRecordGroup> get() = records
    val totalDistanceMeters: Double get() = totals.meters
    val totalDurationSeconds: Double get() = totals.seconds
    val pacedDistanceMeters: Double get() = totals.pacedMeters
    val pacedDurationSeconds: Double get() = totals.pacedSeconds
    val timeOnlyDurationSeconds: Double get() = totals.timeOnlySeconds
    val overallPaceSecondsPerKm: Double? get() = totals.pace

    fun weekByOffset(weeksAgo: Int): CardioWeekStats? = weeks.firstOrNull { it.weeksAgo == weeksAgo }
}

/** Output of `tp107DistanceStats(id, entries)` for a single distance activity. */
data class CardioDistanceExerciseStats(
    val kind: String = "distance",
    val maxWeight: Double = 0.0,
    val e1rm: Double = 0.0,
    val maxVolume: Double = 0.0,
    val count: Int,
    val sessions: Int,
    val best: CardioRecordEntry,
    val latest: CardioRecordEntry,
    val cardio: CardioModelResult
)

/**
 * Pure JVM/Kotlin cardio statistics calculation aligned with `www/app.js`
 * (`tp106IsDistance`, `tp107WeekStart`, `tp107Pace`, `tp107CardioModel`, `tp107DistanceStats`).
 */
object NativeCardioModel {
    const val ALL_ACTIVITIES: String = ""
    const val DEFAULT_PAGE_SIZE: Int = 256
    const val WEEKS_WINDOW: Int = 8
    const val MAX_DISTANCE_METERS: Double = 2_000_000.0
    const val DEFAULT_SOURCE: String = "manual"

    val TP106_DISTANCE_IDS: Set<String> = setOf(
        "running", "jogging", "inline-skating", "cycling", "walking", "hiking"
    )

    @JvmStatic
    fun tp106HasDistance(distanceMeters: Double?): Boolean =
        distanceMeters != null && distanceMeters.isFinite() && distanceMeters >= 0.0

    /**
     * Equivalent of `tp106IsDistance(e)` in `www/app.js`:
     * - Built-in `TP106_DISTANCE_IDS` are always distance activities.
     * - Library definition with `measurementType == "distance"` is always distance.
     * - Known non-distance library exercises (e.g. lifts/timed exercises with stale flags or 0m)
     *   require positive finite `distanceMeters > 0.0`.
     * - Otherwise requires `measurementType == "distance"` or `tp106HasDistance(distanceMeters)`.
     */
    @JvmStatic
    fun tp106IsDistance(
        exerciseId: String?,
        measurementType: String? = null,
        distanceMeters: Double? = null,
        libraryMeasurementType: String? = null,
        isKnownLibraryExercise: Boolean = libraryMeasurementType != null,
        exerciseHasDistance: Boolean? = null,
        exerciseHasPositiveDistance: Boolean? = null
    ): Boolean {
        val id = exerciseId.orEmpty()
        if (id.isBlank()) return false
        if (id in TP106_DISTANCE_IDS) return true
        val isDistMeasure = measurementType == "distance"
        val hasPositive = exerciseHasPositiveDistance ?: (tp106HasDistance(distanceMeters) && distanceMeters!! > 0.0)
        val hasDist = hasPositive || (exerciseHasDistance ?: tp106HasDistance(distanceMeters))
        if (!isDistMeasure && !hasDist && !id.startsWith("custom-")) return false
        if (libraryMeasurementType == "distance") return true
        if (isKnownLibraryExercise && !hasPositive) return false
        return isDistMeasure || hasDist
    }

    @JvmStatic
    fun tp106IsDistance(row: CardioSetRow): Boolean = tp106IsDistance(
        exerciseId = row.exerciseId,
        measurementType = row.measurementType,
        distanceMeters = row.distanceMeters,
        libraryMeasurementType = row.libraryMeasurementType,
        isKnownLibraryExercise = row.isKnownLibraryExercise || row.libraryMeasurementType != null,
        exerciseHasDistance = row.exerciseHasDistance,
        exerciseHasPositiveDistance = row.exerciseHasPositiveDistance
    )

    @JvmStatic
    fun isDistance(row: CardioSetRow): Boolean = tp106IsDistance(row)

    /**
     * Equivalent of `tp107WeekStart`: local Monday calendar date in [zoneId].
     * Uses calendar days (`DayOfWeek.MONDAY`), never fixed 168-hour millisecond offsets.
     */
    @JvmStatic
    fun tp107WeekStart(epochMs: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    @JvmStatic
    fun weekStartDate(epochMs: Long, zoneId: ZoneId): LocalDate = tp107WeekStart(epochMs, zoneId)

    @JvmStatic
    fun weekStartEpochMs(epochMs: Long, zoneId: ZoneId): Long =
        tp107WeekStart(epochMs, zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()

    /**
     * Equivalent of `tp107Pace(seconds, meters)`: `seconds / (meters / 1000.0)` when both are
     * finite and strictly positive, otherwise `null`.
     */
    @JvmStatic
    fun tp107Pace(seconds: Double?, meters: Double?): Double? {
        if (seconds == null || meters == null) return null
        if (!seconds.isFinite() || !meters.isFinite()) return null
        if (seconds <= 0.0 || meters <= 0.0) return null
        val pace = seconds / (meters / 1000.0)
        return if (pace.isFinite() && pace > 0.0) pace else null
    }

    @JvmStatic
    fun paceSecondsPerKm(durationSeconds: Double?, distanceMeters: Double?): Double? =
        tp107Pace(durationSeconds, distanceMeters)

    /** Equivalent of `tp107FormatPace(value)` in `www/app.js`. */
    @JvmStatic
    fun tp107FormatPace(value: Double?): String {
        if (value == null || !value.isFinite() || value <= 0.0) return "—"
        val totalSeconds = floor(value + 0.5).toLong()
        val minutes = totalSeconds / 60L
        val remSeconds = (totalSeconds % 60L).toString().padStart(2, '0')
        return "$minutes:$remSeconds /km"
    }

    /** JS `Math.round(meters)` equivalent for positive meter values. */
    @JvmStatic
    fun roundDistanceMeters(meters: Double?): Int {
        if (meters == null || !meters.isFinite() || meters <= 0.0) return 0
        val rounded = floor(meters + 0.5).toLong()
        return if (rounded in 1L..Int.MAX_VALUE.toLong()) rounded.toInt() else 0
    }

    @JvmStatic
    fun resolveSource(row: CardioSetRow): String =
        row.distanceSource?.takeIf { it.isNotEmpty() } ?: DEFAULT_SOURCE

    @JvmStatic
    fun tp107CardioModel(
        rows: Iterable<CardioSetRow>,
        nowEpochMs: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        activity: String? = null
    ): CardioModelResult = compute(rows, nowEpochMs, zoneId, activity)

    @JvmStatic
    fun compute(
        rows: Iterable<CardioSetRow>,
        nowEpochMs: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        activityFilter: String? = null
    ): CardioModelResult = Accumulator(nowEpochMs, zoneId, activityFilter).also { acc ->
        for (row in rows) acc.add(row)
    }.build()

    @JvmStatic
    fun compute(
        rows: Sequence<CardioSetRow>,
        nowEpochMs: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        activityFilter: String? = null
    ): CardioModelResult = Accumulator(nowEpochMs, zoneId, activityFilter).also { acc ->
        for (row in rows) acc.add(row)
    }.build()

    /** Equivalent of `tp107DistanceStats(id, entries)` in `www/app.js`. */
    @JvmStatic
    fun tp107DistanceStats(
        exerciseId: String,
        rows: Iterable<CardioSetRow>,
        nowEpochMs: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): CardioDistanceExerciseStats? {
        val targetId = exerciseId
        if (targetId.isEmpty()) return null
        val acc = Accumulator(nowEpochMs = nowEpochMs, zoneId = zoneId, activityFilter = targetId)
        for (row in rows) acc.add(row)
        return acc.buildDistanceExerciseStats()
    }

    @JvmStatic
    fun distanceStats(
        rows: Iterable<CardioSetRow>,
        nowEpochMs: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        activityFilter: String? = null
    ): List<CardioDistanceRecordGroup> = compute(rows, nowEpochMs, zoneId, activityFilter).records

    /**
     * Streaming/paged accumulator supporting Room keyset pagination (`DEFAULT_PAGE_SIZE = 256`)
     * without loading the full workout history graph into memory at once.
     */
    class Accumulator(
        val nowEpochMs: Long,
        val zoneId: ZoneId = ZoneId.systemDefault(),
        activityFilter: String? = null
    ) {
        val normalizedFilter: String? = activityFilter?.takeIf { it.isNotEmpty() }

        private val currentWeekMonday: LocalDate = tp107WeekStart(nowEpochMs, zoneId)
        private val weekAccumulators: Array<WeekAccumulator> = Array(WEEKS_WINDOW) { idx ->
            val weeksAgo = (WEEKS_WINDOW - 1) - idx
            val monday = currentWeekMonday.minusWeeks(weeksAgo.toLong())
            WeekAccumulator(
                weekStartDate = monday,
                weekStartIso = monday.toString(),
                weekStartEpochMs = monday.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                weekEndExclusiveEpochMs = monday.plusWeeks(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
                weeksAgo = weeksAgo
            )
        }

        private var totalMeters = 0.0
        private var totalSeconds = 0.0
        private var pacedMeters = 0.0
        private var pacedSeconds = 0.0
        private var timeOnlySeconds = 0.0
        private var totalSetCount = 0
        private val workoutIds = HashSet<String>()
        private val activityBuckets = LinkedHashMap<String, ActivityAccumulator>()
        private val recordBuckets = HashMap<RecordBucketKey, RecordBucketAccumulator>()
        private var latestEntry: CardioRecordEntry? = null
        private var sequenceCounter: Long = 0L

        /** Feed any bounded page; the DAO/adapter owns its cursor, including rejected raw rows. */
        fun addPage(page: Iterable<CardioSetRow>): Accumulator {
            for (row in page) add(row)
            return this
        }

        fun add(row: CardioSetRow): Accumulator {
            if (!row.done) return this
            val exerciseId = row.exerciseId
            if (exerciseId.isBlank()) return this

            val effectiveEpochMs = resolveWorkoutEpochMs(row, zoneId) ?: return this
            if (effectiveEpochMs > nowEpochMs) return this
            if (!tp106IsDistance(row)) return this

            val seconds = sanitizeSeconds(row)
            val meters = sanitizeMeters(row.distanceMeters)
            if (seconds <= 0.0 && meters <= 0.0) return this

            val workoutId = row.workoutId.ifEmpty { row.startedIso.orEmpty() }
            if (workoutId.isBlank()) return this

            val actAcc = activityBuckets.getOrPut(exerciseId) {
                ActivityAccumulator(
                    exerciseId = exerciseId,
                    nameHu = row.exerciseNameHu.trim().ifEmpty { exerciseId },
                    nameEn = row.exerciseNameEn.trim().ifEmpty { exerciseId },
                    nameDe = row.exerciseNameDe.trim().ifEmpty { exerciseId },
                    nameRo = row.exerciseNameRo.trim().ifEmpty { exerciseId }
                )
            }
            actAcc.updateNamesIfBlank(row)
            actAcc.setCount += 1
            actAcc.workoutIds.add(workoutId)

            if (normalizedFilter != null && exerciseId != normalizedFilter) return this

            val seq = sequenceCounter++
            val pace = tp107Pace(seconds, meters)
            val roundedMeters = roundDistanceMeters(meters)
            val entry = CardioRecordEntry(
                exerciseId = exerciseId,
                workoutId = workoutId,
                setId = row.setId,
                setRowId = row.setRowId,
                setNumber = row.setNumber,
                epochMs = effectiveEpochMs,
                meters = meters,
                roundedDistanceMeters = roundedMeters,
                seconds = seconds,
                pace = pace,
                source = resolveSource(row),
                distanceSource = row.distanceSource?.takeIf { it.isNotEmpty() },
                gpsRouteKey163 = row.gpsRouteKey163?.takeIf { it.isNotEmpty() },
                gpsRouteKey193 = row.gpsRouteKey193?.takeIf { it.isNotEmpty() }
            )

            val currentLatest = latestEntry
            if (currentLatest == null || entry.epochMs > currentLatest.epochMs) {
                latestEntry = entry
            }

            totalSetCount += 1
            workoutIds.add(workoutId)
            totalMeters += meters
            totalSeconds += seconds
            if (pace != null) {
                pacedMeters += meters
                pacedSeconds += seconds
            } else if (meters <= 0.0) {
                timeOnlySeconds += seconds
            }

            for (weekAcc in weekAccumulators) {
                if (effectiveEpochMs >= weekAcc.weekStartEpochMs &&
                    effectiveEpochMs < weekAcc.weekEndExclusiveEpochMs
                ) {
                    weekAcc.record(workoutId, meters, seconds, pace)
                    break
                }
            }

            if (pace != null && roundedMeters > 0) {
                val key = RecordBucketKey(exerciseId, roundedMeters)
                recordBuckets.getOrPut(key) { RecordBucketAccumulator(exerciseId, roundedMeters) }
                    .addCandidate(entry, seq)
            }

            return this
        }

        fun build(): CardioModelResult {
            val builtWeeks = weekAccumulators.map { it.build() }
            val builtRecords = recordBuckets.values.mapNotNull { it.build() }
                .sortedWith(compareBy<CardioDistanceRecordGroup> { it.exerciseId }.thenBy { it.roundedDistanceMeters })
            val totals = CardioTotals(
                meters = totalMeters,
                seconds = totalSeconds,
                pace = tp107Pace(pacedSeconds, pacedMeters),
                timeOnlySeconds = timeOnlySeconds,
                pacedMeters = pacedMeters,
                pacedSeconds = pacedSeconds
            )
            return CardioModelResult(
                selectedExerciseId = normalizedFilter,
                totals = totals,
                currentWeek = builtWeeks[WEEKS_WINDOW - 1],
                previousWeek = builtWeeks[WEEKS_WINDOW - 2],
                weeks = builtWeeks,
                records = builtRecords,
                activities = activityBuckets.values.map { it.build() },
                totalSetCount = totalSetCount,
                totalWorkoutCount = workoutIds.size
            )
        }

        internal fun buildDistanceExerciseStats(): CardioDistanceExerciseStats? {
            if (totalSetCount == 0) return null
            val model = build()
            val best = model.records.firstOrNull()?.best ?: latestEntry ?: return null
            val latest = latestEntry ?: best
            return CardioDistanceExerciseStats(
                count = totalSetCount,
                sessions = workoutIds.size,
                best = best,
                latest = latest,
                cardio = model
            )
        }
    }

    private fun sanitizeMeters(distanceMeters: Double?): Double {
        if (distanceMeters == null || !distanceMeters.isFinite()) return 0.0
        return if (distanceMeters > 0.0 && distanceMeters <= MAX_DISTANCE_METERS) distanceMeters else 0.0
    }

    private fun sanitizeSeconds(row: CardioSetRow): Double {
        // Optional durationSeconds is a decoded numeric set.reps projection, not a second time source.
        row.durationSeconds?.let { return if (it.isFinite() && it > 0.0) it else 0.0 }
        // Actual main values come from Number(set.reps). Never infer time from bilateral fields.
        // Canonical native records use decimal seconds; comma/Java-only hex-float forms are invalid.
        val text = row.reps.trim()
        if (!DECIMAL_SECONDS.matches(text)) return 0.0
        return text.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    }

    private val DECIMAL_SECONDS = Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")

    internal fun resolveWorkoutEpochMs(row: CardioSetRow, zoneId: ZoneId): Long? {
        // A present but malformed finish must not silently fall back to a valid start.
        row.finishedIso?.trim()?.takeIf { it.isNotEmpty() }?.let { return parseTime(it, zoneId) }
        row.finishedEpochMs?.let { return it.takeIf { value -> value > 0L } }
        row.startedIso?.trim()?.takeIf { it.isNotEmpty() }?.let { return parseTime(it, zoneId) }
        return row.startedEpochMs.takeIf { it > 0L }
    }

    private fun parseTime(iso: String, zoneId: ZoneId): Long? {
        val instant = runCatching { Instant.parse(iso) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()
            // ECMAScript date-only ISO strings mean UTC, even for a different display zone.
            ?: runCatching { LocalDate.parse(iso).atStartOfDay(ZoneOffset.UTC).toInstant() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(iso).atZone(zoneId).toInstant() }.getOrNull()
        return instant?.let { runCatching { it.toEpochMilli() }.getOrNull() }?.takeIf { it > 0L }
    }

    private data class RecordBucketKey(val exerciseId: String, val roundedDistanceMeters: Int)

    private class RankedCandidate(val entry: CardioRecordEntry, val sequence: Long)

    private class RecordBucketAccumulator(val exerciseId: String, val roundedDistanceMeters: Int) {
        var count = 0
        var best: RankedCandidate? = null
        var second: RankedCandidate? = null

        /**
         * Matches `xs.sort((a, b) => a.seconds - b.seconds || a.date - b.date)` in `www/app.js`,
         * with stable encounter order (`sequence`) when both `seconds` and `epochMs` are equal.
         */
        private fun isBetter(a: RankedCandidate, b: RankedCandidate): Boolean {
            val cmpSec = a.entry.seconds.compareTo(b.entry.seconds)
            if (cmpSec != 0) return cmpSec < 0
            val cmpDate = a.entry.epochMs.compareTo(b.entry.epochMs)
            if (cmpDate != 0) return cmpDate < 0
            return a.sequence < b.sequence
        }

        fun addCandidate(entry: CardioRecordEntry, sequence: Long) {
            count += 1
            val cand = RankedCandidate(entry, sequence)
            val currentBest = best
            if (currentBest == null || isBetter(cand, currentBest)) {
                second = currentBest
                best = cand
                return
            }
            val currentSecond = second
            if (currentSecond == null || isBetter(cand, currentSecond)) {
                second = cand
            }
        }

        fun build(): CardioDistanceRecordGroup? = best?.let {
            CardioDistanceRecordGroup(exerciseId, roundedDistanceMeters, count, it.entry, second?.entry)
        }
    }

    private class WeekAccumulator(
        val weekStartDate: LocalDate, val weekStartIso: String,
        val weekStartEpochMs: Long, val weekEndExclusiveEpochMs: Long, val weeksAgo: Int
    ) {
        var meters = 0.0; var seconds = 0.0
        var pacedMeters = 0.0; var pacedSeconds = 0.0
        var timeOnlySeconds = 0.0; var setCount = 0
        val workoutIds = HashSet<String>()

        fun record(workoutId: String, m: Double, s: Double, pace: Double?) {
            setCount += 1; workoutIds.add(workoutId)
            meters += m; seconds += s
            if (pace != null) {
                pacedMeters += m; pacedSeconds += s
            } else if (m <= 0.0) {
                timeOnlySeconds += s
            }
        }

        fun build() = CardioWeekStats(
            weekStartDate = weekStartDate, weekStartIso = weekStartIso,
            weekStartEpochMs = weekStartEpochMs, weekEndExclusiveEpochMs = weekEndExclusiveEpochMs,
            weeksAgo = weeksAgo, meters = meters, seconds = seconds,
            pace = tp107Pace(pacedSeconds, pacedMeters), timeOnlySeconds = timeOnlySeconds,
            pacedMeters = pacedMeters, pacedSeconds = pacedSeconds,
            setCount = setCount, workoutCount = workoutIds.size
        )
    }

    private class ActivityAccumulator(
        val exerciseId: String, var nameHu: String, var nameEn: String, var nameDe: String, var nameRo: String
    ) {
        var setCount = 0
        val workoutIds = HashSet<String>()

        fun updateNamesIfBlank(row: CardioSetRow) {
            if (nameHu == exerciseId && row.exerciseNameHu.isNotBlank()) nameHu = row.exerciseNameHu.trim()
            if (nameEn == exerciseId && row.exerciseNameEn.isNotBlank()) nameEn = row.exerciseNameEn.trim()
            if (nameDe == exerciseId && row.exerciseNameDe.isNotBlank()) nameDe = row.exerciseNameDe.trim()
            if (nameRo == exerciseId && row.exerciseNameRo.isNotBlank()) nameRo = row.exerciseNameRo.trim()
        }

        fun build() = CardioActivityOption(exerciseId, nameHu, nameEn, nameDe, nameRo, setCount, workoutIds.size)
    }
}