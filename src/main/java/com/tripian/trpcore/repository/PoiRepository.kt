package com.tripian.trpcore.repository

import com.mapbox.maps.CoordinateBounds
import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.pois.model.PoiCategoriesResponse
import com.tripian.one.api.pois.model.PoiCategoryModel
import com.tripian.one.api.pois.model.PoiResponse
import com.tripian.one.api.pois.model.PoisResponse
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.util.extensions.enableRating
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 19.09.2020.
 */
class PoiRepository @Inject constructor(val service: Service) {

    private var poiIds = HashMap<String, Poi>()
    private var poiCategories: PoiCategoryModel? = null

    fun getPoiAlternatives(ids: List<String>): Observable<PoisResponse> {
        val requestedPoiIds = ArrayList<String>()

        ids.forEach { id ->
            if (!poiIds.containsKey(id)) {
                requestedPoiIds.add(id)
            }
        }

        return if (requestedPoiIds.isNotEmpty()) {
            service.getPoi(poiIds = requestedPoiIds.toTypedArray()).map {
                it.data?.forEach { poi ->
                    poiIds[poi.id] = poi
                }
            }.map {
                PoisResponse().apply {
                    data = poiIds.filter { ids.contains(it.key) }.values.toList()
                }
            }
        } else {
            Observable.just(PoisResponse().apply {
                data = poiIds.filter { ids.contains(it.key) }.values.toList()
            })
        }
    }

    fun getPoiWithCategories(cityId: Int, categoryIds: List<Int>, page: Int, limit: Int?): Observable<PoisResponse> {
        return service.getPoi(cityId = cityId, categoryIds = categoryIds.toTypedArray(), page = page, limit = limit).map {
            it.data?.forEach { poi ->
                poiIds[poi.id] = poi
            }
            it
        }
    }

    fun getPoiWithBounds(bounds: CoordinateBounds, categoryIds: List<Int>?): Observable<PoisResponse> {
        val boundary = "${minOf(bounds.north(), bounds.south())}," +
                "${maxOf(bounds.north(), bounds.south())}," +
                "${minOf(bounds.west(),bounds.east())}," +
                "${maxOf(bounds.west(),bounds.east())}"
        return service.getPoi(boundary = boundary, categoryIds = categoryIds?.toTypedArray()).map {
            it.data?.forEach { poi ->
                poiIds[poi.id] = poi
            }
            it
        }
    }

    fun getPoiInfo(poiId: String): Observable<PoiResponse> {
        return if (poiIds.containsKey(poiId)) {
            Observable.just(PoiResponse().apply {
                data = poiIds[poiId]
            })
        } else {
            service.getPoiInfo(poiId = poiId).map {
                it.data?.let { poiIds[it.id] = it }

                it
            }
        }
    }

    fun getPoiWithTaste(cityId: Int, mustTryIds: Int): Observable<PoisResponse> {
        return service.getPoi(cityId = cityId, mustTryIds = mustTryIds).map {
            it.data?.forEach { poi ->
                poiIds[poi.id] = poi
            }
            it
        }
    }

    fun findPoi(poiId: String?): Poi? {
        return poiIds[poiId]
    }

    fun search(cityId: Int, search: String, categoryIds: List<Int>? = null): Observable<PoisResponse> {
        return service.getPoi(cityId = cityId, search = search, categoryIds = categoryIds?.toTypedArray()).map {
            it.data?.forEach { poi ->
                poiIds[poi.id] = poi
            }
            it
        }
    }

    fun getPoiCategories(): Observable<PoiCategoriesResponse> {
        return if (poiCategories != null) {
            Observable.just(PoiCategoriesResponse().apply {
                data = poiCategories
            })
        } else service.getPoiCategories().map {
            poiCategories = it.data

            it
        }
    }

    fun clearItems() {
        poiIds.clear()
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