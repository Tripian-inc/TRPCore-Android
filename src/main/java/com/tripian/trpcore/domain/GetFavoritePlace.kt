package com.tripian.trpcore.domain

import com.tripian.one.api.pois.model.PoisResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.repository.FavoriteRepository
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.repository.convertToPlaceItem
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetFavoritePlace @Inject constructor(val tripModelRepository: TripModelRepository, val favoriteRepository: FavoriteRepository, val poiRepository: PoiRepository) : BaseUseCase<List<PlaceItem>, GetFavoritePlace.Params>() {

    class Params(val cityId: Int?)

    override fun on(params: Params?) {
        if (params!!.cityId == null) {
            onSendError(ErrorModel("City id cannot be blank. Please contact your administrator"))
        } else {
            addObservable {
                favoriteRepository.getUserFavorites(params.cityId!!).flatMap {
                    poiRepository.getPoiAlternatives(it.data!!.map { it.poiId!! }).map(::poi2Place)
                }
            }
        }
    }

    private fun poi2Place(it: PoisResponse): List<PlaceItem> {
        val res = ArrayList<PlaceItem>()

        it.data?.forEach { poi ->
            if (!res.any { it.id == poi.id }) {
                res.add(poi.convertToPlaceItem().apply {
                    stepId = tripModelRepository.getStepId(poi.id) ?: 0
                })
            }
        }

        return res
    }
}