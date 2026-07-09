package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.repository.TimelineRepository
import javax.inject.Inject

/**
 * ResolveCityIdsForActivitiesUseCase
 *
 * Bulk-resolves cityIds for tripItems and favouriteItems by coordinate.
 * Blocking operation — the other sync operations wait on its result.
 * iOS Reference: Guide Operation 1 (City Resolution)
 */
class ResolveCityIdsForActivitiesUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<Map<String, Int>, ResolveCityIdsForActivitiesUseCase.Params>() {

    data class Params(
        val tripItems: List<SegmentActivityItem>,
        val favouriteItems: List<SegmentFavoriteItem>,
        val existingCityMap: Map<String, Int>
    )

    override suspend fun execute(params: Params): Map<String, Int> {
        val cityNames = mutableSetOf<String>()
        params.tripItems.forEach { item ->
            item.cityName?.let { name -> cityNames.add(name) }
        }
        params.favouriteItems.forEach { item ->
            cityNames.add(item.cityName)
        }

        val missingCities = cityNames.filter { name ->
            !params.existingCityMap.containsKey(name)
        }

        if (missingCities.isEmpty()) {
            return params.existingCityMap
        }

        val coordinatesToResolve = mutableListOf<Coordinate>()
        val cityNamesList = mutableListOf<String>()

        params.tripItems.forEach { item ->
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

        params.favouriteItems.forEach { item ->
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
            return params.existingCityMap
        }

        return try {
            val resolvedList = repository.resolveCitiesAsync(coordinatesToResolve)
            val newMap = params.existingCityMap.toMutableMap()
            resolvedList.forEachIndexed { index, resolveData ->
                if (index < cityNamesList.size) {
                    val cityName = cityNamesList[index]
                    val resolvedCityId = resolveData.cityId ?: 0
                    if (resolvedCityId > 0) {
                        newMap[cityName] = resolvedCityId
                    }
                }
            }
            newMap
        } catch (error: Throwable) {
            android.util.Log.e("SYNC", "City resolution failed: ${error.message}")
            params.existingCityMap
        }
    }
}
