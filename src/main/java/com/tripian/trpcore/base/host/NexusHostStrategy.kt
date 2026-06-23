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
     * Nexus /get-product keys on "{TYPE}|{id}" (e.g. "TKT|9148"), while the
     * tour-api productId is "{id}<U+00AC>{TYPE}" (e.g. "9148<U+00AC>TKT", where
     * U+00AC is the NOT SIGN separator). Convert by splitting on that separator
     * and swapping the halves around a '|'. Falls back to the raw productId if it
     * isn't in the expected two-part shape.
     */
    override fun activityDetailId(product: TourProduct): String {
        val parts = product.productId.split(PRODUCT_ID_SEPARATOR)
        return if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
            "${parts[1]}|${parts[0]}"
        } else {
            product.productId
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
}
