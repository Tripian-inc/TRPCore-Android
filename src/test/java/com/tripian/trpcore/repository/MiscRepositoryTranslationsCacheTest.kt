package com.tripian.trpcore.repository

import android.app.Application
import com.tripian.trpcore.util.Preferences
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun warmUpWithFreshCacheLoadsStringsWithoutFetching() {
        val preferences = Preferences(FakeContext())
        stampCache(preferences, TimeUnit.HOURS.toMillis(1), "CachedHello", "English")
        val repo = MiscRepository(Application(), preferences)
        val callCount = AtomicInteger(0)
        repo.translationsFetcher = { lang ->
            callCount.incrementAndGet()
            payload(lang, "English", "ShouldNotBeUsed")
        }

        assertTrue(repo.warmUpFromCache())

        assertTrue(repo.isLanguagesLoaded)
        assertEquals("CachedHello", repo.getLanguageValueForKey("hello"))
        assertEquals(0, callCount.get())
    }

    @Test
    fun warmUpWithoutCacheMakesNoFetch() {
        val repo = newRepository()
        val callCount = AtomicInteger(0)
        repo.translationsFetcher = { lang ->
            callCount.incrementAndGet()
            payload(lang, "English", "ShouldNotBeUsed")
        }

        assertFalse(repo.warmUpFromCache())

        assertFalse(repo.isLanguagesLoaded)
        assertEquals(0, callCount.get())
    }

    @Test
    fun warmUpWithStaleCacheMakesNoFetchAndLeavesLanguagesUnloaded() {
        val preferences = Preferences(FakeContext())
        stampCache(preferences, TimeUnit.HOURS.toMillis(25), "StaleCachedHello", "English")
        val repo = MiscRepository(Application(), preferences)
        val callCount = AtomicInteger(0)
        repo.translationsFetcher = { lang ->
            callCount.incrementAndGet()
            payload(lang, "English", "ShouldNotBeUsed")
        }

        assertFalse(repo.warmUpFromCache())

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

        assertTrue(repo.warmUpFromCache())

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
