package com.tripian.trpcore.domain.model.timeline

import java.io.Serializable

/**
 * FilterData
 * Model for POI listing filter options
 * Contains selected category IDs for filtering POIs by category groups
 */
data class FilterData(
    val selectedCategoryIds: List<Int> = emptyList()
) : Serializable {

    /**
     * Returns true if no filters are applied
     */
    val isEmpty: Boolean
        get() = selectedCategoryIds.isEmpty()

    /**
     * Returns true if any filter is active
     */
    val hasActiveFilter: Boolean
        get() = !isEmpty

    /**
     * Returns the number of selected categories
     */
    val activeFilterCount: Int
        get() = selectedCategoryIds.size

    companion object {
        /**
         * Creates an empty FilterData with no filters applied
         */
        fun empty() = FilterData()
    }
}
