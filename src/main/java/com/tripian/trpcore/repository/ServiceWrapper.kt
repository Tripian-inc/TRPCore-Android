package com.tripian.trpcore.repository

import android.app.Application
import com.tripian.one.TRPRest
import com.tripian.one.api.cities.model.GetCitiesResponse
import com.tripian.one.api.pois.model.PoiCategoriesResponse
import com.tripian.one.api.pois.model.PoiResponse
import com.tripian.one.api.pois.model.PoisResponse
import com.tripian.trpcore.base.awaitCallback
import javax.inject.Inject

/**
 * Thin suspend-only facade around [TRPRest] for the city + POI endpoints the
 * SDK calls through repositories. The legacy Observable-returning interface
 * (Service) was retired with the RxJava → Coroutines migration.
 */
class ServiceWrapper @Inject constructor(
    val app: Application,
    val tone: TRPRest
) {

    suspend fun getCitiesAsync(
        search: String?,
        limit: Int,
        page: Int?
    ): GetCitiesResponse = awaitCallback { ok, fail ->
        tone.cities(
            autoPagination = true,
            search = search,
            countryCode = null,
            page = page,
            limit = limit,
            success = ok,
            error = fail
        )
    }

    suspend fun getPoiAsync(
        poiIds: Array<out String>? = null,
        limit: Int? = null,
        page: Int? = null,
        coordinate: Array<out String>? = null,
        boundary: String? = null,
        distance: Double? = null,
        categoryId: Int? = null,
        categoryIds: Array<out Int>? = null,
        nextUrl: String? = null,
        search: String? = null,
        cityId: Int? = null,
        mustTryIds: Int? = null,
        isAutoPagination: Boolean = true,
        sort: String? = null,
        order: String? = null,
        price: String? = null
    ): PoisResponse = awaitCallback { ok, fail ->
        tone.getPoi(
            isAutoPagination,
            cityId = cityId,
            search = search,
            coordinate = coordinate,
            poiIds = poiIds,
            mustTryIds = mustTryIds,
            categoryIds = categoryIds,
            distance = distance,
            boundary = boundary,
            sort = sort,
            order = order,
            price = price,
            rating = null,
            page = page,
            limit = limit,
            success = ok,
            error = fail
        )
    }

    suspend fun getPoiInfoAsync(poiId: String): PoiResponse = awaitCallback { ok, fail ->
        tone.getPoiDetail(poiId, success = ok, error = fail)
    }

    suspend fun getPoiCategoriesAsync(): PoiCategoriesResponse = awaitCallback { ok, fail ->
        tone.getPoiCategories(success = ok, error = fail)
    }
}
