package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import javax.inject.Inject

/**
 * CreateManualPoiSegmentUseCase
 * Creates a manual_poi segment for a POI
 */
class CreateManualPoiSegmentUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<ResponseModelBase, CreateManualPoiSegmentUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val poi: Poi,
        val selectedDate: String,    // Format: "yyyy-MM-dd"
        val startTime: String,       // Format: "HH:mm"
        val endTime: String,         // Format: "HH:mm"
        val cityId: Int
    )

    companion object {
        const val SEGMENT_TYPE_MANUAL_POI = "manual_poi"
    }

    override fun on(params: Params?) {
        params?.let { p ->
            // Build start and end datetime (format: Y-m-d H:M without seconds)
            val startDatetime = "${p.selectedDate} ${p.startTime}"
            val endDatetime = "${p.selectedDate} ${p.endTime}"

            // Build segment settings
            val segment = TimelineSegmentSettings().apply {
                title = p.poi.name ?: "POI"
                cityId = p.cityId
                startDate = startDatetime
                endDate = endDatetime
                coordinate = p.poi.coordinate
                segmentType = SEGMENT_TYPE_MANUAL_POI
                includePoiIds = listOf(p.poi.id)
                available = false
                distinctPlan = true
            }

            addObservable {
                repository.editSegment(p.tripHash, segment)
                    .toSingleDefault(ResponseModelBase().apply { status = 200 })
                    .toObservable()
            }
        }
    }
}
