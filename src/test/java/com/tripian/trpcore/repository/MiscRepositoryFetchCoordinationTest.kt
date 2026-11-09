package com.tripian.trpcore.repository

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the single-flight fetch coordination in [MiscRepository]: concurrent
 * callers for the same language share one fetch, a replacing fetch (via
 * `refetchLanguagesAsync` or a language switch) cancels the superseded one
 * and waiters follow the replacement instead of failing.
 */
class MiscRepositoryFetchCoordinationTest : MiscRepositoryTestBase() {

    @Test
    fun concurrentCallsForSameLanguageShareOneFetch() = runBlocking {
        val repo = newRepository()
        repo.isOnline = { true }
        val callCount = AtomicInteger(0)
        val gate = CompletableDeferred<Unit>()

        repo.translationsFetcher = { lang ->
            callCount.incrementAndGet()
            gate.await()
            payload(lang, "English", "Hello")
        }

        val first = async(Dispatchers.Default) { repo.getLanguageValuesAsync() }
        val second = async(Dispatchers.Default) { repo.getLanguageValuesAsync() }
        waitUntil { callCount.get() == 1 }
        gate.complete(Unit)

        assertTrue(first.await())
        assertTrue(second.await())
        assertEquals(1, callCount.get())
    }

    @Test
    fun refetchCancelsInFlightFetchAndWaiterFollowsReplacement() = runBlocking {
        val repo = newRepository()
        repo.isOnline = { true }
        val firstStarted = CompletableDeferred<Unit>()
        val firstGate = CompletableDeferred<Unit>()
        val enCalls = AtomicInteger(0)
        val freshCalls = AtomicInteger(0)

        repo.translationsFetcher = { lang ->
            if (enCalls.getAndIncrement() == 0) {
                firstStarted.complete(Unit)
                withContext(NonCancellable) { firstGate.await() }
                payload(lang, "Stale", "StaleHello")
            } else {
                freshCalls.incrementAndGet()
                payload(lang, "Fresh", "FreshHello")
            }
        }

        val waiter = async(Dispatchers.Default) { repo.getLanguageValuesAsync() }
        firstStarted.await()

        val refetch = async(Dispatchers.Default) { repo.refetchLanguagesAsync() }
        waitUntil { freshCalls.get() == 1 }

        firstGate.complete(Unit)

        assertTrue(refetch.await())
        assertTrue(waiter.await())
        assertEquals(2, enCalls.get())
        assertEquals(1, freshCalls.get())
        assertEquals("FreshHello", repo.getLanguageValueForKey("hello"))
    }

    @Test
    fun changeLanguageCancelsInFlightFetchForPreviousLanguage() = runBlocking {
        val repo = newRepository()
        repo.isOnline = { true }
        val enStarted = CompletableDeferred<Unit>()
        val enGate = CompletableDeferred<Unit>()
        val enCalls = AtomicInteger(0)
        val esCalls = AtomicInteger(0)

        repo.translationsFetcher = { lang ->
            if (lang == "en") {
                enCalls.incrementAndGet()
                enStarted.complete(Unit)
                withContext(NonCancellable) { enGate.await() }
                payload("en", "Stale-EN", "StaleHello")
            } else {
                esCalls.incrementAndGet()
                payload(lang, "Spanish", "Hola")
            }
        }

        val enWaiter = async(Dispatchers.Default) { repo.getLanguageValuesAsync() }
        enStarted.await()

        repo.changeLanguage("es")
        waitUntil { repo.isLanguagesLoaded }

        enGate.complete(Unit)

        assertTrue(enWaiter.await())
        assertTrue(repo.getLanguageValuesAsync())
        assertTrue(repo.isLanguagesLoaded)
        assertEquals("es", repo.getSavedLanguage())
        assertEquals("Hola", repo.getLanguageValueForKey("hello"))
        assertEquals(1, enCalls.get())
        assertEquals(1, esCalls.get())
    }
}
