package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TimelineRepository
import javax.inject.Inject

/**
 * UpdateStepTimeUseCase
 * Updates the start and end time of a timeline step
 */
class UpdateStepTimeUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<TimelineStep, UpdateStepTimeUseCase.Params>() {

    data class Params(
        val stepId: Int,
        val startTime: String?,
        val endTime: String?
    )

    override fun on(params: Params?) {
        params?.let {
            addObservable {
                repository.editStep(
                    stepId = it.stepId,
                    startTime = it.startTime,
                    endTime = it.endTime
                )
            }
        }
    }
}
