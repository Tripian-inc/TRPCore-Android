package com.tripian.trpcore.domain.model.timeline

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import com.tripian.trpcore.util.ActivityIdFormat

/**
 * What a booking is recognised by when host items and timeline segments are
 * compared: its `bookingId` first, its bare activityId as the fallback. A
 * `C_`-prefixed bookingId is reduced to its bare form like an activityId; any
 * other bookingId is compared verbatim.
 */
data class BookingKey(val bookingId: String?, val activityId: String?) {

    val isEmpty: Boolean get() = bookingId == null && activityId == null

    companion object {
        fun of(bookingId: String?, activityId: String?): BookingKey = BookingKey(
            bookingId?.takeIf { it.isNotBlank() }?.let { id ->
                if (id.startsWith("C_")) ActivityIdFormat.base(id) else id
            },
            ActivityIdFormat.base(activityId)
        )
    }
}

fun SegmentActivityItem.bookingKey(): BookingKey = BookingKey.of(bookingId, activityId)

fun TimelineSegmentAdditionalData?.bookingKey(): BookingKey =
    BookingKey.of(this?.bookingId, this?.activityId)

/**
 * A set of bookings queried with [contains]. A key with a bookingId must find that
 * bookingId, unless the product is listed without one; a key without a bookingId
 * falls back to its activityId. An empty key is never contained.
 */
class BookingKeySet(keys: Collection<BookingKey>) {

    private val bookingIds = keys.mapNotNull { it.bookingId }.toSet()
    private val activityIds = keys.mapNotNull { it.activityId }.toSet()
    private val activityIdsWithoutBooking = keys
        .filter { it.bookingId == null }
        .mapNotNull { it.activityId }
        .toSet()

    operator fun contains(key: BookingKey): Boolean = when {
        key.bookingId != null -> key.bookingId in bookingIds ||
            key.activityId in activityIdsWithoutBooking
        key.activityId != null -> key.activityId in activityIds
        else -> false
    }
}

/**
 * The host's booked items the timeline holds under no booked or reserved segment,
 * matched by [BookingKeySet]. Items without an activityId are never reported.
 */
fun Timeline.missingBookedItems(tripItems: List<SegmentActivityItem>): List<SegmentActivityItem> {
    val owned = BookingKeySet(
        tripProfile?.segments.orEmpty()
            .filter { segment ->
                segment.segmentType == SegmentType.BOOKED_ACTIVITY ||
                    segment.segmentType == SegmentType.RESERVED_ACTIVITY
            }
            .map { segment -> segment.additionalData.bookingKey() }
    )
    return tripItems.filter { item ->
        val key = item.bookingKey()
        key.activityId != null && key !in owned
    }
}
