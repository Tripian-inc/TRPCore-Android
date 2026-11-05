package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import com.tripian.trpcore.domain.model.itinerary.SegmentDestinationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins [ObsoleteSegmentSelector]'s reconcile rules: the lineId-aware booked-activity
 * removal, the host-id guard, and the unchanged city/date-range reasons it absorbed
 * from the view model.
 */
class ObsoleteSegmentSelectorTest {

    private val cityA = 1
    private val cityB = 2

    private fun hostItem(
        activityId: String?,
        startDatetime: String?,
        cityId: Int? = null,
        lineId: String? = null
    ): SegmentActivityItem = SegmentActivityItem(
        activityId = activityId,
        startDatetime = startDatetime,
        cityId = cityId,
        lineId = lineId
    )

    private fun segment(
        segmentType: String,
        activityId: String?,
        cityId: Int?,
        day: String
    ): TimelineSegment = TimelineSegment().apply {
        this.segmentType = segmentType
        this.cityId = cityId
        this.startDate = "$day 00:00"
        this.additionalData = TimelineSegmentAdditionalData().apply {
            this.activityId = activityId
            this.startDatetime = "$day 10:00"
        }
    }

    private fun timelineDateSegment(available: Boolean = false): TimelineSegment =
        TimelineSegment().apply {
            this.title = "TimelineDate"
            this.available = available
        }

    private fun itinerary(
        tripItems: List<SegmentActivityItem>,
        destinationCityIds: List<Int> = emptyList(),
        start: String = "2026-10-01",
        end: String = "2026-10-31"
    ): ItineraryWithActivities = ItineraryWithActivities(
        startDatetime = start,
        endDatetime = end,
        uniqueId = "trip-1",
        destinationItems = destinationCityIds.map { SegmentDestinationItem(title = "City", coordinate = "0,0", cityId = it) },
        tripItems = tripItems
    )

    @Test
    fun staleBookedSegmentNoLongerOnHostIsSelected() {
        val host = hostItem(activityId = "100", startDatetime = "2026-10-21 09:00", cityId = cityA)
        val stale = segment(SegmentType.BOOKED_ACTIVITY, "200", cityB, "2026-10-22")

        val selection = ObsoleteSegmentSelector.select(listOf(stale), itinerary(listOf(host)))

        assertEquals(mapOf(0 to ObsoleteSegmentSelector.Reason.NOT_IN_HOST_RESERVATIONS), selection.reasons)
        assertFalse(selection.hostIdGuardTripped)
    }

    @Test
    fun bookedSegmentStillOnHostIsKept() {
        val host = hostItem(activityId = "100", startDatetime = "2026-10-21 09:00", cityId = cityA)
        val stillBooked = segment(SegmentType.BOOKED_ACTIVITY, "100", cityA, "2026-10-21")

        val selection = ObsoleteSegmentSelector.select(listOf(stillBooked), itinerary(listOf(host)))

        assertTrue(selection.reasons.isEmpty())
    }

    @Test
    fun bookedSegmentWithoutActivityIdIsNeverSelected() {
        val host = hostItem(activityId = "100", startDatetime = "2026-10-21 09:00", cityId = cityA)
        val idLessSegment = segment(SegmentType.BOOKED_ACTIVITY, null, cityB, "2026-10-22")

        val selection = ObsoleteSegmentSelector.select(listOf(idLessSegment), itinerary(listOf(host)))

        assertTrue(selection.reasons.isEmpty())
    }

    @Test
    fun hostTripItemsWithNoActivityIdTripTheGuardAndSkipTheBookedReason() {
        val idLessHost = hostItem(activityId = null, startDatetime = "2026-10-21 09:00", cityId = cityA)
        val stored = segment(SegmentType.BOOKED_ACTIVITY, "100", cityA, "2026-10-21")

        val selection = ObsoleteSegmentSelector.select(listOf(stored), itinerary(listOf(idLessHost)))

        assertTrue(selection.hostIdGuardTripped)
        assertTrue(selection.reasons.isEmpty())
    }

    @Test
    fun emptyHostTripItemListTripsTheGuardAndSkipsTheBookedReason() {
        val stored = segment(SegmentType.BOOKED_ACTIVITY, "100", cityA, "2026-10-21")

        val selection = ObsoleteSegmentSelector.select(listOf(stored), itinerary(emptyList()))

        assertTrue(selection.hostIdGuardTripped)
        assertTrue(selection.reasons.isEmpty())
    }

    @Test
    fun legacyTwoLegsDistinguishedByCityAndDayOnlyTheCancelledLegIsSelected() {
        val inbound = hostItem(activityId = "100", startDatetime = "2026-10-21 09:00", cityId = cityA)
        val storedInbound = segment(SegmentType.BOOKED_ACTIVITY, "100", cityA, "2026-10-21")
        val storedOutbound = segment(SegmentType.BOOKED_ACTIVITY, "100", cityB, "2026-10-28")

        val selection = ObsoleteSegmentSelector.select(
            listOf(storedInbound, storedOutbound),
            itinerary(listOf(inbound))
        )

        assertEquals(mapOf(1 to ObsoleteSegmentSelector.Reason.NOT_IN_HOST_RESERVATIONS), selection.reasons)
    }

    @Test
    fun reservedSegmentsAreNeverSelectedForTheBookedReason() {
        val host = hostItem(activityId = "100", startDatetime = "2026-10-21 09:00", cityId = cityA)
        val reserved = segment(SegmentType.RESERVED_ACTIVITY, "999", cityB, "2026-10-22")

        val selection = ObsoleteSegmentSelector.select(listOf(reserved), itinerary(listOf(host)))

        assertTrue(selection.reasons.isEmpty())
    }

    @Test
    fun segmentWhoseCityLeftTheItineraryIsSelectedForCityRemoved() {
        val host = hostItem(activityId = "100", startDatetime = "2026-10-21 09:00", cityId = cityA)
        val removedCitySegment = segment(SegmentType.BOOKED_ACTIVITY, "100", cityB, "2026-10-21")

        val selection = ObsoleteSegmentSelector.select(
            listOf(removedCitySegment),
            itinerary(listOf(host), destinationCityIds = listOf(cityA))
        )

        assertEquals(mapOf(0 to ObsoleteSegmentSelector.Reason.CITY_REMOVED), selection.reasons)
    }

    @Test
    fun segmentOutsideTheTripDateRangeIsSelectedForOutOfDateRange() {
        val host = hostItem(activityId = "100", startDatetime = "2026-10-21 09:00", cityId = cityA)
        val outOfRangeSegment = segment(SegmentType.BOOKED_ACTIVITY, "100", cityA, "2026-11-15")

        val selection = ObsoleteSegmentSelector.select(
            listOf(outOfRangeSegment),
            itinerary(listOf(host), start = "2026-10-01", end = "2026-10-31")
        )

        assertEquals(mapOf(0 to ObsoleteSegmentSelector.Reason.OUT_OF_DATE_RANGE), selection.reasons)
    }

    @Test
    fun timelineDateSentinelIsNeverSelected() {
        val host = hostItem(activityId = "100", startDatetime = "2026-10-21 09:00", cityId = cityA)

        val selection = ObsoleteSegmentSelector.select(
            listOf(timelineDateSegment()),
            itinerary(listOf(host), start = "2026-10-01", end = "2026-10-31")
        )

        assertTrue(selection.reasons.isEmpty())
    }

    @Test
    fun whenGuardIsTrippedCityAndDateReasonsStillApply() {
        val idLessHost = hostItem(activityId = null, startDatetime = "2026-10-21 09:00", cityId = cityA)
        val removedCitySegment = segment(SegmentType.BOOKED_ACTIVITY, "100", cityB, "2026-10-21")
        val outOfRangeSegment = segment(SegmentType.BOOKED_ACTIVITY, "200", cityA, "2026-11-15")

        val selection = ObsoleteSegmentSelector.select(
            listOf(removedCitySegment, outOfRangeSegment),
            itinerary(listOf(idLessHost), destinationCityIds = listOf(cityA), start = "2026-10-01", end = "2026-10-31")
        )

        assertTrue(selection.hostIdGuardTripped)
        assertEquals(
            mapOf(
                0 to ObsoleteSegmentSelector.Reason.CITY_REMOVED,
                1 to ObsoleteSegmentSelector.Reason.OUT_OF_DATE_RANGE
            ),
            selection.reasons
        )
    }
}
