package com.tripian.trpcore.repository

import com.tripian.trpcore.util.Preferences
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
        val repo = MiscRepository(android.app.Application(), preferences)
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
    fun cachePastTtlTriggersExactlyOneFetch() = runBlocking {
        val preferences = Preferences(FakeContext())
        stampCache(preferences, TimeUnit.HOURS.toMillis(25), "StaleCachedHello", "English")
        val repo = MiscRepository(android.app.Application(), preferences)
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
