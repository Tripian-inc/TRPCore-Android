package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.domain.model.itinerary.SegmentDestinationItem
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import javax.inject.Inject

/**
 * RemoveSegmentsForDeletedCitiesUseCase
 *
 * Artık itinerary'de olmayan şehirlerin segmentlerini sil
 * iOS Guide Operation 5: Remove Segments for Deleted Cities
 *
 * Sequential deletion (highest index first!)
 * CRITICAL: TimelineDate segment is skipped (special type)
 */
class RemoveSegmentsForDeletedCitiesUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<ResponseModelBase, RemoveSegmentsForDeletedCitiesUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val timeline: Timeline,
        val currentDestinations: List<SegmentDestinationItem>
    )

    override suspend fun execute(params: Params): ResponseModelBase {
        val currentCityIds = params.currentDestinations
            .mapNotNull { dest -> dest.cityId }
            .filter { id -> id > 0 }
            .toSet()

        val segments = params.timeline.tripProfile?.segments ?: emptyList()

        val segmentsToDelete = mutableListOf<Int>()
        segments.forEachIndexed { index, segment ->
            if (segment.segmentType == "TimelineDate") return@forEachIndexed
            val cityId = segment.cityId
            if (cityId == null || cityId <= 0) {
                segmentsToDelete.add(index)
                return@forEachIndexed
            }
            if (cityId !in currentCityIds) {
                segmentsToDelete.add(index)
            }
        }

        if (segmentsToDelete.isEmpty()) return ResponseModelBase()

        // CRITICAL: En yüksek index'ten başla (array shifting problemi olmasın)
        for (index in segmentsToDelete.sortedDescending()) {
            try {
                repository.deleteSegmentAsync(params.tripHash, index)
            } catch (error: Throwable) {
                android.util.Log.e(
                    "SYNC",
                    "Delete city segment (index $index) failed: ${error.message}"
                )
            }
        }
        return ResponseModelBase()
    }
}
