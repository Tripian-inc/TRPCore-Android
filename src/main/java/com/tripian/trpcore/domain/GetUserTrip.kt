package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.di.modules.mytrip.MyTripScope
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.util.extensions.afterToday
import com.tripian.trpcore.util.extensions.today
import com.tripian.one.api.trip.model.TripsResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
@MyTripScope
class GetUserTrip @Inject constructor(val repository: TripRepository) : BaseUseCase<TripsResponse, GetUserTrip.Params>() {

    class Params(val isUpComing: Boolean, val useCache: Boolean = false)

    private var isUpComing = false
    private var upComingTrips: TripsResponse? = null
    private var pastTrips: TripsResponse? = null

    override fun on(params: Params?) {
        isUpComing = params!!.isUpComing

        if (params.useCache && isUpComing && upComingTrips != null) {
            onSendSuccess(upComingTrips!!)
        } else if (params.useCache && !isUpComing && pastTrips != null) {
            onSendSuccess(pastTrips!!)
        }

        addObservable {
            if (isUpComing) {
                repository.getUserTrip(from = today(), to = null, limit = 20, page = null)
            } else {
                repository.getUserTrip(from = null, to = afterToday(), limit = 20, page = null)
            }
        }
    }

    override fun onSendSuccess(t: TripsResponse) {
        super.onSendSuccess(t)

        if (isUpComing) {
            upComingTrips = t
        } else {
            pastTrips = t
        }
    }
}