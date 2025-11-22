package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.Service
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.bookings.model.ReservationsResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetUserReservation @Inject constructor(val service: Service, val tripModelRepository: TripModelRepository) : BaseUseCase<ReservationsResponse, Unit>() {

    override fun on(params: Unit?) {
        addObservable {
            service.getUserReservation(tripModelRepository.trip!!.city!!.id.toString())
        }
    }
}