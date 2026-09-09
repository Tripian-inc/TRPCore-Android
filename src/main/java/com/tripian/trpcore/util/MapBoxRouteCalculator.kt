package com.tripian.trpcore.util

import android.content.Context
import android.widget.Toast
import com.google.android.gms.common.util.CollectionUtils
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpfoundationkit.enums.DirectionErrorStatus
import com.tripian.trpfoundationkit.enums.DirectionProfile
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.Locale
import java.util.Objects

/**
 * This is the main class for calculating [MapboxDirections] directions of a given origin and destination.
 */
class MapBoxRouteCalculator {
    private var routeErrorStatus = DirectionErrorStatus.NONE
    private var directionProfile = DirectionProfile.WALKING
    private var onLoadListener: OnLoadListener? = null
    private var onCurrentLocationLoadListener: OnCurrentLocationLoadListener? = null

    var mdRoute: MapboxDirections? = null

    /** Listener for [calculate] results. */
    interface OnLoadListener {
        fun onLoad(response: DirectionsResponse?)

        fun onMapBoxError(message: String?)
    }

    /** Listener for [calculateToCurrentLocation] results. */
    interface OnCurrentLocationLoadListener {
        fun onLoad(response: Response<DirectionsResponse?>?, source: GeoJsonSource?)

        fun onMapBoxError(message: String?)
    }

    /**
     * Calculates directions for the given waypoints and delivers the result to [OnLoadListener].
     *
     * @param origin      the origin of the route
     * @param destination the destination of the route
     * @param points      the list of Points for which refers to the route points.
     */
    fun calculate(
        origin: Point?,
        destination: Point?,
        points: List<Point?>?,
        alternatives: Boolean = false,
        excludeFerry: Boolean = false
    ) {
        val routeOptionsBuilder = RouteOptions.builder()
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(getDirectionsCriteria(directionProfile))
            .steps(true)
            .coordinatesList(points!!)

        if (alternatives) {
            routeOptionsBuilder.alternatives(true)
        }
        if (excludeFerry) {
            routeOptionsBuilder.exclude(DirectionsCriteria.EXCLUDE_FERRY)
        }

        val routeOptions = routeOptionsBuilder.build()

        mdRoute = MapboxDirections.builder()
            .routeOptions(routeOptions)
            .accessToken(TRPCore.mapBoxApiKey).build()
        mdRoute?.enqueueCall(object : Callback<DirectionsResponse?> {
            override fun onResponse(
                call: Call<DirectionsResponse?>,
                response: Response<DirectionsResponse?>
            ) {
                if (response.body() != null) {
                    if (isErrorContainsNoRoute(response) && routeErrorStatus === DirectionErrorStatus.NONE) {
                        directionProfile = DirectionProfile.AUTOMOBILE
                        routeErrorStatus = DirectionErrorStatus.WALKING
                        calculate(origin, destination, points, alternatives, excludeFerry)
                        return
                    }
                    if (routeErrorStatus === DirectionErrorStatus.WALKING) {
                        routeErrorStatus = DirectionErrorStatus.AUTOMOBILE
                        directionProfile = DirectionProfile.WALKING
                        val newWayPoints = calculateNewWayPoints(
                            response.body()!!.routes()[0].legs()
                        )
                        calculate(origin, destination, newWayPoints, alternatives, excludeFerry)
                        return
                    }
                    if (isErrorContainsNoRoute(response)) {
                        onLoadListener!!.onMapBoxError("No route")
                        return
                    }
                    onLoadListener!!.onLoad(response.body())
                }
            }

            override fun onFailure(call: Call<DirectionsResponse?>, t: Throwable) {
                onLoadListener!!.onMapBoxError(t.message)
            }
        })
    }

    /**
     * Cancel any in-flight Mapbox Directions request.
     * Safe to call multiple times; no-op if no active call.
     */
    fun cancel() {
        mdRoute?.cancelCall()
    }

    /**
     * Single batch request with no walking→automobile fallback: exactly one call, one response.
     * Legs include their steps so callers can rebuild each leg's shape.
     *
     * @param points Full waypoint list (size >= 2). One leg returned per consecutive pair.
     * @param profile Routing profile; defaults to WALKING.
     * @param onResult Invoked once with either response body or throwable, on Mapbox's callback thread.
     */
    fun calculateBatch(
        points: List<Point>,
        profile: DirectionProfile = DirectionProfile.WALKING,
        onResult: (response: DirectionsResponse?, error: Throwable?) -> Unit
    ) {
        val routeOptions = RouteOptions.builder()
            .overview(DirectionsCriteria.OVERVIEW_SIMPLIFIED)
            .profile(getDirectionsCriteria(profile))
            .steps(true)
            .coordinatesList(points)
            .build()

        mdRoute = MapboxDirections.builder()
            .routeOptions(routeOptions)
            .accessToken(TRPCore.mapBoxApiKey)
            .build()

        mdRoute?.enqueueCall(object : Callback<DirectionsResponse?> {
            override fun onResponse(
                call: Call<DirectionsResponse?>,
                response: Response<DirectionsResponse?>
            ) {
                onResult(response.body(), null)
            }

            override fun onFailure(call: Call<DirectionsResponse?>, t: Throwable) {
                onResult(null, t)
            }
        })
    }

    /**
     * Calculates directions from the current location and delivers the result to
     * [OnCurrentLocationLoadListener].
     *
     * @param style       the Style for which refers to the [Style] class instance
     * @param origin      the origin of the route
     * @param destination the destination of the route
     */
    fun calculateToCurrentLocation(
        context: Context?,
        style: Style,
        origin: Point,
        destination: Point
    ) {
        mdRoute = MapboxDirections.builder()
            .routeOptions(
                RouteOptions.builder()
                    .coordinatesList(CollectionUtils.listOf(origin, destination))
                    .overview(DirectionsCriteria.OVERVIEW_FULL)
                    .profile(getDirectionsCriteria(directionProfile))
                    .build()
            )
            .accessToken(TRPCore.mapBoxApiKey).build()
        mdRoute?.enqueueCall(object : Callback<DirectionsResponse?> {
            override fun onResponse(
                call: Call<DirectionsResponse?>,
                response: Response<DirectionsResponse?>
            ) {
                if (response.body() != null) {
                    if (isErrorContainsNoRoute(response) && routeErrorStatus === DirectionErrorStatus.NONE) {
                        directionProfile = DirectionProfile.AUTOMOBILE
                        routeErrorStatus = DirectionErrorStatus.WALKING
                        calculateToCurrentLocation(context, style, origin, destination)
                        return
                    }
                    if (routeErrorStatus === DirectionErrorStatus.WALKING) {
                        routeErrorStatus = DirectionErrorStatus.AUTOMOBILE
                        directionProfile = DirectionProfile.WALKING
                        calculateToCurrentLocation(context, style, origin, destination)
                        return
                    }
                    if (isErrorContainsNoRoute(response)) {
                        Toast.makeText(context, "NO ROUTE", Toast.LENGTH_SHORT).show()
                        return
                    }
                    if (style.isStyleLoaded()) {
                        val source = style.getSourceAs<GeoJsonSource>(ROUTE_CURRENT_LOCATION_SOURCE_ID)
                        if (source != null) {
                            onCurrentLocationLoadListener!!.onLoad(response, source)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<DirectionsResponse?>, t: Throwable) {
                onCurrentLocationLoadListener!!.onMapBoxError(t.message)
            }
        })
    }

    private fun calculateNewWayPoints(routeLegList: List<RouteLeg>?): List<Point?> {
        val newWayPoint: MutableList<Point?> = ArrayList()
        val legSteppers = getLegListAsLegStepper(routeLegList)
        for (legStepper in legSteppers) {
            newWayPoint.add(legStepper.firstPoint)
            if (legStepper.legOrder != routeLegList!!.size - 1) {
                newWayPoint.add(legStepper.lastPoint)
            }
        }
        return newWayPoint
    }

    private fun getLegListAsLegStepper(routeLegList: List<RouteLeg>?): List<LegStepper> {
        val legStepperList: MutableList<LegStepper> = ArrayList()
        for (leg in routeLegList!!) {
            val legStepper = legToLegStepper(routeLegList, leg)
            legStepperList.add(legStepper)
        }
        return legStepperList
    }

    private fun legToLegStepper(routeLegList: List<RouteLeg>?, leg: RouteLeg): LegStepper {
        val stepStart = Objects.requireNonNull(leg.steps())[0]
        val stepEnd = Objects.requireNonNull(leg.steps())[Objects.requireNonNull(
            leg.steps()
        ).size - 1]
        val firstLineString = getLineString(stepStart)
        val lastLineString = getLineString(stepEnd)
        return LegStepper(
            getPointFromLineString(firstLineString, true),
            getPointFromLineString(lastLineString, false),
            routeLegList!!.indexOf(leg)
        )
    }

    private fun getPointFromLineString(lineString: LineString, isFirst: Boolean): Point {
        var order = 0
        if (!isFirst) {
            order = lineString.coordinates().size - 1
        }
        return lineString.coordinates()[order]
    }

    private fun getLineString(step: LegStep): LineString {
        return LineString.fromPolyline(
            step.geometry() ?: "",
            Constants.PRECISION_6
        )
    }

    private fun isErrorContainsNoRoute(response: Response<DirectionsResponse?>): Boolean {
        if (response.body() == null) {
            return false
        }
        if ("NoRoute" == response.body()!!.code()) return true
        try {
            if (response.errorBody() != null && response.errorBody()!!.string()
                    .lowercase(Locale.getDefault()).contains("No Route")
            ) {
                return true
            }
        } catch (e: IOException) {
            return false
        }
        return false
    }

    private fun getDirectionsCriteria(directionProfile: DirectionProfile): String {
        return when (directionProfile) {
            DirectionProfile.AUTOMOBILE -> DirectionsCriteria.PROFILE_DRIVING
            DirectionProfile.WALKING -> DirectionsCriteria.PROFILE_WALKING
            else -> DirectionsCriteria.PROFILE_WALKING
        }
    }

    fun setOnLoadListener(onLoadListener: OnLoadListener?) {
        this.onLoadListener = onLoadListener
    }

    fun setOnCurrentLocationLoadListener(onCurrentLocationLoadListener: OnCurrentLocationLoadListener?) {
        this.onCurrentLocationLoadListener = onCurrentLocationLoadListener
    }

    companion object {
        private const val ROUTE_CURRENT_LOCATION_SOURCE_ID = "route-current-location-source-id"
    }
}