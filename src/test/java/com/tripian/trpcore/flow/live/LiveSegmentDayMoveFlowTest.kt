package com.tripian.trpcore.flow.live

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.one.api.timeline.model.TimelineSettings
import com.tripian.trpcore.domain.model.timeline.TIMELINE_DATE_TITLE
import com.tripian.trpcore.flow.live.LiveTimelineHarness.Companion.CURRENCY
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Segment and step time changes driven from [com.tripian.trpcore.ui.timeline.ACTimelineVM] against the live
 * Tripian API. Each test builds a throwaway trip one week ahead: the production TimelineDate control segment
 * for three days, a generated day plan on day 1 and an activity on day 2.
 */
class LiveSegmentDayMoveFlowTest {

    @get:Rule
    val harness = LiveTimelineHarness()

    private val vm get() = harness.vm

    private val day1 = LocalDate.now().plusDays(7).toString()
    private val day2 = LocalDate.now().plusDays(8).toString()
    private val day3 = LocalDate.now().plusDays(9).toString()

    @Test
    fun `a booked activity moved to another day is accepted by the backend`() {
        val activityType = setUpTrip()
        val (index, activity) = segmentTitled(ACTIVITY_TITLE)

        val call = changeSegmentTime("move $activityType from day 2 to day 3", activity, index, "10:00", "12:00", newDate = day3)

        call.assertPlanFlags(doNotGenerate = 1, available = false, distinctPlan = true)
        assertSegmentStartsAt(ACTIVITY_TITLE, "$day3 10:00")
    }

    @Test
    fun `a booked activity's hours changed on the same day are accepted by the backend`() {
        val activityType = setUpTrip()
        val (index, activity) = segmentTitled(ACTIVITY_TITLE)

        val call = changeSegmentTime("change $activityType hours on day 2", activity, index, "14:00", "16:00")

        call.assertPlanFlags(doNotGenerate = 1, available = false, distinctPlan = true)
        assertSegmentStartsAt(ACTIVITY_TITLE, "$day2 14:00")
    }

    @Test
    fun `an itinerary's hours changed on the same day are accepted by the backend`() {
        setUpTrip()
        val (index, dayPlan) = segmentTitled(DAY_PLAN_TITLE)

        val call = changeSegmentTime("change itinerary hours on day 1", dayPlan, index, "10:00", "17:00")

        call.assertPlanFlags(doNotGenerate = 1, available = true, distinctPlan = dayPlan.distinctPlan)
        assertSegmentStartsAt(DAY_PLAN_TITLE, "$day1 10:00")
    }

    @Test
    fun `a step's time change is accepted by the backend`() {
        setUpTrip()
        val (index, _) = segmentTitled(DAY_PLAN_TITLE)
        val step = vm.timeline.value?.plans?.getOrNull(index)?.steps?.firstOrNull()
        Assume.assumeTrue("The generated day plan has no steps", step != null)
        val stepStart = LocalTime.parse(timeOf(step!!.startDateTimes))
        val stepEnd = step.endDateTimes?.let { LocalTime.parse(timeOf(it)) } ?: stepStart.plusHours(1)
        val newStart = stepStart.plusMinutes(STEP_SHIFT_MINUTES).toString()
        val newEnd = stepEnd.plusMinutes(STEP_SHIFT_MINUTES).toString()
        val segmentCallsBefore = harness.segmentCalls.size
        val stepCallsBefore = harness.stepCalls.size

        vm.resetChangeTimeFinished()
        vm.updateStepTime(step.id, newStart, newEnd, useInlineLoader = true)
        val finished = harness.awaitChangeFinished()

        harness.segmentCalls.drop(segmentCallsBefore).forEach {
            harness.report("step change widened its segment: ${it.flags} -> ${it.outcome}")
        }
        val call = harness.stepCalls.getOrNull(stepCallsBefore)
            ?: throw AssertionError("No step PUT was sent; VM error: ${harness.lastErrorMessage()}")
        harness.report("move step ${step.id} from $stepStart to $newStart on day 1: ${call.outcome}")
        assertTrue("Step PUT ${call.outcome}; VM error: ${harness.lastErrorMessage()}", finished && call.outcome.accepted)
        val reloaded = vm.timeline.value?.plans?.flatMap { it.steps.orEmpty() }?.firstOrNull { it.id == step.id }
        harness.report("step ${step.id} after reload starts at ${reloaded?.startDateTimes}")
        assertTrue(
            "Step ${step.id} should start at $newStart after reload but starts at ${reloaded?.startDateTimes}",
            reloaded?.startDateTimes?.let(::timeOf) == newStart
        )
    }

    /** Builds the trip, loads it into the VM and returns the activity segment type the backend accepted. */
    private fun setUpTrip(): String {
        val created = harness.createTimeline(
            TimelineSettings.create(
                cityId = harness.config.cityId,
                adults = 1,
                children = 0,
                segments = listOf(timelineDateSegment())
            )
        )
        harness.report("created trip ${created.tripHash} on ${harness.config}")

        val dayPlan = harness.addSegment(created.tripHash, dayPlanSegment())
        harness.report("add itinerary day plan: ${dayPlan.flags} -> ${dayPlan.outcome}")
        assertTrue("Adding the day plan was ${dayPlan.outcome}", dayPlan.outcome.accepted)
        harness.awaitGeneration(created.tripHash)

        val activityType = addActivity(created.tripHash, created.city?.coordinate)
        harness.awaitGeneration(created.tripHash)
        harness.loadTimeline(created.tripHash)
        return activityType
    }

    private fun addActivity(tripHash: String, coordinate: Coordinate?): String {
        val booked = harness.addSegment(tripHash, activitySegment(SegmentType.BOOKED_ACTIVITY, coordinate))
        harness.report("add booked_activity: ${booked.flags} -> ${booked.outcome}")
        if (booked.outcome.accepted) return SegmentType.BOOKED_ACTIVITY

        val manual = harness.addSegment(tripHash, activitySegment(SegmentType.MANUAL_POI, coordinate))
        harness.report("add manual_poi fallback: ${manual.flags} -> ${manual.outcome}")
        assertTrue(
            "The backend refused booked_activity (${booked.outcome}) and manual_poi (${manual.outcome})",
            manual.outcome.accepted
        )
        return SegmentType.MANUAL_POI
    }

    private fun changeSegmentTime(
        scenario: String,
        segment: TimelineSegment,
        segmentIndex: Int,
        startTime: String,
        endTime: String,
        newDate: String? = null
    ): LiveTimelineHarness.SegmentCall {
        val callsBefore = harness.segmentCalls.size
        vm.resetChangeTimeFinished()
        vm.updateSegmentTime(segment, segmentIndex, startTime, endTime, newDate = newDate, useInlineLoader = true)
        val finished = harness.awaitChangeFinished()

        val call = harness.segmentCalls.getOrNull(callsBefore)
            ?: throw AssertionError("No segment PUT was sent for '$scenario'; VM error: ${harness.lastErrorMessage()}")
        harness.report("$scenario: ${call.flags} -> ${call.outcome}")
        assertTrue(
            "'$scenario' PUT ${call.outcome}; VM error: ${harness.lastErrorMessage()}",
            finished && call.outcome.accepted
        )
        return call
    }

    private fun segmentTitled(title: String): IndexedValue<TimelineSegment> {
        val timeline: Timeline = vm.timeline.value ?: throw AssertionError("No timeline loaded")
        return timeline.tripProfile?.segments.orEmpty().withIndex().firstOrNull { it.value.title == title }
            ?: throw AssertionError("No segment titled '$title' in trip ${timeline.tripHash}")
    }

    private fun assertSegmentStartsAt(title: String, expectedStart: String) {
        val actual = segmentTitled(title).value.startDate
        harness.report("'$title' after reload starts at $actual")
        assertTrue("'$title' should start at $expectedStart after reload but starts at $actual", actual?.startsWith(expectedStart) == true)
    }

    private fun timeOf(dateTime: String?): String =
        dateTime?.takeIf { it.length >= 16 }?.substring(11, 16)
            ?: throw AssertionError("Unreadable date-time '$dateTime'")

    private fun timelineDateSegment() = TimelineSegmentSettings.create(
        title = TIMELINE_DATE_TITLE,
        cityId = harness.config.cityId,
        startDate = "$day1 00:00",
        endDate = "$day3 23:59",
        adults = 1,
        children = 0,
        available = false,
        distinctPlan = true,
        currency = CURRENCY
    ).apply { segmentType = SegmentType.ITINERARY }

    private fun dayPlanSegment() = TimelineSegmentSettings.create(
        title = DAY_PLAN_TITLE,
        cityId = harness.config.cityId,
        startDate = "$day1 09:00",
        endDate = "$day1 18:00",
        adults = 1,
        children = 0,
        answerIds = emptyList(),
        currency = CURRENCY
    ).apply {
        smartRecommendation = true
        activityFreeText = ""
        activityIds = emptyList()
        excludedActivityIds = emptyList()
        segmentType = SegmentType.ITINERARY
        distinctPlan = true
        available = true
    }

    private fun activitySegment(type: String, coordinate: Coordinate?) = TimelineSegmentSettings.create(
        title = ACTIVITY_TITLE,
        cityId = harness.config.cityId,
        startDate = "$day2 10:00",
        endDate = "$day2 12:00",
        adults = 1,
        children = 0,
        coordinate = coordinate,
        available = false,
        distinctPlan = true,
        currency = CURRENCY
    ).apply {
        segmentType = type
        if (type == SegmentType.BOOKED_ACTIVITY) {
            additionalData = TimelineSegmentAdditionalData().apply {
                activityId = ACTIVITY_ID
                bookingId = "livetest-booking-$day2"
                title = ACTIVITY_TITLE
                startDatetime = "$day2 10:00"
                endDatetime = "$day2 12:00"
                this.coordinate = coordinate
                price = 0.0
                currency = CURRENCY
                duration = 120.0
                isNoLocation = coordinate == null
            }
        }
    }

    private companion object {
        const val ACTIVITY_TITLE = "Live test activity"
        const val DAY_PLAN_TITLE = "Live test day plan"
        const val ACTIVITY_ID = "15423"
        const val STEP_SHIFT_MINUTES = 15L
    }
}
