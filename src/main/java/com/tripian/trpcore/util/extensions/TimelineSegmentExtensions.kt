package com.tripian.trpcore.util.extensions

import com.tripian.one.api.pois.model.AdditionalData
import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.TRPCore

/**
 * U+2212 MINUS SIGN (not an ASCII hyphen — keep the unicode codepoint) used for
 * "no order / flexible" order chips so the character is vertically centered.
 */
const val MINUS_SIGN: String = "−"

/** Text shown in place of a time range while the host has not assigned one yet. */
const val PENDING_TIME_TEXT: String = "--:--"

/** Duration marker of a placeholder slot (flexible reserved activity or booked activity with pending time). */
const val PLACEHOLDER_DURATION: Double = -1.0

const val PLACEHOLDER_START_TIME: String = "00:00"
const val PLACEHOLDER_END_TIME: String = "23:59"

/** Time strings that mark a segment whose start/end were placeholdered (all-day). */
private val FLEXIBLE_TIME_VALUES = setOf(PLACEHOLDER_START_TIME, PLACEHOLDER_END_TIME)

/**
 * True when [TimelineSegment] is a flexible-time reserved activity: segmentType
 * "reserved_activity", additionalData.duration == -1, and start/end times both in
 * {00:00, 23:59}. Flexible activities are pinned to the top of their city group and
 * excluded from time-conflict detection since the envelope is a placeholder interval.
 */
val TimelineSegment.isFlexibleActivity: Boolean
    get() = segmentType == SegmentType.RESERVED_ACTIVITY && hasPlaceholderTimeSlot()

/**
 * True when [TimelineSegment] is a booked activity whose host has not assigned a
 * time yet: segmentType "booked_activity" carrying the same placeholder slot as a
 * flexible activity. Rendered as a pending time and excluded from conflict detection.
 */
val TimelineSegment.isBookedTimePending: Boolean
    get() = segmentType == SegmentType.BOOKED_ACTIVITY && hasPlaceholderTimeSlot()

private fun TimelineSegment.hasPlaceholderTimeSlot(): Boolean {
    if (additionalData?.duration != PLACEHOLDER_DURATION) return false
    val start = startDate?.substringAfter(' ', "")?.takeIf { it.isNotEmpty() } ?: return false
    val end = endDate?.substringAfter(' ', "")?.takeIf { it.isNotEmpty() } ?: return false
    return start in FLEXIBLE_TIME_VALUES && end in FLEXIBLE_TIME_VALUES
}

/**
 * Edit payload for a stored segment, pinned to [segmentIndex] so the backend edits
 * it in place. Every field is copied; [additionalData] is cloned so the caller can
 * retime it without touching the stored segment.
 */
fun TimelineSegment.toEditSettings(segmentIndex: Int): TimelineSegmentSettings {
    val source = this
    return TimelineSegmentSettings().apply {
        this.segmentIndex = segmentIndex
        cityId = source.cityId
        title = source.title
        startDate = source.startDate
        endDate = source.endDate
        adults = source.adults
        children = source.children
        pets = source.pets
        coordinate = source.coordinate
        destinationCoordinate = source.destinationCoordinate
        answerIds = source.answerIds ?: emptyList()
        doNotRecommend = source.doNotRecommend
        excludePoiIds = source.excludePoiIds
        includePoiIds = source.includePoiIds
        considerWeather = source.considerWeather
        distinctPlan = source.distinctPlan
        available = source.available
        accommodation = source.accommodation
        destinationAccommodation = source.destinationAccommodation
        smartRecommendation = source.smartRecommendation
        activityFreeText = source.activityFreeText
        activityIds = source.activityIds
        excludedActivityIds = source.excludedActivityIds
        segmentType = source.segmentType
        additionalData = source.additionalData?.copyOf()
        currency = TRPCore.core.getCurrentCurrency()
    }
}

fun TimelineSegmentAdditionalData.copyOf(): TimelineSegmentAdditionalData {
    val source = this
    return TimelineSegmentAdditionalData().apply {
        activityId = source.activityId
        bookingId = source.bookingId
        title = source.title
        imageUrl = source.imageUrl
        description = source.description
        startDatetime = source.startDatetime
        endDatetime = source.endDatetime
        coordinate = source.coordinate
        cancellation = source.cancellation
        price = source.price
        currency = source.currency
        duration = source.duration
        rating = source.rating
        reviewCount = source.reviewCount
        isNoLocation = source.isNoLocation
    }
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
