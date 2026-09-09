package com.tripian.trpcore.domain.model.timeline

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelinePlan
import com.tripian.one.api.timeline.model.TimelineSegment

/**
 * The plan generated for [segment]: plan ids are the segment's dayIds joined by "-".
 * Segments without dayIds (e.g. host-inserted booked activities) have no plan.
 */
fun Timeline.planFor(segment: TimelineSegment): TimelinePlan? {
    val key = segment.dayIds?.takeIf { it.isNotEmpty() }?.joinToString("-") ?: return null
    return plans?.firstOrNull { it.id == key }
}

/** True while generation is still running for this plan. */
val TimelinePlan.isGenerating: Boolean
    get() = generatedStatus == 0

/** True when generation finished without finding any place for this plan. */
val TimelinePlan.generatedWithoutPois: Boolean
    get() = generatedStatus == -1 && steps.isNullOrEmpty()
