package com.tripian.trpcore.domain.model

/**
 * Created by semihozkoroglu on 30.08.2020.
 */
class Butterfly : BaseModel() {
    var title: String? = null
    var date: String? = null
    var description: String? = null
    var items = ArrayList<ButterflyItem>()
    var isAdded = false
}