package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.util.extensions.poi2MapStep
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetMapStepAlternatives @Inject constructor(val tripModelRepository: TripModelRepository, val poiRepository: PoiRepository) : BaseUseCase<List<MapStep>, GetMapStepAlternatives.Params>() {


    class Params(val poiIds: List<String>?)

    override fun on(params: Params?) {
        addObservable {
            val poiIds = if (params?.poiIds == null) {
                tripModelRepository.dailyPlan?.steps?.flatMap { m -> m.alternatives ?: arrayListOf() }
            } else {
                params.poiIds
            }

            poiRepository.getPoiAlternatives(poiIds!!).map {
                val items = ArrayList<MapStep>()

                it.data?.forEach { poi ->
                    items.add(poi2MapStep(poi, alternative = true).apply {
                        this.order = -1
                        this.planDate = tripModelRepository.dailyPlan?.date
                    })
                }

                items
            }
        }
    }
}