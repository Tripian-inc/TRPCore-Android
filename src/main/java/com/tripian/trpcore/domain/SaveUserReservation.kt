package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.Service
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.gyg.repository.model.PayShoppingRes
import com.tripian.one.api.bookings.model.*
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class SaveUserReservation @Inject constructor(val service: Service, val tripModelRepository: TripModelRepository) : BaseUseCase<ReservationResponse, SaveUserReservation.Params>() {

    class Params(val reservation: PayShoppingRes)

    override fun on(params: Params?) {
        addObservable {
            service.saveUserReservation(ReservationRequest().apply {
                value = ReservationValue().apply {
                    data = ReservationData().apply {
                        shoppingCart = ShoppingCart().apply {
                            tourName = params!!.reservation.tourName
                            cityName = params.reservation.cityName
                            tourImage = params.reservation.tourImage
                            shoppingCartId = params.reservation.shoppingCartId
                            shoppingCartHash = params.reservation.shoppingCartHash
                            bookingHash = params.reservation.bookingHash
                            status = params.reservation.status
                            traveler = try {
                                gson.fromJson(gson.toJson(params.reservation.traveler), Traveler::class.java)
                            } catch (e: java.lang.Exception) {
                                null
                            }
                            billing = try {
                                gson.fromJson(gson.toJson(params.reservation.billing), Billing::class.java)
                            } catch (e: java.lang.Exception) {
                                null
                            }
                            paymentInfo = try {
                                gson.fromJson(gson.toJson(params.reservation.payInfo), PayInfo::class.java)
                            } catch (e: java.lang.Exception) {
                                null
                            }
                            bookings = params.reservation.bookings?.let {
                                try {
                                    arrayListOf(gson.fromJson(gson.toJson(params.reservation.payInfo), PayBooking::class.java))
                                } catch (e: java.lang.Exception) {
                                    arrayListOf()
                                }
                            }
                        }
                    }
                }
                provider = "GYG"
                key = "GYG"
                tripHash = tripModelRepository.trip?.tripHash
            })
        }
    }
}