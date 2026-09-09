package com.tripian.trpcore.base.host

import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.R
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.util.Preferences

/**
 * NexusHostStrategy — Nexus host integration policy.
 *
 * Nexus pre-resolves every cityId in ACSplash (product-lookup / juniper
 * destinationId→cityId) and does NOT pass a tripHash, so the SDK stores it
 * internally and recreates the timeline when the reservation dates move out of
 * range. Each override here replaces exactly one default behavior; nothing else
 * in the SDK changes. See [HostStrategy].
 */
class NexusHostStrategy : HostStrategy() {

    /**
     * Nexus /get-product keys on "{TYPE}|{id}" (e.g. "TKT|9148"), while tour-api
     * ids are "{id}<U+00AC>{TYPE}" (e.g. "9148<U+00AC>TKT", U+00AC = NOT SIGN) —
     * this is what both activity-listing products AND tapped timeline segments/
     * steps carry (additionalData.activityId / poi.additionalData.productId).
     * Convert by swapping the two halves around a '|'. Idempotent: ids already in
     * the host form (no separator) or not in the "{digits}¬{TYPE}" shape pass
     * through unchanged.
     */
    override fun activityDetailIdFromRaw(rawId: String): String {
        if (!rawId.contains(PRODUCT_ID_SEPARATOR)) return rawId
        val parts = rawId.split(PRODUCT_ID_SEPARATOR)
        return if (parts.size == 2 &&
            parts[0].isNotEmpty() && parts[0].all { it.isDigit() } &&
            parts[1].isNotEmpty()
        ) {
            "${parts[1]}|${parts[0]}"
        } else {
            rawId
        }
    }

    /** Nexus brand logo for activities with no image. */
    override val activityImageFallback: Int
        get() = R.drawable.ic_nexus_logo

    private companion object {
        /** Tour-api productId separator (NOT SIGN, U+00AC) — built from its code
         *  point so the source stays pure ASCII across build environments. */
        val PRODUCT_ID_SEPARATOR: Char = 0xAC.toChar()
    }

    /** Trust the cityId already resolved per-destination upstream; skip resolution. */
    override fun resolveTimelineCityId(
        itinerary: ItineraryWithActivities,
        defaultResolver: () -> Int
    ): Int = itinerary.destinationItems.firstOrNull()?.cityId?.takeIf { it != 0 }
        ?: defaultResolver()

    /** True once every destination carries a trusted (non-zero) cityId. */
    override fun createsTimelineWithoutCityResolution(
        itinerary: ItineraryWithActivities
    ): Boolean = itinerary.destinationItems.isNotEmpty() &&
        itinerary.destinationItems.all { (it.cityId ?: 0) != 0 }

    /**
     * Anchor to the cityId resolved upstream — REQUIRED for no-location
     * activities (no coordinate → server leaves cityId 0, and the deleted-cities
     * sync would prune every cityId<=0 segment on the next open).
     */
    override fun anchorBookedActivityCityId(
        settings: TimelineSegmentSettings,
        item: SegmentActivityItem
    ) {
        item.cityId?.takeIf { it > 0 }?.let { settings.cityId = it }
    }

    /**
     * ACSplash already resolved each activity's cityId from its destinationId, so
     * build the name→id map locally instead of calling the cities/resolve API.
     */
    override suspend fun resolveActivityCityMap(
        tripItems: List<SegmentActivityItem>,
        favouriteItems: List<SegmentFavoriteItem>,
        existing: Map<String, Int>,
        defaultResolver: suspend () -> Map<String, Int>
    ): Map<String, Int> {
        val map = existing.toMutableMap()
        tripItems.forEach { item ->
            val name = item.cityName
            val id = item.cityId ?: 0
            if (name != null && id > 0) map[name] = id
        }
        favouriteItems.forEach { item ->
            val id = item.cityId ?: 0
            if (id > 0) map[item.cityName] = id
        }
        return map
    }

    override fun storedTripHash(preferences: Preferences): String? =
        preferences.getString(Preferences.Keys.SAVED_TRIP_HASH)?.takeIf { it.isNotBlank() }

    override fun onTimelineCreated(preferences: Preferences, tripHash: String) {
        preferences.setString(Preferences.Keys.SAVED_TRIP_HASH, tripHash)
    }

    override fun clearStoredTripHash(preferences: Preferences) {
        preferences.deleteKey(Preferences.Keys.SAVED_TRIP_HASH)
    }

    /** Reservations that move out of the stored range → delete + recreate. */
    override fun recreatesTimelineOnDateMismatch(timeline: Timeline): Boolean = true

    /** No reservations → let the user pick a city + dates and create a timeline. */
    override fun createsTimelineFromScratchOnEmpty(): Boolean = true

    /** Nexus has its own onboarding; don't show the SDK's. */
    override fun showsOnboarding(): Boolean = false

    /** Hide the activity-listing category strip for now. */
    override fun showsActivityCategories(): Boolean = false

    /** Skip the schedule-bulk availability sweep for now. */
    override fun runsAvailabilitySweep(): Boolean = false

    /** Draw the day's walking/driving route between reservations and plan steps. */
    override fun drawsRoutesOnMap(): Boolean = true

    /** List the day's reservations and plan steps chronologically, routed end to end. */
    override fun usesFlatTimeline(): Boolean = true
}
