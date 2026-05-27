package com.tripian.trpcore.util.extensions

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.TimelineSegment

/**
 * U+2212 MINUS SIGN — used in place of a regular hyphen for "no order / flexible"
 * order chips so the character is vertically centered in the 20dp circle.
 *
 * NOTE: This is **not** an ASCII hyphen `-`. Keep the unicode codepoint.
 */
const val MINUS_SIGN: String = "−"

/** Time strings that mark a segment whose start/end were placeholdered (all-day). */
private val FLEXIBLE_TIME_VALUES = setOf("00:00", "23:59")

/**
 * Returns true when [TimelineSegment] represents a flexible-time reserved activity:
 *
 *  - segmentType == "reserved_activity"
 *  - additionalData.duration == -1
 *  - start and end times both fall in {00:00, 23:59}
 *
 * Flexible activities are pinned to the top of their city group, render via the
 * dedicated `FlexibleActivityVH`, and are excluded from time-conflict detection
 * (Theme 8) since the 00:00–23:59 envelope is a placeholder, not a real interval.
 */
val TimelineSegment.isFlexibleActivity: Boolean
    get() {
        if (segmentType != SegmentType.RESERVED_ACTIVITY) return false
        if (additionalData?.duration != -1.0) return false
        val start = startDate?.substringAfter(' ', "")?.takeIf { it.isNotEmpty() } ?: return false
        val end = endDate?.substringAfter(' ', "")?.takeIf { it.isNotEmpty() } ?: return false
        return start in FLEXIBLE_TIME_VALUES && end in FLEXIBLE_TIME_VALUES
    }
