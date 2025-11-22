package com.tripian.trpcore.domain

import android.text.TextUtils
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.repository.TripRepository
import com.tripian.one.api.trip.model.TripResponse
import com.tripian.one.api.trip.model.isGenerated
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class FetchTrip @Inject constructor(val repository: TripRepository, val tripModelRepository: TripModelRepository) :
    BaseUseCase<TripResponse, FetchTrip.Params>() {

    private var tripHash: String? = null

    class Params(val tripHash: String, val delay: Long = 0)

    override fun on(params: Params?) {
        tripHash = params!!.tripHash

        if (TextUtils.equals(tripModelRepository.trip?.tripHash, tripHash) && tripModelRepository.trip?.isGenerated()!!) {
            addObservable {
                Observable.just(TripResponse().apply {
                    data = tripModelRepository.trip
                    status = 200
                }).delay(1, TimeUnit.SECONDS)
            }
        } else {
            addObservable {
                repository.fetchTrip(params.tripHash)
                    .delay(params.delay, TimeUnit.SECONDS)
            }
        }
    }

    override fun onSendSuccess(t: TripResponse) {
        if (t.data != null && t.data!!.isGenerated()) {
            tripModelRepository.trip = t.data

            super.onSendSuccess(t)
        } else {
            this@FetchTrip.on(Params(tripHash!!, delay = 2))
        }
    }
}