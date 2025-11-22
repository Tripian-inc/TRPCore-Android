package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.PlanRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.trip.model.PlanResponse
import com.tripian.one.api.trip.model.isGenerated
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class FetchPlan @Inject constructor(val repository: PlanRepository, val tripModelRepository: TripModelRepository) : BaseUseCase<PlanResponse, FetchPlan.Params>() {

    private var planId: Int? = null

    class Params(val planId: Int, val delay: Long = 0)

    override fun on(params: Params?) {
        planId = params!!.planId

        addObservable {
            repository.fetchPlan(params.planId)
                .delay(params.delay, TimeUnit.SECONDS)
        }
    }

    override fun onSendSuccess(t: PlanResponse) {
        if (t.data != null && t.data!!.isGenerated()) {
            tripModelRepository.dailyPlan = t.data

            super.onSendSuccess(t)
        } else {
            this@FetchPlan.on(Params(planId!!, delay = 2))
        }
    }
}