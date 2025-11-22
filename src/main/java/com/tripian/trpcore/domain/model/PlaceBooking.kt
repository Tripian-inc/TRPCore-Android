package com.tripian.trpcore.domain.model

import com.tripian.gyg.domain.model.ExperiencesItem
import com.tripian.one.api.pois.model.Booking


/**
 * Created by semihozkoroglu on 30.09.2020.
 */
class PlaceBooking : BaseModel() {
    var yelp: Booking? = null
    var openTable: Booking? = null
    var getYourGuide: Booking? = null
}