package com.tripian.trpcore.repository.base

import com.tripian.trpcore.domain.model.BaseModel

/**
 * Created by semihozkoroglu on 2019-09-09.
 */
open class ResponseModelBase : BaseModel() {
    var status: Int? = null
    var message: String? = null
}