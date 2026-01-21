package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.cities.model.City
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSettings
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.itinerary.ItineraryCoordinate
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.TripRepository
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * CreateTimelineUseCase
 * Creates timeline from ItineraryWithActivities model
 */
class CreateTimelineUseCase @Inject constructor(
    private val repository: TimelineRepository,
    private val tripRepository: TripRepository
) : BaseUseCase<Timeline, CreateTimelineUseCase.Params>() {

    data class Params(
        val itinerary: ItineraryWithActivities
    )

    override fun on(params: Params?) {
        params?.let { p ->
            val settings = createTimelineSettings(p.itinerary)
            addObservable {
                repository.createTimeline(settings)
            }
        }
    }

    /**
     * Create TimelineSettings from ItineraryWithActivities
     */
    private fun createTimelineSettings(itinerary: ItineraryWithActivities): TimelineSettings {
        val cityId = resolveCityId(itinerary)
        val segments = itinerary.createSegmentsFromTripItems()

        return TimelineSettings().apply {
            this.cityId = cityId
            this.adults = itinerary.getAdultCount()
            this.children = itinerary.getChildCount()
            this.segments = segments
        }
    }

    /**
     * Resolves city ID from itinerary using fallback chain:
     * 1. destinationItems.cityId or tripItems.cityId
     * 2. Search by cityName + countryName from cache
     * 3. Find nearest city by coordinate from cache
     */
    private fun resolveCityId(itinerary: ItineraryWithActivities): Int {
        // 1. Try direct cityId from model (already has fallback in getFirstCityId)
        itinerary.getFirstCityId()?.let { return it }

        // 2. Search by cityName + countryName in cached cities
        val cityName = itinerary.getFirstCityName()
        if (cityName != null) {
            val countryName = itinerary.getFirstCountryName()
            tripRepository.findCityByName(cityName, countryName)?.let { city ->
                return city.id
            }
        }

        // 3. Search by coordinate in cached cities
        val coordinate = itinerary.getFirstCoordinate()
            ?: throw IllegalArgumentException(
                "No location data available. Provide either cityId, cityName, or coordinates."
            )

        val nearestCity = findNearestCityByCoordinate(coordinate)
            ?: throw IllegalArgumentException(
                "Could not find city for coordinate: ${coordinate.toCoordinateString()}. Ensure cities are cached."
            )

        return nearestCity.id
    }

    /**
     * Finds the nearest city from cache by coordinate using Haversine formula
     */
    private fun findNearestCityByCoordinate(coordinate: ItineraryCoordinate): City? {
        val cities = tripRepository.getCachedCities()
        if (cities.isEmpty()) return null

        return cities.minByOrNull { city ->
            city.coordinate?.let { cityCoord ->
                calculateDistance(
                    coordinate.lat, coordinate.lng,
                    cityCoord.lat, cityCoord.lng
                )
            } ?: Double.MAX_VALUE
        }
    }

    /**
     * Calculates distance between two coordinates using Haversine formula
     * @return Distance in kilometers
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
