package com.tripian.trpcore.repository

import com.tripian.one.TRPRest
import com.tripian.one.api.cities.model.CityResolveData
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.CustomPoi
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelinePlan
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.one.api.timeline.model.TimelineSettings
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.one.api.timeline.model.TimelineStepCreateRequest
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.base.awaitCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * TimelineRepository — suspend wrappers over the TRPRest Timeline callback API.
 */
class TimelineRepository @Inject constructor(
    private val trpRest: TRPRest
) {

    // One-shot cache for a freshly-generated timeline. The SavedPlans add flow
    // already fetches the generated timeline in the background (via
    // WaitForGenerationUseCase); caching it here lets the timeline screen apply
    // it on return instead of issuing a second GET. Guarded by tripHash so a
    // cache from one trip can never be applied to another.
    @Volatile
    private var pendingTimeline: Timeline? = null
    @Volatile
    private var pendingTimelineHash: String? = null

    /** Stores [timeline] so the timeline screen can consume it without a GET. */
    fun cacheGeneratedTimeline(tripHash: String, timeline: Timeline) {
        pendingTimelineHash = tripHash
        pendingTimeline = timeline
    }

    /**
     * Returns and clears the cached timeline if it matches [tripHash]; null
     * otherwise. One-shot — a consumed cache is dropped so it can't be reapplied.
     */
    fun consumeGeneratedTimeline(tripHash: String): Timeline? {
        if (pendingTimelineHash != tripHash) return null
        return pendingTimeline.also {
            pendingTimeline = null
            pendingTimelineHash = null
        }
    }

    suspend fun fetchTimelineAsync(tripHash: String): Timeline = awaitCallback { ok, fail ->
        trpRest.getTimeline(
            hash = tripHash,
            currency = TRPCore.core.getCurrentCurrency(),
            success = { response ->
                response.data?.let { ok(it) }
                    ?: fail(Exception("Timeline data is null"))
            },
            error = { throwable -> fail(throwable ?: Exception("Unknown error")) }
        )
    }

    suspend fun createTimelineAsync(settings: TimelineSettings): Timeline = awaitCallback { ok, fail ->
        trpRest.createTimeline(
            settings = settings,
            success = { response ->
                response.data?.let { ok(it) }
                    ?: fail(Exception("Timeline data is null"))
            },
            error = { throwable -> fail(throwable ?: Exception("Unknown error")) }
        )
    }

    suspend fun editSegmentAsync(
        tripHash: String,
        segment: TimelineSegmentSettings
    ): Unit = suspendCancellableCoroutine { cont ->
        trpRest.editTimelineSegment(
            hash = tripHash,
            segment = segment,
            success = { if (cont.isActive) cont.resume(Unit) },
            error = { throwable ->
                if (cont.isActive) {
                    cont.resumeWithException(throwable ?: Exception("Unknown error"))
                }
            }
        )
    }

    suspend fun deleteSegmentAsync(
        tripHash: String,
        segmentIndex: Int
    ): Unit = suspendCancellableCoroutine { cont ->
        trpRest.deleteTimelineSegment(
            hash = tripHash,
            segmentIndex = segmentIndex,
            success = { if (cont.isActive) cont.resume(Unit) },
            error = { throwable ->
                if (cont.isActive) {
                    cont.resumeWithException(throwable ?: Exception("Unknown error"))
                }
            }
        )
    }

    suspend fun addStepAsync(
        planId: Int,
        poiId: String,
        startTime: String? = null,
        endTime: String? = null,
        order: Int? = null
    ): TimelineStep = awaitCallback { ok, fail ->
        trpRest.addTimelineStep(
            planId = planId,
            poiId = poiId,
            startTime = startTime,
            endTime = endTime,
            order = order,
            success = { response ->
                response.data?.let { ok(it) }
                    ?: fail(Exception("Step data is null"))
            },
            error = { throwable -> fail(throwable ?: Exception("Unknown error")) }
        )
    }

    suspend fun addStepAsync(request: TimelineStepCreateRequest): TimelineStep = awaitCallback { ok, fail ->
        trpRest.addTimelineStep(
            step = request,
            success = { response ->
                response.data?.let { ok(it) }
                    ?: fail(Exception("Step data is null"))
            },
            error = { throwable -> fail(throwable ?: Exception("Unknown error")) }
        )
    }

    suspend fun addStepWithCustomPoiAsync(
        planId: Int,
        customPoi: CustomPoi,
        startTime: String? = null,
        endTime: String? = null,
        order: Int? = null
    ): TimelineStep = awaitCallback { ok, fail ->
        trpRest.addTimelineStepWithCustomPoi(
            planId = planId,
            customPoi = customPoi,
            startTime = startTime,
            endTime = endTime,
            order = order,
            success = { response ->
                response.data?.let { ok(it) }
                    ?: fail(Exception("Step data is null"))
            },
            error = { throwable -> fail(throwable ?: Exception("Unknown error")) }
        )
    }

    suspend fun editStepAsync(
        stepId: Int,
        poiId: String? = null,
        startTime: String? = null,
        endTime: String? = null,
        order: Int? = null
    ): TimelineStep = awaitCallback { ok, fail ->
        trpRest.editTimelineStep(
            stepId = stepId,
            poiId = poiId,
            startTime = startTime,
            endTime = endTime,
            order = order,
            success = { response ->
                response.data?.let { ok(it) }
                    ?: fail(Exception("Step data is null"))
            },
            error = { throwable -> fail(throwable ?: Exception("Unknown error")) }
        )
    }

    suspend fun deleteStepAsync(stepId: Int): Unit = suspendCancellableCoroutine { cont ->
        trpRest.deleteTimelineStep(
            stepId = stepId,
            success = { if (cont.isActive) cont.resume(Unit) },
            error = { throwable ->
                if (cont.isActive) {
                    cont.resumeWithException(throwable ?: Exception("Unknown error"))
                }
            }
        )
    }

    suspend fun deleteTimelineAsync(tripHash: String): Unit = suspendCancellableCoroutine { cont ->
        trpRest.deleteTimeline(
            hash = tripHash,
            success = { if (cont.isActive) cont.resume(Unit) },
            error = { throwable ->
                if (cont.isActive) {
                    cont.resumeWithException(throwable ?: Exception("Unknown error"))
                }
            }
        )
    }

    suspend fun getUserTimelinesAsync(
        dateFrom: String? = null,
        dateTo: String? = null,
        limit: Int? = 100
    ): List<Timeline> = awaitCallback { ok, fail ->
        trpRest.getUserTimelines(
            dateFrom = dateFrom,
            dateTo = dateTo,
            limit = limit,
            success = { response -> ok(response.data ?: emptyList()) },
            error = { throwable -> fail(throwable ?: Exception("Unknown error")) }
        )
    }

    suspend fun getTimelinePlansAsync(planId: String): List<TimelinePlan> = awaitCallback { ok, fail ->
        trpRest.getTimelinePlans(
            planId = planId,
            success = { response -> ok(response.data ?: emptyList()) },
            error = { throwable -> fail(throwable ?: Exception("Unknown error")) }
        )
    }

    suspend fun resolveCitiesAsync(coordinates: List<Coordinate>): List<CityResolveData> = awaitCallback { ok, fail ->
        trpRest.resolveCitiesByCoordinates(
            coordinates = coordinates,
            success = { response -> ok(response.data ?: emptyList()) },
            error = { throwable -> fail(throwable ?: Exception("City resolve failed")) }
        )
    }
}
