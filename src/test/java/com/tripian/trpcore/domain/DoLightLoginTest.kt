package com.tripian.trpcore.domain

import com.tripian.one.api.users.model.LoginResponse
import com.tripian.trpcore.repository.TripianUserRepository
import com.tripian.trpcore.util.Preferences
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Pins that a successful light login with a host-provided uniqueId records it
 * as the active identity, so a later saved-trip-hash lookup scopes to it.
 */
class DoLightLoginTest {

    @Test
    fun successfulLoginWithExplicitUniqueIdStoresItAsActive() = runTest {
        val pref: Preferences = mockk(relaxed = true)
        val repository: TripianUserRepository = mockk()
        coEvery {
            repository.lightLoginAsync(uniqueId = "user-a", firstName = any(), lastName = any())
        } returns mockk<LoginResponse>(relaxed = true)

        val useCase = DoLightLogin(pref)
        useCase.tripianUserRepository = repository

        useCase(DoLightLogin.Params(uniqueId = "user-a"))

        verify { pref.setString(Preferences.Keys.ACTIVE_UNIQUE_ID, "user-a") }
    }
}
