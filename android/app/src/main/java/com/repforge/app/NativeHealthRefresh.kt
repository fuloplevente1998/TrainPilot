package com.repforge.app

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import com.repforge.app.ui.health.NativeHealthForegroundAdmission
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    /** Exact local-day boundaries; the current day's query stops at now. */
    internal fun dayWindow(day: LocalDate, zone: ZoneId, now: java.time.Instant): Pair<java.time.Instant, java.time.Instant> {
        val start = day.atStartOfDay(zone).toInstant()
        val midnight = day.plusDays(1).atStartOfDay(zone).toInstant()
        val end = if (midnight.isAfter(now)) now else midnight
        require(end.isAfter(start)) { "Health day has not started yet" }
        return start to end
    }

    private data class Prepared(
        val provider: String,
        val journalGeneration: Long,
        val replacement: NativeDataReplacement.Continuation,
        val zone: ZoneId,
        val healthGrants: Set<String>
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
            // A real main-looper check before SDK work. The caller-side VM may only
            // have a stale snapshot of the execution state by the time readDay begins.
            NativeHealthForegroundAdmission.requireMainAdmission(context)
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
                        NativeDataReplacement.continuationForCurrentData(context), ZoneId.systemDefault(),
                        if (provider == HealthJournalStore.HC && Build.VERSION.SDK_INT >= 34)
                            HealthConnectRepository.READ_PERMS.filter { HealthConnectRepository.isAllowed(context, it) }.toSet()
                        else emptySet())
                }
            }

            val guard = {
                screenGuard()
                check(NativeHealthForegroundAdmission.safeOffMain(context)) {
                    "Workout ownership, restore or backup blocks Health refresh"
                }
                prepared.replacement.requireCurrent(context)
                check(NativeHealthBackupGate.available()) { "Backup is in progress" }
                Unit
            }
            val requireCommitIdentity = {
                guard()
                check(ZoneId.systemDefault() == prepared.zone) { "Health timezone changed; refresh again" }
                val store = HealthJournalStore.get(context)
                if (prepared.provider == HealthJournalStore.HC) {
                    check(HealthConnectRepository.READ_PERMS.filter {
                        HealthConnectRepository.isAllowed(context, it)
                    }.toSet() == prepared.healthGrants) { "Health permission changed during refresh" }
                }
                check(store.generation() == prepared.journalGeneration &&
                    store.preferences().optString("provider", HealthJournalStore.HC) == prepared.provider) {
                    "Health provider or dataset changed; refresh again"
                }
                Unit
            }
            // The existing SDK adapters publish synchronously. Bridge ONLY their short journal writes
            // into the suspending metadata lease; the SDK/permission calls remain outside all leases.
            val commitRead: ((() -> Unit) -> Unit) = { writer ->
                // The tested publisher checks main-only execution BEFORE the metadata lease.
                NativeHealthForegroundAdmission.publish(context, requireCommitIdentity, writer)
            }

            if (prepared.provider == HealthJournalStore.SAMSUNG && requestSamsungPermission) {
                SamsungHealthRepository.requestReadPermissions(activity).getOrThrow()
                withTimeout(10_000) { while (!foreground.get()) delay(50) }
            }
            guard()
            val (startInstant, endInstant) = dayWindow(day, prepared.zone, java.time.Instant.now())
            val start = startInstant.toString()
            val end = endInstant.toString()
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
