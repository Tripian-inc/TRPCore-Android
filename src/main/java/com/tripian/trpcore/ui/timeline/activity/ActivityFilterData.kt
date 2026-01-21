package com.tripian.trpcore.ui.timeline.activity

import java.io.Serializable

/**
 * ActivityFilterData
 * Holds filter state for activity listing
 *
 * @property minPrice Minimum price in currency units (0 = Free)
 * @property maxPrice Maximum price in currency units
 * @property minDuration Minimum duration in minutes
 * @property maxDuration Maximum duration in minutes
 */
data class ActivityFilterData(
    val minPrice: Float = DEFAULT_MIN_PRICE,
    val maxPrice: Float = DEFAULT_MAX_PRICE,
    val minDuration: Float = DEFAULT_MIN_DURATION,
    val maxDuration: Float = DEFAULT_MAX_DURATION
) : Serializable {

    companion object {
        const val DEFAULT_MIN_PRICE = 0f
        const val DEFAULT_MAX_PRICE = 1500f
        const val DEFAULT_MIN_DURATION = 0f // 0 minutes
        const val DEFAULT_MAX_DURATION = 1440f // 24 hours in minutes

        const val PRICE_STEP = 10f
        const val DURATION_STEP = 30f // 30 minutes

        /**
         * Returns default filter (no filtering applied)
         */
        fun default() = ActivityFilterData()
    }

    /**
     * Check if any filter is applied (different from default values)
     */
    fun hasActiveFilters(): Boolean {
        return minPrice != DEFAULT_MIN_PRICE ||
                maxPrice != DEFAULT_MAX_PRICE ||
                minDuration != DEFAULT_MIN_DURATION ||
                maxDuration != DEFAULT_MAX_DURATION
    }

    /**
     * Count number of active filters
     * Price filter counts as 1 if either min or max is changed
     * Duration filter counts as 1 if either min or max is changed
     */
    fun activeFilterCount(): Int {
        var count = 0
        // Price filter
        if (minPrice != DEFAULT_MIN_PRICE || maxPrice != DEFAULT_MAX_PRICE) {
            count++
        }
        // Duration filter
        if (minDuration != DEFAULT_MIN_DURATION || maxDuration != DEFAULT_MAX_DURATION) {
            count++
        }
        return count
    }

    /**
     * Format duration in minutes to display string (e.g., "2h 30m", "0h", "24h")
     */
    fun formatDuration(minutes: Float): String {
        val totalMinutes = minutes.toInt()
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60

        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            mins > 0 -> "${mins}m"
            else -> "0h"
        }
    }

    /**
     * Format price with currency symbol based on SDK language
     * @param price Price value
     * @param currency Currency code (EUR, USD, etc.)
     * @return Formatted price string
     */
    fun formatPrice(price: Float, currency: String): String {
        val priceInt = price.toInt()

        // "Free" for 0 price
        if (priceInt == 0) {
            return "Free" // Will be replaced with localized string in UI
        }

        val symbol = when (currency.uppercase()) {
            "EUR" -> "€"
            "USD" -> "$"
            "GBP" -> "£"
            "TRY" -> "₺"
            else -> currency
        }

        // EUR: symbol after, others: symbol before
        return if (currency.uppercase() == "EUR") {
            "$priceInt$symbol"
        } else {
            "$symbol$priceInt"
        }
    }
}
