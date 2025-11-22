package com.tripian.trpcore.util.event

import com.tripian.trpcore.domain.model.BaseModel

data class EventMessage<T>(var tag: String? = null, var value: T? = null) : BaseModel()