package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.one.api.trip.model.Trip
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class TripListener @Inject constructor(val tripModelRepository: TripModelRepository) : BaseUseCase<Trip, Unit>() {

    override fun on(params: Unit?) {
        addObservable { tripModelRepository.getTripEmitter() }
    }
}