package com.tripian.trpcore.domain.model.timeline

import com.tripian.one.api.pois.model.Coordinate

/**
 * RouteCache
 * Singleton cache for storing route calculation results.
 * Caches by coordinate pair to avoid redundant API calls.
 *
 * Cache key is based on rounded coordinates (4 decimal places ~11m precision)
 * to handle minor coordinate variations.
 */
object RouteCache {

    /**
     * Cached route data without step IDs (those are added when returning)
     */
    data class CachedRoute(
        val distanceMeters: Double,
        val durationSeconds: Double,
        val isWalking: Boolean
    )

    // In-memory cache: key -> cached route data
    private val cache = mutableMapOf<String, CachedRoute>()

    // Coordinate precision for cache key (4 decimal places ≈ 11m accuracy)
    private const val COORDINATE_PRECISION = 4

    /**
     * Generate cache key from two coordinates.
     * Rounds to 4 decimal places for consistency.
     */
    fun generateCacheKey(from: Coordinate, to: Coordinate): String {
        val fromLat = roundCoordinate(from.lat)
        val fromLng = roundCoordinate(from.lng)
        val toLat = roundCoordinate(to.lat)
        val toLng = roundCoordinate(to.lng)
        return "${fromLat}_${fromLng}_${toLat}_${toLng}"
    }

    /**
     * Round coordinate to specified precision
     */
    private fun roundCoordinate(value: Double): String {
        return String.format("%.${COORDINATE_PRECISION}f", value)
    }

    /**
     * Get cached route if available
     */
    fun get(from: Coordinate, to: Coordinate): CachedRoute? {
        val key = generateCacheKey(from, to)
        return cache[key]
    }

    /**
     * Put route result in cache
     */
    fun put(from: Coordinate, to: Coordinate, route: CachedRoute) {
        val key = generateCacheKey(from, to)
        cache[key] = route
    }

    /**
     * Put route result in cache from StepRouteInfo
     */
    fun put(from: Coordinate, to: Coordinate, routeInfo: StepRouteInfo) {
        put(from, to, CachedRoute(
            distanceMeters = routeInfo.distanceMeters,
            durationSeconds = routeInfo.durationSeconds,
            isWalking = routeInfo.isWalking
        ))
    }

    /**
     * Check if route is cached
     */
    fun contains(from: Coordinate, to: Coordinate): Boolean {
        val key = generateCacheKey(from, to)
        return cache.containsKey(key)
    }

    /**
     * Clear all cached routes
     */
    fun clear() {
        cache.clear()
    }

    /**
     * Get cache size (for debugging)
     */
    fun size(): Int = cache.size

    /**
     * Convert cached route to StepRouteInfo with step IDs
     */
    fun CachedRoute.toStepRouteInfo(fromStepId: Int?, toStepId: Int): StepRouteInfo {
        return StepRouteInfo(
            distanceMeters = this.distanceMeters,
            durationSeconds = this.durationSeconds,
            isWalking = this.isWalking,
            fromStepId = fromStepId,
            toStepId = toStepId
        )
    }
}
