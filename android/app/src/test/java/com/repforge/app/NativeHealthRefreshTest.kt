package com.repforge.app

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeHealthRefreshTest {
    private lateinit var context: Context
    private val dispatcher = StandardTestDispatcher()
    @Before fun open() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        HealthJournalStore.get(context).setPreferences(org.json.JSONObject().put("provider", HealthJournalStore.HC))
    }
    @After fun close() {
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        Dispatchers.resetMain()
    }
    @Test fun backgroundActivityIsRejectedBeforeAttemptingAnyProviderRead() = runTest(dispatcher) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup().pause()
        try {
            val result = NativeHealthRefresh.readToday(context, controller.get())
            assertTrue(result.isFailure)
            assertEquals("Foreground health read interrupted", result.exceptionOrNull()!!.message)
        } finally { controller.stop().destroy() }
    }
    @Test fun unavailableHealthConnectDoesNotInventASuccessOrLeaveALifecycleObserver() = runTest(dispatcher) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            val registry = controller.get().lifecycle as LifecycleRegistry
            val before = registry.observerCount
            val result = NativeHealthRefresh.readToday(context, controller.get())
            assertTrue(result.isFailure)
            assertEquals("Health Connect requires Android 14", result.exceptionOrNull()!!.message)
            assertEquals(before, registry.observerCount)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun replacementFenceRejectsHealthRefreshAndAlwaysRemovesItsObserver() = runTest(dispatcher) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            val registry = controller.get().lifecycle as LifecycleRegistry
            val before = registry.observerCount
            context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().putBoolean("blocked", true).commit()
            val result = NativeHealthRefresh.readToday(context, controller.get())
            assertTrue(result.isFailure)
            assertEquals(before, registry.observerCount)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun cancellingQueuedHealthReadReleasesLifecycleObserverWithoutEnteringProvider() = runTest(dispatcher) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val writer = launch { NativeDataReplacement.mutate(context) { entered.complete(Unit); release.await() } }
        entered.await()
        try {
            val registry = controller.get().lifecycle as LifecycleRegistry
            val before = registry.observerCount
            val refresh = backgroundScope.launch { NativeHealthRefresh.readToday(context, controller.get()) }
            runCurrent()
            assertEquals(before + 1, registry.observerCount)
            refresh.cancelAndJoin()
            assertEquals(before, registry.observerCount)
        } finally {
            release.complete(Unit); writer.join()
            controller.pause().stop().destroy()
        }
    }
}
