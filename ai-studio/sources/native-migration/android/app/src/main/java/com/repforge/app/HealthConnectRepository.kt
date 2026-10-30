package com.repforge.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.health.connect.AggregateRecordsRequest
import android.health.connect.AggregateRecordsResponse
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.InsertRecordsResponse
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.ReadRecordsResponse
import android.health.connect.TimeInstantRangeFilter
import android.health.connect.changelog.ChangeLogTokenRequest
import android.health.connect.changelog.ChangeLogTokenResponse
import android.health.connect.changelog.ChangeLogsRequest
import android.health.connect.changelog.ChangeLogsResponse
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord
import android.health.connect.datatypes.AggregationType
import android.health.connect.datatypes.BloodGlucoseRecord
import android.health.connect.datatypes.BloodPressureRecord
import android.health.connect.datatypes.BodyFatRecord
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.DistanceRecord
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.ExerciseSessionType
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.HeartRateVariabilityRmssdRecord
import android.health.connect.datatypes.InstantRecord
import android.health.connect.datatypes.IntervalRecord
import android.health.connect.datatypes.Metadata
import android.health.connect.datatypes.OxygenSaturationRecord
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.RespiratoryRateRecord
import android.health.connect.datatypes.RestingHeartRateRecord
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.SpeedRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.TotalCaloriesBurnedRecord
import android.health.connect.datatypes.Vo2MaxRecord
import android.health.connect.datatypes.WeightRecord
import android.health.connect.datatypes.units.Energy
import android.health.connect.datatypes.units.Length
import android.os.Build
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Collections
import java.util.TreeSet
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.max
import kotlin.math.min

/** Native Android 14+ (API 34) Health Connect repository and aggregator. */
object HealthConnectRepository {
    val READ_PERMS: Array<String> = arrayOf(
        "READ_HEART_RATE",
        "READ_RESTING_HEART_RATE",
        "READ_ACTIVE_CALORIES_BURNED",
        "READ_TOTAL_CALORIES_BURNED",
        "READ_EXERCISE",
        "READ_SLEEP",
        "READ_HEART_RATE_VARIABILITY",
        "READ_STEPS",
        "READ_WEIGHT",
        "READ_BODY_FAT",
        "READ_OXYGEN_SATURATION",
        "READ_VO2_MAX",
        "READ_DISTANCE",
        "READ_SPEED",
        "READ_BLOOD_PRESSURE",
        "READ_BLOOD_GLUCOSE",
        "READ_RESPIRATORY_RATE"
    )

    val ALL_RUNTIME_PERMISSIONS: Array<String> = READ_PERMS.map { "android.permission.health.$it" }.toTypedArray()
    const val WRITE_EXERCISE_PERMISSION: String = "android.permission.health.WRITE_EXERCISE"
    const val BACKGROUND_READ_PERMISSION: String = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

    fun isSupported(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= 34 && context.getSystemService(HealthConnectManager::class.java) != null
    }

    fun isAllowed(context: Context, suffix: String): Boolean {
        return context.checkSelfPermission("android.permission.health.$suffix") == PackageManager.PERMISSION_GRANTED
    }

    fun anyReadAllowed(context: Context): Boolean {
        return READ_PERMS.any { isAllowed(context, it) }
    }

    fun allReadsAllowed(context: Context): Boolean {
        return READ_PERMS.all { isAllowed(context, it) }
    }

    fun permissionStatus(context: Context): JSONObject {
        val r = JSONObject()
        for (n in READ_PERMS) {
            r.put(n, isAllowed(context, n))
        }
        r.put("WRITE_EXERCISE", isAllowed(context, "WRITE_EXERCISE"))
        return r
    }

    fun getStatus(context: Context): JSONObject {
        val r = JSONObject()
        r.put("supported", isSupported(context))
        r.put("permissions", permissionStatus(context))
        r.put("granted", anyReadAllowed(context))
        r.put("backgroundSupported", HealthBackgroundJob.supported(context))
        r.put("backgroundGranted", isAllowed(context, "READ_HEALTH_DATA_IN_BACKGROUND"))
        return r
    }

    fun settingsIntent(): Intent = Intent("android.health.connect.action.HEALTH_CONNECT_SETTINGS")

    private class ObsoleteReadPublication(cause: Throwable) : IllegalStateException("Health read publication invalidated", cause)

    class Access(
        context: Context,
        @JvmField val epoch: Long?,
        private val guard: Runnable
    ) {
        /**
         * Optional foreground publication fence. It surrounds local journal commits,
         * never SDK IPC. The default preserves existing callers' write contract.
         */
        var readCommit: ((() -> Unit) -> Unit) = { writer -> writer() }
        internal var dailyChangeRevision: Long? = null

        fun publishRead(writer: () -> Unit) {
            requireForeground()
            try {
                readCommit {
                    requireForeground()
                    val revision = dailyChangeRevision
                    if (revision == null) writer()
                    else HealthJournalStore.get(appContext).nativePublishAtChangeRevision(requireNotNull(epoch), revision, writer)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: IllegalStateException) {
                throw ObsoleteReadPublication(failure)
            }
        }

        private val appContext: Context = context.applicationContext
        @JvmField
        var origin: String = ""

        fun getContext(): Context = appContext

        fun requireForeground() {
            guard.run()
        }

        fun allowed(suffix: String): Boolean = isAllowed(appContext, suffix)

        fun anyReadAllowed(): Boolean = HealthConnectRepository.anyReadAllowed(appContext)

        fun permissionStatus(): JSONObject {
            val r = JSONObject()
            for (n in READ_PERMS) {
                r.put(n, allowed(n))
            }
            return r
        }

        fun message(e: Exception): String {
            var t: Throwable = e
            while (t.cause != null) t = t.cause!!
            return when (t) {
                is SecurityException -> "Permission denied"
                is TimeoutException -> "Health Connect timed out"
                else -> t.javaClass.simpleName
            }
        }
    }

    @RequiresApi(34)
    suspend fun readHealthDaySuspend(
        context: Context,
        startIso: String,
        endIso: String,
        origin: String = "",
        requireForegroundGuard: () -> Unit = {},
        expectedGeneration: Long? = null,
        readCommit: ((() -> Unit) -> Unit) = { writer -> writer() }
    ): JSONObject = withContext(Dispatchers.IO) {
        val epoch = expectedGeneration ?: HealthJournalStore.get(context).generation()
        val access = Access(context, epoch, Runnable { requireForegroundGuard() })
        access.readCommit = readCommit
        val params = JSONObject().put("start", startIso).put("end", endIso).put("origin", origin)
        Api34.readHealthDay(access, params)
    }

    /** A token is a private candidate until the coordinator's durable checkpoint. */
    @RequiresApi(34)
    internal suspend fun createChangeTokenSuspend(
        context: Context, generation: Long, guard: () -> Unit
    ): String = withContext(Dispatchers.IO) {
        guard()
        Api34.createChangeToken(Access(context, generation, Runnable { guard() })).getString("token")
    }

    @RequiresApi(34)
    internal suspend fun pollChangePageSuspend(
        context: Context, token: String, generation: Long, guard: () -> Unit,
        readCommit: ((() -> Unit) -> Unit)
    ): NativeHealthSync.ChangePage = withContext(Dispatchers.IO) {
        val access = Access(context, generation, Runnable { guard() }).also { it.readCommit = readCommit }
        val result = Api34.pollChangePage(access, token)
        val days = result.getJSONArray("changedDays")
        NativeHealthSync.ChangePage(result.getString("nextToken"), result.getBoolean("hasMore"),
            (0 until days.length()).map { LocalDate.parse(days.getString(it)) }.toSet(), result.getInt("deletedCount") > 0)
    }

    @RequiresApi(34)
    internal fun invalidChangeToken(error: Throwable): Boolean {
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is HealthConnectException && cause.errorCode == HealthConnectException.ERROR_INVALID_ARGUMENT) return true
            cause = cause.cause
        }
        return false
    }

    @RequiresApi(34)
    suspend fun readTrainingWindowSuspend(
        context: Context,
        startIso: String,
        endIso: String,
        requireForegroundGuard: () -> Unit = {}
    ): JSONObject = withContext(Dispatchers.IO) {
        val epoch = HealthJournalStore.get(context).generation()
        val access = Access(context, epoch, Runnable { requireForegroundGuard() })
        val params = JSONObject().put("start", startIso).put("end", endIso)
        Api34.readTrainingWindow(access, params)
    }

    @RequiresApi(34)
    suspend fun writeWorkoutSuspend(
        context: Context,
        startIso: String,
        endIso: String,
        title: String,
        clientRecordId: String = "",
        clientVersion: Long = 1L
    ) = withContext(Dispatchers.IO) {
        val epoch = HealthJournalStore.get(context).generation()
        val access = Access(context, epoch, Runnable {})
        val params = JSONObject()
            .put("start", startIso)
            .put("end", endIso)
            .put("title", title)
            .put("clientRecordId", clientRecordId)
            .put("clientVersion", clientVersion)
        Api34.write(access, params)
    }

    @RequiresApi(34)
    object Api34 {
        private val DIRECT = Executor { it.run() }
        private val ORIGIN_REGEX = Regex("[A-Za-z0-9_.-]{1,255}")

        private fun <T> receiver(f: CompletableFuture<T>): OutcomeReceiver<T, HealthConnectException> {
            return object : OutcomeReceiver<T, HealthConnectException> {
                override fun onResult(result: T) {
                    f.complete(result)
                }

                override fun onError(error: HealthConnectException) {
                    f.completeExceptionally(error)
                }
            }
        }

        private fun range(c: JSONObject, maxMinutes: Long): Array<Instant> {
            val a = Instant.parse(c.optString("start", ""))
            val b = Instant.parse(c.optString("end", ""))
            if (!b.isAfter(a) ||
                Duration.between(a, b).toMinutes() > maxMinutes ||
                b.isAfter(Instant.now().plusSeconds(60))
            ) {
                throw IllegalArgumentException("Invalid Health Connect time range")
            }
            return arrayOf(a, b)
        }

        private fun filter(t: Array<Instant>): TimeInstantRangeFilter {
            return TimeInstantRangeFilter.Builder().setStartTime(t[0]).setEndTime(t[1]).build()
        }

        @Throws(Exception::class)
        private fun <T : Record> records(
            p: Access,
            m: HealthConnectManager,
            type: Class<T>,
            time: TimeInstantRangeFilter
        ): List<T> {
            val all = ArrayList<T>()
            var token = -1L
            var pages = 0
            do {
                p.requireForeground()
                val b = ReadRecordsRequestUsingFilters.Builder(type)
                    .setTimeRangeFilter(time)
                    .setPageSize(1000)
                if (token != -1L) b.setPageToken(token)
                val f = CompletableFuture<ReadRecordsResponse<T>>()
                m.readRecords(b.build(), DIRECT, receiver(f))
                val r = f.get(30, TimeUnit.SECONDS)
                all.addAll(r.records)
                token = r.nextPageToken
                if (++pages > 30 && token != -1L) {
                    throw IllegalStateException("Túl sok Health Connect rekordoldal.")
                }
            } while (token != -1L)
            return all
        }

        private fun <T : Record> safe(
            p: Access,
            m: HealthConnectManager,
            type: Class<T>,
            time: TimeInstantRangeFilter,
            perm: String,
            warnings: JSONArray
        ): List<T> {
            if (!p.allowed(perm)) return emptyList()
            return try {
                val readAt = System.currentTimeMillis()
                val result = records(p, m, type, time)
                p.requireForeground()
                if (p.epoch != null) {
                    // Serializing provider rows is CPU work, not an admitted metadata write.
                    val serialized = HealthConnectJournal.serialize(result, p.getContext().packageName)
                    p.publishRead {
                        HealthJournalStore.get(p.getContext()).reconcileRecords(
                            serialized,
                            type.simpleName,
                            requireNotNull(time.startTime) { "Journal requires a start boundary" }.toEpochMilli(),
                            requireNotNull(time.endTime) { "Journal requires an end boundary" }.toEpochMilli(),
                            readAt,
                            p.epoch
                        )
                    }
                }
                if (p.origin.isEmpty()) result else result.filter { src(it) == p.origin }
            } catch (obsolete: ObsoleteReadPublication) {
                throw obsolete
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                warnings.put("$perm: ${p.message(e)}")
                emptyList()
            }
        }

        private fun src(r: Record): String = r.metadata.dataOrigin.packageName
        private fun id(r: Record): String = r.metadata.id

        private fun addSources(out: MutableSet<String>, rows: List<Record>) {
            for (r in rows) out.add(src(r))
        }

        private fun sourceLabels(p: Access, sources: Set<String>): JSONObject {
            val labels = JSONObject()
            val pm = p.getContext().packageManager
            for (pkg in sources) {
                try {
                    labels.put(pkg, pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString())
                } catch (_: Exception) {
                    labels.put(pkg, pkg)
                }
            }
            return labels
        }

        @Throws(Exception::class)
        private fun <T> aggregate(
            p: Access,
            m: HealthConnectManager,
            time: TimeInstantRangeFilter,
            type: AggregationType<T>
        ): T? {
            p.requireForeground()
            val builder = AggregateRecordsRequest.Builder<T>(time).addAggregationType(type)
            if (p.origin.isNotEmpty()) {
                builder.addDataOriginsFilter(DataOrigin.Builder().setPackageName(p.origin).build())
            }
            val f = CompletableFuture<AggregateRecordsResponse<T>>()
            m.aggregate(builder.build(), DIRECT, receiver(f))
            return f.get(30, TimeUnit.SECONDS).get(type)
        }

        private fun mostCompleteStepOrigin(
            p: Access,
            m: HealthConnectManager,
            time: TimeInstantRangeFilter,
            origins: Set<String>
        ): String {
            if (!p.allowed("READ_STEPS") || origins.isEmpty()) return ""
            val original = p.origin
            val totals = HashMap<String?, Long?>()
            try {
                for (origin in TreeSet(origins)) {
                    if (origin.isEmpty() || origin == p.getContext().packageName) continue
                    p.origin = origin
                    try {
                        val value = aggregate(p, m, time, StepsRecord.STEPS_COUNT_TOTAL)
                        if (value != null) totals[origin] = value
                    } catch (_: Exception) {
                    }
                }
            } finally {
                p.origin = original
            }
            return HealthDailySource.mostComplete(totals)
        }

        private fun putLongAgg(
            out: JSONObject,
            key: String,
            p: Access,
            m: HealthConnectManager,
            time: TimeInstantRangeFilter,
            perm: String,
            type: AggregationType<Long>,
            warnings: JSONArray
        ) {
            if (!p.allowed(perm)) {
                out.put(key, JSONObject.NULL)
                return
            }
            try {
                val v = aggregate(p, m, time, type)
                out.put(key, v ?: JSONObject.NULL)
            } catch (e: Exception) {
                out.put(key, JSONObject.NULL)
                warnings.put("$perm: ${p.message(e)}")
            }
        }

        private fun putEnergyAgg(
            out: JSONObject,
            key: String,
            p: Access,
            m: HealthConnectManager,
            time: TimeInstantRangeFilter,
            perm: String,
            type: AggregationType<Energy>,
            warnings: JSONArray
        ) {
            if (!p.allowed(perm)) {
                out.put(key, JSONObject.NULL)
                return
            }
            try {
                val v = aggregate(p, m, time, type)
                out.put(key, if (v == null) JSONObject.NULL else v.inCalories / 1000.0)
            } catch (e: Exception) {
                out.put(key, JSONObject.NULL)
                warnings.put("$perm: ${p.message(e)}")
            }
        }

        private fun putDistanceAgg(
            out: JSONObject,
            p: Access,
            m: HealthConnectManager,
            time: TimeInstantRangeFilter,
            warnings: JSONArray
        ) {
            if (!p.allowed("READ_DISTANCE")) {
                out.put("distanceMeters", JSONObject.NULL)
                return
            }
            try {
                val v: Length? = aggregate(p, m, time, DistanceRecord.DISTANCE_TOTAL)
                out.put("distanceMeters", if (v == null) JSONObject.NULL else v.inMeters)
            } catch (e: Exception) {
                out.put("distanceMeters", JSONObject.NULL)
                warnings.put("READ_DISTANCE: ${p.message(e)}")
            }
        }

        private fun sessionArray(rows: List<ExerciseSessionRecord>): JSONArray {
            val a = JSONArray()
            for (r in rows) {
                val x = JSONObject()
                    .put("id", id(r))
                    .put("start", r.startTime.toString())
                    .put("end", r.endTime.toString())
                    .put("durationMinutes", max(0L, Duration.between(r.startTime, r.endTime).toMinutes()))
                    .put("exerciseType", r.exerciseType)
                    .put("title", r.title?.toString() ?: "")
                    .put("source", src(r))
                    .put("clientRecordId", r.metadata.clientRecordId ?: JSONObject.NULL)
                a.put(x)
            }
            return a
        }

        private fun putWorkoutEnergy(
            out: JSONObject,
            p: Access,
            t: Array<Instant>,
            sessions: List<ExerciseSessionRecord>,
            active: List<ActiveCaloriesBurnedRecord>,
            total: List<TotalCaloriesBurnedRecord>
        ) {
            val windows = sessions.map {
                HealthWorkoutEnergy.Session(src(it), it.startTime.toEpochMilli(), it.endTime.toEpochMilli())
            }
            val rows = ArrayList<HealthWorkoutEnergy.EnergyRow>()
            for (r in total) {
                rows.add(
                    HealthWorkoutEnergy.EnergyRow(
                        id(r),
                        src(r),
                        r.startTime.toEpochMilli(),
                        r.endTime.toEpochMilli(),
                        r.energy.inCalories / 1000.0,
                        true,
                        r.metadata.lastModifiedTime.toEpochMilli()
                    )
                )
            }
            for (r in active) {
                rows.add(
                    HealthWorkoutEnergy.EnergyRow(
                        id(r),
                        src(r),
                        r.startTime.toEpochMilli(),
                        r.endTime.toEpochMilli(),
                        r.energy.inCalories / 1000.0,
                        false,
                        r.metadata.lastModifiedTime.toEpochMilli()
                    )
                )
            }
            val energy = HealthWorkoutEnergy.match(
                windows,
                rows,
                t[0].toEpochMilli(),
                t[1].toEpochMilli(),
                p.getContext().packageName
            )
            out.put("workoutEnergyVersion", 1)
                .put("workoutCalories", energy.calories ?: JSONObject.NULL)
                .put("workoutEnergyCoverageMs", energy.coverageMs)
                .put("workoutEnergyProrated", energy.prorated)
                .put("workoutEnergySources", JSONArray(energy.sources))
                .put("workoutEnergySourceLabels", sourceLabels(p, energy.sources))
                .put("workoutEnergyTypes", JSONArray(energy.types))
                .put("workoutEnergyRecordIds", JSONArray(energy.recordIds))
        }

        private fun sessionMinutes(sessions: List<ExerciseSessionRecord>, t: Array<Instant>): Double {
            val spans = ArrayList<LongArray>()
            for (r in sessions) {
                val a = max(t[0].toEpochMilli(), r.startTime.toEpochMilli())
                val b = min(t[1].toEpochMilli(), r.endTime.toEpochMilli())
                if (b > a) spans.add(longArrayOf(a, b))
            }
            spans.sortBy { it[0] }
            var total = 0L
            var end = Long.MIN_VALUE
            for (span in spans) {
                total += max(0L, span[1] - max(span[0], end))
                end = max(end, span[1])
            }
            return total / 60_000.0
        }

        private fun asleepStage(t: Int): Boolean {
            return t == SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING ||
                t == SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_LIGHT ||
                t == SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_DEEP ||
                t == SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_REM
        }

        private fun sleepArray(rows: List<SleepSessionRecord>): JSONArray {
            val a = JSONArray()
            for (r in rows) {
                val stages = JSONArray()
                for (s in r.stages) {
                    stages.put(
                        JSONObject()
                            .put("start", s.startTime.toString())
                            .put("end", s.endTime.toString())
                            .put("type", s.type)
                            .put("isAsleep", asleepStage(s.type))
                    )
                }
                a.put(
                    JSONObject()
                        .put("id", id(r))
                        .put("start", r.startTime.toString())
                        .put("end", r.endTime.toString())
                        .put("durationMinutes", max(0L, Duration.between(r.startTime, r.endTime).toMinutes()))
                        .put("source", src(r))
                        .put("stages", stages)
                )
            }
            return a
        }

        private fun speedStats(out: JSONObject, rows: List<SpeedRecord>, t: Array<Instant>) {
            var sum = 0.0
            var maxVal = 0.0
            var n = 0
            for (r in rows) {
                for (s in r.samples) {
                    if (!s.time.isBefore(t[0]) && s.time.isBefore(t[1])) {
                        val v = s.speed.inMetersPerSecond
                        if (v.isFinite() && v >= 0.0) {
                            sum += v
                            maxVal = max(maxVal, v)
                            n++
                        }
                    }
                }
            }
            out.put("averageSpeedMps", if (n > 0) sum / n else JSONObject.NULL)
                .put("maxSpeedMps", if (n > 0) maxVal else JSONObject.NULL)
                .put("speedSamples", n)
        }

        private fun base(p: Access, warnings: JSONArray, sources: Set<String>): JSONObject {
            return JSONObject()
                .put("warnings", warnings)
                .put("permissions", p.permissionStatus())
                .put("sources", JSONArray(sources))
                .put("sourceLabels", sourceLabels(p, sources))
        }

        private fun <T : InstantRecord> latestInstant(rows: List<T>): T? {
            var best: T? = null
            for (r in rows) {
                if (best == null || r.time.isAfter(best.time)) {
                    best = r
                }
            }
            return best
        }

        @JvmStatic
        @Throws(Exception::class)
        fun readStepsWindow(p: Access, c: JSONObject): JSONObject {
            val t = range(c, 26 * 60L)
            p.origin = c.optString("origin", "")
            if (p.origin.isNotEmpty() && !ORIGIN_REGEX.matches(p.origin)) {
                throw IllegalArgumentException("Invalid steps origin")
            }
            val m = p.getContext().getSystemService(HealthConnectManager::class.java)
            val warnings = JSONArray()
            val out = base(p, warnings, emptySet())
                .put("start", t[0].toString())
                .put("end", t[1].toString())
                .put("provider", HealthJournalStore.HC)
                .put("activityOrigin", p.origin)
            putLongAgg(out, "steps", p, m, filter(t), "READ_STEPS", StepsRecord.STEPS_COUNT_TOTAL, warnings)
            return out
        }

        @JvmStatic
        @Throws(Exception::class)
        fun readTrainingWindow(p: Access, c: JSONObject): JSONObject {
            val t = range(c, 24 * 60L)
            val m = p.getContext().getSystemService(HealthConnectManager::class.java)
            val time = filter(t)
            if (!p.anyReadAllowed()) throw SecurityException("Permission denied")
            val warnings = JSONArray()
            val hrs = safe(p, m, HeartRateRecord::class.java, time, "READ_HEART_RATE", warnings)
            var sessions = safe(p, m, ExerciseSessionRecord::class.java, time, "READ_EXERCISE", warnings)
            val speeds = safe(p, m, SpeedRecord::class.java, time, "READ_SPEED", warnings)
            val distances = safe(p, m, DistanceRecord::class.java, time, "READ_DISTANCE", warnings)
            val active = safe(p, m, ActiveCaloriesBurnedRecord::class.java, time, "READ_ACTIVE_CALORIES_BURNED", warnings)
            val total = safe(p, m, TotalCaloriesBurnedRecord::class.java, time, "READ_TOTAL_CALORIES_BURNED", warnings)

            sessions = sessions.filter { src(it) != p.getContext().packageName }
            val sources = TreeSet<String>()
            addSources(sources, hrs)
            addSources(sources, sessions)
            addSources(sources, speeds)
            addSources(sources, distances)
            addSources(sources, active)
            addSources(sources, total)
            sources.remove(p.getContext().packageName)

            val out = base(p, warnings, sources)
                .put("start", t[0].toString())
                .put("end", t[1].toString())
            putLongAgg(out, "steps", p, m, time, "READ_STEPS", StepsRecord.STEPS_COUNT_TOTAL, warnings)
            putLongAgg(out, "averageHeartRate", p, m, time, "READ_HEART_RATE", HeartRateRecord.BPM_AVG, warnings)
            putLongAgg(out, "minHeartRate", p, m, time, "READ_HEART_RATE", HeartRateRecord.BPM_MIN, warnings)
            putLongAgg(out, "maxHeartRate", p, m, time, "READ_HEART_RATE", HeartRateRecord.BPM_MAX, warnings)
            putLongAgg(out, "heartRateSamples", p, m, time, "READ_HEART_RATE", HeartRateRecord.HEART_MEASUREMENTS_COUNT, warnings)
            putEnergyAgg(out, "activeCalories", p, m, time, "READ_ACTIVE_CALORIES_BURNED", ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL, warnings)
            putEnergyAgg(out, "totalCalories", p, m, time, "READ_TOTAL_CALORIES_BURNED", TotalCaloriesBurnedRecord.ENERGY_TOTAL, warnings)
            putWorkoutEnergy(out, p, t, sessions, active, total)
            putDistanceAgg(out, p, m, time, warnings)
            speedStats(out, speeds, t)
            out.put("exerciseSessions", sessionArray(sessions))
                .put("exerciseSessionCount", sessions.size)
                .put("exerciseMinutes", sessionMinutes(sessions, t))
            return out
        }

        @JvmStatic
        @Throws(Exception::class)
        fun readHealthDay(p: Access, c: JSONObject): JSONObject {
            p.dailyChangeRevision = p.epoch?.let { HealthJournalStore.get(p.getContext()).nativeChangeRevision(it) }
            val t = range(c, 36 * 60L)
            p.origin = c.optString("origin", "")
            if (p.origin.isNotEmpty() && !ORIGIN_REGEX.matches(p.origin)) {
                throw IllegalArgumentException("Invalid Health Connect origin")
            }
            val m = p.getContext().getSystemService(HealthConnectManager::class.java)
            val time = filter(t)
            if (!p.anyReadAllowed()) throw SecurityException("Permission denied")
            val warnings = JSONArray()
            // A revoked/partial grant is not a full replacement for existing daily metrics.
            // HealthJournalStore merges missing fields from the previous aggregate when warned.
            if (READ_PERMS.any { !p.allowed(it) }) {
                warnings.put("Partial Health Connect read permission")
            }

            val hrs = safe(p, m, HeartRateRecord::class.java, time, "READ_HEART_RATE", warnings)
            val resting = safe(p, m, RestingHeartRateRecord::class.java, time, "READ_RESTING_HEART_RATE", warnings)
            val sessions = safe(p, m, ExerciseSessionRecord::class.java, time, "READ_EXERCISE", warnings)
            val sleeps = safe(p, m, SleepSessionRecord::class.java, time, "READ_SLEEP", warnings)
            val hrvs = safe(p, m, HeartRateVariabilityRmssdRecord::class.java, time, "READ_HEART_RATE_VARIABILITY", warnings)
            val weights = safe(p, m, WeightRecord::class.java, time, "READ_WEIGHT", warnings)
            val fats = safe(p, m, BodyFatRecord::class.java, time, "READ_BODY_FAT", warnings)
            val oxy = safe(p, m, OxygenSaturationRecord::class.java, time, "READ_OXYGEN_SATURATION", warnings)
            val vo2 = safe(p, m, Vo2MaxRecord::class.java, time, "READ_VO2_MAX", warnings)
            val speeds = safe(p, m, SpeedRecord::class.java, time, "READ_SPEED", warnings)
            val distances = safe(p, m, DistanceRecord::class.java, time, "READ_DISTANCE", warnings)
            val active = safe(p, m, ActiveCaloriesBurnedRecord::class.java, time, "READ_ACTIVE_CALORIES_BURNED", warnings)
            val total = safe(p, m, TotalCaloriesBurnedRecord::class.java, time, "READ_TOTAL_CALORIES_BURNED", warnings)
            val pressures = safe(p, m, BloodPressureRecord::class.java, time, "READ_BLOOD_PRESSURE", warnings)
            val glucose = safe(p, m, BloodGlucoseRecord::class.java, time, "READ_BLOOD_GLUCOSE", warnings)
            val respiratory = safe(p, m, RespiratoryRateRecord::class.java, time, "READ_RESPIRATORY_RATE", warnings)
            val steps = safe(p, m, StepsRecord::class.java, time, "READ_STEPS", warnings)

            val sources = TreeSet<String>()
            for (rows in listOf(
                steps, hrs, resting, sessions, sleeps, hrvs, weights, fats,
                oxy, vo2, speeds, distances, active, total, pressures, glucose, respiratory
            )) {
                addSources(sources, rows)
            }
            sources.remove(p.getContext().packageName)

            val out = base(p, warnings, sources)
                .put("start", t[0].toString())
                .put("end", t[1].toString())
            val requested = p.origin
            val preferred = HealthJournalStore.get(p.getContext()).preferences().optString("hcDailySource", "auto")
            val stepOrigins = steps.map { src(it) }.toSet()
            var activityOrigin = HealthDailySource.choose(requested, preferred, stepOrigins)
            if (requested.isEmpty() && "auto" == preferred) {
                val complete = mostCompleteStepOrigin(p, m, time, stepOrigins)
                if (complete.isNotEmpty()) activityOrigin = complete
            }
            p.origin = activityOrigin
            putLongAgg(out, "steps", p, m, time, "READ_STEPS", StepsRecord.STEPS_COUNT_TOTAL, warnings)
            p.origin = requested
            putLongAgg(out, "averageHeartRate", p, m, time, "READ_HEART_RATE", HeartRateRecord.BPM_AVG, warnings)
            putLongAgg(out, "minHeartRate", p, m, time, "READ_HEART_RATE", HeartRateRecord.BPM_MIN, warnings)
            putLongAgg(out, "maxHeartRate", p, m, time, "READ_HEART_RATE", HeartRateRecord.BPM_MAX, warnings)
            putLongAgg(out, "heartRateSamples", p, m, time, "READ_HEART_RATE", HeartRateRecord.HEART_MEASUREMENTS_COUNT, warnings)
            putLongAgg(out, "restingHeartRate", p, m, time, "READ_RESTING_HEART_RATE", RestingHeartRateRecord.BPM_AVG, warnings)
            p.origin = activityOrigin
            putEnergyAgg(out, "activeCalories", p, m, time, "READ_ACTIVE_CALORIES_BURNED", ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL, warnings)
            putEnergyAgg(out, "totalCalories", p, m, time, "READ_TOTAL_CALORIES_BURNED", TotalCaloriesBurnedRecord.ENERGY_TOTAL, warnings)
            putDistanceAgg(out, p, m, time, warnings)
            p.origin = requested

            out.put("activityOrigin", activityOrigin)
                .put(
                    "aggregateOrigin",
                    if (requested.isEmpty()) {
                        if ("auto" == preferred && activityOrigin.isNotEmpty()) "health_connect_most_complete_steps"
                        else "health_connect_priority"
                    } else {
                        "health_connect_source"
                    }
                )
                .put("source", if (requested.isEmpty()) "@aggregate" else requested)
                .put("availableStepOrigins", JSONArray(stepOrigins))
            speedStats(out, speeds, t)
            out.put("sleepSessions", sleepArray(sleeps))
                .put("exerciseSessions", sessionArray(sessions))

            val hrv = latestInstant(hrvs)
            out.put("hrvRmssdMs", hrv?.heartRateVariabilityMillis ?: JSONObject.NULL)
            if (hrv != null) {
                out.put("hrvTime", hrv.time.toString())
                    .put("hrvSource", src(hrv))
                    .put("hrvId", id(hrv))
            }

            val w = latestInstant(weights)
            out.put("weightKg", if (w == null) JSONObject.NULL else w.weight.inGrams / 1000.0)
            if (w != null) {
                out.put("weightTime", w.time.toString())
                    .put("weightSource", src(w))
                    .put("weightId", id(w))
            }

            val fat = latestInstant(fats)
            out.put("bodyFatPercent", fat?.percentage?.value ?: JSONObject.NULL)
            if (fat != null) {
                out.put("bodyFatTime", fat.time.toString())
                    .put("bodyFatSource", src(fat))
                    .put("bodyFatId", id(fat))
            }

            val o = latestInstant(oxy)
            out.put("oxygenSaturationPercent", o?.percentage?.value ?: JSONObject.NULL)
            if (o != null) {
                out.put("oxygenSaturationTime", o.time.toString())
                    .put("oxygenSaturationSource", src(o))
                    .put("oxygenSaturationId", id(o))
            }

            val v = latestInstant(vo2)
            out.put("vo2Max", v?.vo2MillilitersPerMinuteKilogram ?: JSONObject.NULL)
            if (v != null) {
                out.put("vo2MaxTime", v.time.toString())
                    .put("vo2MaxSource", src(v))
                    .put("vo2MaxId", id(v))
                    .put("vo2MaxMeasurementMethod", v.measurementMethod)
            }

            val bp = latestInstant(pressures)
            out.put("bloodPressureSystolic", bp?.systolic?.inMillimetersOfMercury ?: JSONObject.NULL)
                .put("bloodPressureDiastolic", bp?.diastolic?.inMillimetersOfMercury ?: JSONObject.NULL)
            if (bp != null) {
                out.put("bloodPressureTime", bp.time.toString())
                    .put("bloodPressureSource", src(bp))
                    .put("bloodPressureId", id(bp))
            }

            val bg = latestInstant(glucose)
            out.put("bloodGlucoseMmolL", bg?.level?.inMillimolesPerLiter ?: JSONObject.NULL)
            if (bg != null) {
                out.put("bloodGlucoseTime", bg.time.toString())
                    .put("bloodGlucoseSource", src(bg))
                    .put("bloodGlucoseId", id(bg))
            }

            val rr = latestInstant(respiratory)
            out.put("respiratoryRate", rr?.rate ?: JSONObject.NULL)
            if (rr != null) {
                out.put("respiratoryRateTime", rr.time.toString())
                    .put("respiratoryRateSource", src(rr))
                    .put("respiratoryRateId", id(rr))
            }

            p.requireForeground()
            out.put("readAt", Instant.now().toString())
            if (p.epoch != null) {
                p.publishRead {
                    HealthJournalStore.get(p.getContext()).healthDay(
                        HealthJournalStore.day(t[0].toEpochMilli()),
                        out,
                        p.epoch
                    )
                }
            }
            return out
        }

        @JvmStatic
        @Throws(Exception::class)
        fun sourceDays(p: Access, from: String, to: String, origin: String) {
            if (!ORIGIN_REGEX.matches(origin)) throw IllegalArgumentException("Invalid origin")
            val a = LocalDate.parse(from)
            val b = LocalDate.parse(to)
            if (b.isBefore(a) || ChronoUnit.DAYS.between(a, b) > 30) {
                throw IllegalArgumentException("Choose up to 31 days for a source summary")
            }
            val store = HealthJournalStore.get(p.getContext())
            val manager = p.getContext().getSystemService(HealthConnectManager::class.java)
            p.origin = origin
            var d = b
            while (!d.isBefore(a)) {
                p.requireForeground()
                if (store.needsSourceDay(d.toString(), origin)) {
                    val start = d.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    var end = d.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                    if (end.isAfter(Instant.now())) end = Instant.now()
                    if (end.isAfter(start)) {
                        val time = filter(arrayOf(start, end))
                        val out = JSONObject(store.sourceDaySeed(d.toString(), origin).toString())
                        val warnings = JSONArray()
                        val counts = AggregateRecordsRequest.Builder<Long>(time)
                            .addDataOriginsFilter(DataOrigin.Builder().setPackageName(origin).build())
                        val longTypes = LinkedHashMap<String, AggregationType<Long>>()
                        if (p.allowed("READ_STEPS")) longTypes["steps"] = StepsRecord.STEPS_COUNT_TOTAL
                        if (p.allowed("READ_HEART_RATE")) {
                            longTypes["averageHeartRate"] = HeartRateRecord.BPM_AVG
                            longTypes["minHeartRate"] = HeartRateRecord.BPM_MIN
                            longTypes["maxHeartRate"] = HeartRateRecord.BPM_MAX
                            longTypes["heartRateSamples"] = HeartRateRecord.HEART_MEASUREMENTS_COUNT
                        }
                        if (p.allowed("READ_RESTING_HEART_RATE")) {
                            longTypes["restingHeartRate"] = RestingHeartRateRecord.BPM_AVG
                        }
                        if (longTypes.isNotEmpty()) {
                            for (type in longTypes.values) counts.addAggregationType(type)
                            val f = CompletableFuture<AggregateRecordsResponse<Long>>()
                            manager.aggregate(counts.build(), DIRECT, receiver(f))
                            val response = f.get(30, TimeUnit.SECONDS)
                            for ((k, v) in longTypes) {
                                out.put(k, response.get(v) ?: JSONObject.NULL)
                            }
                        }
                        p.requireForeground()
                        putEnergyAgg(out, "activeCalories", p, manager, time, "READ_ACTIVE_CALORIES_BURNED", ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL, warnings)
                        putEnergyAgg(out, "totalCalories", p, manager, time, "READ_TOTAL_CALORIES_BURNED", TotalCaloriesBurnedRecord.ENERGY_TOTAL, warnings)
                        putDistanceAgg(out, p, manager, time, warnings)
                        p.requireForeground()
                        out.put("source", origin)
                            .put("activityOrigin", origin)
                            .put("aggregateOrigin", "health_connect_source")
                            .put("warnings", warnings)
                            .put("readAt", Instant.now().toString())
                        if (p.epoch != null) {
                            store.healthDay(d.toString(), out, p.epoch)
                        }
                    }
                }
                d = d.minusDays(1)
            }
        }

        @JvmStatic
        @Throws(Exception::class)
        fun readRecovery(p: Access, c: JSONObject): JSONObject {
            val t = range(c, 14 * 24 * 60L)
            val m = p.getContext().getSystemService(HealthConnectManager::class.java)
            val time = filter(t)
            val warnings = JSONArray()
            val sleeps = safe(p, m, SleepSessionRecord::class.java, time, "READ_SLEEP", warnings)
            val hrvs = safe(p, m, HeartRateVariabilityRmssdRecord::class.java, time, "READ_HEART_RATE_VARIABILITY", warnings)
            var sleep: SleepSessionRecord? = null
            for (r in sleeps) {
                if (sleep == null || r.endTime.isAfter(sleep.endTime)) sleep = r
            }
            val latest = latestInstant(hrvs)
            val vals = ArrayList<Double>()
            for (r in hrvs) {
                val v = r.heartRateVariabilityMillis
                if (v > 0.0 && v.isFinite()) vals.add(v)
            }
            vals.sort()
            val med = if (vals.isNotEmpty()) {
                val n = vals.size
                if (n % 2 == 1) vals[n / 2] else (vals[n / 2 - 1] + vals[n / 2]) / 2.0
            } else null

            val out = JSONObject()
                .put("warnings", warnings)
                .put("permissions", p.permissionStatus())
                .put("sleepMinutes", if (sleep == null) JSONObject.NULL else max(0L, Duration.between(sleep.startTime, sleep.endTime).toMinutes()))
                .put("sleepStart", sleep?.startTime?.toString() ?: JSONObject.NULL)
                .put("sleepEnd", sleep?.endTime?.toString() ?: JSONObject.NULL)
            if (sleep != null) out.put("sleepSource", src(sleep))
            out.put("hrvRmssdMs", latest?.heartRateVariabilityMillis ?: JSONObject.NULL)
                .put("hrvBaselineMs", med ?: JSONObject.NULL)
                .put("hrvSamples", vals.size)
            if (latest != null) out.put("hrvSource", src(latest))
            return out
        }

        private fun addGrantedTypes(p: Access, b: ChangeLogTokenRequest.Builder) {
            if (p.allowed("READ_HEART_RATE")) b.addRecordType(HeartRateRecord::class.java)
            if (p.allowed("READ_RESTING_HEART_RATE")) b.addRecordType(RestingHeartRateRecord::class.java)
            if (p.allowed("READ_ACTIVE_CALORIES_BURNED")) b.addRecordType(ActiveCaloriesBurnedRecord::class.java)
            if (p.allowed("READ_TOTAL_CALORIES_BURNED")) b.addRecordType(TotalCaloriesBurnedRecord::class.java)
            if (p.allowed("READ_EXERCISE")) b.addRecordType(ExerciseSessionRecord::class.java)
            if (p.allowed("READ_SLEEP")) b.addRecordType(SleepSessionRecord::class.java)
            if (p.allowed("READ_HEART_RATE_VARIABILITY")) b.addRecordType(HeartRateVariabilityRmssdRecord::class.java)
            if (p.allowed("READ_STEPS")) b.addRecordType(StepsRecord::class.java)
            if (p.allowed("READ_WEIGHT")) b.addRecordType(WeightRecord::class.java)
            if (p.allowed("READ_BODY_FAT")) b.addRecordType(BodyFatRecord::class.java)
            if (p.allowed("READ_OXYGEN_SATURATION")) b.addRecordType(OxygenSaturationRecord::class.java)
            if (p.allowed("READ_VO2_MAX")) b.addRecordType(Vo2MaxRecord::class.java)
            if (p.allowed("READ_DISTANCE")) b.addRecordType(DistanceRecord::class.java)
            if (p.allowed("READ_SPEED")) b.addRecordType(SpeedRecord::class.java)
            if (p.allowed("READ_BLOOD_PRESSURE")) b.addRecordType(BloodPressureRecord::class.java)
            if (p.allowed("READ_BLOOD_GLUCOSE")) b.addRecordType(BloodGlucoseRecord::class.java)
            if (p.allowed("READ_RESPIRATORY_RATE")) b.addRecordType(RespiratoryRateRecord::class.java)
        }

        @JvmStatic
        @Throws(Exception::class)
        fun createChangeToken(p: Access): JSONObject {
            if (!p.anyReadAllowed()) throw SecurityException("Permission denied")
            val b = ChangeLogTokenRequest.Builder()
            addGrantedTypes(p, b)
            val f = CompletableFuture<ChangeLogTokenResponse>()
            p.getContext().getSystemService(HealthConnectManager::class.java)
                .getChangeLogToken(b.build(), DIRECT, receiver(f))
            val r = f.get(30, TimeUnit.SECONDS)
            return JSONObject()
                .put("token", r.token)
                .put("createdAt", Instant.now().toString())
        }

        private fun addDay(days: MutableSet<String>, time: Instant) {
            days.add(time.atZone(ZoneId.systemDefault()).toLocalDate().toString())
        }

        private fun addRecordDays(days: MutableSet<String>, r: Record) {
            when (r) {
                is InstantRecord -> addDay(days, r.time)
                is IntervalRecord -> {
                    var d = r.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                    val e = r.endTime.atZone(ZoneId.systemDefault()).toLocalDate()
                    check(ChronoUnit.DAYS.between(d, e) <= 530) { "Health interval exceeds sync budget" }
                    while (!d.isAfter(e)) {
                        days.add(d.toString())
                        d = d.plusDays(1)
                    }
                }
            }
        }

        /** One bounded IPC page, then the SAME protected publication as daily reads. */
        internal fun pollChangePage(p: Access, token: String): JSONObject {
            p.dailyChangeRevision = null // This Access may have been used for an earlier daily read.
            require(token.isNotEmpty()) { "Empty token" }
            p.requireForeground()
            val f = CompletableFuture<ChangeLogsResponse>()
            p.getContext().getSystemService(HealthConnectManager::class.java)
                .getChangeLogs(ChangeLogsRequest.Builder(token).setPageSize(2000).build(), DIRECT, receiver(f))
            val r = f.get(30, TimeUnit.SECONDS)
            val days = TreeSet<String>()
            val types = TreeSet<String>()
            for (record in r.upsertedRecords) {
                if (src(record) == p.getContext().packageName) continue
                types.add(record.javaClass.simpleName)
                addRecordDays(days, record)
            }
            val deleted = JSONArray()
            for (entry in r.deletedLogs) deleted.put(JSONObject()
                .put("recordId", entry.deletedRecordId).put("deletedMs", entry.deletedTime.toEpochMilli()))
            // Serialization and IPC stay outside the short publication fence.
            val records = HealthConnectJournal.serialize(r.upsertedRecords, p.getContext().packageName)
            if (p.epoch != null) p.publishRead {
                val dirty = HealthJournalStore.get(p.getContext()).applyChanges(records, deleted, p.epoch)
                for (i in 0 until dirty.length()) days.add(dirty.getString(i))
            }
            return JSONObject().put("changedDays", JSONArray(days)).put("changedTypes", JSONArray(types))
                .put("deletedCount", r.deletedLogs.size).put("nextToken", r.nextChangesToken)
                .put("hasMore", r.hasMorePages()).put("checkedAt", Instant.now().toString())
        }

        @JvmStatic
        @Throws(Exception::class)
        fun pollChanges(p: Access, token: String): JSONObject {
            val days = TreeSet<String>()
            val types = TreeSet<String>()
            var next = token
            var deleted = 0
            var pages = 0
            var page: JSONObject
            do {
                page = pollChangePage(p, next)
                val changedDays = page.getJSONArray("changedDays")
                for (i in 0 until changedDays.length()) days.add(changedDays.getString(i))
                val changedTypes = page.getJSONArray("changedTypes")
                for (i in 0 until changedTypes.length()) types.add(changedTypes.getString(i))
                deleted += page.getInt("deletedCount")
                next = page.getString("nextToken")
                check(++pages <= 20 || !page.getBoolean("hasMore")) { "Too many change pages; retry full sync" }
            } while (page.getBoolean("hasMore"))
            return JSONObject().put("changedDays", JSONArray(days)).put("changedTypes", JSONArray(types))
                .put("deletedCount", deleted).put("nextToken", next).put("checkedAt", Instant.now().toString())
        }

        @JvmStatic
        @Throws(Exception::class)
        fun write(p: Access, c: JSONObject) {
            val t = range(c, 24 * 60L)
            var clientRecordId = c.optString("clientRecordId", "")
            if (clientRecordId.isEmpty()) clientRecordId = "trainpilot:${t[0]}"
            val meta = Metadata.Builder()
                .setClientRecordId(clientRecordId)
                .setClientRecordVersion(max(1L, c.optLong("clientVersion", 1L)))
                .setRecordingMethod(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED)
                .build()
            val r = ExerciseSessionRecord.Builder(
                meta,
                t[0],
                t[1],
                ExerciseSessionType.EXERCISE_SESSION_TYPE_STRENGTH_TRAINING
            )
                .setTitle(c.optString("title", "TrainPilot"))
                .build()
            val f = CompletableFuture<InsertRecordsResponse>()
            p.getContext().getSystemService(HealthConnectManager::class.java)
                .insertRecords(Collections.singletonList<android.health.connect.datatypes.Record>(r), DIRECT, receiver(f))
            f.get(30, TimeUnit.SECONDS)
        }
    }
}
