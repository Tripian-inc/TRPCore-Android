package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.domain.usecase.timeline.SegmentTimeRangeAdjustment.datePartOf
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import com.tripian.trpcore.util.extensions.toEditSettings
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
) : SuspendUseCase<ResponseModelBase, UpdateSegmentTimeUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val segmentIndex: Int,
        val original: TimelineSegment,
        val newStartTime: String,  // "HH:mm"
        val newEndTime: String,    // "HH:mm"
        /** Target day "yyyy-MM-dd"; when null the segment's existing date is kept. */
        val newDate: String? = null,
        /** End day "yyyy-MM-dd"; when null the end shares the start's day. */
        val newEndDate: String? = null,
        /** Price of the newly selected time slot; overrides the segment price when non-null. */
        val newPrice: Double? = null
    )

    override suspend fun execute(params: Params): ResponseModelBase {
        val baseDate = datePartOf(params.newDate)
            ?: datePartOf(params.original.startDate)
            ?: datePartOf(params.original.additionalData?.startDatetime)
            ?: return ResponseModelBase().apply { status = 200 }
        val endBaseDate = datePartOf(params.newEndDate) ?: baseDate

        val newStartDate = "$baseDate ${params.newStartTime}"
        val newEndDate = "$endBaseDate ${params.newEndTime}"

        val segment = params.original.toEditSettings(params.segmentIndex).apply {
            startDate = newStartDate
            endDate = newEndDate
            additionalData?.apply {
                startDatetime = newStartDate
                endDatetime = newEndDate
                params.newPrice?.let { price = it }
            }
        }

        repository.editSegmentAsync(params.tripHash, segment)
        return ResponseModelBase().apply { status = 200 }
    }
}
