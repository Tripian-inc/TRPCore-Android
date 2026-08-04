package com.tripian.trpcore.util.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.core.graphics.toColorInt
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.Style
import com.mapbox.maps.TransitionOptions
import com.mapbox.maps.coroutine.awaitCameraForCoordinates
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.viewport.viewport
import com.mapbox.maps.toCameraOptions
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.domain.model.MarkerView
import com.tripian.trpcore.util.extensions.getBitmap
import java.util.Objects

class MapView : MapView {

    private val ROUTE_SOURCE_ID = "route-source-id"
    private val ROUTE_LAYER_ID = "route-layer-id"

    private val RETURN_ROUTE_SOURCE_ID = "return-route-source-id"
    private val RETURN_ROUTE_LAYER_ID = "return-route-layer-id"

    private val STEP_MARKER_ICON_SIZE = 0.8

    var map: MapboxMap? = null
    var style: Style? = null

    private var mapLoadListener: (() -> Unit)? = null
    private var mapZoomLevelListener: ((Double) -> Unit)? = null
    private var mapItemClickListener: ((MapStep) -> Unit)? = null
    private var mapEmptyClickListener: (() -> Unit)? = null
    private var mapInteractionListener: (() -> Unit)? = null

    private var gestureSinceTouchDown = false

    private var mapItems = ArrayList<MapStep>()

    /** Selected marker per city: cityIndex -> markerId. */
    private var selectedMarkerIds = mutableMapOf<Int, String>()

    private lateinit var routeLayer: LineLayer
    private lateinit var returnRouteLayer: LineLayer

    private var routesLayers: ArrayList<LineLayer> = arrayListOf()

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        map = mapboxMap

        compass.visibility = false
        scalebar.enabled = false
        attribution.enabled = false
        logo.enabled = false

        map?.subscribeCameraChanged {
            mapZoomLevelListener?.invoke(it.cameraState.zoom)
        }

        mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            this@MapView.style = style

            style.setStyleTransition(
                TransitionOptions.Builder().delay(0).duration(0).enablePlacementTransitions(false)
                    .build()
            )

            style.addSource(geoJsonSource(RETURN_ROUTE_SOURCE_ID) {
                featureCollection(FeatureCollection.fromFeatures(arrayOf()))
            })
            style.addSource(geoJsonSource(ROUTE_SOURCE_ID) {
                featureCollection(FeatureCollection.fromFeatures(arrayOf()))
            })

            returnRouteLayer = LineLayer(RETURN_ROUTE_LAYER_ID, RETURN_ROUTE_SOURCE_ID)

            returnRouteLayer.lineWidth(4.0)
            returnRouteLayer.lineTranslate(listOf(0.0, 4.0))
            returnRouteLayer.lineDasharray(listOf(1.2, 1.2))
            returnRouteLayer.lineColor("#07074E".toColorInt())

            style.addLayer(returnRouteLayer)

            routeLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID)

            routeLayer.lineWidth(4.0)
            routeLayer.lineTranslate(listOf(0.0, 4.0))
            routeLayer.lineDasharray(listOf(1.2, 1.2))
            routeLayer.lineColor("#07074E".toColorInt())

            style.addLayer(routeLayer)

            mapLoadListener?.invoke()
        }

        setOnMapClickListener()
        setupGestureListeners()
    }

    /**
     * Sets up pan/zoom gesture listeners used to notify [mapInteractionListener]
     * when the user interacts with the map.
     */
    private fun setupGestureListeners() {
        gestures.addOnMoveListener(object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {
                gestureSinceTouchDown = true
                mapInteractionListener?.invoke()
            }

            override fun onMove(detector: MoveGestureDetector): Boolean {
                return false
            }

            override fun onMoveEnd(detector: MoveGestureDetector) {
            }
        })

        gestures.addOnScaleListener(object : OnScaleListener {
            override fun onScaleBegin(detector: StandardScaleGestureDetector) {
                gestureSinceTouchDown = true
                mapInteractionListener?.invoke()
            }

            override fun onScale(detector: StandardScaleGestureDetector) {
            }

            override fun onScaleEnd(detector: StandardScaleGestureDetector) {
            }
        })
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            gestureSinceTouchDown = false
        }
        return super.dispatchTouchEvent(event)
    }

    /**
     * A pan/zoom that ends without leaving the touch slop is still reported as a
     * map click by Mapbox; [gestureSinceTouchDown] keeps those from reaching
     * [mapEmptyClickListener], which would otherwise undo the pan's own effect.
     */
    private fun setOnMapClickListener() {
        gestures.addOnMapClickListener { point ->
            val followsGesture = gestureSinceTouchDown
            val pixel = map?.pixelForCoordinate(point)

            if (pixel != null) {
                map?.queryRenderedFeatures(
                    RenderedQueryGeometry(pixel),
                    RenderedQueryOptions(null, literal(true)),
                    callback = { callback ->
                        val features = callback.value
                        var hitPoi = false
                        if (!features.isNullOrEmpty()) {
                            run loop@{
                                features.forEach { feature ->
                                    if (feature.queriedFeature.feature.properties()
                                            ?.get("poiId") != null
                                    ) {
                                        val annotation =
                                            Gson().fromJson(
                                                feature.queriedFeature.feature.properties(),
                                                MapStep::class.java
                                            )

                                        mapItemClickListener?.invoke(annotation)
                                        hitPoi = true

                                        return@loop
                                    }
                                }
                            }
                        }
                        if (!hitPoi && !followsGesture) {
                            mapEmptyClickListener?.invoke()
                        }
                    })
            }

            true

        }
    }

    fun showMapIcons(items: List<MapStep>) {
        try {
            if (style?.styleLayerExists(ROUTE_LAYER_ID) == true) {
                style?.removeStyleLayer(ROUTE_LAYER_ID)
            }
            if (style?.styleLayerExists(RETURN_ROUTE_LAYER_ID) == true) {
                style?.removeStyleLayer(RETURN_ROUTE_LAYER_ID)
            }

            mapItems.addAll(items)

            if (items.isNotEmpty()) {
                style?.addLayer(routeLayer)
                style?.addLayer(returnRouteLayer)
                style?.moveStyleLayer(
                    RETURN_ROUTE_LAYER_ID,
                    LayerPosition(null, ROUTE_LAYER_ID, null)
                )
            }

            items.forEach { item ->
                if (item.coordinate != null && item.coordinate!!.lng != -1.0 && item.coordinate!!.lat != -1.0) {
                    val uniq = item.group + item.poiId

                    val properties: JsonObject =
                        Gson().fromJson(Gson().toJson(item), JsonElement::class.java).asJsonObject

                    if (style?.getSource(uniq) == null) {
                        style?.addSource(
                            GeoJsonSource.Builder(uniq)
                                .feature(
                                    Feature.fromGeometry(
                                        Point.fromLngLat(
                                            item.coordinate!!.lng,
                                            item.coordinate!!.lat
                                        ), properties
                                    )
                                )
                                .build()
                        )
                    } else {
                        (Objects.requireNonNull(style?.getSource(uniq)) as GeoJsonSource).apply {
                            feature(
                                Feature.fromGeometry(
                                    Point.fromLngLat(
                                        item.coordinate!!.lng,
                                        item.coordinate!!.lat
                                    ), properties
                                )
                            )
                        }
                    }

                    val view = MarkerView(context)

                    if (item.isCityMarker) {
                        view.setCityMarker(true)
                    } else {
                        if (item.markerIcon != -1) {
                            view.iconView.setImageResource(item.markerIcon)
                            view.iconView.visibility = VISIBLE
                        } else {
                            view.iconView.visibility = GONE
                        }

                        if (item.isOffer) {
                            view.iconViewBackground.visibility = VISIBLE
                        } else {
                            view.iconViewBackground.visibility = GONE
                        }

                        if (item.isFlexible) {
                            view.poiOrderTv.text = "−"
                            view.poiOrderTv.visibility = VISIBLE
                        } else if (item.position != -1) {
                            view.poiOrderTv.text = item.position.toString()
                            view.poiOrderTv.visibility = VISIBLE
                        } else {
                            view.poiOrderTv.visibility = GONE
                        }

                        view.setCityIndex(item.cityIndex)

                        view.setSelected(item.isSelected)
                    }

                    val bitmap: Bitmap = view.getBitmap()

                    style?.addImage(uniq, bitmap)

                    if (item.isSelected) {
                        selectedMarkerIds[item.cityIndex] = uniq
                    }

                    if (style?.getLayer(uniq) == null) {
                        val stretchLayer = symbolLayer(uniq, uniq) {
                            iconImage(uniq)
                            iconIgnorePlacement(true)
                            iconAllowOverlap(true)
                            if (!item.isCityMarker) iconSize(STEP_MARKER_ICON_SIZE)
                        }

                        when {
                            item.isCityMarker -> {
                                style?.addLayerAbove(stretchLayer, ROUTE_LAYER_ID)
                            }
                            TextUtils.equals(item.group, "step") -> {
                                style?.addLayerAbove(stretchLayer, ROUTE_LAYER_ID)
                            }
                            else -> {
                                val firstStepItem = mapItems.firstOrNull { it.group == "step" }
                                val stepLayerId = firstStepItem?.let { "step" + it.poiId }
                                if (stepLayerId != null && style?.getLayer(stepLayerId) != null) {
                                    style?.addLayerBelow(stretchLayer, stepLayerId)
                                } else {
                                    style?.addLayerAbove(stretchLayer, ROUTE_LAYER_ID)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MapView", e.message, e)
        }
    }

    suspend fun moveCameraTo(fallbackCoordinate: Point? = null) {
        try {
            val latLngList: MutableList<Point> = ArrayList()
            for (i in mapItems.indices) {
                if (mapItems[i].coordinate != null && mapItems[i].coordinate!!.lat != -1.0 && mapItems[i].coordinate!!.lng != -1.0) {
                    latLngList.add(
                        Point.fromLngLat(
                            mapItems[i].coordinate!!.lng,
                            mapItems[i].coordinate!!.lat
                        )
                    )
                }
            }

            if (latLngList.size > 1) {
                val cameraOptionsForCoordinates = map?.awaitCameraForCoordinates(
                    coordinates = latLngList,
                    camera = cameraOptions {
                        zoom(13.0)
                    },
                    coordinatesPadding = EdgeInsets(100.0, 100.0, 100.0, 100.0),
                    maxZoom = 15.0,
                    offset = null
                )
                cameraOptionsForCoordinates?.let {
                    map?.easeTo(
                        it,
                        mapAnimationOptions {
                            duration(500L)
                        }
                    )
                }
            } else if (latLngList.isNotEmpty()) {
                map?.flyTo(
                    cameraOptions {
                        center(latLngList[0])
                        zoom(13.0)
                    },
                    mapAnimationOptions {
                        duration(500L)
                    }
                )
            } else if (fallbackCoordinate != null) {
                map?.flyTo(
                    cameraOptions {
                        center(fallbackCoordinate)
                        zoom(12.0)
                    },
                    mapAnimationOptions {
                        duration(500L)
                    }
                )
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Fits the camera to the given points. No-op on an empty list;
     * a single point flies to it at zoom 13.
     */
    suspend fun fitCameraToPoints(points: List<Point>) {
        if (points.isEmpty()) return
        try {
            if (points.size > 1) {
                val cameraOptionsForCoordinates = map?.awaitCameraForCoordinates(
                    coordinates = points,
                    camera = cameraOptions {
                        zoom(13.0)
                    },
                    coordinatesPadding = EdgeInsets(100.0, 100.0, 100.0, 100.0),
                    maxZoom = 15.0,
                    offset = null
                )
                cameraOptionsForCoordinates?.let {
                    map?.easeTo(
                        it,
                        mapAnimationOptions {
                            duration(500L)
                        }
                    )
                }
            } else {
                map?.flyTo(
                    cameraOptions {
                        center(points[0])
                        zoom(13.0)
                    },
                    mapAnimationOptions {
                        duration(500L)
                    }
                )
            }
        } catch (_: Exception) {
        }
    }

    fun moveCameraTo(location: Location?, zoom: Double? = null) {
        location?.let {
            map?.flyTo(
                cameraOptions {
                    center(Point.fromLngLat(it.longitude, it.latitude))
                    zoom(zoom ?: 10.0)
                },
                mapAnimationOptions {
                    duration(500L)
                }

            )
        }
    }

    fun clearMap(items: List<MapStep>?) {
        items?.forEach { item ->
            clearItem(item.group + item.poiId)
        }
    }

    fun clearMap() {
        style?.removeStyleLayer(ROUTE_LAYER_ID)
        style?.removeStyleLayer(RETURN_ROUTE_LAYER_ID)

        mapItems.forEach { item ->
            clearItem(item.group + item.poiId)
        }

        mapItems.clear()
        selectedMarkerIds.clear()
    }

    private fun clearItem(uniq: String) {
        style?.removeStyleLayer(uniq)
        style?.removeStyleSource(uniq)
        style?.removeStyleImage(uniq)
    }

    fun showRoute(route: DirectionsRoute) {
        val source = style?.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)

        source?.apply {
            featureCollection(
                FeatureCollection.fromFeature(
                    Feature.fromGeometry(
                        LineString.fromPolyline(
                            route.geometry()!!,
                            Constants.PRECISION_6
                        )
                    )
                )
            )
        }
    }

    fun setOnMapLoadListener(task: () -> Unit) {
        mapLoadListener = task
    }

    fun setOnZoomLevelListener(task: (Double) -> Unit) {
        mapZoomLevelListener = task
    }

    fun setOnMapClickListener(task: (MapStep) -> Unit) {
        mapItemClickListener = task
    }

    /**
     * Called when the user taps empty map space (no POI marker under the tap).
     * Use to toggle UI overlays like the bottom card list.
     */
    fun setOnMapEmptyClickListener(task: () -> Unit) {
        mapEmptyClickListener = task
    }

    /**
     * Set listener for map interactions (pan, zoom, etc.).
     * Called when user begins interacting with the map (not clicking on annotations).
     */
    fun setOnMapInteractionListener(listener: () -> Unit) {
        mapInteractionListener = listener
    }

    @SuppressLint("MissingPermission")
    fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(context)) {

            location.enabled = true

            location.puckBearingEnabled = true
            if (location.locationPuck is LocationPuck2D) {
                location.locationPuck = createDefault2DPuck(withBearing = true)
            }
            viewport.transitionTo(
                targetState = viewport.makeFollowPuckViewportState(),
                transition = viewport.makeImmediateViewportTransition()
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun disableLocation() {
        location.enabled = false
    }

    private var redirectRoute: Pair<DirectionsRoute, MapStep>? = null
    fun redirectRoute(route: Pair<DirectionsRoute, MapStep>) {
        redirectRoute = route

        val item = route.second

        val uniq = item.group + item.poiId + "-route"

        val routeCurrentLocationLayer = LineLayer(
            "$uniq-layer",
            "$uniq-source"
        )

        routeCurrentLocationLayer.lineWidth(3.0)
        routeCurrentLocationLayer.lineTranslate(listOf(0.0, 4.0))
        routeCurrentLocationLayer.lineDasharray(listOf(1.2, 1.2))
        routeCurrentLocationLayer.lineColor("#D9326E".toColorInt())
        style?.addLayerAbove(routeCurrentLocationLayer, ROUTE_LAYER_ID)

        style?.addSource(
            GeoJsonSource.Builder("$uniq-source")
                .feature(
                    Feature.fromGeometry(
                        LineString.fromPolyline(
                            route.first.geometry()!!,
                            Constants.PRECISION_6
                        )
                    )
                )
                .build()
        )

        val view = MarkerView(context)

        if (item.markerIcon != -1) {
            view.iconView.setImageResource(item.markerIcon)
            view.iconView.visibility = VISIBLE
        } else {
            view.iconView.visibility = GONE
        }

        view.iconViewBackground.visibility = GONE

        if (item.isFlexible) {
            view.poiOrderTv.text = "−"
            view.poiOrderTv.visibility = VISIBLE
        } else if (item.position != -1) {
            view.poiOrderTv.text = item.position.toString()
            view.poiOrderTv.visibility = VISIBLE
        } else {
            view.poiOrderTv.visibility = GONE
        }

        val bitmap: Bitmap = view.getBitmap()

        style?.addImage(uniq, bitmap)

        if (style?.getLayer(uniq) == null) {
            style?.addLayerAbove(
                symbolLayer(uniq, uniq) {
                    iconImage(uniq)
                    iconIgnorePlacement(true)
                    iconAllowOverlap(true)
                }, "$uniq-layer"
            )
        }
    }

    fun getBounds(): CoordinateBounds {
        val cameraState = mapboxMap.cameraState
        val bounds = mapboxMap.coordinateBoundsForCamera(cameraState.toCameraOptions())
        return bounds
    }

    fun getDistance(): Double {
        return TurfMeasurement.distance(
            getBounds().center(),
            getBounds().northwest(),
            TurfConstants.UNIT_KILOMETERS
        )
    }

    fun removeRedirect() {
        if (redirectRoute != null) {
            val item = redirectRoute!!.second

            val uniq = item.group + item.poiId + "-route"

            style?.removeStyleLayer(uniq)
            style?.removeStyleSource(uniq)
            style?.removeStyleImage(uniq)
            style?.removeStyleLayer("$uniq-layer")
            style?.removeStyleSource("$uniq-source")
        }

        routesLayers.forEach {
            style?.removeStyleLayer(it.layerId)
            style?.removeStyleSource(it.sourceId)
        }

        routesLayers.clear()
    }

    /**
     * Focuses the camera on a specific marker by its poiId.
     *
     * @param poiId The poiId of the marker to focus on
     */
    fun focusOnMarker(poiId: String) {
        val item = mapItems.find { it.poiId == poiId } ?: return
        val coordinate = item.coordinate ?: return

        map?.flyTo(
            cameraOptions {
                center(Point.fromLngLat(coordinate.lng, coordinate.lat))
                zoom(15.0)
            },
            mapAnimationOptions {
                duration(500L)
            }
        )
    }

    /**
     * Zooms the camera to a specific point with the given zoom level.
     * This method doesn't require the point to be in mapItems.
     *
     * @param point The Point to zoom to
     * @param zoomLevel The zoom level (default 13.0)
     */
    fun zoomToPoint(point: Point, zoomLevel: Double = 13.0) {
        map?.flyTo(
            cameraOptions {
                center(point)
                zoom(zoomLevel)
            },
            mapAnimationOptions {
                duration(500L)
            }
        )
    }

    /**
     * Zooms the camera to a specific coordinate with the given zoom level.
     *
     * @param lng Longitude
     * @param lat Latitude
     * @param zoomLevel The zoom level (default 13.0)
     */
    fun zoomToCoordinate(lng: Double, lat: Double, zoomLevel: Double = 13.0) {
        zoomToPoint(Point.fromLngLat(lng, lat), zoomLevel)
    }

    /**
     * Selects a marker on the map by its poiId.
     * Deselects the previously selected marker in the same city and selects the new one.
     * Each city can have its own selected marker.
     *
     * @param poiId The poiId of the marker to select
     */
    fun selectMarker(poiId: String) {
        val newSelectedItem = mapItems.find { it.poiId == poiId } ?: return

        val newUniq = newSelectedItem.group + newSelectedItem.poiId
        val cityIndex = newSelectedItem.cityIndex
        if (selectedMarkerIds[cityIndex] == newUniq) {
            return
        }

        selectedMarkerIds[cityIndex]?.let { prevUniq ->
            val prevItem = mapItems.find { (it.group + it.poiId) == prevUniq }
            prevItem?.let {
                it.isSelected = false
                updateMarkerImage(it)
            }
        }

        newSelectedItem.isSelected = true
        updateMarkerImage(newSelectedItem)
        selectedMarkerIds[cityIndex] = newUniq
    }

    /**
     * Updates the marker image on the map for the given MapStep.
     * This is called when selection state changes.
     */
    private fun updateMarkerImage(item: MapStep) {
        val uniq = item.group + item.poiId

        val view = MarkerView(context)

        if (item.markerIcon != -1) {
            view.iconView.setImageResource(item.markerIcon)
            view.iconView.visibility = VISIBLE
        } else {
            view.iconView.visibility = GONE
        }

        if (item.isOffer) {
            view.iconViewBackground.visibility = VISIBLE
        } else {
            view.iconViewBackground.visibility = GONE
        }

        if (item.isFlexible) {
            view.poiOrderTv.text = "−"
            view.poiOrderTv.visibility = VISIBLE
        } else if (item.position != -1) {
            view.poiOrderTv.text = item.position.toString()
            view.poiOrderTv.visibility = VISIBLE
        } else {
            view.poiOrderTv.visibility = GONE
        }

        view.setCityIndex(item.cityIndex)

        view.setSelected(item.isSelected)

        val bitmap: Bitmap = view.getBitmap()

        style?.addImage(uniq, bitmap)
    }
}
