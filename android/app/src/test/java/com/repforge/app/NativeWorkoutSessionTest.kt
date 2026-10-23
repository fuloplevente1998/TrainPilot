package com.repforge.app

import android.content.Context
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeWorkoutSessionTest {
    private lateinit var context: Context
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }

    @Before fun open() {
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After fun close() {
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
    }

    private fun candidate(id: String = "phone-session") = JSONObject("""{
        "active":true,"timerOwner":"phone","workoutId":"$id","id":"$id","syncId":"$id",
        "started":"2026-10-23T10:00:00Z","currentExercise":0,
        "exercises":[{"id":"press","repUnit":"ism.","sets":[{"setId":"stable-set","reps":"8","weight":10,"done":false}]}]
    }""")

    @Test fun sessionAndNativeExecutionHaveTheSameIdentityBeforeNavigation() {
        val source = candidate()
        val original = source.toString()
        val saved = NativeWorkoutSession.installPrepared(context, source)
        val native = PhoneWorkoutExecution.store(context).snapshot()!!
        assertEquals(original, source.toString())
        assertEquals("phone-session", saved.getString("workoutId"))
        assertEquals(saved.getString("workoutId"), native.getString("workoutId"))
        assertEquals(saved.getLong("generation"), native.getLong("generation"))
        assertEquals(1L, native.getLong("executionRevision"))
        assertEquals("stable-set", saved.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getString("setId"))
    }

    @Test fun repeatedStartResumesTheFirstSessionInsteadOfReplacingIt() {
        val first = NativeWorkoutSession.installPrepared(context, candidate())
        val second = NativeWorkoutSession.installPrepared(context, candidate("second"))
        assertEquals(first.toString(), second.toString())
        assertEquals("phone-session", PhoneWorkoutExecution.store(context).snapshot()!!.getString("workoutId"))
    }

    @Test fun watchOwnedMirrorIsNeverReplacedByPhoneStart() {
        val watch = ActiveWorkoutStore.saveOwned(context, candidate("watch-session").put("timerOwner", "watch"))
        val saved = NativeWorkoutSession.installPrepared(context, candidate())
        assertEquals(watch.toString(), saved.toString())
        assertNull(PhoneWorkoutExecution.store(context).snapshot())
    }

    @Test fun recoversDurableNativeFrameAfterSnapshotPublicationWasInterrupted() {
        val native = PhoneWorkoutExecution.store(context).prepare(candidate("recover-me")
            .put("generation", 1).put("executionRevision", 0), 0)
        val saved = NativeWorkoutSession.installPrepared(context, candidate("new-session"))
        assertEquals("recover-me", saved.getString("workoutId"))
        assertEquals(native.getLong("generation"), saved.getLong("generation"))
        assertEquals(native.getLong("executionRevision"), saved.getLong("executionRevision"))
    }

    @Test fun restoreOrOwnershipFenceCannotCreateANewSession() {
        val execution = PhoneWorkoutExecution.store(context)
        execution.setRestoreBlocked(true)
        assertThrows(IllegalStateException::class.java) { NativeWorkoutSession.installPrepared(context, candidate()) }
        assertNull(execution.snapshot())
        assertNull(ActiveWorkoutStore.current(context))
        execution.setRestoreBlocked(false)
        execution.suspendCommands(true)
        assertThrows(IllegalStateException::class.java) { NativeWorkoutSession.installPrepared(context, candidate()) }
        assertNull(execution.snapshot())
    }
}
