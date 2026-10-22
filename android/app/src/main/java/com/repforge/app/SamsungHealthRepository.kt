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
    suspend fun readHealthDay(
        context: Context,
        startIso: String,
        endIso: String,
        dayKey: String = "",
        requireForegroundGuard: () -> Unit
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            requireForegroundGuard()
            check(isInstalled(context) && isSdkAvailable()) { "Samsung Health nem érhető el." }
            val range = validateRange(startIso, endIso, 26 * 60L)
            val day = dayKey.ifEmpty { range[0].atZone(ZoneId.systemDefault()).toLocalDate().toString() }
            require(HealthJournalStore.validDay(day) && range[0].atZone(ZoneId.systemDefault()).toLocalDate().toString() == day)
            val journal = HealthJournalStore.get(context)
            val generation = journal.generation()
            val reader = SamsungSdkReader.open(context) { requireForegroundGuard() }
            check(reader.granted.isNotEmpty()) { "Samsung Health olvasási engedély szükséges." }
            val result = reader.daily(range[0], range[1]).put("day", day)
            requireForegroundGuard()
            journal.samsungDay(day, result, reader.records, generation)
            Result.success(result)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { Result.failure(failure) }
    }
}
