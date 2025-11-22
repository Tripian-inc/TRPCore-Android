package com.tripian.trpcore.ui.trip.places

import com.tripian.trpcore.domain.model.BaseModel
import com.tripian.one.api.trip.model.Trip

/**
 * Created by semihozkoroglu on 21.08.2020.
 */
class PageData : BaseModel() {
    var tripHash: String = ""
    var trip: Trip? = null
}