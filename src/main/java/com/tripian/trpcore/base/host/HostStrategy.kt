package com.tripian.trpcore.base.host

import androidx.annotation.DrawableRes
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.R
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.util.Preferences

/**
 * HostStrategy — per-host integration policy (Strategy + Open/Closed principle).
 *
 * The base class encodes the ORIGINAL SDK behavior (Civitatis). When a new host
 * needs different behavior at one of these extension points it subclasses this
 * type and overrides ONLY the methods that differ — there are no host `if`
 * branches anywhere in the SDK, and adding a new customer never means editing an
 * existing call site. Register the active strategy on [com.tripian.trpcore.base.TRPCore.host];
 * the default instance is plain [HostStrategy].
 *
 * @see NexusHostStrategy
 */
open class HostStrategy {

    /**
     * Resolves the timeline's city id. Default: the SDK's coordinate/name based
     * resolution via [defaultResolver]. A host that already resolves cityIds
     * upstream returns its trusted value and skips resolution.
     */
    open fun resolveTimelineCityId(
        itinerary: ItineraryWithActivities,
        defaultResolver: () -> Int
    ): Int = defaultResolver()

    /**
     * Whether the timeline can be created straight from the itinerary, skipping
     * the cities/resolve round-trip. Default: false → the SDK resolves the
     * destination coordinates first.
     */
    open fun createsTimelineWithoutCityResolution(
        itinerary: ItineraryWithActivities
    ): Boolean = false

    /**
     * The product identifier to hand the host when an activity is tapped for its
     * detail. Default: the bare productId (original SDK behavior). A host whose
     * detail endpoint keys on a different identifier overrides this.
     */
    open fun activityDetailId(product: TourProduct): String =
        product.productId.ifEmpty { product.id }

    /**
     * Drawable used for an activity image when there is no source URL or it fails
     * to load. Default: the SDK's generic placeholder. A branded host overrides
     * this with its own logo.
     */
    @get:DrawableRes
    open val activityImageFallback: Int
        get() = R.drawable.trp_bg_place_holder_image

    /**
     * Anchors a booked-activity segment to a city. Default: leave [settings]
     * cityId unset so the server resolves it from the coordinate.
     */
    open fun anchorBookedActivityCityId(
        settings: TimelineSegmentSettings,
        item: SegmentActivityItem
    ) {
        /* default: server resolves the city from the coordinate */
    }

    /**
     * Builds the activity name→cityId map used by initial sync. Default: the
     * SDK's coordinate-based resolution via [defaultResolver].
     */
    open suspend fun resolveActivityCityMap(
        tripItems: List<SegmentActivityItem>,
        favouriteItems: List<SegmentFavoriteItem>,
        existing: Map<String, Int>,
        defaultResolver: suspend () -> Map<String, Int>
    ): Map<String, Int> = defaultResolver()

    /**
     * The tripHash to resume on launch when the host does not pass one itself.
     * Default: null — the host supplies its own hash.
     */
    open fun storedTripHash(preferences: Preferences): String? = null

    /** Persists the just-created tripHash. Default: no-op (the host owns the hash). */
    open fun onTimelineCreated(preferences: Preferences, tripHash: String) {
        /* default: nothing to persist */
    }

    /** Forgets any stored tripHash. Default: no-op. */
    open fun clearStoredTripHash(preferences: Preferences) {
        /* default: nothing stored */
    }

    /**
     * Whether to delete + recreate the timeline when its stored date range does
     * not overlap the new itinerary. Default: false → keep the existing timeline.
     */
    open fun recreatesTimelineOnDateMismatch(timeline: Timeline): Boolean = false
}
