package com.tripian.trpcore.ui.timeline.activity

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.SortOption
import com.tripian.trpcore.domain.usecase.timeline.CreateReservedActivitySegmentUseCase
import com.tripian.trpcore.domain.usecase.timeline.SearchToursUseCase
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.appLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ACActivityListingVM
 * ViewModel for Activity/Tour listing screen
 * iOS Reference: ActivityListingVC
 */
class ACActivityListingVM @Inject constructor(
    private val searchToursUseCase: SearchToursUseCase,
    private val createReservedActivitySegmentUseCase: CreateReservedActivitySegmentUseCase
) : BaseViewModel() {

    // =====================
    // LIVEDATA
    // =====================

    private val _activities = MutableLiveData<List<TourProduct>>()
    val activities: LiveData<List<TourProduct>> = _activities

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSearching = MutableLiveData<Boolean>()
    val isSearching: LiveData<Boolean> = _isSearching

    private val _activityCount = MutableLiveData<Int>()
    val activityCount: LiveData<Int> = _activityCount

    // Multiple category selection support
    private val _selectedCategoryIndices = MutableLiveData(setOf(0))
    val selectedCategoryIndices: LiveData<Set<Int>> = _selectedCategoryIndices

    private val _hasMorePages = MutableLiveData<Boolean>()

    private val _showTimeSelection = MutableLiveData<TourProduct?>()
    val showTimeSelection: LiveData<TourProduct?> = _showTimeSelection

    private val _segmentCreated = MutableLiveData<Boolean>()
    val segmentCreated: LiveData<Boolean> = _segmentCreated

    private val _isCreatingSegment = MutableLiveData<Boolean>()
    val isCreatingSegment: LiveData<Boolean> = _isCreatingSegment

    // Filter state
    private val _currentFilter = MutableLiveData(ActivityFilterData.default())
    val currentFilter: LiveData<ActivityFilterData> = _currentFilter

    // Sort state
    private val _currentSort = MutableLiveData(SortOption.SCORE)

    // Scroll to top event
    private val _scrollToTop = MutableLiveData<Boolean>()
    val scrollToTop: LiveData<Boolean> = _scrollToTop

    // =====================
    // STATE
    // =====================

    private var planData: AddPlanData? = null
    private var tripHash: String = ""
    private var cityId: Int = 0
    private var cityLat: Double = 0.0
    private var cityLng: Double = 0.0
    private var selectedDateString: String? = null  // Format: "yyyy-MM-dd"
    private var currentSearchQuery: String = ""
    private var currentOffset: Int = 0
    private val pageLimit: Int = 10
    private var allActivities: MutableList<TourProduct> = mutableListOf()

    private var searchHandler: Handler? = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val searchDebounceMs = 650L

    // =====================
    // INITIALIZATION
    // =====================

    fun initialize(planData: AddPlanData, tripHash: String) {
        this.planData = planData
        this.tripHash = tripHash
        this.cityId = planData.selectedCity?.id ?: 0

        // Extract city coordinate - required for tour search
        val cityCoordinate = planData.selectedCity?.coordinate
        this.cityLat = cityCoordinate?.lat ?: 0.0
        this.cityLng = cityCoordinate?.lng ?: 0.0

        // Extract selected date and format as "yyyy-MM-dd"
        planData.selectedDay?.let { date ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            this.selectedDateString = dateFormat.format(date)
        }

        // Initial load
        loadActivities()
    }

    // =====================
    // SEARCH
    // =====================

    fun updateSearchText(query: String) {
        currentSearchQuery = query

        // Cancel previous search
        searchRunnable?.let { searchHandler?.removeCallbacks(it) }

        // Debounce search
        searchRunnable = Runnable {
            resetAndSearch()
        }
        searchHandler?.postDelayed(searchRunnable!!, searchDebounceMs)
    }

    private fun resetAndSearch() {
        currentOffset = 0
        allActivities.clear()
        _scrollToTop.value = true
        loadActivities()
    }

    // =====================
    // CATEGORY SELECTION
    // =====================

    /**
     * Handle category selection change from adapter
     * @param selectedIndices Set of selected category indices
     */
    fun onCategorySelectionChanged(selectedIndices: Set<Int>) {
        _selectedCategoryIndices.value = selectedIndices
        resetAndSearch()
    }

    // =====================
    // FILTER
    // =====================

    /**
     * Apply new filter and reload activities
     * @param filter New filter data
     */
    fun applyFilter(filter: ActivityFilterData) {
        _currentFilter.value = filter
        resetAndSearch()
    }

    /**
     * Get current filter data
     */
    fun getCurrentFilter(): ActivityFilterData = _currentFilter.value ?: ActivityFilterData.default()

    /**
     * Check if any filter is currently active
     */
    fun hasActiveFilters(): Boolean = _currentFilter.value?.hasActiveFilters() == true

    /**
     * Get number of active filters
     */
    fun getActiveFilterCount(): Int = _currentFilter.value?.activeFilterCount() ?: 0

    // =====================
    // SORT
    // =====================

    /**
     * Apply new sort option and reload activities
     * @param sort New sort option
     */
    fun applySort(sort: SortOption) {
        _currentSort.value = sort
        resetAndSearch()
    }

    /**
     * Get current sort option
     */
    fun getCurrentSort(): SortOption = _currentSort.value ?: SortOption.DEFAULT

    /**
     * Get category list with icons for adapter
     * Uses language keys from LanguageConst
     */
    fun getCategories(): List<ActivityCategoryItem> {
        return listOf(
            ActivityCategoryItem(
                id = "all",
                languageKey = LanguageConst.ADD_PLAN_CAT_ALL,
                iconRes = R.drawable.ic_all_categories,
                keywords = null
            ),
            ActivityCategoryItem(
                id = "guided_tours",
                languageKey = LanguageConst.ADD_PLAN_CAT_GUIDED_TOURS,
                iconRes = R.drawable.ic_activities,
                keywords = "guided tours, free tours"
            ),
            ActivityCategoryItem(
                id = "tickets",
                languageKey = LanguageConst.ADD_PLAN_CAT_TICKETS,
                iconRes = R.drawable.ic_cat_tickets,
                keywords = "tickets"
            ),
            ActivityCategoryItem(
                id = "excursions",
                languageKey = LanguageConst.ADD_PLAN_CAT_EXCURSIONS,
                iconRes = R.drawable.ic_cat_excursions,
                keywords = "day trip"
            ),
            ActivityCategoryItem(
                id = "poi",
                languageKey = LanguageConst.ADD_PLAN_CAT_POI,
                iconRes = R.drawable.ic_cat_poi,
                keywords = "things to do"
            ),
            ActivityCategoryItem(
                id = "food",
                languageKey = LanguageConst.ADD_PLAN_CAT_FOOD,
                iconRes = R.drawable.ic_cat_food_drinks,
                keywords = "food, tasting tour"
            ),
            ActivityCategoryItem(
                id = "shows",
                languageKey = LanguageConst.ADD_PLAN_CAT_SHOWS,
                iconRes = R.drawable.ic_cat_shows,
                keywords = "show"
            ),
            ActivityCategoryItem(
                id = "transport",
                languageKey = LanguageConst.ADD_PLAN_CAT_TRANSPORT,
                iconRes = R.drawable.ic_cat_transfers,
                keywords = "transfer service, transportation"
            )
        )
    }

    // =====================
    // LOAD ACTIVITIES
    // =====================

    fun loadActivities() {
        // Validate required parameter: cityId
        if (cityId <= 0) return

        if (currentOffset == 0) {
            _isLoading.value = true
        }
        _isSearching.value = currentSearchQuery.isNotEmpty()

        // Build keywords from search query and selected categories
        val combinedKeywords = buildCombinedKeywords()

        // Get filter values
        val filter = _currentFilter.value ?: ActivityFilterData.default()
        val minPrice = if (filter.minPrice > ActivityFilterData.DEFAULT_MIN_PRICE) {
            filter.minPrice.toInt()
        } else null
        val maxPrice = if (filter.maxPrice < ActivityFilterData.DEFAULT_MAX_PRICE) {
            filter.maxPrice.toInt()
        } else null
        val minDuration = if (filter.minDuration > ActivityFilterData.DEFAULT_MIN_DURATION) {
            filter.minDuration.toInt()
        } else null
        val maxDuration = if (filter.maxDuration < ActivityFilterData.DEFAULT_MAX_DURATION) {
            filter.maxDuration.toInt()
        } else null

        // Get sort values
        val sort = _currentSort.value ?: SortOption.DEFAULT

        searchToursUseCase.on(
            params = SearchToursUseCase.Params(
                cityId = cityId,
                lat = cityLat,
                lng = cityLng,
                keywords = combinedKeywords,
                tagIds = null, // Not using tagIds - only keywords
                providerId = 15, // Always use providerId 15 for tour-api
                date = selectedDateString, // Selected date from AddPlan flow
                minPrice = minPrice,
                maxPrice = maxPrice,
                minDuration = minDuration,
                maxDuration = maxDuration,
                sortingBy = sort.sortingBy,
                sortingType = sort.sortingType,
                offset = currentOffset,
                limit = pageLimit
            ),
            success = { response ->
                _isLoading.value = false
                _isSearching.value = false

                val newProducts = response.data?.products ?: emptyList()
                val total = response.data?.total ?: 0

                if (currentOffset == 0) {
                    allActivities.clear()
                }
                allActivities.addAll(newProducts)

                _activities.value = allActivities.toList()
                _activityCount.value = total
                _hasMorePages.value = allActivities.size < total
            },
            error = { error ->
                _isLoading.value = false
                _isSearching.value = false
                showAlert(AlertType.ERROR, error.errorDesc ?: getLanguageForKey(LanguageConst.COMMON_ERROR))
                _activities.value = emptyList()
                _activityCount.value = 0
            }
        )
    }

    fun loadMoreActivities() {
        if (_hasMorePages.value == true && _isLoading.value != true) {
            currentOffset += pageLimit
            loadActivities()
        }
    }

    // =====================
    // TIME SELECTION
    // =====================

    fun onActivityAddClicked(activity: TourProduct) {
        _showTimeSelection.value = activity
    }

    fun clearTimeSelection() {
        _showTimeSelection.value = null
    }

    // =====================
    // CREATE SEGMENT
    // =====================

    fun createReservedActivitySegment(tour: TourProduct, selectedDate: Date, timeSlot: String) {
        _isCreatingSegment.value = true

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(selectedDate)

        createReservedActivitySegmentUseCase.on(
            params = CreateReservedActivitySegmentUseCase.Params(
                tripHash = tripHash,
                tour = tour,
                selectedDate = dateString,
                selectedTimeSlot = timeSlot,
                adults = planData?.travelers ?: 1,
                cityId = cityId
            ),
            success = { _ ->
                _isCreatingSegment.value = false
                _segmentCreated.value = true
            },
            error = { error ->
                _isCreatingSegment.value = false
                showAlert(AlertType.ERROR, error.errorDesc ?: getLanguageForKey(LanguageConst.COMMON_ERROR))
            }
        )
    }

    // =====================
    // HELPERS
    // =====================

    /**
     * Build combined keywords from user search query and selected categories
     * Multiple category keywords are combined with comma separator
     * @return Combined keywords string or null if no keywords
     */
    private fun buildCombinedKeywords(): String? {
        val indices = _selectedCategoryIndices.value ?: setOf(0)
        val categories = getCategories()

        // If "All" is selected or no selection, only use search query
        if (indices.contains(0) || indices.isEmpty()) {
            return if (currentSearchQuery.isNotBlank()) currentSearchQuery else null
        }

        // Combine keywords from selected categories
        val categoryKeywords = indices
            .mapNotNull { categories.getOrNull(it)?.keywords }
            .filter { it.isNotBlank() }
            .joinToString(", ")

        return when {
            currentSearchQuery.isNotBlank() && categoryKeywords.isNotBlank() ->
                "$currentSearchQuery, $categoryKeywords"
            currentSearchQuery.isNotBlank() -> currentSearchQuery
            categoryKeywords.isNotBlank() -> categoryKeywords
            else -> null
        }
    }

    fun getSelectedDate(): Date? = planData?.selectedDay

    fun getAvailableDays(): List<Date> = planData?.availableDays ?: emptyList()

    fun getSdkLanguage(): String = appLanguage

    /**
     * Get currency for filter display
     * Defaults to EUR
     */
    fun getCurrency(): String = "EUR" // TODO: Get from trip settings if available

    // =====================
    // CLEANUP
    // =====================

    override fun onDestroy() {
        searchRunnable?.let { searchHandler?.removeCallbacks(it) }
        searchHandler = null
        super.onDestroy()
    }
}
