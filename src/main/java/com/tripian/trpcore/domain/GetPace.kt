package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.Pace
import com.tripian.trpcore.repository.response.PaceResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetPace @Inject constructor() : BaseUseCase<PaceResponse, Unit>() {

    override fun on(params: Unit?) {
        val response = PaceResponse()
        response.data = ArrayList()
        response.data!!.add(Pace(1, "Slow", "SLOW"))
        response.data!!.add(Pace(2, "Normal", "NORMAL"))
        response.data!!.add(Pace(3, "Fast", "FAST"))

        onSendSuccess(response)
    }
}