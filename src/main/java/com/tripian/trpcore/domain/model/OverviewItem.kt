package com.tripian.trpcore.domain.model

/**
 * Created by semihozkoroglu on 30.09.2020.
 */
class OverviewItem : BaseModel() {
    var items = ArrayList<Butterfly>()
    var generated: Boolean = false
}