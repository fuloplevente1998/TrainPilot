package com.repforge.app

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class NativeGoogleOperationGateTest {
    @Test fun competingSyncAndDeleteAreRejectedInsteadOfQueued() = runTest {
        val gate = NativeGoogleOperationGate()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val first = launch { gate.run { entered.complete(Unit); release.await() } }
        entered.await()
        var deleted = false
        try { gate.run { deleted = true }; fail("Queued deletion must be rejected") }
        catch (_: NativeGoogleOperationGate.Busy) { }
        release.complete(Unit); first.join()
        assertFalse(deleted)
        assertEquals("next", gate.run { "next" })
    }

    @Test fun accountDisconnectCannotRunDuringSuspendedNetworkWork() = runTest {
        val gate = NativeGoogleOperationGate()
        gate.run {
            assertThrows(NativeGoogleOperationGate.Busy::class.java) { gate.runNow { fail() } }
        }
        assertEquals(1, gate.runNow { 1 })
    }

    @Test fun synchronousAccountChangeAlsoBlocksNetworkAdmission() = runTest {
        val gate = NativeGoogleOperationGate()
        gate.runNow {
            assertThrows(NativeGoogleOperationGate.Busy::class.java) { runBlocking { gate.run { fail() } } }
            assertThrows(NativeGoogleOperationGate.Busy::class.java) { gate.runNow { fail() } }
        }
        assertEquals(2, gate.run { 2 })
    }

    @Test fun rejectedNestedOperationDoesNotReleaseTheOuterOperation() = runTest {
        val gate = NativeGoogleOperationGate()
        gate.run {
            repeat(2) {
                try { gate.run { fail() }; fail() } catch (_: NativeGoogleOperationGate.Busy) { }
            }
        }
        assertEquals(3, gate.run { 3 })
    }

    @Test fun cancellationReleasesAdmissionAndDoesNotBecomeSuccess() = runTest {
        val gate = NativeGoogleOperationGate()
        var succeeded = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            gate.run { awaitCancellation() }
            succeeded = true
        }
        job.cancelAndJoin()
        assertTrue(job.isCancelled); assertFalse(succeeded)
        assertEquals("retry", gate.run { "retry" })
    }

    @Test fun networkFailureAndSynchronousFailureBothReleaseAdmission() = runTest {
        val gate = NativeGoogleOperationGate()
        val failure = IOException("offline")
        try { gate.run { throw failure }; fail() } catch (error: IOException) { assertSame(failure, error) }
        assertThrows(IOException::class.java) { gate.runNow { throw failure } }
        assertEquals(4, gate.run { 4 })
    }

    @Test fun simultaneousThreadsHaveExactlyOneWinnerWithoutCheckThenSetRace() {
        val gate = NativeGoogleOperationGate()
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        val attempted = CountDownLatch(8)
        val release = CountDownLatch(1)
        val winners = AtomicInteger()
        val rejected = AtomicInteger()
        try {
            val jobs = (1..8).map {
                pool.submit {
                    check(start.await(5, TimeUnit.SECONDS))
                    try { gate.runNow {
                        winners.incrementAndGet(); attempted.countDown()
                        check(release.await(5, TimeUnit.SECONDS))
                    } } catch (_: NativeGoogleOperationGate.Busy) {
                        rejected.incrementAndGet(); attempted.countDown()
                    }
                }
            }
            start.countDown()
            assertTrue(attempted.await(5, TimeUnit.SECONDS))
            assertEquals(1, winners.get()); assertEquals(7, rejected.get())
            release.countDown()
            jobs.forEach { it.get(5, TimeUnit.SECONDS) }
            assertEquals(5, gate.runNow { 5 })
        } finally { release.countDown(); pool.shutdownNow() }
    }
}
