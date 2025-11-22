package com.tripian.trpcore.domain

import com.tripian.one.api.trip.model.isGenerated
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.util.LanguageConst
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetTripWarning @Inject constructor(val tripModelRepository: TripModelRepository) : BaseUseCase<String, Unit>() {

    override fun on(params: Unit?) {
        addObservable {
            tripModelRepository.getDailyPlanEmitter().map { plan ->
                plan.statusMessage?.let {
                    return@map it
                }
                val index = tripModelRepository.trip?.plans?.indexOfFirst { it.id == plan.id }

                if (index != null) {
                    if (index == 0) {
                        if (!plan.isGenerated()) {
                            return@map miscRepository.getLanguageValueForKey(LanguageConst.NO_RECOMMENDATIONS_ARRIVAL)
                        }
                    } else if (index == tripModelRepository.trip?.plans?.size?.minus(1)) {
                        if (!plan.isGenerated()) {
                            return@map miscRepository.getLanguageValueForKey(LanguageConst.NO_RECOMMENDATIONS_DEPARTURE)
                        }
                    } else if (plan.steps.isNullOrEmpty()) {
                        return@map miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLACES)
                    }
                }

                return@map ""
            }
        }
    }
}