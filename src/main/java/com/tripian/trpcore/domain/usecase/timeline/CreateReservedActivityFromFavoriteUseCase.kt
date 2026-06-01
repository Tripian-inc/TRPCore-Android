package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.BaseUseCase
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
        val startTime: String?,  // Format: "HH:mm" (null = use default 10:00). isFlexible=true iken yok sayılır.
        val endTime: String?,    // Format: "HH:mm" (null = calculate from duration). isFlexible=true iken yok sayılır.
        val adults: Int = 1,
        val resolvedCityId: Int? = null,  // Our system's cityId (resolved from cityName mapping)
        // Flexible (any-time) favorite. true ise window resolveFlexibleWindow ile
        // hesaplanır ve duration = -1.0 yazılır (tour flexible path ile parite).
        val isFlexible: Boolean = false
    )

    companion object {
        const val SEGMENT_TYPE_RESERVED_ACTIVITY = "reserved_activity"
        const val DEFAULT_START_TIME = "10:00"
        const val DEFAULT_DURATION_MINUTES = 120 // 2 hours
    }

    override fun on(params: Params?) {
        params?.let { p ->
            val dateStr = p.selectedDate.toApiDateString()

            // Flexible favorite: bottom-sheet placeholder'ı yerine helper'a
            // güveniyoruz; bugün için window 23:59–23:59'a çekilir (backend
            // 00:00 startı reddediyor). Duration -1.0 markerı tour flexible
            // path ile parite sağlar — renderer aynı FlexibleActivity cell'i
            // gösterir.
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
                // iOS spec §1 + §3.2: additionalData carries ISO-8601 datetimes
                // ("yyyy-MM-dd'T'HH:mm:ss") while the segment-level startDate/endDate
                // stay in the space-separated "yyyy-MM-dd HH:mm" shape. The two
                // formats are intentional — don't unify them.
                this.startDatetime = startDatetime.toAdditionalDataIso()
                this.endDatetime = endDatetime.toAdditionalDataIso()
                this.coordinate = coordinate
                duration = effectiveDuration
                p.favorite.price?.let { price ->
                    this.price = price.value
                    this.currency = price.currency ?: "EUR"
                }
                cancellation = p.favorite.cancellation
                // iOS spec section 2.2: carry rating/ratingCount on every reserved
                // activity write — Saved Plans favorites already store these, so
                // we propagate them straight through to keep the cell parity
                // with tour-listing additions.
                rating = p.favorite.rating
                reviewCount = p.favorite.ratingCount
            }

            // Build segment settings
            val segment = TimelineSegmentSettings().apply {
                title = p.favorite.title
                // Use resolved cityId (our system's ID) if available, fallback to favorite.cityId
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
