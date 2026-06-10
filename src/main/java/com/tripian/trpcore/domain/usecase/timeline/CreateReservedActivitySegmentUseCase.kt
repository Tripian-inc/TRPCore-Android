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
 * No-location handling (migration Theme 6): when the tour returned by the
 * search endpoint carries no usable coordinate, we resolve it through
 * `tour-api/product-lookup` first so the eventual segment has either a real
 * coordinate from the canonical product record OR an explicit
 * `isNoLocation = true` flag. The previous Elvis-to-(0,0) fallback dropped a
 * marker into the Gulf of Guinea, which is the only spot on the planet that is
 * never a valid activity location.
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
        // Flexible (any-time) activity flag. When true, the segment is created
        // with 00:00–23:59 placeholders and duration = -1; timeline rendering
        // recognises this shape and shows the FlexibleActivity cell.
        val isFlexible: Boolean = false
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

    private fun buildSegment(p: Params, tour: TourProduct): TimelineSegmentSettings {
        // Build start and end datetime (format: "yyyy-MM-dd HH:mm").
        // Flexible activities are pinned to the full-day window 00:00–23:59
        // and carry duration -1 so timeline rendering can recognise them.
        val startDatetime: String
        val endDatetime: String
        val effectiveDuration: Double?
        if (p.isFlexible) {
            // Bugün için backend 00:00 startı reddediyor (geçmişe segment).
            // resolveFlexibleWindow bugünde start=end=23:59 döndürerek isteği
            // her zaman "şimdiden ileri"de tutar; diğer günlerde 00:00–23:59
            // davranışı korunur.
            val (start, end) = resolveFlexibleWindow(p.selectedDate)
            startDatetime = "${p.selectedDate} $start"
            endDatetime = "${p.selectedDate} $end"
            effectiveDuration = -1.0
        } else {
            startDatetime = "${p.selectedDate} ${p.selectedTimeSlot}"
            endDatetime = calculateEndTime(p.selectedDate, p.selectedTimeSlot, tour.duration)
            effectiveDuration = tour.duration
        }

        // Only emit a coordinate when both axes are present. A half-null pair
        // would have been silently rewritten to (0.0, 0.0) before — that's
        // the Gulf of Guinea, not "no location".
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
            description = tour.description
            // iOS spec §1 + §3.2: additionalData carries ISO-8601 datetimes
            // ("yyyy-MM-dd'T'HH:mm:ss") while the segment-level startDate/endDate
            // stay in the space-separated "yyyy-MM-dd HH:mm" shape. The two
            // formats are intentional — don't unify them.
            this.startDatetime = startDatetime.toAdditionalDataIso()
            this.endDatetime = endDatetime.toAdditionalDataIso()
            this.coordinate = coordinate
            this.isNoLocation = noLocation
            duration = effectiveDuration
            price = p.slotPrice ?: tour.price
            currency = tour.currency ?: "EUR"
            // iOS spec section 2.2: rating is copied straight from tour metadata,
            // independent of the slot/booking. Without these the timeline's
            // ReservedActivityVH hides the rating row entirely.
            rating = tour.rating
            reviewCount = tour.ratingCount
        }

        // If we couldn't resolve a city from the user-selected one, fall back
        // to whatever the lookup returned — better than the placeholder 0.
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
            // Default to 2 hours if no duration
            return calculateEndTimeWithMinutes(date, startTime, 120)
        }

        // Duration is already in minutes from API
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

            // Format: "yyyy-MM-dd HH:mm" (no seconds)
            return "$date ${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            // Fallback: add 2 hours
            return "$date 00:00"
        }
    }
}
