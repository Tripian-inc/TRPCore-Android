package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.formatDateDay
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetPlanDay @Inject constructor(val tripModelRepository: TripModelRepository) :
    BaseUseCase<Pair<Int, String>, Unit>() {

    override fun on(params: Unit?) {
        addObservable {
            tripModelRepository.getDailyPlanEmitter().map { currentPlan ->
                var dayCount = 1

                var ret = ""

                tripModelRepository.trip?.plans?.forEach {
                    if (it.id == currentPlan.id) {
                        ret =
                            "${miscRepository.getLanguageValueForKey(LanguageConst.DAY)} $dayCount - ${currentPlan.date?.formatDateDay()}"

                        return@forEach
                    }

                    dayCount++
                }

                Pair(currentPlan.id, ret)
            }
        }
    }
}