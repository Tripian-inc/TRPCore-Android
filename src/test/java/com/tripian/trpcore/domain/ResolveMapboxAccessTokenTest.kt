package com.tripian.trpcore.domain

import com.tripian.trpcore.repository.MiscRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveMapboxAccessTokenTest {

    private val miscRepository: MiscRepository = mockk {
        every { getFreshPersistedMapboxAccessToken() } returns null
    }
    private val ensureSession: EnsureSession = mockk {
        coEvery { execute(any()) } returns true
    }
    private val resolveMapboxAccessToken = ResolveMapboxAccessToken(miscRepository, ensureSession)

    @Test
    fun testNonBlankHostTokenIsReturnedWithoutCallingRepository() = runTest {
        val result = resolveMapboxAccessToken.execute("pk.host-token", null)

        assertEquals("pk.host-token", result)
        coVerify(exactly = 0) { miscRepository.getFreshPersistedMapboxAccessToken() }
        coVerify(exactly = 0) { ensureSession.execute(any()) }
        coVerify(exactly = 0) { miscRepository.getMapboxAccessToken() }
    }

    @Test
    fun testFreshPersistedTokenIsReturnedWithoutEnsuringSessionOrFetching() = runTest {
        every { miscRepository.getFreshPersistedMapboxAccessToken() } returns "pk.fresh-cached"

        val result = resolveMapboxAccessToken.execute(null, "user-1")

        assertEquals("pk.fresh-cached", result)
        coVerify(exactly = 0) { ensureSession.execute(any()) }
        coVerify(exactly = 0) { miscRepository.getMapboxAccessToken() }
    }

    @Test
    fun testCacheMissEnsuresSessionWithUniqueIdBeforeFetching() = runTest {
        coEvery { miscRepository.getMapboxAccessToken() } returns "pk.repo-token"

        val result = resolveMapboxAccessToken.execute(null, "user-42")

        assertEquals("pk.repo-token", result)
        coVerifyOrder {
            ensureSession.execute("user-42")
            miscRepository.getMapboxAccessToken()
        }
    }

    @Test
    fun testFailedEnsureSessionStillFetchesAndReturnsToken() = runTest {
        coEvery { ensureSession.execute(any()) } returns false
        coEvery { miscRepository.getMapboxAccessToken() } returns "pk.repo-token"

        val result = resolveMapboxAccessToken.execute(null, "user-1")

        assertEquals("pk.repo-token", result)
        coVerify(exactly = 1) { miscRepository.getMapboxAccessToken() }
    }

    @Test
    fun testBlankHostTokenFallsBackToRepository() = runTest {
        coEvery { miscRepository.getMapboxAccessToken() } returns "pk.repo-token"

        val result = resolveMapboxAccessToken.execute("  ", null)

        assertEquals("pk.repo-token", result)
        coVerify(exactly = 1) { miscRepository.getMapboxAccessToken() }
    }

    @Test
    fun testNullHostTokenFallsBackToRepository() = runTest {
        coEvery { miscRepository.getMapboxAccessToken() } returns "pk.repo-token"

        val result = resolveMapboxAccessToken.execute(null, null)

        assertEquals("pk.repo-token", result)
        coVerify(exactly = 1) { miscRepository.getMapboxAccessToken() }
    }

    @Test
    fun testNullRepositoryResultYieldsNull() = runTest {
        coEvery { miscRepository.getMapboxAccessToken() } returns null

        val result = resolveMapboxAccessToken.execute(null, null)

        assertNull(result)
    }

    @Test
    fun testSessionStepThatNeverReturnsDoesNotBlockFetchBeyondBudget() = runTest {
        coEvery { ensureSession.execute(any()) } coAnswers { awaitCancellation() }
        coEvery { miscRepository.getMapboxAccessToken() } returns "pk.repo-token"

        val result = resolveMapboxAccessToken.execute(null, "user-1")

        assertEquals("pk.repo-token", result)
        coVerify(exactly = 1) { miscRepository.getMapboxAccessToken() }
        assertEquals(MiscRepository.CONFIG_FETCH_TIMEOUT_MS, testScheduler.currentTime)
    }
}
