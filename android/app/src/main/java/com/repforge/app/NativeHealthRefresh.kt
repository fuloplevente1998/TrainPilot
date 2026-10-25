package com.repforge.app

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean

/** Shared foreground-only read through the existing Samsung/HC adapters, never a synthetic provider. */
internal object NativeHealthRefresh {
    private data class Prepared(
        val provider: String,
        val journalGeneration: Long,
        val replacement: NativeDataReplacement.Continuation,
        val zone: ZoneId
    )

    suspend fun readToday(
        context: Context,
        activity: Activity,
        requestSamsungPermission: Boolean = false,
        requireScreenGuard: () -> Unit = {}
    ): Result<JSONObject> =
        readDay(context, activity, LocalDate.now(), requestSamsungPermission, requireScreenGuard)

    suspend fun readDay(
        context: Context,
        activity: Activity,
        day: LocalDate,
        requestSamsungPermission: Boolean = false,
        requireScreenGuard: () -> Unit = {}
    ): Result<JSONObject> = withContext(Dispatchers.Main.immediate) {
        val owner = activity as? LifecycleOwner
            ?: return@withContext Result.failure(IllegalStateException("Foreground lifecycle required"))
        val foreground = AtomicBoolean(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, _ ->
            foreground.set(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }
        owner.lifecycle.addObserver(observer)
        val operationContext = currentCoroutineContext()
        val screenGuard = {
            operationContext.ensureActive()
            check(foreground.get()) { "Foreground health read interrupted" }
            requireScreenGuard()
            Unit
        }
        try {
            screenGuard()
            require(!day.isAfter(LocalDate.now())) { "Future health day" }
            // Short metadata admission only; NEVER hold the lease over Samsung permission UI or SDK IPC.
            val prepared = withContext(Dispatchers.IO) {
                NativeDataReplacement.mutate(context) {
                    val store = HealthJournalStore.get(context)
                    val provider = store.preferences().optString("provider", HealthJournalStore.HC)
                    check(provider == HealthJournalStore.HC || provider == HealthJournalStore.SAMSUNG) {
                        "Unknown health provider"
                    }
                    Prepared(provider, store.generation(),
                        NativeDataReplacement.continuationForCurrentData(context), ZoneId.systemDefault())
                }
            }

            val guard = {
                screenGuard()
                prepared.replacement.requireCurrent(context)
                Unit
            }
            val requireCommitIdentity = {
                guard()
                check(ZoneId.systemDefault() == prepared.zone) { "Health timezone changed; refresh again" }
                val store = HealthJournalStore.get(context)
                check(store.generation() == prepared.journalGeneration &&
                    store.preferences().optString("provider", HealthJournalStore.HC) == prepared.provider) {
                    "Health provider or dataset changed; refresh again"
                }
                Unit
            }
            // The existing SDK adapters publish synchronously. Bridge ONLY their short journal writes
            // into the suspending metadata lease; the SDK/permission calls remain outside all leases.
            val commitRead: ((() -> Unit) -> Unit) = { writer ->
                runBlocking {
                    NativeDataReplacement.mutate(context) {
                        requireCommitIdentity()
                        writer()
                    }
                }
            }

            if (prepared.provider == HealthJournalStore.SAMSUNG && requestSamsungPermission) {
                SamsungHealthRepository.requestReadPermissions(activity).getOrThrow()
                withTimeout(10_000) { while (!foreground.get()) delay(50) }
            }
            guard()
            val start = day.atStartOfDay(prepared.zone).toInstant().toString()
            val end = day.plusDays(1).atStartOfDay(prepared.zone).toInstant().toString()
            val result = if (prepared.provider == HealthJournalStore.SAMSUNG) {
                SamsungHealthRepository.readHealthDay(
                    context, start, end, day.toString(), guard,
                    expectedGeneration = prepared.journalGeneration, readCommit = commitRead
                ).getOrThrow()
            } else {
                check(Build.VERSION.SDK_INT >= 34) { "Health Connect requires Android 14" }
                check(HealthConnectRepository.READ_PERMS.any {
                    context.checkSelfPermission("android.permission.health.$it") == PackageManager.PERMISSION_GRANTED
                }) { "Health Connect read permission required" }
                HealthConnectRepository.readHealthDaySuspend(
                    context, start, end, requireForegroundGuard = guard,
                    expectedGeneration = prepared.journalGeneration, readCommit = commitRead
                )
            }
            withContext(Dispatchers.IO) { requireCommitIdentity() }
            Result.success(result)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { Result.failure(failure) }
        finally { owner.lifecycle.removeObserver(observer) }
    }
}
