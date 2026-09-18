package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.SegmentType
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
 * Adds booked_activity segments for tripItems the timeline holds under no segment
 * of its own. A product whose reserved segment is still on the timeline is left
 * alone: the reserved-to-booked transition replaces that segment itself, and
 * adding it here too would leave the trip with the booking twice. Ids are compared
 * bare, since the SDK's own segments may carry the prefixed form.
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
        val ownedBaseIds = params.timeline.tripProfile?.segments
            ?.filter { segment ->
                segment.segmentType == SegmentType.BOOKED_ACTIVITY ||
                    segment.segmentType == SegmentType.RESERVED_ACTIVITY
            }
            ?.mapNotNull { segment -> ActivityIdFormat.base(segment.additionalData?.activityId) }
            ?.toSet()
            .orEmpty()

        val missingItems = params.itinerary.tripItems
            ?.filter { item ->
                val baseId = ActivityIdFormat.base(item.activityId)
                baseId != null && baseId !in ownedBaseIds
            }
            ?: emptyList()

        if (missingItems.isEmpty()) {
            return ResponseModelBase()
        }

        for (tripItem in missingItems) {
            val segment = params.itinerary.createBookedActivitySegment(
                tripItem,
                fallbackCityId = params.timeline.tripProfile?.cityId
            )
            try {
                repository.editSegmentAsync(params.tripHash, segment)
            } catch (error: Throwable) {
                val reason = ApiErrorMapper.serverMessage(error) ?: error.message.orEmpty()
                android.util.Log.e("SYNC", "Booked activity add failed: ${tripItem.activityId} $reason")
                TRPCore.notifyError(
                    "${tripItem.title.orEmpty()} ${reason}".trim(),
                    TRPCoreErrorCode.BOOKED_ACTIVITY_SYNC_FAILED
                )
            }
        }
        return ResponseModelBase()
    }
}
