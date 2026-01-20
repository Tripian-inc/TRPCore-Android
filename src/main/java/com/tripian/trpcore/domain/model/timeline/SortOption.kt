package com.tripian.trpcore.domain.model.timeline

import com.tripian.trpcore.util.LanguageConst

/**
 * SortOption
 * Enum for POI listing sort options
 * Each option contains the language key for localization and API parameters
 */
enum class SortOption(
    val languageKey: String,
    val sortingBy: String,
    val sortingType: String
) {
    SCORE(LanguageConst.SORT_BY_POPULARITY, "score", "desc"),
    /**
     * Sort by popularity/rating (default)
     */
    POPULARITY(LanguageConst.SORT_BY_POPULARITY, "rating", "desc"),

    /**
     * Sort by rating (highest first)
     */
    RATING(LanguageConst.SORT_BY_RATING, "rating", "desc"),

    /**
     * Sort by price (lowest first)
     */
    PRICE_LOW_TO_HIGH(LanguageConst.SORT_BY_PRICE_LOW_HIGH, "price", "asc"),

    /**
     * Sort by duration (shortest first)
     */
    DURATION_SHORT_TO_LONG(LanguageConst.SORT_BY_DURATION, "duration", "asc"),

    /**
     * Sort by duration (longest first)
     */
    DURATION_LONG_TO_SHORT(LanguageConst.SORT_BY_DURATION_LONG_SHORT, "duration", "desc");

    companion object {
        /**
         * Default sort option
         */
        val DEFAULT = POPULARITY

        /**
         * Find sort option by sortingBy and sortingType
         */
        fun fromApiParams(sortingBy: String?, sortingType: String?): SortOption {
            if (sortingBy == null) return DEFAULT
            return entries.find {
                it.sortingBy == sortingBy && it.sortingType == sortingType
            } ?: DEFAULT
        }
    }
}
