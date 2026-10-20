package com.tripian.trpcore.base

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.tripian.trpcore.domain.ResolveMapboxAccessToken
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.sdk.TRPCoreErrorCode
import com.tripian.trpcore.sdk.TRPCoreSDKListener
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins [TRPCore.ensureMapboxAccessToken] against a host-less setup: no [TRPCore.init] call is
 * made, so the fast path only ever compares the applied token against the fresh persisted one.
 */
class TRPCoreEnsureMapboxAccessTokenTest {

    private val miscRepository: MiscRepository = mockk()
    private val resolveMapboxAccessToken: ResolveMapboxAccessToken = mockk()
    private val listener: TRPCoreSDKListener = mockk(relaxed = true)
    private lateinit var trpCore: TRPCore

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        mockkStatic(Looper::class)
        val mainLooper: Looper = mockk()
        every { Looper.myLooper() } returns null
        every { Looper.getMainLooper() } returns mainLooper
        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } returns true
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0

        trpCore = TRPCore()
        trpCore.miscRepository = miscRepository
        trpCore.resolveMapboxAccessToken = resolveMapboxAccessToken
        TRPCore.setListener(listener)
    }

    @After
    fun tearDown() {
        TRPCore.mapBoxApiKey = ""
        TRPCore.setListener(null)
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun testFastPathReturnsTrueWithoutCallingResolverWhenAppliedTokenIsFresh() = runTest {
        TRPCore.mapBoxApiKey = "pk.applied"
        every { miscRepository.getFreshPersistedMapboxAccessToken() } returns "pk.applied"

        val result = trpCore.ensureMapboxAccessToken("user-1")

        assertTrue(result)
        coVerify(exactly = 0) { resolveMapboxAccessToken.execute(any(), any()) }
    }

    @Test
    fun testStaleAppliedTokenFallsThroughToResolver() = runTest {
        TRPCore.mapBoxApiKey = "pk.stale"
        every { miscRepository.getFreshPersistedMapboxAccessToken() } returns null
        coEvery { resolveMapboxAccessToken.execute(null, "user-1") } returns "pk.fresh"

        val result = trpCore.ensureMapboxAccessToken("user-1")

        assertTrue(result)
        coVerify(exactly = 1) { resolveMapboxAccessToken.execute(null, "user-1") }
    }

    @Test
    fun testResolverTokenIsAppliedAndReturnsTrue() = runTest {
        TRPCore.mapBoxApiKey = ""
        every { miscRepository.getFreshPersistedMapboxAccessToken() } returns null
        coEvery { resolveMapboxAccessToken.execute(null, "user-1") } returns "pk.resolved"

        val result = trpCore.ensureMapboxAccessToken("user-1")

        assertTrue(result)
        assertEquals("pk.resolved", TRPCore.mapBoxApiKey)
    }

    @Test
    fun testNullResolverResultNotifiesErrorOnceAndReturnsFalse() = runTest {
        TRPCore.mapBoxApiKey = ""
        every { miscRepository.getFreshPersistedMapboxAccessToken() } returns null
        coEvery { resolveMapboxAccessToken.execute(null, "user-1") } returns null

        val result = trpCore.ensureMapboxAccessToken("user-1")

        assertFalse(result)
        verify(exactly = 1) { listener.onError(any(), any<TRPCoreErrorCode>()) }
    }

    @Test
    fun testResolverFailureNotifiesErrorOnceAndReturnsFalse() = runTest {
        TRPCore.mapBoxApiKey = ""
        every { miscRepository.getFreshPersistedMapboxAccessToken() } returns null
        coEvery { resolveMapboxAccessToken.execute(null, "user-1") } throws IllegalStateException("boom")

        val result = trpCore.ensureMapboxAccessToken("user-1")

        assertFalse(result)
        verify(exactly = 1) { listener.onError(any(), any<TRPCoreErrorCode>()) }
    }
}
