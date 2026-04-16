package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import io.reactivex.Completable
import javax.inject.Inject

/**
 * AddMissingBookedActivitiesUseCase
 *
 * Timeline'da olmayan tripItems'ları booked segment olarak ekle
 * iOS Guide Operation 4: Add Missing Booked Activities
 *
 * Sequential operation - her eksik item için segment oluşturur
 */
class AddMissingBookedActivitiesUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<ResponseModelBase, AddMissingBookedActivitiesUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val tripItems: List<SegmentActivityItem>,
        val timeline: Timeline,
        val cityNameToIdMap: Map<String, Int>
    )

    override fun on(params: Params?) {
        params?.let {
            addObservable {
                // Mevcut activityId'leri topla (from tripProfile.segments)
                val existingActivityIds = mutableSetOf<String>()

                it.timeline.tripProfile?.segments?.forEach { segment ->
                    segment.additionalData?.activityId?.let { id ->
                        existingActivityIds.add(id)
                    }
                }

                // Eksik tripItems'ları bul
                val missingItems = it.tripItems.filter { item ->
                    item.activityId != null && item.activityId !in existingActivityIds
                }

                if (missingItems.isEmpty()) {
                    return@addObservable io.reactivex.Observable.just(ResponseModelBase())
                }

                // Her eksik item için segment oluştur (sequential)
                val createOperations = missingItems.map { tripItem ->
                    // cityId'yi resolve et
                    val cityId = tripItem.cityName?.let { name ->
                        it.cityNameToIdMap[name]
                    } ?: 0

                    val segment = TimelineSegmentSettings().apply {
                        this.segmentType = SegmentType.BOOKED_ACTIVITY
                        this.title = tripItem.title
                        this.startDate = tripItem.startDatetime
                        this.endDate = tripItem.endDatetime
                        this.cityId = if (cityId > 0) cityId else null
                        this.coordinate = Coordinate().apply {
                            lat = tripItem.coordinate.lat
                            lng = tripItem.coordinate.lng
                        }
                        this.available = false
                        this.distinctPlan = true
                        this.doNotGenerate = 1
                        this.adults = tripItem.adultCount
                        this.children = tripItem.childCount

                        this.additionalData = TimelineSegmentAdditionalData().apply {
                            this.activityId = tripItem.activityId
                            this.bookingId = tripItem.bookingId
                            this.price = tripItem.price?.value
                            this.coordinate = this@apply.coordinate
                        }
                    }

                    repository.editSegment(it.tripHash, segment)
                        .onErrorResumeNext { error: Throwable ->
                            android.util.Log.e(
                                "SYNC",
                                "Add booked failed: ${error.message}"
                            )
                            Completable.complete()
                        }
                }

                Completable.concat(createOperations)
                    .toSingleDefault(ResponseModelBase())
                    .toObservable()
            }
        }
    }
}
