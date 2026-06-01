package com.tripian.trpcore.domain.model.itinerary

import android.os.Parcelable
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.TRPCore
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
     * NOTE: Always returns null - cityId from itinerary model is NOT used.
     * Host app sends garbage/invalid cityIds, so we resolve from coordinates instead.
     */
    fun getFirstCityId(): Int? = null

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
     * Converts tripItems to TimelineSegmentSettings list and appends a single
     * TimelineDate control segment carrying the trip's date range.
     *
     * @param defaultCityId Fallback cityId for TimelineDate when destinationItems has none
     */
    fun createSegmentsFromTripItems(defaultCityId: Int? = null): List<TimelineSegmentSettings> {
        val segments = tripItems?.map { item ->
            createBookedActivitySegment(item)
        }?.toMutableList() ?: mutableListOf()

        // iOS contract: TimelineDate is inserted at index 0 so server-side
        // index stability holds across re-fetches.
        buildTimelineDateSegment(defaultCityId)?.let { segments.add(0, it) }

        return segments
    }

    /**
     * Builds the TimelineDate control segment matching the iOS payload:
     * "yyyy-MM-dd 00:00" / "yyyy-MM-dd 23:59", available=false, doNotGenerate=1,
     * distinctPlan=true.
     *
     * Accepts host date strings in any of: "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
     * or "yyyy-MM-dd'T'HH:mm:ss" — we only need the date prefix.
     */
    internal fun buildTimelineDateSegment(defaultCityId: Int? = null): TimelineSegmentSettings? {
        val startDatePart = extractDatePart(startDatetime) ?: return null
        val endDatePart = extractDatePart(endDatetime) ?: return null

        return TimelineSegmentSettings().apply {
            this.title = "TimelineDate"
            this.startDate = "$startDatePart 00:00"
            this.endDate = "$endDatePart 23:59"
            this.segmentType = "itinerary"
            this.cityId = destinationItems.firstOrNull()?.cityId ?: defaultCityId
            this.adults = getAdultCount()
            this.children = getChildCount()
            this.available = false
            this.doNotGenerate = 1
            this.distinctPlan = true
            this.currency = TRPCore.core.getCurrentCurrency()
        }
    }

    /**
     * Pulls the "yyyy-MM-dd" prefix off a host-supplied datetime. Mirrors iOS
     * `extractDateString` but is tolerant of the ISO `T` separator so hosts
     * that send `"2026-06-15T09:00:00"` still get a usable TimelineDate.
     */
    private fun extractDatePart(datetime: String): String? {
        if (datetime.isEmpty()) return null
        // Date prefix is always the first 10 chars when the string starts with
        // "yyyy-MM-dd". Anything shorter is malformed.
        val candidate = datetime.take(10)
        return candidate.takeIf { it.length == 10 && it[4] == '-' && it[7] == '-' }
    }

    /**
     * Creates a booked activity segment from SegmentActivityItem.
     * Single source of truth — both initial create and sync paths must use this.
     */
    internal fun createBookedActivitySegment(item: SegmentActivityItem): TimelineSegmentSettings {
        // Calculate endDatetime if not provided (use startDatetime + duration)
        val calculatedEndDatetime = calculateEndDatetime(
            item.startDatetime,
            item.endDatetime,
            item.duration
        )

        return TimelineSegmentSettings().apply {
            title = item.title
            // iOS fallback: a tripItem without datetime borrows the trip-level
            // range so the segment is never left without bounds.
            startDate = item.startDatetime ?: this@ItineraryWithActivities.startDatetime
            endDate = calculatedEndDatetime ?: this@ItineraryWithActivities.endDatetime
            segmentType = "booked_activity"
            available = true
            // NOTE: cityId is NOT set - host app sends garbage/invalid cityIds
            // Server will resolve cityId from coordinate instead
            adults = item.adultCount
            children = item.childCount
            doNotGenerate = 1
            currency = TRPCore.core.getCurrentCurrency()

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
                endDatetime = calculatedEndDatetime
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
     * Calculates endDatetime from startDatetime and duration.
     * If endDatetime is already provided, returns it as-is.
     * If endDatetime is null but startDatetime and duration exist, calculates it.
     *
     * @param startDatetime Start time in "yyyy-MM-dd HH:mm" format
     * @param endDatetime End time in "yyyy-MM-dd HH:mm" format (nullable)
     * @param durationMinutes Duration in minutes (nullable)
     * @return Calculated or existing endDatetime, or null if cannot calculate
     */
    private fun calculateEndDatetime(
        startDatetime: String?,
        endDatetime: String?,
        durationMinutes: Double?
    ): String? {
        // If endDatetime exists, use it
        if (!endDatetime.isNullOrBlank()) {
            return endDatetime
        }

        // If no startDatetime or duration, cannot calculate
        if (startDatetime.isNullOrBlank() || durationMinutes == null || durationMinutes <= 0) {
            return null
        }

        return try {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
            val startDate = formatter.parse(startDatetime) ?: return null

            // Add duration (in minutes) to start time
            val calendar = java.util.Calendar.getInstance()
            calendar.time = startDate
            calendar.add(java.util.Calendar.MINUTE, durationMinutes.toInt())

            formatter.format(calendar.time)
        } catch (e: Exception) {
            null
        }
    }
}
