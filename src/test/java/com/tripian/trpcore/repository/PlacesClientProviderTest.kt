package com.tripian.trpcore.repository

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.ResolveGooglePlacesApiKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/**
 * Pins that [PlacesClientProvider] only initialises the Places SDK once per process and
 * skips initialisation entirely when no usable API key is available. Replaces [TRPCore.core]
 * with a mock for the duration of the test and leaves it mocked afterward on a clean JVM.
 */
class PlacesClientProviderTest {

    private val app: Application = mockk(relaxed = true)
    private val resolveGooglePlacesApiKey: ResolveGooglePlacesApiKey = mockk()
    private val core: TRPCore = mockk(relaxed = true)

    companion object {
        private const val KEY_RETRY_INTERVAL_MS: Long = 5L * 60L * 1000L
    }

    @Before
    fun setUp() {
        mockkStatic(Places::class)
        TRPCore.core = core
    }

    @After
    fun tearDown() {
        unmockkStatic(Places::class)
    }

    private fun provider(now: () -> Long = { 0L }): PlacesClientProvider {
        return PlacesClientProvider(app, resolveGooglePlacesApiKey, now)
    }

    @Test
    fun testAlreadyInitialisedSkipsKeyResolutionAndInitialize() = runTest {
        val client: PlacesClient = mockk()
        every { Places.isInitialized() } returns true
        every { Places.createClient(any()) } returns client

        val result = provider().getClient()

        assertSame(client, result)
        verify(exactly = 0) { Places.initialize(any(), any()) }
        coVerify(exactly = 0) { resolveGooglePlacesApiKey.execute(any()) }
    }

    @Test
    fun testNotInitialisedWithUsableKeyInitialisesAndAppliesKey() = runTest {
        val client: PlacesClient = mockk()
        every { Places.isInitialized() } returns false
        every { Places.initialize(any(), any()) } returns Unit
        every { Places.createClient(any()) } returns client
        coEvery { resolveGooglePlacesApiKey.execute(any()) } returns "places.key"

        val result = provider().getClient()

        assertSame(client, result)
        verify(exactly = 1) { Places.initialize(any(), "places.key") }
        verify(exactly = 1) { core.applyGooglePlacesApiKey("places.key") }
    }

    @Test
    fun testNotInitialisedWithNoKeyReturnsNullWithoutInitializing() = runTest {
        every { Places.isInitialized() } returns false
        coEvery { resolveGooglePlacesApiKey.execute(any()) } returns null

        val result = provider().getClient()

        assertNull(result)
        verify(exactly = 0) { Places.initialize(any(), any()) }
        verify(exactly = 0) { Places.createClient(any()) }
    }

    @Test
    fun testSecondCallReturnsCachedClientWithoutTouchingPlacesAgain() = runTest {
        val client: PlacesClient = mockk()
        every { Places.isInitialized() } returns true
        every { Places.createClient(any()) } returns client

        val providerInstance = provider()
        val first = providerInstance.getClient()
        val second = providerInstance.getClient()

        assertSame(client, first)
        assertSame(client, second)
        verify(exactly = 1) { Places.createClient(any()) }
    }

    @Test
    fun testMissingKeyWithinRetryIntervalSkipsSecondResolution() = runTest {
        var currentTime = 0L
        every { Places.isInitialized() } returns false
        coEvery { resolveGooglePlacesApiKey.execute(any()) } returns null

        val providerInstance = provider { currentTime }
        val first = providerInstance.getClient()
        currentTime = KEY_RETRY_INTERVAL_MS - 1
        val second = providerInstance.getClient()

        assertNull(first)
        assertNull(second)
        coVerify(exactly = 1) { resolveGooglePlacesApiKey.execute(any()) }
    }

    @Test
    fun testMissingKeyAtRetryIntervalResolvesAgain() = runTest {
        var currentTime = 0L
        every { Places.isInitialized() } returns false
        coEvery { resolveGooglePlacesApiKey.execute(any()) } returns null

        val providerInstance = provider { currentTime }
        providerInstance.getClient()
        currentTime = KEY_RETRY_INTERVAL_MS
        providerInstance.getClient()

        coVerify(exactly = 2) { resolveGooglePlacesApiKey.execute(any()) }
    }

    @Test
    fun testSuccessfulResolutionAfterRetryIntervalClearsBackoffAndInitialises() = runTest {
        var currentTime = 0L
        val client: PlacesClient = mockk()
        every { Places.isInitialized() } returns false
        every { Places.initialize(any(), any()) } returns Unit
        every { Places.createClient(any()) } returns client
        coEvery { resolveGooglePlacesApiKey.execute(any()) } returns null

        val providerInstance = provider { currentTime }
        val first = providerInstance.getClient()
        currentTime = KEY_RETRY_INTERVAL_MS
        coEvery { resolveGooglePlacesApiKey.execute(any()) } returns "places.key"
        val second = providerInstance.getClient()

        assertNull(first)
        assertSame(client, second)
        verify(exactly = 1) { Places.initialize(any(), "places.key") }
    }
}
