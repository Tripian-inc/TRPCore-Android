package com.tripian.trpcore.domain.model

import com.tripian.trpcore.util.Category
import com.tripian.one.api.reactions.model.Reaction
import com.tripian.one.api.trip.model.Step

/**
 * Created by semihozkoroglu on 30.08.2020.
 */
class ButterflyItem : BaseModel() {
    var day: String? = null
    var category: Category? = null
    var step: Step? = null
    var reaction: Reaction? = null

    var isLikeSelected = false
    var isDislikeSelected = false
    var isClosed = false
}