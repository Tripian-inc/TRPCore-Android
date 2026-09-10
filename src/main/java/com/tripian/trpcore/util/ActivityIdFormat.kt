package com.tripian.trpcore.util

import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.base.TripianProvider

/**
 * Single builder/parser for the API's activity id form. Ids reach the SDK either
 * bare ("15423") or already prefixed ("C_15423_15_28", "J_9148_7"); [base] reduces
 * any variant to the bare product id and [make] rebuilds the full form with the
 * active provider's prefix.
 */
object ActivityIdFormat {

    /** The host-configured tour-api provider, used when a caller has no better source. */
    val DEFAULT_PROVIDER_ID: Int
        get() = TRPCore.provider.id

    /**
     * `{prefix}{productId}_{providerId}`, plus `_{cityId}` when known; the prefix is the
     * active provider's. [activityId] may be plain or already prefixed; it is reduced to
     * its bare product id either way. Returns an empty string when [activityId] carries no id.
     */
    fun make(
        activityId: String?,
        providerId: Int = DEFAULT_PROVIDER_ID,
        cityId: Int? = null
    ): String {
        val baseId = base(activityId) ?: return ""
        val provider = if (providerId > 0) providerId else DEFAULT_PROVIDER_ID
        val prefix = TRPCore.provider.activityIdPrefix
        return if (cityId != null) "$prefix${baseId}_${provider}_$cityId" else "$prefix${baseId}_$provider"
    }

    /**
     * Strips any provider prefix and the provider/city suffixes, leaving the base id.
     * "C_15423_15_28", "J_15423_7" and "15423" all return "15423"; null for a blank input.
     */
    fun base(activityId: String?): String? {
        if (activityId.isNullOrBlank()) return null
        val prefix = TripianProvider.knownPrefixes.firstOrNull { activityId.startsWith(it) }.orEmpty()
        return activityId
            .removePrefix(prefix)
            .split("_")
            .firstOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
