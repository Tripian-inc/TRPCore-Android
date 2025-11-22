package com.tripian.trpcore.ui.trip_detail

import com.tripian.trpcore.domain.model.BaseModel

/**
 * Created by semihozkoroglu on 7.10.2020.
 */
class TripDetail : BaseModel() {
    var poiId: String = ""

    // if coming from butterfly disable action
    var butterflyMode = false

    // change
    var stepId: Int = -1
    var order: Int = -1
}