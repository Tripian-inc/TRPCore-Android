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
 * The routable rows of one city group of the flat timeline in list order: the
 * starting point when it is located, then every located non-flexible row.
 * [key] identifies the chain by its waypoint ids and coordinates, so rows that only
 * changed time reuse the legs already calculated for the same sequence.
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

        /** One chain per city group in list order; rows without a coordinate are skipped. */
        fun collect(items: List<TimelineDisplayItem>): List<FlatRouteChain> {
            val byCity = linkedMapOf<Int?, MutableList<RouteWaypoint>>()
            items.forEach { item ->
                val waypoint = item.routeWaypoint() ?: return@forEach
                byCity.getOrPut(item.city?.id) { mutableListOf() }.add(waypoint)
            }
            return byCity.map { (cityId, waypoints) -> FlatRouteChain(cityId, waypoints) }
        }
    }
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
