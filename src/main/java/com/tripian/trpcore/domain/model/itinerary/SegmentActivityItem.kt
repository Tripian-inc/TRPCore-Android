package com.tripian.trpcore.domain.model.itinerary

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Activities reserved or purchased by the user
 * Displayed as bookedActivity or reservedActivity segment in timeline.
 */
@Parcelize
data class SegmentActivityItem(
    val activityId: String? = null,
    val bookingId: String? = null,
    val title: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val startDatetime: String? = null,      // "yyyy-MM-dd HH:mm"
    val endDatetime: String? = null,        // "yyyy-MM-dd HH:mm"
    val coordinate: ItineraryCoordinate? = null, // Location; null → "no exact location"
    val cancellation: String? = null,       // Cancellation policy
    val adultCount: Int = 1,                // Number of adults
    val childCount: Int = 0,                // Number of children
    val bookingUrl: String? = null,         // Booking URL
    val duration: Double? = null,           // Duration (minutes)
    val price: SegmentActivityPrice? = null,
    val cityId: Int? = null,                // Optional - resolved from cityName if not provided
    val cityName: String? = null,
    val countryName: String? = null
) : Parcelable
