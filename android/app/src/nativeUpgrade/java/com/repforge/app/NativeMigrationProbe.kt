package com.repforge.app

import android.content.Context
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyLocalStorageReader
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** CI-only, same-APK probe: compiled and optimized with the actual native app.
 * The separate platform runner calls this one stable ABI; no AndroidJUnit or
 * unoptimized Kotlin/test library is linked against the shrunken app runtime.
 * This source and its keep rule are enabled only by -PnativeUpgradeTests.
 */
object NativeMigrationProbe {
    private val expectedNotes = "Árvíztűrő 🏋️ \"idézet\"\n".repeat(2400)
    private fun digest(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun assertEquals(expected: Any?, actual: Any?) { check(expected == actual) { "Migration values differ" } }
    private fun assertEquals(expected: Double, actual: Double, tolerance: Double) { check(kotlin.math.abs(expected - actual) <= tolerance) { "Migration numeric value differs" } }
    private fun assertTrue(value: Boolean) { check(value) { "Migration invariant failed" } }
    private fun assertFalse(value: Boolean) { assertTrue(!value) }
    private fun assertNull(value: Any?) { check(value == null) { "Deleted workout survived migration" } }
    private fun assertNotNull(value: Any?) { check(value != null) { "Migration document missing" } }

    @JvmStatic
    fun verify(context: Context, fixtureJson: String) = runBlocking {
        val fixture = JSONObject(fixtureJson)
        val settings = TrainPilotSettingsStore(context)
        withTimeout(90000) { while (!settings.current().migrationV1371Complete) delay(100) }
        val db = TrainPilotDatabase.getInstance(context)
        assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
        val row = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetails("migration-workout"))
        assertEquals(expectedNotes, JSONObject(row.workout.rawJson).getString("notes"))
        assertEquals(1, row.exercises.size); assertEquals(1, row.exercises.single().sets.size)
        val set = row.exercises.single().sets.single()
        assertEquals(12, set.leftSeconds ?: -1); assertEquals(14, set.rightSeconds ?: -1)
        assertEquals(2, set.actualRir ?: -1); assertEquals(fixture.getString("routeKey"), set.gpsRouteKey193)
        assertEquals(fixture.getString("photoId"), row.photos.single().id)
        assertNull(db.workoutHistoryDao().getWorkoutWithDetails("deleted-workout"))
        assertEquals("de", settings.current().language)
        assertEquals("migration-program", settings.current().activeProgramId)
        val program = requireNotNull(db.programDao().getProgramWithDaysById("migration-program"))
        assertEquals("Saját terv 🏋️", program.program.nameHu)
        assertEquals("A", program.days.single().dayId)
        assertEquals("side-plank", org.json.JSONArray(program.days.single().exercisesJson).getString(0))
        assertEquals(77.5, db.bodyWeightDao().getAllWeights().single().kg, 0.0)
        val schedule = db.calendarScheduleDao().getAllEntries().single()
        assertEquals("migration-calendar", schedule.id)
        assertEquals("migration-program", schedule.programId)
        assertFalse(settings.current().hapticSetRecorded227)
        assertNotNull(db.keyValueDocumentDao().getDocument("draft"))
        assertNotNull(db.keyValueDocumentDao().getDocument("legacy:futureSetting"))
        val archive = JSONObject(File(context.noBackupFilesDir, "native-migration-v1371/legacy-localstorage.json").readText())
        assertEquals(fixture.getJSONObject("storage").getString("repforge:history"), JSONObject(archive.getJSONObject("values").getString("legacyStorageRaw")).getString("repforge:history"))
        val source = File(context.noBackupFilesDir, "pre-native-fixture-backup.json").readText()
        assertEquals(File(context.noBackupFilesDir, "pre-native-fixture-backup.sha256").readText(), digest(source))
        val original = LegacyLocalStorageReader(context).read()
        assertEquals(fixture.getJSONObject("storage").getString("repforge:history"), original["repforge:history"])
        assertFalse(original.containsKey("repforge:obsolete"))
        val route = GpsRouteStore(context.noBackupFilesDir).read(fixture.getString("routeKey"))
        assertEquals(2, route.pointCount); assertFalse(route.partial)
        assertTrue(File(context.filesDir, "workout_photos/${fixture.getString("photoId")}.jpg").length() > 0)
        HealthJournalStore.get(context).readableDatabase.rawQuery("SELECT value FROM meta WHERE key='migration-fixture'", null).use {
            assertTrue(it.moveToFirst()); assertEquals("retained-health-data", it.getString(0))
        }
        LegacyWebViewMigrator(context, db, settings).migrateIfNeeded()
        assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
    }
}
