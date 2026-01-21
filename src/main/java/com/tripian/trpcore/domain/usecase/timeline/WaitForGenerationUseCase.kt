package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TimelineRepository
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * WaitForGenerationUseCase
 * Polls until generation completes after a segment is created
 */
class WaitForGenerationUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<Timeline, WaitForGenerationUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val maxRetries: Int = 30,
        val intervalMs: Long = 2000,
        val initialDelayMs: Long = 1000  // Initial delay before polling starts
    )

    override fun on(params: Params?) {
        params?.let { p ->
            addObservable {
                // Add initial delay to allow server to process the new segment
                Observable.timer(p.initialDelayMs, TimeUnit.MILLISECONDS)
                    .flatMap {
                        Observable.interval(0, p.intervalMs, TimeUnit.MILLISECONDS)
                            .take(p.maxRetries.toLong())
                            .flatMap {
                                repository.fetchTimeline(p.tripHash)
                                    .onErrorResumeNext(Observable.empty())
                            }
                            .filter { timeline -> timeline.isTimelineGenerated() }
                            .take(1)
                    }
                    .timeout(p.initialDelayMs + (p.maxRetries * p.intervalMs), TimeUnit.MILLISECONDS)
                    .onErrorResumeNext { throwable: Throwable ->
                        // On timeout, fetch the latest timeline
                        repository.fetchTimeline(p.tripHash)
                    }
            }
        }
    }

    /**
     * Checks if the timeline is fully generated based on segment rules:
     * - If segmentType == "itinerary" AND title != "Empty" → check generatedStatus != 0
     * - For other segment types (booked_activity, etc.) → consider generated (no check needed)
     *
     * NOTE: generatedStatus in tripProfile.segments always stays 0.
     * We need to check plans[index].generatedStatus instead.
     */
    private fun Timeline.isTimelineGenerated(): Boolean {
        val segments = this.tripProfile?.segments ?: return true
        val plans = this.plans ?: return true

        return segments.withIndex().all { (index, segment) ->
            val segmentType = segment.segmentType
            val title = segment.title

            // Only check generatedStatus for itinerary segments that are not "Empty"
            if (segmentType == SegmentType.ITINERARY && title != "Empty") {
                // Get generatedStatus from corresponding plan (same index)
                val planGeneratedStatus = plans.getOrNull(index)?.generatedStatus ?: 0
                planGeneratedStatus != 0
            } else {
                // For booked_activity, reserved_activity, or Empty segments → always considered generated
                true
            }
        }
    }
}
