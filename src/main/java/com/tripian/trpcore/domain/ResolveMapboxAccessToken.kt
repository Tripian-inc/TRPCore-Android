package com.tripian.trpcore.domain

import com.tripian.trpcore.repository.MiscRepository
import javax.inject.Inject

/**
 * Picks the Mapbox access token the SDK runs with: the host-supplied token when present,
 * otherwise the token served by the Tripian backend.
 */
class ResolveMapboxAccessToken @Inject constructor(
    private val miscRepository: MiscRepository
) {

    /**
     * @param hostProvidedToken Token passed to `TRPCore.init`, if any
     * @return The token to apply, or `null` when no token is available
     */
    suspend fun execute(hostProvidedToken: String?): String? {
        if (!hostProvidedToken.isNullOrBlank()) return hostProvidedToken
        return miscRepository.getMapboxAccessToken()
    }
}
