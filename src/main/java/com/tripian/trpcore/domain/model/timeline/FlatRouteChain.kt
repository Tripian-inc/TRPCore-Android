package com.tripian.trpcore.domain.model.timeline

import com.tripian.one.api.pois.model.Coordinate
import java.util.Locale

/**
 * A routable row of the flat timeline.
 *
 * @param id null for the starting point, the step id for plan and manual steps,
 *   [FlatRouteChain.segmentWaypointId] for booked/reserved activity segments.
 */
data class RouteWaypoint(
    val coordinate: Coordinate,
    val id: Int?
)

/**
 * One run of routable rows of a city group of the flat timeline, in list order. A timed
 * row without a location ends the run: the rows before it and the rows after it are
 * routed as separate chains, so no leg is drawn across a place that cannot be placed.
 * The starting point belongs to the first run that has a located row. [key] identifies
 * the chain by its waypoint ids and coordinates, so rows that only changed time reuse
 * the legs already calculated for the same sequence.
 */
data class FlatRouteChain(
    val cityId: Int?,
    val waypoints: List<RouteWaypoint>
) {

    val key: String = waypoints.joinToString(separator = ">", prefix = "$cityId|") { waypoint ->
        String.format(
            Locale.US,
            "%s@%.5f,%.5f",
            waypoint.id?.toString() ?: "start",
            waypoint.coordinate.lat,
            waypoint.coordinate.lng
        )
    }

    val isRoutable: Boolean
        get() = waypoints.size > 1

    companion object {
        /** Negative, so segment waypoints never collide with step ids. */
        fun segmentWaypointId(segmentIndex: Int): Int = -(segmentIndex + 1)

        /**
         * The chains of every city group in list order. Rows are split into runs at each
         * timed row without a location; runs with fewer than two waypoints are dropped.
         */
        fun collect(items: List<TimelineDisplayItem>): List<FlatRouteChain> {
            val chains = mutableListOf<FlatRouteChain>()
            var cityId: Int? = null
            var run = mutableListOf<RouteWaypoint>()

            fun closeRun() {
                if (run.size > 1) chains.add(FlatRouteChain(cityId, run.toList()))
                run = mutableListOf()
            }

            items.forEach { item ->
                if (item.city?.id != cityId && run.isNotEmpty()) {
                    closeRun()
                }
                cityId = item.city?.id
                val waypoint = item.routeWaypoint()
                when {
                    waypoint != null -> run.add(waypoint)
                    item.breaksRouteChain && run.any { it.id != null } -> closeRun()
                }
            }
            closeRun()
            return chains
        }
    }
}

/** A timed row that has no coordinate to route through, so legs must not cross it. */
private val TimelineDisplayItem.breaksRouteChain: Boolean
    get() = when (this) {
        is TimelineDisplayItem.BookedActivity, is TimelineDisplayItem.ManualPoi, is TimelineDisplayItem.PlanStep -> true
        else -> false
    }

/** The waypoint this row contributes to its city's route chain, or null when it is not routed. */
fun TimelineDisplayItem.routeWaypoint(): RouteWaypoint? = when (this) {
    is TimelineDisplayItem.StartingPoint ->
        coordinate?.takeUnless { it.isMissingOrZero() }?.let { RouteWaypoint(it, null) }

    is TimelineDisplayItem.PlanStep ->
        coordinate?.let { RouteWaypoint(it, step.id) }

    is TimelineDisplayItem.ManualPoi ->
        if (isNoLocation) {
            null
        } else {
            step.poi?.coordinate?.takeUnless { it.isMissingOrZero() }?.let { RouteWaypoint(it, step.id) }
        }

    is TimelineDisplayItem.BookedActivity -> {
        val index = segmentIndex
        if (isNoLocation || index == null) {
            null
        } else {
            (segment.additionalData?.coordinate ?: segment.coordinate)
                ?.takeUnless { it.isMissingOrZero() }
                ?.let { RouteWaypoint(it, FlatRouteChain.segmentWaypointId(index)) }
        }
    }

    else -> null
}
