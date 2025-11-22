package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.PlaceBooking
import com.tripian.trpcore.repository.FavoriteRepository
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.favorites.model.FavoriteResponse
import com.tripian.one.api.pois.model.PoiResponse
import com.tripian.one.api.trip.model.Step
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetBookingInfo @Inject constructor(val tripModelRepository: TripModelRepository, val poiRepository: PoiRepository, val favoriteRepository: FavoriteRepository) :
    BaseUseCase<PlaceBooking, GetBookingInfo.Params>() {

    class Params(val poiId: String)

    override fun on(params: Params?) {
        addObservable {
            val trip = tripModelRepository.trip!!

            var foundedStep: Step? = null
            var inDay = 0
            val cityId = trip.city!!.id
            val poiId = params!!.poiId

            run loop@{
                trip.plans?.forEach { plan ->
                    plan.steps?.forEach { step ->
                        if (poiId == step.poi!!.id) {
                            inDay++

                            return@loop
                        }
                    }

                    inDay++
                }
            }

            tripModelRepository.dailyPlan?.steps?.forEach {
                if (poiId == it.poi!!.id) {
                    foundedStep = it

                    return@forEach
                }
            }

            if (foundedStep == null) {
                Observable.zip(
                    poiRepository.getPoiInfo(poiId),
                    favoriteRepository.getFavorite(cityId, params.poiId)
                ) { t1, t2 ->
                    poi2Booking(t1, t2)
                }
            } else {
                Observable.zip(
                    Observable.just(PoiResponse().apply {
                        data = foundedStep!!.poi
                    }),
                    favoriteRepository.getFavorite(cityId, poiId)
                ) { t1, t2 ->
                    poi2Booking(t1, t2)
                }
            }
        }
    }

    private fun poi2Booking(poiInfo: PoiResponse, favInfo: FavoriteResponse?): PlaceBooking {
        val booking = PlaceBooking()

        val poi = poiInfo.data!!

        val bookings = poi.bookings

        if (bookings != null && bookings.isNotEmpty()) {
            for (b in bookings) {
                if (b.providerId == 2 || b.providerName?.lowercase() == "yelp") { // YELP
                    booking.yelp = b
                } else if (b.providerId == 5 || b.providerName?.lowercase() == "opentable") { // OPEN TABLE
                    booking.openTable = b
                } else if (b.providerId == 4 || b.providerName?.lowercase() == "getyourguide") {
                    booking.getYourGuide = b
                }
            }
        }

        return booking
    }
}