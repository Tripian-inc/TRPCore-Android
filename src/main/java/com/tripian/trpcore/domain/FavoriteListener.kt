package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.FavoriteRepository
import com.tripian.one.api.favorites.model.Favorite
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class FavoriteListener @Inject constructor(val repository: FavoriteRepository) : BaseUseCase<Favorite, Unit>() {

    override fun on(params: Unit?) {
        addObservable {
            repository.getFavoriteEmitter()
        }
    }
}