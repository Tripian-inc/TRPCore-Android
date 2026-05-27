package com.tripian.trpcore.domain.manager

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

/**
 * Theme 10 — global observable for the "timeline is being refreshed" state.
 *
 * The single timeline view-model owns the actual refresh operation, but other
 * screens that can be open simultaneously (Saved Plans, the AddPlan
 * Time Selection bottom sheet) need to react to it: they show a small loader
 * while a refresh is in flight and surface a confirmation when it completes.
 *
 * State transitions are driven by ACTimelineVM via [setRefreshing] / [setCompleted]
 * / [setFailed] / [setIdle]. Subscribers receive the most recent value on subscribe.
 */
sealed class TimelineRefreshStatus {
    object Idle : TimelineRefreshStatus()
    object Refreshing : TimelineRefreshStatus()
    object Completed : TimelineRefreshStatus()
    data class Failed(val error: Throwable) : TimelineRefreshStatus()
}

object TimelineRefreshState {

    private val subject: BehaviorSubject<TimelineRefreshStatus> =
        BehaviorSubject.createDefault(TimelineRefreshStatus.Idle)

    /** Observable feed of the current status. Replays the latest value on subscribe. */
    val status: Observable<TimelineRefreshStatus> = subject.hide()

    /** Current value (synchronous). */
    fun current(): TimelineRefreshStatus = subject.value ?: TimelineRefreshStatus.Idle

    fun setRefreshing() = subject.onNext(TimelineRefreshStatus.Refreshing)
    fun setCompleted() = subject.onNext(TimelineRefreshStatus.Completed)
    fun setFailed(error: Throwable) = subject.onNext(TimelineRefreshStatus.Failed(error))
    fun setIdle() = subject.onNext(TimelineRefreshStatus.Idle)
}
