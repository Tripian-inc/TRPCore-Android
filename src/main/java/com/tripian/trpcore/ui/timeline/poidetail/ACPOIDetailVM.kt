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
import com.tripian.trpcore.util.OpeningHours
import javax.inject.Inject

/**
 * ACPOIDetailVM
 * ViewModel for POI Detail screen
 * Handles section visibility, description expansion, and opening hours parsing
 */
class ACPOIDetailVM @Inject constructor() : BaseViewModel() {

    private val _poi = MutableLiveData<Poi>()
    val poi: LiveData<Poi> = _poi

    private val _isDescriptionExpanded = MutableLiveData(false)
    val isDescriptionExpanded: LiveData<Boolean> = _isDescriptionExpanded

    private val _parsedOpeningHours = MutableLiveData<List<OpeningHourItem>>()
    val parsedOpeningHours: LiveData<List<OpeningHourItem>> = _parsedOpeningHours

    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

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

    private val _showCuisinesSection = MutableLiveData(false)
    val showCuisinesSection: LiveData<Boolean> = _showCuisinesSection

    private val _cuisinesList = MutableLiveData<List<String>>()
    val cuisinesList: LiveData<List<String>> = _cuisinesList

    /** Initialize ViewModel with POI data */
    fun initialize(poi: Poi) {
        _poi.value = poi

        processPoi(poi)
    }

    private fun processPoi(poi: Poi) {
        val allProducts = poi.bookings
            ?.filter { it.providerId == TRPCore.provider.id }
            ?.flatMap { it.products ?: emptyList() }
            ?: emptyList()
        _products.value = allProducts
        _showActivitiesSection.value = allProducts.isNotEmpty()

        val hasPhone = !poi.phone.isNullOrBlank()
        val isEatAndDrink = isEatAndDrinkCategory(poi)
        _showPhoneRow.value = hasPhone && isEatAndDrink

        val hasHours = !poi.hours.isNullOrBlank()
        if (hasHours) {
            val parsedHours = parseOpeningHours(poi.hours!!)
            _parsedOpeningHours.value = parsedHours
            _showOpeningHoursRow.value = parsedHours.isNotEmpty()
        } else {
            _showOpeningHoursRow.value = false
        }

        _showKeyDataSection.value = (_showPhoneRow.value == true) || (_showOpeningHoursRow.value == true)

        _showMeetingPointSection.value = poi.coordinate != null

        _showFeaturesSection.value = false

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

    /**
     * Check if POI belongs to Eat & Drink category
     * Phone is ONLY shown for Eat & Drink categories (IDs: 3, 4, 24)
     */
    private fun isEatAndDrinkCategory(poi: Poi): Boolean {
        val eatDrinkCategoryIds = POICategoryManager.getCategoryIds(POIListingType.EAT_AND_DRINK)
            ?: listOf(3, 4, 24)

        val poiCategoryIds = poi.category?.map { it.id } ?: emptyList()
        return poiCategoryIds.any { it in eatDrinkCategoryIds }
    }

    /**
     * Build the day-by-day display rows from the POI `hours` string; days with no
     * entry render as closed.
     */
    private fun parseOpeningHours(hoursString: String): List<OpeningHourItem> {
        val dayTexts = OpeningHours.dayTexts(hoursString)
        return OpeningHours.DAY_ORDER.map { day ->
            val hours = dayTexts[day]
            OpeningHourItem(
                dayName = getLocalizedDayName(day),
                hours = hours ?: getLanguageForKey(LanguageConst.CLOSED),
                isClosed = hours == null
            )
        }
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
