package com.tripian.trpcore.domain.model.itinerary

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Activities that the user has added to favorites
 * Used as activityIds when creating Smart Recommendations.
 */
@Parcelize
data class SegmentFavoriteItem(
    val activityId: String? = null,         // e.g., "15423"
    val title: String,
    val cityName: String,
    val cityId: Int? = null,
    val photoUrl: String? = null,
    val description: String? = null,
    val activityUrl: String? = null,
    val coordinate: ItineraryCoordinate,
    val rating: Double? = null,             // Rating (0-5)
    val ratingCount: Int? = null,
    val cancellation: String? = null,
    val duration: Double? = null,           // Duration (minutes)
    val price: SegmentActivityPrice? = null,
    val locations: List<String>? = null     // Location descriptions
) : Parcelable
