package com.repforge.app

import com.repforge.app.ui.calendar.NativeCalendarRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant
import java.time.Duration
import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.data.store.TrainPilotUserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeCalendarTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private val dispatcher = StandardTestDispatcher()
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }

    @Before fun open() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        settings = TrainPilotSettingsStore(context)
        runBlocking {
            settings.resetAllSettingsForEraseLocal()
            TrainPilotDatabase.ensureBuiltinCatalogSeeded(db, context)
        }
    }

    @After fun close() {
        db.close()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        Dispatchers.resetMain()
    }

    private suspend fun edit(): NativeCalendarRepository.Edit {
        val program=db.programDao().getProgramsWithDays().first()
        return NativeCalendarRepository.Edit(programId=program.program.id,dayId=program.days.first().dayId,
            date=LocalDate.of(2026,10,24),time=LocalTime.of(18,0),minutes=45)
    }
    private fun repo() = NativeCalendarRepository(db) {
        ActiveWorkoutStore.current(context)?.takeIf { it.optBoolean("active") }?.optString("scheduleId")
    }
    private suspend fun rejected(block: suspend () -> Unit) {
        var error: Exception? = null
        try { block() } catch (e: Exception) { error=e }
        assertNotNull("Operation must be rejected",error)
    }
    @Test fun createUsesLegacyManagedEventIdentityAndExactDuration() = runTest {
        val e=edit();val row=repo().save(e,ZoneId.of("Europe/Budapest"))
        assertEquals("planned",row.status)
        assertEquals(com.repforge.shared.LegacySyncIdentity18.calendarEventId(row.id,row.date,row.programId,row.dayId),row.googleEventId)
        assertEquals(45L,Duration.between(Instant.parse(row.startIso),Instant.parse(row.endIso)).toMinutes())
        assertEquals(row.id,JSONObject(row.rawJson).getString("scheduleId"))
    }
    @Test fun movingPreservesIdentityUnknownRawFieldsAndOldSnapshotCannotOverwrite() = runTest {
        val e=edit();val initial=repo().save(e)
        val original=initial.copy(rawJson=JSONObject(initial.rawJson).put("futureField",123).toString())
        db.calendarScheduleDao().upsertEntry(original)
        val moved=repo().save(e.copy(id=original.id,expected=original,date=e.date.plusDays(1)))
        assertEquals(original.id,moved.id);assertEquals(original.googleEventId,moved.googleEventId)
        assertEquals(123,JSONObject(moved.rawJson).getInt("futureField"))
        rejected { repo().save(e.copy(id=original.id,expected=original)) }
        assertEquals(moved,db.calendarScheduleDao().getEntry(original.id))
    }
    @Test fun skipAndRestoreKeepIdAndDeleteKeepsRemoteCancellationRecord() = runTest {
        val row=repo().save(edit());repo().changeStatus(row,"skipped")
        val skipped=db.calendarScheduleDao().getEntry(row.id)!!
        repo().changeStatus(skipped,"planned")
        val restored=db.calendarScheduleDao().getEntry(row.id)!!
        assertEquals(row.id,restored.id);assertEquals("planned",restored.status)
        repo().changeStatus(restored,"skipped",delete=true)
        val deleted=db.calendarScheduleDao().getEntry(row.id)!!
        assertTrue(NativeCalendarRepository.cancelled(deleted));assertEquals(row.googleEventId,deleted.googleEventId)
        rejected { repo().changeStatus(deleted,"planned") }
    }
    @Test fun completedPlanCannotBeMovedSkippedOrDeleted() = runTest {
        val e=edit();val row=repo().save(e).copy(status="completed")
        db.calendarScheduleDao().upsertEntry(row)
        rejected { repo().save(e.copy(id=row.id,expected=row)) }
        rejected { repo().changeStatus(row,"skipped") }
        assertEquals(row,db.calendarScheduleDao().getEntry(row.id))
    }
    @Test fun missingProgramOrDayCannotCreateOrphanPlans() = runTest {
        val e=edit();rejected { repo().save(e.copy(programId="missing")) }
        rejected { repo().save(e.copy(dayId="missing")) }
        assertTrue(db.calendarScheduleDao().getAllEntries().isEmpty())
    }
    @Test fun invalidDurationAndNonexistentDstWallTimeDoNotWrite() = runTest {
        val e=edit()
        for (n in listOf(-1,0,9,241)) rejected { repo().save(e.copy(minutes=n)) }
        rejected { repo().save(e.copy(date=LocalDate.of(2026,3,29),time=LocalTime.of(2,30)),ZoneId.of("Europe/Budapest")) }
        assertTrue(db.calendarScheduleDao().getAllEntries().isEmpty())
    }
    @Test fun monthQueryIncludesOnlyItsDateRangeAndRetainsStableOrder() = runTest {
        val e=edit();val a=repo().save(e.copy(date=LocalDate.of(2026,10,1)))
        val b=repo().save(e.copy(date=LocalDate.of(2026,10,31)))
        repo().save(e.copy(date=LocalDate.of(2026,11,1)))
        val result=db.calendarScheduleDao().observeEntriesInRange("2026-10-01","2026-10-31").first()
        assertEquals(listOf(a.id,b.id),result.map { it.id })
    }
    @Test fun invalidStartBindingIsRejectedForCancelledSkippedAndWrongDay() = runTest {
        val row=repo().save(edit())
        assertEquals(row,NativeCalendarRepository.requireStart(row,row.programId,row.dayId))
        assertThrows(IllegalStateException::class.java) { NativeCalendarRepository.requireStart(row,row.programId,"wrong") }
        assertThrows(IllegalStateException::class.java) { NativeCalendarRepository.requireStart(row.copy(status="skipped"),row.programId,row.dayId) }
        assertThrows(IllegalStateException::class.java) { NativeCalendarRepository.requireStart(row.copy(rawJson="{\"cancelled\":true}"),row.programId,row.dayId) }
    }
    @Test fun scheduledStartKeepsScheduleBindingAndProtectsActivePlanFromEditing() = runTest {
        val e=edit();val row=repo().save(e)
        val saved=NativeWorkoutSession.startWorkout(context,row.programId,row.dayId,row.id,db)
        assertEquals(row.id,saved.getString("scheduleId"));assertEquals(row.id,PhoneWorkoutExecution.store(context).snapshot()!!.getString("scheduleId"))
        rejected { repo().changeStatus(row,"skipped") }
        rejected { repo().save(e.copy(id=row.id,expected=row,date=e.date.plusDays(1))) }
    }
    @Test fun completingScheduledWorkoutMarksCalendarAndBlocksHistoryResurrection() = runTest {
        val row=repo().save(edit())
        val saved=NativeWorkoutSession.startWorkout(context,row.programId,row.dayId,row.id,db)
        TrainPilotWorkoutFinalization(context,db).finishWorkout(saved)
        assertEquals("completed",db.calendarScheduleDao().getEntry(row.id)!!.status)
        assertTrue(db.workoutHistoryDao().hasScheduleHistory(row.id))
        db.calendarScheduleDao().upsertEntry(row)
        rejected { NativeWorkoutSession.startWorkout(context,row.programId,row.dayId,row.id,db) }
        assertFalse(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
    }
    @Test fun failedPlanWriteDoesNotLeavePartialCalendarRows() = runTest {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER calendar_full BEFORE INSERT ON calendar_entries BEGIN SELECT RAISE(ABORT,'synthetic disk failure'); END")
        rejected { repo().save(edit()) };assertTrue(db.calendarScheduleDao().getAllEntries().isEmpty())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER calendar_full")
        assertEquals("planned",repo().save(edit()).status)
    }
}
