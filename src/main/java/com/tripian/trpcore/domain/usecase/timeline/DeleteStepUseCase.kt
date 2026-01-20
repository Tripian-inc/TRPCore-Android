package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import javax.inject.Inject

/**
 * DeleteStepUseCase
 * Timeline'dan step siler
 */
class DeleteStepUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<ResponseModelBase, DeleteStepUseCase.Params>() {

    data class Params(
        val stepId: Int
    )

    override fun on(params: Params?) {
        params?.let { p ->
            addObservable {
                repository.deleteStep(p.stepId)
                    .toSingleDefault(ResponseModelBase().apply { status = 200 })
                    .toObservable()
            }
        }
    }
}
