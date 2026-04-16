package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.itinerary.SegmentDestinationItem
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import io.reactivex.Completable
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
) : BaseUseCase<ResponseModelBase, RemoveSegmentsForDeletedCitiesUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val timeline: Timeline,
        val currentDestinations: List<SegmentDestinationItem>
    )

    override fun on(params: Params?) {
        params?.let {
            addObservable {
                // Mevcut cityId'leri Set'e al (valid olan - > 0)
                val currentCityIds = it.currentDestinations
                    .mapNotNull { dest -> dest.cityId }
                    .filter { id -> id > 0 }
                    .toSet()

                val segments = it.timeline.tripProfile?.segments ?: emptyList()

                // Silinecek segmentleri bul (TimelineDate hariç)
                val segmentsToDelete = mutableListOf<Int>()

                segments.forEachIndexed { index, segment ->
                    // TimelineDate'i atla (special segment type)
                    if (segment.segmentType == "TimelineDate") {
                        return@forEachIndexed
                    }

                    val cityId = segment.cityId

                    // cityId yok veya geçersiz (<= 0)
                    if (cityId == null || cityId <= 0) {
                        segmentsToDelete.add(index)
                        return@forEachIndexed
                    }

                    // cityId mevcut destinations'ta yok
                    if (cityId !in currentCityIds) {
                        segmentsToDelete.add(index)
                    }
                }

                if (segmentsToDelete.isEmpty()) {
                    return@addObservable io.reactivex.Observable.just(ResponseModelBase())
                }

                // CRITICAL: En yüksek index'ten başla (array shifting problemi olmasın)
                val deleteOperations = segmentsToDelete
                    .sortedDescending() // Highest first!
                    .map { index ->
                        repository.deleteSegment(it.tripHash, index)
                            .onErrorResumeNext { error: Throwable ->
                                android.util.Log.e(
                                    "SYNC",
                                    "Delete city segment (index $index) failed: ${error.message}"
                                )
                                Completable.complete() // Continue chain
                            }
                    }

                Completable.concat(deleteOperations)
                    .toSingleDefault(ResponseModelBase())
                    .toObservable()
            }
        }
    }
}
