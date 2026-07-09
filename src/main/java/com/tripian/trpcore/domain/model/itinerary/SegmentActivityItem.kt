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
    val coordinate: ItineraryCoordinate,
    val cancellation: String? = null,
    val adultCount: Int = 1,
    val childCount: Int = 0,
    val bookingUrl: String? = null,
    val duration: Double? = null,           // Duration (minutes)
    val price: SegmentActivityPrice? = null,
    val cityId: Int? = null,                // Optional - resolved from cityName if not provided
    val cityName: String? = null,
    val countryName: String? = null
) : Parcelable
