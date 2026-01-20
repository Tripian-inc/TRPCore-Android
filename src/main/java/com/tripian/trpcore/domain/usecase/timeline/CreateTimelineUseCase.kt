package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSettings
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.repository.TimelineRepository
import javax.inject.Inject

/**
 * CreateTimelineUseCase
 * Creates timeline from ItineraryWithActivities model
 */
class CreateTimelineUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<Timeline, CreateTimelineUseCase.Params>() {

    data class Params(
        val itinerary: ItineraryWithActivities
    )

    override fun on(params: Params?) {
        params?.let { p ->
            val settings = createTimelineSettings(p.itinerary)
            addObservable {
                repository.createTimeline(settings)
            }
        }
    }

    /**
     * Create TimelineSettings from ItineraryWithActivities
     */
    private fun createTimelineSettings(itinerary: ItineraryWithActivities): TimelineSettings {
        val cityId = itinerary.getFirstCityId()
            ?: throw IllegalArgumentException("City ID is required")

        val segments = itinerary.createSegmentsFromTripItems()

        return TimelineSettings().apply {
            this.cityId = cityId
            this.adults = itinerary.getAdultCount()
            this.children = itinerary.getChildCount()
            this.segments = segments
        }
    }
}
