package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.repository.TimelineRepository
import javax.inject.Inject

/**
 * ResolveCityIdsForActivitiesUseCase
 *
 * tripItems ve favouriteItems için cityId'leri toplu çözme
 * iOS Guide Operation 1: City Resolution
 *
 * BLOCKING operation - diğer sync operasyonları bunu bekler
 */
class ResolveCityIdsForActivitiesUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<Map<String, Int>, ResolveCityIdsForActivitiesUseCase.Params>() {

    data class Params(
        val tripItems: List<SegmentActivityItem>,
        val favouriteItems: List<SegmentFavoriteItem>,
        val existingCityMap: Map<String, Int>
    )

    override fun on(params: Params?) {
        params?.let {
            addObservable {
                // 1. Unique city names topla (tripItems + favourites)
                val cityNames = mutableSetOf<String>()
                it.tripItems.forEach { item ->
                    item.cityName?.let { name -> cityNames.add(name) }
                }
                it.favouriteItems.forEach { item ->
                    cityNames.add(item.cityName)
                }

                // 2. Cache'de olmayanları filtrele
                val missingCities = cityNames.filter { name ->
                    !it.existingCityMap.containsKey(name)
                }

                if (missingCities.isEmpty()) {
                    // Hepsi cache'de, direkt return et
                    return@addObservable io.reactivex.Observable.just(it.existingCityMap)
                }

                // 3. Koordinatları topla (missing cities için)
                val coordinatesToResolve = mutableListOf<Coordinate>()
                val cityNamesList = mutableListOf<String>()

                // tripItems'tan koordinatları topla
                it.tripItems.forEach { item ->
                    if (item.cityName != null && item.cityName in missingCities) {
                        coordinatesToResolve.add(
                            Coordinate().apply {
                                lat = item.coordinate.lat
                                lng = item.coordinate.lng
                            }
                        )
                        cityNamesList.add(item.cityName)
                    }
                }

                // favouriteItems'tan koordinatları topla
                it.favouriteItems.forEach { item ->
                    if (item.cityName in missingCities) {
                        coordinatesToResolve.add(
                            Coordinate().apply {
                                lat = item.coordinate.lat
                                lng = item.coordinate.lng
                            }
                        )
                        cityNamesList.add(item.cityName)
                    }
                }

                if (coordinatesToResolve.isEmpty()) {
                    return@addObservable io.reactivex.Observable.just(it.existingCityMap)
                }

                // 4. API'den batch resolve
                repository.resolveCities(coordinatesToResolve)
                    .map { resolvedList ->
                        val newMap = it.existingCityMap.toMutableMap()

                        // Resolved cityId'leri map'e ekle
                        resolvedList.forEachIndexed { index, resolveData ->
                            if (index < cityNamesList.size) {
                                val cityName = cityNamesList[index]
                                val resolvedCityId = resolveData.cityId ?: 0

                                if (resolvedCityId > 0) {
                                    newMap[cityName] = resolvedCityId
                                }
                            }
                        }

                        newMap as Map<String, Int>
                    }
                    .onErrorReturn { error ->
                        android.util.Log.e(
                            "SYNC",
                            "City resolution failed: ${error.message}"
                        )
                        it.existingCityMap // Fallback: mevcut map'i döndür
                    }
            }
        }
    }
}
