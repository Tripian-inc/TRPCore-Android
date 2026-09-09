package com.tripian.trpcore.ui.timeline.poidetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.manager.POICategoryManager
import com.tripian.trpcore.repository.TourRepository
import com.tripian.trpcore.ui.timeline.poilisting.POIListingType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.OpeningHours
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ACPOIDetailVM
 * ViewModel for POI Detail screen
 * Handles section visibility, description expansion, and opening hours parsing
 */
class ACPOIDetailVM @Inject constructor(
    private val tourRepository: TourRepository
) : BaseViewModel() {

    private val _poi = MutableLiveData<Poi>()
    val poi: LiveData<Poi> = _poi

    private val _isDescriptionExpanded = MutableLiveData(false)
    val isDescriptionExpanded: LiveData<Boolean> = _isDescriptionExpanded

    private val _parsedOpeningHours = MutableLiveData<List<OpeningHourItem>>()
    val parsedOpeningHours: LiveData<List<OpeningHourItem>> = _parsedOpeningHours

    private val _products = MutableLiveData<List<TourProduct>>()
    val products: LiveData<List<TourProduct>> = _products

    private val _isLoadingProducts = MutableLiveData(false)
    val isLoadingProducts: LiveData<Boolean> = _isLoadingProducts

    private val _showActivitiesSection = MutableLiveData(false)
    val showActivitiesSection: LiveData<Boolean> = _showActivitiesSection

    private var tripStartDate: String? = null
    private var tripEndDate: String? = null
    private var loadedProducts: MutableList<TourProduct> = mutableListOf()
    private var productTotal: Int = 0
    private var productOffset: Int = 0
    private var isLoadingMoreProducts: Boolean = false

    /** More pages exist while the window fetched so far ends before the reported total. */
    private val hasMoreProducts: Boolean
        get() = productOffset + PRODUCT_PAGE_SIZE < productTotal

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

    /**
     * @param tripStartDate / [tripEndDate] "yyyy-MM-dd" — the window products are
     *   queried for. Without them the products section stays hidden, since an
     *   undated query would offer activities outside the trip.
     */
    fun initialize(poi: Poi, tripStartDate: String?, tripEndDate: String?) {
        _poi.value = poi
        this.tripStartDate = tripStartDate
        this.tripEndDate = tripEndDate

        processPoi(poi)
        loadProducts()
    }

    /**
     * First page of the POI's own products. The section is kept visible while the
     * request is in flight so it can host the skeleton.
     */
    private fun loadProducts() {
        val poi = _poi.value ?: return
        val cityId = poi.cityId ?: 0
        val startDate = tripStartDate
        if (poi.hasBookings != true || cityId <= 0 || startDate.isNullOrBlank()) {
            updateActivitiesSectionVisibility()
            return
        }

        loadedProducts = mutableListOf()
        productOffset = 0
        productTotal = 0
        _isLoadingProducts.value = true
        updateActivitiesSectionVisibility()

        viewModelScope.launch {
            runCatching { fetchProductPage(poi, cityId, startDate, offset = 0) }
                .onSuccess { page ->
                    loadedProducts = page.products.toMutableList()
                    productTotal = page.total
                    _products.value = loadedProducts.toList()
                }
            _isLoadingProducts.value = false
            updateActivitiesSectionVisibility()
        }
    }

    /** Next page; ignored while one is in flight or when the list is complete. */
    fun loadMoreProducts() {
        val poi = _poi.value ?: return
        val cityId = poi.cityId ?: 0
        val startDate = tripStartDate ?: return
        if (isLoadingMoreProducts || !hasMoreProducts || cityId <= 0) return

        isLoadingMoreProducts = true
        _isLoadingProducts.value = true

        viewModelScope.launch {
            val nextOffset = productOffset + PRODUCT_PAGE_SIZE
            runCatching { fetchProductPage(poi, cityId, startDate, nextOffset) }
                .onSuccess { page ->
                    productOffset = nextOffset
                    productTotal = page.total
                    loadedProducts.addAll(page.products)
                    _products.value = loadedProducts.toList()
                }
            isLoadingMoreProducts = false
            _isLoadingProducts.value = false
            updateActivitiesSectionVisibility()
        }
    }

    private suspend fun fetchProductPage(
        poi: Poi,
        cityId: Int,
        startDate: String,
        offset: Int
    ): ProductPage {
        val response = tourRepository.searchToursAsync(
            cityId = cityId,
            lat = poi.coordinate?.lat ?: 0.0,
            lng = poi.coordinate?.lng ?: 0.0,
            poiId = poi.id,
            providerId = TRPCore.provider.id,
            instantAvailability = null,
            date = startDate,
            to = tripEndDate ?: startDate,
            currency = TRPCore.core.appConfig.appCurrency,
            offset = offset,
            limit = PRODUCT_PAGE_SIZE
        )
        val data = response.data
        return ProductPage(
            products = data?.products.orEmpty(),
            total = data?.total ?: 0
        )
    }

    /**
     * Visible only for a POI the API marks as bookable, and only while it either
     * holds products or is still loading them.
     */
    private fun updateActivitiesSectionVisibility() {
        val hasBookings = _poi.value?.hasBookings == true
        val isLoading = _isLoadingProducts.value == true
        val hasProducts = _products.value?.isNotEmpty() == true
        _showActivitiesSection.value = hasBookings && (isLoading || hasProducts)
    }

    private fun processPoi(poi: Poi) {
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
    fun onProductClicked(product: TourProduct) {
        product.productId?.let { productId ->
            TRPCore.notifyActivityDetailRequested(productId)
        }
    }

    /**
     * Get city name from POI locations
     */
    fun getCityName(): String? {
        return _poi.value?.locations?.firstOrNull()?.name
    }

    private data class ProductPage(val products: List<TourProduct>, val total: Int)

    private companion object {
        const val PRODUCT_PAGE_SIZE = 10
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
