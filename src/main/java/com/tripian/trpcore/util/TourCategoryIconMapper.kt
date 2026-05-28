package com.tripian.trpcore.util

import androidx.annotation.DrawableRes
import com.tripian.trpcore.R

/**
 * Maps a facet category `key` (e.g. `"activity_main_category_1"`) to a local drawable
 * resource. Mirrors `TRPTourCategoryIconMapper` on iOS. Unknown keys (including a
 * null key) fall back to [DEFAULT_ICON].
 *
 * Used by the AddPlan Activity Listing category chip strip.
 */
object TourCategoryIconMapper {

    /** Icon shown next to the "All" chip that clears category filtering. */
    @DrawableRes
    val ALL_CATEGORIES_ICON: Int = R.drawable.trp_ic_all_categories

    /** Fallback drawable for keys we don't have an explicit mapping for. */
    @DrawableRes
    val DEFAULT_ICON: Int = R.drawable.trp_ic_cat_experiences

    @DrawableRes
    fun iconRes(key: String?): Int = when (key) {
        "activity_main_category_1" -> R.drawable.trp_ic_cat_activities
        "activity_main_category_2" -> R.drawable.trp_ic_cat_excursions
        "activity_main_category_4" -> R.drawable.trp_ic_cat_actions
        "activity_main_category_5" -> R.drawable.trp_ic_cat_tickets
        "activity_main_category_6" -> R.drawable.trp_ic_cat_shows
        "activity_main_category_7" -> R.drawable.trp_ic_cat_passes
        "activity_main_category_8" -> R.drawable.trp_ic_cat_passes
        "activity_main_category_9" -> R.drawable.trp_ic_cat_food_drinks
        "activity_main_category_11" -> R.drawable.trp_ic_cat_transfers
        "activity_main_category_12" -> R.drawable.trp_ic_cat_services
        "activity_main_category_13" -> R.drawable.trp_ic_cat_experiences
        else -> DEFAULT_ICON
    }
}
