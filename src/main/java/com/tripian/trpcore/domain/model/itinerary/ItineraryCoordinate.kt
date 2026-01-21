package com.tripian.trpcore.domain.model.itinerary

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Location information (lat/lng)
 * Compatible with TRPOne's Coordinate but Parcelable
 */
@Parcelize
data class ItineraryCoordinate(
    val lat: Double,
    val lng: Double
) : Parcelable {

    /**
     * Convert to "lat,lng" format
     */
    fun toCoordinateString(): String = "$lat,$lng"

    companion object {
        /**
         * Parse from "lat,lng" format
         */
        fun fromString(coordinateString: String): ItineraryCoordinate? {
            return try {
                val parts = coordinateString.split(",")
                if (parts.size == 2) {
                    ItineraryCoordinate(
                        lat = parts[0].trim().toDouble(),
                        lng = parts[1].trim().toDouble()
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
