package com.tripian.trpcore.domain.model.itinerary

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Destination/City information
 * Each trip must have at least one destination.
 */
@Parcelize
data class SegmentDestinationItem(
    val title: String,                      // City/destination name (e.g., "Barcelona")
    val coordinate: String,                 // "lat,lon" format (e.g., "41.3851,2.1734")
    val cityId: Int? = null,                // Tripian city ID (retrieved from API, optional)
    val dates: List<String>? = null         // Days in this city ["yyyy-MM-dd", "yyyy-MM-dd"]
) : Parcelable {

    /**
     * Convert coordinate string to ItineraryCoordinate
     */
    fun getCoordinateObject(): ItineraryCoordinate? {
        return ItineraryCoordinate.fromString(coordinate)
    }
}
