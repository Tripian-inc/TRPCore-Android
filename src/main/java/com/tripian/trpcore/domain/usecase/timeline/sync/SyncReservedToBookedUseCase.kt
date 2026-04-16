package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.timeline.TransitionInfo
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import io.reactivex.Completable
import javax.inject.Inject

/**
 * SyncReservedToBookedUseCase
 *
 * Transition'ları senkronize et: reserved'ları sil, booked'ları oluştur
 * iOS Guide Operation 3: Reserved → Booked Transition
 *
 * Two-phase sequential operation:
 * PHASE 1: Delete reserved segments (highest index first!)
 * PHASE 2: Create booked segments
 *
 * CRITICAL: Index ordering - must delete highest-index-first to prevent array shifting
 */
class SyncReservedToBookedUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<ResponseModelBase, SyncReservedToBookedUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val transitions: List<TransitionInfo>,
        val cityNameToIdMap: Map<String, Int>
    )

    override fun on(params: Params?) {
        params?.let {
            addObservable {
                // PHASE 1: Delete reserved segments (CRITICAL: highest index first!)
                val sortedTransitions = it.transitions.sortedByDescending { t -> t.segmentIndex }

                val deleteOperations = sortedTransitions.map { transition ->
                    repository.deleteSegment(it.tripHash, transition.segmentIndex)
                        .onErrorResumeNext { error: Throwable ->
                            android.util.Log.e(
                                "SYNC",
                                "Delete reserved (index ${transition.segmentIndex}) failed: ${error.message}"
                            )
                            Completable.complete() // Continue chain even on error
                        }
                }

                // PHASE 2: Create booked segments
                val createOperations = sortedTransitions.map { transition ->
                    val tripItem = transition.tripItem

                    // cityId'yi resolve et
                    val cityId = tripItem.cityName?.let { name ->
                        it.cityNameToIdMap[name]
                    } ?: 0

                    val segment = TimelineSegmentSettings().apply {
                        this.segmentType = SegmentType.BOOKED_ACTIVITY
                        this.title = tripItem.title
                        this.startDate = tripItem.startDatetime
                        this.endDate = tripItem.endDatetime
                        this.cityId = if (cityId > 0) cityId else null
                        this.coordinate = Coordinate().apply {
                            lat = tripItem.coordinate.lat
                            lng = tripItem.coordinate.lng
                        }
                        this.available = false
                        this.distinctPlan = true
                        this.doNotGenerate = 1
                        this.adults = tripItem.adultCount
                        this.children = tripItem.childCount

                        this.additionalData = TimelineSegmentAdditionalData().apply {
                            this.activityId = tripItem.activityId
                            this.bookingId = tripItem.bookingId
                            this.price = tripItem.price?.value
                            this.coordinate = this@apply.coordinate
                        }
                    }

                    repository.editSegment(it.tripHash, segment)
                        .onErrorResumeNext { error: Throwable ->
                            android.util.Log.e(
                                "SYNC",
                                "Create booked (activityId ${tripItem.activityId}) failed: ${error.message}"
                            )
                            Completable.complete() // Continue chain
                        }
                }

                // Sequential execution: delete all → create all
                Completable.concat(deleteOperations + createOperations)
                    .toSingleDefault(ResponseModelBase())
                    .toObservable()
            }
        }
    }
}
