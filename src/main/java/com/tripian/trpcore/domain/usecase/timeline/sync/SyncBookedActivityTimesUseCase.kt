package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.ApiErrorMapper
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import com.tripian.trpcore.domain.model.timeline.toDate
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.sdk.TRPCoreErrorCode
import com.tripian.trpcore.util.ActivityIdFormat
import com.tripian.trpcore.util.extensions.toEditSettings
import javax.inject.Inject

/**
 * Re-times stored booked-activity segments whose start/end/duration no longer
 * match the host reservation (e.g. a transfer whose meeting time was assigned
 * after the segment was created with a pending slot). Only the time fields are
 * edited; the segment keeps its index, city and the rest of its payload.
 */
class SyncBookedActivityTimesUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<SyncBookedActivityTimesUseCase.Result, SyncBookedActivityTimesUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val itinerary: ItineraryWithActivities,
        val timeline: Timeline
    )

    data class Result(val mutated: Boolean)

    override suspend fun execute(params: Params): Result {
        val tripItemsByKey = tripItemsByMatchKey(params.itinerary.tripItems)
        if (tripItemsByKey.isEmpty()) return Result(mutated = false)

        var mutated = false
        params.timeline.tripProfile?.segments?.forEachIndexed { index, segment ->
            if (segment.segmentType != SegmentType.BOOKED_ACTIVITY) return@forEachIndexed
            val key = ActivityIdFormat.matchKey(segment.additionalData?.activityId) ?: return@forEachIndexed
            val tripItem = tripItemsByKey[key] ?: return@forEachIndexed
            val desired = params.itinerary.createBookedActivitySegment(tripItem)
            val desiredStart = desired.startDate ?: return@forEachIndexed
            val desiredEnd = desired.endDate ?: return@forEachIndexed

            val currentStart = segment.additionalData?.startDatetime ?: segment.startDate
            val currentEnd = segment.additionalData?.endDatetime ?: segment.endDate
            val sameStart = currentStart.toDate() == desiredStart.toDate()
            val sameEnd = currentEnd.toDate() == desiredEnd.toDate()
            val sameDuration = segment.additionalData?.duration == desired.additionalData?.duration
            if (sameStart && sameEnd && sameDuration) return@forEachIndexed

            val settings = segment.toEditSettings(index).apply {
                startDate = desiredStart
                endDate = desiredEnd
                additionalData?.apply {
                    startDatetime = desiredStart
                    endDatetime = desiredEnd
                    duration = desired.additionalData?.duration
                }
            }
            try {
                repository.editSegmentAsync(params.tripHash, settings)
                mutated = true
            } catch (error: Throwable) {
                val reason = ApiErrorMapper.serverMessage(error) ?: error.message.orEmpty()
                TRPCore.notifyError(
                    "${tripItem.title.orEmpty()} $reason".trim(),
                    TRPCoreErrorCode.BOOKED_ACTIVITY_SYNC_FAILED
                )
            }
        }
        return Result(mutated)
    }

    private fun tripItemsByMatchKey(items: List<SegmentActivityItem>?): Map<String, SegmentActivityItem> {
        val result = LinkedHashMap<String, SegmentActivityItem>()
        items?.forEach { item ->
            val key = ActivityIdFormat.matchKey(item.activityId) ?: return@forEach
            result.putIfAbsent(key, item)
        }
        return result
    }
}
