package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import javax.inject.Inject

/**
 * DeleteSegmentUseCase
 * Timeline'dan segment siler
 */
class DeleteSegmentUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<ResponseModelBase, DeleteSegmentUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val segmentIndex: Int
    )

    override fun on(params: Params?) {
        params?.let { p ->
            addObservable {
                repository.deleteSegment(p.tripHash, p.segmentIndex)
                    .toSingleDefault(ResponseModelBase().apply { status = 200 })
                    .toObservable()
            }
        }
    }
}
