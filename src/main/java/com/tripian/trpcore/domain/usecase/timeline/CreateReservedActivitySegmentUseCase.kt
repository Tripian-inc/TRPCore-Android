package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import javax.inject.Inject

/**
 * CreateReservedActivitySegmentUseCase
 * Creates a reserved_activity segment for a tour/activity
 */
class CreateReservedActivitySegmentUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<ResponseModelBase, CreateReservedActivitySegmentUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val tour: TourProduct,
        val selectedDate: String,      // Format: "yyyy-MM-dd"
        val selectedTimeSlot: String,  // Format: "HH:mm"
        val adults: Int = 1,
        val cityId: Int
    )

    companion object {
        const val SEGMENT_TYPE_RESERVED_ACTIVITY = "reserved_activity"
    }

    override fun on(params: Params?) {
        params?.let { p ->
            // Build start and end datetime (format: "yyyy-MM-dd HH:mm")
            val startDatetime = "${p.selectedDate} ${p.selectedTimeSlot}"
            val endDatetime = calculateEndTime(p.selectedDate, p.selectedTimeSlot, p.tour.duration)

            // Build coordinate from tour location
            val coordinate = p.tour.locations?.firstOrNull()?.let { loc ->
                Coordinate().apply {
                    lat = loc.lat ?: 0.0
                    lng = loc.lon ?: 0.0  // Note: API uses "lon" but Coordinate uses "lng"
                }
            }

            // Build additional data
            val additionalData = TimelineSegmentAdditionalData().apply {
                activityId = p.tour.productId
                title = p.tour.title
                imageUrl = p.tour.images?.firstOrNull()?.url
                description = p.tour.description
                this.startDatetime = startDatetime
                this.endDatetime = endDatetime
                this.coordinate = coordinate
                duration = p.tour.duration
                price = p.tour.price
                currency = p.tour.currency ?: "EUR"
            }

            // Build segment settings
            val segment = TimelineSegmentSettings().apply {
                title = p.tour.title
                cityId = p.cityId
                startDate = startDatetime
                endDate = endDatetime
                adults = p.adults
                segmentType = SEGMENT_TYPE_RESERVED_ACTIVITY
                this.additionalData = additionalData
                available = false
                distinctPlan = true
            }

            addObservable {
                repository.editSegment(p.tripHash, segment)
                    .toSingleDefault(ResponseModelBase().apply { status = 200 })
                    .toObservable()
            }
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
