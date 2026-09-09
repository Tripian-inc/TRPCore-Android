package com.tripian.trpcore.util

import com.tripian.trpcore.base.TRPCore

/**
 * Single builder/parser for the API's activity id form. Ids reach the SDK either
 * bare ("15423") or already prefixed ("C_15423_15_28"); [base] reduces any variant
 * to the bare product id and [make] rebuilds the full form.
 */
object ActivityIdFormat {

    /** The host-configured tour-api provider, used when a caller has no better source. */
    val DEFAULT_PROVIDER_ID: Int
        get() = TRPCore.provider.id

    /**
     * `C_{productId}_{providerId}`, plus `_{cityId}` when known. [activityId] may be
     * plain or already `C_`-prefixed; it is reduced to its bare product id either way.
     * Returns an empty string when [activityId] carries no id.
     */
    fun make(
        activityId: String?,
        providerId: Int = DEFAULT_PROVIDER_ID,
        cityId: Int? = null
    ): String {
        val baseId = base(activityId) ?: return ""
        val provider = if (providerId > 0) providerId else DEFAULT_PROVIDER_ID
        return if (cityId != null) "C_${baseId}_${provider}_$cityId" else "C_${baseId}_$provider"
    }

    /**
     * Strips the "C_" prefix and any provider/city suffixes, leaving the base id.
     * "C_15423_15_28" and "15423" both return "15423"; null for a blank input.
     */
    fun base(activityId: String?): String? {
        if (activityId.isNullOrBlank()) return null
        return activityId
            .removePrefix("C_")
            .split("_")
            .firstOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
