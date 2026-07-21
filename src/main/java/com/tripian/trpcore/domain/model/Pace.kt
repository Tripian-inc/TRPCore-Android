package com.tripian.trpcore.domain.model

data class Pace(
    val id: Int,
    val paceName: String,
    val paceSymbol: String
) : BaseModel()