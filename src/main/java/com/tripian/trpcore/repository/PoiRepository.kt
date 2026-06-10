package com.tripian.trpcore.repository

import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.pois.model.PoiCategoriesResponse
import com.tripian.one.api.pois.model.PoiCategoryModel
import com.tripian.one.api.pois.model.PoiResponse
import com.tripian.one.api.pois.model.PoisResponse
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.util.extensions.enableRating
import javax.inject.Inject

class PoiRepository @Inject constructor(val service: ServiceWrapper) {

    private var poiIds = HashMap<String, Poi>()
    private var poiCategories: PoiCategoryModel? = null

    fun findPoi(poiId: String?): Poi? = poiIds[poiId]

    fun clearItems() {
        poiIds.clear()
    }

    suspend fun searchAsync(
        cityId: Int,
        search: String,
        categoryIds: List<Int>? = null
    ): PoisResponse {
        val response = service.getPoiAsync(
            cityId = cityId,
            search = search,
            categoryIds = categoryIds?.toTypedArray()
        )
        response.data?.forEach { poi -> poiIds[poi.id] = poi }
        return response
    }

    suspend fun getPoiWithCategoriesAsync(
        cityId: Int,
        categoryIds: List<Int>,
        page: Int,
        limit: Int?
    ): PoisResponse {
        val response = service.getPoiAsync(
            cityId = cityId,
            categoryIds = categoryIds.toTypedArray(),
            page = page,
            limit = limit
        )
        response.data?.forEach { poi -> poiIds[poi.id] = poi }
        return response
    }

    suspend fun getPoiInfoAsync(poiId: String): PoiResponse {
        poiIds[poiId]?.let { cached ->
            return PoiResponse().apply { data = cached }
        }
        val response = service.getPoiInfoAsync(poiId)
        response.data?.let { poiIds[it.id] = it }
        return response
    }

    suspend fun getPoiCategoriesAsync(): PoiCategoriesResponse {
        poiCategories?.let { cached ->
            return PoiCategoriesResponse().apply { data = cached }
        }
        val response = service.getPoiCategoriesAsync()
        poiCategories = response.data
        return response
    }

    suspend fun searchWithFiltersAsync(
        cityId: Int,
        search: String? = null,
        categoryIds: List<Int>? = null,
        page: Int = 1,
        limit: Int = 30,
        sort: String? = null,
        order: String? = null,
        minPrice: Int? = null,
        maxPrice: Int? = null
    ): PoisResponse {
        val priceParam = if (minPrice != null || maxPrice != null) {
            "${minPrice ?: 0},${maxPrice ?: 1500}"
        } else null
        val response = service.getPoiAsync(
            cityId = cityId, search = search,
            categoryIds = categoryIds?.toTypedArray(),
            page = page, limit = limit,
            sort = sort, order = order, price = priceParam
        )
        response.data?.forEach { poi -> poiIds[poi.id] = poi }
        return response
    }
}

fun Poi.convertToPlaceItem(): PlaceItem {
    return PlaceItem().apply {
        id = this@convertToPlaceItem.id
        title = this@convertToPlaceItem.name ?: ""
        image = this@convertToPlaceItem.image?.url
        category = this@convertToPlaceItem.category?.joinToString(", ") { poiCategory ->
            poiCategory.name ?: ""
        } ?: ""

        if (this@convertToPlaceItem.enableRating()) {
            ratingCount = this@convertToPlaceItem.ratingCount ?: 0
            rating = this@convertToPlaceItem.rating
        } else {
            ratingCount = -1
            rating = -1f
        }
    }
}
