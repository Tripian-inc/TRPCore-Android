package com.tripian.trpcore.domain

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.trip.model.StepResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.StepRepository
import com.tripian.trpcore.repository.TripModelRepository
import javax.inject.Inject

class AddCustomPoiStep @Inject constructor(
    val repository: StepRepository,
    val tripModelRepository: TripModelRepository,
    val fetchPlan: FetchPlan
) : BaseUseCase<StepResponse, AddCustomPoiStep.Params>() {

    class Params(
        val name: String,
        val lat: Double,
        val lng: Double,
        val address: String,
        val description: String,
        val imageUrl: String,
        val web: String,
        val stepType: String
    )

    override fun on(params: Params?) {
        addObservable {
            repository.addCustomPoiStep(
                planId = tripModelRepository.dailyPlan!!.id,
                name = params!!.name,
                coordinate = Coordinate().apply {
                    lat = params.lat
                    lng = params.lng
                },
                address = params.address,
                description = params.description,
                imageUrl = params.imageUrl,
                web = params.web,
                stepType = params.stepType
            )
        }
    }

    override fun onSendSuccess(t: StepResponse) {
        fetchPlan.on(FetchPlan.Params(tripModelRepository.dailyPlan!!.id), success = {
            super.onSendSuccess(t)
        }, error = {
            super.onSendError(it)
        })
    }
}