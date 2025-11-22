package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripRepository
import com.tripian.one.api.trip.model.DeleteResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class DeleteTrip @Inject constructor(val repository: TripRepository) : BaseUseCase<DeleteResponse, DeleteTrip.Params>() {

    class Params(var tripHash: String)

    override fun on(params: Params?) {
        addObservable { repository.deleteTrip(params!!.tripHash) }
    }
}