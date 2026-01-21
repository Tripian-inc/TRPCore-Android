package com.tripian.trpcore.domain.usecase.timeline

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.timeline.RouteCache
import com.tripian.trpcore.domain.model.timeline.RouteCache.toStepRouteInfo
import com.tripian.trpcore.domain.model.timeline.StepRouteInfo
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

/**
 * GetTimelineStepRoutesUseCase
 * Calculates route information between steps in a Recommendations segment
 * Returns a list of StepRouteInfo for display in the UI
 *
 * Uses dynamic routing profile:
 * - < 1.8km: Walking profile
 * - >= 1.8km: Driving profile
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

    /**
     * Calculate routes between all consecutive points
     * Starting point → Step 1 → Step 2 → ... → Step N
     */
    private fun calculateRoutes(
        startingPoint: Coordinate?,
        steps: List<TimelineStep>,
        onComplete: (List<StepRouteInfo>) -> Unit
    ) {
        val routeInfoList = mutableListOf<StepRouteInfo>()
        val coordinatePairs = buildCoordinatePairs(startingPoint, steps)

        if (coordinatePairs.isEmpty()) {
            onComplete(emptyList())
            return
        }

        // Calculate routes sequentially to avoid rate limiting
        calculateRoutesSequentially(coordinatePairs, 0, routeInfoList, onComplete)
    }

    /**
     * Build list of coordinate pairs with step IDs for route calculation
     */
    private fun buildCoordinatePairs(
        startingPoint: Coordinate?,
        steps: List<TimelineStep>
    ): List<CoordinatePair> {
        val pairs = mutableListOf<CoordinatePair>()

        // Get step coordinates
        val stepCoordinates = steps.mapNotNull { step ->
            step.poi?.coordinate?.let { coord ->
                StepCoordinate(step.id ?: 0, coord)
            }
        }

        if (stepCoordinates.isEmpty()) return pairs

        // Starting point to first step
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

        // Between consecutive steps
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

    /**
     * Calculate routes sequentially (one at a time) to avoid API rate limiting
     */
    private fun calculateRoutesSequentially(
        pairs: List<CoordinatePair>,
        index: Int,
        routeInfoList: MutableList<StepRouteInfo>,
        onComplete: (List<StepRouteInfo>) -> Unit
    ) {
        if (index >= pairs.size) {
            onComplete(routeInfoList)
            return
        }

        val pair = pairs[index]
        calculateSingleRoute(pair) { routeInfo ->
            routeInfo?.let { routeInfoList.add(it) }

            // Continue with next pair
            calculateRoutesSequentially(pairs, index + 1, routeInfoList, onComplete)
        }
    }

    /**
     * Calculate route between two coordinates
     * First checks cache, then tries walking profile, falls back to driving if distance >= 1.8km
     */
    private fun calculateSingleRoute(
        pair: CoordinatePair,
        onResult: (StepRouteInfo?) -> Unit
    ) {
        // Check cache first
        val cachedRoute = RouteCache.get(pair.from, pair.to)
        if (cachedRoute != null) {
            // Return cached result with step IDs
            onResult(cachedRoute.toStepRouteInfo(pair.fromStepId, pair.toStepId))
            return
        }

        val origin = Point.fromLngLat(pair.from.lng, pair.from.lat)
        val destination = Point.fromLngLat(pair.to.lng, pair.to.lat)

        // Calculate straight-line distance to determine profile
        val straightLineDistance = calculateStraightLineDistance(pair.from, pair.to)
        val isWalking = straightLineDistance < StepRouteInfo.WALKING_THRESHOLD_METERS

        val profile = if (isWalking) {
            DirectionsCriteria.PROFILE_WALKING
        } else {
            DirectionsCriteria.PROFILE_DRIVING
        }

        val routeOptions = RouteOptions.builder()
            .overview(DirectionsCriteria.OVERVIEW_SIMPLIFIED)
            .profile(profile)
            .coordinatesList(listOf(origin, destination))
            .build()

        val mapboxDirections = MapboxDirections.builder()
            .routeOptions(routeOptions)
            .accessToken(TRPCore.mapBoxApiKey)
            .build()

        mapboxDirections.enqueueCall(object : Callback<DirectionsResponse?> {
            override fun onResponse(
                call: Call<DirectionsResponse?>,
                response: Response<DirectionsResponse?>
            ) {
                val routes = response.body()?.routes()
                if (!routes.isNullOrEmpty()) {
                    val route = routes[0]
                    val distance = route.distance() ?: straightLineDistance
                    val duration = route.duration() ?: 0.0

                    // Determine walking based on actual route distance
                    val actualIsWalking = distance < StepRouteInfo.WALKING_THRESHOLD_METERS

                    val routeInfo = StepRouteInfo(
                        distanceMeters = distance,
                        durationSeconds = duration,
                        isWalking = actualIsWalking,
                        fromStepId = pair.fromStepId,
                        toStepId = pair.toStepId
                    )

                    // Cache the result
                    RouteCache.put(pair.from, pair.to, routeInfo)

                    onResult(routeInfo)
                } else {
                    // API returned no routes, use straight line estimate
                    val routeInfo = StepRouteInfo(
                        distanceMeters = straightLineDistance,
                        durationSeconds = estimateDuration(straightLineDistance, isWalking),
                        isWalking = isWalking,
                        fromStepId = pair.fromStepId,
                        toStepId = pair.toStepId
                    )

                    // Cache the estimate as well
                    RouteCache.put(pair.from, pair.to, routeInfo)

                    onResult(routeInfo)
                }
            }

            override fun onFailure(call: Call<DirectionsResponse?>, t: Throwable) {
                // On failure, use straight line estimate
                val routeInfo = StepRouteInfo(
                    distanceMeters = straightLineDistance,
                    durationSeconds = estimateDuration(straightLineDistance, isWalking),
                    isWalking = isWalking,
                    fromStepId = pair.fromStepId,
                    toStepId = pair.toStepId
                )

                // Cache the estimate as well (to avoid repeated failed API calls)
                RouteCache.put(pair.from, pair.to, routeInfo)

                onResult(routeInfo)
            }
        })
    }

    /**
     * Calculate straight-line distance between two coordinates using Haversine formula
     */
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

    /**
     * Estimate duration based on distance
     * Walking: ~5 km/h = ~12 min/km
     * Driving: ~40 km/h = ~1.5 min/km
     */
    private fun estimateDuration(distanceMeters: Double, isWalking: Boolean): Double {
        val distanceKm = distanceMeters / 1000.0
        val minutesPerKm = if (isWalking) 12.0 else 1.5
        return (distanceKm * minutesPerKm * 60) // Return seconds
    }

    /**
     * Helper class to hold step coordinate with ID
     */
    private data class StepCoordinate(
        val stepId: Int,
        val coordinate: Coordinate
    )

    /**
     * Helper class to hold coordinate pair for route calculation
     */
    private data class CoordinatePair(
        val from: Coordinate,
        val to: Coordinate,
        val fromStepId: Int?,
        val toStepId: Int
    )
}
