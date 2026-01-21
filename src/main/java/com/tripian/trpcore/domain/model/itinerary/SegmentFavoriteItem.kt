package com.tripian.trpcore.domain.model.itinerary

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Activities that the user has added to favorites
 * Used as activityIds when creating Smart Recommendations.
 *
 * ID Format: "C_{customId}_{providerId}" (e.g., "C_15423_15")
 */
@Parcelize
data class SegmentFavoriteItem(
    val activityId: String? = null,         // Format: "C_15423_15"
    val title: String,                      // Activity title
    val cityName: String,                   // City name
    val cityId: Int? = null,                // City ID
    val photoUrl: String? = null,           // Image URL
    val description: String? = null,        // Description
    val activityUrl: String? = null,        // Detail URL
    val coordinate: ItineraryCoordinate,    // Location
    val rating: Double? = null,             // Rating (0-5)
    val ratingCount: Int? = null,           // Review count
    val cancellation: String? = null,       // Cancellation policy
    val duration: Double? = null,           // Duration (minutes)
    val price: SegmentActivityPrice? = null,// Price information
    val locations: List<String>? = null     // Location descriptions
) : Parcelable
