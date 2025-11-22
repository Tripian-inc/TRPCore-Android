package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.repository.convertToPlaceItem
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetPlacesInTrip @Inject constructor(
    val tripModelRepository: TripModelRepository,
    val repository: TripRepository
) : BaseUseCase<List<PlaceItem>, GetPlacesInTrip.Params>() {

    class Params(val categoryIds: List<Int>)

    override fun on(params: Params?) {
        val categories = params?.categoryIds!!

        val res = ArrayList<PlaceItem>()

        var inDay = 1

        tripModelRepository.trip?.plans?.forEach { plan ->
            plan.steps?.forEach { step ->
                step.poi?.category?.let { poiCategories ->
                    val poiCategoryIds = poiCategories.map { it.id }
                    if ((categories intersect poiCategoryIds.toSet()).isNotEmpty()) {

                        if (res.any { it.id == step.poi!!.id }) {
                            res.first { it.id == step.poi!!.id }.partOfDays.add(inDay)
                        } else {
                            res.add(step.poi!!.convertToPlaceItem().apply {
                                stepId = step.id
                                partOfDays.add(inDay)
                                match = step.score
                            })
                        }

                    }
                }
            }

            inDay++
        }

        onSendSuccess(res)
    }
}