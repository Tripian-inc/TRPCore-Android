package com.tripian.trpcore.domain

import com.tripian.one.api.bookings.model.ReservationsResponse
import com.tripian.one.api.pois.model.PoisResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.Service
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.repository.convertToPlaceItem
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetBookingPlace @Inject constructor(val service: Service, val tripModelRepository: TripModelRepository, val poiRepository: PoiRepository) :
    BaseUseCase<ArrayList<PlaceItem>, GetBookingPlace.Params>() {

    class Params(val cityId: Int?)

    override fun on(params: Params?) {
        if (params!!.cityId == null) {
            onSendError(ErrorModel("City id cannot be blank. Please contact your administrator"))
        } else {
            addObservable {
                service.getUserReservation(tripModelRepository.trip!!.city!!.id.toString()).flatMap { userReservation ->
                    poiRepository.getPoiAlternatives(userReservation.data!!.map { it.poiId }).map { poiResponse ->
                        poi2Place(userReservation, poiResponse)
                    }
                }
            }
        }
    }

    private fun poi2Place(userReservation: ReservationsResponse, poiResponse: PoisResponse): ArrayList<PlaceItem> {
        val res = ArrayList<PlaceItem>()

        poiResponse.data?.forEach { poi ->
            if (!res.any { it.id == poi.id }) {
                res.add(poi.convertToPlaceItem().apply {
                    stepId = tripModelRepository.getStepId(poi.id) ?: 0
                    reservation = userReservation.data?.find { it.poiId == poi.id }
                })
            }
        }

        return res
    }
}