package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.TimelineRepository
import javax.inject.Inject

class UpdateStepTimeUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<TimelineStep, UpdateStepTimeUseCase.Params>() {

    data class Params(val stepId: Int, val startTime: String?, val endTime: String?)

    override suspend fun execute(params: Params): TimelineStep =
        repository.editStepAsync(
            stepId = params.stepId, startTime = params.startTime, endTime = params.endTime
        )
}
