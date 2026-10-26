package com.repforge.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.runBlocking
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeHealthAutoRefreshBackupTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test fun nestedBackupScopesPreventAnyNewHealthPublicationUntilLastRelease() {
        var commits = 0
        NativeHealthBackupGate.begin()
        try {
            assertFalse(NativeHealthBackupGate.available())
            assertThrows(IllegalStateException::class.java) { NativeHealthBackupGate.publish { commits++ } }
            NativeHealthBackupGate.begin()
            try {
                assertFalse(NativeHealthBackupGate.available())
            } finally { NativeHealthBackupGate.end() }
            assertFalse(NativeHealthBackupGate.available())
            assertEquals(0, commits)
        } finally { NativeHealthBackupGate.end() }
        assertTrue(NativeHealthBackupGate.available())
        NativeHealthBackupGate.publish { commits++ }
        assertEquals(1, commits)
    }

    @Test fun zipTransactionDirectoryFailureReleasesAdmissionAndLaterExportSucceeds() = runBlocking {
        // Deliberately replace the parent directory with a regular file. transactionDir()
        // must fail in mkdir BEFORE ZIP extraction; the gate must still be released.
        val root = File(context.noBackupFilesDir, "archive_transactions")
        val hadDirectory = root.isDirectory
        val temporaryBlock = File(context.noBackupFilesDir, "archive_transactions-blocked-test")
        check(!temporaryBlock.exists()) { "ZIP staging test fixture already exists" }
        if (hadDirectory) {
            assertTrue(root.renameTo(temporaryBlock))
        } else {
            assertFalse(root.exists())
        }
        val output = File(context.cacheDir, "health-auto-refresh-recovery.json")
        try {
            assertTrue(root.createNewFile())
            val manager = BackupRestoreManager(context)
            val failed = manager.importZipArchiveFromUri(Uri.EMPTY)
            assertTrue("ZIP mkdir failure must be reported", failed.isFailure)
            assertTrue(failed.exceptionOrNull() is java.io.IOException)
            assertTrue(failed.exceptionOrNull()!!.message.orEmpty().contains("Cannot create backup directory"))
            assertTrue("Failed ZIP staging must never strand backup admission", NativeHealthBackupGate.available())
            assertTrue(root.delete())
            val valid = manager.exportJsonBackupToUri(Uri.fromFile(output))
            assertTrue("Next real JSON archive export must work: ${valid.exceptionOrNull()}", valid.isSuccess)
            assertTrue(NativeHealthBackupGate.available())
        } finally {
            if (root.isFile) root.delete()
            if (hadDirectory) assertTrue(temporaryBlock.renameTo(root))
            output.delete()
            assertTrue("No admission leak after the fixture cleanup", NativeHealthBackupGate.available())
        }
    }

    @Test fun initialPartialHealthDayKeepsActualFieldsAndMarksMissingOnesUnavailable() {
        context.deleteDatabase("health-journal.db")
        val store = HealthJournalStore(context)
        try {
            val date = LocalDate.now().minusDays(2).toString()
            val result = JSONObject()
                .put("source", "@aggregate")
                .put("steps", 1525)
                .put("weightKg", JSONObject.NULL)
                .put("warnings", JSONArray().put("READ_WEIGHT unavailable"))
            store.healthDay(date, result, store.generation())
            val projected = store.projection().getJSONObject("days").getJSONObject(date)
            assertEquals(1525, projected.getInt("steps"))
            assertTrue(projected.optBoolean("partial"))
            assertTrue(projected.isNull("weightKg") || !projected.has("weightKg"))
        } finally {
            store.close()
        }
    }
}
