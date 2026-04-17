package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegmentAdditionalData
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
 * TimelineDate segment'ini itinerary tarih değişikliklerine göre güncelle
 * iOS Guide Operation 2: Date Range Sync
 *
 * CRITICAL: NO empty segments - sadece mevcut TimelineDate segment'i UPDATE et
 * BACKWARD COMPATIBILITY: Empty segmentler varsa migration yap
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
                // 1. TimelineDate segment'ini bul (title == "TimelineDate")
                val segments = it.timeline.tripProfile?.segments ?: emptyList()
                val timelineDateSegment = segments.find { segment ->
                    segment.title == "TimelineDate"
                }

                if (timelineDateSegment == null) {
                    // BACKWARD COMPATIBILITY: TimelineDate yok, migration gerekli
                    android.util.Log.d("SYNC", "TimelineDate not found, performing migration")
                    return@addObservable migrateToTimelineDateSegment(it)
                }

                // 2. Itinerary tarihlerini parse et (format: "yyyy-MM-dd HH:mm")
                val itineraryDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                val itineraryStartDate = itineraryDateFormat.parse(it.itinerary.startDatetime)
                val itineraryEndDate = itineraryDateFormat.parse(it.itinerary.endDatetime)

                if (itineraryStartDate == null || itineraryEndDate == null) {
                    return@addObservable io.reactivex.Observable.just(ResponseModelBase())
                }

                // 3. TimelineDate için target formatı oluştur (dd.MM.yyyy HH:mm)
                val targetFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US)
                val cal = Calendar.getInstance()

                // Start date: 00:00
                cal.time = itineraryStartDate
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                val targetStartDate = targetFormat.format(cal.time)

                // End date: 23:59
                cal.time = itineraryEndDate
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                val targetEndDate = targetFormat.format(cal.time)

                // 4. Mevcut TimelineDate dates ile karşılaştır
                val currentStartDate = timelineDateSegment.startDate
                val currentEndDate = timelineDateSegment.endDate

                if (currentStartDate == targetStartDate && currentEndDate == targetEndDate) {
                    // Değişiklik yok, update gerekmiyor
                    android.util.Log.d("SYNC", "TimelineDate segment already up-to-date")
                    return@addObservable io.reactivex.Observable.just(ResponseModelBase())
                }

                // 5. TimelineDate segment'i güncelle
                android.util.Log.d("SYNC", "Updating TimelineDate: $targetStartDate - $targetEndDate")

                val updatedSegment = TimelineSegmentSettings().apply {
                    this.segmentType = timelineDateSegment.segmentType
                    this.title = "TimelineDate"
                    this.startDate = targetStartDate
                    this.endDate = targetEndDate
                    this.cityId = it.itinerary.destinationItems.firstOrNull()?.cityId
                }

                repository.editSegment(it.tripHash, updatedSegment)
                    .onErrorResumeNext { error: Throwable ->
                        android.util.Log.e("SYNC", "Update TimelineDate failed: ${error.message}")
                        Completable.complete()
                    }
                    .toSingleDefault(ResponseModelBase())
                    .toObservable()
            }
        }
    }

    /**
     * BACKWARD COMPATIBILITY: Migration from empty segments to TimelineDate segment
     *
     * Steps:
     * 1. Find and delete all empty ITINERARY segments
     * 2. Create new TimelineDate segment with itinerary date range
     */
    private fun migrateToTimelineDateSegment(params: Params): io.reactivex.Observable<ResponseModelBase> {
        val segments = params.timeline.tripProfile?.segments ?: emptyList()

        // 1. Find empty ITINERARY segments (title like "Day Plan", generic date placeholder)
        val emptySegmentIndices = mutableListOf<Int>()
        segments.forEachIndexed { index, segment ->
            // Empty segment criteria:
            // - Type: ITINERARY
            // - Title: "Day Plan" or similar generic title (placeholder for date)
            // - No additionalData (not a booked activity)
            if (segment.segmentType == SegmentType.ITINERARY &&
                (segment.title == "Day Plan" || segment.title?.startsWith("Day") == true) &&
                segment.additionalData == null
            ) {
                emptySegmentIndices.add(index)
            }
        }

        // 2. Delete empty segments (highest-index-first)
        val deleteOperations = emptySegmentIndices
            .sortedDescending()
            .map { index ->
                repository.deleteSegment(params.tripHash, index)
                    .onErrorResumeNext { error: Throwable ->
                        android.util.Log.e("SYNC", "Delete empty segment (index $index) failed: ${error.message}")
                        Completable.complete()
                    }
            }

        // 3. Create TimelineDate segment
        val itineraryDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val itineraryStartDate = itineraryDateFormat.parse(params.itinerary.startDatetime)
        val itineraryEndDate = itineraryDateFormat.parse(params.itinerary.endDatetime)

        if (itineraryStartDate == null || itineraryEndDate == null) {
            return io.reactivex.Observable.just(ResponseModelBase())
        }

        val targetFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US)
        val cal = Calendar.getInstance()

        cal.time = itineraryStartDate
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        val targetStartDate = targetFormat.format(cal.time)

        cal.time = itineraryEndDate
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val targetEndDate = targetFormat.format(cal.time)

        val createTimelineDateSegment = {
            val segment = TimelineSegmentSettings().apply {
                this.segmentType = SegmentType.ITINERARY
                this.title = "TimelineDate"
                this.startDate = targetStartDate
                this.endDate = targetEndDate
                this.cityId = params.itinerary.destinationItems.firstOrNull()?.cityId
            }

            repository.editSegment(params.tripHash, segment)
                .onErrorResumeNext { error: Throwable ->
                    android.util.Log.e("SYNC", "Create TimelineDate segment failed: ${error.message}")
                    Completable.complete()
                }
        }

        // Sequential: Delete all empty segments → Create TimelineDate
        return Completable.concat(deleteOperations)
            .andThen(createTimelineDateSegment())
            .toSingleDefault(ResponseModelBase())
            .toObservable()
    }
}
