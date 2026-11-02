package com.tripian.trpcore.flow

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.trpcore.domain.model.timeline.TIMELINE_DATE_TITLE
import com.tripian.trpcore.flow.TimelineFixtures.DAY_1
import com.tripian.trpcore.flow.TimelineFixtures.DAY_2
import com.tripian.trpcore.flow.TimelineFixtures.DAY_3
import com.tripian.trpcore.flow.TimelineFixtures.TRIP_HASH
import com.tripian.trpcore.flow.TimelineFixtures.bookedActivity
import com.tripian.trpcore.flow.TimelineFixtures.itinerary
import com.tripian.trpcore.flow.TimelineFixtures.manualPoi
import com.tripian.trpcore.flow.TimelineFixtures.reservedActivity
import com.tripian.trpcore.flow.TimelineFixtures.timeline
import com.tripian.trpcore.flow.TimelineFixtures.untypedSegment
import com.tripian.trpcore.util.LanguageConst
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Segment time changes and day moves, driven from [com.tripian.trpcore.ui.timeline.ACTimelineVM]
 * down to the `PUT timeline/{hash}` request.
 */
class TimelineSegmentDayMoveFlowTest {

    @get:Rule
    val harness = TimelineFlowHarness()

    private val vm get() = harness.vm

    @Test
    fun `moving a booked activity to another day sends one bare unavailable segment and closes the sheet`() {
        val initial = timeline(listOf(itinerary("$DAY_1 09:00", "$DAY_1 18:00"), bookedActivity("$DAY_2 10:00", "$DAY_2 12:00")))
        harness.loadTimeline(initial)

        succeedSegmentChange(initial, segmentIndex = 1, startTime = "10:00", endTime = "12:00", newDate = DAY_3)

        val put = harness.takeRequest()
        assertSegmentPut(put, segmentIndex = 1, segmentType = "booked_activity", available = false, distinctPlan = true, startDate = "$DAY_3 10:00", endDate = "$DAY_3 12:00")
        val additionalData = put.body!!.getAsJsonObject("additionalData")
        assertEquals("$DAY_3 10:00", additionalData.get("startDatetime").asString)
        assertEquals("$DAY_3 12:00", additionalData.get("endDatetime").asString)
        assertNull("The segment must not be wrapped", put.body!!.get("segments"))
        assertNull("The segment must not be wrapped", put.body!!.get("segment"))
        harness.assertTimelineReloaded()
        harness.assertChangeFinished()
    }

    @Test
    fun `a booked activity that arrives available still moves to another day as unavailable with a distinct plan`() {
        val initial = timeline(listOf(itinerary("$DAY_1 09:00", "$DAY_1 18:00"), bookedActivity("$DAY_2 10:00", "$DAY_2 12:00", available = true)))
        harness.loadTimeline(initial)

        succeedSegmentChange(initial, segmentIndex = 1, startTime = "10:00", endTime = "12:00", newDate = DAY_3)

        assertSegmentPut(harness.takeRequest(), segmentIndex = 1, segmentType = "booked_activity", available = false, distinctPlan = true, startDate = "$DAY_3 10:00", endDate = "$DAY_3 12:00")
        harness.assertTimelineReloaded()
        harness.assertChangeFinished()
    }

    @Test
    fun `changing a reserved activity time on the same day sends it unavailable with a distinct plan`() {
        val initial = timeline(listOf(itinerary("$DAY_1 09:00", "$DAY_1 18:00"), reservedActivity("$DAY_2 10:00", "$DAY_2 12:00")))
        harness.loadTimeline(initial)

        succeedSegmentChange(initial, segmentIndex = 1, startTime = "14:00", endTime = "16:00")

        assertSegmentPut(harness.takeRequest(), segmentIndex = 1, segmentType = "reserved_activity", available = false, distinctPlan = true, startDate = "$DAY_2 14:00", endDate = "$DAY_2 16:00")
        harness.assertTimelineReloaded()
        harness.assertChangeFinished()
    }

    @Test
    fun `changing a manual poi time on the same day sends it unavailable with a distinct plan`() {
        val initial = timeline(listOf(itinerary("$DAY_1 09:00", "$DAY_1 18:00"), manualPoi("$DAY_2 10:00", "$DAY_2 11:00")))
        harness.loadTimeline(initial)

        succeedSegmentChange(initial, segmentIndex = 1, startTime = "12:00", endTime = "13:00")

        assertSegmentPut(harness.takeRequest(), segmentIndex = 1, segmentType = "manual_poi", available = false, distinctPlan = true, startDate = "$DAY_2 12:00", endDate = "$DAY_2 13:00")
        harness.assertTimelineReloaded()
        harness.assertChangeFinished()
    }

    @Test
    fun `a segment without a type is sent as unavailable with a distinct plan when its time changes on the same day`() {
        val initial = timeline(listOf(itinerary("$DAY_1 09:00", "$DAY_1 18:00"), untypedSegment("$DAY_2 10:00", "$DAY_2 11:00")))
        harness.loadTimeline(initial)

        succeedSegmentChange(initial, segmentIndex = 1, startTime = "15:00", endTime = "16:00")

        assertSegmentPut(harness.takeRequest(), segmentIndex = 1, segmentType = null, available = false, distinctPlan = true, startDate = "$DAY_2 15:00", endDate = "$DAY_2 16:00")
        harness.assertTimelineReloaded()
        harness.assertChangeFinished()
    }

    @Test
    fun `changing an itinerary time on the same day keeps it available`() {
        val initial = timeline(listOf(itinerary("$DAY_1 09:00", "$DAY_1 18:00")))
        harness.loadTimeline(initial)

        succeedSegmentChange(initial, segmentIndex = 0, startTime = "10:00", endTime = "19:00")

        assertSegmentPut(harness.takeRequest(), segmentIndex = 0, segmentType = "itinerary", available = true, distinctPlan = false, startDate = "$DAY_1 10:00", endDate = "$DAY_1 19:00")
        harness.assertTimelineReloaded()
        harness.assertChangeFinished()
    }

    @Test
    fun `an itinerary that arrives unavailable goes out available with its distinct plan flag echoed`() {
        val initial = timeline(listOf(itinerary("$DAY_1 09:00", "$DAY_1 18:00", available = false, distinctPlan = true)))
        harness.loadTimeline(initial)

        succeedSegmentChange(initial, segmentIndex = 0, startTime = "10:00", endTime = "19:00")

        assertSegmentPut(harness.takeRequest(), segmentIndex = 0, segmentType = "itinerary", available = true, distinctPlan = true, startDate = "$DAY_1 10:00", endDate = "$DAY_1 19:00")
        harness.assertTimelineReloaded()
        harness.assertChangeFinished()
    }

    @Test
    fun `an itinerary titled TimelineDate is sent as unavailable with a distinct plan when its time changes on the same day`() {
        val initial = timeline(listOf(itinerary("$DAY_1 09:00", "$DAY_1 18:00", title = TIMELINE_DATE_TITLE)))
        harness.loadTimeline(initial)

        succeedSegmentChange(initial, segmentIndex = 0, startTime = "10:00", endTime = "19:00")

        val put = harness.takeRequest()
        assertSegmentPut(put, segmentIndex = 0, segmentType = "itinerary", available = false, distinctPlan = true, startDate = "$DAY_1 10:00", endDate = "$DAY_1 19:00")
        assertEquals(TIMELINE_DATE_TITLE, put.body!!.get("title").asString)
        harness.assertTimelineReloaded()
        harness.assertChangeFinished()
    }

    @Test
    fun `moving an itinerary to another day sends doNotGenerate 1 and stays available`() {
        val initial = timeline(listOf(itinerary("$DAY_1 09:00", "$DAY_1 18:00")))
        harness.loadTimeline(initial)

        succeedSegmentChange(initial, segmentIndex = 0, startTime = "09:00", endTime = "18:00", newDate = DAY_2)

        assertSegmentPut(harness.takeRequest(), segmentIndex = 0, segmentType = "itinerary", available = true, distinctPlan = false, startDate = "$DAY_2 09:00", endDate = "$DAY_2 18:00")
        harness.assertTimelineReloaded()
        harness.assertChangeFinished()
    }

    @Test
    fun `a rejected day move shows the backend message and leaves the sheet open`() {
        val backendMessage = "The segment's dates cannot change while its plan is kept; regenerate the segment or keep its dates."
        val initial = timeline(listOf(itinerary("$DAY_1 09:00", "$DAY_1 18:00"), bookedActivity("$DAY_2 10:00", "$DAY_2 12:00")))
        harness.loadTimeline(initial)
        harness.enqueueJson(400, TimelineFixtures.errorJson(backendMessage))

        vm.updateSegmentTime(segmentAt(1), 1, "10:00", "12:00", newDate = DAY_3, useInlineLoader = true)
        harness.awaitIdle()

        assertSegmentPut(harness.takeRequest(), segmentIndex = 1, segmentType = "booked_activity", available = false, distinctPlan = true, startDate = "$DAY_3 10:00", endDate = "$DAY_3 12:00")
        harness.assertNoMoreRequests()
        assertEquals(backendMessage, vm.error.value)
        assertEquals(false, vm.changeTimeFinished.value)
    }

    @Test
    fun `an identical rejection after the shown error was consumed surfaces again`() {
        val backendMessage = "The segment's dates cannot change while its plan is kept; regenerate the segment or keep its dates."
        val initial = timeline(listOf(itinerary("$DAY_1 09:00", "$DAY_1 18:00"), bookedActivity("$DAY_2 10:00", "$DAY_2 12:00")))
        harness.loadTimeline(initial)
        harness.enqueueJson(400, TimelineFixtures.errorJson(backendMessage))
        harness.enqueueJson(400, TimelineFixtures.errorJson(backendMessage))

        vm.updateSegmentTime(segmentAt(1), 1, "10:00", "12:00", newDate = DAY_3, useInlineLoader = true)
        harness.awaitIdle()
        assertEquals(backendMessage, vm.error.value)
        assertEquals(false, vm.changeTimeFinished.value)

        vm.consumeError()
        assertNull(vm.error.value)

        vm.updateSegmentTime(segmentAt(1), 1, "10:00", "12:00", newDate = DAY_3, useInlineLoader = true)
        harness.awaitIdle()
        assertEquals(backendMessage, vm.error.value)
        assertEquals(false, vm.changeTimeFinished.value)

        val firstPut = harness.takeRequest()
        val secondPut = harness.takeRequest()
        assertSegmentPut(firstPut, segmentIndex = 1, segmentType = "booked_activity", available = false, distinctPlan = true, startDate = "$DAY_3 10:00", endDate = "$DAY_3 12:00")
        assertEquals(firstPut.body, secondPut.body)
        harness.assertNoMoreRequests()
    }

    @Test
    fun `a rejection with a blank message shows the common error and leaves the sheet open`() {
        assertRejectionFallsBackToCommonError(TimelineFixtures.errorJson(""))
    }

    @Test
    fun `a rejection without a message shows the common error and leaves the sheet open`() {
        assertRejectionFallsBackToCommonError("{}")
    }

    @Test
    fun `a successful retry after a rejection clears the error and closes the sheet`() {
        val backendMessage = "The segment's dates cannot change while its plan is kept; regenerate the segment or keep its dates."
        val initial = timeline(listOf(itinerary("$DAY_1 09:00", "$DAY_1 18:00"), bookedActivity("$DAY_2 10:00", "$DAY_2 12:00")))
        harness.loadTimeline(initial)
        harness.enqueueJson(400, TimelineFixtures.errorJson(backendMessage))

        vm.updateSegmentTime(segmentAt(1), 1, "10:00", "12:00", newDate = DAY_3, useInlineLoader = true)
        harness.awaitIdle()
        assertEquals(backendMessage, vm.error.value)
        vm.consumeError()
        vm.resetChangeTimeFinished()

        succeedSegmentChange(initial, segmentIndex = 1, startTime = "10:00", endTime = "12:00", newDate = DAY_3)

        assertSegmentPut(harness.takeRequest(), segmentIndex = 1, segmentType = "booked_activity", available = false, distinctPlan = true, startDate = "$DAY_3 10:00", endDate = "$DAY_3 12:00")
        assertSegmentPut(harness.takeRequest(), segmentIndex = 1, segmentType = "booked_activity", available = false, distinctPlan = true, startDate = "$DAY_3 10:00", endDate = "$DAY_3 12:00")
        harness.assertTimelineReloaded()
        harness.assertChangeFinished()
    }

    private fun assertRejectionFallsBackToCommonError(errorBody: String) {
        val initial = timeline(listOf(itinerary("$DAY_1 09:00", "$DAY_1 18:00"), bookedActivity("$DAY_2 10:00", "$DAY_2 12:00")))
        harness.loadTimeline(initial)
        harness.enqueueJson(400, errorBody)

        vm.updateSegmentTime(segmentAt(1), 1, "10:00", "12:00", newDate = DAY_3, useInlineLoader = true)
        harness.awaitIdle()

        assertSegmentPut(harness.takeRequest(), segmentIndex = 1, segmentType = "booked_activity", available = false, distinctPlan = true, startDate = "$DAY_3 10:00", endDate = "$DAY_3 12:00")
        harness.assertNoMoreRequests()
        assertEquals(harness.localized(LanguageConst.COMMON_ERROR), vm.error.value)
        assertEquals(false, vm.changeTimeFinished.value)
    }

    private fun segmentAt(index: Int): TimelineSegment = vm.timeline.value!!.tripProfile!!.segments!![index]

    private fun succeedSegmentChange(
        reloaded: Timeline,
        segmentIndex: Int,
        startTime: String,
        endTime: String,
        newDate: String? = null
    ) {
        harness.enqueueJson(200, TimelineFixtures.segmentUpdatedJson())
        harness.enqueueJson(200, TimelineFixtures.timelineResponseJson(reloaded))
        vm.updateSegmentTime(segmentAt(segmentIndex), segmentIndex, startTime, endTime, newDate = newDate, useInlineLoader = true)
        harness.awaitIdle()
    }

    /** Asserts an in-place segment update; every update carries doNotGenerate 1 because segmentIndex is set. */
    private fun assertSegmentPut(
        call: TimelineFlowHarness.RecordedCall,
        segmentIndex: Int,
        segmentType: String?,
        available: Boolean,
        distinctPlan: Boolean,
        startDate: String,
        endDate: String
    ) {
        assertEquals("PUT", call.method)
        assertEquals("${TimelineFlowHarness.MOCK_BASE_PATH}timeline/$TRIP_HASH", call.pathWithoutQuery)
        assertEquals("en", call.query["lang"])
        call.assertPlanFlags(doNotGenerate = 1, available = available, distinctPlan = distinctPlan)
        val body = call.body!!
        assertEquals(segmentIndex, body.get("segmentIndex").asInt)
        if (segmentType == null) {
            assertTrue("segmentType must be absent or null", body.get("segmentType")?.isJsonNull ?: true)
        } else {
            assertEquals(segmentType, body.get("segmentType").asString)
        }
        assertEquals(startDate, body.get("startDate").asString)
        assertEquals(endDate, body.get("endDate").asString)
    }
}
