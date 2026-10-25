package com.repforge.app

import android.content.Context
import android.os.Looper
import androidx.room.Room
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The real UI saves/deletes on IO, while the canonical execution store requires the main looper. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeProgramEditorThreadingTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        ActiveWorkoutStore.clear(context)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
    }
    @After fun cleanup() {
        db.close()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        Dispatchers.resetMain()
    }
    @Test fun realIoSaveAndDeleteCanCheckTheMainOnlyStoreWithoutStartingAnExecution() = runTest(dispatcher) {
        val draft = NativeProgramEditor.addDay(NativeProgramEditor.Draft("IO terv", "", emptyList()), "A")
        val id = withContext(Dispatchers.IO) {
            assertNotEquals(Looper.getMainLooper(), Looper.myLooper())
            NativeProgramEditor.save(context, db, null, draft, "hu")
        }
        val opened = db.programDao().getProgramWithDaysById(id)!!
        assertEquals("IO terv", opened.program.nameHu)
        withContext(Dispatchers.IO) { NativeProgramEditor.delete(context, db, opened, com.repforge.app.data.store.TrainPilotSettingsStore(context)) }
        assertNull(db.programDao().getProgramWithDaysById(id))
        assertNull(ActiveWorkoutStore.current(context))
        assertNull(PhoneWorkoutExecution.store(context).snapshot())
    }
}
