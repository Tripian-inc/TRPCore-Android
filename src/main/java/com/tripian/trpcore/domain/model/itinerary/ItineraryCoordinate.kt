package com.tripian.trpcore.domain.model.itinerary

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Konum bilgisi (lat/lng)
 * TRPOne'daki Coordinate ile uyumlu ancak Parcelable
 */
@Parcelize
data class ItineraryCoordinate(
    val lat: Double,
    val lng: Double
) : Parcelable {

    /**
     * "lat,lng" formatına dönüştür
     */
    fun toCoordinateString(): String = "$lat,$lng"

    companion object {
        /**
         * "lat,lng" formatından parse et
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
