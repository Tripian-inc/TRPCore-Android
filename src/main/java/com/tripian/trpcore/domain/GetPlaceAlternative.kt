package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.repository.convertToPlaceItem
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetPlaceAlternative @Inject constructor(
    val tripModelRepository: TripModelRepository,
    val poiRepository: PoiRepository
) : BaseUseCase<List<PlaceItem>, GetPlaceAlternative.Params>() {

    class Params(val categoryIds: List<Int>)

    override fun on(params: Params?) {
        addObservable {
            val categories = params?.categoryIds!!

            val poiIds = ArrayList<String>()

            tripModelRepository.trip?.plans?.forEach { m ->
                m.steps?.flatMap { t -> t.alternatives ?: arrayListOf() }?.let { poiIds.addAll(it) }
            }

            poiRepository.getPoiAlternatives(poiIds).map {
                val items = ArrayList<PlaceItem>()

                it.data?.forEach { poi ->
                    poi.category?.let { poiCategories ->
                        val poiCategoryIds = poiCategories.map { poiCategory -> poiCategory.id }
                        if ((categories intersect poiCategoryIds.toSet()).isNotEmpty()) {
                            items.add(poi.convertToPlaceItem())
                        }
                    }
                }

                items
            }
        }
    }
}