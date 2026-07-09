package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.domain.model.timeline.toApiDateString
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import com.tripian.trpcore.util.extensions.resolveFlexibleWindow
import com.tripian.trpcore.util.extensions.toAdditionalDataIso
import java.util.Date
import javax.inject.Inject

/**
 * Creates a reserved_activity segment from a SegmentFavoriteItem (saved plan).
 *
 * additionalData datetimes are ISO-8601 while segment startDate/endDate stay
 * "yyyy-MM-dd HH:mm" — the two formats are intentional. For flexible activities
 * [resolveFlexibleWindow] keeps the window ahead of "now" (backend rejects a
 * 00:00 start) and duration -1.0 marks the segment flexible. `cancellation` is
 * left null so the cell renders the localized free-cancellation text instead of
 * the favorite's untranslated label.
 */
class CreateReservedActivityFromFavoriteUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<ResponseModelBase, CreateReservedActivityFromFavoriteUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val favorite: SegmentFavoriteItem,
        val selectedDate: Date,
        val startTime: String?,
        val endTime: String?,
        val adults: Int = 1,
        val resolvedCityId: Int? = null,
        val isFlexible: Boolean = false,
        val slotPrice: Double? = null
    )

    companion object {
        const val SEGMENT_TYPE_RESERVED_ACTIVITY = "reserved_activity"
        const val DEFAULT_START_TIME = "10:00"
        const val DEFAULT_DURATION_MINUTES = 120
    }

    override suspend fun execute(params: Params): ResponseModelBase {
        val p = params
        val dateStr = p.selectedDate.toApiDateString()

        val startTimeStr: String
        val endDatetime: String
        val effectiveDuration: Double?
        if (p.isFlexible) {
            val (start, end) = resolveFlexibleWindow(dateStr)
            startTimeStr = start
            endDatetime = "$dateStr $end"
            effectiveDuration = -1.0
        } else {
            startTimeStr = p.startTime ?: DEFAULT_START_TIME
            endDatetime = if (!p.endTime.isNullOrEmpty()) {
                "$dateStr ${p.endTime}"
            } else {
                calculateEndTime(dateStr, startTimeStr, p.favorite.duration)
            }
            effectiveDuration = p.favorite.duration
        }

        val startDatetime = "$dateStr $startTimeStr"

        val coordinate = Coordinate().apply {
            lat = p.favorite.coordinate.lat
            lng = p.favorite.coordinate.lng
        }

        val additionalData = TimelineSegmentAdditionalData().apply {
            activityId = p.favorite.activityId
            title = p.favorite.title
            imageUrl = p.favorite.photoUrl
            this.startDatetime = startDatetime.toAdditionalDataIso()
            this.endDatetime = endDatetime.toAdditionalDataIso()
            this.coordinate = coordinate
            duration = effectiveDuration
            val slot = p.slotPrice
            if (slot != null) {
                this.price = slot
                this.currency = TRPCore.core.getCurrentCurrency()
            } else {
                p.favorite.price?.let { price ->
                    this.price = price.value
                    this.currency = price.currency ?: "EUR"
                }
            }
            cancellation = null
            rating = p.favorite.rating
            reviewCount = p.favorite.ratingCount
        }

        val segment = TimelineSegmentSettings().apply {
            title = p.favorite.title
            cityId = p.resolvedCityId ?: p.favorite.cityId
            startDate = startDatetime
            endDate = endDatetime
            adults = p.adults
            segmentType = SEGMENT_TYPE_RESERVED_ACTIVITY
            this.additionalData = additionalData
            this.coordinate = coordinate
            available = false
            distinctPlan = true
            currency = TRPCore.core.getCurrentCurrency()
        }

        repository.editSegmentAsync(p.tripHash, segment)
        return ResponseModelBase().apply { status = 200 }
    }

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
            return "$date 12:00"
        }
    }
}
