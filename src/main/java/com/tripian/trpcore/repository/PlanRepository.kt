package com.tripian.trpcore.repository

import com.tripian.one.api.trip.model.ExportPlanRequest
import com.tripian.one.api.trip.model.ExportPlanResponse
import com.tripian.one.api.trip.model.PlanResponse
import com.tripian.one.api.trip.model.UpdatePlanRequest
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 20.09.2020.
 */
class PlanRepository @Inject constructor(val service: Service) {

    fun updateTime(planId: Int, startTime: String, endTime: String): Observable<PlanResponse> {
        return service.updatePlan(planId, UpdatePlanRequest().apply {
            this.startTime = startTime
            this.endTime = endTime
        })
    }

    fun updatePlanOrder(planId: Int, stepOrders: List<Int>): Observable<PlanResponse> {
        return service.updatePlan(planId, UpdatePlanRequest().apply {
            this.stepOrders = stepOrders
        })
    }

    fun fetchPlan(planId: Int): Observable<PlanResponse> {
        return service.fetchPlan(planId)
    }

    fun exportPlan(planId: Int, tripHash: String): Observable<ExportPlanResponse> {
        return service.exportPlan(ExportPlanRequest(planId, tripHash))
    }
}