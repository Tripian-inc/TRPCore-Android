package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.FavoriteRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.favorites.model.FavoriteResponse
import com.tripian.trpcore.repository.MiscRepository
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class FetchLanguages @Inject constructor() : BaseUseCase<Boolean, FetchLanguages.Params>() {

    class Params

    override fun on(params: Params?) {
        addObservable {
            miscRepository.getLanguageValues()
        }
    }
}