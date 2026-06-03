package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TimelineRepository
import io.reactivex.Observable
import retrofit2.HttpException
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
        val maxRetries: Int = 15,
        val intervalMs: Long = 2000,
        val initialDelayMs: Long = 1000  // Initial delay before polling starts
    )

    override fun on(params: Params?) {
        params?.let { p ->
            addObservable {
                // Add initial delay to allow server to process the new segment
                Observable.timer(p.initialDelayMs, TimeUnit.MILLISECONDS)
                    .flatMap {
                        // Sequential polling: each request waits for the previous one to
                        // complete, then waits intervalMs before retrying. Up to maxRetries
                        // total attempts, stopping early once the timeline is generated.
                        repository.fetchTimeline(p.tripHash)
                            .onErrorResumeNext { throwable: Throwable ->
                                // For 4xx client errors, stop polling and propagate error
                                if (throwable is HttpException && throwable.code() in 400..499) {
                                    Observable.error(throwable)
                                } else {
                                    // For other errors (network, 5xx), continue polling
                                    Observable.empty()
                                }
                            }
                            .repeatWhen { completions ->
                                completions
                                    .take((p.maxRetries - 1).toLong())
                                    .concatMap {
                                        Observable.timer(p.intervalMs, TimeUnit.MILLISECONDS)
                                    }
                            }
                            .filter { timeline -> timeline.isTimelineGenerated() }
                            .take(1)
                            .switchIfEmpty(repository.fetchTimeline(p.tripHash))
                    }
                    .onErrorResumeNext { throwable: Throwable ->
                        // For 4xx errors, propagate the error (don't fetch again)
                        if (throwable is HttpException && throwable.code() in 400..499) {
                            Observable.error(throwable)
                        } else {
                            // On other errors, fetch the latest timeline as fallback
                            repository.fetchTimeline(p.tripHash)
                        }
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
