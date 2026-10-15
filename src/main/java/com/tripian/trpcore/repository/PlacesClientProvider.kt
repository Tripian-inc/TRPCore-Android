package com.tripian.trpcore.repository

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.ResolveGooglePlacesApiKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shares one [PlacesClient] across the SDK. Places can only be initialised once per process,
 * so the API key is resolved before the first client is created. A resolution that yields no
 * key is not retried for [KEY_RETRY_INTERVAL_MS].
 *
 * @param now Clock used for the retry interval
 */
@Singleton
class PlacesClientProvider internal constructor(
    private val app: Application,
    private val resolveGooglePlacesApiKey: ResolveGooglePlacesApiKey,
    private val now: () -> Long
) {
    @Inject
    constructor(
        app: Application,
        resolveGooglePlacesApiKey: ResolveGooglePlacesApiKey
    ) : this(app, resolveGooglePlacesApiKey, System::currentTimeMillis)

    private val mutex = Mutex()

    @Volatile
    private var client: PlacesClient? = null

    private var lastMissingKeyAt: Long? = null

    /**
     * @return The shared client, or `null` when Places is not initialised and no API key is
     * available, including while the retry interval after a missing key is running
     */
    suspend fun getClient(): PlacesClient? {
        client?.let { return it }
        return mutex.withLock {
            client?.let { return@withLock it }
            if (!Places.isInitialized()) {
                if (isKeyRetryPending()) return@withLock null
                val key = resolveGooglePlacesApiKey.execute(TRPCore.core.hostGooglePlacesApiKey)
                if (key.isNullOrBlank()) {
                    lastMissingKeyAt = now()
                    return@withLock null
                }
                lastMissingKeyAt = null
                TRPCore.core.applyGooglePlacesApiKey(key)
                Places.initialize(app.applicationContext, key)
            }
            Places.createClient(app.applicationContext).also { client = it }
        }
    }

    private fun isKeyRetryPending(): Boolean {
        val missingAt = lastMissingKeyAt ?: return false
        return now() - missingAt in 0 until KEY_RETRY_INTERVAL_MS
    }

    companion object {
        /** Wait after a resolution that yielded no key before asking the backend again. */
        private const val KEY_RETRY_INTERVAL_MS: Long = 5L * 60L * 1000L
    }
}
