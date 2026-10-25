package com.repforge.app

import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.OpenableColumns
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.shared.GpsRouteLedger193
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Arrays
import java.util.Locale
import java.util.TreeSet
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Consolidates BackupArchivePlugin and NativeFilesPlugin into a 100% native manager.
 * Handles JSON & ZIP exports with read-back verification, 2-phase atomic photo & HealthJournal
 * restore with rollback journals, post-restore GPS route pruning (#163/#193), and full local erasure.
 */
class BackupRestoreManager(context: Context) {
    companion object {
        const val JSON_LIMIT: Int = 20 * 1024 * 1024
        const val PHOTO_LIMIT: Int = 6 * 1024 * 1024
        const val MAX_ENTRIES: Int = 5002
        const val ARCHIVE_LIMIT: Long = 500L * 1024L * 1024L
        private val UUID_REGEX = Regex("[a-f0-9-]{36}")
        private val ROUTE_KEY_REGEX = Regex("^[A-Za-z0-9:._|+\\-]{1,200}$")
        internal fun photoDeleted(photo: JSONObject): Boolean = photo.has("deletedAt") &&
            !photo.isNull("deletedAt") && photo.optLong("deletedAt", -1L) != 0L &&
            photo.opt("deletedAt") != false && photo.optString("deletedAt").isNotBlank()
    }

    private val appContext = context.applicationContext
    private val database = TrainPilotDatabase.getInstance(appContext)
    private val settingsStore = TrainPilotSettingsStore(appContext)
    private val migrator = LegacyWebViewMigrator(appContext, database, settingsStore)
    internal var restoreCheckpoint: ((String) -> Unit)? = null

    private fun mkdir(f: File) {
        if (!f.isDirectory && !f.mkdirs()) throw IOException("Cannot create backup directory.")
    }

    private fun transactionsDir(): File {
        val d = File(appContext.noBackupFilesDir, "archive_transactions")
        mkdir(d)
        return d
    }

    private fun photosDir(): File {
        val d = File(appContext.filesDir, "workout_photos")
        mkdir(d)
        return d
    }

    private fun transactionDir(id: String): File {
        if (!UUID_REGEX.matches(id)) throw IOException("Invalid archive token.")
        return File(transactionsDir(), id)
    }

    private fun removeRecursive(f: File) {
        if (f.isDirectory) {
            val files = f.listFiles() ?: throw IOException("Cannot read backup directory.")
            for (x in files) removeRecursive(x)
        }
        if (f.exists() && !f.delete()) throw IOException("Cannot remove backup file.")
    }

    private fun readLimited(f: File, maxBytes: Int): ByteArray {
        FileInputStream(f).use { input ->
            ByteArrayOutputStream().use { out ->
                ArchiveIO.copy(input, out, maxBytes.toLong())
                return out.toByteArray()
            }
        }
    }

    private fun writeSynced(f: File, data: ByteArray) {
        FileOutputStream(f).use { out ->
            out.write(data)
            out.fd.sync()
        }
    }

    private fun photoIds(data: JSONObject): Set<String> {
        val ids = TreeSet<String>()
        if (data.optJSONArray("history") == null) throw IOException("Missing workout history.")
        for (photo in NativeCloudPhotoStage.photoRows(data)) {
            if (photoDeleted(photo)) continue
            val id = photo.optString("id")
            if (!UUID_REGEX.matches(id)) throw IOException("Invalid photo reference.")
            ids.add(id)
        }
        if (ids.size > 5000) throw IOException("Too many photos.")
        return ids
    }

    /** Exports JSON backup to a user-selected SAF Uri and verifies the written bytes by reading back. */
    suspend fun exportJsonBackupToUri(uri: Uri): Result<Pair<String, Int>> = withContext(Dispatchers.IO) {
        NativeHealthBackupGate.begin()
        try {
            val settings = settingsStore.current()
            val healthSnapshot = if (settings.healthBackupConsent) {
                HealthJournalStore.get(appContext).exportSnapshot()
            } else null
            val jsonObj = migrator.exportBackupJson(settings.healthBackupConsent, healthSnapshot)
            val expectedBytes = jsonObj.toString().toByteArray(StandardCharsets.UTF_8)
            if (expectedBytes.isEmpty() || expectedBytes.size > JSON_LIMIT) {
                throw IOException("Érvénytelen vagy túl nagy mentés.")
            }
            appContext.contentResolver.openOutputStream(uri, "w").use { out ->
                if (out == null) throw IOException("Nem nyitható meg írásra.")
                out.write(expectedBytes)
                out.flush()
            }
            val actualBytes = readUriBytes(uri, JSON_LIMIT)
            if (actualBytes.isEmpty() || !Arrays.equals(expectedBytes, actualBytes)) {
                throw IOException("A visszaolvasott fájl eltér vagy üres.")
            }
            Result.success(resolveDisplayName(uri, "repforge-backup.json") to actualBytes.size)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (e: Exception) { Result.failure(e) }
        finally { NativeHealthBackupGate.end() }
    }

    /** Imports a JSON backup from a user-selected SAF Uri with full health & route reconciliation. */
    suspend fun importJsonBackupFromUri(uri: Uri, recoverInterrupted: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        NativeHealthBackupGate.begin()
        try {
            val bytes = readUriBytes(uri, JSON_LIMIT)
            val parsed = JSONObject(String(bytes, StandardCharsets.UTF_8))
            val data = if (parsed.optString("app") == "RepForgeSync") parsed.getJSONObject("data") else parsed
            photoIds(data) // Validate the history/photo envelope before acquiring the destructive lease.
            val operation: suspend () -> Unit = {
                val token = UUID.randomUUID().toString()
                val dir = transactionDir(token); mkdir(dir)
                writeSynced(File(dir, "backup.json"), data.toString().toByteArray(StandardCharsets.UTF_8))
                completeReplacement(NativeReplacementJournal.Entry("json", token, false))
            }
            if (recoverInterrupted) NativeDataReplacement.recover(appContext, operation)
            else NativeDataReplacement.exclusively(appContext, operation)
            Result.success(Unit)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { Result.failure(error) }
        finally { NativeHealthBackupGate.end() }
    }

    /** Reuse the same staged redo path for an already merged Drive payload, under its caller's lease. */
    internal suspend fun installCloudMerge(data: JSONObject, stagedPhotos: NativeCloudPhotoStage? = null) {
        check(NativeDataReplacement.blocked(appContext)) { "Cloud import requires the replacement lease" }
        val ids = photoIds(data)
        val token = UUID.randomUUID().toString()
        val dir = transactionDir(token); mkdir(dir)
        try {
            val bytes = data.toString().toByteArray(StandardCharsets.UTF_8)
            if (bytes.isEmpty() || bytes.size > JSON_LIMIT) throw IOException("Invalid cloud backup size.")
            if (stagedPhotos == null) {
                writeSynced(File(dir, "backup.json"), bytes)
                completeReplacement(NativeReplacementJournal.Entry("json", token, false))
            } else {
                stagedPhotos.requireUnchanged()
                val incoming = File(dir, "incoming"); mkdir(incoming)
                val photos = File(incoming, "photos"); mkdir(photos)
                writeSynced(File(incoming, "backup.json"), bytes)
                val hashes = JSONObject().put("backup.json", FileInputStream(File(incoming, "backup.json")).use { ArchiveIO.hash(it) })
                var total = bytes.size.toLong()
                for (id in ids) {
                    val target = File(photos, "$id.jpg")
                    FileInputStream(File(stagedPhotos.photos, "$id.jpg")).use { input -> FileOutputStream(target).use { out ->
                        total += ArchiveIO.copy(input, out, PHOTO_LIMIT.toLong()); out.fd.sync()
                    } }
                    if (total > ARCHIVE_LIMIT) throw IOException("Cloud photo archive is too large.")
                    hashes.put("photos/$id.jpg", stagedPhotos.verifiedCopyHash(id, target))
                }
                val manifest = JSONObject().put("format", "TrainPilotArchive").put("version", 1)
                    .put("cloudMerge", true).put("cloudPhotoDeletes", JSONArray(stagedPhotos.deletedPhotoIds.toList()))
                    .put("sha256", hashes).toString().toByteArray(StandardCharsets.UTF_8)
                if (manifest.size > JSON_LIMIT || total + manifest.size > ARCHIVE_LIMIT) throw IOException("Cloud manifest is too large.")
                writeSynced(File(incoming, "manifest.json"), manifest)
                completeReplacement(NativeReplacementJournal.Entry("zip", token, false))
                stagedPhotos.markInstalled()
            }
        } catch (error: Exception) {
            // Once a receipt exists, replay owns this directory and must keep its verified files.
            if (!NativeReplacementJournal(appContext).exists()) runCatching { removeRecursive(dir) }
            throw error
        }
    }

    /** Explicit replay of the staged, already selected backup; never an automatic unlock. */
    suspend fun resumeInterruptedReplacement(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entry = NativeReplacementJournal(appContext).read()
            NativeDataReplacement.recover(appContext) {
                if (entry.kind == "erase") eraseLocalContents(entry) else completeReplacement(entry)
            }
            Result.success(Unit)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { Result.failure(error) }
    }

    private suspend fun completeReplacement(entry: NativeReplacementJournal.Entry) {
        val journal = NativeReplacementJournal(appContext)
        val dir = transactionDir(requireNotNull(entry.token))
        if (!entry.committed) {
            val payload = File(dir, if (entry.kind == "zip") "incoming/manifest.json" else "backup.json")
            val payloadHash = FileInputStream(payload).use { ArchiveIO.hash(it) }
            check(entry.payloadSha256 == null || entry.payloadSha256 == payloadHash) { "A félbehagyott mentés sérült." }
            val durableEntry = entry.copy(payloadSha256 = payloadHash)
            val data = JSONObject(String(readLimited(File(dir, if (entry.kind == "zip") "incoming/backup.json" else "backup.json"), JSON_LIMIT), StandardCharsets.UTF_8))
            photoIds(data)
            var cloudDeletes: Set<String> = emptySet()
            if (entry.kind == "zip") {
                // Recheck staged checksums during replay as well as on the original import.
                val incoming = File(dir, "incoming")
                val manifest = JSONObject(String(readLimited(File(incoming, "manifest.json"), JSON_LIMIT), StandardCharsets.UTF_8))
                check(manifest.optString("format") == "TrainPilotArchive" && manifest.optInt("version") == 1)
                val hashes = manifest.getJSONObject("sha256")
                val expected = setOf("backup.json", "manifest.json") + photoIds(data).map { "photos/$it.jpg" }
                val actual = incoming.walkTopDown().filter { it.isFile }.map { it.relativeTo(incoming).invariantSeparatorsPath }.toSet()
                ArchiveIO.verify(incoming, actual, expected, hashes.keys().asSequence().associateWith { hashes.getString(it) })
                if (manifest.opt("cloudMerge") == true) {
                    val rows = manifest.getJSONArray("cloudPhotoDeletes")
                    val requested = (0 until rows.length()).map { rows.getString(it) }
                    cloudDeletes = requested.toSet()
                    check(requested.size == cloudDeletes.size && cloudDeletes == NativeCloudPhotoStage.references(data)
                        .filter { it.deleted }.map { it.id }.toSet()) { "Invalid cloud photo deletion plan." }
                }
            }
            journal.write(durableEntry)
            restoreCheckpoint?.invoke("receipt-prepared")
            if (entry.kind == "zip") installPhotosTransactionally(dir, data, cloudDeletes)
            restoreCheckpoint?.invoke("photos-installed")
            applyValidatedRestoreData(data)
            restoreCheckpoint?.invoke("data-imported")
            journal.write(durableEntry.copy(committed = true))
            restoreCheckpoint?.invoke("receipt-committed")
        }
        removeRecursive(dir)
        journal.clear()
    }

    /** Exports a full ZIP archive (backup.json + photos/(image).jpg + manifest.json) and verifies SHA-256. */
    suspend fun exportZipArchiveToUri(uri: Uri): Result<Pair<String, Long>> = withContext(Dispatchers.IO) {
        NativeHealthBackupGate.begin()
        var tmp: File? = null
        try {
            val settings = settingsStore.current()
            val healthSnapshot = if (settings.healthBackupConsent) {
                HealthJournalStore.get(appContext).exportSnapshot()
            } else null
            val data = migrator.exportBackupJson(settings.healthBackupConsent, healthSnapshot)
            val jsonBytes = data.toString().toByteArray(StandardCharsets.UTF_8)
            if (jsonBytes.isEmpty() || jsonBytes.size > JSON_LIMIT) {
                throw IOException("Invalid backup size.")
            }
            val manifest = JSONObject()
                .put("format", "TrainPilotArchive")
                .put("version", 1)
            val checks = JSONObject()
            tmp = File.createTempFile("trainpilot-", ".zip", appContext.cacheDir)
            var total = jsonBytes.size.toLong()

            ZipOutputStream(BufferedOutputStream(FileOutputStream(tmp))).use { zip ->
                zip.putNextEntry(ZipEntry("backup.json"))
                zip.write(jsonBytes)
                zip.closeEntry()
                checks.put("backup.json", ArchiveIO.hash(ByteArrayInputStream(jsonBytes)))

                for (id in photoIds(data)) {
                    val photo = File(photosDir(), "$id.jpg")
                    if (!photo.isFile) throw IOException("A referenced photo is missing: $id")
                    if (photo.length() == 0L) throw IOException("A referenced photo is empty: $id")
                    if (photo.length() > PHOTO_LIMIT) throw IOException("A referenced photo exceeds 6 MB: $id")
                    total += photo.length()
                    if (total > ARCHIVE_LIMIT) throw IOException("Maximum 500 MB per archive.")
                    val name = "photos/$id.jpg"
                    zip.putNextEntry(ZipEntry(name))
                    val digest = MessageDigest.getInstance("SHA-256")
                    DigestInputStream(FileInputStream(photo), digest).use { input ->
                        ArchiveIO.copy(input, zip, PHOTO_LIMIT.toLong())
                    }
                    zip.closeEntry()
                    val checksum = StringBuilder()
                    for (b in digest.digest()) {
                        checksum.append(String.format(Locale.US, "%02x", b.toInt() and 255))
                    }
                    checks.put(name, checksum.toString())
                }

                manifest.put("sha256", checks)
                val manifestBytes = manifest.toString().toByteArray(StandardCharsets.UTF_8)
                if (total + manifestBytes.size > ARCHIVE_LIMIT) {
                    throw IOException("Maximum 500 MB expanded archive.")
                }
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifestBytes)
                zip.closeEntry()
            }

            appContext.contentResolver.openOutputStream(uri, "w").use { out ->
                if (out == null) throw IOException("Cannot write selected file.")
                FileInputStream(tmp).use { input ->
                    ArchiveIO.copy(input, out, ARCHIVE_LIMIT + JSON_LIMIT)
                }
                out.flush()
            }

            val actualHash = appContext.contentResolver.openInputStream(uri).use { input ->
                if (input == null) throw IOException("Cannot verify exported file.")
                ArchiveIO.hash(input)
            }
            val expectedHash = FileInputStream(tmp).use { ArchiveIO.hash(it) }
            if (expectedHash != actualHash) {
                throw IOException("Export verification failed.")
            }
            Result.success(resolveDisplayName(uri, "TrainPilot.zip") to tmp.length())
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (e: Exception) { Result.failure(e) }
        finally {
            tmp?.delete()
            NativeHealthBackupGate.end()
        }
    }

    /** Extracts, verifies, and atomically installs a full ZIP archive from a user-selected SAF Uri. */
    suspend fun importZipArchiveFromUri(uri: Uri, recoverInterrupted: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        NativeHealthBackupGate.begin()
        var stagingDir: File? = null
        try {
            // mkdir of archive_transactions may throw. Include *all* staging
            // preparation after begin() under finally, not only the ZIP extraction.
            val token = UUID.randomUUID().toString()
            val dir = transactionDir(token)
            stagingDir = dir
            mkdir(dir)
            val incoming = File(dir, "incoming")
            mkdir(incoming)
            val names = appContext.contentResolver.openInputStream(uri).use { raw ->
                if (raw == null) throw IOException("Cannot read selected archive.")
                ArchiveIO.extract(raw, incoming, JSON_LIMIT, PHOTO_LIMIT, ARCHIVE_LIMIT, MAX_ENTRIES)
            }
            val data = JSONObject(String(readLimited(File(incoming, "backup.json"), JSON_LIMIT), StandardCharsets.UTF_8))
            val manifest = JSONObject(String(readLimited(File(incoming, "manifest.json"), JSON_LIMIT), StandardCharsets.UTF_8))
            if ("TrainPilotArchive" != manifest.optString("format") || manifest.optInt("version") != 1) {
                throw IOException("Unknown archive format.")
            }
            val checks = manifest.getJSONObject("sha256")
            val expected = HashSet<String>().apply {
                add("backup.json")
                add("manifest.json")
                for (id in photoIds(data)) add("photos/$id.jpg")
            }
            val hashes = HashMap<String, String>()
            val keys = checks.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                hashes[k] = checks.getString(k)
            }
            ArchiveIO.verify(incoming, names, expected, hashes)

            val operation: suspend () -> Unit = { completeReplacement(NativeReplacementJournal.Entry("zip", token, false)) }
            if (recoverInterrupted) NativeDataReplacement.recover(appContext, operation)
            else NativeDataReplacement.exclusively(appContext, operation)
            Result.success(Unit)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            // Keep originals/journal if rollback itself failed; never erase recovery evidence.
            // transactionDir may have failed *before* a staging directory existed.
            val dir = stagingDir
            if (dir != null && !NativeReplacementJournal(appContext).exists() &&
                !File(dir, "journal.json").isFile
            ) {
                try { removeRecursive(dir) } catch (_: Exception) { }
            }
            Result.failure(e)
        } finally {
            NativeHealthBackupGate.end()
        }
    }

    private fun installPhotosTransactionally(dir: File, data: JSONObject, deleted: Set<String> = emptySet()) {
        val original = File(dir, "originals")
        mkdir(original)
        val priorJournal = File(dir, "journal.json")
        val touched = if (priorJournal.isFile) JSONArray(String(readLimited(priorJournal, JSON_LIMIT), StandardCharsets.UTF_8)) else JSONArray()
        fun rememberOriginal(id: String) {
            val dest = File(photosDir(), "$id.jpg")
            val alreadyTouched = (0 until touched.length()).any { touched.optString(it) == id }
            if (!alreadyTouched && dest.exists() && !File(original, "$id.jpg").exists()) {
                FileInputStream(dest).use { input -> FileOutputStream(File(original, "$id.jpg")).use { out ->
                    ArchiveIO.copy(input, out, PHOTO_LIMIT.toLong()); out.fd.sync()
                } }
            }
            if (!alreadyTouched) {
                touched.put(id)
                writeSynced(File(dir, "journal.json"), touched.toString().toByteArray(StandardCharsets.UTF_8))
            }
        }
        try {
            for (id in photoIds(data)) {
                val dest = File(photosDir(), "$id.jpg")
                val src = File(dir, "incoming/photos/$id.jpg")
                if (!src.isFile) throw IOException("Photo archive missing.")
                rememberOriginal(id)
                val tmp = File(photosDir(), "$id.restore")
                FileInputStream(src).use { input ->
                    FileOutputStream(tmp).use { out ->
                        ArchiveIO.copy(input, out, PHOTO_LIMIT.toLong())
                        out.fd.sync()
                    }
                }
                if (dest.exists() && !dest.delete()) throw IOException("Cannot replace photo.")
                if (!tmp.renameTo(dest)) throw IOException("Cannot install photo.")
                restoreCheckpoint?.invoke("photo-installed:$id")
            }
            for (id in deleted) {
                rememberOriginal(id)
                val dest = File(photosDir(), "$id.jpg")
                if (dest.exists() && !dest.delete()) throw IOException("Cannot remove deleted cloud photo.")
                restoreCheckpoint?.invoke("photo-deleted:$id")
            }
        } catch (e: Exception) {
            rollbackDir(dir)
            throw e
        }
    }

    private fun rollbackDir(dir: File) {
        val journal = File(dir, "journal.json")
        if (!journal.isFile) return
        val ids = JSONArray(String(readLimited(journal, JSON_LIMIT), StandardCharsets.UTF_8))
        val originals = File(dir, "originals")
        for (i in 0 until ids.length()) {
            val id = ids.getString(i)
            if (!UUID_REGEX.matches(id)) throw IOException("Invalid restore journal.")
            val target = File(photosDir(), "$id.jpg")
            val old = File(originals, "$id.jpg")
            if (old.exists()) {
                val temp = File(photosDir(), "$id.rollback")
                FileInputStream(old).use { input ->
                    FileOutputStream(temp).use { out ->
                        ArchiveIO.copy(input, out, PHOTO_LIMIT.toLong())
                        out.fd.sync()
                    }
                }
                if (target.exists() && !target.delete()) throw IOException("Cannot replace rolled-back photo.")
                if (!temp.renameTo(target)) throw IOException("Cannot restore original photo.")
            } else if (target.exists() && !target.delete()) {
                throw IOException("Cannot roll back new photo.")
            }
        }
        journal.delete()
    }

    private suspend fun applyValidatedRestoreData(data: JSONObject) {
        val previousWorkouts = database.workoutHistoryDao().getAllWorkoutsWithDetails()
        val healthJournal = data.optJSONObject("nativeHealthJournal")
        val healthStore = HealthJournalStore.get(appContext)
        var healthToken: String? = null

        val pendingHealthRestore = healthStore.pendingRestore()
        if (healthJournal != null || pendingHealthRestore.isNotEmpty()) {
            HealthBackgroundService.stop(appContext)
            (appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler)?.cancel(HealthBackgroundJob.ID)
            if (pendingHealthRestore.isNotEmpty()) healthStore.finishRestore(pendingHealthRestore, false)
            if (healthJournal != null) healthToken = healthStore.prepareRestore(healthJournal)
            restoreCheckpoint?.invoke("health-prepared")
        }

        try {
            if (healthToken != null) {
                healthStore.installRestore(healthToken)
                restoreCheckpoint?.invoke("health-installed")
            }
            migrator.importBackupJson(data, replaceAll = true)
            settingsStore.markMigrationComplete() // Never reimport the old WebView over an explicit restored backup.
            restoreCheckpoint?.invoke("room-settings-imported")
            if (healthToken != null) {
                healthStore.finishRestore(healthToken, true)
                restoreCheckpoint?.invoke("health-committed")
            }
        } catch (e: Exception) {
            if (healthToken != null) {
                try {
                    healthStore.finishRestore(healthToken, false)
                } catch (_: Exception) {
                }
            }
            throw e
        } finally {
            try {
                HealthBackgroundJob.schedule(appContext)
            } catch (_: Exception) {
            }
        }

        // #163 / #193: Prune local GPS route files that are no longer referenced after confirmed restore
        val restoredWorkouts = database.workoutHistoryDao().getAllWorkoutsWithDetails()
        pruneRoutesAfterRestore(previousWorkouts.map { it.workout.rawJson }, restoredWorkouts.map { it.workout.rawJson })
        WearSyncRepository.publishWatchHomeSnapshot(appContext)
    }

    private fun pruneRoutesAfterRestore(previousRawJsonList: List<String>, restoredRawJsonList: List<String>) {
        try {
            val previousObjs = previousRawJsonList.mapNotNull { runCatching { JSONObject(it) }.getOrNull() }
            val restoredObjs = restoredRawJsonList.mapNotNull { runCatching { JSONObject(it) }.getOrNull() }
            val keepKeys = extractRouteKeys(restoredObjs)
            val keepWorkoutIds = restoredObjs.map { it.optString("syncId").ifEmpty { it.optString("id").ifEmpty { it.optString("started") } } }.toSet()

            val ledger193 = GpsRouteLedger193.open(appContext.noBackupFilesDir)
            val routeStore163 = GpsRouteStore(appContext.noBackupFilesDir)

            for (prev in previousObjs) {
                val wid = prev.optString("syncId").ifEmpty { prev.optString("id").ifEmpty { prev.optString("started") } }
                if (prev.optInt("ownershipProtocol193") == 1 && wid.isNotEmpty() && wid !in keepWorkoutIds) {
                    runCatching { ledger193.deleteWorkout(wid) }
                }
            }
            for (key in extractRouteKeys(previousObjs)) {
                if (key !in keepKeys) {
                    if (key.startsWith("wear193:")) {
                        runCatching { ledger193.delete(key) }
                    } else {
                        runCatching { routeStore163.delete(key) }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun extractRouteKeys(workouts: List<JSONObject>): Set<String> {
        val keys = HashSet<String>()
        for (w in workouts) {
            val exercises = w.optJSONArray("exercises") ?: continue
            for (i in 0 until exercises.length()) {
                val sets = exercises.optJSONObject(i)?.optJSONArray("sets") ?: continue
                for (j in 0 until sets.length()) {
                    val s = sets.optJSONObject(j) ?: continue
                    for (field in arrayOf("gpsRouteKey163", "gpsRouteKey193")) {
                        val k = s.optString(field)
                        if (ROUTE_KEY_REGEX.matches(k)) keys.add(k)
                    }
                }
            }
        }
        return keys
    }

    /** Erases all local workouts, programs, weights, photos, GPS routes, BLE watch, and HealthJournal. */
    suspend fun eraseAllLocalData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            NativeDataReplacement.exclusively(appContext) {
                eraseLocalContents(NativeReplacementJournal.Entry("erase", null, false))
            }
            Result.success(Unit)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { Result.failure(error) }
    }

    private suspend fun eraseLocalContents(entry: NativeReplacementJournal.Entry) {
        val journal = NativeReplacementJournal(appContext)
        if (!entry.committed) {
            journal.write(entry)
                DistanceTrackingService.erase(appContext)
                HealthBackgroundService.stop(appContext)
                (appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler)?.cancel(HealthBackgroundJob.ID)
                HealthJournalStore.get(appContext).erase()
                if (!BleWatchPreference(appContext).clear()) {
                    throw IOException("Cannot clear selected watch.")
                }
                database.workoutHistoryDao().deleteAllWorkouts()
                database.workoutHistoryDao().deleteAllTombstones()
                database.bodyWeightDao().deleteAllWeights()
                database.calendarScheduleDao().deleteAllEntries()
                database.exerciseLibraryDao().deleteAllCustomExercises()
                database.programDao().deleteAllPrograms()
                database.keyValueDocumentDao().deleteAllDocuments()
                TrainPilotDatabase.ensureBuiltinCatalogSeeded(database, appContext)
                settingsStore.resetAllSettingsForEraseLocal()

                removeRecursive(photosDir())
                removeRecursive(transactionsDir())
                val gpsRoutesDir = File(appContext.noBackupFilesDir, "gps_routes")
                if (gpsRoutesDir.exists()) removeRecursive(gpsRoutesDir)
                val wearRoutesDir = File(appContext.noBackupFilesDir, "wear_routes193")
                if (wearRoutesDir.exists()) removeRecursive(wearRoutesDir)

                val backupCache = File(appContext.cacheDir, "backup_export")
                if (backupCache.exists()) removeRecursive(backupCache)
                appContext.cacheDir.listFiles()?.forEach { f ->
                    if (f.name.startsWith("trainpilot-")) removeRecursive(f)
                }
                withContext(Dispatchers.Main) {
                    runCatching { ActiveWorkoutStore.clear(appContext) }
                }
                WearSyncRepository.publishWatchHomeSnapshot(appContext)
            journal.write(entry.copy(committed = true))
        }
        journal.clear()
    }

    private fun readUriBytes(uri: Uri, limit: Int): ByteArray {
        appContext.contentResolver.openInputStream(uri).use { input ->
            if (input == null) throw IOException("Nem nyitható meg olvasásra.")
            ByteArrayOutputStream().use { out ->
                val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer)
                    if (n == -1) break
                    if (out.size() + n > limit) throw IOException("Maximum 20 MB.")
                    out.write(buffer, 0, n)
                }
                return out.toByteArray()
            }
        }
    }

    private fun resolveDisplayName(uri: Uri, fallback: String): String {
        try {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) return c.getString(0)
            }
        } catch (_: Exception) {
        }
        return fallback
    }

    fun createCalendarInsertIntent(title: String, description: String, startMs: Long, endMs: Long): Intent {
        if (startMs <= 0L || endMs <= startMs) throw IllegalArgumentException("Érvénytelen edzésidőpont.")
        return Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title.ifBlank { "RepForge edzés" })
            .putExtra(CalendarContract.Events.DESCRIPTION, description)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMs)
    }

    fun createVideoIntent(url: String): Intent {
        val uri = Uri.parse(url)
        val host = uri.host
        if ("https" != uri.scheme || !("www.youtube.com" == host || "www.muscleandstrength.com" == host)) {
            throw IllegalArgumentException("Nem támogatott videóhivatkozás.")
        }
        return Intent(Intent.ACTION_VIEW, uri)
    }
}
