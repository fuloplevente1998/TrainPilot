package com.repforge.app

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
class NativeLegacyDraftAdoptionTest {
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
            db.exerciseLibraryDao().upsertExercises(TrainPilotDatabase.defaultExercises())
        }
    }

    @After fun close() {
        db.close()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        Dispatchers.resetMain()
    }

    private fun raw() = """{"savedAt":"2026-10-23T10:10:00Z","current":0,
        "session":{"started":"2026-10-23T10:00:00Z","syncId":"legacy-adopt",
        "type":"quick","quickWorkout":true,"exercises":[{"id":"running","sets":[
        {"done":false,"weight":0,"reps":"61","distanceMeters":69,"gpsRouteKey163":"kept"}]}]}}"""
    private suspend fun seed() { db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("draft",raw(),1L)) }
    private suspend fun resume() = NativeWorkoutSession.resumeImportedDraft(context,db)
    private fun store() = PhoneWorkoutExecution.store(context)
    private suspend fun reject(block: suspend () -> Unit) {
        var error: Exception? = null
        try { block() } catch (e: Exception) { error=e }
        assertNotNull(error)
    }
    @Test fun adoptsPausedWithLibraryMetadataAndKeepsOriginalAndArchive() = runTest {
        seed(); val saved=resume()
        assertEquals("legacy-adopt",saved.getString("workoutId"));assertTrue(saved.getBoolean("workoutPaused"))
        val ex=saved.getJSONArray("exercises").getJSONObject(0);val row=ex.getJSONArray("sets").getJSONObject(0)
        assertEquals("distance",ex.getString("measurementType"));assertEquals("61",row.getString("reps"))
        assertEquals(69,row.getInt("distanceMeters"));assertEquals("kept",row.getString("gpsRouteKey163"))
        assertFalse(row.getBoolean("done"));assertFalse(saved.has(NativeGpsMeasurement.FIELD))
        assertEquals(raw(),db.keyValueDocumentDao().getDocument("draft")!!.json)
        assertEquals(raw(),db.keyValueDocumentDao().getDocument("legacyDraftArchive|"+NativeLegacyDraft.hash(raw()))!!.json)
        assertEquals(saved.getLong("generation"),store().snapshot()!!.getLong("generation"))
    }
    @Test fun retryKeepsSameGenerationAndSetIds() = runTest {
        seed();val a=resume();val b=resume()
        assertEquals(a.getLong("generation"),b.getLong("generation"))
        assertEquals(a.getJSONArray("exercises").toString(),b.getJSONArray("exercises").toString())
    }
    @Test fun archiveWriteFailureLeavesDraftWithoutNativeExecution() = runTest {
        seed();db.openHelper.writableDatabase.execSQL("CREATE TRIGGER archive_full BEFORE INSERT ON kv_documents WHEN NEW.doc_key != 'draft' BEGIN SELECT RAISE(ABORT,'synthetic disk failure'); END")
        reject { resume() };assertNull(store().snapshot());assertNull(ActiveWorkoutStore.current(context))
        assertEquals(raw(),db.keyValueDocumentDao().getDocument("draft")!!.json)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER archive_full")
        assertEquals("legacy-adopt",resume().getString("workoutId"))
    }
    @Test fun nativeWriteFailureRollsBackArchiveAndLeavesOriginalForRetry() = runTest {
        seed();store().writableDatabase.execSQL("CREATE TRIGGER native_full BEFORE INSERT ON execution BEGIN SELECT RAISE(ABORT,'synthetic disk failure'); END")
        reject { resume() };assertNull(store().snapshot());assertEquals(raw(),db.keyValueDocumentDao().getDocument("draft")!!.json)
        assertNull(db.keyValueDocumentDao().getDocument("legacyDraftArchive|"+NativeLegacyDraft.hash(raw())))
        store().writableDatabase.execSQL("DROP TRIGGER native_full");assertEquals("legacy-adopt",resume().getString("workoutId"))
    }
    @Test fun restoreFencePreventsAdoptionWithoutRemovingSource() = runTest {
        seed();store().setRestoreBlocked(true);reject { resume() }
        assertNull(store().snapshot());assertEquals(raw(),db.keyValueDocumentDao().getDocument("draft")!!.json)
    }
    @Test fun closedWatchDocumentPreventsResurrection() = runTest {
        seed();db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("wearClosedWorkouts","[\"legacy-adopt\"]",1L))
        reject { resume() };assertNull(store().snapshot());assertNotNull(db.keyValueDocumentDao().getDocument("draft"))
    }
}
