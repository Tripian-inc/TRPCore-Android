package com.tripian.trpcore.domain

import com.tripian.trpcore.repository.MiscRepository
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Picks the Google Places API key the SDK runs with: the host-supplied key when present,
 * otherwise the key served by the Tripian backend, fetched after a session has been ensured
 * (capped at [MiscRepository.CONFIG_FETCH_TIMEOUT_MS]) so the request carries a bearer token.
 */
class ResolveGooglePlacesApiKey @Inject internal constructor(
    private val miscRepository: MiscRepository,
    private val ensureSession: EnsureSession
) {
    /**
     * @param hostProvidedKey Key passed to `TRPCore.init`, if any; [PLACEHOLDER_API_KEY] counts as absent
     * @param uniqueId User id for the light login run when no session exists; `null` uses the
     * device guest id
     * @return The key to apply, or `null` when no key is available
     */
    suspend fun execute(hostProvidedKey: String?, uniqueId: String?): String? {
        if (isUsableGooglePlacesApiKey(hostProvidedKey)) return hostProvidedKey
        withTimeoutOrNull(MiscRepository.CONFIG_FETCH_TIMEOUT_MS) { ensureSession.execute(uniqueId) }
        return miscRepository.getGooglePlacesApiKey()?.takeIf { isUsableGooglePlacesApiKey(it) }
    }

    companion object {
        /** Placeholder key shipped by host templates; treated the same as a missing key. */
        internal const val PLACEHOLDER_API_KEY = "DEFAULT_API_KEY"

        /** `true` when [key] is neither null, blank nor [PLACEHOLDER_API_KEY]. */
        internal fun isUsableGooglePlacesApiKey(key: String?): Boolean {
            return !key.isNullOrBlank() && key != PLACEHOLDER_API_KEY
        }
    }
}
