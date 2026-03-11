package com.tripian.trpcore.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.cities.model.GetCitiesResponse
import com.tripian.one.api.cities.model.GetCityResponse
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.trip.model.DeleteResponse
import com.tripian.one.api.trip.model.TripRequest
import com.tripian.one.api.trip.model.TripResponse
import com.tripian.one.api.trip.model.TripsResponse
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.util.Preferences
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class TripRepository @Inject constructor(
    val service: Service,
    val preferences: Preferences
) {

    private var items = ArrayList<City>()
    private val gson = Gson()

    /**
     * Saves cities to SharedPreferences as JSON.
     * Called after successful API fetch.
     */
    private fun saveCitiesToCache() {
        if (items.isNotEmpty()) {
            val json = gson.toJson(items)
            preferences.setString(Preferences.Keys.CACHED_CITIES, json)
        }
    }

    /**
     * Loads cities from SharedPreferences cache.
     * Called when API fetch fails or at initialization.
     */
    private fun loadCitiesFromCache(): List<City> {
        val json = preferences.getString(Preferences.Keys.CACHED_CITIES, "")
        if (json.isNullOrEmpty()) return emptyList()

        return try {
            val type = object : TypeToken<List<City>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Pre-fetch cities and cache them.
     * Called at SDK initialization to ensure cities are available throughout the app.
     * Runs on IO thread to avoid blocking main thread (ANR prevention).
     *
     * Flow:
     * 1. If memory cache is empty, load from SharedPreferences first (fast)
     * 2. Then fetch from API and update both memory and SharedPreferences cache
     * 3. If API fails, use cached data
     *
     * @return Observable<Boolean> - true if cities were fetched/cached successfully
     */
    fun prefetchCities(): Observable<Boolean> {
        return if (items.isEmpty()) {
            // First try to load from SharedPreferences cache (fast)
            val cachedCities = loadCitiesFromCache()
            if (cachedCities.isNotEmpty()) {
                items.clear()
                items.addAll(cachedCities)
            }

            // Then fetch from API and update cache
            service.getCities(null, 1000, null)
                .subscribeOn(Schedulers.io())
                .map { response ->
                    response.data?.let { list ->
                        val sortedCities = list.sortedBy { it.name }
                        items.clear()
                        items.addAll(sortedCities)
                        saveCitiesToCache()  // Save to SharedPreferences
                    }
                    true
                }
                .onErrorReturn { error ->
                    // API failed, but we may have cache - return success if we have data
                    items.isNotEmpty()
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

    /**
     * Find a city by coordinate from the cache.
     * Uses distance calculation to find the nearest city within threshold.
     *
     * @param lat Latitude
     * @param lng Longitude
     * @param thresholdKm Maximum distance in kilometers (default 50km)
     * @return City if found within threshold, null otherwise
     */
    fun findCityByCoordinate(lat: Double, lng: Double, thresholdKm: Double = 50.0): City? {
        return items.filter { city ->
            city.coordinate?.let { coord ->
                val distance = calculateDistance(lat, lng, coord.lat, coord.lng)
                distance <= thresholdKm
            } ?: false
        }.minByOrNull { city ->
            city.coordinate?.let { coord ->
                calculateDistance(lat, lng, coord.lat, coord.lng)
            } ?: Double.MAX_VALUE
        }
    }

    /**
     * Haversine formula for distance calculation between two coordinates.
     *
     * @return Distance in kilometers
     */
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    /**
     * Resolve cities by coordinates using API.
     * Used when city is not found in cache.
     *
     * @param coordinates List of coordinates to resolve
     * @return Observable list of resolved City objects
     */
    fun resolveCitiesByCoordinates(coordinates: List<Coordinate>): Observable<List<City>> {
        return Observable.create { emitter ->
            TRPCore.core.trpRest.resolveCitiesByCoordinates(
                coordinates = coordinates,
                success = { response ->
                    val cities = response.data?.mapNotNull { resolveData ->
                        resolveData.cityId?.let { cityId ->
                            getCachedCityById(cityId)
                        }
                    } ?: emptyList()
                    emitter.onNext(cities)
                    emitter.onComplete()
                },
                error = { error ->
                    emitter.onError(error ?: Exception("City resolve failed"))
                }
            )
        }
    }

    fun getUserTrip(from: String?, to: String?, limit: Int, page: Int?): Observable<TripsResponse> {
        return service.getUserTrip(from, to, limit, page)
    }

    fun getCities(search: String?, limit: Int, page: Int?): Observable<GetCitiesResponse> {
        return if (items.isEmpty()) {
            // Try SharedPreferences cache first
            val cachedCities = loadCitiesFromCache()
            if (cachedCities.isNotEmpty()) {
                items.addAll(cachedCities)
            }

            service.getCities(search, limit, page).map { getCitiesResponse ->
                getCitiesResponse.data?.let { list ->
                    val sortedCities = list.sortedBy { it.name }
                    items.clear()
                    items.addAll(sortedCities)
                    saveCitiesToCache()  // Save to SharedPreferences
                }

                GetCitiesResponse().apply {
                    data = items
                    status = 200
                }
            }.onErrorReturn { error ->
                // API failed, return cached data
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