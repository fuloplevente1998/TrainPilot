package com.repforge.app

import android.app.Activity
import android.content.Context
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
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean

/** Shared foreground-only read through the existing Samsung/HC adapters, never a synthetic provider. */
internal object NativeHealthRefresh {
    /** Exact local-day boundaries; the current day's query stops at now. */
    internal fun dayWindow(day: LocalDate, zone: ZoneId, now: java.time.Instant): Pair<java.time.Instant, java.time.Instant> {
        return NativeHealthSync.window(day, zone, now)
    }

    private data class Prepared(
        val provider: String,
        val journalGeneration: Long,
        val replacement: NativeDataReplacement.Continuation,
        val zone: ZoneId,
        val healthGrants: Set<String>,
        val localDay: LocalDate
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
    ): Result<JSONObject> = execute(context, activity, day, requestSamsungPermission, requireScreenGuard)

    suspend fun sync(
        context: Context, activity: Activity, full: Boolean = false,
        requestSamsungPermission: Boolean = false,
        progress: (NativeHealthSync.Progress) -> Unit = {},
        onPermissionFinished: () -> Unit = {},
        requireScreenGuard: () -> Unit = {}
    ): Result<JSONObject> = execute(context, activity, LocalDate.now(), requestSamsungPermission,
        requireScreenGuard, full, progress, onPermissionFinished)

    private suspend fun execute(
        context: Context, activity: Activity, day: LocalDate, requestSamsungPermission: Boolean,
        requireScreenGuard: () -> Unit, fullSync: Boolean? = null,
        progress: (NativeHealthSync.Progress) -> Unit = {},
        onPermissionFinished: () -> Unit = {}
    ): Result<JSONObject> = withContext(Dispatchers.Main.immediate) {
        val owner = activity as? LifecycleOwner
            ?: return@withContext Result.failure(IllegalStateException("Foreground lifecycle required"))
        val foreground = AtomicBoolean(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val lifecycleEpoch = AtomicLong(0)
        val readLifecycleEpoch = AtomicLong(-1)
        val observer = LifecycleEventObserver { _, _ ->
            val resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (!resumed) lifecycleEpoch.incrementAndGet()
            foreground.set(resumed)
        }
        owner.lifecycle.addObserver(observer)
        val operationContext = currentCoroutineContext()
        val screenGuard = {
            operationContext.ensureActive()
            check(foreground.get()) { "Foreground health read interrupted" }
            check(readLifecycleEpoch.get() < 0 || readLifecycleEpoch.get() == lifecycleEpoch.get()) {
                "Health foreground scene changed; retry"
            }
            requireScreenGuard()
            Unit
        }
        try {
            screenGuard()
            // A real main-looper check before SDK work. The caller-side VM may only
            // have a stale snapshot of the execution state by the time readDay begins.
            NativeHealthForegroundAdmission.requireMainAdmission(context)
            require(!day.isAfter(LocalDate.now())) { "Future health day" }
            // Permission manager IPC must stay outside the metadata lease, too.
            val grantedBeforePrepare = withContext(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= 34)
                    HealthConnectRepository.READ_PERMS.filter { HealthConnectRepository.isAllowed(context, it) }.toSet()
                else emptySet()
            }
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
                        if (provider == HealthJournalStore.HC) grantedBeforePrepare else emptySet(), LocalDate.now())
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
                check(LocalDate.now() == prepared.localDay) { "Health local day changed; refresh again" }
                check(ZoneId.systemDefault() == prepared.zone) { "Health timezone changed; refresh again" }
                val store = HealthJournalStore.get(context)
                check(store.generation() == prepared.journalGeneration &&
                    store.preferences().optString("provider", HealthJournalStore.HC) == prepared.provider) {
                    "Health provider or dataset changed; refresh again"
                }
                Unit
            }
            val requirePermissionIdentity = {
                guard()
                if (prepared.provider == HealthJournalStore.HC) {
                    check(HealthConnectRepository.READ_PERMS.filter {
                        HealthConnectRepository.isAllowed(context, it)
                    }.toSet() == prepared.healthGrants) { "Health permission changed during refresh" }
                }
                Unit
            }
            // The existing SDK adapters publish synchronously. Bridge ONLY their short journal writes
            // into the suspending metadata lease; the SDK/permission calls remain outside all leases.
            val commitRead: ((() -> Unit) -> Unit) = { writer ->
                // Permission IPC and main-only execution probe both precede the metadata lease.
                requirePermissionIdentity()
                NativeHealthForegroundAdmission.publish(context, requireCommitIdentity, writer)
            }

            if (prepared.provider == HealthJournalStore.SAMSUNG && requestSamsungPermission) {
                SamsungHealthRepository.requestReadPermissions(activity).getOrThrow()
                withTimeout(10_000) { while (!foreground.get()) delay(50) }
            }
            // A deliberate consent dialog is allowed before reading; later scene loss invalidates the whole flight.
            readLifecycleEpoch.set(lifecycleEpoch.get())
            onPermissionFinished()
            guard()
            if (prepared.provider == HealthJournalStore.HC) {
                check(Build.VERSION.SDK_INT >= 34) { "Health Connect requires Android 14" }
                check(HealthConnectRepository.isSupported(context) && prepared.healthGrants.isNotEmpty()) {
                    "Health Connect read permission required"
                }
            }
            val samsungGrants = if (prepared.provider == HealthJournalStore.SAMSUNG)
                SamsungHealthRepository.readGrantScope(context, guard) else emptySet()
            suspend fun read(target: LocalDate): JSONObject {
                val (startInstant, endInstant) = dayWindow(target, prepared.zone, java.time.Instant.now())
                return if (prepared.provider == HealthJournalStore.SAMSUNG) {
                    SamsungHealthRepository.readHealthDayScoped(context, startInstant.toString(), endInstant.toString(),
                        target.toString(), guard, expectedGeneration = prepared.journalGeneration,
                        readCommit = commitRead, expectedGrantScope = samsungGrants).getOrThrow()
                } else {
                    HealthConnectRepository.readHealthDaySuspend(context, startInstant.toString(), endInstant.toString(),
                        requireForegroundGuard = guard, expectedGeneration = prepared.journalGeneration, readCommit = commitRead)
                }
            }
            val result = if (fullSync == null) read(day) else {
                val store = HealthJournalStore.get(context)
                val provider = object : NativeHealthSync.Provider {
                    override val changesSupported = prepared.provider == HealthJournalStore.HC
                    override suspend fun createToken() = HealthConnectRepository.createChangeTokenSuspend(
                        context, prepared.journalGeneration, guard)
                    override suspend fun pollPage(token: String) = HealthConnectRepository.pollChangePageSuspend(
                        context, token, prepared.journalGeneration, guard, commitRead)
                    override fun invalidToken(error: Throwable) = changesSupported && HealthConnectRepository.invalidChangeToken(error)
                    override suspend fun requireIdentity() {
                        withContext(Dispatchers.IO) { requirePermissionIdentity(); requireCommitIdentity() }
                        if (changesSupported) check(HealthConnectRepository.isSupported(context)) { "Health provider unavailable" }
                        else {
                            check(SamsungHealthRepository.isInstalled(context) && SamsungHealthRepository.isSdkAvailable()) {
                                "Samsung provider unavailable"
                            }
                            check(SamsungHealthRepository.readGrantScope(context, guard) == samsungGrants) {
                                "Samsung read permissions changed"
                            }
                        }
                    }
                    override suspend fun readDay(day: LocalDate): Boolean {
                        val value = read(day)
                        return (value.optJSONArray("warnings")?.length() ?: 0) == 0 &&
                            (value.optJSONArray("failedFields")?.length() ?: 0) == 0
                    }
                }
                val journal = object : NativeHealthSync.Journal {
                    override suspend fun checkpoint() = withContext(Dispatchers.IO) {
                        store.nativeSyncState(prepared.provider, prepared.journalGeneration)
                    }
                    override suspend fun dirtyDays(): Set<LocalDate> = withContext(Dispatchers.IO) {
                        store.nativeSyncDirtyDays()
                    }
                    override suspend fun attempt(at: java.time.Instant) = withContext(Dispatchers.IO) {
                        commitRead { store.nativeSyncAttempt(prepared.journalGeneration, at.toString()) }
                    }
                    override suspend fun commit(expected: NativeHealthSync.Checkpoint, next: NativeHealthSync.Checkpoint,
                        full: Boolean, at: java.time.Instant) = withContext(Dispatchers.IO) {
                        commitRead { store.nativeSyncCheckpoint(prepared.provider, prepared.journalGeneration,
                            expected.revision, next, full, at.toString()) }
                    }
                }
                val grants = if (provider.changesSupported) prepared.healthGrants else samsungGrants
                val scope = "v1|${prepared.provider}|${prepared.zone.id}|${grants.sorted().joinToString(",")}"
                val report = NativeHealthSync.run(provider, journal, scope, prepared.localDay, fullSync, guard,
                    { value -> withContext(Dispatchers.Main.immediate) { screenGuard(); progress(value) } })
                JSONObject().put("partial", !report.successful).put("full", report.full)
                    .put("completedDays", report.completedDays).put("failedDays", org.json.JSONArray(report.failedDays.map { it.toString() }))
            }
            withContext(Dispatchers.IO) { requirePermissionIdentity(); requireCommitIdentity() }
            Result.success(result)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { Result.failure(failure) }
        finally { owner.lifecycle.removeObserver(observer) }
    }
}
