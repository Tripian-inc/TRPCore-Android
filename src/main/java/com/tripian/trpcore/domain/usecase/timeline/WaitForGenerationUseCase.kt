package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.TimelineRepository
import kotlinx.coroutines.delay
import retrofit2.HttpException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * WaitForGenerationUseCase
 * Polls until generation completes after a segment is created.
 * 4xx errors abort polling; other failures are treated as transient until [Params.maxFailures] is reached.
 * When polling exhausts, returns the last successful fetch (or one final fetch).
 */
class WaitForGenerationUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<Timeline, WaitForGenerationUseCase.Params>() {

    /**
     * @param maxRetries number of fetch attempts.
     * @param maxFailures limit on non-4xx failures; the attempt whose failure count reaches it rethrows its error.
     * @param isGenerated terminal condition; null uses the all-itinerary-segments-generated rule.
     * @param onAttemptCompleted called with the 1-based attempt number after every attempt that does not end polling.
     */
    data class Params(
        val tripHash: String,
        val maxRetries: Int = 15,
        val intervalMs: Long = 2000,
        val initialDelayMs: Long = 1000,
        val maxFailures: Int = Int.MAX_VALUE,
        val isGenerated: ((Timeline) -> Boolean)? = null,
        val onAttemptCompleted: (suspend (attempt: Int) -> Unit)? = null
    )

    override suspend fun execute(params: Params): Timeline =
        poll(params) { repository.fetchTimelineAsync(params.tripHash) }

    companion object {

        /**
         * Runs the polling loop of [Params] against [fetch]; exposed so callers can drive it with a fake fetch.
         */
        internal suspend fun poll(params: Params, fetch: suspend () -> Timeline): Timeline {
            val isGenerated = params.isGenerated ?: { timeline: Timeline -> timeline.isTimelineGenerated() }
            delay(params.initialDelayMs)

            var lastFetched: Timeline? = null
            var failures = 0

            for (attempt in 1..params.maxRetries) {
                try {
                    val timeline = fetch()
                    lastFetched = timeline
                    if (isGenerated(timeline)) return timeline
                } catch (e: CancellationException) {
                    throw e
                } catch (e: HttpException) {
                    if (e.code() in 400..499) throw e
                    failures++
                    if (failures >= params.maxFailures) throw e
                } catch (e: Throwable) {
                    failures++
                    if (failures >= params.maxFailures) throw e
                }
                params.onAttemptCompleted?.invoke(attempt)
                if (attempt < params.maxRetries) {
                    delay(params.intervalMs)
                }
            }

            return lastFetched ?: fetch()
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
}
