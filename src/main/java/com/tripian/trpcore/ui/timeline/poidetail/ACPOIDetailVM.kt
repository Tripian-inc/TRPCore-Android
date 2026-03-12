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

    // Cuisines section (for Cafe/Restaurant)
    private val _showCuisinesSection = MutableLiveData(false)
    val showCuisinesSection: LiveData<Boolean> = _showCuisinesSection

    private val _cuisinesList = MutableLiveData<List<String>>()
    val cuisinesList: LiveData<List<String>> = _cuisinesList

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
        // Activities Section - visible if has products from providerId 15 bookings
        val allProducts = poi.bookings
            ?.filter { it.providerId == 15 }
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

        // Features Section - DISABLED: Features görünümü kapatıldı
        // _showFeaturesSection.value = !poi.tags.isNullOrEmpty()
        _showFeaturesSection.value = false

        // Cuisines Section - DISABLED: Cuisines görünümü kapatıldı
        // val cuisines = parseCuisines(poi.cuisines)
        // _cuisinesList.value = cuisines
        // _showCuisinesSection.value = isEatAndDrink && cuisines.isNotEmpty()
        _showCuisinesSection.value = false
    }

    /**
     * Parse cuisines string to list
     * Handles comma-separated format: "Italian, Pizza, Pasta"
     */
    private fun parseCuisines(cuisinesString: String?): List<String> {
        if (cuisinesString.isNullOrBlank()) return emptyList()
        return cuisinesString.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
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
     * Multi-language day name mappings to English abbreviations.
     * Supports: English, Spanish, German, French, Turkish, Italian, Portuguese
     */
    private val dayNameMappings = mapOf(
        // English
        "Mon" to "Mon", "Tue" to "Tue", "Wed" to "Wed", "Thu" to "Thu", "Fri" to "Fri", "Sat" to "Sat", "Sun" to "Sun",
        "Monday" to "Mon", "Tuesday" to "Tue", "Wednesday" to "Wed", "Thursday" to "Thu", "Friday" to "Fri", "Saturday" to "Sat", "Sunday" to "Sun",
        // Spanish
        "Lun" to "Mon", "Mar" to "Tue", "Mié" to "Wed", "Mie" to "Wed", "Jue" to "Thu", "Vie" to "Fri", "Sáb" to "Sat", "Sab" to "Sat", "Dom" to "Sun",
        "Lunes" to "Mon", "Martes" to "Tue", "Miércoles" to "Wed", "Miercoles" to "Wed", "Jueves" to "Thu", "Viernes" to "Fri", "Sábado" to "Sat", "Sabado" to "Sat", "Domingo" to "Sun",
        // German
        "Mo" to "Mon", "Di" to "Tue", "Mi" to "Wed", "Do" to "Thu", "Fr" to "Fri", "Sa" to "Sat", "So" to "Sun",
        "Montag" to "Mon", "Dienstag" to "Tue", "Mittwoch" to "Wed", "Donnerstag" to "Thu", "Freitag" to "Fri", "Samstag" to "Sat", "Sonntag" to "Sun",
        // French
        "Lun" to "Mon", "Mar" to "Tue", "Mer" to "Wed", "Jeu" to "Thu", "Ven" to "Fri", "Sam" to "Sat", "Dim" to "Sun",
        "Lundi" to "Mon", "Mardi" to "Tue", "Mercredi" to "Wed", "Jeudi" to "Thu", "Vendredi" to "Fri", "Samedi" to "Sat", "Dimanche" to "Sun",
        // Turkish
        "Pzt" to "Mon", "Sal" to "Tue", "Çar" to "Wed", "Car" to "Wed", "Per" to "Thu", "Cum" to "Fri", "Cmt" to "Sat", "Paz" to "Sun",
        "Pazartesi" to "Mon", "Salı" to "Tue", "Sali" to "Tue", "Çarşamba" to "Wed", "Carsamba" to "Wed", "Perşembe" to "Thu", "Persembe" to "Thu", "Cuma" to "Fri", "Cumartesi" to "Sat", "Pazar" to "Sun",
        // Italian
        "Lun" to "Mon", "Mar" to "Tue", "Mer" to "Wed", "Gio" to "Thu", "Ven" to "Fri", "Sab" to "Sat", "Dom" to "Sun",
        "Lunedì" to "Mon", "Lunedi" to "Mon", "Martedì" to "Tue", "Martedi" to "Tue", "Mercoledì" to "Wed", "Mercoledi" to "Wed", "Giovedì" to "Thu", "Giovedi" to "Thu", "Venerdì" to "Fri", "Venerdi" to "Fri", "Sabato" to "Sat", "Domenica" to "Sun",
        // Portuguese
        "Seg" to "Mon", "Ter" to "Tue", "Qua" to "Wed", "Qui" to "Thu", "Sex" to "Fri", "Sáb" to "Sat", "Sab" to "Sat", "Dom" to "Sun",
        "Segunda" to "Mon", "Terça" to "Tue", "Terca" to "Tue", "Quarta" to "Wed", "Quinta" to "Thu", "Sexta" to "Fri", "Sábado" to "Sat", "Sabado" to "Sat", "Domingo" to "Sun"
    )

    /**
     * Get all recognized day names (for finding day parts in string)
     */
    private val allDayNames: List<String> by lazy {
        dayNameMappings.keys.sortedByDescending { it.length } // Longer names first to match "Monday" before "Mon"
    }

    /**
     * Parse opening hours string to list of OpeningHourItem
     * Input format: "Sun, Sat: 9:00 AM - 1:00 AM | Mon-Fri: 8:30 AM - 1:00 AM"
     * Also supports localized formats: "Lun, Mar: 9:00 - 17:00 | Mié-Vie: 8:30 - 18:00"
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
            if (daysPart.isEmpty()) continue // Skip if no day names found

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
     * e.g., "Lun, Mar: 9:00 - 17:00" -> "Lun, Mar"
     * Supports multiple languages
     */
    private fun findDaysPart(group: String): String {
        var lastDayEnd = 0

        for (i in group.indices) {
            for (dayName in allDayNames) {
                if (group.startsWith(dayName, i, ignoreCase = true)) {
                    val endPos = i + dayName.length
                    if (endPos > lastDayEnd) {
                        lastDayEnd = endPos
                    }
                }
            }
        }

        return group.substring(0, lastDayEnd)
    }

    /**
     * Normalize a localized day name to English abbreviation
     * e.g., "Lun" -> "Mon", "Montag" -> "Mon"
     */
    private fun normalizeDayName(localizedDay: String): String? {
        val trimmed = localizedDay.trim()
        // Check exact match first (case-insensitive)
        for ((key, value) in dayNameMappings) {
            if (key.equals(trimmed, ignoreCase = true)) {
                return value
            }
        }
        return null
    }

    /**
     * Parse days string to list of day abbreviations (English)
     * Handles formats like: "Mon-Fri", "Sun, Sat", "Mon, Wed, Fri"
     * Also supports localized: "Lun-Vie", "Sáb, Dom"
     */
    private fun parseDays(daysString: String): List<String> {
        val result = mutableListOf<String>()
        val dayOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        // Split by comma first
        val parts = daysString.split(",").map { it.trim() }

        for (part in parts) {
            if (part.contains("-")) {
                // Range like "Mon-Fri" or "Lun-Vie"
                val rangeParts = part.split("-").map { it.trim() }
                if (rangeParts.size == 2) {
                    val startDay = normalizeDayName(rangeParts[0])
                    val endDay = normalizeDayName(rangeParts[1])

                    if (startDay != null && endDay != null) {
                        val startIdx = dayOrder.indexOf(startDay)
                        val endIdx = dayOrder.indexOf(endDay)
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
                }
            } else {
                // Single day
                val normalizedDay = normalizeDayName(part)
                if (normalizedDay != null && dayOrder.contains(normalizedDay)) {
                    result.add(normalizedDay)
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
