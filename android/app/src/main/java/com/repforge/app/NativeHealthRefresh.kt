package com.repforge.app

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean

/** Shared foreground-only read through the existing Samsung/HC adapters, never a synthetic provider. */
internal object NativeHealthRefresh {
    suspend fun readToday(context: Context, activity: Activity, requestSamsungPermission: Boolean = false): Result<JSONObject> =
        readDay(context, activity, LocalDate.now(), requestSamsungPermission)

    suspend fun readDay(context: Context, activity: Activity, day: LocalDate, requestSamsungPermission: Boolean = false): Result<JSONObject> =
        withContext(Dispatchers.Main.immediate) {
            val owner = activity as? LifecycleOwner
                ?: return@withContext Result.failure(IllegalStateException("Foreground lifecycle required"))
            val foreground = AtomicBoolean(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
            val observer = LifecycleEventObserver { _, _ ->
                foreground.set(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
            }
            owner.lifecycle.addObserver(observer)
            val guard = { check(foreground.get()) { "Foreground health read interrupted" }; Unit }
            try {
                val result = NativeDataReplacement.mutate(context) {
                    val provider = withContext(Dispatchers.IO) { HealthJournalStore.get(context).preferences() }
                        .optString("provider", HealthJournalStore.HC)
                    if (provider == HealthJournalStore.SAMSUNG && requestSamsungPermission) {
                        SamsungHealthRepository.requestReadPermissions(activity).getOrThrow()
                        withTimeout(10000) { while (!foreground.get()) delay(50) }
                    }
                    guard()
                    require(day <= LocalDate.now()) { "Future health day" }
                    val zone = ZoneId.systemDefault()
                    val start = day.atStartOfDay(zone).toInstant().toString()
                    val end = day.plusDays(1).atStartOfDay(zone).toInstant().toString()
                    if (provider == HealthJournalStore.SAMSUNG) {
                        SamsungHealthRepository.readHealthDay(context, start, end, day.toString(), guard).getOrThrow()
                    } else {
                        check(Build.VERSION.SDK_INT >= 34) { "Health Connect requires Android 14" }
                        check(HealthConnectRepository.READ_PERMS.any {
                            context.checkSelfPermission("android.permission.health.$it") == PackageManager.PERMISSION_GRANTED
                        }) { "Health Connect read permission required" }
                        HealthConnectRepository.readHealthDaySuspend(context, start, end, requireForegroundGuard = guard)
                    }
                }
                Result.success(result)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { Result.failure(failure) }
            finally { owner.lifecycle.removeObserver(observer) }
        }
}
