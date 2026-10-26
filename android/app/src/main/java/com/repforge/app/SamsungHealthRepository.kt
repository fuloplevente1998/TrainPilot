package com.repforge.app

import android.app.Activity
import android.content.Context
import android.os.Looper
import com.samsung.android.sdk.health.data.HealthDataService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Optional, foreground-only, read-only Samsung Health Data SDK 1.1.0 repository.
 * Preserves 100% of SamsungHealthPlugin's error-code unwrapping, DST-safe day boundaries,
 * non-overlapping energy matching, and HealthJournalStore integration.
 */
object SamsungHealthRepository {
    const val SOURCE: String = "com.sec.android.app.shealth"

    val PERMISSION_KEYS: List<String> = listOf(
        "READ_STEPS",
        "READ_ACTIVE_CALORIES_BURNED",
        "READ_TOTAL_CALORIES_BURNED",
        "READ_DISTANCE",
        "READ_EXERCISE",
        "READ_HEART_RATE",
        "READ_SLEEP",
        "READ_WEIGHT",
        "READ_BODY_FAT",
        "READ_OXYGEN_SATURATION",
        "READ_ENERGY_SCORE"
    )

    @JvmStatic
    fun isInstalled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        return try {
            context.packageManager.getPackageInfo(SOURCE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    @JvmStatic
    fun isSdkAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        return try {
            Class.forName("com.samsung.android.sdk.health.data.HealthDataService")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    /** Preserve the SDK exception code even if wrapped inside a platform/ExecutionException cause. */
    @JvmStatic
    fun errorCode(error: Throwable?): String {
        val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        var cause = error
        while (cause != null && seen.add(cause)) {
            if (cause.javaClass.name == "com.samsung.android.sdk.health.data.error.HealthDataException") {
                try {
                    val code = cause.javaClass.getMethod("getErrorCode").invoke(cause)
                    if (code != null) return "SAMSUNG_HEALTH_$code"
                } catch (_: Exception) {
                }
            }
            cause = cause.cause
        }
        return "SAMSUNG_HEALTH_UNAVAILABLE"
    }

    fun getStatus(context: Context, grantedKeys: Set<String> = emptySet()): JSONObject {
        val permissions = JSONObject()
        for (key in PERMISSION_KEYS) {
            permissions.put(key, grantedKeys.contains(key))
        }
        permissions.put("READ_HEART_RATE_VARIABILITY", false)
        permissions.put("WRITE_EXERCISE", false)
        return JSONObject()
            .put("provider", HealthJournalStore.SAMSUNG)
            .put("installed", isInstalled(context))
            .put("sdkAvailable", isSdkAvailable())
            .put("permissions", permissions)
            .put("granted", grantedKeys.isNotEmpty())
            .put("hrvSupported", false)
            .put("backgroundSupported", false)
    }

    @Throws(IllegalArgumentException::class)
    fun validateRange(startIso: String, endIso: String, maxMinutes: Long): Array<Instant> {
        val start = Instant.parse(startIso)
        val end = Instant.parse(endIso)
        if (!end.isAfter(start) || Duration.between(start, end).toMinutes() > maxMinutes) {
            throw IllegalArgumentException("Invalid Samsung health time range.")
        }
        return arrayOf(start, end)
    }

    /** Explicit permission UI, initiated by selecting Samsung Health. */
    suspend fun requestReadPermissions(activity: Activity): Result<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            check(isInstalled(activity) && isSdkAvailable()) { "Samsung Health nem érhető el." }
            suspendCancellableCoroutine<Unit> { continuation ->
                try {
                    HealthDataService.getStore(activity).requestPermissionsAsync(SamsungSdkReader.permissions(), activity)
                        .setCallback(Looper.getMainLooper(), {
                            if (continuation.isActive) continuation.resume(Unit)
                        }, { error -> if (continuation.isActive) continuation.resumeWithException(error) })
                } catch (error: Exception) { if (continuation.isActive) continuation.resumeWithException(error) }
            }
        }
    }

    /** Real SDK read. Never substitute an empty/fresh day for unavailable data. */
    private fun grantScope(reader: SamsungSdkReader.Reader): Set<String> = reader.grantedKeys()

    /** Permission-only SDK IPC. Call outside every journal/metadata lock. */
    internal suspend fun readGrantScope(context: Context, guard: () -> Unit): Set<String> = withContext(Dispatchers.IO) {
        guard()
        grantScope(SamsungSdkReader.open(context) { guard() }).also {
            check(it.isNotEmpty()) { "Samsung Health read permission required" }
        }
    }

    suspend fun readHealthDay(
        context: Context,
        startIso: String,
        endIso: String,
        dayKey: String = "",
        requireForegroundGuard: () -> Unit,
        expectedGeneration: Long? = null,
        readCommit: ((() -> Unit) -> Unit) = { writer -> writer() }
    ): Result<JSONObject> = readHealthDayScoped(context, startIso, endIso, dayKey,
        requireForegroundGuard, expectedGeneration, readCommit, null)

    internal suspend fun readHealthDayScoped(
        context: Context, startIso: String, endIso: String, dayKey: String,
        requireForegroundGuard: () -> Unit, expectedGeneration: Long?,
        readCommit: ((() -> Unit) -> Unit), expectedGrantScope: Set<String>?
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            requireForegroundGuard()
            check(isInstalled(context) && isSdkAvailable()) { "Samsung Health nem érhető el." }
            val range = validateRange(startIso, endIso, 26 * 60L)
            val day = dayKey.ifEmpty { range[0].atZone(ZoneId.systemDefault()).toLocalDate().toString() }
            require(HealthJournalStore.validDay(day) && range[0].atZone(ZoneId.systemDefault()).toLocalDate().toString() == day)
            val journal = HealthJournalStore.get(context)
            val generation = expectedGeneration ?: journal.generation()
            val reader = SamsungSdkReader.open(context) { requireForegroundGuard() }
            check(reader.granted.isNotEmpty()) { "Samsung Health olvasási engedély szükséges." }
            check(expectedGrantScope == null || grantScope(reader) == expectedGrantScope) { "Samsung read permissions changed" }
            val result = reader.daily(range[0], range[1]).put("day", day)
            // Missing grants must not erase prior values in the canonical Samsung daily row.
            // Use the existing journal failedFields merge, without changing the store schema.
            val permissionState = result.optJSONObject("permissions") ?: JSONObject()
            val deniedFields = mapOf(
                "READ_STEPS" to listOf("steps"),
                "READ_ACTIVE_CALORIES_BURNED" to listOf("activeCalories"),
                "READ_TOTAL_CALORIES_BURNED" to listOf("totalCalories"),
                "READ_DISTANCE" to listOf("distanceMeters"),
                "READ_HEART_RATE" to listOf("averageHeartRate", "minHeartRate", "maxHeartRate", "heartRateSamples"),
                "READ_SLEEP" to listOf("sleepSessions"),
                "READ_WEIGHT" to listOf("weightKg", "weightTime", "weightSource", "bodyFatPercent", "bodyFatTime",
                    "bodyFatSource", "bodyFatMassKg", "skeletalMuscleMassKg", "fatFreeMassKg",
                    "totalBodyWaterLiters", "basalMetabolicRateKcal", "bodyMassIndex"),
                "READ_OXYGEN_SATURATION" to listOf("oxygenSaturationPercent", "oxygenSaturationTime",
                    "oxygenSaturationSource"),
                "READ_ENERGY_SCORE" to listOf("energyScore", "energyScoreTime", "energyScoreSource")
            )
            val failures = result.optJSONArray("failedFields") ?: org.json.JSONArray().also {
                result.put("failedFields", it)
            }
            val recorded = (0 until failures.length()).mapNotNull { failures.optString(it).takeIf(String::isNotBlank) }.toMutableSet()
            for ((permission, fields) in deniedFields) {
                if (!permissionState.optBoolean(permission, false)) {
                    for (field in fields) if (recorded.add(field)) failures.put(field)
                }
            }
            requireForegroundGuard()
            // Permission probe is another SDK IPC, performed strictly outside metadata locks.
            // A revoked or expanded grant invalidates the original read rather than publishing stale metrics.
            val currentGrants = SamsungSdkReader.open(context) { requireForegroundGuard() }.granted
            check(currentGrants == reader.granted) { "Samsung read permissions changed" }
            readCommit {
                requireForegroundGuard()
                journal.samsungDay(day, result, reader.journal, generation)
            }
            Result.success(result)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { Result.failure(failure) }
    }
}
