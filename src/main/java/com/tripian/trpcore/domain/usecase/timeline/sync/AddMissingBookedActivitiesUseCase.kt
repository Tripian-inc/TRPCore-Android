package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.ApiErrorMapper
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import com.tripian.trpcore.sdk.TRPCoreErrorCode
import com.tripian.trpcore.util.ActivityIdFormat
import javax.inject.Inject

/**
 * Adds booked_activity segments for tripItems not yet present on the timeline.
 * Delegates payload construction to ItineraryWithActivities.createBookedActivitySegment
 * so initial-create and sync paths emit identical segments.
 */
class AddMissingBookedActivitiesUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<ResponseModelBase, AddMissingBookedActivitiesUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val itinerary: ItineraryWithActivities,
        val timeline: Timeline
    )

    override suspend fun execute(params: Params): ResponseModelBase {
        val existingActivityKeys = params.timeline.tripProfile?.segments
            ?.mapNotNull { segment -> ActivityIdFormat.matchKey(segment.additionalData?.activityId) }
            ?.toSet()
            ?: emptySet()

        val missingItems = params.itinerary.tripItems
            ?.filter { item ->
                val key = ActivityIdFormat.matchKey(item.activityId)
                key != null && key !in existingActivityKeys
            }
            ?: emptyList()

        if (missingItems.isEmpty()) {
            return ResponseModelBase()
        }

        for (tripItem in missingItems) {
            val segment = params.itinerary.createBookedActivitySegment(tripItem)
            try {
                repository.editSegmentAsync(params.tripHash, segment)
            } catch (error: Throwable) {
                val reason = ApiErrorMapper.serverMessage(error) ?: error.message.orEmpty()
                TRPCore.notifyError(
                    "${tripItem.title.orEmpty()} ${reason}".trim(),
                    TRPCoreErrorCode.BOOKED_ACTIVITY_SYNC_FAILED
                )
            }
        }
        return ResponseModelBase()
    }
}
