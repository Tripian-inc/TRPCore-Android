package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.TimelineRepository
import javax.inject.Inject

class FetchTimelineUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<Timeline, FetchTimelineUseCase.Params>() {

    data class Params(val tripHash: String)

    override suspend fun execute(params: Params): Timeline =
        repository.fetchTimelineAsync(params.tripHash)
}
