package com.tripian.trpcore.repository.model

import com.tripian.trpcore.domain.model.BaseModel

data class TokenModel constructor(
    val hms: Boolean,
    val pushToken: String? = null,
) : BaseModel()