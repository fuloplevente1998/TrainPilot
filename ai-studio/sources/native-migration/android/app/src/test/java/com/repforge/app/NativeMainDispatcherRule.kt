package com.repforge.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Pumps real Room/IO -> Main guards on Robolectric's main test thread via runTest. */
@OptIn(ExperimentalCoroutinesApi::class)
class NativeMainDispatcherRule : TestWatcher() {
    val dispatcher = StandardTestDispatcher()
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}

internal suspend fun <T : Throwable> assertNativeSuspendThrows(type: Class<T>, operation: suspend () -> Unit) {
    try { operation() }
    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
    catch (failure: Throwable) {
        assertTrue("Expected ${type.simpleName}, got $failure", type.isInstance(failure))
        return
    }
    fail("Expected ${type.simpleName}")
}
