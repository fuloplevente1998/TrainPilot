package com.repforge.app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyLocalStorageReader
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LegacyUpgradeVerificationTest {
    @Test fun signedUpdatePreservesAndMigratesOriginalData() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(Intent().setClassName("com.repforge.app", "com.repforge.app.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            val settings = TrainPilotSettingsStore(context)
            withTimeout(90000) { while (!settings.current().migrationV1371Complete) delay(100) }
            val db = TrainPilotDatabase.getInstance(context)
            assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
            val row = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetails("migration-workout"))
            assertEquals(UpgradeFixture.notes, JSONObject(row.workout.rawJson).getString("notes"))
            assertEquals(1, row.exercises.size); assertEquals(1, row.exercises.single().sets.size)
            val set = row.exercises.single().sets.single()
            assertEquals(12, set.leftSeconds ?: -1); assertEquals(14, set.rightSeconds ?: -1)
            assertEquals(2, set.actualRir ?: -1); assertEquals(UpgradeFixture.ROUTE_KEY, set.gpsRouteKey193)
            assertEquals(UpgradeFixture.PHOTO_ID, row.photos.single().id)
            assertNull(db.workoutHistoryDao().getWorkoutWithDetails("deleted-workout"))
            assertEquals("de", settings.current().language)
            assertEquals("migration-program", settings.current().activeProgramId)
            assertFalse(settings.current().hapticSetRecorded227)
            assertNotNull(db.keyValueDocumentDao().getDocument("draft"))
            assertNotNull(db.keyValueDocumentDao().getDocument("legacy:futureSetting"))
            val archive = JSONObject(File(context.noBackupFilesDir, "native-migration-v1371/legacy-localstorage.json").readText())
            assertEquals(UpgradeFixture.values().getValue("repforge:history"), JSONObject(archive.getJSONObject("values").getString("legacyStorageRaw")).getString("repforge:history"))
            val source = File(context.noBackupFilesDir, "pre-native-fixture-backup.json").readText()
            assertEquals(File(context.noBackupFilesDir, "pre-native-fixture-backup.sha256").readText(), UpgradeFixture.digest(source))
            val original = LegacyLocalStorageReader(context).read()
            assertEquals(UpgradeFixture.values().getValue("repforge:history"), original["repforge:history"])
            assertFalse(original.containsKey("repforge:obsolete"))
            val route = GpsRouteStore(context.noBackupFilesDir).read(UpgradeFixture.ROUTE_KEY)
            assertEquals(2, route.pointCount); assertFalse(route.partial)
            assertTrue(File(context.filesDir, "workout_photos/${UpgradeFixture.PHOTO_ID}.jpg").length() > 0)
            HealthJournalStore.get(context).readableDatabase.rawQuery("SELECT value FROM meta WHERE key='migration-fixture'", null).use {
                assertTrue(it.moveToFirst()); assertEquals("retained-health-data", it.getString(0))
            }
            LegacyWebViewMigrator(context, db, settings).migrateIfNeeded()
            assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }
}
