package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import javax.inject.Inject

class CreateManualPoiSegmentUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<ResponseModelBase, CreateManualPoiSegmentUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val poi: Poi,
        val selectedDate: String,
        val startTime: String,
        val endTime: String,
        val cityId: Int
    )

    companion object {
        const val SEGMENT_TYPE_MANUAL_POI = "manual_poi"
    }

    override suspend fun execute(params: Params): ResponseModelBase {
        val startDatetime = "${params.selectedDate} ${params.startTime}"
        val endDatetime = "${params.selectedDate} ${params.endTime}"

        val segment = TimelineSegmentSettings().apply {
            title = params.poi.name ?: "POI"
            cityId = params.cityId
            startDate = startDatetime
            endDate = endDatetime
            coordinate = params.poi.coordinate
            segmentType = SEGMENT_TYPE_MANUAL_POI
            includePoiIds = listOf(params.poi.id)
            available = false
            distinctPlan = true
            currency = TRPCore.core.getCurrentCurrency()
        }

        repository.editSegmentAsync(params.tripHash, segment)
        return ResponseModelBase().apply { status = 200 }
    }
}
