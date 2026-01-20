package com.tripian.trpcore.domain.model.timeline

import java.io.Serializable

/**
 * StepRouteInfo
 * Holds route information between two points (starting point or steps)
 *
 * @param distanceMeters Distance in meters from Mapbox API
 * @param durationSeconds Duration in seconds from Mapbox API
 * @param isWalking Whether walking profile was used (< 1.8km)
 * @param fromStepId Source step ID (null if starting from accommodation/city center)
 * @param toStepId Destination step ID
 */
data class StepRouteInfo(
    val distanceMeters: Double,
    val durationSeconds: Double,
    val isWalking: Boolean,
    val fromStepId: Int? = null,
    val toStepId: Int
) : Serializable {

    companion object {
        // Walking threshold in meters (1.8 km)
        const val WALKING_THRESHOLD_METERS = 1800.0

        // Walking speed assumption: ~5 km/h = ~12 min/km
        private const val WALKING_MINUTES_PER_KM = 12.0
    }

    /**
     * Distance in kilometers, minimum 0.1 km
     */
    val distanceKm: Double
        get() = (distanceMeters / 1000.0).coerceAtLeast(0.1)

    /**
     * Duration in minutes
     * For walking < 2km, uses heuristic calculation (12 min/km)
     * Otherwise uses Mapbox duration
     */
    val durationMinutes: Int
        get() = if (isWalking && distanceKm < 2.0) {
            // Walking heuristic: ~12 min per km
            (distanceKm * WALKING_MINUTES_PER_KM).toInt().coerceAtLeast(1)
        } else {
            (durationSeconds / 60).toInt().coerceAtLeast(1)
        }

    /**
     * Formatted distance string (e.g., "1,5" for Turkish locale)
     * Returns only the number part for use in format string
     */
    val formattedDistanceValue: String
        get() = String.format(java.util.Locale.getDefault(), "%.1f", distanceKm)

    /**
     * Format route info using the provided format string.
     * Format should be like "%d min (%@ km)" where:
     * - %d is replaced with minutes (integer)
     * - %@ is replaced with distance in km (decimal)
     *
     * @param format The format string from language key
     * @return Formatted route info string (e.g., "5 min (1,2 km)")
     */
    fun formatWithTemplate(format: String): String {
        // Replace %d with minutes and %@ with distance
        return format
            .replace("%d", durationMinutes.toString())
            .replace("%@", formattedDistanceValue)
    }
}
