package com.tripian.trpcore.repository

import com.tripian.one.TRPRest
import com.tripian.one.api.tour.model.TourProductLookupResponse
import com.tripian.one.api.tour.model.TourScheduleAvailabilityResponse
import com.tripian.one.api.tour.model.TourScheduleResponse
import com.tripian.one.api.tour.model.TourSearchResponse
import com.tripian.trpcore.base.awaitCallback
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
     * @param to Optional - Range end date (YYYY-MM-DD); paired with `date`
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
        to: String? = null,
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
                to = to,
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
     * @param date Date to check availability (YYYY-MM-DD); range start when `to` is set
     * @param to Optional range end date (YYYY-MM-DD); response then carries per-day buckets in `dates`
     * @param currency Optional currency code (e.g., "EUR")
     */
    fun getTourSchedule(
        productId: String,
        date: String,
        to: String? = null,
        currency: String? = null
    ): Single<TourScheduleResponse> {
        return Single.create { emitter ->
            trpRest.getTourSchedule(
                productId = productId,
                date = date,
                to = to,
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

    /**
     * Lookup a single tour product by provider + product id.
     * GET /tour-api/product-lookup
     *
     * Used to resolve activities the timeline doesn't carry coordinates / city for
     * (e.g. no-location segments).
     */
    fun lookupTourProduct(
        providerId: Int,
        productId: String
    ): Single<TourProductLookupResponse> {
        return Single.create { emitter ->
            trpRest.lookupTourProduct(
                providerId = providerId,
                productId = productId,
                success = { response -> emitter.onSuccess(response) },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Tour product lookup failed"))
                }
            )
        }
    }

    /**
     * Batch availability lookup across multiple activities on a single target date.
     * POST /tour-api/schedule-bulk
     *
     * @param items Activity IDs (e.g. "C_163295_15")
     * @param date Target date "YYYY-MM-DD"
     * @param currency Optional currency override
     * @param lang Optional language override
     */
    fun getTourScheduleAvailability(
        items: List<String>,
        date: String,
        currency: String? = null,
        lang: String? = null
    ): Single<TourScheduleAvailabilityResponse> {
        return Single.create { emitter ->
            trpRest.getTourScheduleAvailability(
                items = items,
                date = date,
                currency = currency,
                lang = lang,
                success = { response -> emitter.onSuccess(response) },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Schedule availability lookup failed"))
                }
            )
        }
    }

    // ------------------------------------------------------------------
    // Suspend equivalents of the Single endpoints above. Added during the
    // RxJava → Coroutines migration; legacy Single versions remain until
    // every UseCase caller is ported (Phase D).
    // ------------------------------------------------------------------

    suspend fun searchToursAsync(
        cityId: Int,
        lat: Double,
        lng: Double,
        keywords: String? = null,
        tagIds: String? = null,
        providerId: Int? = null,
        date: String? = null,
        to: String? = null,
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
    ): TourSearchResponse = awaitCallback { ok, fail ->
        trpRest.searchTours(
            cityId = cityId,
            lat = lat,
            lng = lng,
            keywords = keywords,
            tagIds = tagIds,
            providerId = providerId,
            date = date,
            to = to,
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
            success = ok,
            error = { throwable -> fail(throwable ?: Exception("Unknown error")) }
        )
    }

    suspend fun getTourScheduleAsync(
        productId: String,
        date: String,
        to: String? = null,
        currency: String? = null
    ): TourScheduleResponse = awaitCallback { ok, fail ->
        trpRest.getTourSchedule(
            productId = productId,
            date = date,
            to = to,
            currency = currency,
            success = ok,
            error = { throwable -> fail(throwable ?: Exception("Unknown error")) }
        )
    }

    suspend fun lookupTourProductAsync(
        providerId: Int,
        productId: String
    ): TourProductLookupResponse = awaitCallback { ok, fail ->
        trpRest.lookupTourProduct(
            providerId = providerId,
            productId = productId,
            success = ok,
            error = { throwable -> fail(throwable ?: Exception("Tour product lookup failed")) }
        )
    }

    suspend fun getTourScheduleAvailabilityAsync(
        items: List<String>,
        date: String,
        currency: String? = null,
        lang: String? = null
    ): TourScheduleAvailabilityResponse = awaitCallback { ok, fail ->
        trpRest.getTourScheduleAvailability(
            items = items,
            date = date,
            currency = currency,
            lang = lang,
            success = ok,
            error = { throwable -> fail(throwable ?: Exception("Schedule availability lookup failed")) }
        )
    }
}
