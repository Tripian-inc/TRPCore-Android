package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.TourRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import com.tripian.trpcore.util.extensions.resolveFlexibleWindow
import com.tripian.trpcore.util.extensions.toAdditionalDataIso
import javax.inject.Inject

/**
 * CreateReservedActivitySegmentUseCase
 * Creates a reserved_activity segment for a tour/activity.
 *
 * When the tour carries no usable coordinate, it is resolved through
 * `tour-api/product-lookup` first so the segment has either a real coordinate
 * or an explicit `isNoLocation = true` flag.
 */
class CreateReservedActivitySegmentUseCase @Inject constructor(
    private val timelineRepository: TimelineRepository,
    private val tourRepository: TourRepository
) : SuspendUseCase<ResponseModelBase, CreateReservedActivitySegmentUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val tour: TourProduct,
        val selectedDate: String,      // Format: "yyyy-MM-dd"
        val selectedTimeSlot: String,  // Format: "HH:mm". Ignored when [isFlexible].
        val adults: Int = 1,
        val cityId: Int,
        val slotPrice: Double? = null, // Minimum price from selected time slot (if available)
        /**
         * Flexible (any-time) activity flag. When true, the segment is created
         * with 00:00–23:59 placeholders and duration = -1, rendered as FlexibleActivity.
         */
        val isFlexible: Boolean = false,
        /**
         * Activity ids [selectedDate] already holds, so the engine does not suggest
         * them again for that day.
         */
        val excludedActivityIds: List<String> = emptyList()
    )

    companion object {
        const val SEGMENT_TYPE_RESERVED_ACTIVITY = "reserved_activity"
    }

    override suspend fun execute(params: Params): ResponseModelBase {
        val resolvedTour = resolveTour(params.tour)
        val segment = buildSegment(params, resolvedTour)
        timelineRepository.editSegmentAsync(params.tripHash, segment)
        return ResponseModelBase().apply { status = 200 }
    }

    /**
     * Returns the tour to use when building the segment. If the search-result
     * tour already carries a valid coordinate we trust it; otherwise we fetch
     * the canonical product record (which typically includes coordinate and
     * city). Lookup failures fall back to the original tour — the add still
     * proceeds, the resulting segment is just flagged `isNoLocation`.
     */
    private suspend fun resolveTour(tour: TourProduct): TourProduct {
        if (hasUsableCoordinate(tour)) return tour
        val productId = tour.productId
        if (productId.isEmpty()) return tour
        return try {
            val response = tourRepository.lookupTourProductAsync(
                providerId = tour.providerId,
                productId = productId
            )
            response.data?.product ?: tour
        } catch (_: Throwable) {
            tour
        }
    }

    private fun hasUsableCoordinate(tour: TourProduct): Boolean {
        val loc = tour.locations?.firstOrNull() ?: return false
        return loc.lat != null && loc.lon != null
    }

    /**
     * additionalData datetimes are ISO-8601 while segment-level startDate/endDate
     * stay "yyyy-MM-dd HH:mm" — the two formats are intentional (iOS spec).
     * For flexible activities [resolveFlexibleWindow] keeps today's request ahead
     * of "now", since the backend rejects segments starting in the past.
     */
    private fun buildSegment(p: Params, tour: TourProduct): TimelineSegmentSettings {
        val startDatetime: String
        val endDatetime: String
        val effectiveDuration: Double?
        if (p.isFlexible) {
            val (start, end) = resolveFlexibleWindow(p.selectedDate)
            startDatetime = "${p.selectedDate} $start"
            endDatetime = "${p.selectedDate} $end"
            effectiveDuration = -1.0
        } else {
            startDatetime = "${p.selectedDate} ${p.selectedTimeSlot}"
            endDatetime = calculateEndTime(p.selectedDate, p.selectedTimeSlot, tour.duration)
            effectiveDuration = tour.duration
        }

        val loc = tour.locations?.firstOrNull()
        val coordinate = if (loc?.lat != null && loc.lon != null) {
            Coordinate().apply {
                lat = loc.lat!!
                lng = loc.lon!!
            }
        } else {
            null
        }
        val noLocation = coordinate == null

        val additionalData = TimelineSegmentAdditionalData().apply {
            activityId = tour.productId
            title = tour.title
            imageUrl = tour.images?.firstOrNull()?.url
            this.startDatetime = startDatetime.toAdditionalDataIso()
            this.endDatetime = endDatetime.toAdditionalDataIso()
            this.coordinate = coordinate
            this.isNoLocation = noLocation
            duration = effectiveDuration
            price = p.slotPrice ?: tour.price
            currency = tour.currency ?: "EUR"
            rating = tour.rating
            reviewCount = tour.ratingCount
        }

        val resolvedCityId = if (p.cityId > 0) p.cityId else tour.cityId

        return TimelineSegmentSettings().apply {
            title = tour.title
            cityId = resolvedCityId
            startDate = startDatetime
            endDate = endDatetime
            adults = p.adults
            segmentType = SEGMENT_TYPE_RESERVED_ACTIVITY
            this.additionalData = additionalData
            available = false
            distinctPlan = true
            excludedActivityIds = p.excludedActivityIds.takeIf { it.isNotEmpty() }
            currency = TRPCore.core.getCurrentCurrency()
        }
    }

    /**
     * Calculate end time based on start time and duration
     * @param date Date string (yyyy-MM-dd)
     * @param startTime Start time (HH:mm)
     * @param duration Duration in MINUTES (Double) - API returns duration in minutes
     * @return End datetime string (yyyy-MM-dd HH:mm)
     */
    private fun calculateEndTime(date: String, startTime: String, duration: Double?): String {
        if (duration == null || duration <= 0) {
            return calculateEndTimeWithMinutes(date, startTime, 120)
        }

        val durationMinutes = duration.toInt()
        return calculateEndTimeWithMinutes(date, startTime, durationMinutes)
    }

    private fun calculateEndTimeWithMinutes(date: String, startTime: String, durationMinutes: Int): String {
        try {
            val timeParts = startTime.split(":")
            val startHour = timeParts[0].toInt()
            val startMinute = timeParts[1].toInt()

            val totalMinutes = startHour * 60 + startMinute + durationMinutes
            val endHour = (totalMinutes / 60) % 24
            val endMinute = totalMinutes % 60

            return "$date ${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            return "$date 00:00"
        }
    }
}
