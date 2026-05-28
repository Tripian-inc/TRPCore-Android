package com.tripian.trpcore.domain.usecase.timeline

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.Point
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.timeline.RouteCache
import com.tripian.trpcore.domain.model.timeline.RouteCache.toStepRouteInfo
import com.tripian.trpcore.domain.model.timeline.StepRouteInfo
import com.tripian.trpcore.util.MapBoxRouteCalculator
import io.reactivex.Observable
import javax.inject.Inject

/**
 * GetTimelineStepRoutesUseCase
 * Calculates route information between steps using a single batch Mapbox Directions request.
 * Mirrors the iOS TRPRouteCalculator pattern: one waypoint list → one API call → response.legs[i]
 * maps to step pair i. Per-leg `isWalking` flag is derived from leg distance.
 *
 * All Mapbox Directions calls go through [MapBoxRouteCalculator.calculateBatch] — the single
 * Mapbox SDK entry point for the project's routing flows.
 */
class GetTimelineStepRoutesUseCase @Inject constructor() :
    BaseUseCase<List<StepRouteInfo>, GetTimelineStepRoutesUseCase.Params>() {

    data class Params(
        val startingPointCoordinate: Coordinate?,
        val steps: List<TimelineStep>
    )

    override fun on(params: Params?) {
        params?.let { p ->
            addObservable {
                Observable.create { emitter ->
                    calculateRoutes(p.startingPointCoordinate, p.steps) { routeInfoList ->
                        if (!emitter.isDisposed) {
                            emitter.onNext(routeInfoList)
                            emitter.onComplete()
                        }
                    }
                }
            }
        }
    }

    private fun calculateRoutes(
        startingPoint: Coordinate?,
        steps: List<TimelineStep>,
        onComplete: (List<StepRouteInfo>) -> Unit
    ) {
        val coordinatePairs = buildCoordinatePairs(startingPoint, steps)

        if (coordinatePairs.isEmpty()) {
            onComplete(emptyList())
            return
        }

        // All cached → no API call
        if (coordinatePairs.all { RouteCache.contains(it.from, it.to) }) {
            val cached = coordinatePairs.map { pair ->
                RouteCache.get(pair.from, pair.to)!!.toStepRouteInfo(pair.fromStepId, pair.toStepId)
            }
            onComplete(cached)
            return
        }

        // Build flat waypoint list: [pairs[0].from, pairs[0].to, pairs[1].to, ..., pairs[n-1].to]
        val points = mutableListOf<Point>()
        points.add(Point.fromLngLat(coordinatePairs.first().from.lng, coordinatePairs.first().from.lat))
        coordinatePairs.forEach { pair ->
            points.add(Point.fromLngLat(pair.to.lng, pair.to.lat))
        }

        MapBoxRouteCalculator().calculateBatch(points) { response, error ->
            if (error != null) {
                onComplete(buildFallbackEstimates(coordinatePairs))
                return@calculateBatch
            }
            val legs = response?.routes()?.firstOrNull()?.legs()
            if (legs == null) {
                onComplete(buildFallbackEstimates(coordinatePairs))
                return@calculateBatch
            }
            onComplete(mapLegsToRouteInfo(legs, coordinatePairs))
        }
    }

    /**
     * Map response legs to coordinate pairs.
     * Falls back to Haversine estimate per pair when leg count mismatches or values are null.
     */
    private fun mapLegsToRouteInfo(
        legs: List<RouteLeg>,
        pairs: List<CoordinatePair>
    ): List<StepRouteInfo> {
        return pairs.mapIndexed { index, pair ->
            val leg = legs.getOrNull(index)
            val straightLineDistance = calculateStraightLineDistance(pair.from, pair.to)
            val distance = leg?.distance() ?: straightLineDistance
            val isWalking = distance < StepRouteInfo.WALKING_THRESHOLD_METERS
            val duration = leg?.duration() ?: estimateDuration(distance, isWalking)
            val routeInfo = StepRouteInfo(
                distanceMeters = distance,
                durationSeconds = duration,
                isWalking = isWalking,
                fromStepId = pair.fromStepId,
                toStepId = pair.toStepId
            )
            RouteCache.put(pair.from, pair.to, routeInfo)
            routeInfo
        }
    }

    private fun buildCoordinatePairs(
        startingPoint: Coordinate?,
        steps: List<TimelineStep>
    ): List<CoordinatePair> {
        val pairs = mutableListOf<CoordinatePair>()

        val stepCoordinates = steps.mapNotNull { step ->
            step.poi?.coordinate?.let { coord ->
                StepCoordinate(step.id ?: 0, coord)
            }
        }

        if (stepCoordinates.isEmpty()) return pairs

        if (startingPoint != null && startingPoint.lat != 0.0 && startingPoint.lng != 0.0) {
            pairs.add(
                CoordinatePair(
                    from = startingPoint,
                    to = stepCoordinates.first().coordinate,
                    fromStepId = null,
                    toStepId = stepCoordinates.first().stepId
                )
            )
        }

        for (i in 0 until stepCoordinates.size - 1) {
            pairs.add(
                CoordinatePair(
                    from = stepCoordinates[i].coordinate,
                    to = stepCoordinates[i + 1].coordinate,
                    fromStepId = stepCoordinates[i].stepId,
                    toStepId = stepCoordinates[i + 1].stepId
                )
            )
        }

        return pairs
    }

    private fun buildFallbackEstimates(pairs: List<CoordinatePair>): List<StepRouteInfo> {
        return pairs.map { pair ->
            val distance = calculateStraightLineDistance(pair.from, pair.to)
            val isWalking = distance < StepRouteInfo.WALKING_THRESHOLD_METERS
            val routeInfo = StepRouteInfo(
                distanceMeters = distance,
                durationSeconds = estimateDuration(distance, isWalking),
                isWalking = isWalking,
                fromStepId = pair.fromStepId,
                toStepId = pair.toStepId
            )
            RouteCache.put(pair.from, pair.to, routeInfo)
            routeInfo
        }
    }

    private fun calculateStraightLineDistance(from: Coordinate, to: Coordinate): Double {
        val earthRadius = 6371000.0 // meters

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
        return (distanceKm * minutesPerKm * 60) // Return seconds
    }

    private data class StepCoordinate(
        val stepId: Int,
        val coordinate: Coordinate
    )

    private data class CoordinatePair(
        val from: Coordinate,
        val to: Coordinate,
        val fromStepId: Int?,
        val toStepId: Int
    )
}
