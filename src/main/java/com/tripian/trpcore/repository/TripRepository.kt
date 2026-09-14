package com.tripian.trpcore.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.base.awaitCallback
import com.tripian.trpcore.util.CityTimeZones
import com.tripian.trpcore.util.Preferences
import javax.inject.Inject

class TripRepository @Inject constructor(
    val service: ServiceWrapper,
    val preferences: Preferences
) {

    @Volatile
    private var items: List<City> = emptyList()
    private val gson = Gson()

    private fun saveCitiesToCache(cities: List<City>) {
        if (cities.isNotEmpty()) {
            val json = gson.toJson(cities)
            preferences.setString(Preferences.Keys.CACHED_CITIES, json)
        }
    }

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
     * Pre-fetches cities and caches them. Called after [appLanguage] is set so
     * the fetched names match the requested language. Safe to call repeatedly.
     *
     * Flow:
     * 1. If memory cache is empty, load from SharedPreferences first (fast).
     * 2. Always fetch from API and update both memory and SharedPreferences.
     * 3. On API failure, fall back to whatever is already in the in-memory cache.
     *
     * City timezones are registered by id so time/day pickers can resolve the
     * city clock even when the selected city object carries no timezone.
     *
     * The cache is published as an immutable snapshot so concurrent readers on
     * other threads never observe a partially replaced list.
     */
    suspend fun prefetchCitiesAsync(): Boolean {
        if (items.isEmpty()) {
            val cachedCities = loadCitiesFromCache()
            if (cachedCities.isNotEmpty()) {
                items = cachedCities
                CityTimeZones.register(cachedCities)
            }
        }
        return try {
            val response = service.getCitiesAsync(null, 1000, null)
            response.data?.let { list ->
                val sortedCities = list.sortedBy { it.name }
                items = sortedCities
                saveCitiesToCache(sortedCities)
            }
            CityTimeZones.register(items)
            true
        } catch (_: Throwable) {
            val snapshot = items
            CityTimeZones.register(snapshot)
            snapshot.isNotEmpty()
        }
    }

    fun getCachedCityById(cityId: Int): City? = items.find { it.id == cityId }

    fun getCachedCities(): List<City> = items.toList()

    fun hasCachedCities(): Boolean = items.isNotEmpty()

    /**
     * Find a city by name from the cache.
     * @param cityName City name to search (case-insensitive)
     * @param countryName Optional country name for more precise matching
     */
    fun findCityByName(cityName: String, countryName: String? = null): City? {
        val normalizedCityName = cityName.trim().lowercase()
        return items.find { city ->
            val cityNameMatches = city.name?.trim()?.lowercase() == normalizedCityName
            if (countryName != null && cityNameMatches) {
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

    /** Haversine formula — distance in kilometers. */
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    /**
     * Resolve cities by coordinates using API; used when the city is not
     * found in cache. Throws on network error.
     */
    suspend fun resolveCitiesByCoordinatesAsync(
        coordinates: List<Coordinate>
    ): CityResolveResult = awaitCallback { ok, fail ->
        TRPCore.core.trpRest.resolveCitiesByCoordinates(
            coordinates = coordinates,
            success = { response ->
                val resolvedCities = mutableListOf<City>()
                val unresolvedCityNames = mutableListOf<String>()

                response.data?.forEach { resolveData ->
                    if (resolveData.cityId == null || resolveData.cityId == 0) {
                        resolveData.cityName?.let { unresolvedCityNames.add(it) }
                    } else {
                        getCachedCityById(resolveData.cityId!!)?.let {
                            resolvedCities.add(it)
                        }
                    }
                }

                val result = when {
                    resolvedCities.isEmpty() ->
                        CityResolveResult.AllFailed(unresolvedCityNames)
                    unresolvedCityNames.isNotEmpty() ->
                        CityResolveResult.PartialSuccess(resolvedCities, unresolvedCityNames)
                    else ->
                        CityResolveResult.Success(resolvedCities)
                }
                ok(result)
            },
            error = { error ->
                fail(error ?: Exception("City resolve failed"))
            }
        )
    }

    fun clearItems() {
        items = emptyList()
    }

    fun getContinentImage(slug: String): Int {
        return when (slug) {
            "europe" -> R.drawable.trp_im_europa
            "north-america" -> R.drawable.trp_im_north_america
            "south-america" -> R.drawable.trp_im_south_america
            "africa" -> R.drawable.trp_im_africa
            "asia" -> R.drawable.trp_im_asia
            "australia", "oceania" -> R.drawable.trp_im_australia
            else -> R.drawable.trp_im_europa
        }
    }
}
