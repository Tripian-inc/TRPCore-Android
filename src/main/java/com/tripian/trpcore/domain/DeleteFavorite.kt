package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.FavoriteRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.trip.model.DeleteResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class DeleteFavorite @Inject constructor(val tripModelRepository: TripModelRepository, val repository: FavoriteRepository) : BaseUseCase<DeleteResponse, DeleteFavorite.Params>() {

    class Params(val favoriteId: Int)

    override fun on(params: Params?) {
        addObservable {
            repository.deleteUserFavorite(tripModelRepository.trip!!.city!!.id, params!!.favoriteId)
        }
    }
}