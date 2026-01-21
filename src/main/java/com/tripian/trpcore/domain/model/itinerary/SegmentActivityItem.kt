package com.tripian.trpcore.domain.model.itinerary

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Activities reserved or purchased by the user
 * Displayed as bookedActivity or reservedActivity segment in timeline.
 */
@Parcelize
data class SegmentActivityItem(
    val activityId: String? = null,         // Activity ID
    val bookingId: String? = null,          // Booking ID
    val title: String? = null,              // Title
    val imageUrl: String? = null,           // Image URL
    val description: String? = null,        // Description
    val startDatetime: String? = null,      // "yyyy-MM-dd HH:mm"
    val endDatetime: String? = null,        // "yyyy-MM-dd HH:mm"
    val coordinate: ItineraryCoordinate,    // Location
    val cancellation: String? = null,       // Cancellation policy
    val adultCount: Int = 1,                // Number of adults
    val childCount: Int = 0,                // Number of children
    val bookingUrl: String? = null,         // Booking URL
    val duration: Double? = null,           // Duration (minutes)
    val price: SegmentActivityPrice? = null,// Price
    val cityId: Int? = null,                // City ID (optional - resolved from cityName if not provided)
    val cityName: String? = null,           // City name (e.g., "Barcelona")
    val countryName: String? = null         // Country name (e.g., "Spain")
) : Parcelable
