package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.tour.model.TourSearchResponse
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.TourRepository
import javax.inject.Inject

/**
 * SearchToursUseCase
 * Searches for tours/activities in a city
 */
class SearchToursUseCase @Inject constructor(
    private val repository: TourRepository
) : SuspendUseCase<TourSearchResponse, SearchToursUseCase.Params>() {

    data class Params(
        val cityId: Int,
        val lat: Double,             // Required - City latitude
        val lng: Double,             // Required - City longitude
        val keywords: String? = null,
        val tagIds: String? = null,
        val categoryIds: String? = null,
        val providerId: Int? = null,
        val date: String? = null,    // Format: "YYYY-MM-DD"
        val to: String? = null,      // Format: "YYYY-MM-DD"
        val minPrice: Int? = null,
        val maxPrice: Int? = null,
        val minDuration: Int? = null,
        val maxDuration: Int? = null,
        val adults: Int? = null,
        val currency: String? = null,
        val sortingBy: String? = "popularity",
        val sortingType: String? = "desc",
        val offset: Int = 0,
        val limit: Int = 30
    )

    override suspend fun execute(params: Params): TourSearchResponse =
        repository.searchToursAsync(
            cityId = params.cityId,
            lat = params.lat,
            lng = params.lng,
            keywords = params.keywords,
            tagIds = params.tagIds,
            categoryIds = params.categoryIds,
            providerId = params.providerId,
            date = params.date,
            to = params.to,
            minPrice = params.minPrice,
            maxPrice = params.maxPrice,
            minDuration = params.minDuration,
            maxDuration = params.maxDuration,
            adults = params.adults,
            currency = params.currency,
            sortingBy = params.sortingBy,
            sortingType = params.sortingType,
            offset = params.offset,
            limit = params.limit
        )
}
