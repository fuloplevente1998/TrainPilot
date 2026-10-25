package com.repforge.app

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean

/** Admission/publication tests: no simulated SDK success is treated as a device read. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeHealthAutoRefreshPublicationTest {
    private lateinit var context: Context

    @Before fun open() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun optInUsesTheExistingHealthJournalPreferenceAndProjection() {
        val store = HealthJournalStore.get(context)
        val previouslyEnabled = store.preferences().optBoolean("autoOnOpen", false)
        try {
            store.setPreferences(org.json.JSONObject().put("autoOnOpen", true))
            assertTrue(store.preferences().optBoolean("autoOnOpen"))
            assertTrue(store.projection().getJSONObject("preferences").optBoolean("autoOnOpen"))
            store.setPreferences(org.json.JSONObject().put("autoOnOpen", false))
            assertFalse(store.preferences().optBoolean("autoOnOpen"))
        } finally {
            store.setPreferences(org.json.JSONObject().put("autoOnOpen", previouslyEnabled))
        }
    }

    @Test fun theHealthConnectReadHookRunsExactlyOnePublication() {
        var written = 0
        val access = HealthConnectRepository.Access(context, null, Runnable {})
        access.readCommit = { writer -> writer() }
        access.publishRead { written++ }
        assertEquals(1, written)
    }

    @Test fun leavingForegroundWhileAcquiringTheWriteLeaseCancelsPublication() {
        val foreground = AtomicBoolean(true)
        val access = HealthConnectRepository.Access(context, null, Runnable {
            check(foreground.get()) { "foreground lost" }
        })
        var written = false
        access.readCommit = { writer ->
            foreground.set(false) // lifecycle changes between outer and inner admission
            writer()
        }
        assertThrows(IllegalStateException::class.java) {
            access.publishRead { written = true }
        }
        assertFalse(written)
    }

    @Test fun pendingRestoreStopsOldProviderPublicationDespiteSuccessfulRead() {
        val continuation = NativeDataReplacement.continuationForCurrentData(context)
        val access = HealthConnectRepository.Access(context, null, Runnable {})
        access.readCommit = { writer ->
            runBlocking {
                NativeDataReplacement.mutate(context) {
                    continuation.requireCurrent(context)
                    writer()
                }
            }
        }
        var written = false
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().putBoolean("blocked", true).commit()
        try {
            assertThrows(IllegalStateException::class.java) {
                access.publishRead { written = true }
            }
            assertFalse(written)
        } finally {
            context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
                .edit().clear().commit()
        }
    }

    @Test fun disposedHealthScreenIsRejectedAndLifecycleObserverIsRemoved() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            val registry = controller.get().lifecycle as LifecycleRegistry
            val observers = registry.observerCount
            val result = NativeHealthRefresh.readToday(context, controller.get(),
                requireScreenGuard = { throw IllegalStateException("Screen no longer visible") })
            assertTrue(result.isFailure)
            assertEquals("Screen no longer visible", result.exceptionOrNull()?.message)
            assertEquals(observers, registry.observerCount)
        } finally {
            controller.pause().stop().destroy()
            Dispatchers.resetMain()
        }
    }
}
