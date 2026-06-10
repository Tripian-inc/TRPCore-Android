package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.pois.model.PoisResponse
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.PoiRepository
import javax.inject.Inject

class SearchPOIsUseCase @Inject constructor(
    private val repository: PoiRepository
) : SuspendUseCase<PoisResponse, SearchPOIsUseCase.Params>() {

    data class Params(
        val cityId: Int,
        val search: String? = null,
        val categoryIds: List<Int>? = null,
        val page: Int = 1,
        val limit: Int = 30,
        val sortingBy: String? = null,
        val sortingType: String? = null,
        val minPrice: Int? = null,
        val maxPrice: Int? = null
    )

    override suspend fun execute(params: Params): PoisResponse =
        repository.searchWithFiltersAsync(
            cityId = params.cityId, search = params.search,
            categoryIds = params.categoryIds,
            page = params.page, limit = params.limit,
            sort = params.sortingBy, order = params.sortingType,
            minPrice = params.minPrice, maxPrice = params.maxPrice
        )
}
