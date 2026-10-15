package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.analytics.TRPAnalytics
import com.tripian.trpcore.analytics.TRPAnalyticsEvent
import com.tripian.trpcore.analytics.TRPAnalyticsParam
import com.tripian.trpcore.analytics.TRPAnalyticsScreen
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.util.event.SingleLiveEvent
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [ACCitySelection] — the first step of the native "create trip
 * from scratch" flow. Loads the cached city list (prefetching if the cache is
 * cold, which it is on the empty-reservations path), exposes the popular subset
 * for the "Top Destinations" chips, supports name search and SINGLE selection.
 */
class ACCitySelectionVM @Inject constructor(
    private val tripRepository: TripRepository
) : BaseViewModel() {

    private val allCities = mutableListOf<City>()

    /** Filtered "All destinations" list (by the current search query). */
    val cities = MutableLiveData<List<City>>(emptyList())

    /** Cities flagged `isPopular` — drives the "Top Destinations" chip row. */
    val popularCities = MutableLiveData<List<City>>(emptyList())

    /** The single selected city (null until one is picked). */
    val selectedCity = MutableLiveData<City?>(null)

    /** Open the date step with the chosen city. */
    val onCitySelectedNext = SingleLiveEvent<City>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)
        TRPAnalytics.screen(TRPAnalyticsScreen.CITY_SELECTION)
        loadCities()
    }

    private fun loadCities() {
        viewModelScope.launch {
            if (!tripRepository.hasCachedCities()) {
                showFullScreenLoaderNoText()
                runCatching { tripRepository.prefetchCitiesAsync() }
                hideLottieLoading()
            }
            val all = tripRepository.getCachedCities() // already name-sorted
            allCities.clear()
            allCities.addAll(all)
            cities.value = all
            // TODO(isPopular): after TRPOne >= 1.4.9 (City.isPopular published) +
            // dependency bump, switch to: popularCities.value = all.filter { it.isPopular }
            popularCities.value = emptyList()
        }
    }

    fun search(query: String) {
        val q = query.trim().lowercase()
        val results = if (q.isEmpty()) {
            allCities
        } else {
            allCities.filter { (it.name ?: "").lowercase().contains(q) }
        }
        cities.value = results
        if (q.isNotEmpty()) {
            TRPAnalytics.log(
                TRPAnalyticsEvent.SEARCH,
                mapOf(
                    TRPAnalyticsParam.SCREEN_NAME to TRPAnalyticsScreen.CITY_SELECTION,
                    TRPAnalyticsParam.QUERY to q,
                    TRPAnalyticsParam.RESULT_COUNT to results.size
                )
            )
        }
    }

    /** Single selection — selecting a city replaces any previous one. */
    fun select(city: City) {
        selectedCity.value = city
    }

    fun proceed() {
        val city = selectedCity.value ?: return
        TRPAnalytics.log(
            TRPAnalyticsEvent.CITY_SELECTED,
            mapOf(
                TRPAnalyticsParam.CITY_ID to city.id,
                TRPAnalyticsParam.CITY_NAME to city.name
            )
        )
        onCitySelectedNext.value = city
    }
}
