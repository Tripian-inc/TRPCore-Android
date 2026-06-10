package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import javax.inject.Inject

class DeleteStepUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<ResponseModelBase, DeleteStepUseCase.Params>() {

    data class Params(val stepId: Int)

    override suspend fun execute(params: Params): ResponseModelBase {
        repository.deleteStepAsync(params.stepId)
        return ResponseModelBase().apply { status = 200 }
    }
}
