package com.tripian.trpcore.ui.timeline.savedplans

import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem

/**
 * Sealed class representing items in the Saved Plans list
 * Supports city grouping with section headers
 */
sealed class SavedPlansListItem {

    /**
     * Section header showing city name
     */
    data class SectionHeader(
        val cityName: String,
        val cityId: Int?
    ) : SavedPlansListItem()

    /**
     * Activity item (favorite)
     */
    data class ActivityItem(
        val favorite: SegmentFavoriteItem
    ) : SavedPlansListItem()
}
