package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripModelRepository
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class ChangeDailyPlan @Inject constructor(val tripModelRepository: TripModelRepository) : BaseUseCase<Unit, ChangeDailyPlan.Params>() {

    class Params(val planId: Int)

    override fun on(params: Params?) {
        tripModelRepository.trip?.plans?.forEach {
            if (it.id == params?.planId) {
                // TODO: plan icin generated status'a bakilacak
                tripModelRepository.dailyPlan = it

                onSendSuccess(Unit)
            }
        }
    }
}