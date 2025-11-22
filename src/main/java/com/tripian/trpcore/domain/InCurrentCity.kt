package com.tripian.trpcore.domain

import android.location.Location
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.util.extensions.isInCity
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class InCurrentCity @Inject constructor(val tripModelRepository: TripModelRepository) : BaseUseCase<Boolean, InCurrentCity.Params>() {

    class Params(val userLocation: Location)

    override fun on(params: Params?) {
        onSendSuccess(tripModelRepository.trip!!.city!!.isInCity(params!!.userLocation))
    }
}