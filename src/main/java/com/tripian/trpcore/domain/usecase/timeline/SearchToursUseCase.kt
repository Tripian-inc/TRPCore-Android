package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.tour.model.TourSearchResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TourRepository
import javax.inject.Inject

/**
 * SearchToursUseCase
 * Searches for tours/activities in a city
 */
class SearchToursUseCase @Inject constructor(
    private val repository: TourRepository
) : BaseUseCase<TourSearchResponse, SearchToursUseCase.Params>() {

    data class Params(
        val cityId: Int,
        val lat: Double,             // Required - City latitude
        val lng: Double,             // Required - City longitude
        val keywords: String? = null,
        val tagIds: String? = null,  // Comma-separated tag IDs for category filtering (not used - use keywords instead)
        val providerId: Int? = null, // Provider ID for filtering (default: 15 for tour-api)
        val date: String? = null,    // Format: "YYYY-MM-DD"
        val minPrice: Int? = null,
        val maxPrice: Int? = null,
        val minDuration: Int? = null, // Minimum duration in minutes
        val maxDuration: Int? = null, // Maximum duration in minutes
        val adults: Int? = null,
        val currency: String? = null,
        val sortingBy: String? = "popularity",
        val sortingType: String? = "desc",
        val offset: Int = 0,
        val limit: Int = 30
    )

    override fun on(params: Params?) {
        params?.let { p ->
            addObservable {
                repository.searchTours(
                    cityId = p.cityId,
                    lat = p.lat,
                    lng = p.lng,
                    keywords = p.keywords,
                    tagIds = p.tagIds,
                    providerId = p.providerId,
                    date = p.date,
                    minPrice = p.minPrice,
                    maxPrice = p.maxPrice,
                    minDuration = p.minDuration,
                    maxDuration = p.maxDuration,
                    adults = p.adults,
                    currency = p.currency,
                    sortingBy = p.sortingBy,
                    sortingType = p.sortingType,
                    offset = p.offset,
                    limit = p.limit
                ).toObservable()
            }
        }
    }
}
