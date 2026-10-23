package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import com.tripian.trpcore.domain.model.timeline.TransitionInfo
import javax.inject.Inject

/**
 * DetectReservedToBookedTransitionUseCase
 *
 * Finds reserved_activity segments whose product now arrives as booked in
 * tripItems. Pairing comes from [BookedActivityPairing]: a reserved segment only
 * takes an item no booked segment already holds, one item per segment, same day
 * first. Pure detection — no API calls.
 * iOS Reference: Guide Operation 3 (Reserved → Booked Transition)
 */
class DetectReservedToBookedTransitionUseCase @Inject constructor() :
    SuspendUseCase<List<TransitionInfo>, DetectReservedToBookedTransitionUseCase.Params>() {

    data class Params(
        val timeline: Timeline,
        val tripItems: List<SegmentActivityItem>
    )

    override suspend fun execute(params: Params): List<TransitionInfo> {
        val segments = params.timeline.tripProfile?.segments ?: return emptyList()
        return BookedActivityPairing.plan(params.tripItems, segments).reserved.pairs
            .mapNotNull { match ->
                val activityId = segments[match.segmentIndex].additionalData?.activityId
                    ?: return@mapNotNull null
                TransitionInfo(
                    segmentIndex = match.segmentIndex,
                    activityId = activityId,
                    tripItem = match.item
                )
            }
    }
}
