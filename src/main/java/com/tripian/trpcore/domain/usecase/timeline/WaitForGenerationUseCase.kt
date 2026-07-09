package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.TimelineRepository
import kotlinx.coroutines.delay
import retrofit2.HttpException
import javax.inject.Inject

/**
 * WaitForGenerationUseCase
 * Polls until generation completes after a segment is created.
 * 4xx errors abort polling; other failures are treated as transient.
 * When polling exhausts, returns the last successful fetch (or one final fetch).
 */
class WaitForGenerationUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<Timeline, WaitForGenerationUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val maxRetries: Int = 15,
        val intervalMs: Long = 2000,
        val initialDelayMs: Long = 1000
    )

    override suspend fun execute(params: Params): Timeline {
        delay(params.initialDelayMs)

        var lastFetched: Timeline? = null

        repeat(params.maxRetries) { attempt ->
            try {
                val timeline = repository.fetchTimelineAsync(params.tripHash)
                lastFetched = timeline
                if (timeline.isTimelineGenerated()) return timeline
            } catch (e: HttpException) {
                if (e.code() in 400..499) throw e
            } catch (_: Throwable) {
            }
            if (attempt < params.maxRetries - 1) {
                delay(params.intervalMs)
            }
        }

        return lastFetched ?: repository.fetchTimelineAsync(params.tripHash)
    }

    /**
     * Checks if the timeline is fully generated based on segment rules:
     * - If segmentType == "itinerary" AND title != "Empty" → check generatedStatus != 0
     * - For other segment types (booked_activity, etc.) → consider generated (no check needed)
     */
    private fun Timeline.isTimelineGenerated(): Boolean {
        val segments = this.tripProfile?.segments ?: return true
        val plans = this.plans ?: return true

        return segments.withIndex().all { (index, segment) ->
            val segmentType = segment.segmentType
            val title = segment.title
            if (segmentType == SegmentType.ITINERARY && title != "Empty") {
                val planGeneratedStatus = plans.getOrNull(index)?.generatedStatus ?: 0
                planGeneratedStatus != 0
            } else {
                true
            }
        }
    }
}
