package com.tripian.trpcore.util.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
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
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.viewport
import com.mapbox.maps.toCameraOptions
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.domain.model.MarkerView
import com.tripian.trpcore.util.extensions.getBitmap
import java.util.Objects

/**
 * Created by semihozkoroglu on 15.09.2020.
 */
class MapView : MapView {

    private val ROUTE_SOURCE_ID = "route-source-id"
    private val ROUTE_LAYER_ID = "route-layer-id"

    private val RETURN_ROUTE_SOURCE_ID = "return-route-source-id"
    private val RETURN_ROUTE_LAYER_ID = "return-route-layer-id"

    var map: MapboxMap? = null
    var style: Style? = null

    private var mapLoadListener: (() -> Unit)? = null
    private var mapZoomLevelListener: ((Double) -> Unit)? = null
    private var mapItemClickListener: ((MapStep) -> Unit)? = null

    private var mapItems = ArrayList<MapStep>()

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
    }

    private fun setOnMapClickListener() {
        gestures.addOnMapClickListener { point ->
            val pixel = map?.pixelForCoordinate(point)

            if (pixel != null) {
                map?.queryRenderedFeatures(
                    RenderedQueryGeometry(pixel),
                    RenderedQueryOptions(null, literal(true)),
                    callback = { callback ->
                        val features = callback.value
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

                                        return@loop
                                    }
                                }
                            }
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

                    if (item.markerIcon != -1) {
                        view.iconView.setImageResource(item.markerIcon)
                    }

                    if (item.isOffer) {
                        view.iconViewBackground.visibility = VISIBLE
                    } else {
                        view.iconViewBackground.visibility = GONE
                    }

                    if (item.position != -1) {
                        view.poiOrderTv.text = item.position.toString()
//                        if (item.position == 1) {
//                            view.poiOrderTv.background = ContextCompat.getDrawable(
//                                context,
//                                R.drawable.bg_marker_green
//                            )
//                        }
                        view.poiOrderTv.visibility = VISIBLE
                    } else {
                        view.poiOrderTv.visibility = GONE
                    }

                    val bitmap: Bitmap = view.getBitmap()

                    style?.addImage(uniq, bitmap)

                    if (style?.getLayer(uniq) == null) {
                        if (TextUtils.equals(item.group, "step")) {
                            val stretchLayer = symbolLayer(uniq, uniq) {
                                iconImage(uniq)
                                iconIgnorePlacement(true)
                                iconAllowOverlap(true)
                            }
                            style?.addLayerAbove(stretchLayer, ROUTE_LAYER_ID)
                        } else {
                            val stretchLayer = symbolLayer(uniq, uniq) {
                                iconImage(uniq)
                                iconIgnorePlacement(true)
                                iconAllowOverlap(true)
                            }
                            style?.addLayerBelow(stretchLayer, "step" + mapItems[0].poiId)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MapView", e.message, e)
        }
    }

    suspend fun moveCameraTo() {
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
                    coordinates = listOf(latLngList.first(), latLngList.last()),
                    camera = cameraOptions {
                        zoom(13.0)
                    },
                    coordinatesPadding = EdgeInsets(50.0, 50.0, 50.0, 50.0),
                    maxZoom = 13.0,
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
                if (latLngList.isNotEmpty()) {
                    map?.flyTo(
                        cameraOptions {
                            center(latLngList[0])
                            zoom(13.0)
                        },
                        mapAnimationOptions {
                            duration(500L)
                        }

                    )
                }
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

//    fun showTravels(routes: List<Parts>) {
//        var currentMode: String? = ""
//        var currentId = ""
//        val tmpParts = arrayListOf<Parts>()
//
//        routes.forEach {
//            if (it.mode != currentMode) {
//                drawRoutes(tmpParts.clone() as List<Parts>, currentMode, currentId)
//
//                currentMode = it.mode
//                currentId = "${it.id ?: System.currentTimeMillis()}"
//                tmpParts.clear()
//            }
//
//            tmpParts.add(it)
//        }
//
//        if (tmpParts.size > 0) {
//            drawRoutes(tmpParts, currentMode, currentId)
//        }
//    }

//    private fun drawRoutes(routes: List<Parts>, mode: String?, id: String) {
//        if (routes.isEmpty() || mode.isNullOrEmpty()) return
//
//        val color = when (mode) {
//            "walk" -> "#008B9E" // blue
//            "bus" -> "#F6D047" // yellow
//            "metro", "rail_underground" -> "#006F54" // green
//            else -> "#D9326E" // orange
//        }
//
//        val layer = LineLayer(id, id)
//
//        layer.lineWidth(4.0)
//        layer.lineTranslate(listOf(0.0, 4.0))
//        layer.lineColor(Color.parseColor(color))
//
//        style?.addLayer(layer)
//
//        val featureData =
//            routes.map { it.coords }.flatMap { it.map { Point.fromLngLat(it.lng!!, it.lat!!) } }
//                .let {
//                    FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(it)))
//                }
//
//        style?.getSourceAs<GeoJsonSource>(id)?.apply {
//            featureCollection(featureData)
//        } ?: run {
//            style?.addSource(
//                geoJsonSource(id).apply {
//                    featureCollection(featureData)
//                }
//            )
//        }
//
//        routesLayers.add(layer)
//    }

    fun setOnMapLoadListener(task: () -> Unit) {
        mapLoadListener = task
    }

    fun setOnZoomLevelListener(task: (Double) -> Unit) {
        mapZoomLevelListener = task
    }

    fun setOnMapClickListener(task: (MapStep) -> Unit) {
        mapItemClickListener = task
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
        }

        view.iconViewBackground.visibility = GONE

        if (item.position != -1) {
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
}

//package com.tripian.trpcore.util.widget
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.Color
//import android.location.Location
//import android.text.TextUtils
//import android.util.AttributeSet
//import android.view.View
//import com.google.gson.Gson
//import com.google.gson.JsonElement
//import com.google.gson.JsonObject
//import com.mapbox.android.core.permissions.PermissionsManager
//import com.mapbox.api.directions.v5.models.DirectionsRoute
//import com.mapbox.core.constants.Constants
//import com.mapbox.geojson.Feature
//import com.mapbox.geojson.FeatureCollection
//import com.mapbox.geojson.LineString
//import com.mapbox.geojson.Point
//import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
//import com.mapbox.mapboxsdk.geometry.LatLng
//import com.mapbox.mapboxsdk.geometry.LatLngBounds
//import com.mapbox.mapboxsdk.location.LocationComponent
//import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
//import com.mapbox.mapboxsdk.location.LocationComponentOptions
//import com.mapbox.mapboxsdk.location.modes.RenderMode
//import com.mapbox.mapboxsdk.maps.MapboxMap
//import com.mapbox.mapboxsdk.maps.Style
//import com.mapbox.mapboxsdk.style.layers.LineLayer
//import com.mapbox.mapboxsdk.style.layers.PropertyFactory
//import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage
//import com.mapbox.mapboxsdk.style.layers.SymbolLayer
//import com.mapbox.mapboxsdk.style.layers.TransitionOptions
//import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
//import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
//import com.tripian.trpcore.domain.model.MapStep
//import com.tripian.trpcore.util.extensions.dp2Px
//import com.tripian.trpcore.util.extensions.getBitmap
//import java.util.Objects
//
///**
// * Created by semihozkoroglu on 15.09.2020.
// */
//class MapView2 : com.mapbox.mapboxsdk.maps.MapView {
//
//    private val ROUTE_SOURCE_ID = "route-source-id"
//    private val ROUTE_CURRENT_LOCATION_SOURCE_ID = "route-current-location-source-id"
//    private val ROUTE_LAYER_ID = "route-layer-id"
//    private val ROUTE_CURRENT_LOCATION_LAYER_ID = "route-current-location-layer-id"
//
//    var map: MapboxMap? = null
//    var style: Style? = null
//    var locationComponent: LocationComponent? = null
//    private var mapLoadListener: (() -> Unit)? = null
//    private var mapZoomLevelListener: ((Double) -> Unit)? = null
//    private var mapItemClickListener: ((MapStep) -> Unit)? = null
//
//    var mapItems = ArrayList<MapStep>()
//
//    private lateinit var routeLayer: LineLayer
//
//    constructor(context: Context) : super(context) {
//        init()
//    }
//
//    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
//        init()
//    }
//
//    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
//        init()
//    }
//
//    private fun init() {
//        getMapAsync {
//            map = it
//            map?.uiSettings?.isCompassEnabled = false
//            map?.uiSettings?.isRotateGesturesEnabled = false
//
//            map?.addOnCameraMoveListener {
//                map?.let { mapZoomLevelListener?.invoke(it.cameraPosition.zoom) }
//            }
//
//            map?.setStyle(Style.MAPBOX_STREETS) { style ->
//                this@MapView.style = style
//
//                style.transition = TransitionOptions(0, 0, false)
//
//                style.addSource(
//                    GeoJsonSource(
//                        ROUTE_SOURCE_ID,
//                        FeatureCollection.fromFeatures(arrayOf())
//                    )
//                )
//                style.addSource(
//                    GeoJsonSource(
//                        ROUTE_CURRENT_LOCATION_SOURCE_ID,
//                        FeatureCollection.fromFeatures(arrayOf())
//                    )
//                )
//
//                routeLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID)
//                val routeCurrentLocationLayer = LineLayer(
//                    ROUTE_CURRENT_LOCATION_LAYER_ID,
//                    ROUTE_CURRENT_LOCATION_SOURCE_ID
//                )
//
//                routeLayer.setProperties(
//                    PropertyFactory.lineWidth(3f),
//                    PropertyFactory.lineTranslate(arrayOf(0f, 4f)),
//                    PropertyFactory.lineDasharray(arrayOf(1.2f, 1.2f)),
//                    PropertyFactory.lineColor(Color.parseColor("#3887be"))
//                )
//
//                routeCurrentLocationLayer.setProperties(
//                    PropertyFactory.lineWidth(3f),
//                    PropertyFactory.lineTranslate(arrayOf(0f, 4f)),
//                    PropertyFactory.lineDasharray(arrayOf(1.2f, 1.2f)),
//                    PropertyFactory.lineColor(Color.parseColor("#FF5252"))
//                )
//
//                style.addLayer(routeCurrentLocationLayer)
//
//                mapLoadListener?.invoke()
//            }
//
//            setOnMapClickListener()
//        }
//    }
//
//    private fun setOnMapClickListener() {
//        map?.addOnMapClickListener { point ->
//            val pixel = map?.projection?.toScreenLocation(point)
//
//            if (pixel != null) {
//                val features = map?.queryRenderedFeatures(pixel)
//
//                if (!features.isNullOrEmpty()) {
//                    run loop@{
//                        features.forEach { feature ->
//                            if (feature.properties()?.get("poiId") != null) {
//                                val annotation = Gson().fromJson(feature.properties(), MapStep::class.java)
//
//                                mapItemClickListener?.invoke(annotation)
//
//                                return@loop
//                            }
//                        }
//                    }
//                }
//            }
//
//            true
//        }
//    }
//
//    fun showMapIcons(items: List<MapStep>) {
//        try {
//            style?.removeLayer(routeLayer)
//
//            mapItems.addAll(items)
//
//            if (items.isNotEmpty()) {
//                style?.addLayer(routeLayer)
//            }
//
//            items.forEach { item ->
//                if (item.coordinate != null && item.coordinate!!.lng != -1.0 && item.coordinate!!.lat != -1.0) {
//                    val uniq = item.group + item.poiId
//
//                    val properties: JsonObject = Gson().fromJson(Gson().toJson(item), JsonElement::class.java).asJsonObject
//
//                    if (style?.getSource(uniq) == null) {
//                        style?.addSource(
//                            GeoJsonSource(
//                                uniq,
//                                Feature.fromGeometry(Point.fromLngLat(item.coordinate!!.lng, item.coordinate!!.lat), properties),
//                                GeoJsonOptions()
//                            )
//                        )
//                    } else {
//                        (Objects.requireNonNull(style?.getSource(uniq)) as GeoJsonSource)
//                            .setGeoJson(Feature.fromGeometry(Point.fromLngLat(item.coordinate!!.lng, item.coordinate!!.lat), properties))
//                    }
//
//                    val view = com.tripian.trpcore.domain.model.MarkerView(context)
//
//                    if (item.markerIcon != -1) {
//                        view.iconView.setImageResource(item.markerIcon)
//                    }
//
//                    if (item.isOffer) {
//                        view.iconViewBackground.visibility = View.VISIBLE
//                    } else {
//                        view.iconViewBackground.visibility = View.GONE
//                    }
//
//                    if (item.position != -1) {
//                        view.poiOrderTv.text = item.position.toString()
//                        view.poiOrderTv.visibility = View.VISIBLE
//                    } else {
//                        view.poiOrderTv.visibility = View.GONE
//                    }
//
//                    val bitmap: Bitmap = view.getBitmap()
//
//                    style?.addImage(uniq, bitmap)
//
//                    if (style?.getLayer(uniq) == null) {
//                        if (TextUtils.equals(item.group, "step")) {
//                            style?.addLayer(
//                                SymbolLayer(uniq, uniq)
//                                    .withProperties(
//                                        iconImage(uniq),
//                                        PropertyFactory.iconIgnorePlacement(true),
//                                        PropertyFactory.iconAllowOverlap(true)
//                                    )
//                            )
//                        } else {
//                            style?.addLayerBelow(
//                                SymbolLayer(uniq, uniq)
//                                    .withProperties(
//                                        iconImage(uniq),
//                                        PropertyFactory.iconIgnorePlacement(true),
//                                        PropertyFactory.iconAllowOverlap(true)
//                                    ), "step" + mapItems[0].poiId
//                            )
//                        }
//                    }
//                }
//            }
//        } catch (_: Exception) {
//        }
//    }
//
//    fun moveCameraTo() {
//        try {
//            val latLngList: MutableList<LatLng> = ArrayList()
//            for (i in mapItems.indices) {
//                if (mapItems[i].coordinate != null && mapItems[i].coordinate!!.lat != -1.0 && mapItems[i].coordinate!!.lng != -1.0) {
//                    latLngList.add(LatLng(mapItems[i].coordinate!!.lat, mapItems[i].coordinate!!.lng))
//                }
//            }
//
//            if (latLngList.size > 1) {
//                val latLngBounds = LatLngBounds.Builder()
//                    .includes(latLngList)
//                    .build()
//
//                val padding = dp2Px(32f).toInt()
//                val paddingLarge = dp2Px(80f).toInt()
//
//                map?.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, padding, padding, padding, paddingLarge))
//            } else {
//                if (latLngList.isNotEmpty()) {
//                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngList[0], 12.0))
//                }
//            }
//        } catch (_: Exception) {
//        }
//    }
//
//    fun moveCameraTo(location: Location?, zoom: Double? = null) {
//        location?.let {
//            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), zoom ?: 16.0))
//        }
//    }
//
//    fun clearMap(items: List<MapStep>?) {
//        items?.forEach { item ->
//            clearItem(item.group + item.poiId)
//        }
//    }
//
//    fun clearMap() {
//        mapItems.forEach { item ->
//            clearItem(item.group + item.poiId)
//        }
//
//        mapItems.clear()
//    }
//
//    private fun clearItem(uniq: String) {
//        style?.removeLayer(uniq)
//        style?.removeSource(uniq)
//        style?.removeImage(uniq)
//    }
//
//    fun showRoute(route: DirectionsRoute) {
//        val source = style?.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
//
//        source?.setGeoJson(
//            FeatureCollection.fromFeature(
//                Feature.fromGeometry(
//                    LineString.fromPolyline(
//                        route.geometry()!!,
//                        Constants.PRECISION_6
//                    )
//                )
//            )
//        )
//    }
//
//    fun setOnMapLoadListener(task: () -> Unit) {
//        mapLoadListener = task
//    }
//
//    fun setOnZoomLevelListener(task: (Double) -> Unit) {
//        mapZoomLevelListener = task
//    }
//
//    fun setOnMapClickListener(task: (MapStep) -> Unit) {
//        mapItemClickListener = task
//    }
//
//    @SuppressLint("MissingPermission")
//    fun enableLocation() {
//        if (PermissionsManager.areLocationPermissionsGranted(context)) {
//            val locationComponentOptions = LocationComponentOptions.builder(context)
//                .layerBelow(routeLayer.id)
//                .bearingTintColor(Color.BLUE)
//                .build();
//
//            val locationComponentActivationOptions = LocationComponentActivationOptions
//                .builder(context, style!!)
//                .locationComponentOptions(locationComponentOptions)
//                .build();
//
//            locationComponent = map?.locationComponent
//            locationComponent?.activateLocationComponent(locationComponentActivationOptions)
//            locationComponent?.isLocationComponentEnabled = true
//            locationComponent?.renderMode = RenderMode.GPS
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    fun disableLocation() {
//        locationComponent?.isLocationComponentEnabled = false
//    }
//
//    fun redirectRoute(route: DirectionsRoute) {
//        val source = style?.getSourceAs<GeoJsonSource>(ROUTE_CURRENT_LOCATION_SOURCE_ID)
//
//        source?.setGeoJson(
//            FeatureCollection.fromFeature(
//                Feature.fromGeometry(
//                    LineString.fromPolyline(
//                        route.geometry()!!,
//                        Constants.PRECISION_6
//                    )
//                )
//            )
//        )
//    }
//
//    fun getBounds(): LatLngBounds {
//        return map!!.projection.visibleRegion.latLngBounds
//    }
//
//    fun getDistance(): Double {
//        return map!!.cameraPosition.target.distanceTo(map!!.projection.visibleRegion.latLngBounds.northEast) / 1000
//    }
//}