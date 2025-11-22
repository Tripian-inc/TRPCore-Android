package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.FavoriteRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.favorites.model.FavoriteResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class AddFavorite @Inject constructor(val tripModelRepository: TripModelRepository, val repository: FavoriteRepository) : BaseUseCase<FavoriteResponse, AddFavorite.Params>() {

    class Params(val poiId: String)

    override fun on(params: Params?) {
        addObservable {
            repository.addUserFavorite(tripModelRepository.trip!!.city!!.id, tripModelRepository.trip!!.tripHash!!, params!!.poiId)
        }
    }
}