package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.repository.StepRepository
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class StepListener @Inject constructor(val repository: StepRepository) : BaseUseCase<MapStep, Unit>() {

    override fun on(params: Unit?) {
        addObservable {
            repository.getStepEmitter()
        }
    }
}