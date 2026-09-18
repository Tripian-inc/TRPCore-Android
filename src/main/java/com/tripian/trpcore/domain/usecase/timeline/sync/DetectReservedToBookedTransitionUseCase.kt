package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import com.tripian.trpcore.domain.model.timeline.TransitionInfo
import com.tripian.trpcore.util.ActivityIdFormat
import javax.inject.Inject

/**
 * DetectReservedToBookedTransitionUseCase
 *
 * Finds reserved_activity segments whose activityId now arrives as booked in
 * tripItems. Pure detection — no API calls.
 * iOS Reference: Guide Operation 3 (Reserved → Booked Transition)
 */
class DetectReservedToBookedTransitionUseCase @Inject constructor() :
    SuspendUseCase<List<TransitionInfo>, DetectReservedToBookedTransitionUseCase.Params>() {

    data class Params(
        val timeline: Timeline,
        val tripItems: List<SegmentActivityItem>
    )

    override suspend fun execute(params: Params): List<TransitionInfo> =
        detectTransitions(params.timeline, params.tripItems)

    private fun detectTransitions(
        timeline: Timeline,
        tripItems: List<SegmentActivityItem>
    ): List<TransitionInfo> {
        val segments = timeline.tripProfile?.segments ?: return emptyList()

        val bookedBaseIds = tripItems
            .mapNotNull { ActivityIdFormat.base(it.activityId) }
            .toSet()

        val transitions = mutableListOf<TransitionInfo>()

        segments.forEachIndexed { index, segment ->
            if (segment.segmentType == SegmentType.RESERVED_ACTIVITY) {
                val activityId = segment.additionalData?.activityId
                val baseId = ActivityIdFormat.base(activityId)

                if (activityId != null && baseId in bookedBaseIds) {
                    val tripItem = tripItems.find { ActivityIdFormat.base(it.activityId) == baseId }

                    tripItem?.let { item ->
                        transitions.add(
                            TransitionInfo(
                                segmentIndex = index,
                                activityId = activityId,
                                tripItem = item,
                                reservedCityId = segment.cityId?.takeIf { it > 0 }
                            )
                        )
                    }
                }
            }
        }

        return transitions
    }
}
