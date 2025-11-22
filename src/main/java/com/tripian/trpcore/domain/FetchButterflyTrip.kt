package com.tripian.trpcore.domain

import com.tripian.one.api.trip.model.TripResponse
import com.tripian.one.api.trip.model.isGenerated
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.repository.TripRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class FetchButterflyTrip @Inject constructor(val repository: TripRepository, val tripModelRepository: TripModelRepository) : BaseUseCase<TripResponse, FetchButterflyTrip.Params>() {

    private var tripHash: String? = null
    private var currentPlanIndex = 0

    private var requestCount = 0

    class Params(val tripHash: String, val delay: Long = 0)

    override fun on(params: Params?) {
        tripHash = params!!.tripHash

        addObservable {
            repository.fetchTrip(params.tripHash)
                .delay(params.delay, TimeUnit.SECONDS)
        }
    }

    override fun onSendSuccess(t: TripResponse) {
        if (t.data != null && t.data!!.plans != null &&
            t.data!!.plans?.get(currentPlanIndex) != null &&
            t.data!!.plans!![currentPlanIndex].isGenerated()
        ) {
            tripModelRepository.trip = t.data

            super.onSendSuccess(t)

            currentPlanIndex++

            if (t.data!!.plans!!.size > currentPlanIndex) {
                for (i in currentPlanIndex until t.data!!.plans!!.size) {
                    if (!t.data!!.plans!![i].isGenerated()) {
                        currentPlanIndex = i

                        this@FetchButterflyTrip.on(Params(tripHash!!, delay = 2))
                        break
                    }
                }
            }
        } else {
            if (requestCount++ == 10 && currentPlanIndex == 0) {
                super.onSendSuccess(t)
                return
            }
            this@FetchButterflyTrip.on(Params(tripHash!!, delay = 2))
        }
    }
}