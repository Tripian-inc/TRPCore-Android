package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.PlanRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.trip.model.PlanResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class UpdateDailyPlan @Inject constructor(val repository: PlanRepository, val tripModelRepository: TripModelRepository, val fetchPlan: FetchPlan) :
    BaseUseCase<PlanResponse, UpdateDailyPlan.Params>() {

    class Params(val startTime: String, val endTime: String)

    override fun on(params: Params?) {
        addObservable { repository.updateTime(tripModelRepository.dailyPlan!!.id!!, params!!.startTime, params.endTime) }
    }

    override fun onSendSuccess(t: PlanResponse) {
        fetchPlan.on(FetchPlan.Params(t.data!!.id!!), success = {
            super.onSendSuccess(it)
        }, error = {
            super.onSendError(it)
        })
    }
}