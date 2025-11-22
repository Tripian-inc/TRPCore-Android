package com.tripian.trpcore.domain.model

/**
 * Created by semihozkoroglu on 30.08.2020.
 */
data class Pace(
    val id: Int,
    val paceName: String,
    val paceSymbol: String
) : BaseModel()