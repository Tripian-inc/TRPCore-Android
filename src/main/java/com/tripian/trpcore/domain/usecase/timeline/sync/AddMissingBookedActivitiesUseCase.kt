package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.ApiErrorMapper
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import com.tripian.trpcore.sdk.TRPCoreErrorCode
import javax.inject.Inject

/**
 * Adds booked_activity segments for the tripItems [BookedActivityPairing] leaves
 * unpaired: no stored booked segment and no reserved segment awaiting conversion took them.
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
        val missingItems = BookedActivityPairing
            .plan(params.itinerary.tripItems, params.timeline.tripProfile?.segments)
            .missingItems

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
