package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.domain.model.itinerary.SegmentDestinationItem
import com.tripian.trpcore.util.event.SingleLiveEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for [ACDateSelection] — step 2 of the create-trip flow. Holds the
 * city chosen on [ACCitySelection] and builds the [ItineraryWithActivities]
 * (one destination + the picked date range, no activities) for timeline creation.
 */
class ACDateSelectionVM @Inject constructor() : BaseViewModel() {

    var city: City? = null
        private set

    /** Emitted on Next with the built itinerary; the Activity hands off to the SDK. */
    val onBuildItinerary = SingleLiveEvent<ItineraryWithActivities>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)
        @Suppress("DEPRECATION")
        city = arguments?.getSerializable(ARG_CITY) as? City
    }

    /**
     * Build the itinerary from the chosen city + range. [endMillis] may equal
     * [startMillis] for a single-day trip.
     */
    fun buildAndContinue(startMillis: Long, endMillis: Long, uniqueId: String?) {
        val c = city ?: return
        val coord = c.coordinate ?: return
        val start = dateFormat.format(Date(startMillis))
        val end = dateFormat.format(Date(if (endMillis >= startMillis) endMillis else startMillis))

        val destination = SegmentDestinationItem(
            title = c.name.orEmpty(),
            coordinate = "${coord.lat},${coord.lng}",
            cityId = c.id,
            countryName = c.country?.name
        )

        onBuildItinerary.value = ItineraryWithActivities(
            startDatetime = "$start 00:00",
            endDatetime = "$end 23:59",
            uniqueId = uniqueId.orEmpty(),
            tripianHash = null,
            destinationItems = listOf(destination),
            tripItems = emptyList()
        )
    }

    companion object {
        const val ARG_CITY = "selected_city"
    }
}
