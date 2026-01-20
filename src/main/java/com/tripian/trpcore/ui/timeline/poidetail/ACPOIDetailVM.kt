package com.tripian.trpcore.ui.timeline.poidetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.pois.model.Product
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.manager.POICategoryManager
import com.tripian.trpcore.ui.timeline.poilisting.POIListingType
import com.tripian.trpcore.util.LanguageConst
import javax.inject.Inject

/**
 * ACPOIDetailVM
 * ViewModel for POI Detail screen
 * Handles section visibility, description expansion, and opening hours parsing
 */
class ACPOIDetailVM @Inject constructor() : BaseViewModel() {

    // =====================
    // LIVEDATA
    // =====================

    private val _poi = MutableLiveData<Poi>()
    val poi: LiveData<Poi> = _poi

    private val _isDescriptionExpanded = MutableLiveData(false)
    val isDescriptionExpanded: LiveData<Boolean> = _isDescriptionExpanded

    private val _parsedOpeningHours = MutableLiveData<List<OpeningHourItem>>()
    val parsedOpeningHours: LiveData<List<OpeningHourItem>> = _parsedOpeningHours

    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    // Section visibility
    private val _showActivitiesSection = MutableLiveData(false)
    val showActivitiesSection: LiveData<Boolean> = _showActivitiesSection

    private val _showKeyDataSection = MutableLiveData(false)
    val showKeyDataSection: LiveData<Boolean> = _showKeyDataSection

    private val _showMeetingPointSection = MutableLiveData(false)
    val showMeetingPointSection: LiveData<Boolean> = _showMeetingPointSection

    private val _showFeaturesSection = MutableLiveData(false)
    val showFeaturesSection: LiveData<Boolean> = _showFeaturesSection

    private val _showPhoneRow = MutableLiveData(false)
    val showPhoneRow: LiveData<Boolean> = _showPhoneRow

    private val _showOpeningHoursRow = MutableLiveData(false)
    val showOpeningHoursRow: LiveData<Boolean> = _showOpeningHoursRow

    // =====================
    // INITIALIZATION
    // =====================

    /**
     * Initialize ViewModel with POI data
     * @param poi Poi object from intent
     */
    fun initialize(poi: Poi) {
        _poi.value = poi

        // Parse data and determine section visibility
        processPoi(poi)
    }

    private fun processPoi(poi: Poi) {
        // Activities Section - visible if has products
        val allProducts = poi.bookings
            ?.flatMap { it.products ?: emptyList() }
            ?: emptyList()
        _products.value = allProducts
        _showActivitiesSection.value = allProducts.isNotEmpty()

        // Phone visibility - only for Eat & Drink categories
        val hasPhone = !poi.phone.isNullOrBlank()
        val isEatAndDrink = isEatAndDrinkCategory(poi)
        _showPhoneRow.value = hasPhone && isEatAndDrink

        // Opening hours
        val hasHours = !poi.hours.isNullOrBlank()
        if (hasHours) {
            val parsedHours = parseOpeningHours(poi.hours!!)
            _parsedOpeningHours.value = parsedHours
            _showOpeningHoursRow.value = parsedHours.isNotEmpty()
        } else {
            _showOpeningHoursRow.value = false
        }

        // Key Data Section - visible if phone OR opening hours are shown
        _showKeyDataSection.value = (_showPhoneRow.value == true) || (_showOpeningHoursRow.value == true)

        // Meeting Point Section - visible if has coordinate
        _showMeetingPointSection.value = poi.coordinate != null

        // Features Section - visible if has tags
        _showFeaturesSection.value = !poi.tags.isNullOrEmpty()
    }

    // =====================
    // PHONE VISIBILITY
    // =====================

    /**
     * Check if POI belongs to Eat & Drink category
     * Phone is ONLY shown for Eat & Drink categories (IDs: 3, 4, 24)
     */
    private fun isEatAndDrinkCategory(poi: Poi): Boolean {
        val eatDrinkCategoryIds = POICategoryManager.getCategoryIds(POIListingType.EAT_AND_DRINK)
            ?: listOf(3, 4, 24) // Fallback

        val poiCategoryIds = poi.category?.map { it.id } ?: emptyList()
        return poiCategoryIds.any { it in eatDrinkCategoryIds }
    }

    // =====================
    // OPENING HOURS PARSER
    // =====================

    /**
     * Parse opening hours string to list of OpeningHourItem
     * Input format: "Sun, Sat: 9:00 AM - 1:00 AM | Mon-Fri: 8:30 AM - 1:00 AM"
     * Output: List of day-based entries with 24h format
     */
    private fun parseOpeningHours(hoursString: String): List<OpeningHourItem> {
        val result = mutableListOf<OpeningHourItem>()
        val dayOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayHoursMap = mutableMapOf<String, String>()

        // Split by | to get each time range group
        val groups = hoursString.split("|").map { it.trim() }

        for (group in groups) {
            // Split by : to separate days from time
            val colonIndex = group.lastIndexOf(":")
            if (colonIndex < 0) continue

            // Find the first colon that separates days from time
            // Days part might contain commas and hyphens
            val daysPart = findDaysPart(group)
            val timePart = group.substring(daysPart.length).trim().removePrefix(":").trim()

            val days = parseDays(daysPart)
            val convertedTime = convertTo24HourFormat(timePart)

            for (day in days) {
                dayHoursMap[day] = convertedTime
            }
        }

        // Build result in day order
        for (day in dayOrder) {
            val localizedDay = getLocalizedDayName(day)
            val hours = dayHoursMap[day]
            if (hours != null) {
                result.add(OpeningHourItem(localizedDay, hours, false))
            } else {
                // Day is closed
                result.add(OpeningHourItem(localizedDay, getLanguageForKey(LanguageConst.CLOSED), true))
            }
        }

        return result
    }

    /**
     * Find the days part of the group string
     * e.g., "Sun, Sat: 9:00 AM - 1:00 AM" -> "Sun, Sat"
     */
    private fun findDaysPart(group: String): String {
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        var lastDayEnd = 0

        for (i in group.indices) {
            for (dayName in dayNames) {
                if (group.startsWith(dayName, i)) {
                    lastDayEnd = i + dayName.length
                }
            }
        }

        return group.substring(0, lastDayEnd)
    }

    /**
     * Parse days string to list of day abbreviations
     * Handles formats like: "Mon-Fri", "Sun, Sat", "Mon, Wed, Fri"
     */
    private fun parseDays(daysString: String): List<String> {
        val result = mutableListOf<String>()
        val dayOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        // Split by comma first
        val parts = daysString.split(",").map { it.trim() }

        for (part in parts) {
            if (part.contains("-")) {
                // Range like "Mon-Fri"
                val rangeParts = part.split("-").map { it.trim() }
                if (rangeParts.size == 2) {
                    val startIdx = dayOrder.indexOf(rangeParts[0])
                    val endIdx = dayOrder.indexOf(rangeParts[1])
                    if (startIdx >= 0 && endIdx >= 0) {
                        if (startIdx <= endIdx) {
                            for (i in startIdx..endIdx) {
                                result.add(dayOrder[i])
                            }
                        } else {
                            // Wrap around (e.g., Fri-Mon)
                            for (i in startIdx until dayOrder.size) {
                                result.add(dayOrder[i])
                            }
                            for (i in 0..endIdx) {
                                result.add(dayOrder[i])
                            }
                        }
                    }
                }
            } else {
                // Single day
                if (dayOrder.contains(part)) {
                    result.add(part)
                }
            }
        }

        return result
    }

    /**
     * Convert 12-hour format to 24-hour format
     * e.g., "9:00 AM - 1:00 AM" -> "09:00 - 01:00"
     */
    private fun convertTo24HourFormat(timeString: String): String {
        val parts = timeString.split("-").map { it.trim() }
        if (parts.size != 2) return timeString

        val startTime = convert12To24(parts[0])
        val endTime = convert12To24(parts[1])

        return "$startTime - $endTime"
    }

    /**
     * Convert single time from 12h to 24h format
     * e.g., "9:00 AM" -> "09:00"
     */
    private fun convert12To24(time: String): String {
        val trimmed = time.trim().uppercase()
        val isPM = trimmed.contains("PM")
        val isAM = trimmed.contains("AM")

        val timeOnly = trimmed.replace("AM", "").replace("PM", "").trim()
        val timeParts = timeOnly.split(":").map { it.trim() }

        if (timeParts.size != 2) return time

        var hour = timeParts[0].toIntOrNull() ?: return time
        val minute = timeParts[1].toIntOrNull() ?: return time

        if (isPM && hour != 12) {
            hour += 12
        } else if (isAM && hour == 12) {
            hour = 0
        }

        return String.format("%02d:%02d", hour, minute)
    }

    /**
     * Get localized day name from English abbreviation
     */
    private fun getLocalizedDayName(day: String): String {
        return when (day) {
            "Mon" -> getLanguageForKey(LanguageConst.MONDAY)
            "Tue" -> getLanguageForKey(LanguageConst.TUESDAY)
            "Wed" -> getLanguageForKey(LanguageConst.WEDNESDAY)
            "Thu" -> getLanguageForKey(LanguageConst.THURSDAY)
            "Fri" -> getLanguageForKey(LanguageConst.FRIDAY)
            "Sat" -> getLanguageForKey(LanguageConst.SATURDAY)
            "Sun" -> getLanguageForKey(LanguageConst.SUNDAY)
            else -> day
        }
    }

    // =====================
    // ACTIONS
    // =====================

    /**
     * Toggle description expanded state
     */
    fun toggleDescription() {
        _isDescriptionExpanded.value = !(_isDescriptionExpanded.value ?: false)
    }

    /**
     * Handle product card click
     * Notifies host app via TRPCore listener
     */
    fun onProductClicked(product: Product) {
        product.id?.let { productId ->
            TRPCore.notifyActivityDetailRequested(productId)
        }
    }

    /**
     * Get city name from POI locations
     */
    fun getCityName(): String? {
        return _poi.value?.locations?.firstOrNull()?.name
    }
}

/**
 * Data class for parsed opening hour entry
 */
data class OpeningHourItem(
    val dayName: String,
    val hours: String,
    val isClosed: Boolean = false
)
