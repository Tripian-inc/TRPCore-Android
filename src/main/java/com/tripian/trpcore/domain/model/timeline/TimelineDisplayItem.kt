package com.tripian.trpcore.domain.model.timeline

import com.tripian.one.api.cities.model.City
import com.tripian.one.api.timeline.model.TimelinePlan
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.util.LanguageConst
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * TimelineDisplayItem
 * Sealed class representing timeline items to be displayed in the UI
 */
sealed class TimelineDisplayItem : Serializable {
    abstract val startTime: Date?
    abstract val city: City?
    abstract val segmentIndex: Int?
    abstract val order: Int  // Order number within each city group (1-based)

    /**
     * Section Header - City grouping header
     */
    data class SectionHeader(
        val cityName: String,
        override val city: City? = null
    ) : TimelineDisplayItem() {
        override val startTime: Date? = null
        override val segmentIndex: Int? = null
        override val order: Int = 0
    }

    /**
     * Section Footer - Separator between city groups
     */
    data class SectionFooter(
        override val city: City? = null
    ) : TimelineDisplayItem() {
        override val startTime: Date? = null
        override val segmentIndex: Int? = null
        override val order: Int = 0
    }

    /**
     * Booked Activity - Activity with completed reservation
     */
    data class BookedActivity(
        val segment: TimelineSegment,
        val isReserved: Boolean,
        override val segmentIndex: Int? = null,
        override val city: City? = null,
        override val order: Int = 1
    ) : TimelineDisplayItem() {
        override val startTime: Date?
            get() = segment.startDate?.toDate()

        val title: String
            get() = segment.additionalData?.title ?: segment.title ?: ""

        val imageUrl: String?
            get() = segment.additionalData?.imageUrl

        val duration: Double?
            get() = if (isReserved) segment.additionalData?.duration else null

        val price: Double?
            get() = if (isReserved) segment.additionalData?.price else null

        val currency: String?
            get() = segment.additionalData?.currency

        val cancellation: String?
            get() = segment.additionalData?.cancellation

        val adults: Int
            get() = segment.adults

        val children: Int
            get() = segment.children

        val description: String?
            get() = segment.additionalData?.description ?: segment.description

        val startDateTime: String?
            get() = segment.additionalData?.startDatetime ?: segment.startDate

        val endDateTime: String?
            get() = segment.additionalData?.endDatetime ?: segment.endDate
    }

    /**
     * Recommendations - Plan containing AI recommendations
     */
    data class Recommendations(
        val plan: TimelinePlan,
        val steps: List<TimelineStep>,
        val segment: TimelineSegment? = null,
        override val segmentIndex: Int? = null,
        var isExpanded: Boolean = true,
        override val order: Int = 1,
        val startingOrder: Int = 1,  // Starting order for steps (sequential numbering)
        var routeInfoList: List<StepRouteInfo> = emptyList(),  // Route info between steps
        val cachedCity: City? = null  // City from cache with full coordinate data
    ) : TimelineDisplayItem() {
        override val startTime: Date?
            get() = plan.startDate.toDate()

        override val city: City?
            get() = cachedCity ?: plan.city

        val title: String
            get() = segment?.title ?: plan.name ?: "Recommendations"

        val isGenerating: Boolean
            get() = plan.generatedStatus == 0

        val hasNoPois: Boolean
            get() = plan.generatedStatus == -1

        /**
         * Starting point name for the segment.
         * If accommodation exists, returns accommodation name.
         * Otherwise, returns "City Name | City Center".
         */
        val startingPointName: String?
            get() {
                // If segment has accommodation, use accommodation name
                val accommodation = segment?.accommodation
                if (accommodation?.name != null) {
                    return accommodation.name
                }

                // Otherwise, use city name with "City Center" suffix
                // Use cachedCity first (has full data), then fall back to plan.city
                val cityName =
                    cachedCity?.name ?: plan.city?.name ?: segment?.cityId?.let { "City" }
                return if (cityName != null) "$cityName | ${
                    TRPCore.core.miscRepository.getLanguageValueForKey(
                        LanguageConst.ADD_PLAN_CITY_CENTER
                    )
                }" else null
            }

        /**
         * Starting point coordinate.
         * Priority: segment.coordinate > accommodation.coordinate > cachedCity.coordinate > plan.city.coordinate
         */
        val startingPointCoordinate: com.tripian.one.api.pois.model.Coordinate?
            get() {
                // First, try segment's direct coordinate (starting point from API)
                segment?.coordinate?.let { coord ->
                    if (coord.lat != 0.0 && coord.lng != 0.0) {
                        return coord
                    }
                }

                // If segment has accommodation, use accommodation coordinate
                val accommodation = segment?.accommodation
                if (accommodation?.coordinate != null) {
                    val coord = accommodation.coordinate
                    if (coord != null && coord.lat != 0.0 && coord.lng != 0.0) {
                        return coord
                    }
                }

                // Try cachedCity coordinate (from cache, has full data)
                cachedCity?.coordinate?.let { coord ->
                    if (coord.lat != 0.0 && coord.lng != 0.0) {
                        return coord
                    }
                }

                // Finally, fall back to plan.city coordinate
                return plan.city?.coordinate
            }
    }

    /**
     * Manual POI - Place manually added by user
     */
    data class ManualPoi(
        val step: TimelineStep,
        val segment: TimelineSegment? = null,
        override val segmentIndex: Int? = null,
        override val city: City? = null,
        override val order: Int = 1
    ) : TimelineDisplayItem() {
        // Use step.startDateTimes first, fallback to segment.startDate
        override val startTime: Date?
            get() = step.startDateTimes?.toDate() ?: segment?.startDate?.toDate()

        // End time from step or segment
        val endTime: Date?
            get() = step.endDateTimes?.toDate() ?: segment?.endDate?.toDate()

        val title: String
            get() = step.poi?.name ?: segment?.title ?: ""

        val imageUrl: String?
            get() = step.poi?.image?.url

        val address: String?
            get() = step.poi?.address

        val categoryName: String?
            get() = step.poi?.category?.firstOrNull()?.name

        val rating: Float?
            get() = step.poi?.rating

        val reviewCount: Int?
            get() = step.poi?.ratingCount

        val stepId: Int?
            get() = step.id
    }

    /**
     * Empty State - Empty day display
     */
    data class EmptyState(
        val message: String
    ) : TimelineDisplayItem() {
        override val startTime: Date? = null
        override val city: City? = null
        override val segmentIndex: Int? = null
        override val order: Int = 0
    }

    /**
     * Generating State - Plan being generated display
     */
    data class GeneratingState(
        val message: String = "Generating your itinerary..."
    ) : TimelineDisplayItem() {
        override val startTime: Date? = null
        override val city: City? = null
        override val segmentIndex: Int? = null
        override val order: Int = 0
    }
}

/**
 * Extension: Convert String to Date
 */
fun String?.toDate(): Date? {
    if (this == null) return null
    return try {
        when {
            this.contains(" ") -> {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(this)
            }

            this.contains("T") -> {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(this)
            }

            else -> {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(this)
            }
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Extension: Convert Date to API string
 */
fun Date.toApiDateString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(this)
}

/**
 * Extension: Convert Date to API datetime string
 */
fun Date.toApiDateTimeString(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(this)
}

/**
 * Extension: Convert Date to time string
 */
fun Date.toTimeString(): String {
    return SimpleDateFormat("HH:mm", Locale.US).format(this)
}

/**
 * Extension: Generate date range
 * Ignores time component - only compares dates
 */
fun generateDateRange(startDate: Date?, endDate: Date?): List<Date> {
    if (startDate == null || endDate == null) return emptyList()

    val dates = mutableListOf<Date>()

    // Normalize start date to beginning of day (00:00:00)
    val startCalendar = Calendar.getInstance().apply {
        time = startDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Normalize end date to beginning of day (00:00:00)
    val endCalendar = Calendar.getInstance().apply {
        time = endDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    while (!startCalendar.after(endCalendar)) {
        dates.add(startCalendar.time)
        startCalendar.add(Calendar.DAY_OF_MONTH, 1)
    }

    return dates
}
