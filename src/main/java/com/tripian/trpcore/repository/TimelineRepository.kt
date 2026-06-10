package com.tripian.trpcore.repository

import com.tripian.one.TRPRest
import com.tripian.one.api.cities.model.CityResolveData
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.*
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.base.awaitCallback
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * TimelineRepository
 * Wraps TRPRest Timeline methods into RxJava Observables
 */
class TimelineRepository @Inject constructor(
    private val trpRest: TRPRest
) {

    /**
     * Fetch timeline
     */
    fun fetchTimeline(tripHash: String): Observable<Timeline> {
        return Single.create<Timeline> { emitter ->
            trpRest.getTimeline(
                hash = tripHash,
                currency = TRPCore.core.getCurrentCurrency(),
                success = { response ->
                    response.data?.let { timeline ->
                        emitter.onSuccess(timeline)
                    } ?: emitter.onError(Exception("Timeline data is null"))
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Unknown error"))
                }
            )
        }.toObservable()
    }

    /**
     * Create timeline
     */
    fun createTimeline(settings: TimelineSettings): Observable<Timeline> {
        return Single.create<Timeline> { emitter ->
            trpRest.createTimeline(
                settings = settings,
                success = { response ->
                    response.data?.let { timeline ->
                        emitter.onSuccess(timeline)
                    } ?: emitter.onError(Exception("Timeline data is null"))
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Unknown error"))
                }
            )
        }.toObservable()
    }

    /**
     * Create/edit segment
     */
    fun editSegment(tripHash: String, segment: TimelineSegmentSettings): Completable {
        return Completable.create { emitter ->
            trpRest.editTimelineSegment(
                hash = tripHash,
                segment = segment,
                success = { _ ->
                    emitter.onComplete()
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Unknown error"))
                }
            )
        }
    }

    /**
     * Delete segment
     */
    fun deleteSegment(tripHash: String, segmentIndex: Int): Completable {
        return Completable.create { emitter ->
            trpRest.deleteTimelineSegment(
                hash = tripHash,
                segmentIndex = segmentIndex,
                success = { _ ->
                    emitter.onComplete()
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Unknown error"))
                }
            )
        }
    }

    /**
     * Add step (with POI)
     */
    fun addStep(
        planId: Int,
        poiId: String,
        startTime: String? = null,
        endTime: String? = null,
        order: Int? = null
    ): Observable<TimelineStep> {
        return Single.create<TimelineStep> { emitter ->
            trpRest.addTimelineStep(
                planId = planId,
                poiId = poiId,
                startTime = startTime,
                endTime = endTime,
                order = order,
                success = { response ->
                    response.data?.let { step ->
                        emitter.onSuccess(step)
                    } ?: emitter.onError(Exception("Step data is null"))
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Unknown error"))
                }
            )
        }.toObservable()
    }

    /**
     * Add step (with Request object)
     */
    fun addStep(request: TimelineStepCreateRequest): Observable<TimelineStep> {
        return Single.create<TimelineStep> { emitter ->
            trpRest.addTimelineStep(
                step = request,
                success = { response ->
                    response.data?.let { step ->
                        emitter.onSuccess(step)
                    } ?: emitter.onError(Exception("Step data is null"))
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Unknown error"))
                }
            )
        }.toObservable()
    }

    /**
     * Add step with custom POI
     */
    fun addStepWithCustomPoi(
        planId: Int,
        customPoi: CustomPoi,
        startTime: String? = null,
        endTime: String? = null,
        order: Int? = null
    ): Observable<TimelineStep> {
        return Single.create<TimelineStep> { emitter ->
            trpRest.addTimelineStepWithCustomPoi(
                planId = planId,
                customPoi = customPoi,
                startTime = startTime,
                endTime = endTime,
                order = order,
                success = { response ->
                    response.data?.let { step ->
                        emitter.onSuccess(step)
                    } ?: emitter.onError(Exception("Step data is null"))
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Unknown error"))
                }
            )
        }.toObservable()
    }

    /**
     * Edit step
     */
    fun editStep(
        stepId: Int,
        poiId: String? = null,
        startTime: String? = null,
        endTime: String? = null,
        order: Int? = null
    ): Observable<TimelineStep> {
        return Single.create<TimelineStep> { emitter ->
            trpRest.editTimelineStep(
                stepId = stepId,
                poiId = poiId,
                startTime = startTime,
                endTime = endTime,
                order = order,
                success = { response ->
                    response.data?.let { step ->
                        emitter.onSuccess(step)
                    } ?: emitter.onError(Exception("Step data is null"))
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Unknown error"))
                }
            )
        }.toObservable()
    }

    /**
     * Delete step
     */
    fun deleteStep(stepId: Int): Completable {
        return Completable.create { emitter ->
            trpRest.deleteTimelineStep(
                stepId = stepId,
                success = { _ ->
                    emitter.onComplete()
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Unknown error"))
                }
            )
        }
    }

    /**
     * Delete timeline
     */
    fun deleteTimeline(tripHash: String): Completable {
        return Completable.create { emitter ->
            trpRest.deleteTimeline(
                hash = tripHash,
                success = { _ ->
                    emitter.onComplete()
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Unknown error"))
                }
            )
        }
    }

    /**
     * Get user timelines
     */
    fun getUserTimelines(
        dateFrom: String? = null,
        dateTo: String? = null,
        limit: Int? = 100
    ): Observable<List<Timeline>> {
        return Single.create<List<Timeline>> { emitter ->
            trpRest.getUserTimelines(
                dateFrom = dateFrom,
                dateTo = dateTo,
                limit = limit,
                success = { response ->
                    emitter.onSuccess(response.data ?: emptyList())
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Unknown error"))
                }
            )
        }.toObservable()
    }

    /**
     * Get timeline plans
     */
    fun getTimelinePlans(planId: String): Observable<List<TimelinePlan>> {
        return Single.create<List<TimelinePlan>> { emitter ->
            trpRest.getTimelinePlans(
                planId = planId,
                success = { response ->
                    emitter.onSuccess(response.data ?: emptyList())
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("Unknown error"))
                }
            )
        }.toObservable()
    }

    /**
     * Resolve city IDs from coordinates
     * Calls cities/resolve API to get cityIds for given coordinates
     *
     * @param coordinates List of Coordinate objects to resolve
     * @return Observable with list of CityResolveData containing cityIds
     */
    fun resolveCities(coordinates: List<Coordinate>): Observable<List<CityResolveData>> {
        return Single.create<List<CityResolveData>> { emitter ->
            trpRest.resolveCitiesByCoordinates(
                coordinates = coordinates,
                success = { response ->
                    emitter.onSuccess(response.data ?: emptyList())
                },
                error = { throwable ->
                    emitter.onError(throwable ?: Exception("City resolve failed"))
                }
            )
        }.toObservable()
    }

    // ------------------------------------------------------------------
    // Suspend equivalents — every method unwraps the TRPRest response
    // the same way the Single/Completable versions do, then exposes a
    // suspend signature. Legacy RxJava versions stay in place until the
    // UseCases that consume them are migrated (Phase D).
    // ------------------------------------------------------------------

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
