package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.repository.TimelineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the TimelineDate control segment aligned with the host-supplied itinerary range.
 *
 * iOS parity — 3-step optimistic flow:
 *   1. LOCAL: mutate the existing TimelineDate segment in-place
 *   2. UI   : caller re-publishes the timeline + refreshes display
 *   3. API  : fire-and-forget PUT /timeline/{hash} with segmentIndex hint
 *
 * Detection uses BOTH `title == "TimelineDate"` AND `available == false` — the SDK
 * sentinel contract. No polling: TimelineDate carries `doNotGenerate=1`, so the
 * server has nothing to regenerate.
 */
class UpdateDateRangeUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<UpdateDateRangeUseCase.Result, UpdateDateRangeUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val itinerary: ItineraryWithActivities,
        val timeline: Timeline
    )

    /** True iff the TimelineDate segment was mutated — caller must refresh UI. */
    data class Result(val mutated: Boolean)

    override suspend fun execute(params: Params): Result {
        val target = params.itinerary.buildTimelineDateSegment()
            ?: return Result(mutated = false)

        val segments = params.timeline.tripProfile?.segments
            ?: return Result(mutated = false)

        val existingIndex = segments.indexOfFirst { seg ->
            seg.title == "TimelineDate" && !seg.available
        }
        if (existingIndex < 0) return Result(mutated = false)

        val existing = segments[existingIndex]
        if (existing.startDate == target.startDate &&
            existing.endDate == target.endDate
        ) {
            return Result(mutated = false)
        }

        // STEP 1 — Local optimistic update. The caller holds the same Timeline
        // reference and will re-emit it after we return.
        existing.startDate = target.startDate
        existing.endDate = target.endDate

        // STEP 3 — Background PUT. `segmentIndex` tells the server this is an
        // UPDATE on the existing TimelineDate, not a new insertion.
        target.segmentIndex = existingIndex
        fireAndForgetEdit(params.tripHash, target)

        return Result(mutated = true)
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    private fun fireAndForgetEdit(tripHash: String, segment: TimelineSegmentSettings) {
        // Mirrors the legacy Schedulers.io() subscribe — survives the caller
        // coroutine because the TimelineDate sync must outlive its trigger.
        GlobalScope.launch(Dispatchers.IO) {
            try {
                repository.editSegmentAsync(tripHash, segment)
            } catch (error: Throwable) {
                android.util.Log.e("SYNC", "TimelineDate edit failed: ${error.message}")
            }
        }
    }
}
