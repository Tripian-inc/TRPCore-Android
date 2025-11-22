package com.tripian.trpcore.domain.model

import com.tripian.one.api.favorites.model.Favorite
import com.tripian.one.api.offers.model.Offer
import com.tripian.one.api.pois.model.Image
import com.tripian.trpcore.ui.trip_detail.Mode

/**
 * Created by semihozkoroglu on 30.09.2020.
 */
class PlaceDetail : BaseModel() {
    var id: String = ""
    var title: String? = null
    var images: List<Image>? = null
    var partOfDay: Int = 0
    var match: String? = null
    var ratingCount: Int = -1
    var rating: Float = -1f
    var price: Int = -1
    var mustTry: String? = null
    var cuisines: String? = null
    var tags: String? = null
    var hours: List<OpenHour>? = null
    var phone: String? = null
    var webSite: String? = null
    var address: String? = null
    var attention: String? = null
    var description: String? = null
    var favorite: Favorite? = null
    var mapStep: MapStep? = null
    var mode: Mode? = null
    var offers: List<Offer>? = null
}