package com.tripian.trpcore.domain

import android.location.Location
import android.text.TextUtils
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.MapLeg
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.util.MapBoxRouteCalculator
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetDirectionRoutes @Inject constructor() :
    BaseUseCase<DirectionsRoute, GetDirectionRoutes.Params>() {

    class Params(
        val origin: Location? = null,
        val destination: MapStep? = null,
        val steps: List<MapStep>? = null
    )

    override fun on(params: Params?) {
        addObservable {
            PublishSubject.create {
                val points: MutableList<Point> = ArrayList()

                if (params!!.origin != null && params.destination != null) {
                    params.origin.let { origin ->
                        if (origin.longitude != -1.0 && origin.latitude != -1.0) {
                            points.add(Point.fromLngLat(origin.longitude, origin.latitude))
                        }
                    }

                    params.destination.let { destination ->
                        if (destination.coordinate!!.lng != -1.0 && destination.coordinate!!.lat != -1.0) {
                            points.add(
                                Point.fromLngLat(
                                    destination.coordinate!!.lng,
                                    destination.coordinate!!.lat
                                )
                            )
                        }
                    }
                } else {
                    params.steps?.forEach { item ->
                        if (item.coordinate!!.lng != -1.0 && item.coordinate!!.lat != -1.0) {
                            points.add(
                                Point.fromLngLat(
                                    item.coordinate!!.lng,
                                    item.coordinate!!.lat
                                )
                            )
                        }
                    }
                }


                if (points.size > 1) {
                    val routeCalculator = MapBoxRouteCalculator()
                    routeCalculator.setOnLoadListener(object :
                        MapBoxRouteCalculator.OnLoadListener {
                        override fun onLoad(response: DirectionsResponse?) {
                            if (response?.routes() != null && response.routes().isNotEmpty()) {
                                val route = response.routes()[0]

                                params.steps?.let { steps ->
                                    val legs = route.legs()
                                    if (legs != null && legs.isNotEmpty()) {
                                        for (index in steps.indices) {
                                            if (legs.size > index) {
                                                steps[index].leg = MapLeg(
                                                    legs[index].distance(),
                                                    legs[index].duration()
                                                )
                                            }
                                        }
                                    }
                                }

                                it.onNext(route)
                            } else {
                                it.onError(ErrorModel())
                            }
                        }

                        override fun onMapBoxError(message: String?) {
                            if (!TextUtils.isEmpty(message)) {
                                it.onError(ErrorModel(errorDesc = message!!))
                            } else {
                                it.onError(ErrorModel())
                            }
                        }
                    })

                    routeCalculator.calculate(null, null, points)
                } else {
                    it.onError(ErrorModel())
                }
            }
        }
    }
}