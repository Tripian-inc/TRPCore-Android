package com.tripian.trpcore.flow

import com.tripian.trpcore.flow.TimelineFixtures.DAY_1
import com.tripian.trpcore.flow.TimelineFixtures.DAY_2
import com.tripian.trpcore.flow.TimelineFixtures.TRIP_HASH
import com.tripian.trpcore.flow.TimelineFixtures.itinerary
import com.tripian.trpcore.flow.TimelineFixtures.step
import com.tripian.trpcore.flow.TimelineFixtures.timeline
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Step time changes, driven from [com.tripian.trpcore.ui.timeline.ACTimelineVM] down to
 * `PUT timeline/steps/{stepId}` and, when the step leaves its segment, the widening `PUT timeline/{hash}`.
 */
class TimelineStepTimeChangeFlowTest {

    @get:Rule
    val harness = TimelineFlowHarness()

    private val vm get() = harness.vm

    private val stepId = 42
    private val initial = timeline(
        segments = listOf(itinerary("$DAY_1 09:00", "$DAY_1 18:00")),
        stepsBySegment = mapOf(0 to listOf(step(stepId, "$DAY_1 10:00", "$DAY_1 11:00")))
    )

    @Test
    fun `moving a step before its itinerary widens the segment on the same day before updating the step`() {
        harness.loadTimeline(initial)
        harness.enqueueJson(200, TimelineFixtures.segmentUpdatedJson())
        harness.enqueueJson(200, TimelineFixtures.stepResponseJson(step(stepId, "$DAY_1 07:00", "$DAY_1 08:00")))
        harness.enqueueJson(200, TimelineFixtures.timelineResponseJson(initial))

        vm.updateStepTime(stepId, "07:00", "08:00", useInlineLoader = true)
        harness.awaitIdle()

        assertWideningPut(harness.takeRequest(), startDate = "$DAY_1 07:00", endDate = "$DAY_1 18:00")
        assertStepPut(harness.takeRequest(), startTime = "07:00", endTime = "08:00")
        harness.assertTimelineReloaded()
        harness.assertChangeFinished()
    }

    @Test
    fun `widening an itinerary past midnight for a step sends doNotGenerate 1 and stays available`() {
        harness.loadTimeline(initial)
        harness.enqueueJson(200, TimelineFixtures.segmentUpdatedJson())
        harness.enqueueJson(200, TimelineFixtures.stepResponseJson(step(stepId, "$DAY_1 23:00", "$DAY_2 00:30")))
        harness.enqueueJson(200, TimelineFixtures.timelineResponseJson(initial))

        vm.updateStepTime(stepId, "23:00", "00:30", useInlineLoader = true)
        harness.awaitIdle()

        assertWideningPut(harness.takeRequest(), startDate = "$DAY_1 09:00", endDate = "$DAY_2 00:30")
        assertStepPut(harness.takeRequest(), startTime = "23:00", endTime = "00:30")
        harness.assertTimelineReloaded()
        harness.assertChangeFinished()
    }

    @Test
    fun `a step that stays inside its segment is updated without touching the segment`() {
        harness.loadTimeline(initial)
        harness.enqueueJson(200, TimelineFixtures.stepResponseJson(step(stepId, "$DAY_1 10:30", "$DAY_1 11:30")))
        harness.enqueueJson(200, TimelineFixtures.timelineResponseJson(initial))

        vm.updateStepTime(stepId, "10:30", "11:30", useInlineLoader = true)
        harness.awaitIdle()

        assertStepPut(harness.takeRequest(), startTime = "10:30", endTime = "11:30")
        harness.assertTimelineReloaded()
        harness.assertChangeFinished()
    }

    private fun assertWideningPut(call: TimelineFlowHarness.RecordedCall, startDate: String, endDate: String) {
        assertEquals("PUT", call.method)
        assertEquals("${TimelineFlowHarness.MOCK_BASE_PATH}timeline/$TRIP_HASH", call.pathWithoutQuery)
        assertEquals("en", call.query["lang"])
        call.assertPlanFlags(doNotGenerate = 1, available = true, distinctPlan = false)
        val body = call.body!!
        assertEquals(0, body.get("segmentIndex").asInt)
        assertEquals("itinerary", body.get("segmentType").asString)
        assertEquals(startDate, body.get("startDate").asString)
        assertEquals(endDate, body.get("endDate").asString)
    }

    private fun assertStepPut(call: TimelineFlowHarness.RecordedCall, startTime: String, endTime: String) {
        assertEquals("PUT", call.method)
        assertEquals("${TimelineFlowHarness.MOCK_BASE_PATH}timeline/steps/$stepId", call.pathWithoutQuery)
        assertEquals("en", call.query["lang"])
        assertEquals(startTime, call.body!!.get("startTime").asString)
        assertEquals(endTime, call.body!!.get("endTime").asString)
    }
}
