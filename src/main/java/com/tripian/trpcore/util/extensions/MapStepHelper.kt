package com.tripian.trpcore.util.extensions

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.trip.model.Step
import com.tripian.trpcore.domain.model.MapStep

/**
 * Created by semihozkoroglu on 7.10.2020.
 */
fun step2MapStep(step: Step, alternative: Boolean = false): MapStep {
    val mapStep = step.poi?.let { poi2MapStep(it, alternative) } ?: run { MapStep() }
    return mapStep.apply {
        alternatives = step.alternatives
        times = step.times

        stepId = step.id
    }
}

fun poi2MapStep(poi: Poi, alternative: Boolean = false): MapStep {
    return MapStep().apply {
        group = if (alternative) "alternative" else "step"
        poiId = poi.id
        isCustomPoi = poiId.startsWith("c_")
        this.poi = poi
        name = poi.name
        description = poi.description
        image = poi.image?.url

        markerIcon = if (alternative) mapIcons[poi.icon] ?: -1 else mapIcons[poi.icon] ?: -1

        coordinate = Coordinate().apply {
            lat = poi.coordinate?.lat ?: 0.0
            lng = poi.coordinate?.lng ?: 0.0
        }
        category = if (poi.category != null && poi.category!!.isNotEmpty()) {
            poi.category!![0].name
        } else {
            ""
        }

        if (poi.enableRating()) {
            price = if (poi.price != null) {
                poi.price!!
            } else {
                -1
            }
            ratingCount = poi.ratingCount ?: 0
            rating = poi.rating
        } else {
            ratingCount = -1
            rating = -1f
            price = -1
        }
    }
}

fun MapStep.getHoursText(): String {
    var hourText = ""
    if (times?.from.isNullOrEmpty().not()) {
        hourText = times?.from!!
    }
    if (times?.to.isNullOrEmpty().not()) {
        hourText += " - " + times?.to!!
    }
    return hourText
}