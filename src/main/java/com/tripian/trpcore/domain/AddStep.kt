package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.StepRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.trip.model.StepResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class AddStep @Inject constructor(val repository: StepRepository, val tripModelRepository: TripModelRepository, val fetchPlan: FetchPlan) : BaseUseCase<StepResponse, AddStep.Params>() {

    class Params(val poiId: String)

    override fun on(params: Params?) {
        addObservable { repository.addStep(tripModelRepository.dailyPlan!!.id!!, params!!.poiId) }
    }

    override fun onSendSuccess(t: StepResponse) {
        fetchPlan.on(FetchPlan.Params(tripModelRepository.dailyPlan!!.id!!), success = {
            super.onSendSuccess(t)
        }, error = {
            super.onSendError(it)
        })
    }
}