package com.tripian.trpcore.repository.response

import com.tripian.trpcore.domain.model.Pace
import com.tripian.trpcore.repository.base.ResponseModelBase

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class PaceResponse : ResponseModelBase() {

    var data: ArrayList<Pace>? = null

}