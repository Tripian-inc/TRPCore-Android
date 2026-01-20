package com.tripian.trpcore.domain.model.itinerary

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Destinasyon/Şehir bilgisi
 * Her seyahatte en az bir destinasyon olmalıdır.
 */
@Parcelize
data class SegmentDestinationItem(
    val title: String,                      // Şehir/destinasyon adı (örn: "Barcelona")
    val coordinate: String,                 // "lat,lon" formatında (örn: "41.3851,2.1734")
    val cityId: Int? = null,                // Tripian city ID (API'den alınır, opsiyonel)
    val dates: List<String>? = null         // Bu şehirdeki günler ["yyyy-MM-dd", "yyyy-MM-dd"]
) : Parcelable {

    /**
     * Coordinate string'i ItineraryCoordinate'e dönüştür
     */
    fun getCoordinateObject(): ItineraryCoordinate? {
        return ItineraryCoordinate.fromString(coordinate)
    }
}
