package com.repforge.app

import androidx.room.withTransaction
import com.repforge.app.ui.settings.NativeGoogleMaintenance
import android.accounts.Account
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.NativeBackupPrivacy
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.shared.GpsRoutePacket193
import com.repforge.shared.LegacySyncIdentity18
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.max

data class GoogleSyncUiState(
    val connectedEmail: String = "",
    val connectedName: String = "",
    val connectedSub: String = "",
    val isBusy: Boolean = false,
    val activeOperation: String = "",
    val stageMessage: String = "",
    val lastDriveSyncIso: String = "",
    val lastCalendarSyncIso: String = "",
    val errorMessage: String? = null
)

internal enum class GoogleSyncAction(val key: String) { CONNECT("connect"), DRIVE("drive"), CALENDAR("calendar") }
internal data class GoogleSyncCompletion(val action: GoogleSyncAction, val syncedAt: String? = null, val count: Int? = null, val maintenance: NativeGoogleMaintenance.Report? = null)

/**
 * Native replacement of GoogleSyncPlugin + #18 multi-device Drive merge engine.
 * Access tokens are never written to prefs, backups, or logs; UI forwards only an opaque consent result.
 */
class GoogleSyncRepository internal constructor(context: Context, private val operations: NativeGoogleOperationGate) {
    constructor(context: Context) : this(context, processOperations)

    companion object {
        private const val PREFS_NAME = "repforge-google"
        private val processOperations = NativeGoogleOperationGate()
        private val accountEpoch = java.util.concurrent.atomic.AtomicLong(0)
        private const val LIMIT = 20 * 1024 * 1024
        private const val PHOTO_LIMIT = 6 * 1024 * 1024
        private const val MANAGED_CALENDAR_DESC = "RepForge managed workout calendar v1"
        private val FILE_ID_REGEX = Regex("[A-Za-z0-9_-]+")
        private val UUID_REGEX = Regex("[a-f0-9-]{36}")
        private val SNAPSHOT_NAME_REGEX = Regex("^repforge-sync-([a-z0-9-]{36})-")
        private val APP_FILE_REGEX = Regex("repforge-sync-[a-z0-9-]{36}-[A-Za-z0-9_-]{1,80}\\.json|trainpilot-photo-[a-f0-9-]{36}\\.jpg")
        private val EVENT_ID_REGEX = Regex("rf[0-9a-f]{64}")
    }

    private val appContext = context.applicationContext
    private val database = TrainPilotDatabase.getInstance(appContext)
    private val settingsStore = TrainPilotSettingsStore(appContext)
    private val migrator = LegacyWebViewMigrator(appContext, database, settingsStore)

    private val _uiState = MutableStateFlow(loadInitialUiState())
    val uiState: StateFlow<GoogleSyncUiState> = _uiState.asStateFlow()

    private fun prefs(): SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadInitialUiState(): GoogleSyncUiState {
        val rawProfile = prefs().getString("profile", "") ?: ""
        val profile = runCatching { if (rawProfile.isNotEmpty()) JSONObject(rawProfile) else null }.getOrNull()
        return GoogleSyncUiState(
            connectedEmail = profile?.optString("email").orEmpty(),
            connectedName = profile?.optString("name").orEmpty(),
            connectedSub = profile?.optString("sub").orEmpty()
        )
    }

    fun refreshProfileState() {
        val loaded = loadInitialUiState()
        _uiState.update { current ->
            if (current.connectedSub != loaded.connectedSub) loaded.copy(
                isBusy = current.isBusy, activeOperation = current.activeOperation,
                stageMessage = current.stageMessage, errorMessage = current.errorMessage)
            else current.copy(connectedEmail = loaded.connectedEmail,
                connectedName = loaded.connectedName, connectedSub = loaded.connectedSub)
        }
    }

    private fun scopesForOperation(op: String): List<Scope> {
        val scopes = mutableListOf(
            Scope("openid"),
            Scope("https://www.googleapis.com/auth/userinfo.email"),
            Scope("https://www.googleapis.com/auth/userinfo.profile")
        )
        if (op.startsWith("drive")) {
            scopes.add(Scope("https://www.googleapis.com/auth/drive.appdata"))
        }
        if (op.startsWith("calendar")) {
            scopes.add(Scope("https://www.googleapis.com/auth/calendar.app.created"))
            scopes.add(Scope("https://www.googleapis.com/auth/calendar.calendarlist.readonly"))
        }
        return scopes
    }

    /** UI state is feedback only; atomic admission is shared by every repository instance. */
    private suspend fun <T> runGoogleOperation(op: String, stage: String, operation: suspend () -> T): Result<T> {
        return try {
            operations.run {
                // Drive sync stages a snapshot/merge outside the replacement mutex.
                // An unattended Health read must not race this cloud backup window.
                if (op == GoogleSyncAction.DRIVE.key || op == "authorize-drive") NativeHealthBackupGate.begin()
                try {
                    check(op !in setOf("drive", "calendar") || !prefs().getBoolean("maintenancePaused:$op", prefs().getBoolean("maintenancePaused", false))) {
                        "Cloud synchronization is paused after maintenance; explicitly authorize sync again"
                    }
                    refreshProfileState()
                    _uiState.update { it.copy(isBusy = true, activeOperation = op, stageMessage = stage, errorMessage = null) }
                    currentCoroutineContext().ensureActive()
                    val value = operation()
                    currentCoroutineContext().ensureActive()
                    Result.success(value)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (e: Exception) {
                    val message = formatError(e)
                    _uiState.update { it.copy(errorMessage = message) }
                    Result.failure(Exception(message, e))
                } finally {
                    _uiState.update { it.copy(isBusy = false, activeOperation = "", stageMessage = "") }
                    if (op == GoogleSyncAction.DRIVE.key || op == "authorize-drive") NativeHealthBackupGate.end()
                }
            }
        } catch (busy: NativeGoogleOperationGate.Busy) { Result.failure(busy) }
    }

    /** Hold the account-operation gate from authorization through consent, verified identity and I/O.
     * The UI resolver is cancellable and process-local; no replacement/workout fence is held here.
     */
    internal suspend fun authorizeAndRun(
        activity: Activity,
        action: GoogleSyncAction,
        resolve: suspend (PendingIntent) -> Intent?,
        recoverEarlier: Boolean = false,
        maintenance: NativeGoogleMaintenance.Request? = null
    ): Result<GoogleSyncCompletion> = withContext(Dispatchers.IO) {
        val language = settingsStore.current().language
        val strings = com.repforge.app.ui.text.nativeResources(appContext, language)
        runGoogleOperation(maintenance?.kind?.key ?: "authorize-${action.key}", strings.getString(R.string.native_google_authorizing)) {
            val dataTicket = maintenance?.dataset ?: NativeDataReplacement.continuationForCurrentData(appContext)
            if (maintenance != null) requireMaintenance(maintenance)
            val email = prefs().getString("email", "").orEmpty()
            if (action != GoogleSyncAction.CONNECT)
                NativeGoogleAccountPolicy.requireConnected(prefs().getString("sub", "").orEmpty(), email)
            val authorization = withContext(Dispatchers.Main) {
                val builder = AuthorizationRequest.builder().setRequestedScopes(scopesForOperation(maintenance?.kind?.key ?: action.key) +
                    if (maintenance?.kind == NativeGoogleMaintenance.Kind.REVOKE)
                        scopesForOperation("drive") + scopesForOperation("calendar") else emptyList())
                if (email.isNotBlank()) builder.setAccount(Account(email, "com.google"))
                Identity.getAuthorizationClient(activity).authorize(builder.build()).await()
            }
            val result = if (authorization.hasResolution()) {
                val pending = authorization.pendingIntent ?: throw IOException(strings.getString(R.string.native_google_launch_failed))
                _uiState.update { it.copy(stageMessage = strings.getString(R.string.native_google_waiting)) }
                val data = resolve(pending) ?: throw IOException(strings.getString(R.string.native_google_cancelled))
                currentCoroutineContext().ensureActive()
                Identity.getAuthorizationClient(appContext).getAuthorizationResultFromIntent(data)
            } else authorization
            val token = result.accessToken?.takeIf { it.isNotBlank() }
                ?: throw IOException(strings.getString(R.string.native_google_no_token))
            currentCoroutineContext().ensureActive()
            dataTicket.requireCurrent(appContext)
            if (maintenance != null) {
                requireMaintenance(maintenance)
                verifyAndFetchProfile(token)
                requireMaintenance(maintenance)
                val report = performMaintenance(activity, token, maintenance)
                GoogleSyncCompletion(action, maintenance = report)
            } else {
                if (action != GoogleSyncAction.CONNECT) {
                    verifyAndFetchProfile(token)
                    dataTicket.requireCurrent(appContext)
                    check(prefs().edit().putBoolean("maintenancePaused:${action.key}", false).commit())
                }
                when (action) {
                    GoogleSyncAction.CONNECT -> {
                        connectWithTokenInternal(token, dataTicket)
                        GoogleSyncCompletion(action)
                    }
                    GoogleSyncAction.DRIVE -> GoogleSyncCompletion(action,
                        syncedAt = syncDriveCloudInternal(token, false, recoverEarlier, true))
                    GoogleSyncAction.CALENDAR -> GoogleSyncCompletion(action, count = syncCalendarCloudInternal(token, dataTicket))
                }
            }
        }
    }

    suspend fun completeConnectWithToken(accessToken: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        runGoogleOperation("connect", "Google-fiók ellenőrzése…") {
            connectWithTokenInternal(accessToken)
        }
    }

    private suspend fun connectWithTokenInternal(accessToken: String,
        dataTicket: NativeDataReplacement.Continuation = NativeDataReplacement.continuationForCurrentData(appContext)): JSONObject {
        val profile = verifyAndFetchProfile(accessToken, connecting = true)
        currentCoroutineContext().ensureActive()
        return NativeDataReplacement.mutate(appContext) {
            dataTicket.requireCurrent(appContext)
            val safe = JSONObject().put("sub", profile.getString("sub"))
                .put("email", profile.getString("email")).put("name", profile.optString("name"))
            val editor = prefs().edit().putString("profile", safe.toString())
                .putString("email", safe.getString("email")).putString("sub", safe.getString("sub"))
            if (prefs().getString("sub", "") != safe.getString("sub")) editor.remove("calendar")
            check(editor.commit()) { "A Google-fiók nem menthető." }
            accountEpoch.incrementAndGet()
            refreshProfileState()
            safe
        }
    }

    fun disconnect(): Result<Unit> = try {
        operations.runNow {
            // Disconnect clears identity, not a fail-closed maintenance pause. Otherwise
            // reconnecting could silently resurrect deleted cloud data before explicit sync.
            val previous = prefs()
            val pauses = listOf("maintenancePaused", "maintenancePaused:drive", "maintenancePaused:calendar")
                .filter { previous.contains(it) }.associateWith { previous.getBoolean(it, true) }
            val editor = previous.edit().clear()
            pauses.forEach { (key, value) -> editor.putBoolean(key, value) }
            check(editor.commit()) { "A Google-fiók lekapcsolása nem menthető." }
            accountEpoch.incrementAndGet()
            _uiState.value = loadInitialUiState()
            Result.success(Unit)
        }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun revokeAccess(activity: Activity): Result<Unit> = withContext(Dispatchers.IO) {
        runGoogleOperation("revoke", "Google maintenance") {
            val request = captureMaintenance(NativeGoogleMaintenance.Kind.REVOKE)
            prepareMaintenance(request)
            revokeAccessInternal(activity, request)
        }
    }

    internal suspend fun previewMaintenance(kind: NativeGoogleMaintenance.Kind): NativeGoogleMaintenance.Request =
        withContext(Dispatchers.IO) { operations.run { captureMaintenance(kind) } }

    private suspend fun captureMaintenance(kind: NativeGoogleMaintenance.Kind): NativeGoogleMaintenance.Request {
        val sub = prefs().getString("sub", "").orEmpty()
        val email = prefs().getString("email", "").orEmpty()
        NativeGoogleAccountPolicy.requireConnected(sub, email)
        NativeGoogleMaintenance.requireSafe(appContext)
        return NativeGoogleMaintenance.Request(kind, sub, email,
            NativeDataReplacement.continuationForCurrentData(appContext), accountEpoch.get())
    }

    private suspend fun requireMaintenance(request: NativeGoogleMaintenance.Request) {
        currentCoroutineContext().ensureActive()
        request.requireAccount(prefs().getString("sub", "").orEmpty(), prefs().getString("email", "").orEmpty(), accountEpoch.get())
        request.dataset.requireCurrent(appContext)
        NativeGoogleMaintenance.requireSafe(appContext)
    }

    private suspend fun prepareMaintenance(request: NativeGoogleMaintenance.Request) {
        requireMaintenance(request) // Main-only admission before the metadata lease.
        NativeDataReplacement.mutate(appContext) {
            request.dataset.requireCurrent(appContext)
            request.requireAccount(prefs().getString("sub", "").orEmpty(), prefs().getString("email", "").orEmpty(), accountEpoch.get())
            check(com.repforge.app.ui.health.NativeHealthForegroundAdmission.safeOffMain(appContext))
            // Persist fail-closed before any remote deletion. Local history is never touched.
            check(prefs().edit().putBoolean("maintenancePaused", true)
                .putBoolean("maintenancePaused:drive", true).putBoolean("maintenancePaused:calendar", true).commit())
            database.withTransaction {
                val dao = database.keyValueDocumentDao()
                for (prefix in listOf("cloudBase:", "cloudHeads:", "cloudCalendarSent:")) dao.deleteDocument(prefix + request.sub)
                val stamp = System.currentTimeMillis()
                dao.putDocument(KeyValueDocumentEntity("cloudPrefs", JSONObject().put("drive", false).put("calendar", false).toString(), stamp))
                dao.putDocument(KeyValueDocumentEntity("cloudStatus", "{}", stamp))
            }
        }
    }

    private suspend fun revokeAccessInternal(activity: Activity, request: NativeGoogleMaintenance.Request) {
        requireMaintenance(request)
        withContext(Dispatchers.Main) {
            Identity.getAuthorizationClient(activity).revokeAccess(RevokeAccessRequest.builder()
                .setAccount(Account(request.email, "com.google"))
                .setScopes((scopesForOperation("drive") + scopesForOperation("calendar")).distinct()).build()).await()
        }
        requireMaintenance(request)
        NativeDataReplacement.mutate(appContext) {
            request.dataset.requireCurrent(appContext)
            request.requireAccount(prefs().getString("sub", "").orEmpty(), prefs().getString("email", "").orEmpty(), accountEpoch.get())
            check(prefs().edit().clear().putBoolean("maintenancePaused", true).commit())
            accountEpoch.incrementAndGet()
            refreshProfileState()
        }
    }

    private suspend fun performMaintenance(activity: Activity?, token: String, request: NativeGoogleMaintenance.Request): NativeGoogleMaintenance.Report {
        prepareMaintenance(request)
        return try {
            when (request.kind) {
                NativeGoogleMaintenance.Kind.REVOKE -> {
                    revokeAccessInternal(requireNotNull(activity), request)
                    NativeGoogleMaintenance.Report(1, true)
                }
                NativeGoogleMaintenance.Kind.DRIVE -> deleteDriveInternal(token, request)
                NativeGoogleMaintenance.Kind.CALENDAR -> deleteCalendarsInternal(token, request)
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { NativeGoogleMaintenance.Report(0, false, true) }
    }

    /**
     * Full #18 multi-device Drive Cloud Sync:
     * 1. Lists all `repforge-sync-<device>-*.json` files in `appDataFolder`.
     * 2. Selects current snapshot per device + optional archive snapshots when `recoverEarlier == true`.
     * 3. Merges `history`, `weights`, `scheduled`, `programs`, `exercises`, and `settings` using `tp18MergeSync`.
     * 4. Syncs workout photos (`trainpilot-photo-<id>.jpg`) with MD5 verification.
     * 5. Accepts the local merge through the redo importer, then publishes/verifies the snapshot.
     * 6. Records `cloudBase:<owner>` only after publication, deletes photos and prunes old snapshots.
     */
    suspend fun syncDriveCloud(
        accessToken: String,
        silent: Boolean = false,
        recoverEarlier: Boolean = false,
        preferPhoneOnConflict: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        runGoogleOperation("drive", "Drive-fájlok lekérése…") {
            syncDriveCloudInternal(accessToken, silent, recoverEarlier, preferPhoneOnConflict)
        }
    }

    private suspend fun syncDriveCloudInternal(accessToken: String, silent: Boolean, recoverEarlier: Boolean, preferPhoneOnConflict: Boolean): String {
        check(ActiveWorkoutStore.current(appContext)?.optBoolean("active", false) != true) {
            "Fejezd be az aktív edzést a felhőszinkron előtt."
        }
        var photoStage: NativeCloudPhotoStage? = null
        return try {
            val profile = verifyAndFetchProfile(accessToken)
            val owner = profile.getString("sub")
            val localData = migrator.exportBackupJson(includeHealthJournal = false)
            val files = driveListInternal(accessToken)
            val (currentFiles, archiveFiles) = selectSnapshots18(files)

            val baseDoc = database.keyValueDocumentDao().getDocument("cloudBase:$owner")
            val baseData = baseDoc?.json?.let { runCatching { JSONObject(it) }.getOrNull() }

            val remotes = ArrayList<JSONObject>()
            for (i in currentFiles.indices) {
                _uiState.value = _uiState.value.copy(stageMessage = "Mentések olvasása (${i + 1}/${currentFiles.size})…")
                val f = currentFiles[i]
                val snap = driveReadInternal(accessToken, f.getString("id"), owner)
                if (snap != null) remotes.add(snap)
            }

            val archives = ArrayList<JSONObject>()
            if (recoverEarlier && !silent) {
                for (i in archiveFiles.indices) {
                    _uiState.value = _uiState.value.copy(stageMessage = "Korábbi mentések vizsgálata (${i + 1}/${archiveFiles.size})…")
                    val f = archiveFiles[i]
                    val snap = try { driveReadInternal(accessToken, f.getString("id"), owner) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { null }
                    if (snap != null) archives.add(snap)
                }
            }

            val tombstones = database.workoutHistoryDao().getAllTombstones()
            val deletedIds = HashSet<String>()
            val deletedKeys = HashSet<String>()
            for (t in tombstones) {
                if (t.workoutKey.isNotEmpty()) deletedKeys.add(t.workoutKey)
                val arr = runCatching { JSONArray(t.idsJson) }.getOrNull()
                if (arr != null) {
                    for (j in 0 until arr.length()) deletedIds.add(arr.optString(j))
                }
            }

            val merged = NativeBackupPrivacy.sanitize(mergeSync18(
                local = localData,
                remotes = remotes,
                base = baseData,
                archives = archives,
                deletedHistoryIds = deletedIds,
                deletedHistoryKeys = deletedKeys,
                preferLocalOnConflict = preferPhoneOnConflict
            ))

            // Validate portable v144 favorites before any Drive upload or local replacement.
            NativeExerciseFavorites.decode(merged.opt(NativeExerciseFavorites.KEY))

            // Network work may only touch private staged files; errors must not become a successful sync.
            _uiState.value = _uiState.value.copy(stageMessage = "Edzésfotók szinkronizálása…")
            val stagedPhotos = NativeCloudPhotoStage(appContext, merged).also { photoStage = it }
            stagedPhotos.prepare(upload = { id, file -> writeDrivePhotoInternal(accessToken, id, file) },
                download = { id, driveId, file -> readDrivePhotoInternal(accessToken, id, driveId, file) })

            val deviceId = settingsStore.ensureCloudDeviceId()
            val mergedCanonical = LegacySyncIdentity18.canonical(merged)
            val baseCanonical = baseData?.let { LegacySyncIdentity18.canonical(it) }

            val continuation = NativeCloudMergeGuard.installAndPublish(appContext, localData, merged, stagedPhotos, owner) { accepted ->
                if (baseCanonical == null || mergedCanonical != baseCanonical || files.length() == 0) {
                    _uiState.update { it.copy(stageMessage = "Felhőmentés feltöltése és ellenőrzése…") }
                    val envelope = JSONObject().put("app", "RepForgeSync").put("schema", 1)
                        .put("owner", owner).put("device", deviceId).put("data", accepted)
                    writeSnapshotInternal(accessToken, owner, deviceId, envelope)
                }
            }

            // Persistent deletedAt/Drive IDs make a failed delete retryable. Reject stale continuations.
            val operationContext = currentCoroutineContext()
            stagedPhotos.finishRemoteDeletes { id, driveId ->
                continuation.requireCurrent(appContext)
                operationContext.ensureActive()
                NativeCloudPhotoDeletion.deleteUnreferenced(appContext, id, driveId, continuation) {
                    deleteDrivePhotoInternal(accessToken, id, driveId)
                }
            }
            val nowIso = Instant.now().toString()
            NativeDataReplacement.mutate(appContext) {
                continuation.requireCurrent(appContext)
                database.keyValueDocumentDao().putDocument(
                    KeyValueDocumentEntity("cloudStatus", JSONObject().put("drive", nowIso).toString(), System.currentTimeMillis()))
            }

            if (recoverEarlier) {
                _uiState.value = _uiState.value.copy(stageMessage = "Régi mentések karbantartása…")
                try { pruneSnapshotsInternal(accessToken, owner, deviceId) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Optional retention; the verified snapshot stays authoritative. */ }
            }

            WearSyncRepository.publishWatchHomeSnapshot(appContext)
            _uiState.value = _uiState.value.copy(
                lastDriveSyncIso = nowIso,
                errorMessage = null
            )
            nowIso
        } finally { photoStage?.close() }
    }

    /** Syncs scheduled workouts with the TrainPilot-managed Google Calendar (`rf[0-9a-f]{64}` IDs). */
    suspend fun syncCalendarCloud(accessToken: String): Result<Int> = withContext(Dispatchers.IO) {
        runGoogleOperation("calendar", "Naptár szinkronizálása…") {
            syncCalendarCloudInternal(accessToken)
        }
    }

    private suspend fun syncCalendarCloudInternal(accessToken: String,
        dataTicket: NativeDataReplacement.Continuation = NativeDataReplacement.continuationForCurrentData(appContext)): Int {
        verifyAndFetchProfile(accessToken)
        currentCoroutineContext().ensureActive()
        dataTicket.requireCurrent(appContext)
        val calendarId = ensureManagedCalendar(accessToken, dataTicket)
        val entries = database.calendarScheduleDao().getAllEntries()
        val base = "https://www.googleapis.com/calendar/v3/calendars/${enc(calendarId)}/events"
        var count = 0
        for (operation in NativeCalendarSyncPlan.operations(entries)) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            dataTicket.requireCurrent(appContext)
            val id = operation.id
            if (operation.delete) {
                try { request(accessToken, "DELETE", "$base/$id", null) }
                catch (err: ApiError) { if (err.code != 404 && err.code != 410) throw err }
            } else {
                try { request(accessToken, "POST", "$base?sendUpdates=none", operation.payload) }
                catch (err: ApiError) {
                    if (err.code != 409) throw err
                    request(accessToken, "PUT", "$base/$id?sendUpdates=none", operation.payload)
                }
            }
            count++
        }
        val nowIso = Instant.now().toString()
        dataTicket.requireCurrent(appContext)
        _uiState.value = _uiState.value.copy(
            lastCalendarSyncIso = nowIso,
            errorMessage = null
        )
        return count
    }

    suspend fun deleteAllDriveAppData(accessToken: String): Result<Int> = withContext(Dispatchers.IO) {
        runGoogleOperation("drive-delete", "Google maintenance") {
            val ticket = captureMaintenance(NativeGoogleMaintenance.Kind.DRIVE)
            verifyAndFetchProfile(accessToken)
            val report = performMaintenance(null, accessToken, ticket)
            check(report.complete) { "Partial Google deletion: ${report.completed}; remote outcome uncertain" }
            report.completed
        }
    }

    suspend fun deleteManagedCalendars(accessToken: String): Result<Int> = withContext(Dispatchers.IO) {
        runGoogleOperation("calendar-delete", "Google maintenance") {
            val ticket = captureMaintenance(NativeGoogleMaintenance.Kind.CALENDAR)
            verifyAndFetchProfile(accessToken)
            val report = performMaintenance(null, accessToken, ticket)
            check(report.complete) { "Partial Google deletion: ${report.completed}; remote outcome uncertain" }
            report.completed
        }
    }

    private suspend fun deleteDriveInternal(token: String, ticket: NativeGoogleMaintenance.Request): NativeGoogleMaintenance.Report {
        requireMaintenance(ticket)
        val files = appFiles(token, "name contains 'repforge-sync-' or name contains 'trainpilot-photo-'")
        val ids = (0 until files.length()).map { files.getJSONObject(it) }
            .filter { APP_FILE_REGEX.matches(it.optString("name")) }.map { it.getString("id") }
        return NativeGoogleMaintenance.deleteSequentially(ids, { requireMaintenance(ticket) }) { id ->
            try { request(token, "DELETE", "https://www.googleapis.com/drive/v3/files/${enc(id)}", null) }
            catch (e: ApiError) { if (e.code != 404 && e.code != 410) throw e }
        }
    }

    private suspend fun deleteCalendarsInternal(token: String, ticket: NativeGoogleMaintenance.Request): NativeGoogleMaintenance.Report {
        var page = ""
        val ids = ArrayList<String>()
        do {
            requireMaintenance(ticket)
            val response = request(token, "GET", "https://www.googleapis.com/calendar/v3/users/me/calendarList?maxResults=250" +
                if (page.isEmpty()) "" else "&pageToken=${enc(page)}", null)
            val rows = response.optJSONArray("items") ?: JSONArray()
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                if (row.optString("description") == MANAGED_CALENDAR_DESC && row.optString("accessRole") == "owner") ids.add(row.getString("id"))
            }
            page = response.optString("nextPageToken")
        } while (page.isNotEmpty())
        val report = NativeGoogleMaintenance.deleteSequentially(ids, { requireMaintenance(ticket) }) { id ->
            try { request(token, "DELETE", "https://www.googleapis.com/calendar/v3/calendars/${enc(id)}", null) }
            catch (e: ApiError) { if (e.code != 404 && e.code != 410) throw e }
        }
        try {
            requireMaintenance(ticket)
            NativeDataReplacement.mutate(appContext) {
                ticket.dataset.requireCurrent(appContext)
                check(prefs().edit().remove("calendar").commit())
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { return report.copy(complete = false) }
        return report
    }

    // =========================================================================
    // #18 Multi-Device Cloud Merge Engine (Exact Port of www/index.html #18)
    // =========================================================================

    internal fun selectSnapshots18(files: JSONArray): Pair<List<JSONObject>, List<JSONObject>> {
        val groups = LinkedHashMap<String, MutableList<JSONObject>>()
        for (i in 0 until files.length()) {
            val f = files.optJSONObject(i) ?: continue
            val m = SNAPSHOT_NAME_REGEX.find(f.optString("name", "")) ?: continue
            val deviceId = m.groupValues[1]
            groups.getOrPut(deviceId) { ArrayList() }.add(f)
        }
        val current = ArrayList<JSONObject>()
        val oldest = ArrayList<JSONObject>()
        val recent = ArrayList<JSONObject>()
        for (rows in groups.values) {
            rows.sortBy { it.optString("createdTime", "") }
            if (rows.isEmpty()) continue
            current.add(rows.last())
            val older = rows.subList(0, rows.size - 1)
            if (older.isNotEmpty()) {
                oldest.add(older.first())
                for (j in older.size - 1 downTo 1) {
                    recent.add(older[j])
                }
            }
        }
        current.sortBy { it.optString("createdTime", "") }
        val archive = ArrayList<JSONObject>()
        val seen = current.map { it.optString("id") }.toMutableSet()
        for (f in oldest + recent) {
            val id = f.optString("id")
            if (id.isEmpty() || !seen.add(id)) continue
            archive.add(f)
            if (archive.size >= 48) break
        }
        return current to archive
    }

    internal fun mergeSync18(
        local: JSONObject,
        remotes: List<JSONObject>,
        base: JSONObject?,
        archives: List<JSONObject>,
        deletedHistoryIds: Set<String>,
        deletedHistoryKeys: Set<String>,
        preferLocalOnConflict: Boolean
    ): JSONObject {
        val out = JSONObject(local.toString())
        fun readWeightMarkers(data: JSONObject?): List<NativeWeightTombstones18.Marker> =
            if (data == null || !data.has(NativeWeightTombstones18.KEY)) emptyList()
            else NativeWeightTombstones18.decode(data.get(NativeWeightTombstones18.KEY))
        val mergedWeightMarkers = NativeWeightTombstones18.union(readWeightMarkers(local), readWeightMarkers(base),
            *remotes.map(::readWeightMarkers).toTypedArray(), *archives.map(::readWeightMarkers).toTypedArray())
        out.put(NativeWeightTombstones18.KEY, NativeWeightTombstones18.encode(mergedWeightMarkers))
        out.put(
            "history",
            mergeHistoryRows18(
                local.optJSONArray("history"),
                remotes.map { it.optJSONArray("history") },
                base?.optJSONArray("history"),
                archives.map { it.optJSONArray("history") },
                deletedHistoryIds,
                deletedHistoryKeys,
                preferLocalOnConflict
            )
        )
        out.put(
            "weights",
            mergeWeightRows18(
                local.optJSONArray("weights"),
                remotes.map { it.optJSONArray("weights") },
                base?.optJSONArray("weights"),
                archives.map { it.optJSONArray("weights") },
                preferLocalOnConflict, NativeWeightTombstones18.Index(mergedWeightMarkers)
            )
        )
        out.put(
            "scheduled",
            mergeScheduledRows18(
                local.optJSONArray("scheduled"),
                remotes.map { it.optJSONArray("scheduled") },
                preferLocalOnConflict
            )
        )
        for (key in arrayOf("settings", "exercises", "plan", "programs", "activeProgramId", "plannerSettings", "exerciseFavorites")) {
            var selected = local.opt(key)
            for (remote in remotes) {
                val x = remote.opt(key) ?: continue
                if (LegacySyncIdentity18.canonical(selected) == LegacySyncIdentity18.canonical(x)) continue
                val baseVal = base?.opt(key)
                if (baseVal != null && LegacySyncIdentity18.canonical(x) == LegacySyncIdentity18.canonical(baseVal)) continue
                selected = if (baseVal != null && LegacySyncIdentity18.canonical(selected) == LegacySyncIdentity18.canonical(baseVal)) {
                    x
                } else if (preferLocalOnConflict) {
                    selected
                } else {
                    x
                }
            }
            if (selected != null) out.put(key, selected)
        }
        return out
    }

    private fun mergeHistoryVariant18(a: JSONObject, b: JSONObject): JSONObject? {
        if (LegacySyncIdentity18.workoutIdentity(a) != LegacySyncIdentity18.workoutIdentity(b)) return null
        val merged = JSONObject(a.toString())
        val incoming = JSONObject(b.toString())
        val keys = incoming.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = incoming.opt(k)
            if (k == "id" || k == "photos" || v == null || v === JSONObject.NULL || v.toString().isEmpty()) continue
            if (k == "feedback" && merged.optJSONObject("feedback") != null && v is JSONObject) {
                val at = merged.getJSONObject("feedback").optLong("updatedAt", 0L)
                val bt = v.optLong("updatedAt", 0L)
                if (bt < at) continue
            }
            if (k == "health240" && merged.optJSONObject("health240") != null && v is JSONObject) {
                val at = runCatching { Instant.parse(merged.getJSONObject("health240").optString("syncedAt")).toEpochMilli() }.getOrDefault(0L)
                val bt = runCatching { Instant.parse(v.optString("syncedAt")).toEpochMilli() }.getOrDefault(0L)
                if (bt < at) continue
            }
            merged.put(k, v)
        }
        val aPhotos = a.optJSONArray("photos")
        val bPhotos = b.optJSONArray("photos")
        if (aPhotos != null || bPhotos != null) {
            val byId = LinkedHashMap<String, JSONObject>()
            for (arr in listOfNotNull(aPhotos, bPhotos)) {
                for (i in 0 until arr.length()) {
                    val p = arr.optJSONObject(i) ?: continue
                    val pid = p.optString("id")
                    if (pid.isEmpty()) continue
                    val old = byId[pid]
                    if (old == null) {
                        byId[pid] = JSONObject(p.toString())
                    } else {
                        val pUpd = p.optLong("updatedAt", 0L)
                        val oUpd = old.optLong("updatedAt", 0L)
                        val newer = if (pUpd >= oUpd) p else old
                        val combined = JSONObject(old.toString())
                        val pKeys = newer.keys()
                        while (pKeys.hasNext()) {
                            val pk = pKeys.next()
                            combined.put(pk, newer.get(pk))
                        }
                        val driveId = p.optString("driveFileId").ifEmpty { old.optString("driveFileId") }
                        if (driveId.isNotEmpty()) combined.put("driveFileId", driveId)
                        val delAt = listOf(p.optString("deletedAt"), old.optString("deletedAt"))
                            .filter { it.isNotEmpty() && it != "null" && it != "false" && it != "0" }
                            .maxOrNull()
                        if (delAt != null) combined.put("deletedAt", delAt)
                        combined.put("updatedAt", max(pUpd, oUpd))
                        byId[pid] = combined
                    }
                }
            }
            val sortedPhotos = byId.values.sortedBy { it.optString("createdAt", "") }
            val photosOut = JSONArray()
            sortedPhotos.forEach { photosOut.put(it) }
            merged.put("photos", photosOut)
        }
        val realId = listOf(a.optString("id"), b.optString("id"))
            .firstOrNull { it.isNotEmpty() && !LegacySyncIdentity18.isSyntheticId("history", it) }
        merged.put("id", realId ?: LegacySyncIdentity18.legacyHistoryId(merged))
        return merged
    }

    private fun mergeHistoryRows18(
        localRows: JSONArray?,
        remoteSets: List<JSONArray?>,
        baseRows: JSONArray?,
        archiveSets: List<JSONArray?>,
        deletedIds: Set<String>,
        deletedKeys: Set<String>,
        preferLocal: Boolean
    ): JSONArray {
        val items = ArrayList<JSONObject>()
        fun isDeleted(x: JSONObject): Boolean {
            val id = x.optString("id")
            val legacy = LegacySyncIdentity18.legacyHistoryId(x)
            val key = x.optString("syncId").ifEmpty { x.optString("key").ifEmpty { x.optString("started") } }
            return (id.isNotEmpty() && id in deletedIds) ||
                (legacy in deletedIds) ||
                (key.isNotEmpty() && key in deletedKeys)
        }

        fun add(src: JSONObject?, archiveOnly: Boolean) {
            if (src == null) return
            val x = JSONObject(src.toString())
            if (isDeleted(x)) return
            val xId = x.optString("id")
            if (archiveOnly && xId.isNotEmpty()) return
            if (xId.isNotEmpty()) {
                val byId = items.indexOfFirst { it.optString("id") == xId }
                if (byId >= 0) {
                    if (!archiveOnly) {
                        val mergedVariant = mergeHistoryVariant18(items[byId], x)
                        items[byId] = mergedVariant ?: if (preferLocal) items[byId] else x
                    }
                    return
                }
            }
            val sameIdx = items.indexOfFirst {
                LegacySyncIdentity18.workoutIdentity(it) == LegacySyncIdentity18.workoutIdentity(x)
            }
            if (sameIdx >= 0) {
                items[sameIdx] = mergeHistoryVariant18(items[sameIdx], x) ?: items[sameIdx]
                return
            }
            items.add(x)
        }

        if (localRows != null) for (i in 0 until localRows.length()) add(localRows.optJSONObject(i), false)
        for (rows in remoteSets) if (rows != null) for (i in 0 until rows.length()) add(rows.optJSONObject(i), false)
        if (baseRows != null) for (i in 0 until baseRows.length()) add(baseRows.optJSONObject(i), true)
        for (rows in archiveSets) if (rows != null) for (i in 0 until rows.length()) add(rows.optJSONObject(i), true)

        // Ensure unique legacy IDs
        val used = HashMap<String, String>()
        for (item in items) {
            val id = item.optString("id")
            if (id.isNotEmpty() && !LegacySyncIdentity18.isSyntheticId("history", id)) {
                used[id] = LegacySyncIdentity18.canonical(item)
            }
        }
        for (item in items) {
            val existingId = item.optString("id")
            if (existingId.isNotEmpty() && !LegacySyncIdentity18.isSyntheticId("history", existingId)) continue
            val root = LegacySyncIdentity18.legacyHistoryId(item)
            var id = root
            var n = 1
            val body = LegacySyncIdentity18.workoutIdentity(item)
            while (used.containsKey(id) && used[id] != body) {
                id = "$root-${++n}"
            }
            item.put("id", id)
            used[id] = body
        }
        items.sortByDescending { it.optString("started", "") }
        val out = JSONArray()
        items.forEach { out.put(it) }
        return out
    }

    private fun mergeWeightRows18(
        localRows: JSONArray?,
        remoteSets: List<JSONArray?>,
        baseRows: JSONArray?,
        archiveSets: List<JSONArray?>,
        preferLocal: Boolean,
        deletions: NativeWeightTombstones18.Index
    ): JSONArray {
        val items = ArrayList<JSONObject>()
        fun add(src: JSONObject?, archiveOnly: Boolean) {
            if (src == null) return
            if (deletions.deleted(src)) return
            val x = JSONObject(src.toString())
            val xId = x.optString("id")
            if (archiveOnly && xId.isNotEmpty()) return
            if (xId.isNotEmpty()) {
                val byId = items.indexOfFirst { it.optString("id") == xId }
                if (byId >= 0) {
                    if (!archiveOnly && !preferLocal) items[byId] = x
                    return
                }
            }
            val sameIdx = items.indexOfFirst {
                LegacySyncIdentity18.weightIdentity(it) == LegacySyncIdentity18.weightIdentity(x)
            }
            if (sameIdx >= 0) {
                val real = listOf(items[sameIdx].optString("id"), xId)
                    .firstOrNull { it.isNotEmpty() && !LegacySyncIdentity18.isSyntheticId("weights", it) }
                val merged = if (preferLocal) items[sameIdx] else x
                merged.put("id", real ?: LegacySyncIdentity18.legacyWeightId(merged))
                items[sameIdx] = merged
                return
            }
            if (xId.isEmpty() || LegacySyncIdentity18.isSyntheticId("weights", xId)) {
                x.put("id", LegacySyncIdentity18.legacyWeightId(x))
            }
            items.add(x)
        }
        if (localRows != null) for (i in 0 until localRows.length()) add(localRows.optJSONObject(i), false)
        for (rows in remoteSets) if (rows != null) for (i in 0 until rows.length()) add(rows.optJSONObject(i), false)
        if (baseRows != null) for (i in 0 until baseRows.length()) add(baseRows.optJSONObject(i), true)
        for (rows in archiveSets) if (rows != null) for (i in 0 until rows.length()) add(rows.optJSONObject(i), true)
        items.sortByDescending { it.optString("date", "") }
        val out = JSONArray()
        items.forEach { out.put(it) }
        return out
    }

    private fun mergeScheduledRows18(
        localRows: JSONArray?,
        remoteSets: List<JSONArray?>,
        preferLocal: Boolean
    ): JSONArray {
        val map = LinkedHashMap<String, JSONObject>()
        if (localRows != null) {
            for (i in 0 until localRows.length()) {
                val x = localRows.optJSONObject(i) ?: continue
                val key = x.optString("id").ifEmpty { x.optString("scheduleId").ifEmpty { x.optString("start") } }
                if (key.isNotEmpty()) map[key] = JSONObject(x.toString())
            }
        }
        for (rows in remoteSets) {
            if (rows == null) continue
            for (i in 0 until rows.length()) {
                val x = rows.optJSONObject(i) ?: continue
                val key = x.optString("id").ifEmpty { x.optString("scheduleId").ifEmpty { x.optString("start") } }
                if (key.isEmpty()) continue
                val old = map[key]
                if (old == null) {
                    map[key] = JSONObject(x.toString())
                } else {
                    val oldUpd = old.optLong("updatedAt", 0L)
                    val newUpd = x.optLong("updatedAt", 0L)
                    map[key] = when {
                        oldUpd != newUpd -> if (oldUpd > newUpd) old else JSONObject(x.toString())
                        preferLocal -> old
                        else -> JSONObject(x.toString())
                    }
                }
            }
        }
        val sorted = map.values.sortedByDescending { it.optString("start", "") }
        val out = JSONArray()
        sorted.forEach { out.put(it) }
        return out
    }

    // =========================================================================
    // Low-Level HTTP & Drive / Calendar Transport
    // =========================================================================

    private fun verifyAndFetchProfile(accessToken: String, connecting: Boolean = false): JSONObject {
        val sub = prefs().getString("sub", "").orEmpty()
        val email = prefs().getString("email", "").orEmpty()
        if (!connecting) NativeGoogleAccountPolicy.requireConnected(sub, email)
        val profile = request(accessToken, "GET", "https://www.googleapis.com/oauth2/v3/userinfo", null)
        NativeGoogleAccountPolicy.requireMatchingProfile(sub, email, profile, connecting)
        return profile
    }

    private fun driveListInternal(accessToken: String): JSONArray {
        val all = JSONArray()
        var page = ""
        do {
            val q = enc("trashed = false and name contains 'repforge-sync-'")
            val fields = enc("nextPageToken,files(id,name,createdTime,version)")
            val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q&fields=$fields&pageSize=1000" +
                if (page.isEmpty()) "" else "&pageToken=${enc(page)}"
            val x = request(accessToken, "GET", url, null)
            val a = x.optJSONArray("files")
            if (a != null) {
                for (i in 0 until a.length()) all.put(a.get(i))
            }
            page = x.optString("nextPageToken")
        } while (page.isNotEmpty())
        return all
    }

    private fun driveReadInternal(accessToken: String, id: String, expectedOwner: String): JSONObject? {
        if (!FILE_ID_REGEX.matches(id)) throw IOException("Hibás fájlazonosító.")
        val meta = request(accessToken, "GET", "https://www.googleapis.com/drive/v3/files/$id?fields=name,version", null)
        if (!meta.optString("name").startsWith("repforge-sync-")) {
            throw IOException("Nem TrainPilot-kompatibilis mentés.")
        }
        val snap = request(accessToken, "GET", "https://www.googleapis.com/drive/v3/files/$id?alt=media", null)
        if ("RepForgeSync" != snap.optString("app") || snap.optInt("schema") != 1 || expectedOwner != snap.optString("owner")) {
            throw IOException("Nem TrainPilot-kompatibilis mentés.")
        }
        return snap.optJSONObject("data")
    }

    private fun writeSnapshotInternal(accessToken: String, owner: String, device: String, parsed: JSONObject) {
        val data = parsed.toString()
        if ("RepForgeSync" != parsed.optString("app") ||
            owner != parsed.optString("owner") ||
            data.toByteArray(StandardCharsets.UTF_8).size > LIMIT
        ) {
            throw IOException("Érvénytelen felhőmentés.")
        }
        if (!UUID_REGEX.matches(device)) throw IOException("Hibás eszközazonosító.")
        val meta = JSONObject()
            .put("name", "repforge-sync-$device-${UUID.randomUUID()}.json")
            .put("parents", JSONArray().put("appDataFolder"))
            .put("mimeType", "application/json")
        val boundary = "repforge" + UUID.randomUUID().toString().replace("-", "")
        val body = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$meta\r\n" +
            "--$boundary\r\nContent-Type: application/json\r\n\r\n$data\r\n--$boundary--\r\n"
        val created = http(
            accessToken,
            "POST",
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,version",
            body,
            "multipart/related; boundary=$boundary"
        )
        val read = request(
            accessToken,
            "GET",
            "https://www.googleapis.com/drive/v3/files/${created.getString("id")}?alt=media",
            null
        )
        if (read.toString() != parsed.toString()) {
            throw IOException("A felhőmentés visszaellenőrzése nem sikerült.")
        }
    }

    private fun pruneSnapshotsInternal(accessToken: String, owner: String, device: String): JSONObject {
        if (!UUID_REGEX.matches(device)) throw IOException("Invalid device ID.")
        val files = appFiles(accessToken, "name contains 'repforge-sync-$device-'")
        val rows = ArrayList<JSONObject>()
        for (i in 0 until files.length()) {
            val f = files.getJSONObject(i)
            if (APP_FILE_REGEX.matches(f.optString("name"))) rows.add(f)
        }
        rows.sortBy { it.optString("createdTime") }
        var deleted = 0
        var protectedLegacy = 0
        // Keep the oldest safety snapshot (index 0) and the latest ten.
        for (i in 1 until rows.size - 10) {
            val f = rows[i]
            val snapshot = request(
                accessToken,
                "GET",
                "https://www.googleapis.com/drive/v3/files/${enc(f.getString("id"))}?alt=media",
                null
            )
            if ("RepForgeSync" != snapshot.optString("app") ||
                owner != snapshot.optString("owner") ||
                device != snapshot.optString("device")
            ) {
                continue
            }
            val data = snapshot.optJSONObject("data")
            var legacy = data == null
            for (key in arrayOf("history", "weights")) {
                val a = data?.optJSONArray(key)
                if (a == null) {
                    legacy = true
                    continue
                }
                for (j in 0 until a.length()) {
                    val id = a.getJSONObject(j).optString("id")
                    if (LegacySyncIdentity18.isLegacyProtectedId(id)) legacy = true
                }
            }
            if (legacy) {
                protectedLegacy++
                continue
            }
            request(accessToken, "DELETE", "https://www.googleapis.com/drive/v3/files/${enc(f.getString("id"))}", null)
            deleted++
        }
        return JSONObject().put("deleted", deleted).put("protectedLegacy", protectedLegacy)
    }

    private fun photoName(id: String): String = "trainpilot-photo-$id.jpg"

    private fun md5(f: File): String {
        val d = MessageDigest.getInstance("MD5")
        FileInputStream(f).use { input ->
            val b = ByteArray(8192)
            while (true) {
                val n = input.read(b)
                if (n == -1) break
                d.update(b, 0, n)
            }
        }
        val s = StringBuilder()
        for (x in d.digest()) s.append(String.format(Locale.US, "%02x", x.toInt() and 255))
        return s.toString()
    }

    private fun writeDrivePhotoInternal(accessToken: String, id: String, f: File): String {
        val name = photoName(id)
        val q = enc("trashed = false and name = '$name'")
        val existing = request(
            accessToken,
            "GET",
            "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q&fields=${enc("files(id,name,size,md5Checksum)")}&pageSize=2",
            null
        )
        val files = existing.optJSONArray("files")
        if (files != null && files.length() > 0) {
            val x = files.getJSONObject(0)
            if (x.optLong("size", -1L) == f.length() && md5(f).equals(x.optString("md5Checksum"), ignoreCase = true)) {
                return x.getString("id")
            }
        }
        val meta = JSONObject()
            .put("name", name)
            .put("parents", JSONArray().put("appDataFolder"))
            .put("mimeType", "image/jpeg")
        val boundary = "trainpilot" + UUID.randomUUID().toString().replace("-", "")
        val c = (URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,size,md5Checksum").openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 45_000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            setChunkedStreamingMode(8192)
        }
        try {
            c.outputStream.use { o ->
                FileInputStream(f).use { input ->
                    o.write(
                        "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$meta\r\n--$boundary\r\nContent-Type: image/jpeg\r\n\r\n"
                            .toByteArray(StandardCharsets.UTF_8)
                    )
                    val b = ByteArray(8192)
                    while (true) {
                        val n = input.read(b)
                        if (n == -1) break
                        o.write(b, 0, n)
                    }
                    o.write("\r\n--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8))
                }
            }
            val status = c.responseCode
            if (status !in 200..299) throw apiError(c, status)
            val created = JSONObject(String(readResponse(c, LIMIT), StandardCharsets.UTF_8))
            if (name != created.optString("name") ||
                created.optLong("size", -1L) != f.length() ||
                !md5(f).equals(created.optString("md5Checksum"), ignoreCase = true)
            ) {
                throw IOException("A fotó Drive-ellenőrzése nem sikerült.")
            }
            return created.getString("id")
        } finally {
            c.disconnect()
        }
    }

    private fun readDrivePhotoInternal(accessToken: String, pid: String, driveFileId: String, destFile: File) {
        val name = photoName(pid)
        val meta = request(
            accessToken,
            "GET",
            "https://www.googleapis.com/drive/v3/files/$driveFileId?fields=name,size,mimeType,md5Checksum",
            null
        )
        val size = meta.optLong("size", -1L)
        if (name != meta.optString("name") || "image/jpeg" != meta.optString("mimeType") || size <= 0L || size > PHOTO_LIMIT) {
            throw IOException("Nem érvényes TrainPilot-fotó.")
        }
        val bytes = download(accessToken, "https://www.googleapis.com/drive/v3/files/$driveFileId?alt=media", PHOTO_LIMIT)
        NativeCloudPhotoStage.installDownload(pid, meta, bytes, destFile)
    }

    private fun deleteDrivePhotoInternal(accessToken: String, pid: String, driveFileId: String) {
        val name = photoName(pid)
        try {
            val meta = request(accessToken, "GET", "https://www.googleapis.com/drive/v3/files/$driveFileId?fields=name", null)
            if (name != meta.optString("name")) throw IOException("Nem ehhez a naplófotóhoz tartozó Drive-fájl.")
            request(accessToken, "DELETE", "https://www.googleapis.com/drive/v3/files/$driveFileId", null)
        } catch (e: ApiError) {
            if (e.code != 404 && e.code != 410) throw e
        }
    }

    private suspend fun ensureManagedCalendar(accessToken: String, dataTicket: NativeDataReplacement.Continuation): String {
        var id = prefs().getString("calendar", "") ?: ""
        if (id.isNotEmpty()) {
            try {
                val c = request(accessToken, "GET", "https://www.googleapis.com/calendar/v3/calendars/${enc(id)}", null)
                if (MANAGED_CALENDAR_DESC != c.optString("description")) {
                    throw IOException("This is not a TrainPilot-managed calendar.")
                }
                return id
            } catch (e: ApiError) {
                if (e.code != 404 && e.code != 410) throw e
                NativeDataReplacement.mutate(appContext) {
                    dataTicket.requireCurrent(appContext)
                    check(prefs().edit().remove("calendar").commit()) { "A Google-naptár állapota nem menthető." }
                }
                id = ""
            }
        }
        var page = ""
        do {
            val r = request(
                accessToken,
                "GET",
                "https://www.googleapis.com/calendar/v3/users/me/calendarList?maxResults=250" +
                    if (page.isEmpty()) "" else "&pageToken=${enc(page)}",
                null
            )
            val a = r.optJSONArray("items")
            if (a != null) {
                for (i in 0 until a.length()) {
                    val c = a.getJSONObject(i)
                    if (MANAGED_CALENDAR_DESC == c.optString("description") && "owner" == c.optString("accessRole")) {
                        id = c.getString("id")
                    }
                }
            }
            page = r.optString("nextPageToken")
        } while (id.isEmpty() && page.isNotEmpty())

        if (id.isEmpty()) {
            val c = JSONObject()
                .put("summary", "TrainPilot edzések")
                .put("description", MANAGED_CALENDAR_DESC)
                .put("timeZone", TimeZone.getDefault().id)
            id = request(accessToken, "POST", "https://www.googleapis.com/calendar/v3/calendars", c).getString("id")
        }
        NativeDataReplacement.mutate(appContext) {
            dataTicket.requireCurrent(appContext)
            check(prefs().edit().putString("calendar", id).commit()) { "A Google-naptár állapota nem menthető." }
        }
        return id
    }

    private fun appFiles(accessToken: String, query: String): JSONArray {
        val all = JSONArray()
        var page = ""
        do {
            val result = request(
                accessToken,
                "GET",
                "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=${enc("trashed = false and ($query)")}&fields=${enc("nextPageToken,files(id,name,createdTime)")}&pageSize=1000" +
                    if (page.isEmpty()) "" else "&pageToken=${enc(page)}",
                null
            )
            val files = result.optJSONArray("files")
            if (files != null) {
                for (i in 0 until files.length()) all.put(files.get(i))
            }
            page = result.optString("nextPageToken")
        } while (page.isNotEmpty())
        return all
    }

    private fun download(accessToken: String, url: String, limit: Int): ByteArray {
        var attempt = 0
        while (true) {
            val c = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 20_000
                readTimeout = 45_000
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            try {
                val status = c.responseCode
                if (status !in 200..299) {
                    val error = apiError(c, status)
                    if (attempt < 3 && GoogleRequestPolicy.transientStatus(status, error.reason)) {
                        val delay = GoogleRequestPolicy.delay(attempt, c.getHeaderField("Retry-After"), System.currentTimeMillis())
                        if (delay >= 0L) {
                            c.disconnect()
                            Thread.sleep(delay)
                            attempt++
                            continue
                        }
                    }
                    throw error
                }
                return readResponse(c, limit)
            } finally {
                c.disconnect()
            }
        }
    }

    private fun request(accessToken: String, method: String, url: String, body: JSONObject?): JSONObject {
        return http(accessToken, method, url, body?.toString(), "application/json; charset=UTF-8")
    }

    private fun http(accessToken: String, method: String, url: String, body: String?, type: String): JSONObject {
        val eventId = if (body != null && method == "POST" && type.startsWith("application/json")) {
            runCatching { JSONObject(body).optString("id") }.getOrDefault("")
        } else ""
        val retryable = GoogleRequestPolicy.canRetry(method, url, eventId)
        var attempt = 0
        while (true) {
            val c = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 20_000
                readTimeout = 30_000
                requestMethod = method
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (body != null) {
                    c.doOutput = true
                    c.setRequestProperty("Content-Type", type)
                    c.outputStream.use { o -> o.write(body.toByteArray(StandardCharsets.UTF_8)) }
                }
                val status = c.responseCode
                if (status !in 200..299) {
                    val error = apiError(c, status)
                    if (retryable && attempt < 3 && GoogleRequestPolicy.transientStatus(status, error.reason)) {
                        val delay = GoogleRequestPolicy.delay(attempt, c.getHeaderField("Retry-After"), System.currentTimeMillis())
                        if (delay >= 0L) {
                            c.disconnect()
                            Thread.sleep(delay)
                            attempt++
                            continue
                        }
                    }
                    if (status == 401) {
                        Identity.getAuthorizationClient(appContext)
                            .clearToken(ClearTokenRequest.builder().setToken(accessToken).build())
                    }
                    throw error
                }
                if (status == 204) return JSONObject()
                val text = String(readResponse(c, LIMIT), StandardCharsets.UTF_8)
                return if (text.isEmpty()) JSONObject() else JSONObject(text)
            } finally {
                c.disconnect()
            }
        }
    }

    private fun readResponse(c: HttpURLConnection, limit: Int): ByteArray {
        c.inputStream.use { input ->
            ByteArrayOutputStream().use { out ->
                val b = ByteArray(8192)
                while (true) {
                    val n = input.read(b)
                    if (n == -1) break
                    if (out.size() + n > limit) throw IOException("A válasz túl nagy.")
                    out.write(b, 0, n)
                }
                return out.toByteArray()
            }
        }
    }

    private fun enc(x: String): String = URLEncoder.encode(x, "UTF-8")

    private class ApiError(val code: Int, val reason: String) : IOException("HTTP $code ($reason)")

    private fun apiError(connection: HttpURLConnection, code: Int): ApiError {
        var reason = ""
        try {
            connection.errorStream?.use { input ->
                ByteArrayOutputStream().use { out ->
                    val b = ByteArray(1024)
                    while (true) {
                        val n = input.read(b)
                        if (n == -1 || out.size() + n > 8192) break
                        out.write(b, 0, n)
                    }
                    val error = JSONObject(out.toString("UTF-8")).optJSONObject("error")
                    val details = error?.optJSONArray("errors")
                    if (details != null && details.length() > 0) {
                        reason = details.getJSONObject(0).optString("reason")
                    }
                }
            }
        } catch (_: Exception) {
        }
        return ApiError(code, reason)
    }

    private suspend fun formatError(e: Exception): String {
        val lang = runCatching { settingsStore.current().language }.getOrDefault(Locale.getDefault().language)
        val network = GoogleRequestPolicy.networkMessage(e, lang)
        if (network != null) return network
        if (e is ApiError) return apiMessage(e, lang)
        return e.message ?: "A Google-művelet nem sikerült."
    }

    private fun apiMessage(error: ApiError, lang: String): String {
        val messages = when {
            error.code == 401 -> arrayOf(
                "A Google-engedély lejárt vagy visszavonták. Indíts kézi szinkront az új engedélykéréshez.",
                "Google access expired or was revoked. Run manual sync to request access again.",
                "Google-Zugriff ist abgelaufen oder widerrufen. Manuell synchronisieren, um erneut Zugriff anzufordern.",
                "Accesul Google a expirat sau a fost revocat. Pornește sincronizarea manuală pentru o nouă autorizare."
            )
            error.reason == "storageQuotaExceeded" -> arrayOf(
                "A Google Drive tárhelye megtelt. Szabadíts fel helyet, majd indíts kézi szinkront.",
                "Google Drive storage is full. Free some space, then run manual sync.",
                "Google Drive ist voll. Speicher freigeben und manuell synchronisieren.",
                "Spațiul Google Drive este plin. Eliberează spațiu și sincronizează manual."
            )
            error.code == 429 || GoogleRequestPolicy.transientStatus(error.code, error.reason) -> arrayOf(
                "A Google átmenetileg nem fogadja a kérést. Várj, majd próbáld újra; a helyi adatok megmaradnak.",
                "Google is temporarily refusing requests. Wait and retry; local data remains available.",
                "Google lehnt Anfragen vorübergehend ab. Warten und erneut versuchen; lokale Daten bleiben erhalten.",
                "Google refuză temporar cererile. Așteaptă și reîncearcă; datele locale rămân disponibile."
            )
            error.code == 403 -> arrayOf(
                "A Google nem engedélyezte ezt a műveletet. Ellenőrizd az engedélyt, az API/OAuth beállítást és a tanúsítványt.",
                "Google did not permit this operation. Check permission, API/OAuth configuration and the signing certificate.",
                "Google erlaubt diesen Vorgang nicht. Berechtigung, API/OAuth-Einstellung und Zertifikat prüfen.",
                "Google nu a permis operațiunea. Verifică permisiunea, configurația API/OAuth și certificatul."
            )
            else -> arrayOf(
                "A Google-művelet nem sikerült.",
                "The Google operation failed.",
                "Der Google-Vorgang ist fehlgeschlagen.",
                "Operațiunea Google a eșuat."
            )
        }
        val idx = when (lang) {
            "hu" -> 0
            "de" -> 2
            "ro" -> 3
            else -> 1
        }
        return "${messages[idx]} (${error.code})"
    }
}
