package com.tripian.trpcore.domain.model.itinerary

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Destination/City information
 * Used to define trip destinations. cityId is optional - SDK can resolve it from cityName/countryName.
 */
@Parcelize
data class SegmentDestinationItem(
    val title: String,                      // City/destination name (e.g., "Barcelona")
    val coordinate: String,                 // "lat,lon" format (e.g., "41.3851,2.1734")
    val cityId: Int? = null,                // Tripian city ID (optional - resolved from cityName if not provided)
    val dates: List<String>? = null,        // Days in this city ["yyyy-MM-dd", "yyyy-MM-dd"]
    val countryName: String? = null         // Country name (e.g., "Spain") - helps with city resolution
) : Parcelable {

    /**
     * Convert coordinate string to ItineraryCoordinate
     */
    fun getCoordinateObject(): ItineraryCoordinate? {
        return ItineraryCoordinate.fromString(coordinate)
    }
}
