package com.tripian.trpcore.domain.usecase.timeline

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.domain.model.timeline.RouteCache
import com.tripian.trpcore.domain.model.timeline.RouteCache.toStepRouteInfo
import com.tripian.trpcore.domain.model.timeline.StepRouteInfo
import com.tripian.trpcore.util.MapBoxRouteCalculator
import com.tripian.trpfoundationkit.enums.DirectionProfile
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * GetTimelineStepRoutesUseCase
 * Calculates route information between consecutive waypoints with batch Mapbox Directions requests.
 * Every pair is first routed on foot; pairs whose walking distance reaches
 * [StepRouteInfo.WALKING_THRESHOLD_METERS] are re-routed by car in a second batch request so
 * their distance, duration and shape reflect driving. Straight-line estimates fill in for any
 * pair Mapbox returns no leg for.
 */
class GetTimelineStepRoutesUseCase @Inject constructor() :
    SuspendUseCase<List<StepRouteInfo>, GetTimelineStepRoutesUseCase.Params>() {

    /**
     * @param stepId Timeline step id the waypoint stands for; null for a starting point or a plain coordinate.
     */
    data class Waypoint(
        val coordinate: Coordinate,
        val stepId: Int?
    )

    data class Params(val waypoints: List<Waypoint>) {

        companion object {
            /** Starting point (when located) followed by every located step, in order. */
            fun forSteps(startingPointCoordinate: Coordinate?, steps: List<TimelineStep>): Params {
                val waypoints = mutableListOf<Waypoint>()
                if (startingPointCoordinate != null &&
                    startingPointCoordinate.lat != 0.0 && startingPointCoordinate.lng != 0.0
                ) {
                    waypoints.add(Waypoint(startingPointCoordinate, stepId = null))
                }
                steps.forEach { step ->
                    step.poi?.coordinate?.let { waypoints.add(Waypoint(it, step.id ?: 0)) }
                }
                return Params(waypoints)
            }

            fun forCoordinates(coordinates: List<Coordinate>): Params =
                Params(coordinates.map { Waypoint(it, stepId = null) })
        }
    }

    override suspend fun execute(params: Params): List<StepRouteInfo> {
        val coordinatePairs = buildCoordinatePairs(params.waypoints)
        if (coordinatePairs.isEmpty()) return emptyList()

        if (coordinatePairs.all { RouteCache.contains(it.from, it.to) }) {
            return coordinatePairs.map { pair ->
                RouteCache.get(pair.from, pair.to)!!.toStepRouteInfo(pair.fromStepId, pair.toStepId)
            }
        }

        val points = buildWaypoints(coordinatePairs)
        val walkingLegs = requestLegs(points, DirectionProfile.WALKING)
        val walkingRoutes = coordinatePairs.mapIndexed { index, pair ->
            buildWalkingRouteInfo(pair, walkingLegs?.getOrNull(index))
        }

        val drivingLegs = if (walkingRoutes.any { !it.isWalking }) {
            requestLegs(points, DirectionProfile.AUTOMOBILE)
        } else {
            null
        }

        return coordinatePairs.mapIndexed { index, pair ->
            val walkingRoute = walkingRoutes[index]
            val routeInfo = if (walkingRoute.isWalking) {
                walkingRoute
            } else {
                buildDrivingRouteInfo(walkingRoute, drivingLegs?.getOrNull(index))
            }
            RouteCache.put(pair.from, pair.to, routeInfo)
            routeInfo
        }
    }

    /**
     * @return One leg per consecutive waypoint pair, or null when the request fails or yields no route.
     */
    private suspend fun requestLegs(points: List<Point>, profile: DirectionProfile): List<RouteLeg>? =
        suspendCancellableCoroutine { cont ->
            val calculator = MapBoxRouteCalculator()
            cont.invokeOnCancellation { calculator.cancel() }
            calculator.calculateBatch(points, profile) { response, error ->
                if (!cont.isActive) return@calculateBatch
                val legs = if (error == null) response?.routes()?.firstOrNull()?.legs() else null
                cont.resume(legs)
            }
        }

    private fun buildWaypoints(pairs: List<CoordinatePair>): List<Point> {
        val points = mutableListOf(pairs.first().from.toPoint())
        pairs.forEach { pair ->
            points.add(pair.to.toPoint())
        }
        return points
    }

    private fun buildWalkingRouteInfo(pair: CoordinatePair, leg: RouteLeg?): StepRouteInfo {
        val distance = leg?.distance() ?: calculateStraightLineDistance(pair.from, pair.to)
        val isWalking = distance < StepRouteInfo.WALKING_THRESHOLD_METERS
        return StepRouteInfo(
            distanceMeters = distance,
            durationSeconds = leg?.duration() ?: estimateDuration(distance, isWalking),
            isWalking = isWalking,
            fromStepId = pair.fromStepId,
            toStepId = pair.toStepId,
            encodedGeometry = leg?.encodedGeometry() ?: straightLineGeometry(pair.from, pair.to)
        )
    }

    private fun buildDrivingRouteInfo(walkingRoute: StepRouteInfo, leg: RouteLeg?): StepRouteInfo {
        val distance = leg?.distance() ?: walkingRoute.distanceMeters
        return walkingRoute.copy(
            distanceMeters = distance,
            durationSeconds = leg?.duration() ?: estimateDuration(distance, isWalking = false),
            encodedGeometry = leg?.encodedGeometry() ?: walkingRoute.encodedGeometry
        )
    }

    private fun buildCoordinatePairs(waypoints: List<Waypoint>): List<CoordinatePair> =
        waypoints.zipWithNext { from, to ->
            CoordinatePair(
                from = from.coordinate,
                to = to.coordinate,
                fromStepId = from.stepId,
                toStepId = to.stepId ?: 0
            )
        }

    private fun RouteLeg.encodedGeometry(): String? {
        val points = steps()
            ?.mapNotNull { step -> step.geometry() }
            ?.flatMap { geometry -> LineString.fromPolyline(geometry, Constants.PRECISION_6).coordinates() }
            ?: return null
        return if (points.size > 1) PolylineUtils.encode(points, Constants.PRECISION_6) else null
    }

    private fun straightLineGeometry(from: Coordinate, to: Coordinate): String =
        PolylineUtils.encode(listOf(from.toPoint(), to.toPoint()), Constants.PRECISION_6)

    private fun Coordinate.toPoint(): Point = Point.fromLngLat(lng, lat)

    private fun calculateStraightLineDistance(from: Coordinate, to: Coordinate): Double {
        val earthRadius = 6371000.0
        val lat1 = Math.toRadians(from.lat)
        val lat2 = Math.toRadians(to.lat)
        val deltaLat = Math.toRadians(to.lat - from.lat)
        val deltaLng = Math.toRadians(to.lng - from.lng)
        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    private fun estimateDuration(distanceMeters: Double, isWalking: Boolean): Double {
        val distanceKm = distanceMeters / 1000.0
        val minutesPerKm = if (isWalking) 12.0 else 1.5
        return (distanceKm * minutesPerKm * 60)
    }

    private data class CoordinatePair(
        val from: Coordinate,
        val to: Coordinate,
        val fromStepId: Int?,
        val toStepId: Int
    )
}
