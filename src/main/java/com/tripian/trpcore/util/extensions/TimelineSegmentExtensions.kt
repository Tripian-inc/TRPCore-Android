package com.tripian.trpcore.util.extensions

import com.tripian.one.api.pois.model.AdditionalData
import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData

/**
 * U+2212 MINUS SIGN (not an ASCII hyphen — keep the unicode codepoint) used for
 * "no order / flexible" order chips so the character is vertically centered.
 */
const val MINUS_SIGN: String = "−"

/** Time strings that mark a segment whose start/end were placeholdered (all-day). */
private val FLEXIBLE_TIME_VALUES = setOf("00:00", "23:59")

/**
 * True when [TimelineSegment] is a flexible-time reserved activity: segmentType
 * "reserved_activity", additionalData.duration == -1, and start/end times both in
 * {00:00, 23:59}. Flexible activities are pinned to the top of their city group and
 * excluded from time-conflict detection since the envelope is a placeholder interval.
 */
val TimelineSegment.isFlexibleActivity: Boolean
    get() {
        if (segmentType != SegmentType.RESERVED_ACTIVITY) return false
        if (additionalData?.duration != -1.0) return false
        val start = startDate?.substringAfter(' ', "")?.takeIf { it.isNotEmpty() } ?: return false
        val end = endDate?.substringAfter(' ', "")?.takeIf { it.isNotEmpty() } ?: return false
        return start in FLEXIBLE_TIME_VALUES && end in FLEXIBLE_TIME_VALUES
    }

/**
 * Writes the price the schedule lookup resolved for the booked slot, together with
 * the currency it was requested in. A null [price] leaves the stored values alone.
 */
fun TimelineSegmentAdditionalData.applyScheduledPrice(price: Double?, currency: String?) {
    if (price == null) return
    this.price = price
    currency?.takeIf { it.isNotEmpty() }?.let { this.currency = it }
}

/** [applyScheduledPrice] for activity steps, creating the container when the POI lacks one. */
fun Poi.applyScheduledPrice(price: Double?, currency: String?) {
    if (price == null) return
    val data = additionalData ?: AdditionalData().also { additionalData = it }
    data.price = price
    currency?.takeIf { it.isNotEmpty() }?.let { data.currency = it }
}
