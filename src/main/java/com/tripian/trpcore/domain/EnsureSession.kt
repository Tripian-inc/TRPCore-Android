package com.tripian.trpcore.domain

import android.util.Log
import com.tripian.one.TokenManager
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Guarantees a Tripian session exists before requests that need a bearer token.
 */
internal class EnsureSession internal constructor(
    private val doLightLogin: DoLightLogin,
    private val hasSession: () -> Boolean,
) {
    @Inject
    constructor(doLightLogin: DoLightLogin) : this(doLightLogin, { TokenManager.headerToken() != null })

    /**
     * Performs a light login with [uniqueId] when no session is stored; a blank [uniqueId] falls
     * back to the device guest id.
     *
     * @return `true` when a session exists afterwards, `false` when the light login failed
     */
    suspend fun execute(uniqueId: String?): Boolean {
        if (hasSession()) return true
        return try {
            doLightLogin(DoLightLogin.Params(uniqueId = uniqueId))
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Log.w(TAG, "Light login before config fetch failed", error)
            false
        }
    }

    private companion object {
        const val TAG = "EnsureSession"
    }
}
