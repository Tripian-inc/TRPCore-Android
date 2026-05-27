package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import javax.inject.Inject

/**
 * UpdateSegmentTimeUseCase
 *
 * Updates the start/end time of an existing top-level segment (reserved_activity
 * or flexible activity) in-place. The backend uses [TimelineSegmentSettings.segmentIndex]
 * as the identity for editSegment, so we re-send the whole segment payload with
 * the index pinned and only the time fields changed.
 */
class UpdateSegmentTimeUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<ResponseModelBase, UpdateSegmentTimeUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val segmentIndex: Int,
        val original: TimelineSegment,
        val newStartTime: String,  // "HH:mm"
        val newEndTime: String     // "HH:mm"
    )

    override fun on(params: Params?) {
        params?.let { p ->
            val baseDate = datePart(p.original.startDate)
                ?: datePart(p.original.additionalData?.startDatetime)
                ?: return  // Can't rebuild times without an anchor date

            val newStartDate = "$baseDate ${p.newStartTime}"
            val newEndDate = "$baseDate ${p.newEndTime}"

            val updatedAdditionalData = p.original.additionalData?.let { src ->
                TimelineSegmentAdditionalData().apply {
                    activityId = src.activityId
                    bookingId = src.bookingId
                    title = src.title
                    imageUrl = src.imageUrl
                    description = src.description
                    startDatetime = newStartDate
                    endDatetime = newEndDate
                    coordinate = src.coordinate
                    cancellation = src.cancellation
                    price = src.price
                    currency = src.currency
                    duration = src.duration
                    rating = src.rating
                    reviewCount = src.reviewCount
                    isNoLocation = src.isNoLocation
                }
            }

            val segment = TimelineSegmentSettings().apply {
                segmentIndex = p.segmentIndex
                cityId = p.original.cityId
                title = p.original.title
                description = p.original.description
                startDate = newStartDate
                endDate = newEndDate
                adults = p.original.adults
                children = p.original.children
                pets = p.original.pets
                coordinate = p.original.coordinate
                destinationCoordinate = p.original.destinationCoordinate
                answerIds = p.original.answerIds ?: emptyList()
                doNotRecommend = p.original.doNotRecommend
                excludePoiIds = p.original.excludePoiIds
                includePoiIds = p.original.includePoiIds
                considerWeather = p.original.considerWeather
                distinctPlan = p.original.distinctPlan
                available = p.original.available
                accommodation = p.original.accommodation
                destinationAccommodation = p.original.destinationAccommodation
                smartRecommendation = p.original.smartRecommendation
                activityFreeText = p.original.activityFreeText
                activityIds = p.original.activityIds
                excludedActivityIds = p.original.excludedActivityIds
                segmentType = p.original.segmentType
                additionalData = updatedAdditionalData
                currency = TRPCore.core.getCurrentCurrency()
            }

            addObservable {
                repository.editSegment(p.tripHash, segment)
                    .toSingleDefault(ResponseModelBase().apply { status = 200 })
                    .toObservable()
            }
        }
    }

    /** "yyyy-MM-dd HH:mm" or "yyyy-MM-dd" → "yyyy-MM-dd"; null on parse miss. */
    private fun datePart(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val spaceIdx = raw.indexOf(' ')
        val datePiece = if (spaceIdx > 0) raw.substring(0, spaceIdx) else raw
        // Expect "yyyy-MM-dd"
        return if (datePiece.length >= 10 && datePiece[4] == '-' && datePiece[7] == '-')
            datePiece.substring(0, 10) else null
    }
}
