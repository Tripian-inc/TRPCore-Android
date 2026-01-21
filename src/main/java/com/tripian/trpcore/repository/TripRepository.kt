package com.tripian.trpcore.repository

import com.tripian.one.api.cities.model.City
import com.tripian.one.api.cities.model.GetCitiesResponse
import com.tripian.one.api.cities.model.GetCityResponse
import com.tripian.one.api.trip.model.DeleteResponse
import com.tripian.one.api.trip.model.TripRequest
import com.tripian.one.api.trip.model.TripResponse
import com.tripian.one.api.trip.model.TripsResponse
import com.tripian.trpcore.R
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class TripRepository @Inject constructor(val service: Service) {

    private var items = ArrayList<City>()

    /**
     * Pre-fetch cities and cache them.
     * Called at SDK initialization to ensure cities are available throughout the app.
     * @return Observable<Boolean> - true if cities were fetched/cached successfully
     */
    fun prefetchCities(): Observable<Boolean> {
        return if (items.isEmpty()) {
            service.getCities(null, 1000, null).map { response ->
                response.data?.let { list ->
                    val sortedCities = list.sortedBy { it.name }
                    items.clear()
                    items.addAll(sortedCities)
                }
                true
            }
        } else {
            Observable.just(true)
        }
    }

    /**
     * Get a city by ID from the cache.
     * @param cityId City ID to look up
     * @return City if found, null otherwise
     */
    fun getCachedCityById(cityId: Int): City? {
        return items.find { it.id == cityId }
    }

    /**
     * Get all cached cities.
     * @return List of cached cities (empty if not yet fetched)
     */
    fun getCachedCities(): List<City> {
        return items.toList()
    }

    /**
     * Check if cities are cached.
     * @return true if cities are available in cache
     */
    fun hasCachedCities(): Boolean {
        return items.isNotEmpty()
    }

    /**
     * Find a city by name from the cache.
     * Optionally filter by country name for more accurate matching.
     *
     * @param cityName City name to search (case-insensitive)
     * @param countryName Optional country name for more precise matching
     * @return City if found, null otherwise
     */
    fun findCityByName(cityName: String, countryName: String? = null): City? {
        val normalizedCityName = cityName.trim().lowercase()

        return items.find { city ->
            val cityNameMatches = city.name?.trim()?.lowercase() == normalizedCityName

            if (countryName != null && cityNameMatches) {
                // If country is provided, also check country match
                val normalizedCountryName = countryName.trim().lowercase()
                city.country?.name?.trim()?.lowercase() == normalizedCountryName
            } else {
                cityNameMatches
            }
        }
    }

    fun getUserTrip(from: String?, to: String?, limit: Int, page: Int?): Observable<TripsResponse> {
        return service.getUserTrip(from, to, limit, page)
    }

    fun getCities(search: String?, limit: Int, page: Int?): Observable<GetCitiesResponse> {
        return if (items.isEmpty()) {
            service.getCities(search, limit, page).map { getCitiesResponse ->
                getCitiesResponse.data?.let { list ->
                    val sortedCities = list.sortedBy { it.name }
                    items.clear()
                    items.addAll(sortedCities)
                }

                GetCitiesResponse().apply {
                    data = items
                    status = 200
                }
            }
        } else {
            Observable.just(GetCitiesResponse().apply {
                data = items
                status = 200
            })
        }
    }

    fun getCity(cityId: Int): Observable<GetCityResponse> {
        return service.getCity(cityId).map {
            GetCityResponse().apply {
                data = it.data
                status = 200
            }
        }
    }

    fun fetchTrip(tripHash: String): Observable<TripResponse> {
        return service.fetchTrip(tripHash)
    }

    fun createTrip(request: TripRequest): Observable<TripResponse> {
        return service.createTrip(request)
    }

    fun updateTrip(tripHash: String, request: TripRequest): Observable<TripResponse> {
        return service.updateTrip(tripHash, request)
    }

    fun deleteTrip(tripHash: String): Observable<DeleteResponse> {
        return service.deleteTrip(tripHash)
    }

    fun clearItems() {
        items.clear()
    }

    fun getContinentImage(slug: String): Int {
        return when(slug) {
            "europe" -> R.drawable.im_europa
            "north-america" -> R.drawable.im_north_america
            "south-america" -> R.drawable.im_south_america
            "africa" -> R.drawable.im_africa
            "asia" -> R.drawable.im_asia
            "australia", "oceania" -> R.drawable.im_australia
            else -> {
                R.drawable.im_europa}
        }
    }
}