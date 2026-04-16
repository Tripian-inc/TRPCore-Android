package com.tripian.trpcore.domain.model.timeline

import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem

/**
 * Reserved → Booked transition için bilgi taşıyan model
 * iOS guide'a göre: reserved_activity segment'lerinin booked_activity'ye dönüşmesi
 */
data class TransitionInfo(
    val segmentIndex: Int,           // Timeline'daki reserved segment index'i
    val activityId: String,           // Activity ID (matching key)
    val tripItem: SegmentActivityItem // Booked activity verisi
)
