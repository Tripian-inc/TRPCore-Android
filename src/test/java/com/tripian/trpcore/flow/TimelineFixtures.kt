package com.tripian.trpcore.flow

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelinePlan
import com.tripian.one.api.timeline.model.TimelineProfile
import com.tripian.one.api.timeline.model.TimelineResponse
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.one.api.timeline.model.TimelineStepResponse

/** TRPOne model builders for flow scenarios and their Gson-serialized backend payloads. */
object TimelineFixtures {

    const val TRIP_HASH = "flow-trip-hash"
    const val DAY_1 = "2026-11-10"
    const val DAY_2 = "2026-11-11"
    const val DAY_3 = "2026-11-12"

    private val gson = Gson()

    fun itinerary(
        startDate: String,
        endDate: String,
        available: Boolean = true,
        distinctPlan: Boolean = false,
        title: String? = null
    ) = segment(SegmentType.ITINERARY, startDate, endDate).apply {
        this.available = available
        this.distinctPlan = distinctPlan
        title?.let { this.title = it }
    }

    fun untypedSegment(startDate: String, endDate: String) = segment(null, startDate, endDate)

    fun manualPoi(startDate: String, endDate: String) = segment(SegmentType.MANUAL_POI, startDate, endDate)

    fun reservedActivity(startDate: String, endDate: String) =
        activitySegment(SegmentType.RESERVED_ACTIVITY, startDate, endDate, bookingId = null)

    fun bookedActivity(startDate: String, endDate: String, available: Boolean = false) =
        activitySegment(SegmentType.BOOKED_ACTIVITY, startDate, endDate, bookingId = "booking-1").apply {
            this.available = available
        }

    fun step(id: Int, startDateTime: String, endDateTime: String) = TimelineStep().apply {
        this.id = id
        startDateTimes = startDateTime
        endDateTimes = endDateTime
    }

    /**
     * A timeline whose plans are index-aligned with [segments]; [stepsBySegment] puts steps
     * into the plan of the segment at that index.
     */
    fun timeline(
        segments: List<TimelineSegment>,
        stepsBySegment: Map<Int, List<TimelineStep>> = emptyMap()
    ) = Timeline().apply {
        id = 1
        tripHash = TRIP_HASH
        tripProfile = TimelineProfile().apply {
            cityId = 1
            this.segments = segments
        }
        plans = segments.mapIndexed { index, segment ->
            TimelinePlan().apply {
                id = "plan-$index"
                startDate = segment.startDate.orEmpty()
                endDate = segment.endDate.orEmpty()
                steps = stepsBySegment[index].orEmpty()
            }
        }
    }

    fun timelineResponseJson(timeline: Timeline): String = gson.toJson(
        TimelineResponse().apply {
            status = 200
            success = true
            data = timeline
        }
    )

    fun segmentUpdatedJson(): String = """{"data":{"updated":true}}"""

    fun stepResponseJson(step: TimelineStep): String = gson.toJson(
        TimelineStepResponse().apply {
            status = 200
            success = true
            data = step
        }
    )

    fun errorJson(message: String): String = gson.toJson(JsonObject().apply { addProperty("message", message) })

    private fun segment(type: String?, startDate: String, endDate: String) = TimelineSegment().apply {
        segmentType = type
        title = "${type ?: "untyped"} segment"
        cityId = 1
        this.startDate = startDate
        this.endDate = endDate
    }

    private fun activitySegment(type: String, startDate: String, endDate: String, bookingId: String?) =
        segment(type, startDate, endDate).apply {
            additionalData = TimelineSegmentAdditionalData().apply {
                activityId = "activity-1"
                this.bookingId = bookingId
                title = "Activity"
                startDatetime = startDate
                endDatetime = endDate
                price = 50.0
                currency = "USD"
            }
        }
}
