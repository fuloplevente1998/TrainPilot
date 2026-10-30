package com.repforge.app

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.room.withTransaction
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryEntity
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/** Explicit workout-window HC refresh through the existing SDK adapter and canonical journal mapper. */
internal class NativeWorkoutHealth(private val context: Context, private val db: TrainPilotDatabase) {
    data class Request(val expected: WorkoutHistoryEntity, val ticket: NativeDataReplacement.Continuation)

    internal suspend fun prepare(expected: WorkoutHistoryEntity): Request = NativeDataReplacement.mutate(context) {
        NativeJournalEditor.requireNotActive(context, aliases(expected))
        db.withTransaction {
            val latest = db.workoutHistoryDao().getWorkoutWithDetailsById(expected.id)?.workout ?: error("Workout deleted")
            check(latest.started == expected.started && latest.finished == expected.finished) { "Workout window changed" }
            for (id in aliases(latest)) check(!db.workoutHistoryDao().isTombstoned(id, JSONObject.quote(id))) { "Workout deleted" }
            val duration = Duration.between(Instant.parse(latest.started), Instant.parse(latest.finished))
            require(!duration.isZero && !duration.isNegative && duration <= Duration.ofHours(24))
            Request(latest, NativeDataReplacement.continuationForCurrentData(context))
        }
    }

    internal suspend fun attach(request: Request, response: JSONObject) = NativeDataReplacement.mutate(context) {
        currentCoroutineContext().ensureActive()
        request.ticket.requireCurrent(context)
        val expected = request.expected
        NativeJournalEditor.requireNotActive(context, aliases(expected))
        check(sameInstant(response.optString("start"), expected.started) && sameInstant(response.optString("end"), expected.finished)) {
            "Provider returned a different workout window"
        }
        db.withTransaction {
            val latest = db.workoutHistoryDao().getWorkoutWithDetailsById(expected.id) ?: error("Workout deleted")
            val current = latest.workout
            check(current.started == expected.started && current.finished == expected.finished &&
                current.health240Json == expected.health240Json) { "Workout or Health data changed; refresh again" }
            for (id in aliases(current)) check(!db.workoutHistoryDao().isTombstoned(id, JSONObject.quote(id))) {
                "Deleted workout cannot receive Health data"
            }
            val health = JSONObject(response.toString()).put("windowStart", current.started).put("windowEnd", current.finished)
                .put("syncedAt", Instant.now().toString())
            val raw = LegacyWebViewMigrator.workoutJson(latest).put("health240", health)
            // All other fields come from the latest row, including edits made while the SDK was reading.
            // Original Wear telemetry/window markers stay separate; an HC refresh does not rewrite them.
            LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context)).saveWorkoutJsonToRoom(raw)
            check(db.workoutHistoryDao().getWorkoutWithDetailsById(current.id)?.workout?.health240Json == health.toString()) {
                "Workout Health data was not stored"
            }
        }
    }

    suspend fun refresh(activity: Activity, expected: WorkoutHistoryEntity): Result<Unit> = withContext(Dispatchers.Main.immediate) {
        val owner = activity as? LifecycleOwner
            ?: return@withContext Result.failure(IllegalStateException("Foreground lifecycle required"))
        val foreground = AtomicBoolean(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, _ -> foreground.set(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
        owner.lifecycle.addObserver(observer)
        try {
            check(foreground.get()) { "Foreground health read required" }
            if (Build.VERSION.SDK_INT < 34 || !HealthConnectRepository.isSupported(context))
                throw UnsupportedOperationException("Health Connect requires Android 14")
            if (listOf("READ_STEPS", "READ_HEART_RATE", "READ_ACTIVE_CALORIES_BURNED", "READ_TOTAL_CALORIES_BURNED",
                    "READ_EXERCISE", "READ_DISTANCE", "READ_SPEED").none {
                    context.checkSelfPermission("android.permission.health.$it") == PackageManager.PERMISSION_GRANTED })
                throw SecurityException("Health Connect read permission required")
            val request = withContext(Dispatchers.IO) { prepare(expected) }
            val guard = { check(foreground.get()) { "Foreground health read interrupted" }; request.ticket.requireCurrent(context); Unit }
            guard()
            // No metadata lease, Room transaction or snapshot monitor is held over provider IPC.
            val response = HealthConnectRepository.readTrainingWindowSuspend(context, request.expected.started, request.expected.finished, guard)
            guard()
            withContext(Dispatchers.IO) { attach(request, response) }
            Result.success(Unit)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { Result.failure(failure) }
        finally { owner.lifecycle.removeObserver(observer) }
    }

    companion object {
        private fun aliases(row: WorkoutHistoryEntity) = setOf(row.id, row.syncId, row.workoutKey).filter { it.isNotBlank() }.toSet()
        private fun sameInstant(a: String, b: String) = runCatching { Instant.parse(a) == Instant.parse(b) }.getOrDefault(false)
        fun matchesWindow(health: JSONObject, workout: WorkoutHistoryEntity) =
            sameInstant(health.optString("windowStart"), workout.started) && sameInstant(health.optString("windowEnd"), workout.finished)

        /** Missing/null/invalid values stay missing, distinct from a real zero. No cross-source sum. */
        fun metrics(data: JSONObject, wear: Boolean): Map<String, Double> {
            val keys = if (wear) listOf("averageHeartRate", "minHeartRate", "maxHeartRate", "heartRateSamples",
                "totalCalories", "steps", "activeDurationSeconds", "distanceMeters", "averageSpeedMps", "maxSpeedMps")
            else listOf("averageHeartRate", "minHeartRate", "maxHeartRate", "heartRateSamples", "steps",
                "activeCalories", "totalCalories", "workoutCalories", "distanceMeters", "averageSpeedMps", "maxSpeedMps",
                "exerciseMinutes", "exerciseSessionCount")
            return keys.mapNotNull { key ->
                val value = (data.opt(key) as? Number)?.toDouble()
                if (value == null || !value.isFinite() || value < 0 ||
                    (key in setOf("averageHeartRate", "minHeartRate", "maxHeartRate") && value == 0.0)) null else key to value
            }.toMap()
        }
    }
}
