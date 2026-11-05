package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.util.ActivityIdFormat

/**
 * Picks the timeline segments that no longer belong on the trip after a host sync,
 * unioned by index against a single snapshot. Pure: no I/O, no Android dependencies.
 *
 * Reasons, in priority order: a booked activity the host no longer reports
 * (lineId-aware, see [BookedActivityPairing]); a city that left the itinerary;
 * a day outside the trip range.
 *
 * Never a candidate: the TimelineDate sentinel, a segment whose city is simply
 * unresolved, and a booked segment carrying no activityId. When the host sends no
 * trip items or any of them lacks an activity id, the booked-activity reason is
 * skipped entirely so a partial payload cannot delete booked cards it still holds. A reserved activity the user added
 * inside the SDK is never removed for not being in the host reservations.
 */
object ObsoleteSegmentSelector {

    enum class Reason { NOT_IN_HOST_RESERVATIONS, CITY_REMOVED, OUT_OF_DATE_RANGE }

    /**
     * @param reasons segment index to the first reason that selected it.
     * @param hostIdGuardTripped true when the host sent no trip items or one without an
     *   activity id, and the booked-activity reason was skipped.
     */
    data class Selection(val reasons: Map<Int, Reason>, val hostIdGuardTripped: Boolean) {
        val indices: Set<Int>
            get() = reasons.keys
    }

    fun select(segments: List<TimelineSegment>?, itinerary: ItineraryWithActivities): Selection {
        val all = segments.orEmpty()
        val reasons = linkedMapOf<Int, Reason>()

        val hostItems = itinerary.tripItems.orEmpty()
        val hostIdGuardTripped = hostItems.isEmpty() ||
            hostItems.any { ActivityIdFormat.matchKey(it.activityId) == null }
        if (!hostIdGuardTripped) {
            BookedActivityPairing.plan(hostItems, all).booked.unmatchedSegmentIndices
                .forEach { reasons.putIfAbsent(it, Reason.NOT_IN_HOST_RESERVATIONS) }
        }

        val currentCityIds = itinerary.destinationItems
            .mapNotNull { it.cityId }
            .filter { it > 0 }
            .toSet()
        val tripStart = calendarDay(itinerary.startDatetime)
        val tripEnd = calendarDay(itinerary.endDatetime)

        all.forEachIndexed { index, segment ->
            if (index in reasons) return@forEachIndexed
            if (segment.title == "TimelineDate" && !segment.available) return@forEachIndexed

            val cityId = segment.cityId
            if (currentCityIds.isNotEmpty() && cityId != null && cityId > 0 && cityId !in currentCityIds) {
                reasons[index] = Reason.CITY_REMOVED
                return@forEachIndexed
            }

            if (tripStart != null && tripEnd != null) {
                val day = calendarDay(segment.startDate)
                if (day != null && (day < tripStart || day > tripEnd)) {
                    reasons[index] = Reason.OUT_OF_DATE_RANGE
                }
            }
        }
        return Selection(reasons, hostIdGuardTripped)
    }

    private fun calendarDay(datetime: String?): String? =
        datetime?.take(10)?.takeIf { it.length == 10 && it[4] == '-' && it[7] == '-' }
}
