package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.domain.model.timeline.TransitionInfo
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import com.tripian.trpcore.util.extensions.cityNameKey
import javax.inject.Inject

/**
 * SyncReservedToBookedUseCase
 *
 * Syncs reserved→booked transitions in two sequential phases: deletes the reserved
 * segments (highest index first, so backend array shifts don't invalidate the
 * remaining indices), then creates the booked segments.
 *
 * The booked payload is built by [ItineraryWithActivities.createBookedActivitySegment]
 * so it carries the same `additionalData` (title, image, datetimes, duration, price)
 * as every other booked segment; a partial payload renders as an empty cell.
 * `doNotGenerate` must stay 0 on every segment — the API rejects the request otherwise.
 * iOS Reference: Guide Operation 3 (Reserved → Booked Transition)
 */
class SyncReservedToBookedUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<ResponseModelBase, SyncReservedToBookedUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val transitions: List<TransitionInfo>,
        val cityNameToIdMap: Map<String, Int>,
        val itinerary: ItineraryWithActivities
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
            val cityId = tripItem.cityId?.takeIf { it > 0 }
                ?: tripItem.cityName?.let { name -> params.cityNameToIdMap[name.cityNameKey()] }
                ?: 0

            val segment = params.itinerary.createBookedActivitySegment(tripItem).apply {
                if (this.cityId == null && cityId > 0) this.cityId = cityId
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
