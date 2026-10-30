package com.repforge.app

import android.content.Context
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeReplacementRecoveryTest {
    private lateinit var context: Context
    private lateinit var journal: NativeReplacementJournal
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    private val token = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    @Before fun open() {
        Dispatchers.setMain(StandardTestDispatcher())
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        journal = NativeReplacementJournal(context); journal.clear()
        File(context.noBackupFilesDir, "archive_transactions").deleteRecursively()
    }
    @After fun close() {
        journal.clear()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        Dispatchers.resetMain()
    }
    private fun stage(committed: Boolean = false): File {
        val dir = File(context.noBackupFilesDir, "archive_transactions/$token").apply { mkdirs() }
        val file = File(dir, "backup.json")
        val bytes = """{"history":[],"programs":[],"exercises":[],"weights":[],"scheduled":[],"settings":{"language":"en"}}""".toByteArray()
        file.writeBytes(bytes)
        journal.write(NativeReplacementJournal.Entry("json", token, committed, ArchiveIO.hash(ByteArrayInputStream(bytes))))
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().putBoolean("blocked", true).commit()
        PhoneWorkoutExecution.store(context).setRestoreBlocked(true)
        return file
    }
    @Test fun receiptRoundTripsItsVerifiedIdentityAndCommitPhase() {
        val entry = NativeReplacementJournal.Entry("zip", token, false, "a".repeat(64))
        journal.write(entry); assertEquals(entry, journal.read())
        journal.write(entry.copy(committed = true)); assertTrue(journal.read().committed)
        journal.clear(); assertFalse(journal.exists())
    }
    @Test fun malformedReceiptDoesNotBecomeAnAutomaticUnlockOrPathTraversal() {
        journal.write(NativeReplacementJournal.Entry("json", "../outside", false, "a".repeat(64)))
        assertThrows(IllegalStateException::class.java) { journal.read() }
        assertTrue(journal.exists())
    }
    @Test fun pendingReceiptKeepsTheFenceUntilExplicitRecoveryRetiresIt() = runTest {
        NativeDataReplacement.exclusively(context) { journal.write(NativeReplacementJournal.Entry("erase", null, false)) }
        assertTrue(NativeDataReplacement.blocked(context))
        assertTrue(PhoneWorkoutExecution.store(context).restoreBlocked())
        NativeDataReplacement.recover(context) { journal.clear() }
        assertFalse(NativeDataReplacement.blocked(context))
        assertFalse(PhoneWorkoutExecution.store(context).restoreBlocked())
    }
    @Test fun stagedJsonReplayUsesTheExistingImporterAndRetiresBothReceiptAndFence() = runTest {
        val db = TrainPilotDatabase.getInstance(context)
        withContext(Dispatchers.IO) { db.clearAllTables() }
        val settings = TrainPilotSettingsStore(context)
        settings.setLanguage("hu")
        val file = stage()
        val result = BackupRestoreManager(context).resumeInterruptedReplacement()
        assertTrue(result.exceptionOrNull()?.message, result.isSuccess)
        assertEquals("en", settings.current().language)
        assertFalse(file.parentFile!!.exists())
        assertFalse(journal.exists()); assertFalse(NativeDataReplacement.blocked(context))
    }
    @Test fun corruptStagedPayloadFailsBeforeChangingSettingsAndRetainsEvidence() = runTest {
        val settings = TrainPilotSettingsStore(context); settings.setLanguage("hu")
        val file = stage(); file.writeText("{\"history\":[],\"settings\":{\"language\":\"ro\"}}")
        assertTrue(BackupRestoreManager(context).resumeInterruptedReplacement().isFailure)
        assertEquals("hu", settings.current().language)
        assertTrue(journal.exists()); assertTrue(file.exists()); assertTrue(NativeDataReplacement.blocked(context))
    }
    @Test fun committedReceiptOnlyCleansUpAndDoesNotReimportThePayload() = runTest {
        val settings = TrainPilotSettingsStore(context); settings.setLanguage("de")
        val file = stage(committed = true); file.writeText("not JSON; import must not happen")
        assertTrue(BackupRestoreManager(context).resumeInterruptedReplacement().isSuccess)
        assertEquals("de", settings.current().language)
        assertFalse(file.parentFile!!.exists()); assertFalse(NativeDataReplacement.blocked(context))
    }
    @Test fun faultsAtPreparedImportedAndCommittedPhasesKeepAReplayableReceipt() = runTest {
        for (phase in listOf("receipt-prepared", "data-imported", "receipt-committed")) {
            journal.clear()
            context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
            PhoneWorkoutExecution.store(context).setRestoreBlocked(false)
            val file = stage()
            val manager = BackupRestoreManager(context)
            manager.restoreCheckpoint = { current -> if (current == phase) throw java.io.IOException("injected $phase") }
            assertTrue(manager.resumeInterruptedReplacement().isFailure)
            assertTrue(journal.exists()); assertTrue(NativeDataReplacement.blocked(context))
            assertEquals(phase == "receipt-committed", journal.read().committed)
            assertTrue(BackupRestoreManager(context).resumeInterruptedReplacement().isSuccess)
            assertFalse(journal.exists()); assertFalse(file.parentFile!!.exists())
        }
    }
    @Test fun stagedHealthRestoreCanRecoverAfterPreparationInstallationAndCommitFaults() = runTest {
        for (phase in listOf("health-prepared", "health-installed", "room-settings-imported", "health-committed")) {
            journal.clear()
            context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
            PhoneWorkoutExecution.store(context).setRestoreBlocked(false)
            val health = HealthJournalStore.get(context)
            val file = stage()
            val data = org.json.JSONObject(file.readText()).put("nativeHealthJournal", health.exportSnapshot())
            file.writeText(data.toString())
            journal.write(journal.read().copy(payloadSha256 = file.inputStream().use { ArchiveIO.hash(it) }))
            val manager = BackupRestoreManager(context)
            manager.restoreCheckpoint = { current -> if (current == phase) throw java.io.IOException("injected $phase") }
            assertTrue(manager.resumeInterruptedReplacement().isFailure)
            assertTrue(journal.exists()); assertTrue(NativeDataReplacement.blocked(context))
            assertTrue(BackupRestoreManager(context).resumeInterruptedReplacement().isSuccess)
            assertEquals("", health.pendingRestore())
            assertFalse(journal.exists()); assertFalse(NativeDataReplacement.blocked(context))
        }
    }
    @Test fun missingReceiptFailsClosedAndRequiresAnExplicitNewBackupChoice() = runTest {
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().putBoolean("blocked", true).commit()
        assertTrue(BackupRestoreManager(context).resumeInterruptedReplacement().isFailure)
        assertTrue(NativeDataReplacement.blocked(context))
    }
}
