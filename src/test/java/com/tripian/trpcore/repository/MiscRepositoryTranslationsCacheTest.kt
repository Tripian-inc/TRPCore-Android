package com.tripian.trpcore.repository

import android.app.Application
import com.tripian.trpcore.util.Preferences
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the 24h translations cache TTL in [MiscRepository]: a per-language
 * blob within the window is served from preferences without a fetch; one
 * past the window is treated as stale and triggers exactly one fetch.
 */
class MiscRepositoryTranslationsCacheTest : MiscRepositoryTestBase() {

    private val language = "en"

    private fun stampCache(preferences: Preferences, ageMs: Long, helloValue: String, label: String) {
        preferences.setString(
            Preferences.Keys.translationsForLanguage(language),
            payload(language, label, helloValue)
        )
        preferences.setLong(
            Preferences.Keys.translationsFetchedAtForLanguage(language),
            System.currentTimeMillis() - ageMs
        )
    }

    @Test
    fun cacheWithinTtlIsServedWithoutFetching() = runBlocking {
        val preferences = Preferences(FakeContext())
        stampCache(preferences, TimeUnit.HOURS.toMillis(23), "CachedHello", "English")
        val repo = MiscRepository(Application(), preferences)
        repo.isOnline = { true }
        val callCount = AtomicInteger(0)
        repo.translationsFetcher = { lang ->
            callCount.incrementAndGet()
            payload(lang, "English", "ShouldNotBeUsed")
        }

        assertTrue(repo.getLanguageValuesAsync())

        assertEquals(0, callCount.get())
        assertEquals("CachedHello", repo.getLanguageValueForKey("hello"))
    }

    @Test
    fun warmUpWithFreshCacheLoadsStringsWithoutFetching() = runBlocking {
        val preferences = Preferences(FakeContext())
        stampCache(preferences, TimeUnit.HOURS.toMillis(1), "CachedHello", "English")
        val repo = MiscRepository(Application(), preferences)
        val callCount = AtomicInteger(0)
        repo.translationsFetcher = { lang ->
            callCount.incrementAndGet()
            payload(lang, "English", "ShouldNotBeUsed")
        }

        repo.warmUpFromCache().join()

        assertTrue(repo.isLanguagesLoaded)
        assertEquals("CachedHello", repo.getLanguageValueForKey("hello"))
        assertEquals(0, callCount.get())
    }

    @Test
    fun warmUpReadsTheCacheOffTheCallerThread() = runBlocking {
        val context = GatedContext(Preferences.Keys.translationsForLanguage(language))
        val preferences = Preferences(context)
        stampCache(preferences, TimeUnit.HOURS.toMillis(1), "CachedHello", "English")
        val repo = MiscRepository(Application(), preferences)

        val warmUp = repo.warmUpFromCache()

        assertTrue(context.preferences.awaitGatedRead())
        assertFalse(repo.isLanguagesLoaded)
        context.preferences.release()
        warmUp.join()

        assertNotNull(context.preferences.readerThread)
        assertNotSame(Thread.currentThread(), context.preferences.readerThread)
        assertTrue(repo.isLanguagesLoaded)
        assertEquals("CachedHello", repo.getLanguageValueForKey("hello"))
    }

    @Test
    fun warmUpDoesNotOverwriteAFetchThatFinishedForAnotherLanguage() = runBlocking {
        val context = GatedContext(Preferences.Keys.translationsForLanguage(language))
        val preferences = Preferences(context)
        stampCache(preferences, TimeUnit.HOURS.toMillis(1), "CachedHello", "English")
        val repo = MiscRepository(Application(), preferences)
        repo.isOnline = { true }
        repo.translationsFetcher = { lang -> payload(lang, "Español", "Hola") }

        val warmUp = repo.warmUpFromCache()
        assertTrue(context.preferences.awaitGatedRead())
        repo.changeLanguage("es")
        assertTrue(repo.getLanguageValuesAsync())
        context.preferences.release()
        warmUp.join()

        assertTrue(repo.isLanguagesLoaded)
        assertEquals("Hola", repo.getLanguageValueForKey("hello"))
        assertEquals("es", repo.appliedLanguage.value)
    }

    @Test
    fun warmUpWithoutCacheMakesNoFetch() = runBlocking {
        val repo = newRepository()
        val callCount = AtomicInteger(0)
        repo.translationsFetcher = { lang ->
            callCount.incrementAndGet()
            payload(lang, "English", "ShouldNotBeUsed")
        }

        repo.warmUpFromCache().join()

        assertFalse(repo.isLanguagesLoaded)
        assertEquals(0, callCount.get())
    }

    @Test
    fun warmUpWithStaleCacheMakesNoFetchAndLeavesLanguagesUnloaded() = runBlocking {
        val preferences = Preferences(FakeContext())
        stampCache(preferences, TimeUnit.HOURS.toMillis(25), "StaleCachedHello", "English")
        val repo = MiscRepository(Application(), preferences)
        val callCount = AtomicInteger(0)
        repo.translationsFetcher = { lang ->
            callCount.incrementAndGet()
            payload(lang, "English", "ShouldNotBeUsed")
        }

        repo.warmUpFromCache().join()

        assertFalse(repo.isLanguagesLoaded)
        assertEquals(0, callCount.get())
    }

    @Test
    fun warmUpWhenAlreadyLoadedMakesNoFetch() = runBlocking {
        val repo = newRepository()
        repo.isOnline = { true }
        val callCount = AtomicInteger(0)
        repo.translationsFetcher = { lang ->
            callCount.incrementAndGet()
            payload(lang, "English", "FetchedHello")
        }

        assertTrue(repo.getLanguageValuesAsync())
        assertEquals(1, callCount.get())

        repo.warmUpFromCache().join()

        assertTrue(repo.isLanguagesLoaded)
        assertEquals(1, callCount.get())
        assertEquals("FetchedHello", repo.getLanguageValueForKey("hello"))
    }

    @Test
    fun cachePastTtlTriggersExactlyOneFetch() = runBlocking {
        val preferences = Preferences(FakeContext())
        stampCache(preferences, TimeUnit.HOURS.toMillis(25), "StaleCachedHello", "English")
        val repo = MiscRepository(Application(), preferences)
        repo.isOnline = { true }
        val callCount = AtomicInteger(0)
        repo.translationsFetcher = { lang ->
            callCount.incrementAndGet()
            payload(lang, "English", "FreshFetchedHello")
        }

        assertTrue(repo.getLanguageValuesAsync())

        assertEquals(1, callCount.get())
        assertEquals("FreshFetchedHello", repo.getLanguageValueForKey("hello"))
    }
}
