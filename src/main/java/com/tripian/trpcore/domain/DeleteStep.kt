package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.StepRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.trip.model.DeleteResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class DeleteStep @Inject constructor(val repository: StepRepository, val tripModelRepository: TripModelRepository, val fetchPlan: FetchPlan) : BaseUseCase<DeleteResponse, DeleteStep.Params>() {

    class Params(val stepId: Int)

    override fun on(params: Params?) {
        addObservable { repository.deleteStep(params!!.stepId) }
    }

    override fun onSendSuccess(t: DeleteResponse) {
        tripModelRepository.deleteStep(t.data?.recordId)

        super.onSendSuccess(t)
    }
}