package com.tripian.trpcore.domain.model

import com.tripian.one.api.cities.model.City

/**
 * Created by semihozkoroglu on 12.07.2021.
 */
class CitySelect : BaseModel() {
    var city: City? = null
    var title: String? = null
    var imageId: Int = 0
}