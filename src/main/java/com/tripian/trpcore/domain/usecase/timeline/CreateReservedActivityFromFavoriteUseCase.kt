package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.domain.model.timeline.toApiDateString
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import java.util.Date
import javax.inject.Inject

/**
 * CreateReservedActivityFromFavoriteUseCase
 * Creates a reserved_activity segment from a SegmentFavoriteItem (saved plan)
 */
class CreateReservedActivityFromFavoriteUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<ResponseModelBase, CreateReservedActivityFromFavoriteUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val favorite: SegmentFavoriteItem,
        val selectedDate: Date,
        val startTime: String?,  // Format: "HH:mm" (null = use default 10:00)
        val endTime: String?,    // Format: "HH:mm" (null = calculate from duration)
        val adults: Int = 1
    )

    companion object {
        const val SEGMENT_TYPE_RESERVED_ACTIVITY = "reserved_activity"
        const val DEFAULT_START_TIME = "10:00"
        const val DEFAULT_DURATION_MINUTES = 120 // 2 hours
    }

    override fun on(params: Params?) {
        params?.let { p ->
            val dateStr = p.selectedDate.toApiDateString()
            val startTimeStr = p.startTime ?: DEFAULT_START_TIME

            // Build start datetime
            val startDatetime = "$dateStr $startTimeStr"

            // Calculate end time from duration or use provided
            val endDatetime = if (!p.endTime.isNullOrEmpty()) {
                "$dateStr ${p.endTime}"
            } else {
                calculateEndTime(dateStr, startTimeStr, p.favorite.duration)
            }

            // Build coordinate from favorite
            val coordinate = Coordinate().apply {
                lat = p.favorite.coordinate.lat
                lng = p.favorite.coordinate.lng
            }

            // Build additional data
            val additionalData = TimelineSegmentAdditionalData().apply {
                activityId = p.favorite.activityId
                title = p.favorite.title
                imageUrl = p.favorite.photoUrl
                description = p.favorite.description
                this.startDatetime = startDatetime
                this.endDatetime = endDatetime
                this.coordinate = coordinate
                duration = p.favorite.duration
                p.favorite.price?.let { price ->
                    this.price = price.value
                    this.currency = price.currency ?: "EUR"
                }
                cancellation = p.favorite.cancellation
            }

            // Build segment settings
            val segment = TimelineSegmentSettings().apply {
                title = p.favorite.title
                cityId = p.favorite.cityId
                startDate = startDatetime
                endDate = endDatetime
                adults = p.adults
                segmentType = SEGMENT_TYPE_RESERVED_ACTIVITY
                this.additionalData = additionalData
                this.coordinate = coordinate
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
     * @param duration Duration in MINUTES (Double)
     * @return End datetime string (yyyy-MM-dd HH:mm)
     */
    private fun calculateEndTime(date: String, startTime: String, duration: Double?): String {
        val durationMinutes = duration?.toInt() ?: DEFAULT_DURATION_MINUTES

        try {
            val timeParts = startTime.split(":")
            val startHour = timeParts[0].toInt()
            val startMinute = timeParts[1].toInt()

            val totalMinutes = startHour * 60 + startMinute + durationMinutes
            val endHour = (totalMinutes / 60) % 24
            val endMinute = totalMinutes % 60

            return "$date ${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            // Fallback: add default duration
            return "$date 12:00"
        }
    }
}
