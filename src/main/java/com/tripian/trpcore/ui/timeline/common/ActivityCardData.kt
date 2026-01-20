package com.tripian.trpcore.ui.timeline.common

import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem

/**
 * Common UI model for activity card
 * Used by both ActivityListing and SavedPlans screens
 */
data class ActivityCardData(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val rating: Double?,
    val ratingCount: Int?,
    val duration: Double?,
    val isRefundable: Boolean,
    val price: Double?,
    val currency: String
) {
    companion object {
        /**
         * Create from TourProduct (Activity Listing)
         */
        fun fromTourProduct(tour: TourProduct): ActivityCardData {
            return ActivityCardData(
                id = tour.id ?: tour.productId ?: "",
                title = tour.title ?: "",
                imageUrl = tour.images?.firstOrNull()?.url,
                rating = tour.rating,
                ratingCount = tour.ratingCount,
                duration = tour.duration,
                isRefundable = tour.tags?.contains("non_refundable") != true,
                price = tour.price,
                currency = tour.currency ?: "EUR"
            )
        }

        /**
         * Create from SegmentFavoriteItem (Saved Plans)
         */
        fun fromFavorite(favorite: SegmentFavoriteItem): ActivityCardData {
            return ActivityCardData(
                id = favorite.activityId ?: "",
                title = favorite.title,
                imageUrl = favorite.photoUrl,
                rating = favorite.rating,
                ratingCount = favorite.ratingCount,
                duration = favorite.duration,
                isRefundable = favorite.cancellation?.lowercase() != "non_refundable",
                price = favorite.price?.value,
                currency = favorite.price?.currency ?: "EUR"
            )
        }
    }
}
