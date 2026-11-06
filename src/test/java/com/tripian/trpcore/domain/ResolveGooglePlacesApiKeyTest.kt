package com.tripian.trpcore.domain

import com.tripian.trpcore.repository.MiscRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveGooglePlacesApiKeyTest {

    private val miscRepository: MiscRepository = mockk {
        every { getFreshPersistedGooglePlacesApiKey() } returns null
    }
    private val ensureSession: EnsureSession = mockk {
        coEvery { execute(any()) } returns true
    }
    private val resolveGooglePlacesApiKey = ResolveGooglePlacesApiKey(miscRepository, ensureSession)

    @Test
    fun testNonBlankHostKeyIsReturnedWithoutEnsuringSessionOrCallingRepository() = runTest {
        val result = resolveGooglePlacesApiKey.execute("host.key", "user-1")

        assertEquals("host.key", result)
        coVerify(exactly = 0) { miscRepository.getFreshPersistedGooglePlacesApiKey() }
        coVerify(exactly = 0) { ensureSession.execute(any()) }
        coVerify(exactly = 0) { miscRepository.getGooglePlacesApiKey() }
    }

    @Test
    fun testFreshPersistedKeyIsReturnedWithoutEnsuringSessionOrFetching() = runTest {
        every { miscRepository.getFreshPersistedGooglePlacesApiKey() } returns "places.fresh-cached"

        val result = resolveGooglePlacesApiKey.execute(null, "user-1")

        assertEquals("places.fresh-cached", result)
        coVerify(exactly = 0) { ensureSession.execute(any()) }
        coVerify(exactly = 0) { miscRepository.getGooglePlacesApiKey() }
    }

    @Test
    fun testFreshPersistedPlaceholderKeyIsIgnoredAndKeyIsFetched() = runTest {
        every { miscRepository.getFreshPersistedGooglePlacesApiKey() } returns "DEFAULT_API_KEY"
        coEvery { miscRepository.getGooglePlacesApiKey() } returns "repo.key"

        val result = resolveGooglePlacesApiKey.execute(null, "user-1")

        assertEquals("repo.key", result)
        coVerify(exactly = 1) { ensureSession.execute("user-1") }
    }

    @Test
    fun testMissingHostKeyEnsuresSessionWithUniqueIdBeforeFetching() = runTest {
        coEvery { miscRepository.getGooglePlacesApiKey() } returns "repo.key"

        val result = resolveGooglePlacesApiKey.execute(null, "user-42")

        assertEquals("repo.key", result)
        coVerifyOrder {
            ensureSession.execute("user-42")
            miscRepository.getGooglePlacesApiKey()
        }
    }

    @Test
    fun testNullUniqueIdIsPassedToEnsureSession() = runTest {
        coEvery { miscRepository.getGooglePlacesApiKey() } returns "repo.key"

        resolveGooglePlacesApiKey.execute(null, null)

        coVerify(exactly = 1) { ensureSession.execute(null) }
    }

    @Test
    fun testFailedEnsureSessionStillFetchesAndReturnsKey() = runTest {
        coEvery { ensureSession.execute(any()) } returns false
        coEvery { miscRepository.getGooglePlacesApiKey() } returns "repo.key"

        val result = resolveGooglePlacesApiKey.execute(null, "user-1")

        assertEquals("repo.key", result)
        coVerify(exactly = 1) { miscRepository.getGooglePlacesApiKey() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testSessionStepThatNeverReturnsDoesNotBlockFetchBeyondBudget() = runTest {
        coEvery { ensureSession.execute(any()) } coAnswers { awaitCancellation() }
        coEvery { miscRepository.getGooglePlacesApiKey() } returns "repo.key"

        val result = resolveGooglePlacesApiKey.execute(null, "user-1")

        assertEquals("repo.key", result)
        coVerify(exactly = 1) { miscRepository.getGooglePlacesApiKey() }
        assertEquals(MiscRepository.CONFIG_FETCH_TIMEOUT_MS, testScheduler.currentTime)
    }

    @Test
    fun testBlankHostKeyFallsBackToRepository() = runTest {
        coEvery { miscRepository.getGooglePlacesApiKey() } returns "repo.key"

        val result = resolveGooglePlacesApiKey.execute("  ", null)

        assertEquals("repo.key", result)
        coVerify(exactly = 1) { ensureSession.execute(any()) }
        coVerify(exactly = 1) { miscRepository.getGooglePlacesApiKey() }
    }

    @Test
    fun testDefaultApiKeyHostValueIsTreatedAsBlank() = runTest {
        coEvery { miscRepository.getGooglePlacesApiKey() } returns "repo.key"

        val result = resolveGooglePlacesApiKey.execute("DEFAULT_API_KEY", null)

        assertEquals("repo.key", result)
        coVerify(exactly = 1) { ensureSession.execute(any()) }
        coVerify(exactly = 1) { miscRepository.getGooglePlacesApiKey() }
    }

    @Test
    fun testRepositoryReturningDefaultApiKeyYieldsNull() = runTest {
        coEvery { miscRepository.getGooglePlacesApiKey() } returns "DEFAULT_API_KEY"

        val result = resolveGooglePlacesApiKey.execute(null, null)

        assertNull(result)
    }

    @Test
    fun testRepositoryReturningEmptyKeyYieldsNull() = runTest {
        coEvery { miscRepository.getGooglePlacesApiKey() } returns ""

        val result = resolveGooglePlacesApiKey.execute(null, null)

        assertNull(result)
    }

    @Test
    fun testNullRepositoryResultYieldsNull() = runTest {
        coEvery { miscRepository.getGooglePlacesApiKey() } returns null

        val result = resolveGooglePlacesApiKey.execute(null, null)

        assertNull(result)
    }
}
