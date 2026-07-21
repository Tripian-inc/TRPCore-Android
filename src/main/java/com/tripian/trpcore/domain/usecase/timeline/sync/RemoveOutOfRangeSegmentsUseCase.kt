package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.repository.TimelineRepository
import javax.inject.Inject

/**
 * RemoveOutOfRangeSegmentsUseCase
 *
 * When the host re-opens the SDK with a shifted date range, removes segments
 * anchored on days that fall outside the new range.
 *
 * Rules:
 *  - Skip the TimelineDate sentinel (`title == "TimelineDate" && !available`).
 *  - A segment is "out of range" when its `startDate` day prefix is strictly
 *    before the new start day or strictly after the new end day.
 *  - Segments without a parseable date are left alone.
 *  - Local mutation is optimistic; server DELETEs run sequentially in
 *    descending index order to avoid array shifts on the backend.
 */
class RemoveOutOfRangeSegmentsUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<RemoveOutOfRangeSegmentsUseCase.Result, RemoveOutOfRangeSegmentsUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val itinerary: ItineraryWithActivities,
        val timeline: Timeline
    )

    /** True iff at least one segment was removed — caller refreshes the UI. */
    data class Result(val removedCount: Int) {
        val mutated: Boolean get() = removedCount > 0
    }

    override suspend fun execute(params: Params): Result {
        val result = applyLocally(params) ?: return Result(0)
        if (result.indicesDescending.isEmpty()) return Result(0)
        for (idx in result.indicesDescending) {
            try {
                repository.deleteSegmentAsync(params.tripHash, idx)
            } catch (error: Throwable) {
                android.util.Log.e(
                    "SYNC",
                    "Out-of-range segment delete (index $idx) failed: ${error.message}"
                )
            }
        }
        return Result(result.indicesDescending.size)
    }

    private data class LocalPass(val indicesDescending: List<Int>)

    /**
     * Optimistically removes out-of-range segments from the in-memory list;
     * if the list isn't mutable, the server delete + post-sync refetch reconcile.
     */
    private fun applyLocally(params: Params): LocalPass? {
        val newRange = extractDayRange(params.itinerary) ?: return null
        val segments = params.timeline.tripProfile?.segments ?: return null

        val outOfRange = segments.withIndex().filter { (_, seg) ->
            isOutOfRange(seg, newRange.first, newRange.second)
        }
        if (outOfRange.isEmpty()) return LocalPass(emptyList())

        val mutable = segments as? MutableList<TimelineSegment>
        val indicesDesc = outOfRange.map { it.index }.sortedDescending()
        if (mutable != null) {
            indicesDesc.forEach { mutable.removeAt(it) }
        }
        return LocalPass(indicesDesc)
    }

    private fun isOutOfRange(seg: TimelineSegment, startDay: String, endDay: String): Boolean {
        if (seg.title == "TimelineDate" && !seg.available) return false
        val day = seg.startDate?.take(10)?.takeIf {
            it.length == 10 && it[4] == '-' && it[7] == '-'
        } ?: return false
        return day < startDay || day > endDay
    }

    private fun extractDayRange(itinerary: ItineraryWithActivities): Pair<String, String>? {
        val start = itinerary.startDatetime.take(10).takeIf {
            it.length == 10 && it[4] == '-' && it[7] == '-'
        } ?: return null
        val end = itinerary.endDatetime.take(10).takeIf {
            it.length == 10 && it[4] == '-' && it[7] == '-'
        } ?: return null
        if (start > end) return null
        return start to end
    }
}
