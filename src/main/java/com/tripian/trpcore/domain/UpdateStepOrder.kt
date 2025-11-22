package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.StepRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.trip.model.StepResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class UpdateStepOrder @Inject constructor(val stepRepository: StepRepository, val tripModelRepository: TripModelRepository, val fetchPlan: FetchPlan) :
    BaseUseCase<StepResponse, UpdateStepOrder.Params>() {

    class Params(val stepId: Int, val poiId: String?, val order: Int?)

    override fun on(params: Params?) {
        addObservable { stepRepository.updateStep(params!!.stepId, params.poiId, params.order) }
    }

    override fun onSendSuccess(t: StepResponse) {
        fetchPlan.on(FetchPlan.Params(tripModelRepository.dailyPlan!!.id), success = {
            super.onSendSuccess(t)
        }, error = {
            super.onSendError(it)
        })
    }
}