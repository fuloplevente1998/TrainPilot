package com.repforge.app

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Real app SAF -> Room/DataStore -> erase -> restore -> re-export pipeline.
 * Instrumentation is restricted to a disposable DEBUG emulator by reset().
 * Synthetic data only; migration from an old signed APK is tested in the separate upgrade lane.
 */
@RunWith(AndroidJUnit4::class)
class NativeDataIntegrityArchiveTest {
    private val fixture = NativeLocalizationEmulatorFixture()
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val database: TrainPilotDatabase get() = TrainPilotDatabase.getInstance(context)
    private val manager get() = BackupRestoreManager(context)
    private val photoId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    private val photoBytes = ByteArray(2048) { ((it * 73 + 19) and 255).toByte() }

    @Before fun prepare() = fixture.reset()
    @After fun cleanup() {
        File(context.filesDir, "workout_photos/$photoId.jpg").delete()
        fixture.stopServices()
    }

    private fun sha(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun local(name: String) = File(context.cacheDir, "native-data-integrity-$name")
    private fun uri(file: File): Uri = Uri.fromFile(file)
    private fun writeJson(name: String, value: JSONObject): Uri =
        uri(local(name).apply { writeText(value.toString(), Charsets.UTF_8) })

    private fun synthetic(count: Int): JSONObject {
        val history = JSONArray()
        for (i in 0 until count) {
            val id = "integrity-%04d".format(java.util.Locale.US, i)
            val minute = "%02d".format(java.util.Locale.US, i % 60)
            val exercise = JSONObject()
                .put("id", "side-plank").put("name", "Oldalsó plank").put("repUnit", "mp/oldal")
                .put("sets", JSONArray().put(JSONObject()
                    .put("set", 1).put("setId", "$id:set:1").put("reps", "26")
                    .put("done", true).put("leftSeconds", 12).put("rightSeconds", 14)
                    .put("actualRir", 2).put("gpsRouteKey193", "synthetic-route-$i")))
            history.put(JSONObject()
                .put("id", id).put("syncId", id).put("key", "stable-$id")
                .put("started", "2026-10-21T09:$minute:00Z")
                .put("finished", "2026-10-21T09:$minute:30Z")
                .put("notes", "Árvíztűrő 🏋️ sor $i\nidézet \"$i\"")
                .put("originDevice", if (i % 2 == 0) "phone" else "watch")
                .put("exercises", JSONArray().put(exercise))
                .put("timeLogs162", JSONObject().put("totalSeconds", 30).put("pausedSeconds", 3))
                .put("activeIntervals", JSONArray().put(JSONObject().put("start", 1).put("end", 31)))
                .put("wear", JSONObject().put("heartRateAvg", 101).put("calories", 42))
                .put("health240", JSONObject().put("steps", 22).put("source", "synthetic")))
        }
        return JSONObject()
            .put("history", history)
            .put("programs", JSONArray().put(JSONObject()
                .put("id", "integrity-program").put("name", "Próba program")
                .put("hu", "Próba").put("en", "Test").put("de", "Test").put("ro", "Test")
                .put("builtin", false)
                .put("days", JSONArray().put(JSONObject()
                    .put("id", "A").put("name", "Nap A")
                    .put("exercises", JSONArray().put("side-plank"))))))
            .put("weights", JSONArray().put(JSONObject()
                .put("id", "integrity-weight").put("date", "2026-10-21").put("kg", 77.5)))
            .put("scheduled", JSONArray().put(JSONObject().put("id", "integrity-calendar")
                .put("date", "2026-10-21").put("start", "2026-10-21T10:00:00Z")
                .put("programId", "integrity-program").put("dayId", "A")))
            .put("phase3HistoryTombstones", JSONArray().put(JSONObject()
                .put("ids", JSONArray().put("permanently-deleted"))
                .put("key", "permanently-deleted").put("deletedAt", 1720000000000L)))
            .put("draft", JSONObject().put("resume", true).put("notes", "Folytatás 🏋️"))
            .put("plannerSettings", JSONObject().put("futureField", 17))
            .put("settings", JSONObject().put("language", "de").put("themeKey", "classicRose")
                .put("restSeconds", 45).put("workSeconds", 17).put("rounds", 4)
                .put("sound", false).put("vibration", true))
    }

    private fun semanticSignature(data: JSONObject, expected: Int): String {
        val history = data.getJSONArray("history")
        assertEquals(expected, history.length())
        val result = StringBuilder()
        val rows = (0 until history.length()).map { history.getJSONObject(it) }.sortedBy { it.getString("id") }
        for (i in 0 until expected) {
            val row = rows[i]
            val id = row.getString("id")
            assertEquals("integrity-%04d".format(java.util.Locale.US, i), id)
            assertEquals("stable-$id", row.getString("key"))
            assertTrue(row.getString("notes").contains("Árvíztűrő 🏋️"))
            val first = row.getJSONArray("exercises").getJSONObject(0)
                .getJSONArray("sets").getJSONObject(0)
            assertEquals(12, first.getInt("leftSeconds"))
            assertEquals(14, first.getInt("rightSeconds"))
            assertEquals(2, first.getInt("actualRir"))
            assertEquals("synthetic-route-$i", first.getString("gpsRouteKey193"))
            result.append(id).append('|').append(row.getString("notes")).append('|')
                .append(first.getString("gpsRouteKey193")).append(';')
        }
        assertEquals("Folytatás 🏋️", data.getJSONObject("draft").getString("notes"))
        assertEquals(17, data.getJSONObject("plannerSettings").getInt("futureField"))
        assertEquals("de", data.getJSONObject("settings").getString("language"))
        assertEquals("integrity-calendar", data.getJSONArray("scheduled").getJSONObject(0).getString("id"))
        assertEquals("integrity-weight", data.getJSONArray("weights").getJSONObject(0).getString("id"))
        assertEquals(1, data.getJSONArray("phase3HistoryTombstones").length())
        val days = data.getJSONArray("programs").getJSONObject(0).getJSONArray("days")
        assertEquals("A", days.getJSONObject(0).getString("id"))
        return sha(result.toString().toByteArray(Charsets.UTF_8))
    }

    private fun runCount(count: Int) { runBlocking(Dispatchers.IO) {
        val source = writeJson("source-$count.json", synthetic(count))
        val imported = manager.importJsonBackupFromUri(source)
        assertTrue("Input import: ${imported.exceptionOrNull()}", imported.isSuccess)
        assertEquals(count, database.workoutHistoryDao().getWorkoutCount())
        val first = uri(local("export-$count.json"))
        val export = manager.exportJsonBackupToUri(first)
        assertTrue("Export: ${export.exceptionOrNull()}", export.isSuccess)
        val signature = semanticSignature(JSONObject(local("export-$count.json").readText()), count)
        val erased = manager.eraseAllLocalData()
        assertTrue("Isolated erase: ${erased.exceptionOrNull()}", erased.isSuccess)
        assertEquals(0, database.workoutHistoryDao().getWorkoutCount())
        val restored = manager.importJsonBackupFromUri(first)
        assertTrue("Restore: ${restored.exceptionOrNull()}", restored.isSuccess)
        assertEquals(count, database.workoutHistoryDao().getWorkoutCount())
        val again = uri(local("reexport-$count.json"))
        val reexport = manager.exportJsonBackupToUri(again)
        assertTrue("Re-export: ${reexport.exceptionOrNull()}", reexport.isSuccess)
        val after = semanticSignature(JSONObject(local("reexport-$count.json").readText()), count)
        assertEquals("Portable ID/business value hash changed", signature, after)
        assertFalse(NativeReplacementJournal(context).exists())
        assertFalse(NativeDataReplacement.blocked(context))
        Log.i("NativeDataIntegrity", "sample=$count count=$count beforeSha256=$signature afterSha256=$after status=PASS")
    }

    }

    @Test fun jsonRoundTrip0() = runCount(0)
    @Test fun jsonRoundTrip200() = runCount(200)
    @Test fun jsonRoundTrip1500() = runCount(1500)

    private suspend fun preparePhotoArchive(): File {
        val json = synthetic(1)
        val photo = JSONObject().put("id", photoId).put("label", "before")
            .put("createdAt", "2026-10-21T09:00:00Z")
            .put("width", 1).put("height", 1).put("bytes", photoBytes.size)
        json.getJSONArray("history").getJSONObject(0).put("photos", JSONArray().put(photo))
        val import = manager.importJsonBackupFromUri(writeJson("photo-source.json", json))
        assertTrue("Photo fixture import: ${import.exceptionOrNull()}", import.isSuccess)
        val privatePhoto = File(context.filesDir, "workout_photos/$photoId.jpg")
        assertTrue(privatePhoto.parentFile!!.isDirectory || privatePhoto.parentFile!!.mkdirs())
        privatePhoto.writeBytes(photoBytes)
        val archive = local("photos.zip")
        val exported = manager.exportZipArchiveToUri(uri(archive))
        assertTrue("ZIP export: ${exported.exceptionOrNull()}", exported.isSuccess)
        ZipFile(archive).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toSet()
            assertEquals(setOf("backup.json", "manifest.json", "photos/$photoId.jpg"), names)
            assertArrayEquals(photoBytes, zip.getInputStream(zip.getEntry("photos/$photoId.jpg")).readBytes())
            val backup = zip.getInputStream(zip.getEntry("backup.json")).bufferedReader().readText()
            assertFalse("Portable backup must not include external absolute photo paths", backup.contains(context.filesDir.absolutePath))
        }
        return archive
    }

    @Test fun zipPhotoBytesSurviveEraseRestoreAndReexport() { runBlocking(Dispatchers.IO) {
        val archive = preparePhotoArchive()
        val before = sha(photoBytes)
        val erased = manager.eraseAllLocalData()
        assertTrue("ZIP test erase: ${erased.exceptionOrNull()}", erased.isSuccess)
        val restored = manager.importZipArchiveFromUri(uri(archive))
        assertTrue("ZIP restore: ${restored.exceptionOrNull()}", restored.isSuccess)
        val privatePhoto = File(context.filesDir, "workout_photos/$photoId.jpg")
        assertTrue(privatePhoto.isFile)
        assertEquals(before, sha(privatePhoto.readBytes()))
        assertEquals(photoId, database.workoutHistoryDao().getWorkoutWithDetails("integrity-0000")!!.photos.single().id)
        val reexport = local("photos-reexport.zip")
        val out = manager.exportZipArchiveToUri(uri(reexport))
        assertTrue("Reexport: ${out.exceptionOrNull()}", out.isSuccess)
        ZipFile(reexport).use { zip ->
            val bytes = zip.getInputStream(zip.getEntry("photos/$photoId.jpg")).readBytes()
            assertEquals(before, sha(bytes))
        }
        Log.i("NativeDataIntegrity", "sample=photo photoSha256=$before reexportSha256=$before status=PASS")
    } }

    @Test fun invalidJsonAndTruncatedZipCannotReplaceExistingData() { runBlocking(Dispatchers.IO) {
        val source = writeJson("fault-base.json", synthetic(1))
        assertTrue(manager.importJsonBackupFromUri(source).isSuccess)
        val before = database.workoutHistoryDao().getWorkoutCount()
        val badJson = local("invalid.json").apply { writeText("{\"history\":[", Charsets.UTF_8) }
        assertTrue(manager.importJsonBackupFromUri(uri(badJson)).isFailure)
        assertEquals(before, database.workoutHistoryDao().getWorkoutCount())
        val archive = local("fault-valid.zip")
        assertTrue(manager.exportZipArchiveToUri(uri(archive)).isSuccess)
        val bytes = archive.readBytes()
        val badZip = local("truncated.zip").apply { writeBytes(bytes.copyOf(bytes.size - 22)) }
        assertTrue(manager.importZipArchiveFromUri(uri(badZip)).isFailure)
        assertEquals(before, database.workoutHistoryDao().getWorkoutCount())
        assertFalse(NativeReplacementJournal(context).exists())
        assertFalse(NativeDataReplacement.blocked(context))
        Log.i("NativeDataIntegrity", "faults=invalid-json,truncated-zip unchangedCount=$before status=PASS")
    } }
}
