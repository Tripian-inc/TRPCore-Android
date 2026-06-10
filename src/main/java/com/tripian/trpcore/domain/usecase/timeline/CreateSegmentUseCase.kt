package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.one.api.trip.model.Accommodation
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import javax.inject.Inject

class CreateSegmentUseCase @Inject constructor(
    private val repository: TimelineRepository
) : SuspendUseCase<ResponseModelBase, CreateSegmentUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val title: String,
        val cityId: Int,
        val startDate: String,
        val endDate: String,
        val adults: Int = 1,
        val children: Int = 0,
        val activityFreeText: String = "",
        val activityIds: List<String> = emptyList(),
        val excludedActivityIds: List<String> = emptyList(),
        val smartRecommendation: Boolean = true,
        val accommodation: Accommodation? = null
    )

    override suspend fun execute(params: Params): ResponseModelBase {
        val segment = TimelineSegmentSettings.create(
            title = params.title, cityId = params.cityId,
            startDate = params.startDate, endDate = params.endDate,
            coordinate = null, adults = params.adults, children = params.children,
            answerIds = emptyList(), accommodation = params.accommodation,
            currency = TRPCore.core.getCurrentCurrency()
        ).apply {
            smartRecommendation = params.smartRecommendation
            activityFreeText = params.activityFreeText
            activityIds = params.activityIds
            excludedActivityIds = params.excludedActivityIds
            segmentType = SegmentType.ITINERARY
            distinctPlan = true
            available = true
        }
        repository.editSegmentAsync(params.tripHash, segment)
        return ResponseModelBase().apply { status = 200 }
    }
}
