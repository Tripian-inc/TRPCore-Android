package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import com.tripian.trpcore.util.ActivityIdFormat

/**
 * Pairs the host's booked items with the activity segments already on the timeline.
 *
 * The product match key alone is not an identity: one product can be booked several
 * times (e.g. both legs of a round-trip transfer). Inside a match-key group an item
 * pairs with a stored segment by line id when both carry one; legacy segments without
 * a line id fall back to city + calendar day, then city, then day, then any remaining
 * item of the same product. Reserved segments pair with the items no booked segment
 * took, preferring the same day.
 *
 * Pure and deterministic: the result does not depend on the order of the inputs.
 * Items without a match key take part in no pairing and are never reported as missing.
 */
object BookedActivityPairing {

    /**
     * A stored activity segment reduced to the fields pairing reads.
     *
     * @param index position of the segment in the timeline's segment list.
     * @param lineId the host line id persisted on the segment; null for legacy segments.
     * @param day calendar day of the segment's start, "yyyy-MM-dd".
     */
    data class StoredSegment(
        val index: Int,
        val matchKey: String,
        val lineId: String?,
        val cityId: Int?,
        val day: String?
    )

    /** One incoming [item] bound to the stored segment at [segmentIndex]. */
    data class Match(val item: SegmentActivityItem, val segmentIndex: Int)

    /**
     * @param pairs matched items, ordered by segment index.
     * @param unmatchedItems keyed items no stored segment took, in input order.
     * @param unmatchedSegmentIndices stored segments no item took, ascending.
     */
    data class Result(
        val pairs: List<Match>,
        val unmatchedItems: List<SegmentActivityItem>,
        val unmatchedSegmentIndices: List<Int>
    )

    /**
     * Booked pairing followed by reserved pairing on the leftover items.
     * [missingItems] is what still needs a new booked segment.
     */
    data class SyncPlan(val booked: Result, val reserved: Result) {
        val missingItems: List<SegmentActivityItem>
            get() = reserved.unmatchedItems
    }

    /** Runs the full booked → reserved pairing for one timeline snapshot. */
    fun plan(items: List<SegmentActivityItem>?, segments: List<TimelineSegment>?): SyncPlan {
        val booked = pairBooked(
            items.orEmpty(),
            storedSegments(segments, SegmentType.BOOKED_ACTIVITY)
        )
        val reserved = pairReserved(
            booked.unmatchedItems,
            storedSegments(segments, SegmentType.RESERVED_ACTIVITY)
        )
        return SyncPlan(booked, reserved)
    }

    /**
     * Segments of [segmentType] that carry a match key. The timeline API's segment
     * payload has no line id field, so every stored segment is treated as legacy.
     */
    fun storedSegments(segments: List<TimelineSegment>?, segmentType: String): List<StoredSegment> =
        segments.orEmpty().mapIndexedNotNull { index, segment ->
            if (segment.segmentType != segmentType) return@mapIndexedNotNull null
            val key = ActivityIdFormat.matchKey(segment.additionalData?.activityId)
                ?: return@mapIndexedNotNull null
            StoredSegment(
                index = index,
                matchKey = key,
                lineId = null,
                cityId = segment.cityId,
                day = calendarDay(segment.additionalData?.startDatetime ?: segment.startDate)
            )
        }

    /** Line id first, then city + day, city, day, any for stored segments without a line id. */
    fun pairBooked(items: List<SegmentActivityItem>, stored: List<StoredSegment>): Result =
        pairByGroup(items, stored) { group, segments ->
            val pairs = mutableListOf<Pair<Candidate, StoredSegment>>()
            for (segment in segments) {
                val lineId = segment.lineId ?: continue
                val candidate = group.firstOrNull { !it.taken && it.lineId == lineId } ?: continue
                candidate.taken = true
                pairs += candidate to segment
            }
            val legacy = segments.filter { it.lineId == null }
            val tiers: List<(Candidate, StoredSegment) -> Boolean> = listOf(
                { c, s -> sameCity(c, s) && sameDay(c, s) },
                { c, s -> sameCity(c, s) },
                { c, s -> sameDay(c, s) },
                { _, _ -> true }
            )
            pairs += pairTiers(group, legacy, tiers)
            pairs
        }

    /** One reserved segment consumes one item of its product: same day first, then any. */
    fun pairReserved(items: List<SegmentActivityItem>, stored: List<StoredSegment>): Result =
        pairByGroup(items, stored) { group, segments ->
            val tiers: List<(Candidate, StoredSegment) -> Boolean> = listOf(
                { c, s -> sameDay(c, s) },
                { _, _ -> true }
            )
            pairTiers(group, segments, tiers)
        }

    private class Candidate(
        val position: Int,
        val item: SegmentActivityItem,
        val lineId: String?,
        val cityId: Int?,
        val day: String?
    ) {
        var taken: Boolean = false
    }

    private val candidateOrder: Comparator<Candidate> = compareBy<Candidate>(
        { it.day.orEmpty() },
        { it.item.startDatetime.orEmpty() },
        { it.lineId.orEmpty() },
        { it.cityId ?: 0 },
        { it.item.endDatetime.orEmpty() },
        { it.item.title.orEmpty() }
    ).thenBy { it.position }

    private fun pairByGroup(
        items: List<SegmentActivityItem>,
        stored: List<StoredSegment>,
        pairGroup: (List<Candidate>, List<StoredSegment>) -> List<Pair<Candidate, StoredSegment>>
    ): Result {
        val candidatesByKey = items
            .mapIndexedNotNull { position, item ->
                val key = ActivityIdFormat.matchKey(item.activityId) ?: return@mapIndexedNotNull null
                key to Candidate(
                    position = position,
                    item = item,
                    lineId = item.lineId?.takeIf { it.isNotBlank() },
                    cityId = item.cityId,
                    day = calendarDay(item.startDatetime)
                )
            }
            .groupBy({ it.first }, { it.second })
        val storedByKey = stored.groupBy { it.matchKey }

        val pairs = mutableListOf<Pair<Candidate, StoredSegment>>()
        for (key in candidatesByKey.keys.intersect(storedByKey.keys).sorted()) {
            val group = candidatesByKey.getValue(key).sortedWith(candidateOrder)
            val segments = storedByKey.getValue(key).sortedBy { it.index }
            pairs += pairGroup(group, segments)
        }

        val pairedSegmentIndices = pairs.map { it.second.index }.toSet()
        val pairedPositions = pairs.map { it.first.position }.toSet()
        return Result(
            pairs = pairs
                .sortedBy { it.second.index }
                .map { (candidate, segment) -> Match(candidate.item, segment.index) },
            unmatchedItems = candidatesByKey.values.flatten()
                .filter { it.position !in pairedPositions }
                .sortedBy { it.position }
                .map { it.item },
            unmatchedSegmentIndices = stored
                .map { it.index }
                .filter { it !in pairedSegmentIndices }
                .sorted()
        )
    }

    /** Applies [tiers] in order; each tier pairs every still-free segment it can. */
    private fun pairTiers(
        group: List<Candidate>,
        segments: List<StoredSegment>,
        tiers: List<(Candidate, StoredSegment) -> Boolean>
    ): List<Pair<Candidate, StoredSegment>> {
        val pairs = mutableListOf<Pair<Candidate, StoredSegment>>()
        val pairedIndices = mutableSetOf<Int>()
        for (matches in tiers) {
            for (segment in segments) {
                if (segment.index in pairedIndices) continue
                val candidate = group.firstOrNull { !it.taken && matches(it, segment) } ?: continue
                candidate.taken = true
                pairedIndices += segment.index
                pairs += candidate to segment
            }
        }
        return pairs
    }

    private fun sameCity(candidate: Candidate, segment: StoredSegment): Boolean {
        val cityId = candidate.cityId?.takeIf { it > 0 } ?: return false
        return cityId == segment.cityId
    }

    private fun sameDay(candidate: Candidate, segment: StoredSegment): Boolean =
        candidate.day != null && candidate.day == segment.day

    /** "yyyy-MM-dd" prefix of any datetime shape the SDK handles, or null. */
    private fun calendarDay(datetime: String?): String? =
        datetime?.take(10)?.takeIf { it.length == 10 && it[4] == '-' && it[7] == '-' }
}
