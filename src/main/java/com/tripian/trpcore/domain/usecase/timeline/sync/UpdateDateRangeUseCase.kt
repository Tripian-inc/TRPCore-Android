package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import io.reactivex.Completable
import javax.inject.Inject

/**
 * Keeps the TimelineDate control segment aligned with the host-supplied itinerary range.
 * - Found and matches itinerary  → no-op.
 * - Found and differs            → in-place UPDATE.
 * - Missing                      → CREATE via ItineraryWithActivities.buildTimelineDateSegment.
 */
class UpdateDateRangeUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<ResponseModelBase, UpdateDateRangeUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val itinerary: ItineraryWithActivities,
        val timeline: Timeline
    )

    override fun on(params: Params?) {
        params?.let {
            addObservable {
                val target = it.itinerary.buildTimelineDateSegment()
                    ?: return@addObservable io.reactivex.Observable.just(ResponseModelBase())

                val existing = it.timeline.tripProfile?.segments
                    ?.find { segment -> segment.title == "TimelineDate" }

                if (existing != null &&
                    existing.startDate == target.startDate &&
                    existing.endDate == target.endDate
                ) {
                    return@addObservable io.reactivex.Observable.just(ResponseModelBase())
                }

                editTimelineDateSegment(it.tripHash, target)
            }
        }
    }

    private fun editTimelineDateSegment(
        tripHash: String,
        segment: TimelineSegmentSettings
    ): io.reactivex.Observable<ResponseModelBase> {
        return repository.editSegment(tripHash, segment)
            .onErrorResumeNext { error: Throwable ->
                android.util.Log.e("SYNC", "TimelineDate edit failed: ${error.message}")
                Completable.complete()
            }
            .toSingleDefault(ResponseModelBase())
            .toObservable()
    }
}
