package com.tripian.trpcore.domain.model

import com.tripian.one.api.bookings.model.Reservation

/**
 * Created by semihozkoroglu on 30.09.2020.
 */
class PlaceItem : BaseModel() {
    var id: String = ""
    var stepId: Int = -1
    var image: String? = ""
    var title: String = ""
    var description: String? = ""
    var partOfDays = arrayListOf<Int>()
    var match: String? = ""
    var ratingCount: Int = -1
    var rating: Float = -1f
    var category: String? = null
    var reservation: Reservation? = null
}