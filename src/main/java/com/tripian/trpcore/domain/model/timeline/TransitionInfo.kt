package com.tripian.trpcore.domain.model.timeline

import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem

/**
 * Carries one reserved_activity → booked_activity transition detected on the timeline.
 */
data class TransitionInfo(
    val segmentIndex: Int,            // Index of the reserved segment on the timeline
    val activityId: String,           // Matching key
    val tripItem: SegmentActivityItem // Booked activity data
)
