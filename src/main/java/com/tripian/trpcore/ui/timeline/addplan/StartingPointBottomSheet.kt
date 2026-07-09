package com.tripian.trpcore.ui.timeline.addplan

/**
 * Data class for starting point options
 * Used for tracking the selected starting point type in AddPlanContainerVM
 */
data class StartingPointOption(
    val id: Int,
    val nameResId: Int = 0,
    val iconResId: Int = 0,
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    companion object {
        const val CITY_CENTER = 0
        const val MY_ACCOMMODATION = 1
        const val SEARCH_LOCATION = 2
    }
}
