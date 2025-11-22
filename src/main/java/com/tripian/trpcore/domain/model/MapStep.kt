package com.tripian.trpcore.domain.model

import com.tripian.one.api.pois.model.Booking
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.reactions.model.Reaction
import com.tripian.one.api.trip.model.StepHours

/**
 * Created by semihozkoroglu on 15.09.2020.
 */
class MapStep : BaseModel() {
    var homeBase: Boolean = false
    var poiId: String = ""
    var poi: Poi? = null
    var name: String? = ""
    var description: String? = ""
    var stepId: Int = -1 // null for alternative
    var markerIcon: Int = -1
    var order: Int = -1
    var position: Int = -1
    var coordinate: Coordinate? = null
    var leg: MapLeg? = null
    var group: String = ""
    var image: String? = ""
    var category: String? = ""
    var price: Int = -1
    var rating: Float = -1f
    var ratingCount: Int = -1
    var times: StepHours? = null
    var alternatives: List<String>? = null
    var reaction: Reaction? = null
    var isOffer: Boolean = false
    var bookingProducts: List<Booking>? = null
    var isCustomPoi: Boolean = false
    var planDate: String? = null

    fun isRatingAvailable(): Boolean {
        return rating != -1f && ratingCount > 0
    }
}

data class UberModel(
    val pickupLocation: Coordinate,
    val pickupName: String,
    val pickupAddress: String,
    val dropoffLocation: Coordinate,
    val dropOffName: String,
    val dropOffAddress: String
)

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