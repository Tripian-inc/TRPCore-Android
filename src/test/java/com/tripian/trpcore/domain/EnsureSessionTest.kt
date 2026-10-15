package com.tripian.trpcore.domain

import android.util.Log
import com.tripian.one.api.users.model.LoginResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins that [EnsureSession] skips the light login when a session already exists, otherwise
 * forwards the given unique id, swallows ordinary login failures as `false` and lets
 * [CancellationException] propagate.
 */
class EnsureSessionTest {

    private val doLightLogin: DoLightLogin = mockk()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun ensureSession(hasSession: () -> Boolean): EnsureSession {
        return EnsureSession(doLightLogin, hasSession)
    }

    @Test
    fun testExistingSessionReturnsTrueWithoutLogin() = runTest {
        val result = ensureSession(hasSession = { true }).execute("user-1")

        assertTrue(result)
        coVerify(exactly = 0) { doLightLogin(any()) }
    }

    @Test
    fun testNoSessionPerformsLightLoginWithUniqueId() = runTest {
        coEvery { doLightLogin(any()) } returns mockk<LoginResponse>(relaxed = true)

        val result = ensureSession(hasSession = { false }).execute("user-1")

        assertTrue(result)
        coVerify(exactly = 1) { doLightLogin(match { it.uniqueId == "user-1" }) }
    }

    @Test
    fun testNullUniqueIdPassesNullThrough() = runTest {
        coEvery { doLightLogin(any()) } returns mockk<LoginResponse>(relaxed = true)

        val result = ensureSession(hasSession = { false }).execute(null)

        assertTrue(result)
        coVerify(exactly = 1) { doLightLogin(match { it.uniqueId == null }) }
    }

    @Test
    fun testLightLoginFailureReturnsFalseWithoutThrowing() = runTest {
        coEvery { doLightLogin(any()) } throws RuntimeException("network error")

        val result = ensureSession(hasSession = { false }).execute("user-1")

        assertFalse(result)
    }

    @Test(expected = CancellationException::class)
    fun testCancellationExceptionPropagates() = runTest {
        coEvery { doLightLogin(any()) } throws CancellationException("cancelled")

        ensureSession(hasSession = { false }).execute("user-1")
    }
}
