package com.tripian.trpcore.repository.base

import com.tripian.trpcore.domain.model.BaseModel

open class ResponseModelBase : BaseModel() {
    var status: Int? = null
    var message: String? = null
}