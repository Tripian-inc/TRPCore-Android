package com.tripian.trpcore.repository

import com.tripian.one.TRPRest
import com.tripian.one.api.tour.model.TourScheduleResponse
import com.tripian.one.api.tour.model.TourSearchResponse
import io.reactivex.Single
import javax.inject.Inject

/**
 * TourRepository
 * Wraps TRPRest Tour methods into RxJava Singles
 */
class TourRepository @Inject constructor(
    private val trpRest: TRPRest
) {

    /**
     * Search tours/activities
     * Uses the individual parameters overload for flexibility
     *
     * @param cityId Required - City ID to search tours in
     * @param lat Required - Latitude for location-based search (city coordinate)
     * @param lng Required - Longitude for location-based search (city coordinate)
     * @param keywords Optional - Search keywords
     * @param tagIds Optional - Comma-separated tag IDs for category filtering
     * @param providerId Optional - Provider ID for filtering (default: 15)
     * @param date Optional - Date filter (YYYY-MM-DD)
     * @param minPrice Optional - Minimum price filter
     * @param maxPrice Optional - Maximum price filter
     * @param minDuration Optional - Minimum duration in minutes
     * @param maxDuration Optional - Maximum duration in minutes
     * @param adults Optional - Number of adults
     * @param currency Optional - Currency code (e.g., "EUR")
     * @param sortingBy Optional - Sort field ("price", "rating", "popularity")
     * @param sortingType Optional - Sort direction ("asc", "desc")
     * @param offset Pagination offset (default 0)
     * @param limit Pagination limit (default 30)
     */
    fun searchTours(
        cityId: Int,
        lat: Double,
        lng: Double,
        keywords: String? = null,
        tagIds: String? = null,
        providerId: Int? = null,
        date: String? = null,
        minPrice: Int? = null,
        maxPrice: Int? = null,
        minDuration: Int? = null,
        maxDuration: Int? = null,
        adults: Int? = null,
        currency: String? = null,
        sortingBy: String? = null,
        sortingType: String? = null,
        offset: Int = 0,
        limit: Int = 30
    ): Single<TourSearchResponse> {
        return Single.create { emitter ->
            trpRest.searchTours(
                cityId = cityId,
                lat = lat,
                lng = lng,
                keywords = keywords,
                tagIds = tagIds,
                providerId = providerId,
                date = date,
                minPrice = minPrice,
                maxPrice = maxPrice,
                minDuration = minDuration,
                maxDuration = maxDuration,
                adults = adults,
                currency = currency,
                sortingBy = sortingBy,
                sortingType = sortingType,
                offset = offset,
                limit = limit,
                success = { response ->
                    emitter.onSuccess(response)
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Unknown error"))
                }
            )
        }
    }

    /**
     * Get tour schedule/availability
     *
     * @param productId Tour product ID
     * @param date Date to check availability (YYYY-MM-DD)
     * @param currency Optional currency code (e.g., "EUR")
     */
    fun getTourSchedule(
        productId: String,
        date: String,
        currency: String? = null
    ): Single<TourScheduleResponse> {
        return Single.create { emitter ->
            trpRest.getTourSchedule(
                productId = productId,
                date = date,
                currency = currency,
                success = { response ->
                    emitter.onSuccess(response)
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Unknown error"))
                }
            )
        }
    }
}
