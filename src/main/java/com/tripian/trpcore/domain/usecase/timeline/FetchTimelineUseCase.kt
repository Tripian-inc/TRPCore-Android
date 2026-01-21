package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TimelineRepository
import javax.inject.Inject

/**
 * FetchTimelineUseCase
 * Fetches timeline by trip hash
 */
class FetchTimelineUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<Timeline, FetchTimelineUseCase.Params>() {

    data class Params(
        val tripHash: String
    )

    override fun on(params: Params?) {
        params?.let {
            addObservable {
                repository.fetchTimeline(it.tripHash)
            }
        }
    }
}
