package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.trip.model.Plan
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetTripPlans @Inject constructor(val tripModelRepository: TripModelRepository) : BaseUseCase<Pair<Int, List<Plan>>, Unit>() {

    override fun on(params: Unit?) {
        onSendSuccess(
            Pair(
                tripModelRepository.trip!!.plans!!.indexOfFirst { it.id == tripModelRepository.dailyPlan?.id },
                tripModelRepository.trip!!.plans!!
            )
        )
    }
}