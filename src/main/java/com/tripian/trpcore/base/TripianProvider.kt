package com.tripian.trpcore.base

/**
 * Tour-api content providers, their numeric ids and the prefix the tour-api wraps a
 * product id with (`{prefix}{productId}_{providerId}[_{cityId}]`).
 *
 * The active provider is global and host-configurable via [TRPCore.provider];
 * all tour-api operations (product-lookup, activity listing/search, availability,
 * POI-detail product filtering) read it instead of hard-coding an id.
 */
enum class TripianProvider(val id: Int, val activityIdPrefix: String) {
    CIVITATIS(15, "C_"),
    NEXUS(7, "J_");

    companion object {
        /** Prefixes of every provider, for parsing ids stored before the active provider changed. */
        val knownPrefixes: List<String> = entries.map { it.activityIdPrefix }
    }
}
