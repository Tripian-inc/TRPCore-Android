package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.pois.model.PoisResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.PoiRepository
import javax.inject.Inject

/**
 * SearchPOIsUseCase
 * Searches for POIs in a city with optional category, search, sort and filter options
 */
class SearchPOIsUseCase @Inject constructor(
    private val repository: PoiRepository
) : BaseUseCase<PoisResponse, SearchPOIsUseCase.Params>() {

    data class Params(
        val cityId: Int,
        val search: String? = null,
        val categoryIds: List<Int>? = null,
        val page: Int = 1,
        val limit: Int = 30,
        // Sorting parameters
        val sortingBy: String? = null,
        val sortingType: String? = null,
        // Filter parameters
        val minPrice: Int? = null,
        val maxPrice: Int? = null
    )

    override fun on(params: Params?) {
        params?.let { p ->
            addObservable {
                // Use searchWithFilters for all cases to support sorting and filtering
                repository.searchWithFilters(
                    cityId = p.cityId,
                    search = p.search,
                    categoryIds = p.categoryIds,
                    page = p.page,
                    limit = p.limit,
                    sort = p.sortingBy,
                    order = p.sortingType,
                    minPrice = p.minPrice,
                    maxPrice = p.maxPrice
                )
            }
        }
    }
}
