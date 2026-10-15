package com.tripian.trpcore.domain

import com.tripian.trpcore.repository.MiscRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveMapboxAccessTokenTest {

    private val miscRepository: MiscRepository = mockk()
    private val resolveMapboxAccessToken = ResolveMapboxAccessToken(miscRepository)

    @Test
    fun testNonBlankHostTokenIsReturnedWithoutCallingRepository() = runTest {
        val result = resolveMapboxAccessToken.execute("pk.host-token")

        assertEquals("pk.host-token", result)
        coVerify(exactly = 0) { miscRepository.getMapboxAccessToken() }
    }

    @Test
    fun testBlankHostTokenFallsBackToRepository() = runTest {
        coEvery { miscRepository.getMapboxAccessToken() } returns "pk.repo-token"

        val result = resolveMapboxAccessToken.execute("  ")

        assertEquals("pk.repo-token", result)
        coVerify(exactly = 1) { miscRepository.getMapboxAccessToken() }
    }

    @Test
    fun testNullHostTokenFallsBackToRepository() = runTest {
        coEvery { miscRepository.getMapboxAccessToken() } returns "pk.repo-token"

        val result = resolveMapboxAccessToken.execute(null)

        assertEquals("pk.repo-token", result)
        coVerify(exactly = 1) { miscRepository.getMapboxAccessToken() }
    }

    @Test
    fun testNullRepositoryResultYieldsNull() = runTest {
        coEvery { miscRepository.getMapboxAccessToken() } returns null

        val result = resolveMapboxAccessToken.execute(null)

        assertNull(result)
    }
}
