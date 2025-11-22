package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.trip.model.Plan
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetDailyPlan @Inject constructor(val tripModelRepository: TripModelRepository) : BaseUseCase<Plan, Unit>() {

    override fun on(params: Unit?) {
        if (tripModelRepository.dailyPlan != null) {
            onSendSuccess(tripModelRepository.dailyPlan!!)
        }

        addObservable { tripModelRepository.getDailyPlanEmitter() }
    }
}