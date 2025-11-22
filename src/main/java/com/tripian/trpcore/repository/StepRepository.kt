package com.tripian.trpcore.repository

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.trip.model.AddCustomPoiStepRequest
import com.tripian.one.api.trip.model.AddStepRequest
import com.tripian.one.api.trip.model.CustomPoiPhotoModel
import com.tripian.one.api.trip.model.CustomPoiRequest
import com.tripian.one.api.trip.model.DeleteResponse
import com.tripian.one.api.trip.model.StepResponse
import com.tripian.one.api.trip.model.UpdateStepRequest
import com.tripian.one.api.trip.model.UpdateStepTimeRequest
import com.tripian.trpcore.domain.model.MapStep
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class StepRepository @Inject constructor(val service: Service) {

    private var stepEmitter = PublishSubject.create<MapStep>()

    fun addStep(planId: Int, poiId: String): Observable<StepResponse> {
        return service.addStep(AddStepRequest(planId, poiId)).map {
            stepEmitter.onNext(MapStep().apply {
                this.group = "step"
                this.poiId = it.data!!.poi!!.id
                this.poi = it.data!!.poi
                this.stepId = it.data!!.id
            })

            it
        }
    }

    fun addCustomPoiStep(
        planId: Int,
        name: String,
        coordinate: Coordinate,
        address: String,
        description: String,
        imageUrl: String,
        web: String,
        stepType: String
    ): Observable<StepResponse> {
        return service.addCustomPoiStep(
            AddCustomPoiStepRequest(
                planId = planId,
                customPoi = CustomPoiRequest(
                    name = name,
                    address = address,
                    description = description,
                    photos = listOf(CustomPoiPhotoModel(
                        url = imageUrl
                    )),
                    web = web,
                    coordinate = coordinate
                ),
                stepType = stepType
            )
        ).map {
            stepEmitter.onNext(MapStep().apply {
                this.group = "nexus_product"
                this.poiId = it.data!!.poi!!.id
                this.poi = it.data!!.poi
                this.stepId = it.data!!.id
            })

            it
        }
    }

    fun deleteStep(stepId: Int): Observable<DeleteResponse> {
        return service.deleteStep(stepId).map {
            stepEmitter.onNext(MapStep().apply {
                this.group = "alternative"
                this.stepId = it.data!!.recordId!!
            })

            it
        }
    }

    fun updateStep(stepId: Int, poiId: String?, order: Int?): Observable<StepResponse> {
        return service.updateStep(stepId, UpdateStepRequest(order, poiId))
    }

    fun updateStepTime(stepId: Int, startTime: String?, endTime: String?): Observable<StepResponse> {
        return service.updateStepTime(stepId, UpdateStepTimeRequest(startTime, endTime))
    }

    fun getStepEmitter(): Observable<MapStep> {
        return stepEmitter
    }
}