package com.tripian.trpcore.domain.model.timeline

import com.tripian.one.api.timeline.model.TimelineSegment
import java.util.Calendar

/** The activity's start datetime: `additionalData.startDatetime`, else the segment start. */
fun TimelineSegment.effectiveStartDatetime(): String? =
    additionalData?.startDatetime?.takeIf { it.isNotBlank() } ?: startDate

/**
 * The activity's end datetime: `additionalData.endDatetime`, else start plus
 * `additionalData.duration`, else the segment `endDate` unless it merely repeats
 * the start (the filler written when the host supplied no end). Null means the
 * end is unknown: the cell shows the start alone and the segment stays out of
 * overlap checks.
 */
fun TimelineSegment.effectiveEndDatetime(): String? =
    additionalData?.endDatetime?.takeIf { it.isNotBlank() }
        ?: endDatetimeFromDuration()
        ?: endDate?.takeIf { it.isNotBlank() && it != startDate }

private fun TimelineSegment.endDatetimeFromDuration(): String? {
    val minutes = additionalData?.duration?.takeIf { it > 0 } ?: return null
    val start = effectiveStartDatetime().toDate() ?: return null
    val end = Calendar.getInstance().apply {
        time = start
        add(Calendar.MINUTE, minutes.toInt())
    }.time
    return end.toApiDateTimeString()
}
