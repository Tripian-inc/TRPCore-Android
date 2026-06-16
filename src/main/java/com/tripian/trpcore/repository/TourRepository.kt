package com.tripian.trpcore.repository

import com.tripian.one.TRPRest
import com.tripian.one.api.tour.model.TourProductLookupResponse
import com.tripian.one.api.tour.model.TourScheduleAvailabilityResponse
import com.tripian.one.api.tour.model.TourScheduleResponse
import com.tripian.one.api.tour.model.TourSearchResponse
import com.tripian.trpcore.base.awaitCallback
import javax.inject.Inject

/**
 * TourRepository — suspend wrappers over the TRPRest Tour callback API.
 */
class TourRepository @Inject constructor(
    private val trpRest: TRPRest
) {

    suspend fun searchToursAsync(
        cityId: Int,
        lat: Double,
        lng: Double,
        keywords: String? = null,
        tagIds: String? = null,
        categoryIds: String? = null,
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
            categoryIds = categoryIds,
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
