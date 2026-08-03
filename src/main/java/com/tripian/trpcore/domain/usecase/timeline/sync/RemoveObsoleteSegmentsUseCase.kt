package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import javax.inject.Inject

/**
 * RemoveObsoleteSegmentsUseCase
 *
 * The single segment-deletion entry point of the post-sync sweep. The caller
 * computes every removal reason against one timeline snapshot and hands the union
 * over; this deletes them highest-index-first so the backend's array shifts never
 * invalidate a pending index. Separate cascades would each hold indices from the
 * same stale snapshot and delete the wrong segments.
 *
 * iOS Reference: `reconcileSegmentsWithItinerary`
 */
class RemoveObsoleteSegmentsUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<ResponseModelBase, RemoveObsoleteSegmentsUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val segmentIndices: Set<Int>
    )

    override suspend fun execute(params: Params): ResponseModelBase {
        for (index in params.segmentIndices.sortedDescending()) {
            try {
                repository.deleteSegmentAsync(params.tripHash, index)
            } catch (error: Throwable) {
                android.util.Log.e(
                    "SYNC",
                    "Obsolete segment delete (index $index) failed: ${error.message}"
                )
            }
        }
        return ResponseModelBase()
    }
}
