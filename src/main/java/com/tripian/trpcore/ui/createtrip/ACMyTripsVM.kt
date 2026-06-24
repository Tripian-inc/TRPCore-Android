package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.repository.TimelineRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [ACMyTrips] — the list of the user's existing, not-past
 * timelines shown when the SDK opens with no reservations. First paints from the
 * cache that [com.tripian.trpcore.ui.splash.ACSplashVM] populated (instant), then
 * refreshes from the network. [refresh] re-runs on resume so a trip created via
 * the FAB appears on return.
 */
class ACMyTripsVM @Inject constructor(
    private val timelineRepository: TimelineRepository
) : BaseViewModel() {

    val trips = MutableLiveData<List<Timeline>>(emptyList())

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)
        // Instant paint from the splash-populated cache.
        timelineRepository.cachedUserTimelines?.let { trips.value = TripDisplay.sortByStart(it) }
    }

    /** Fetch + filter not-past + sort; show the loader only when nothing is shown yet. */
    fun refresh() {
        val showLoader = trips.value.isNullOrEmpty()
        viewModelScope.launch {
            if (showLoader) showFullScreenLoaderNoText()
            val notPast = runCatching {
                TripDisplay.notPast(timelineRepository.getUserTimelinesAsync())
            }.getOrNull()
            hideLottieLoading()
            if (notPast != null) {
                timelineRepository.cachedUserTimelines = notPast
                trips.value = TripDisplay.sortByStart(notPast)
            }
        }
    }
}
