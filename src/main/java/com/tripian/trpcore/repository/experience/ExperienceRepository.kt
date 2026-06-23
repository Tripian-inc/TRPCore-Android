package com.tripian.trpcore.repository.experience

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/** Response of GET juniper_cities/{destinationId} — `data` is the Tripian cityId. */
internal class JuniperCityResponse {
    @SerializedName("data") val data: Int? = null
    @SerializedName("status") val status: Int? = null
}

private interface ExperienceService {
    @GET("juniper_cities/{destinationId}")
    suspend fun getCityIdFromDestination(
        @Path("destinationId") destinationId: String
    ): JuniperCityResponse
}

/**
 * Minimal client for the Tripian common API "juniper" mapping that converts a
 * booking `destinationID` (carried in a reservation's detailURL) into a Tripian
 * `cityId`. Ported from the legacy TRPGyg ExperienceRepository for the Nexus
 * integration; the resolved cityId is then used to look up the city centre via
 * TripRepository.getCachedCityById().
 */
object ExperienceRepository {

    private const val BASE_URL = "https://commonapi.tripian.com/"

    private val service: ExperienceService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExperienceService::class.java)
    }

    /** destinationId → Tripian cityId, or null when unresolved (data null/0 or error). */
    suspend fun getCityIdFromDestination(destinationId: String): Int? =
        runCatching { service.getCityIdFromDestination(destinationId).data }
            .getOrNull()
            ?.takeIf { it != 0 }
}
