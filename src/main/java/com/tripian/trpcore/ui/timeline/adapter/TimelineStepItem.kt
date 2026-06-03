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
         * Captured at construction so the inner adapter's DiffUtil reacts when
         * the VM mutates `step.startDateTimes` / `step.endDateTimes` in place
         * (see `ACTimelineVM.applyLocalStepTimeUpdate`). Without these
         * snapshots the `step` reference stays the same across rebuilds, the
         * data class equals returns true and the StepPoiVH / StepActivityVH
         * row never re-renders the updated time.
         */
        val startDateTimeSnapshot: String? = null,
        val endDateTimeSnapshot: String? = null
    ) : TimelineStepItem() {
        // Unique identifier for DiffUtil
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
        // Unique identifier for DiffUtil
        val id: String get() = "route_${routeInfo.fromStepId ?: "start"}_${routeInfo.toStepId}"
    }
}
