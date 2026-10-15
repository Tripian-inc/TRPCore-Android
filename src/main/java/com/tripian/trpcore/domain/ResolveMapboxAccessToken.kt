package com.tripian.trpcore.domain

import com.tripian.trpcore.repository.MiscRepository
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Picks the Mapbox access token the SDK runs with: the host-supplied token when present, then a
 * fresh persisted token, otherwise the token served by the Tripian backend, fetched after a
 * session has been ensured so the request carries a bearer token. The session step is capped at
 * [MiscRepository.CONFIG_FETCH_TIMEOUT_MS]; the fetch runs even when it fails or times out.
 */
internal class ResolveMapboxAccessToken @Inject constructor(
    private val miscRepository: MiscRepository,
    private val ensureSession: EnsureSession
) {

    /**
     * @param hostProvidedToken Token passed to `TRPCore.init`, if any
     * @param uniqueId User id for the light login run when no session exists; `null` uses the
     * device guest id
     * @return The token to apply, or `null` when no token is available
     */
    suspend fun execute(hostProvidedToken: String?, uniqueId: String?): String? {
        if (!hostProvidedToken.isNullOrBlank()) return hostProvidedToken
        miscRepository.getFreshPersistedMapboxAccessToken()?.let { return it }
        withTimeoutOrNull(MiscRepository.CONFIG_FETCH_TIMEOUT_MS) { ensureSession.execute(uniqueId) }
        return miscRepository.getMapboxAccessToken()
    }
}
