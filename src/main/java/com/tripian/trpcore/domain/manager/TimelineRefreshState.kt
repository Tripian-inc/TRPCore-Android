package com.tripian.trpcore.domain.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global state holder for the "timeline is being refreshed" status.
 *
 * The single timeline view-model owns the actual refresh operation, but other
 * screens that can be open simultaneously (Saved Plans, the AddPlan Time
 * Selection bottom sheet) need to react to it: they show a small loader while
 * a refresh is in flight and surface a confirmation when it completes.
 *
 * State transitions are driven by ACTimelineVM via [setRefreshing] / [setCompleted]
 * / [setFailed] / [setIdle]. Subscribers receive the most recent value on collect.
 */
sealed class TimelineRefreshStatus {
    object Idle : TimelineRefreshStatus()
    object Refreshing : TimelineRefreshStatus()
    object Completed : TimelineRefreshStatus()
    data class Failed(val error: Throwable) : TimelineRefreshStatus()
}

object TimelineRefreshState {

    private val _status = MutableStateFlow<TimelineRefreshStatus>(TimelineRefreshStatus.Idle)

    /** Hot state feed; collectors get the latest value on subscribe. */
    val status: StateFlow<TimelineRefreshStatus> = _status.asStateFlow()

    /** Current value (synchronous). */
    fun current(): TimelineRefreshStatus = _status.value

    fun setRefreshing() { _status.value = TimelineRefreshStatus.Refreshing }
    fun setCompleted() { _status.value = TimelineRefreshStatus.Completed }
    fun setFailed(error: Throwable) { _status.value = TimelineRefreshStatus.Failed(error) }
    fun setIdle() { _status.value = TimelineRefreshStatus.Idle }
}
