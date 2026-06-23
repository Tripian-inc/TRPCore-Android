package com.tripian.trpcore.base

/**
 * Tour-api content providers and their numeric ids.
 *
 * The active provider is global and host-configurable via [TRPCore.provider];
 * all tour-api operations (product-lookup, activity listing/search, availability,
 * POI-detail product filtering) read it instead of hard-coding an id.
 */
enum class TripianProvider(val id: Int) {
    CIVITATIS(15),
    NEXUS(7)
}
