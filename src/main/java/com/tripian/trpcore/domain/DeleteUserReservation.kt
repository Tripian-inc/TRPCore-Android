package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.Service
import com.tripian.one.api.trip.model.DeleteResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class DeleteUserReservation @Inject constructor(val service: Service) : BaseUseCase<DeleteResponse, DeleteUserReservation.Params>() {

    class Params(val reservationId: Int)

    override fun on(params: Params?) {
        addObservable {
            service.deleteUserReservation(params!!.reservationId)
        }
    }
}