package com.tripian.trpcore.domain

import com.tripian.one.api.trip.model.ExportPlanResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.PlanRepository
import javax.inject.Inject

class ExportPlan @Inject constructor(private val planRepository: PlanRepository) :
    BaseUseCase<ExportPlanResponse, ExportPlan.Params>() {

    class Params(val planId: Int, val tripHash: String)

    override fun on(params: Params?) {
        addObservable {
            planRepository.exportPlan(
                planId = params?.planId ?: 0,
                tripHash = params?.tripHash ?: ""
            )
        }
    }
}
