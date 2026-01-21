package com.tripian.trpcore.domain.model.timeline

import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.trip.model.Accommodation
import com.tripian.trpcore.R
import com.tripian.trpcore.util.LanguageConst
import java.io.Serializable
import java.util.Calendar
import java.util.Date

/**
 * AddPlanData
 * Holds data collected throughout the AddPlan flow
 * iOS Reference: AddPlanContainerVC.swift
 */
data class AddPlanData(
    // Selected day
    var selectedDay: Date? = null,
    var selectedDayIndex: Int = 0,

    // Selected city
    var selectedCity: City? = null,

    // Mode selection
    var selectedMode: AddPlanMode = AddPlanMode.NONE,

    // Manual mode - selected category
    var selectedManualCategory: ManualCategory? = null,

    // Starting point (Smart Recommendations)
    var startingPointLocation: Coordinate? = null,
    var startingPointName: String? = null,
    var startingPointAccommodation: Accommodation? = null,

    // Time information (Smart Recommendations) - String format "HH:mm"
    var startTime: String? = null,
    var endTime: String? = null,

    // Legacy time fields (Date format) - for backward compatibility
    var startTimeDate: Date? = null,
    var endTimeDate: Date? = null,

    // Traveler count (Smart Recommendations)
    var travelers: Int = 1,

    // Legacy traveler fields - for backward compatibility
    var adults: Int = 1,
    var children: Int = 0,

    // Selected categories (Smart Recommendations - multi-select)
    var selectedSmartCategories: MutableList<SmartCategory> = mutableListOf(),

    // Legacy categories - for backward compatibility
    var selectedCategories: MutableList<String> = mutableListOf(),

    // Selected activity IDs (from Favorites)
    var activityIds: MutableList<String> = mutableListOf(),

    // Manually added POI
    var selectedPoi: Poi? = null,

    // Trip hash
    var tripHash: String? = null,

    // Available days and cities
    var availableDays: List<Date> = emptyList(),
    var cities: List<City> = emptyList(),

    // Legacy accommodation field - for backward compatibility
    var accommodation: Accommodation? = null
) : Serializable {

    // =====================
    // BACKWARD COMPATIBILITY ALIASES
    // =====================

    /** Alias for selectedDay - backward compatibility */
    var selectedDate: Date?
        get() = selectedDay
        set(value) { selectedDay = value }

    /** Alias for selectedMode - backward compatibility */
    var mode: AddPlanMode
        get() = selectedMode
        set(value) { selectedMode = value }

    /**
     * Categories as comma-separated string for API
     */
    val smartCategoriesAsString: String
        get() = selectedSmartCategories.map { it.apiValue }.joinToString(",")

    /**
     * Validation for Smart Recommendations mode
     */
    fun isValidForSmartMode(): Boolean {
        return selectedDay != null &&
                selectedCity != null &&
                startTime != null &&
                endTime != null &&
                startingPointLocation != null &&
                selectedSmartCategories.isNotEmpty()
    }

    /**
     * Validation for Manual mode
     */
    fun isValidForManualMode(): Boolean {
        return selectedDay != null &&
                selectedCity != null &&
                selectedManualCategory != null
    }

    /**
     * Check if continue button should be enabled on SelectDay screen
     */
    fun canContinueFromSelectDay(): Boolean {
        return when (selectedMode) {
            AddPlanMode.SMART_RECOMMENDATIONS, AddPlanMode.SMART -> true
            AddPlanMode.MANUAL -> selectedManualCategory != null
            AddPlanMode.NONE -> false
        }
    }

    /**
     * Check if continue button should be enabled on TimeAndTravelers screen
     */
    fun canContinueFromTimeAndTravelers(): Boolean {
        return startingPointLocation != null &&
                startTime != null &&
                endTime != null &&
                travelers >= 1
    }

    /**
     * Check if continue button should be enabled on CategorySelection screen
     */
    fun canContinueFromCategorySelection(): Boolean {
        return selectedSmartCategories.isNotEmpty()
    }

    /**
     * Legacy isValid() method - backward compatibility
     * Checks if data is valid for the selected mode
     */
    fun isValid(): Boolean {
        return when (selectedMode) {
            AddPlanMode.SMART_RECOMMENDATIONS, AddPlanMode.SMART -> isValidForSmartMode()
            AddPlanMode.MANUAL -> isValidForManualMode()
            AddPlanMode.NONE -> false
        }
    }

    /**
     * Reset all data
     */
    fun reset() {
        selectedDay = null
        selectedDayIndex = 0
        selectedCity = null
        selectedMode = AddPlanMode.NONE
        selectedManualCategory = null
        startingPointLocation = null
        startingPointName = null
        startingPointAccommodation = null
        startTime = null
        endTime = null
        travelers = 1
        selectedSmartCategories.clear()
        activityIds.clear()
        selectedPoi = null
    }

    /**
     * Clear selection (mode and categories)
     */
    fun clearSelection() {
        selectedMode = AddPlanMode.NONE
        selectedManualCategory = null
        selectedSmartCategories.clear()
    }

    companion object {
        /**
         * Default start time string (10:00)
         */
        fun getDefaultStartTime(): String = "10:00"

        /**
         * Default end time string (18:00)
         */
        fun getDefaultEndTime(): String = "18:00"

        /**
         * Default start time as Date - backward compatibility overload
         */
        fun getDefaultStartTime(date: Date): Date {
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.set(Calendar.HOUR_OF_DAY, 10)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            return calendar.time
        }

        /**
         * Default end time as Date - backward compatibility overload
         */
        fun getDefaultEndTime(date: Date): Date {
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.set(Calendar.HOUR_OF_DAY, 18)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            return calendar.time
        }

        /**
         * Default start time as Date (alias)
         */
        fun getDefaultStartTimeDate(date: Date): Date = getDefaultStartTime(date)

        /**
         * Default end time as Date (alias)
         */
        fun getDefaultEndTimeDate(date: Date): Date = getDefaultEndTime(date)
    }
}

/**
 * AddPlan Mode
 * iOS Reference: AddPlanMode enum
 */
enum class AddPlanMode {
    NONE,               // No mode selected
    SMART_RECOMMENDATIONS, // AI-powered smart recommendations
    SMART,              // Alias for SMART_RECOMMENDATIONS (backward compatibility)
    MANUAL              // Manual POI/Activity addition
}

/**
 * AddPlan Step
 * iOS Reference: AddPlanStep enum
 */
enum class AddPlanStep {
    SELECT_DAY_AND_CITY,  // Step 1: Day, City, Mode selection
    TIME_AND_TRAVELERS,   // Step 2: Starting point, time, travelers (Smart only)
    CATEGORY_SELECTION    // Step 3: Category selection (Smart only)
}

/**
 * Smart Recommendation Categories (7 categories)
 * iOS Reference: CategorySelection grid items
 */
enum class SmartCategory(
    val apiValue: String,
    val iconRes: Int,
    val titleKey: String
) {
    GUIDED_TOURS(
        apiValue = "guided tours, free tours",
        iconRes = R.drawable.ic_activities,
        titleKey = LanguageConst.ADD_PLAN_CAT_GUIDED_TOURS
    ),
    TICKETS(
        apiValue = "tickets",
        iconRes = R.drawable.ic_cat_tickets,
        titleKey = LanguageConst.ADD_PLAN_CAT_TICKETS
    ),
    EXCURSIONS(
        apiValue = "day trip",
        iconRes = R.drawable.ic_cat_excursions,
        titleKey = LanguageConst.ADD_PLAN_CAT_EXCURSIONS
    ),
    POI(
        apiValue = "things to do",
        iconRes = R.drawable.ic_cat_poi,
        titleKey = LanguageConst.ADD_PLAN_CAT_POI
    ),
    FOOD(
        apiValue = "food, tasting tour",
        iconRes = R.drawable.ic_cat_food_drinks,
        titleKey = LanguageConst.ADD_PLAN_CAT_FOOD
    ),
    SHOWS(
        apiValue = "show",
        iconRes = R.drawable.ic_cat_shows,
        titleKey = LanguageConst.ADD_PLAN_CAT_SHOWS
    ),
    TRANSPORT(
        apiValue = "transfer service, transportation",
        iconRes = R.drawable.ic_cat_transfers,
        titleKey = LanguageConst.ADD_PLAN_CAT_TRANSPORT
    )
}

/**
 * Manual Mode Categories (3 categories)
 * iOS Reference: Manual category buttons in SelectDay screen
 */
enum class ManualCategory(
    val iconRes: Int,
    val titleKey: String
) {
    ACTIVITIES(
        iconRes = R.drawable.ic_activities,
        titleKey = LanguageConst.ADD_PLAN_CAT_MANUAL_ACTIVITIES
    ),
    PLACES_OF_INTEREST(
        iconRes = R.drawable.ic_see_do,
        titleKey = LanguageConst.ADD_PLAN_CAT_MANUAL_PLACES
    ),
    EAT_AND_DRINK(
        iconRes = R.drawable.ic_eat_drink,
        titleKey = LanguageConst.ADD_PLAN_CAT_MANUAL_EAT_DRINK
    )
}

/**
 * Starting Point Type
 */
enum class StartingPointType {
    NEAR_ME,       // User's current location
    CITY_CENTER,   // City center
    ACCOMMODATION, // Accommodation location
    CUSTOM         // Custom location
}

/**
 * Legacy AddPlan Category (for backward compatibility)
 */
data class AddPlanCategory(
    val id: String,
    val nameKey: String,
    val iconResId: Int,
    var isSelected: Boolean = false
) : Serializable

/**
 * Segment Type Constants
 */
object SegmentTypes {
    const val ITINERARY = "itinerary"
    const val BOOKED_ACTIVITY = "booked_activity"
    const val RESERVED_ACTIVITY = "reserved_activity"
    const val MANUAL_POI = "manual_poi"
    const val GENERATED = "generated"
}

/**
 * Category ID Constants (Legacy)
 */
object CategoryIds {
    const val ACTIVITIES = "activities"
    const val SEE_DO = "see_do"
    const val EAT_DRINK = "eat_drink"
    const val TOURS = "guided_tours"
    const val TICKETS = "tickets"
    const val FOOD = "food"
    const val SPORTS = "sports"
    const val ENTERTAINMENT = "entertainment"
}
