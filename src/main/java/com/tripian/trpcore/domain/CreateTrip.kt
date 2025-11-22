package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.util.extensions.formatDateString
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.companion.model.Companion
import com.tripian.one.api.trip.model.*
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class CreateTrip @Inject constructor(val repository: TripRepository) : BaseUseCase<TripResponse, CreateTrip.Params>() {

    class Params(
        var city: City? = null,
        var place: Accommodation? = null,
        var adult: Int,
        var child: Int,
        var arrivalDate: Long,
        var departureDate: Long,
        var arrivalTime: Long,
        var departureTime: Long,
        var pace: String? = null,
        var companions: List<Companion>? = null,
        var answers: List<Int>? = null
    )

    override fun on(params: Params?) {
        addObservable {
            val answers = ArrayList<Int>()

            if (params?.answers != null) {
                answers.addAll(params.answers!!)
            }

            tripianUserRepository.user?.answers?.forEach { u ->
                if (!answers.contains(u) && u != 0) {
                    answers.add(u)
                }
            }

            repository.createTrip(
                TripRequest(
                    params!!.city!!.id!!,
                    formatDateString(params.arrivalDate, params.arrivalTime),
                    formatDateString(params.departureDate, params.departureTime),
                    params.adult, params.child, params.place, answers, params.companions?.map { c -> c.id!! }, theme = null,
                )
            )
        }
    }
}