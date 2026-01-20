package com.tripian.trpcore.domain.model.timeline

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import java.io.Serializable

/**
 * Represents a saved item that can be selected as starting point.
 * Can be either a booked activity or a favourite activity.
 */
sealed class SavedItem : Serializable {

    data class BookedActivity(val segment: TimelineSegment) : SavedItem()
    data class FavouriteActivity(val item: SegmentFavoriteItem) : SavedItem()

    val title: String
        get() = when (this) {
            is BookedActivity -> segment.title ?: segment.additionalData?.title ?: ""
            is FavouriteActivity -> item.title
        }

    val cityName: String?
        get() = when (this) {
            is BookedActivity -> null // TimelineSegment doesn't have city name directly
            is FavouriteActivity -> item.cityName
        }

    val cityId: Int?
        get() = when (this) {
            is BookedActivity -> segment.cityId
            is FavouriteActivity -> item.cityId
        }

    val coordinate: Coordinate?
        get() = when (this) {
            is BookedActivity -> segment.coordinate ?: segment.additionalData?.coordinate
            is FavouriteActivity -> Coordinate().apply {
                lat = item.coordinate.lat
                lng = item.coordinate.lng
            }
        }
}
