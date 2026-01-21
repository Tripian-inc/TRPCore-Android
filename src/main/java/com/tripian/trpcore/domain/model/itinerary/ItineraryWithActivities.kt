package com.tripian.trpcore.domain.model.itinerary

import android.os.Parcelable
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import kotlinx.parcelize.Parcelize

/**
 * Main Itinerary model - Used to start the SDK
 *
 * This model transfers the user's travel information to the SDK.
 * SDK uses this information to create a Timeline or fetch an existing one.
 */
@Parcelize
data class ItineraryWithActivities(
    val tripName: String? = null,                          // Trip name
    val startDatetime: String,                             // Format: "yyyy-MM-dd HH:mm"
    val endDatetime: String,                               // Format: "yyyy-MM-dd HH:mm"
    val uniqueId: String,                                  // User unique ID
    val tripianHash: String? = null,                       // Existing timeline hash if available
    val destinationItems: List<SegmentDestinationItem> = emptyList(),    // Optional - Can fallback to tripItems
    val favouriteItems: List<SegmentFavoriteItem>? = null, // Favorite activities
    val tripItems: List<SegmentActivityItem>? = null       // Booked/Reserved activities
) : Parcelable {

    /**
     * Returns the cityId of the first destination.
     * Falls back to tripItems if destinationItems is empty.
     */
    fun getFirstCityId(): Int? =
        destinationItems.firstOrNull()?.cityId
            ?: tripItems?.firstOrNull()?.cityId

    /**
     * Returns the coordinate of the first destination.
     * Falls back to tripItems if destinationItems is empty.
     */
    fun getFirstCoordinate(): ItineraryCoordinate? =
        destinationItems.firstOrNull()?.getCoordinateObject()
            ?: tripItems?.firstOrNull()?.coordinate

    /**
     * Gets adult count from tripItems (from first item or default 1)
     */
    fun getAdultCount(): Int = tripItems?.firstOrNull()?.adultCount ?: 1

    /**
     * Gets child count from tripItems (from first item or default 0)
     */
    fun getChildCount(): Int = tripItems?.firstOrNull()?.childCount ?: 0

    /**
     * Checks if itinerary has any location data (from destinationItems or tripItems)
     */
    fun hasLocationData(): Boolean =
        destinationItems.isNotEmpty() || !tripItems.isNullOrEmpty()

    /**
     * Returns the city name from first destination or tripItem.
     * Used for city search when cityId is not available.
     */
    fun getFirstCityName(): String? =
        destinationItems.firstOrNull()?.title
            ?: tripItems?.firstOrNull()?.cityName

    /**
     * Returns the country name from first destination or tripItem.
     * Used for more accurate city search.
     */
    fun getFirstCountryName(): String? =
        destinationItems.firstOrNull()?.countryName
            ?: tripItems?.firstOrNull()?.countryName

    /**
     * Returns favorite activity IDs (for Smart Recommendations)
     */
    fun getFavoriteActivityIds(): List<String> {
        return favouriteItems?.mapNotNull { it.activityId } ?: emptyList()
    }

    /**
     * Converts tripItems to TimelineSegmentSettings list and adds empty segments
     * for start/end dates if no activity exists on those dates.
     *
     * Logic:
     * 1. Create segments from tripItems (booked_activity type)
     * 2. Check if startDate has an activity, if not add empty segment at index 0
     * 3. Check if endDate has an activity, if not add empty segment at end
     */
    fun createSegmentsFromTripItems(): List<TimelineSegmentSettings> {
        val adults = getAdultCount()
        val children = getChildCount()
        val cityId = getFirstCityId()

        // Create segments from tripItems (booked activities)
        val segments = tripItems?.map { item ->
            createBookedActivitySegment(item)
        }?.toMutableList() ?: mutableListOf()

        // Extract date strings (yyyy-MM-dd format)
        val startDateStr = extractDateString(startDatetime) ?: return segments
        val endDateStr = extractDateString(endDatetime) ?: return segments

        // Check if there's a tripItem on the start date
        val hasItemOnStartDate = tripItems?.any { item ->
            val itemDate = item.startDatetime ?: return@any false
            extractDateString(itemDate) == startDateStr
        } ?: false

        // Check if there's a tripItem on the end date
        val hasItemOnEndDate = tripItems?.any { item ->
            val itemDate = item.startDatetime ?: return@any false
            extractDateString(itemDate) == endDateStr
        } ?: false

        // Add empty segment for start date if needed (at index 0)
        if (!hasItemOnStartDate) {
            val emptyStartSegment = createEmptySegment(
                date = startDateStr,
                adults = adults,
                children = children,
                cityId = cityId
            )
            segments.add(0, emptyStartSegment)
        }

        // Add empty segment for end date if needed (and different from start)
        if (!hasItemOnEndDate && startDateStr != endDateStr) {
            val emptyEndSegment = createEmptySegment(
                date = endDateStr,
                adults = adults,
                children = children,
                cityId = cityId
            )
            segments.add(emptyEndSegment)
        }

        return segments
    }

    /**
     * Creates a booked activity segment from SegmentActivityItem
     */
    private fun createBookedActivitySegment(item: SegmentActivityItem): TimelineSegmentSettings {
        return TimelineSegmentSettings().apply {
            title = item.title
            startDate = item.startDatetime
            endDate = item.endDatetime
            segmentType = "booked_activity"
            available = true
            cityId = item.cityId
            adults = item.adultCount
            children = item.childCount
            doNotGenerate = 1

            // Coordinate
            item.coordinate.let { coord ->
                coordinate = com.tripian.one.api.pois.model.Coordinate().apply {
                    lat = coord.lat
                    lng = coord.lng
                }
            }

            // Additional data for booked activity
            additionalData = TimelineSegmentAdditionalData().apply {
                activityId = item.activityId
                bookingId = item.bookingId
                this.title = item.title
                imageUrl = item.imageUrl
                description = item.description
                startDatetime = item.startDatetime
                endDatetime = item.endDatetime
                cancellation = item.cancellation
                duration = item.duration
                item.price?.let { price ->
                    this.price = price.value
                    this.currency = price.currency
                }
                this.coordinate = com.tripian.one.api.pois.model.Coordinate().apply {
                    lat = item.coordinate.lat
                    lng = item.coordinate.lng
                }
            }
        }
    }

    /**
     * Creates an empty segment for days without activities.
     * This ensures the timeline covers the full trip date range.
     *
     * @param date Date string in "yyyy-MM-dd" format
     * @param adults Number of adults
     * @param children Number of children
     * @param cityId City ID from destination
     */
    private fun createEmptySegment(
        date: String,
        adults: Int,
        children: Int,
        cityId: Int?
    ): TimelineSegmentSettings {
        return TimelineSegmentSettings().apply {
            this.title = "Empty"
            this.startDate = "$date 00:00"
            this.endDate = "$date 23:59"
            this.segmentType = "itinerary"
            this.available = false
            this.cityId = cityId
            this.adults = adults
            this.children = children
            this.doNotGenerate = 1
        }
    }

    /**
     * Extracts date string (yyyy-MM-dd) from datetime string (yyyy-MM-dd HH:mm)
     */
    private fun extractDateString(datetime: String): String? {
        val parts = datetime.split(" ")
        return parts.firstOrNull()
    }
}
