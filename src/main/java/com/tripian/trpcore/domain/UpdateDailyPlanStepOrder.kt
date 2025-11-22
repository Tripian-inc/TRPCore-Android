package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.PlanRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.trip.model.PlanResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class UpdateDailyPlanStepOrder @Inject constructor(
    val repository: PlanRepository,
    val tripModelRepository: TripModelRepository,
    val fetchPlan: FetchPlan
) :
    BaseUseCase<PlanResponse, UpdateDailyPlanStepOrder.Params>() {

    class Params(val stepOrders: List<Int>)

    override fun on(params: Params?) {
        addObservable {
            repository.updatePlanOrder(
                tripModelRepository.dailyPlan!!.id,
                params!!.stepOrders,
            )
        }
    }

    override fun onSendSuccess(t: PlanResponse) {
        fetchPlan.on(FetchPlan.Params(t.data!!.id), success = {
            super.onSendSuccess(it)
        }, error = {
            super.onSendError(it)
        })
    }
}