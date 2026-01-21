package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.one.api.trip.model.Accommodation
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import javax.inject.Inject

/**
 * CreateSegmentUseCase
 * Creates new segment for Smart Recommendations
 */
class CreateSegmentUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<ResponseModelBase, CreateSegmentUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val title: String,
        val cityId: Int,
        val startDate: String,  // Format: "yyyy-MM-dd HH:mm"
        val endDate: String,    // Format: "yyyy-MM-dd HH:mm"
        val adults: Int = 1,
        val children: Int = 0,
        val activityFreeText: String = "",  // Categories as comma-separated string (e.g., "guided tours, free tours,tickets")
        val activityIds: List<String> = emptyList(),  // Favorite tour IDs
        val smartRecommendation: Boolean = true,
        val accommodation: Accommodation? = null  // Starting point accommodation (Google Place)
    )

    override fun on(params: Params?) {
        params?.let { p ->
            // Don't send coordinate when cityId is present
            val segment = TimelineSegmentSettings.create(
                title = p.title,
                cityId = p.cityId,
                startDate = p.startDate,
                endDate = p.endDate,
                coordinate = null,  // Don't send coordinate when using cityId
                adults = p.adults,
                children = p.children,
                answerIds = emptyList(),  // Empty list instead of null
                accommodation = p.accommodation  // Starting point (Google Place)
            ).apply {
                smartRecommendation = p.smartRecommendation
                activityFreeText = p.activityFreeText
                activityIds = p.activityIds
                segmentType = SegmentType.ITINERARY
                distinctPlan = true
                available = true
            }

            addObservable {
                repository.editSegment(p.tripHash, segment)
                    .toSingleDefault(ResponseModelBase().apply { status = 200 })
                    .toObservable()
            }
        }
    }
}
