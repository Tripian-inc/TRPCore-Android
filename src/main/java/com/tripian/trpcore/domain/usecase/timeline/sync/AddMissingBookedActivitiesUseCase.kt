package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import io.reactivex.Completable
import javax.inject.Inject

/**
 * Adds booked_activity segments for tripItems not yet present on the timeline.
 * Delegates payload construction to ItineraryWithActivities.createBookedActivitySegment
 * so initial-create and sync paths emit identical segments.
 */
class AddMissingBookedActivitiesUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<ResponseModelBase, AddMissingBookedActivitiesUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val itinerary: ItineraryWithActivities,
        val timeline: Timeline
    )

    override fun on(params: Params?) {
        params?.let {
            addObservable {
                val existingActivityIds = it.timeline.tripProfile?.segments
                    ?.mapNotNull { segment -> segment.additionalData?.activityId }
                    ?.toSet()
                    ?: emptySet()

                val missingItems = it.itinerary.tripItems
                    ?.filter { item -> item.activityId != null && item.activityId !in existingActivityIds }
                    ?: emptyList()

                if (missingItems.isEmpty()) {
                    return@addObservable io.reactivex.Observable.just(ResponseModelBase())
                }

                val createOperations = missingItems.map { tripItem ->
                    val segment = it.itinerary.createBookedActivitySegment(tripItem)
                    repository.editSegment(it.tripHash, segment)
                        .onErrorResumeNext { error: Throwable ->
                            android.util.Log.e("SYNC", "Add booked failed: ${error.message}")
                            Completable.complete()
                        }
                }

                Completable.concat(createOperations)
                    .toSingleDefault(ResponseModelBase())
                    .toObservable()
            }
        }
    }
}
