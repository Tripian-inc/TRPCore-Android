package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.CustomPoi
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TimelineRepository
import javax.inject.Inject

/**
 * AddStepUseCase
 * Timeline plan'ına yeni step ekler
 */
class AddStepUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<TimelineStep, AddStepUseCase.Params>() {

    sealed class Params {
        abstract val planId: Int
        abstract val startTime: String?
        abstract val endTime: String?
        abstract val order: Int?

        /**
         * POI ID ile step ekle
         */
        data class WithPoi(
            override val planId: Int,
            val poiId: String,
            override val startTime: String? = null,
            override val endTime: String? = null,
            override val order: Int? = null
        ) : Params()

        /**
         * Custom POI ile step ekle
         */
        data class WithCustomPoi(
            override val planId: Int,
            val customPoi: CustomPoi,
            override val startTime: String? = null,
            override val endTime: String? = null,
            override val order: Int? = null
        ) : Params()
    }

    override fun on(params: Params?) {
        params?.let { p ->
            addObservable {
                when (p) {
                    is Params.WithPoi -> repository.addStep(
                        planId = p.planId,
                        poiId = p.poiId,
                        startTime = p.startTime,
                        endTime = p.endTime,
                        order = p.order
                    )
                    is Params.WithCustomPoi -> repository.addStepWithCustomPoi(
                        planId = p.planId,
                        customPoi = p.customPoi,
                        startTime = p.startTime,
                        endTime = p.endTime,
                        order = p.order
                    )
                }
            }
        }
    }
}
