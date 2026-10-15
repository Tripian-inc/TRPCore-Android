package com.tripian.trpcore.repository

import android.app.Application
import com.google.gson.GsonBuilder
import com.tripian.one.TRPRest
import com.tripian.one.api.misc.model.ConfigListResponse
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.util.Preferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins that a stale persisted token still triggers a fresh `misc/config-list` request even
 * when an in-memory copy of the list is already cached. Replaces [TRPCore.core] with a mock
 * for the duration of the test and leaves it mocked afterward on a clean JVM.
 */
class MiscRepositoryConfigListRefetchTest {

    private val app: Application = mockk(relaxed = true)
    private val preferences: Preferences = mockk(relaxed = true)

    companion object {
        private const val NOW = 1_700_000_000_000L
        private const val STALE_FETCHED_AT = NOW - (25L * 60L * 60L * 1000L)
    }

    private fun repository(): MiscRepository {
        return spyk(MiscRepository(app = app, preferences = preferences, now = { NOW }))
    }

    private fun responseWithToken(token: String): ConfigListResponse {
        val json = """{"data":{"mapboxAccessToken":"$token"}}"""
        return GsonBuilder().create().fromJson(json, ConfigListResponse::class.java)
    }

    @Test
    fun testStaleTokenTriggersRefetchEvenWhenConfigListIsAlreadyCached() = runTest {
        every { preferences.getString(Preferences.Keys.MAPBOX_ACCESS_TOKEN, "") } returns "pk.stale"
        every {
            preferences.getLong(Preferences.Keys.MAPBOX_ACCESS_TOKEN_FETCHED_AT, 0L)
        } returns STALE_FETCHED_AT

        val trpRest: TRPRest = mockk()
        val core: TRPCore = mockk()
        every { core.trpRest } returns trpRest
        TRPCore.core = core

        val successSlot = slot<(ConfigListResponse) -> Unit>()
        every { trpRest.getConfigList(capture(successSlot), any()) } answers {
            successSlot.captured(responseWithToken("pk.old"))
        }

        val repository = repository()
        repository.getConfigListAsync()

        every { trpRest.getConfigList(capture(successSlot), any()) } answers {
            successSlot.captured(responseWithToken("pk.new"))
        }

        val result = repository.getMapboxAccessToken()

        assertEquals("pk.new", result)
        verify(exactly = 2) { trpRest.getConfigList(any(), any()) }
    }
}
