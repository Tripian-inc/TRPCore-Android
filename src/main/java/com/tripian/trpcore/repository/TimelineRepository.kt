package com.tripian.trpcore.repository

import com.tripian.one.TRPRest
import com.tripian.one.api.timeline.model.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

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
}
