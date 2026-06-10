package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import javax.inject.Inject

class DeleteSegmentUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<ResponseModelBase, DeleteSegmentUseCase.Params>() {

    data class Params(val tripHash: String, val segmentIndex: Int)

    override suspend fun execute(params: Params): ResponseModelBase {
        repository.deleteSegmentAsync(params.tripHash, params.segmentIndex)
        return ResponseModelBase().apply { status = 200 }
    }
}
