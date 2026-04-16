package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import io.reactivex.Completable
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * UpdateDateRangeUseCase
 *
 * Itinerary tarih aralığı genişlediyse boş segmentler ekle
 * iOS Guide Operation 2: Date Range Sync
 *
 * Sequential operation - her eksik gün için segment oluşturur
 */
class UpdateDateRangeUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<ResponseModelBase, UpdateDateRangeUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val itinerary: ItineraryWithActivities,
        val timeline: Timeline
    )

    override fun on(params: Params?) {
        params?.let {
            addObservable {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

                // Timeline'daki tarih aralığını bul
                val segments = it.timeline.tripProfile?.segments ?: emptyList()
                val timelineDates = segments.mapNotNull { segment ->
                    segment.startDate?.let { dateStr -> dateFormat.parse(dateStr) }
                }

                if (timelineDates.isEmpty()) {
                    return@addObservable io.reactivex.Observable.just(ResponseModelBase())
                }

                val timelineStartDate = timelineDates.minOrNull()
                val timelineEndDate = timelineDates.maxOrNull()

                // Itinerary tarih aralığı
                val itineraryStartDate = dateFormat.parse(it.itinerary.startDatetime)
                val itineraryEndDate = dateFormat.parse(it.itinerary.endDatetime)

                // Eksik tarih aralıklarını tespit et
                val missingDates = mutableListOf<Date>()

                // Başlangıç tarih kontrolü
                if (itineraryStartDate != null && timelineStartDate != null &&
                    itineraryStartDate.before(timelineStartDate)
                ) {
                    // Başta eksik günler var
                    val cal = Calendar.getInstance()
                    cal.time = itineraryStartDate
                    while (cal.time.before(timelineStartDate)) {
                        missingDates.add(cal.time)
                        cal.add(Calendar.DAY_OF_MONTH, 1)
                    }
                }

                // Bitiş tarih kontrolü
                if (itineraryEndDate != null && timelineEndDate != null &&
                    itineraryEndDate.after(timelineEndDate)
                ) {
                    // Sonda eksik günler var
                    val cal = Calendar.getInstance()
                    cal.time = timelineEndDate
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    while (cal.time.before(itineraryEndDate) || cal.time == itineraryEndDate) {
                        missingDates.add(cal.time)
                        cal.add(Calendar.DAY_OF_MONTH, 1)
                    }
                }

                if (missingDates.isEmpty()) {
                    return@addObservable io.reactivex.Observable.just(ResponseModelBase())
                }

                // Her eksik gün için boş segment oluştur (sequential)
                val createOperations = missingDates.map { date ->
                    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val dayStr = dayFormat.format(date)

                    val segment = TimelineSegmentSettings().apply {
                        this.segmentType = SegmentType.ITINERARY
                        this.title = "Day Plan"
                        this.startDate = "$dayStr 00:00"
                        this.endDate = "$dayStr 23:59"
                        // cityId itinerary'den ilk şehir
                        this.cityId = it.itinerary.destinationItems.firstOrNull()?.cityId
                    }

                    repository.editSegment(it.tripHash, segment)
                        .onErrorResumeNext { error: Throwable ->
                            android.util.Log.e(
                                "SYNC",
                                "Add date range failed: ${error.message}"
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
