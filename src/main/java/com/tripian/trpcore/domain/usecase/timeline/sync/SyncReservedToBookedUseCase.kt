package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.domain.model.timeline.TransitionInfo
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import javax.inject.Inject

/**
 * SyncReservedToBookedUseCase
 *
 * Syncs reserved→booked transitions in two sequential phases: deletes the reserved
 * segments (highest index first, so backend array shifts don't invalidate the
 * remaining indices), then creates the booked segments.
 * iOS Reference: Guide Operation 3 (Reserved → Booked Transition)
 */
class SyncReservedToBookedUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<ResponseModelBase, SyncReservedToBookedUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val transitions: List<TransitionInfo>,
        val cityNameToIdMap: Map<String, Int>
    )

    override suspend fun execute(params: Params): ResponseModelBase {
        val sortedTransitions = params.transitions.sortedByDescending { t -> t.segmentIndex }

        for (transition in sortedTransitions) {
            try {
                repository.deleteSegmentAsync(params.tripHash, transition.segmentIndex)
            } catch (error: Throwable) {
                android.util.Log.e(
                    "SYNC",
                    "Delete reserved (index ${transition.segmentIndex}) failed: ${error.message}"
                )
            }
        }

        for (transition in sortedTransitions) {
            val tripItem = transition.tripItem
            val cityId = tripItem.cityName?.let { name ->
                params.cityNameToIdMap[name]
            } ?: 0

            val segment = TimelineSegmentSettings().apply {
                this.segmentType = SegmentType.BOOKED_ACTIVITY
                this.title = tripItem.title
                this.startDate = tripItem.startDatetime
                this.endDate = tripItem.endDatetime
                this.cityId = if (cityId > 0) cityId else null
                this.coordinate = tripItem.coordinate?.let {
                    Coordinate().apply {
                        lat = it.lat
                        lng = it.lng
                    }
                }
                this.available = false
                this.distinctPlan = true
                this.adults = tripItem.adultCount
                this.children = tripItem.childCount

                this.additionalData = TimelineSegmentAdditionalData().apply {
                    this.activityId = tripItem.activityId
                    this.bookingId = tripItem.bookingId
                    this.price = tripItem.price?.value
                    this.coordinate = this@apply.coordinate
                    this.isNoLocation = (tripItem.coordinate == null)
                }
            }

            try {
                repository.editSegmentAsync(params.tripHash, segment)
            } catch (error: Throwable) {
                android.util.Log.e(
                    "SYNC",
                    "Create booked (activityId ${tripItem.activityId}) failed: ${error.message}"
                )
            }
        }
        return ResponseModelBase()
    }
}
