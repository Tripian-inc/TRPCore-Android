package com.tripian.trpcore.repository

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins [MiscRepository.retryLanguagesAsync]: a retry after a timed-out wait
 * joins the fetch still in flight instead of starting a second request, and
 * a retry after a failed fetch starts exactly one new request.
 */
class MiscRepositoryRetryTest : MiscRepositoryTestBase() {

    @Test
    fun retryJoinsStillInFlightFetchInsteadOfStartingANewOne() = runBlocking {
        val repo = newRepository()
        repo.isOnline = { true }
        val callCount = AtomicInteger(0)
        val gate = CompletableDeferred<Unit>()

        repo.translationsFetcher = { lang ->
            callCount.incrementAndGet()
            gate.await()
            payload(lang, "English", "Hello")
        }

        var timedOut = false
        try {
            withTimeout(50) { repo.waitForLanguagesLoadedAsync() }
        } catch (_: TimeoutCancellationException) {
            timedOut = true
        }
        assertTrue(timedOut)
        assertEquals(1, callCount.get())

        val retry = async(start = CoroutineStart.UNDISPATCHED) { repo.retryLanguagesAsync() }
        assertFalse(retry.isCompleted)
        assertEquals(1, callCount.get())
        gate.complete(Unit)

        assertTrue(retry.await())
        assertEquals(1, callCount.get())
        assertEquals("Hello", repo.getLanguageValueForKey("hello"))
    }

    @Test
    fun retryAfterFailedFetchStartsExactlyOneNewRequest() = runBlocking {
        val repo = newRepository()
        repo.isOnline = { true }
        val callCount = AtomicInteger(0)

        repo.translationsFetcher = { lang ->
            if (callCount.getAndIncrement() == 0) {
                throw RuntimeException("network error")
            }
            payload(lang, "English", "Hello")
        }

        assertFalse(repo.waitForLanguagesLoadedAsync())
        assertEquals(1, callCount.get())

        assertTrue(repo.retryLanguagesAsync())
        assertEquals(2, callCount.get())
        assertEquals("Hello", repo.getLanguageValueForKey("hello"))
    }

    @Test
    fun retryWhenAlreadyLoadedReturnsTrueWithoutExtraFetch() = runBlocking {
        val repo = newRepository()
        repo.isOnline = { true }
        val callCount = AtomicInteger(0)

        repo.translationsFetcher = { lang ->
            callCount.incrementAndGet()
            payload(lang, "English", "Hello")
        }

        assertTrue(repo.getLanguageValuesAsync())
        assertEquals(1, callCount.get())

        assertTrue(repo.retryLanguagesAsync())
        assertEquals(1, callCount.get())
    }
}
