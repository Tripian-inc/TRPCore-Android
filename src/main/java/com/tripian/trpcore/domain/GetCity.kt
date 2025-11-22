package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.repository.TripRepository
import com.tripian.one.api.cities.model.GetCityResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetCity @Inject constructor(val repository: TripRepository, val tripModelRepository: TripModelRepository) : BaseUseCase<GetCityResponse, Unit>() {

    override fun on(params: Unit?) {
        addObservable {
            repository.getCity(tripModelRepository.trip!!.city!!.id)
        }
    }
}