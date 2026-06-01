package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import io.reactivex.Completable
import io.reactivex.Observable
import javax.inject.Inject

/**
 * RemoveOutOfRangeSegmentsUseCase
 *
 * When the host re-opens the SDK with a shifted date range (e.g. 1–5 June →
 * 3–7 June), the segments anchored on days that no longer exist (1 June,
 * 2 June) become orphans — visible in the data but not reachable from the
 * day filter. iOS leaves them in place; on Android we clean them up so the
 * data stays in sync with the new range.
 *
 * Rules:
 *  - Skip the TimelineDate sentinel (`title == "TimelineDate" && !available`).
 *  - A segment is "out of range" when its `startDate` day prefix is strictly
 *    before the new start day or strictly after the new end day.
 *  - Segments without a parseable date are left alone — we can't decide.
 *  - Local mutation is optimistic (so the UI updates immediately when paired
 *    with a re-publish); server DELETEs run sequentially in descending index
 *    order to avoid array shifts on the backend.
 */
class RemoveOutOfRangeSegmentsUseCase @Inject constructor(
    private val repository: TimelineRepository
) : BaseUseCase<RemoveOutOfRangeSegmentsUseCase.Result, RemoveOutOfRangeSegmentsUseCase.Params>() {

    data class Params(
        val tripHash: String,
        val itinerary: ItineraryWithActivities,
        val timeline: Timeline
    )

    /** True iff at least one segment was removed — caller refreshes the UI. */
    data class Result(val removedCount: Int) {
        val mutated: Boolean get() = removedCount > 0
    }

    override fun on(params: Params?) {
        params?.let {
            addObservable {
                val result = applyLocally(it) ?: return@addObservable Observable.just(Result(0))
                if (result.indicesDescending.isEmpty()) {
                    return@addObservable Observable.just(Result(0))
                }
                // Server-side cleanup, sequential & descending. Errors are
                // logged but don't break the chain.
                val deletions = result.indicesDescending.map { idx ->
                    repository.deleteSegment(it.tripHash, idx)
                        .onErrorResumeNext { error: Throwable ->
                            android.util.Log.e(
                                "SYNC",
                                "Out-of-range segment delete (index $idx) failed: ${error.message}"
                            )
                            Completable.complete()
                        }
                }
                Completable.concat(deletions)
                    .toSingleDefault(Result(result.indicesDescending.size))
                    .toObservable()
            }
        }
    }

    private data class LocalPass(val indicesDescending: List<Int>)

    private fun applyLocally(params: Params): LocalPass? {
        val newRange = extractDayRange(params.itinerary) ?: return null
        val segments = params.timeline.tripProfile?.segments ?: return null

        val outOfRange = segments.withIndex().filter { (_, seg) ->
            isOutOfRange(seg, newRange.first, newRange.second)
        }
        if (outOfRange.isEmpty()) return LocalPass(emptyList())

        // Mutate the underlying list (Gson backs it with ArrayList). If the
        // collection isn't mutable for any reason, skip the local pass and let
        // the server-side delete + post-sync refetch reconcile.
        val mutable = segments as? MutableList<TimelineSegment>
        val indicesDesc = outOfRange.map { it.index }.sortedDescending()
        if (mutable != null) {
            indicesDesc.forEach { mutable.removeAt(it) }
        }
        return LocalPass(indicesDesc)
    }

    private fun isOutOfRange(seg: TimelineSegment, startDay: String, endDay: String): Boolean {
        if (seg.title == "TimelineDate" && !seg.available) return false
        val day = seg.startDate?.take(10)?.takeIf {
            it.length == 10 && it[4] == '-' && it[7] == '-'
        } ?: return false
        return day < startDay || day > endDay
    }

    private fun extractDayRange(itinerary: ItineraryWithActivities): Pair<String, String>? {
        val start = itinerary.startDatetime.take(10).takeIf {
            it.length == 10 && it[4] == '-' && it[7] == '-'
        } ?: return null
        val end = itinerary.endDatetime.take(10).takeIf {
            it.length == 10 && it[4] == '-' && it[7] == '-'
        } ?: return null
        if (start > end) return null
        return start to end
    }
}
