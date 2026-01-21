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
     */
    data class Step(
        val step: TimelineStep,
        val order: Int
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
