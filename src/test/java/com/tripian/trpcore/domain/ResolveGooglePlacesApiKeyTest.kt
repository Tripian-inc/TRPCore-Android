package com.tripian.trpcore.domain

import com.tripian.trpcore.repository.MiscRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveGooglePlacesApiKeyTest {

    private val miscRepository: MiscRepository = mockk()
    private val resolveGooglePlacesApiKey = ResolveGooglePlacesApiKey(miscRepository)

    @Test
    fun testNonBlankHostKeyIsReturnedWithoutCallingRepository() = runTest {
        val result = resolveGooglePlacesApiKey.execute("host.key")

        assertEquals("host.key", result)
        coVerify(exactly = 0) { miscRepository.getGooglePlacesApiKey() }
    }

    @Test
    fun testBlankHostKeyFallsBackToRepository() = runTest {
        coEvery { miscRepository.getGooglePlacesApiKey() } returns "repo.key"

        val result = resolveGooglePlacesApiKey.execute("  ")

        assertEquals("repo.key", result)
        coVerify(exactly = 1) { miscRepository.getGooglePlacesApiKey() }
    }

    @Test
    fun testNullHostKeyFallsBackToRepository() = runTest {
        coEvery { miscRepository.getGooglePlacesApiKey() } returns "repo.key"

        val result = resolveGooglePlacesApiKey.execute(null)

        assertEquals("repo.key", result)
        coVerify(exactly = 1) { miscRepository.getGooglePlacesApiKey() }
    }

    @Test
    fun testDefaultApiKeyHostValueIsTreatedAsBlank() = runTest {
        coEvery { miscRepository.getGooglePlacesApiKey() } returns "repo.key"

        val result = resolveGooglePlacesApiKey.execute("DEFAULT_API_KEY")

        assertEquals("repo.key", result)
        coVerify(exactly = 1) { miscRepository.getGooglePlacesApiKey() }
    }

    @Test
    fun testRepositoryReturningDefaultApiKeyYieldsNull() = runTest {
        coEvery { miscRepository.getGooglePlacesApiKey() } returns "DEFAULT_API_KEY"

        val result = resolveGooglePlacesApiKey.execute(null)

        assertNull(result)
    }

    @Test
    fun testNullRepositoryResultYieldsNull() = runTest {
        coEvery { miscRepository.getGooglePlacesApiKey() } returns null

        val result = resolveGooglePlacesApiKey.execute(null)

        assertNull(result)
    }
}
