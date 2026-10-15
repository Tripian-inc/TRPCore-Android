package com.tripian.trpcore.repository

import com.google.gson.GsonBuilder
import com.tripian.one.api.misc.model.ConfigListResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins that `misc/config-list` responses carrying `mapboxAccessToken` and
 * `googlePlacesApiKey` are parsed by plain Gson into [ConfigListResponse.data], and that
 * their absence yields null.
 */
class ConfigListResponseParsingTest {

    private val gson = GsonBuilder().create()

    @Test
    fun testMapboxAccessTokenIsParsedFromData() {
        val json = """{"data":{"mapboxAccessToken":"pk.abc","apiKey":"x"}}"""

        val response = gson.fromJson(json, ConfigListResponse::class.java)

        assertEquals("pk.abc", response.data?.mapboxAccessToken)
    }

    @Test
    fun testMapboxAccessTokenIsNullWhenKeyIsAbsent() {
        val json = """{"data":{"apiKey":"x"}}"""

        val response = gson.fromJson(json, ConfigListResponse::class.java)

        assertNull(response.data?.mapboxAccessToken)
    }

    @Test
    fun testGooglePlacesApiKeyIsParsedFromData() {
        val json = """{"data":{"googlePlacesApiKey":"places.abc","apiKey":"x"}}"""

        val response = gson.fromJson(json, ConfigListResponse::class.java)

        assertEquals("places.abc", response.data?.googlePlacesApiKey)
    }

    @Test
    fun testGooglePlacesApiKeyIsNullWhenKeyIsAbsent() {
        val json = """{"data":{"apiKey":"x"}}"""

        val response = gson.fromJson(json, ConfigListResponse::class.java)

        assertNull(response.data?.googlePlacesApiKey)
    }
}
