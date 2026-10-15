package com.tripian.trpcore.repository

import android.app.Application
import com.google.gson.GsonBuilder
import com.tripian.one.api.misc.model.ConfigList
import com.tripian.trpcore.util.Preferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the caching rules of [MiscRepository.getMapboxAccessToken] and
 * [MiscRepository.getGooglePlacesApiKey]: a fresh persisted secret is served without a
 * network call, a stale one triggers a refetch, a timeout behaves like a failure, a failed
 * or empty refetch falls back to whatever was already persisted, and a single refetch
 * persists every secret the response carries.
 */
class MiscRepositoryTest {

    private val app: Application = mockk(relaxed = true)
    private val preferences: Preferences = mockk(relaxed = true)

    private fun repository(): MiscRepository {
        return spyk(MiscRepository(app = app, preferences = preferences, now = { NOW }))
    }

    private fun configListWith(token: String? = null, placesKey: String? = null): ConfigList {
        val fields = mutableListOf<String>()
        if (token != null) fields.add(""""mapboxAccessToken":"$token"""")
        if (placesKey != null) fields.add(""""googlePlacesApiKey":"$placesKey"""")
        val json = "{${fields.joinToString(",")}}"
        return GsonBuilder().create().fromJson(json, ConfigList::class.java)
    }

    companion object {
        private const val NOW = 1_700_000_000_000L
        private const val STALE_FETCHED_AT = NOW - (25L * 60L * 60L * 1000L)
    }

    @Test
    fun testFreshPersistedTokenIsReturnedWithoutFetching() = runTest {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns "pk.cached"
        every {
            preferences.getLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, 0L)
        } returns NOW

        val repository = repository()

        val result = repository.getMapboxAccessToken()

        assertEquals("pk.cached", result)
        coVerify(exactly = 0) { repository.getConfigListAsync(any()) }
    }

    @Test
    fun testStalePersistedTokenTriggersFetchAndPersistsNewToken() = runTest {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns "pk.stale"
        every {
            preferences.getLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, 0L)
        } returns STALE_FETCHED_AT

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } returns configListWith("pk.fresh")

        val result = repository.getMapboxAccessToken()

        assertEquals("pk.fresh", result)
        verify(exactly = 1) {
            preferences.setString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "pk.fresh")
        }
        verify(exactly = 1) {
            preferences.setLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, NOW)
        }
    }

    @Test
    fun testFetchFailureWithStaleTokenReturnsStaleToken() = runTest {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns "pk.stale"
        every {
            preferences.getLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, 0L)
        } returns STALE_FETCHED_AT

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } throws RuntimeException("network error")

        val result = repository.getMapboxAccessToken()

        assertEquals("pk.stale", result)
    }

    @Test
    fun testFetchFailureWithNoPersistedTokenReturnsNull() = runTest {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns ""
        every {
            preferences.getLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, 0L)
        } returns 0L

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } throws RuntimeException("network error")

        val result = repository.getMapboxAccessToken()

        assertNull(result)
    }

    @Test
    fun testBlankFetchedTokenWithNoPersistedTokenReturnsNull() = runTest {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns ""
        every {
            preferences.getLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, 0L)
        } returns 0L

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } returns configListWith(null)

        val result = repository.getMapboxAccessToken()

        assertNull(result)
        verify(exactly = 0) { preferences.setString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, any()) }
    }

    @Test
    fun testBlankFetchedTokenWithStaleTokenReturnsStaleTokenWithoutPersisting() = runTest {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns "pk.stale"
        every {
            preferences.getLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, 0L)
        } returns STALE_FETCHED_AT

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } returns configListWith("")

        val result = repository.getMapboxAccessToken()

        assertEquals("pk.stale", result)
        verify(exactly = 0) { preferences.setString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, any()) }
    }

    @Test
    fun testNullConfigListWithStaleTokenReturnsStaleToken() = runTest {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns "pk.stale"
        every {
            preferences.getLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, 0L)
        } returns STALE_FETCHED_AT

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } returns null

        val result = repository.getMapboxAccessToken()

        assertEquals("pk.stale", result)
    }

    @Test
    fun testFetchedAtZeroIsNotFresh() = runTest {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns "pk.stale"
        every {
            preferences.getLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, 0L)
        } returns 0L

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } returns configListWith("pk.fresh")

        val result = repository.getMapboxAccessToken()

        assertEquals("pk.fresh", result)
    }

    @Test
    fun testFetchedAtInFutureIsNotFresh() = runTest {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns "pk.stale"
        every {
            preferences.getLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, 0L)
        } returns NOW + (60L * 60L * 1000L)

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } returns configListWith("pk.fresh")

        val result = repository.getMapboxAccessToken()

        assertEquals("pk.fresh", result)
    }

    @Test
    fun testFetchTimeoutWithStaleTokenReturnsStaleTokenWithoutPersisting() = runTest {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns "pk.stale"
        every {
            preferences.getLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, 0L)
        } returns STALE_FETCHED_AT

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } coAnswers {
            delay(20_000)
            configListWith("pk.fresh")
        }

        val result = repository.getMapboxAccessToken()

        assertEquals("pk.stale", result)
        verify(exactly = 0) { preferences.setString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, any()) }
    }

    @Test
    fun testFetchTimeoutWithNoPersistedTokenReturnsNull() = runTest {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns ""
        every {
            preferences.getLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, 0L)
        } returns 0L

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } coAnswers {
            delay(20_000)
            configListWith("pk.fresh")
        }

        val result = repository.getMapboxAccessToken()

        assertNull(result)
    }

    @Test
    fun testFreshPersistedPlacesKeyIsReturnedWithoutFetching() = runTest {
        every { preferences.getString(Preferences.Keys.GOOGLE_PLACES_API_KEY, "") } returns "places.cached"
        every {
            preferences.getLong(Preferences.Keys.GOOGLE_PLACES_API_KEY_FETCHED_AT, 0L)
        } returns NOW

        val repository = repository()

        val result = repository.getGooglePlacesApiKey()

        assertEquals("places.cached", result)
        coVerify(exactly = 0) { repository.getConfigListAsync(any()) }
    }

    @Test
    fun testStalePersistedPlacesKeyTriggersFetchAndPersistsNewKey() = runTest {
        every { preferences.getString(Preferences.Keys.GOOGLE_PLACES_API_KEY, "") } returns "places.stale"
        every {
            preferences.getLong(Preferences.Keys.GOOGLE_PLACES_API_KEY_FETCHED_AT, 0L)
        } returns STALE_FETCHED_AT

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } returns configListWith(placesKey = "places.fresh")

        val result = repository.getGooglePlacesApiKey()

        assertEquals("places.fresh", result)
        verify(exactly = 1) {
            preferences.setString(Preferences.Keys.GOOGLE_PLACES_API_KEY, "places.fresh")
        }
        verify(exactly = 1) {
            preferences.setLong(Preferences.Keys.GOOGLE_PLACES_API_KEY_FETCHED_AT, NOW)
        }
    }

    @Test
    fun testPlacesKeyFetchFailureWithStaleKeyReturnsStaleKey() = runTest {
        every { preferences.getString(Preferences.Keys.GOOGLE_PLACES_API_KEY, "") } returns "places.stale"
        every {
            preferences.getLong(Preferences.Keys.GOOGLE_PLACES_API_KEY_FETCHED_AT, 0L)
        } returns STALE_FETCHED_AT

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } throws RuntimeException("network error")

        val result = repository.getGooglePlacesApiKey()

        assertEquals("places.stale", result)
    }

    @Test
    fun testPlacesKeyFetchTimeoutWithStaleKeyReturnsStaleKey() = runTest {
        every { preferences.getString(Preferences.Keys.GOOGLE_PLACES_API_KEY, "") } returns "places.stale"
        every {
            preferences.getLong(Preferences.Keys.GOOGLE_PLACES_API_KEY_FETCHED_AT, 0L)
        } returns STALE_FETCHED_AT

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } coAnswers {
            delay(20_000)
            configListWith(placesKey = "places.fresh")
        }

        val result = repository.getGooglePlacesApiKey()

        assertEquals("places.stale", result)
        verify(exactly = 0) { preferences.setString(Preferences.Keys.GOOGLE_PLACES_API_KEY, any()) }
    }

    @Test
    fun testPlacesKeyFetchFailureWithNoPersistedKeyReturnsNull() = runTest {
        every { preferences.getString(Preferences.Keys.GOOGLE_PLACES_API_KEY, "") } returns ""
        every {
            preferences.getLong(Preferences.Keys.GOOGLE_PLACES_API_KEY_FETCHED_AT, 0L)
        } returns 0L

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } throws RuntimeException("network error")

        val result = repository.getGooglePlacesApiKey()

        assertNull(result)
    }

    @Test
    fun testGetPersistedGooglePlacesApiKeyReturnsStoredKeyRegardlessOfAge() {
        every { preferences.getString(Preferences.Keys.GOOGLE_PLACES_API_KEY, "") } returns "places.any-age"

        val repository = repository()

        assertEquals("places.any-age", repository.getPersistedGooglePlacesApiKey())
    }

    @Test
    fun testGetPersistedGooglePlacesApiKeyReturnsNullWhenBlank() {
        every { preferences.getString(Preferences.Keys.GOOGLE_PLACES_API_KEY, "") } returns "   "

        val repository = repository()

        assertNull(repository.getPersistedGooglePlacesApiKey())
    }

    @Test
    fun testOneFetchPersistsBothSecretsAndAvoidsSecondRequest() = runTest {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns "pk.stale"
        every {
            preferences.getLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, 0L)
        } returns STALE_FETCHED_AT
        every { preferences.getString(Preferences.Keys.GOOGLE_PLACES_API_KEY, "") } returns "places.stale"
        every {
            preferences.getLong(Preferences.Keys.GOOGLE_PLACES_API_KEY_FETCHED_AT, 0L)
        } returns NOW

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } returns configListWith(
            token = "pk.fresh",
            placesKey = "places.fresh"
        )

        val mapboxResult = repository.getMapboxAccessToken()

        assertEquals("pk.fresh", mapboxResult)
        verify(exactly = 1) {
            preferences.setString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "pk.fresh")
        }
        verify(exactly = 1) {
            preferences.setLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, NOW)
        }
        verify(exactly = 1) {
            preferences.setString(Preferences.Keys.GOOGLE_PLACES_API_KEY, "places.fresh")
        }
        verify(exactly = 1) {
            preferences.setLong(Preferences.Keys.GOOGLE_PLACES_API_KEY_FETCHED_AT, NOW)
        }

        every { preferences.getString(Preferences.Keys.GOOGLE_PLACES_API_KEY, "") } returns "places.fresh"
        every { preferences.getLong(Preferences.Keys.GOOGLE_PLACES_API_KEY_FETCHED_AT, 0L) } returns NOW

        val placesResult = repository.getGooglePlacesApiKey()

        assertEquals("places.fresh", placesResult)
        coVerify(exactly = 1) { repository.getConfigListAsync(true) }
    }

    @Test
    fun testBlankSecretInResponseIsNotPersisted() = runTest {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns "pk.stale"
        every {
            preferences.getLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, 0L)
        } returns STALE_FETCHED_AT
        every { preferences.getString(Preferences.Keys.GOOGLE_PLACES_API_KEY, "") } returns "places.stale"
        every {
            preferences.getLong(Preferences.Keys.GOOGLE_PLACES_API_KEY_FETCHED_AT, 0L)
        } returns STALE_FETCHED_AT

        val repository = repository()
        coEvery { repository.getConfigListAsync(true) } returns configListWith(
            token = "pk.fresh",
            placesKey = ""
        )

        val result = repository.getMapboxAccessToken()

        assertEquals("pk.fresh", result)
        verify(exactly = 0) { preferences.setString(Preferences.Keys.GOOGLE_PLACES_API_KEY, any()) }
        verify(exactly = 0) { preferences.setLong(Preferences.Keys.GOOGLE_PLACES_API_KEY_FETCHED_AT, any()) }
    }

    @Test
    fun testGetPersistedMapboxAccessTokenReturnsStoredTokenRegardlessOfAge() {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns "pk.any-age"

        val repository = repository()

        assertEquals("pk.any-age", repository.getPersistedMapboxAccessToken())
    }

    @Test
    fun testGetPersistedMapboxAccessTokenReturnsNullWhenEmpty() {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns ""

        val repository = repository()

        assertNull(repository.getPersistedMapboxAccessToken())
    }

    @Test
    fun testGetPersistedMapboxAccessTokenReturnsNullWhenBlank() {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns "   "

        val repository = repository()

        assertNull(repository.getPersistedMapboxAccessToken())
    }
}
