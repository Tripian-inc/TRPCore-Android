package com.tripian.trpcore.domain.model.timeline

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.util.ActivityIdFormat

/**
 * Where a planned activity came from. Bookings are trip-level commitments;
 * recommendations are engine output tied to a single day.
 */
enum class PlannedActivitySource {
    BOOKING,
    RECOMMENDATION
}

/**
 * One occurrence of a bookable activity already placed in the trip.
 *
 * @param productId bare product id — the form every consumer compares on.
 * @param day "yyyy-MM-dd", or null when the source carries no usable date.
 */
data class PlannedActivity(
    val productId: String,
    val providerId: Int,
    val cityId: Int?,
    val day: String?,
    val source: PlannedActivitySource
)

/**
 * Every bookable activity already in the trip, from a single walk over both
 * sources: booked/reserved segments and itinerary activity steps. Consumers slice
 * by day or source instead of re-walking the timeline.
 *
 * Booking ids carry no reliable provider, so they report
 * [ActivityIdFormat.DEFAULT_PROVIDER_ID] — what the exclude payload has always
 * sent. Recommendation ids use the step's own provider.
 */
fun Timeline.plannedActivities(): List<PlannedActivity> {
    val activities = mutableListOf<PlannedActivity>()

    tripProfile?.segments.orEmpty().forEach { segment ->
        if (segment.segmentType != SegmentType.BOOKED_ACTIVITY &&
            segment.segmentType != SegmentType.RESERVED_ACTIVITY
        ) return@forEach

        val productId = ActivityIdFormat.base(segment.additionalData?.activityId) ?: return@forEach

        activities += PlannedActivity(
            productId = productId,
            providerId = ActivityIdFormat.DEFAULT_PROVIDER_ID,
            cityId = segment.cityId?.takeIf { it > 0 },
            day = dayPartOf(segment.additionalData?.startDatetime ?: segment.startDate),
            source = PlannedActivitySource.BOOKING
        )
    }

    plans.orEmpty().forEach { plan ->
        plan.steps.orEmpty().forEach { step ->
            if (step.stepType != STEP_TYPE_ACTIVITY) return@forEach

            val additionalData = step.poi?.additionalData
            val productId = ActivityIdFormat.base(additionalData?.productId) ?: return@forEach

            activities += PlannedActivity(
                productId = productId,
                providerId = additionalData?.providerId ?: ActivityIdFormat.DEFAULT_PROVIDER_ID,
                cityId = plan.city?.id?.takeIf { it > 0 },
                day = dayPartOf(step.startDateTimes),
                source = PlannedActivitySource.RECOMMENDATION
            )
        }
    }

    return activities
}

/**
 * "yyyy-MM-dd" → the activity ids that day already holds, in API form. Threaded
 * into the AddPlan flow: it both blocks a day that already holds the activity and
 * tells the server what the day holds when a new segment is created for it.
 */
fun Timeline.plannedActivityIdsByDay(): Map<String, List<String>> {
    val idsByDay = mutableMapOf<String, MutableList<String>>()

    plannedActivities().forEach { activity ->
        val day = activity.day ?: return@forEach
        val id = ActivityIdFormat.make(activity.productId, activity.providerId, activity.cityId)
        if (id.isEmpty()) return@forEach

        val dayIds = idsByDay.getOrPut(day) { mutableListOf() }
        if (id !in dayIds) dayIds += id
    }

    return idsByDay
}

private const val STEP_TYPE_ACTIVITY = "activity"

private fun dayPartOf(value: String?): String? {
    if (value == null || value.length < 10) return null
    return value.substring(0, 10)
}
