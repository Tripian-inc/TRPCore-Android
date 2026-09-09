package com.tripian.trpcore.ui.splash

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.DoLightLogin
import com.tripian.trpcore.domain.model.itinerary.ItineraryCoordinate
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityPrice
import com.tripian.trpcore.domain.model.itinerary.SegmentDestinationItem
import com.tripian.trpcore.domain.usecase.timeline.LookupTourProductUseCase
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.ui.createtrip.TripDisplay
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.repository.experience.ExperienceRepository
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.event.SingleLiveEvent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

private const val TRIP_END_PADDING_DAYS = 5

/**
 * Entry-point ViewModel used when the SDK is started from a host app (e.g. the
 * Nexus Capacitor bridge launches [ACSplash] with the user's reservations).
 *
 * Flow: light login (no email/password) → build an [ItineraryWithActivities]
 * from the reservations → the Activity hands off to the SDK timeline via
 * TRPCore.startWithItinerary.
 *
 * Per reservation the `detailURL` carries `destinationID`, `startDate`, `endDate`
 * and (optionally) `productId`/`providerId`. Destination resolution priority:
 *   1. product-lookup (productId + providerId) → TourProduct (cityId + coordinate)
 *   2. fallback: destinationID → Tripian cityId (juniper) → city centre coordinate
 * Each reservation becomes a booked-activity [SegmentActivityItem] (tripItems) and
 * contributes its city as a [SegmentDestinationItem] (one per unique city). Each
 * destination keeps its own cityId so TRPCore creates the timeline without
 * re-resolving. The stored tripHash (if any) is carried via `tripianHash`.
 */
class ACSplashVM @Inject constructor(
    private val doLightLogin: DoLightLogin,
    private val tripRepository: TripRepository,
    private val timelineRepository: TimelineRepository,
    private val lookupTourProductUseCase: LookupTourProductUseCase,
    private val preferences: Preferences
) : BaseViewModel() {

    /** Emitted once the itinerary is built; the Activity calls startWithItinerary. */
    val onItineraryReady = SingleLiveEvent<ItineraryWithActivities>()

    /**
     * Emitted when there are no reservations and the host allows creating a trip
     * from scratch ([com.tripian.trpcore.base.host.HostStrategy.createsTimelineFromScratchOnEmpty]);
     * the Activity opens the native city-selection flow.
     */
    val onShowCreateTrip = SingleLiveEvent<Unit>()

    /**
     * Emitted (instead of [onShowCreateTrip]) when the user already has at least
     * one not-past timeline; the Activity opens the native "My Trips" list.
     */
    val onShowMyTrips = SingleLiveEvent<Unit>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)
        showFullScreenLoaderNoText()
        prepare()
    }

    private fun prepare() {
        val uniqueId = arguments?.getString("uniqueId")

        viewModelScope.launch {
            runCatching {
                // Light login — uniqueId only (device-derived guest id when absent).
                // No firstName/lastName are sent.
                doLightLogin(
                    DoLightLogin.Params(uniqueId = uniqueId)
                )
                buildItinerary()
            }.onSuccess { itinerary ->
                if (itinerary != null) {
                    onItineraryReady.value = itinerary
                } else if (TRPCore.host.createsTimelineFromScratchOnEmpty()) {
                    // No reservations → host policy: show the user's existing
                    // not-past timelines if any, otherwise the create-trip flow.
                    // Light login already ran above. A fetch failure falls back
                    // to create (never errors the SDK).
                    val notPast = runCatching {
                        TripDisplay.notPast(timelineRepository.getUserTimelinesAsync())
                    }.getOrDefault(emptyList())
                    hideLottieLoading()
                    if (notPast.isNotEmpty()) {
                        timelineRepository.cachedUserTimelines = notPast
                        onShowMyTrips.call()
                    } else {
                        onShowCreateTrip.call()
                    }
                } else {
                    fail("No resolvable destination in reservations")
                }
            }.onFailure { t ->
                fail((t as? ErrorModel)?.errorDesc ?: t.message ?: "Failed to start")
            }
        }
    }

    private fun fail(message: String) {
        hideLottieLoading()
        TRPCore.notifyError(message)
        finishActivity()
    }

    /**
     * Parse the reservations JSON. Each reservation becomes a booked-activity
     * tripItem and contributes its (de-duplicated) city as a destination.
     */
    private suspend fun buildItinerary(): ItineraryWithActivities? {
        val reservationsJson = arguments?.getString("reservations") ?: return null
        val reservations = runCatching {
            Gson().fromJson(reservationsJson, Array<NexusReservationDto>::class.java)?.toList()
        }.getOrNull().orEmpty()
        if (reservations.isEmpty()) return null

        // Populate the city cache so getCachedCityById() can resolve centres.
        tripRepository.prefetchCitiesAsync()

        val destinations = LinkedHashMap<Int, SegmentDestinationItem>()
        val activities = mutableListOf<SegmentActivityItem>()
        val startDates = mutableListOf<String>()
        val endDates = mutableListOf<String>()

        reservations.forEach { r ->
            val uri = r.detailURL?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() }
            val startDate = uri?.getQueryParameter("startDate")
            val endDate = uri?.getQueryParameter("endDate")
            val serviceDate = r.date?.takeIf { it.length >= 10 }?.take(10)
            (startDate ?: serviceDate ?: endDate)?.let { startDates.add(it) }
            (endDate ?: serviceDate ?: startDate)?.let { endDates.add(it) }

            // The destination/activity build below needs a parseable detailURL.
            if (uri == null) return@forEach
            val url = r.detailURL ?: return@forEach

            // Priority 1: product-lookup with the fixed Nexus providerId (7) and the
            // detailURL's `productID`. Falls back to the destinationID→cityId path
            // below when there is no productID or the lookup yields nothing.
            val productId = uri.getQueryParameter("productID") ?: uri.getQueryParameter("productId")
            val product = if (!productId.isNullOrBlank()) {
                runCatching {
                    lookupTourProductUseCase(
                        LookupTourProductUseCase.Params(
                            providerId = TRPCore.provider.id,
                            productId = productId
                        )
                    ).data?.product
                }.getOrNull()
            } else null

            // City: product cityId, else destinationID → juniper cityId.
            val cityId = product?.cityId?.takeIf { it != 0 }
                ?: uri.getQueryParameter("destinationID")?.takeIf { it.isNotBlank() }
                    ?.let { ExperienceRepository.getCityIdFromDestination(it) }
                ?: return@forEach

            val city = tripRepository.getCachedCityById(cityId)
            val center = city?.coordinate
            val loc = product?.locations?.firstOrNull()
            // Activity coordinate: ONLY the product's real location. When absent,
            // it stays null → the activity renders as "no exact location" instead
            // of falling back to the city centre.
            val locLat = loc?.lat
            val locLng = loc?.lon
            val activityCoord = if (locLat != null && locLng != null && !(locLat == 0.0 && locLng == 0.0)) {
                ItineraryCoordinate(locLat, locLng)
            } else null
            // The destination (city) still needs a centre coordinate for the timeline.
            val centerLat = center?.lat ?: activityCoord?.lat
            val centerLng = center?.lng ?: activityCoord?.lng
            if (centerLat == null || centerLng == null) {
                return@forEach
            }

            // Destination — one per unique city, placed at the city centre.
            if (!destinations.containsKey(cityId)) {
                destinations[cityId] = SegmentDestinationItem(
                    title = city?.name ?: product?.title.orEmpty(),
                    coordinate = "$centerLat,$centerLng",
                    cityId = cityId,
                    dates = listOfNotNull(startDate, endDate).distinct().ifEmpty { null },
                    countryName = city?.country?.name
                )
            }

            // Booked activity — one per reservation.
            val priceValue = product?.currentPrice ?: product?.price ?: r.amountClient
            val price = priceValue?.let {
                SegmentActivityPrice(currency = product?.currency ?: r.currencyClient ?: "EUR", value = it)
            }
            val imageUrl = product?.images?.firstOrNull { it.isCover == true }?.url
                ?: product?.images?.firstOrNull()?.url
                ?: r.nexusBookingInformation?.coverImageUrl
            // Each reservation sits on ITS OWN date (TripService.Date), not the
            // shared trip-range startDate — otherwise both land on day one.
            val activityStart = reservationDatetime(r.date)
                ?: startDate?.let { "${it.take(10)} 09:00" }
            activities.add(
                SegmentActivityItem(
                    activityId = productId ?: product?.productId,
                    bookingId = r.locator,
                    title = product?.title ?: r.title,
                    imageUrl = imageUrl,
                    description = product?.description ?: r.nexusBookingInformation?.description,
                    startDatetime = activityStart,
                    // With a known duration the SDK computes the end; otherwise keep
                    // it on the same slot so it doesn't span to the trip end.
                    endDatetime = if (product?.duration != null) null else activityStart,
                    coordinate = activityCoord,
                    cancellation = r.nexusBookingInformation?.cancellationPolicyDescription,
                    adultCount = r.paxAdults ?: 1,
                    childCount = (r.paxChildren ?: 0) + (r.paxBabies ?: 0),
                    bookingUrl = url,
                    duration = product?.duration,
                    price = price,
                    cityId = cityId,
                    cityName = city?.name,
                    countryName = city?.country?.name
                )
            )
        }

        if (destinations.isEmpty() && activities.isEmpty()) return null

        val start = startDates.minOrNull()
        val end = (endDates.maxOrNull() ?: start)?.let { plusDaysUtc(it, TRIP_END_PADDING_DAYS) }
        // Host policy: the stored tripHash to resume (null when the host passes its own).
        val storedHash = TRPCore.host.storedTripHash(preferences)

        return ItineraryWithActivities(
            startDatetime = toDatetime(start, "00:00"),
            endDatetime = toDatetime(end, "23:59"),
            uniqueId = arguments?.getString("uniqueId").orEmpty(),
            tripianHash = storedHash,
            destinationItems = destinations.values.toList(),
            tripItems = activities
        )
    }

    /** Normalize a "yyyy-MM-dd" date (or null) into the SDK's "yyyy-MM-dd HH:mm". */
    private fun toDatetime(date: String?, time: String): String {
        val d = date?.takeIf { it.isNotBlank() }
        return when {
            d == null -> "${today()} $time"
            d.length >= 16 && d.contains(' ') -> d
            else -> "${d.take(10)} $time"
        }
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /**
     * Shifts a "yyyy-MM-dd" date by [days] on the UTC Gregorian calendar,
     * returning "yyyy-MM-dd". Returns [date] unchanged when it cannot be parsed.
     */
    private fun plusDaysUtc(date: String, days: Int): String = runCatching {
        val utc = TimeZone.getTimeZone("UTC")
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = utc
            isLenient = false
        }
        val calendar = GregorianCalendar(utc).apply {
            time = format.parse(date)!!
            add(Calendar.DAY_OF_MONTH, days)
        }
        format.format(calendar.time)
    }.getOrDefault(date)

    /**
     * Converts a reservation date — e.g. "2026-07-02T20:30:00" (ISO) or
     * "2026-07-02 20:30" or "2026-07-02" — into the SDK's "yyyy-MM-dd HH:mm".
     * Returns null when the date part is missing/malformed.
     */
    private fun reservationDatetime(raw: String?): String? {
        val s = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (s.length < 10) return null
        val datePart = s.take(10)
        if (datePart[4] != '-' || datePart[7] != '-') return null
        val timePart = if (s.length >= 16 && (s[10] == 'T' || s[10] == ' ')) {
            s.substring(11, 16)
        } else {
            "09:00"
        }
        return "$datePart $timePart"
    }
}
