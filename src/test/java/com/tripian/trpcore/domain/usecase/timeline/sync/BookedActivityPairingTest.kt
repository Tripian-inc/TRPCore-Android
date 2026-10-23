package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineProfile
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the pairing contract of [BookedActivityPairing]: line-id identity inside a
 * match-key group, the legacy city/day fallback tiers, reserved-to-item pairing and
 * the leftover reporting that [SyncBookedActivityTimesUseCase],
 * [AddMissingBookedActivitiesUseCase] and [DetectReservedToBookedTransitionUseCase] rely on.
 */
class BookedActivityPairingTest {

    private val cityA = 1
    private val cityB = 2

    private fun item(
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
        activityId: String,
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

    private fun timelineOf(segments: List<TimelineSegment>): Timeline =
        Timeline().apply { tripProfile = TimelineProfile().apply { this.segments = segments } }

    @Test
    fun pairBookedMatchesByLineIdDespiteSwappedCityAndDay() {
        val itemIn = item(activityId = "2160", startDatetime = "2026-10-21 09:45", cityId = cityA, lineId = "24150760")
        val itemOut = item(activityId = "2160", startDatetime = "2026-10-28 09:00", cityId = cityB, lineId = "24150761")

        val storedIn = BookedActivityPairing.StoredSegment(index = 0, matchKey = "2160", lineId = "24150761", cityId = cityA, day = "2026-10-21")
        val storedOut = BookedActivityPairing.StoredSegment(index = 1, matchKey = "2160", lineId = "24150760", cityId = cityB, day = "2026-10-28")

        val result = BookedActivityPairing.pairBooked(listOf(itemIn, itemOut), listOf(storedIn, storedOut))

        assertEquals(2, result.pairs.size)
        assertEquals(itemOut, result.pairs[0].item)
        assertEquals(0, result.pairs[0].segmentIndex)
        assertEquals(itemIn, result.pairs[1].item)
        assertEquals(1, result.pairs[1].segmentIndex)
        assertTrue(result.unmatchedItems.isEmpty())
        assertTrue(result.unmatchedSegmentIndices.isEmpty())
    }

    @Test
    fun planPairsCorruptedLegacySegmentsByCityThenDayRegardlessOfItemOrder() {
        val segments = listOf(
            segment(SegmentType.BOOKED_ACTIVITY, "2160", cityA, "2026-10-21"),
            segment(SegmentType.BOOKED_ACTIVITY, "2160", cityB, "2026-10-21")
        )
        val itemIn = item("2160", "2026-10-21 09:45", cityId = cityA)
        val itemOut = item("2160", "2026-10-28 09:00", cityId = cityB)

        val forward = BookedActivityPairing.plan(listOf(itemIn, itemOut), segments)
        val reversed = BookedActivityPairing.plan(listOf(itemOut, itemIn), segments)

        for (plan in listOf(forward, reversed)) {
            assertEquals(2, plan.booked.pairs.size)
            assertEquals(itemIn, plan.booked.pairs[0].item)
            assertEquals(0, plan.booked.pairs[0].segmentIndex)
            assertEquals(itemOut, plan.booked.pairs[1].item)
            assertEquals(1, plan.booked.pairs[1].segmentIndex)
            assertTrue(plan.missingItems.isEmpty())
        }
    }

    @Test
    fun planPairsCorrectLegacySegmentsByCityAndDay() {
        val segments = listOf(
            segment(SegmentType.BOOKED_ACTIVITY, "2160", cityA, "2026-10-21"),
            segment(SegmentType.BOOKED_ACTIVITY, "2160", cityB, "2026-10-28")
        )
        val itemIn = item("2160", "2026-10-21 09:45", cityId = cityA)
        val itemOut = item("2160", "2026-10-28 09:00", cityId = cityB)

        val plan = BookedActivityPairing.plan(listOf(itemIn, itemOut), segments)

        assertEquals(2, plan.booked.pairs.size)
        assertEquals(itemIn, plan.booked.pairs[0].item)
        assertEquals(itemOut, plan.booked.pairs[1].item)
        assertTrue(plan.missingItems.isEmpty())
    }

    @Test
    fun planFallsBackToCityAloneWhenBothStoredSegmentsShareTheSameDay() {
        val segments = listOf(
            segment(SegmentType.BOOKED_ACTIVITY, "2160", cityA, "2026-10-21"),
            segment(SegmentType.BOOKED_ACTIVITY, "2160", cityA, "2026-10-21")
        )
        val itemSameDay = item("2160", "2026-10-21 09:45", cityId = cityA)
        val itemOtherDay = item("2160", "2026-10-28 09:00", cityId = cityA)

        val plan = BookedActivityPairing.plan(listOf(itemSameDay, itemOtherDay), segments)

        assertEquals(2, plan.booked.pairs.size)
        assertEquals(itemSameDay, plan.booked.pairs[0].item)
        assertEquals(itemOtherDay, plan.booked.pairs[1].item)
        assertTrue(plan.missingItems.isEmpty())
    }

    @Test
    fun reservedSegmentPrefersTheSameDayItemOverAnEarlierOne() {
        val segments = listOf(segment(SegmentType.RESERVED_ACTIVITY, "9999", cityId = null, day = "2026-10-28"))
        val itemEarly = item("9999", "2026-10-21 09:00")
        val itemLate = item("9999", "2026-10-28 09:00")

        val plan = BookedActivityPairing.plan(listOf(itemEarly, itemLate), segments)

        assertEquals(1, plan.reserved.pairs.size)
        assertEquals(itemLate, plan.reserved.pairs[0].item)
        assertEquals(listOf(itemEarly), plan.missingItems)
    }

    @Test
    fun reservedSegmentMatchesByProductAloneWhenNoItemSharesItsDay() {
        val segments = listOf(segment(SegmentType.RESERVED_ACTIVITY, "8888", cityId = null, day = "2026-10-28"))
        val onlyItem = item("8888", "2026-10-21 09:00")

        val plan = BookedActivityPairing.plan(listOf(onlyItem), segments)

        assertEquals(1, plan.reserved.pairs.size)
        assertEquals(onlyItem, plan.reserved.pairs[0].item)
        assertTrue(plan.missingItems.isEmpty())
    }

    @Test
    fun reservedSegmentDoesNotTakeAnItemAlreadyHeldByABookedSegment() {
        val segments = listOf(
            segment(SegmentType.BOOKED_ACTIVITY, "7777", cityA, "2026-10-21"),
            segment(SegmentType.RESERVED_ACTIVITY, "7777", cityA, "2026-10-21")
        )
        val onlyItem = item("7777", "2026-10-21 09:00", cityId = cityA)

        val plan = BookedActivityPairing.plan(listOf(onlyItem), segments)

        assertEquals(1, plan.booked.pairs.size)
        assertEquals(onlyItem, plan.booked.pairs[0].item)
        assertTrue(plan.reserved.pairs.isEmpty())
        assertEquals(listOf(1), plan.reserved.unmatchedSegmentIndices)
        assertTrue(plan.missingItems.isEmpty())
    }

    @Test
    fun planReportsUnmatchedItemsAndSegmentsAndIgnoresBlankActivityIds() {
        val storedSegment = segment(SegmentType.BOOKED_ACTIVITY, "B1", cityA, "2026-10-21")
        val unmatchedItem = item("A1", "2026-10-21 09:00", cityId = cityA)
        val blankItem = item(activityId = null, startDatetime = "2026-10-21 09:00", cityId = cityA)

        val plan = BookedActivityPairing.plan(listOf(unmatchedItem, blankItem), listOf(storedSegment))

        assertEquals(listOf(unmatchedItem), plan.missingItems)
        assertEquals(listOf(0), plan.booked.unmatchedSegmentIndices)
        assertTrue(plan.booked.pairs.isEmpty())
    }

    @Test
    fun matchKeyIgnoresTheProviderPrefixOnTheStoredSegment() {
        val segments = listOf(segment(SegmentType.BOOKED_ACTIVITY, "C_2160_15_28", cityA, "2026-10-21"))
        val matchingItem = item("2160", "2026-10-21 09:00", cityId = cityA)

        val plan = BookedActivityPairing.plan(listOf(matchingItem), segments)

        assertEquals(1, plan.booked.pairs.size)
        assertEquals(matchingItem, plan.booked.pairs[0].item)
    }

    @Test
    fun matchKeyIgnoresTheProductTypeSuffixOnTheStoredSegment() {
        val segments = listOf(segment(SegmentType.BOOKED_ACTIVITY, "2160¬TRF", cityA, "2026-10-21"))
        val matchingItem = item("2160", "2026-10-21 09:00", cityId = cityA)

        val plan = BookedActivityPairing.plan(listOf(matchingItem), segments)

        assertEquals(1, plan.booked.pairs.size)
        assertEquals(matchingItem, plan.booked.pairs[0].item)
    }

    @Test
    fun detectReservedToBookedTransitionReportsTheMatchedSegmentIndex() = runBlocking {
        val reservedSegment = segment(SegmentType.RESERVED_ACTIVITY, "2160", cityA, "2026-10-21")
        val matchingItem = item("2160", "2026-10-21 09:00", cityId = cityA)
        val timeline = timelineOf(listOf(reservedSegment))
        val useCase = DetectReservedToBookedTransitionUseCase()

        val transitions = useCase(DetectReservedToBookedTransitionUseCase.Params(timeline, listOf(matchingItem)))

        assertEquals(1, transitions.size)
        assertEquals(0, transitions[0].segmentIndex)
        assertEquals("2160", transitions[0].activityId)
        assertEquals(matchingItem, transitions[0].tripItem)
    }

    @Test
    fun planPairsLegacySegmentMatchingNeitherCityNorDayWithTheRemainingItem() {
        val segments = listOf(
            segment(SegmentType.BOOKED_ACTIVITY, "2160", cityA, "2026-10-21")
        )
        val rescheduled = item("2160", "2026-10-28 09:00", cityId = cityB)

        val plan = BookedActivityPairing.plan(listOf(rescheduled), segments)

        assertEquals(1, plan.booked.pairs.size)
        assertEquals(0, plan.booked.pairs[0].segmentIndex)
        assertEquals(rescheduled, plan.booked.pairs[0].item)
        assertTrue(plan.booked.unmatchedSegmentIndices.isEmpty())
        assertTrue(plan.missingItems.isEmpty())
    }
}
