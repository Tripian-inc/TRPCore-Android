package com.tripian.trpcore.repository.model

import com.tripian.trpcore.domain.model.BaseModel

/**
 * Created by semihozkoroglu on 17.04.2021.
 */
data class TokenModel constructor(
    val hms: Boolean,
    val pushToken: String? = null,
) : BaseModel()