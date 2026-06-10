package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.CustomPoi
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.repository.TimelineRepository
import javax.inject.Inject

class AddStepUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<TimelineStep, AddStepUseCase.Params>() {

    sealed class Params {
        abstract val planId: Int
        abstract val startTime: String?
        abstract val endTime: String?
        abstract val order: Int?

        data class WithPoi(
            override val planId: Int,
            val poiId: String,
            override val startTime: String? = null,
            override val endTime: String? = null,
            override val order: Int? = null
        ) : Params()

        data class WithCustomPoi(
            override val planId: Int,
            val customPoi: CustomPoi,
            override val startTime: String? = null,
            override val endTime: String? = null,
            override val order: Int? = null
        ) : Params()
    }

    override suspend fun execute(params: Params): TimelineStep = when (params) {
        is Params.WithPoi -> repository.addStepAsync(
            planId = params.planId, poiId = params.poiId,
            startTime = params.startTime, endTime = params.endTime, order = params.order
        )
        is Params.WithCustomPoi -> repository.addStepWithCustomPoiAsync(
            planId = params.planId, customPoi = params.customPoi,
            startTime = params.startTime, endTime = params.endTime, order = params.order
        )
    }
}
