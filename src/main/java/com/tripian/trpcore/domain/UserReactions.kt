package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.Service
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.reactions.model.ReactionsResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class UserReactions @Inject constructor(val repository: Service, val tripModelRepository: TripModelRepository) : BaseUseCase<ReactionsResponse, Unit>() {

    override fun on(params: Unit?) {
        addObservable {
            repository.getUserReactions(tripModelRepository.trip!!.tripHash!!)
        }
    }
}