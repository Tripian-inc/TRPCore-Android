package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import com.tripian.trpcore.domain.model.timeline.TransitionInfo
import javax.inject.Inject

/**
 * DetectReservedToBookedTransitionUseCase
 *
 * Reserved olan ama artık tripItems'ta booked olarak gelen activity'leri bulur
 * iOS Guide Operation 3: Reserved → Booked Transition
 *
 * Pure logic, API call yok - sadece detection
 */
class DetectReservedToBookedTransitionUseCase @Inject constructor() :
    BaseUseCase<List<TransitionInfo>, DetectReservedToBookedTransitionUseCase.Params>() {

    data class Params(
        val timeline: Timeline,
        val tripItems: List<SegmentActivityItem>
    )

    override fun on(params: Params?) {
        params?.let {
            addObservable {
                io.reactivex.Observable.just(
                    detectTransitions(it.timeline, it.tripItems)
                )
            }
        }
    }

    /**
     * Timeline'daki reserved_activity segmentlerini tripItems ile karşılaştır
     * Aynı activityId'ye sahip olanları transition olarak işaretle
     */
    private fun detectTransitions(
        timeline: Timeline,
        tripItems: List<SegmentActivityItem>
    ): List<TransitionInfo> {
        val segments = timeline.tripProfile?.segments ?: return emptyList()

        // tripItems activityId'lerini Set'e al (O(1) lookup için)
        val bookedActivityIds = tripItems
            .mapNotNull { it.activityId }
            .toSet()

        // Reserved olan ama artık booked'da olan segmentleri bul
        val transitions = mutableListOf<TransitionInfo>()

        segments.forEachIndexed { index, segment ->
            if (segment.segmentType == SegmentType.RESERVED_ACTIVITY) {
                val activityId = segment.additionalData?.activityId

                if (activityId != null && activityId in bookedActivityIds) {
                    // Bu reserved activity artık booked olmuş
                    val tripItem = tripItems.find { it.activityId == activityId }

                    tripItem?.let { item ->
                        transitions.add(
                            TransitionInfo(
                                segmentIndex = index,
                                activityId = activityId,
                                tripItem = item
                            )
                        )
                    }
                }
            }
        }

        return transitions
    }
}
