package com.tripian.trpcore.ui.timeline.adapter

import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.domain.model.timeline.StepRouteInfo
import java.io.Serializable

/**
 * TimelineStepItem
 * Sealed class for RecyclerView items within Recommendations
 * Supports both Step items and Route Separator items for interleaved display
 */
sealed class TimelineStepItem : Serializable {

    /**
     * Step item - represents a POI or Activity step
     *
     * @param step The TimelineStep data
     * @param order Display order number (1-based)
     * @param hasConflict Whether this step has a time conflict with another item
     * @param showTimeOverlapText Whether to show "Time Overlap" text next to time
     * @param isAvailabilityExpired Whether the activity step's booked time is no longer
     *   offered (set by the post-load schedule-bulk sweep). Takes precedence over
     *   conflict styling when true.
     */
    data class Step(
        val step: TimelineStep,
        val order: Int,
        val hasConflict: Boolean = false,
        val showTimeOverlapText: Boolean = false,
        val isAvailabilityExpired: Boolean = false,
        /**
         * Snapshots of `step.startDateTimes`/`endDateTimes` captured at construction
         * so DiffUtil detects in-place time mutations on the shared `step` reference
         * (see `ACTimelineVM.applyLocalStepTimeUpdate`).
         */
        val startDateTimeSnapshot: String? = null,
        val endDateTimeSnapshot: String? = null,
        /**
         * Snapshot of the activity price captured at construction, so DiffUtil
         * detects the schedule sweep rewriting it on the shared `step` reference.
         */
        val priceSnapshot: Double? = null
    ) : TimelineStepItem() {
        val id: String get() = "step_${step.id}"
    }

    /**
     * Route Separator item - shows distance/duration between steps
     *
     * @param routeInfo Route information containing distance and duration
     */
    data class RouteSeparator(
        val routeInfo: StepRouteInfo
    ) : TimelineStepItem() {
        val id: String get() = "route_${routeInfo.fromStepId ?: "start"}_${routeInfo.toStepId}"
    }
}
