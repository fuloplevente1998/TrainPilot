package com.repforge.app.wear

import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import org.junit.Assert.*
import org.junit.Test

class WearHealthFuturesTest {
    private val direct = Executor { it.run() }

    @Test fun successfulStartAndEndCommandsAcceptNullResults() {
        val events = mutableListOf<String>()
        val failures = mutableListOf<Exception>()
        val start = SettableFuture.create<Void>()
        val end = SettableFuture.create<Void>()
        WearHealthFutures.awaitCompletion(start, direct, { true }, failures::add) { events.add("started") }
        WearHealthFutures.awaitCompletion(end, direct, { true }, failures::add) { events.add("ended") }
        assertTrue(events.isEmpty())
        WearHealthFutureFixture.completeWithNull(start)
        assertEquals(listOf("started"), events)
        WearHealthFutureFixture.completeWithNull(end)
        assertEquals(listOf("started", "ended"), events)
        assertTrue(failures.isEmpty())
    }

    @Test fun completionRunsOnTheSuppliedExecutorAfterTheFutureFinishes() {
        val queue = mutableListOf<Runnable>()
        val future = SettableFuture.create<Void>()
        var completed = false
        WearHealthFutures.awaitCompletion(future, Executor { queue.add(it) }, { true }, { throw it }) { completed = true }
        assertTrue(queue.isEmpty())
        WearHealthFutureFixture.completeWithNull(future)
        assertFalse(completed)
        assertEquals(1, queue.size)
        queue.removeAt(0).run()
        assertTrue(completed)
    }

    @Test fun failedCommandReportsTheHealthServicesCauseWithoutRunningSuccess() {
        val future = SettableFuture.create<Void>()
        val cause = IllegalStateException("permission denied")
        val failures = mutableListOf<Exception>()
        var completed = false
        WearHealthFutures.awaitCompletion(future, direct, { true }, failures::add) { completed = true }
        future.setException(cause)
        assertFalse(completed)
        assertSame(cause, failures.single().cause)
    }

    @Test fun cancelledCommandIsNotTreatedAsSuccessfulCompletion() {
        val future = SettableFuture.create<Void>()
        val failures = mutableListOf<Exception>()
        var completed = false
        WearHealthFutures.awaitCompletion(future, direct, { true }, failures::add) { completed = true }
        future.cancel(false)
        assertFalse(completed)
        assertTrue(failures.single() is CancellationException)
    }

    @Test fun queuedCallbacksDoNotChangeADestroyedService() {
        val queue = mutableListOf<Runnable>()
        val future = SettableFuture.create<Void>()
        val failures = mutableListOf<Exception>()
        var active = true
        var completed = false
        WearHealthFutures.awaitCompletion(future, Executor { queue.add(it) }, { active }, failures::add) { completed = true }
        WearHealthFutureFixture.completeWithNull(future)
        active = false
        queue.single().run()
        assertFalse(completed)
        assertTrue(failures.isEmpty())
    }

    @Test fun queryResultsAreDeliveredAsData() {
        val future = SettableFuture.create<String>()
        val failures = mutableListOf<Exception>()
        var result: String? = null
        WearHealthFutures.awaitValue(future, direct, { true }, failures::add) { result = it }
        future.set("capabilities")
        assertEquals("capabilities", result)
        assertTrue(failures.isEmpty())
    }

    @Test fun missingQueryDataIsStillAnError() {
        val future = SettableFuture.create<String>()
        val failures = mutableListOf<Exception>()
        var completed = false
        WearHealthFutures.awaitValue(future, direct, { true }, failures::add) { completed = true }
        WearHealthFutureFixture.completeWithNull(future)
        assertFalse(completed)
        assertTrue(failures.single() is IllegalStateException)
    }

    @Test fun onlyTheOldNullCommandCallbackErrorIsEligibleForRetry() {
        val prefix = "Mérési hiba: Parameter specified as non-null is null: method com.repforge.app.wear.WearHealthService."
        for (method in listOf("capabilities\$lambda\$7\$lambda\$6", "end\$lambda\$12")) {
            assertTrue(WearHealthFutures.isLegacyNullCompletionError("$prefix$method, parameter it"))
        }
        for (message in listOf("", "permission denied", "Másik alkalmazás már edzést mér.",
                "${prefix}capabilities\$lambda\$7, parameter capabilities",
                "Parameter specified as non-null is null: method another.Service.start, parameter it")) {
            assertFalse(message, WearHealthFutures.isLegacyNullCompletionError(message))
        }
    }
}
