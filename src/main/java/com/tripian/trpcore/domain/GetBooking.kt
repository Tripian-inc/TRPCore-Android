package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.Service
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.bookings.model.Reservation
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetBooking @Inject constructor(val service: Service, val tripModelRepository: TripModelRepository, val poiRepository: PoiRepository) : BaseUseCase<List<Reservation>?, GetBooking.Params>() {

    class Params(val cityId: Int?)

    override fun on(params: Params?) {
        addObservable {
            service.getUserReservation(params!!.cityId?.toString()!!).map { it.data ?: arrayListOf() }
        }
    }
}