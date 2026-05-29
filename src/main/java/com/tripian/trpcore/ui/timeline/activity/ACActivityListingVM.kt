package com.tripian.trpcore.ui.timeline.activity

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.tour.model.TourFacet
import com.tripian.one.api.tour.model.TourFacetCategory
import com.tripian.one.api.tour.model.TourFacetDurationRange
import com.tripian.one.api.tour.model.TourFacetPriceRange
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.SortOption
import com.tripian.trpcore.domain.usecase.timeline.CreateReservedActivitySegmentUseCase
import com.tripian.trpcore.domain.usecase.timeline.FetchTimelineUseCase
import com.tripian.trpcore.domain.usecase.timeline.SearchToursUseCase
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.TourCategoryIconMapper
import com.tripian.trpcore.util.extensions.appLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 *  * ACActivityListingVM
 *  * ViewModel for Activity/Tour listing screen
 * iOS Reference: ActivityListingVC
 */
class ACActivityListingVM @Inject constructor(
    private val searchToursUseCase: SearchToursUseCase,
    private val createReservedActivitySegmentUseCase: CreateReservedActivitySegmentUseCase,
    private val fetchTimelineUseCase: FetchTimelineUseCase
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
    private val _isLoadingMore = MutableLiveData<Boolean>(false)

    private val _showTimeSelection = MutableLiveData<TourProduct?>()
    val showTimeSelection: LiveData<TourProduct?> = _showTimeSelection

    /**
     * Emitted after the reserved-activity segment was created AND the timeline was
     * re-fetched successfully. Carries the tour title and the selected date so the
     * Activity can format and show the success toast. Activity sets it back to null
     * after consuming.
     */
    data class AddedToItineraryResult(val activityName: String, val selectedDate: Date)

    private val _addedToItinerarySuccess = MutableLiveData<AddedToItineraryResult?>()
    val addedToItinerarySuccess: LiveData<AddedToItineraryResult?> = _addedToItinerarySuccess

    fun clearAddedToItinerarySuccess() {
        _addedToItinerarySuccess.value = null
    }

    // Filter state
    private val _currentFilter = MutableLiveData(ActivityFilterData.default())
    val currentFilter: LiveData<ActivityFilterData> = _currentFilter

    // Sort state
    private val _currentSort = MutableLiveData(SortOption.POPULARITY)

    // Scroll to top event
    private val _scrollToTop = MutableLiveData<Boolean>()
    val scrollToTop: LiveData<Boolean> = _scrollToTop

    // Facet-driven category strip — populated from the search response. The Activity
    // Listing UI prefers facet categories over the hard-coded fallback when at least
    // one facet category is present. Multi-select; chip "All" (index 0) clears.
    private val _facetCategories = MutableLiveData<List<TourFacetCategory>>(emptyList())
    val facetCategories: LiveData<List<TourFacetCategory>> = _facetCategories

    // Bounds for the price / duration filter sliders. When null, the filter bottom
    // sheet falls back to its own DEFAULT_* bounds.
    private val _priceRangeFacet = MutableLiveData<TourFacetPriceRange?>(null)
    val priceRangeFacet: LiveData<TourFacetPriceRange?> = _priceRangeFacet

    private val _durationRangeFacet = MutableLiveData<TourFacetDurationRange?>(null)
    val durationRangeFacet: LiveData<TourFacetDurationRange?> = _durationRangeFacet

    // =====================
    // STATE
    // =====================

    private var planData: AddPlanData? = null
    private var tripHash: String = ""
    private var cityId: Int = 0
    private var selectedDayIndex: Int = 0
    private var cityLat: Double = 0.0
    private var cityLng: Double = 0.0
    private var selectedDateString: String? = null  // Format: "yyyy-MM-dd"
    private var currentSearchQuery: String = ""
    private var currentOffset: Int = 0
    private val pageLimit: Int = 10
    private var allActivities: MutableList<TourProduct> = mutableListOf()
    // API-reported total (used for the count label when no client-side filter is active).
    private var apiTotal: Int = 0

    // =====================
    // INITIALIZATION
    // =====================

    fun initialize(planData: AddPlanData, tripHash: String) {
        this.planData = planData
        this.tripHash = tripHash
        this.cityId = planData.selectedCity?.id ?: 0
        this.selectedDayIndex = planData.selectedDayIndex

        // Extract city coordinate - required for tour search
        val cityCoordinate = planData.selectedCity?.coordinate
        this.cityLat = cityCoordinate?.lat ?: 0.0
        this.cityLng = cityCoordinate?.lng ?: 0.0

        // Extract selected date and format as "yyyy-MM-dd"
        planData.selectedDay?.let { date ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            this.selectedDateString = dateFormat.format(date)
        }

        // Initial load
        loadActivities()
    }

    // =====================
    // SEARCH
    // =====================

    /**
     * Search is now purely client-side: filters the already-loaded [allActivities]
     * by title. No API call is made — keystroke feedback is instant and pagination
     * (which is API-driven) is paused while a query is active (see [loadMoreActivities]).
     */
    fun updateSearchText(query: String) {
        currentSearchQuery = query
        applyClientSideSearchFilter()
    }

    /**
     * Re-emits [allActivities] through the title filter into [_activities] and
     * updates the count label. When the query is blank the API-reported total is
     * shown; while filtering, the count reflects the visible (filtered) size.
     */
    private fun applyClientSideSearchFilter() {
        val q = currentSearchQuery.trim()
        val filtered = if (q.isBlank()) {
            allActivities.toList()
        } else {
            allActivities.filter { it.title?.contains(q, ignoreCase = true) == true }
        }
        _activities.value = filtered
        _activityCount.value = if (q.isBlank()) apiTotal else filtered.size
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
        // Category-triggered reloads keep the bottom-sheet variant (so the
        // list stays visible behind the loader) but show the same "Getting
        // activities" copy as the initial load instead of running text-less.
        showBottomSheetLoader(LanguageConst.LOADING_TEXT_GETTING_ACTIVITIES, "")
        suppressNextIsLoadingLoader = true
        resetAndSearch()
    }

    /**
     * When `true`, the next `_isLoading = true` transition will NOT trigger the
     * default full-screen loader from the Activity — the VM has already shown
     * a specific loader (e.g. bottom-sheet) and the Activity should only update
     * non-loader state. Cleared automatically when consumed by the Activity.
     */
    private var suppressNextIsLoadingLoader: Boolean = false

    fun consumeLoaderSuppression(): Boolean {
        val v = suppressNextIsLoadingLoader
        suppressNextIsLoadingLoader = false
        return v
    }

    /**
     * When `true`, the next `_isLoading = true` transition should be rendered
     * as an inline shimmer skeleton (filter/sort reload) instead of a Lottie
     * loader. Cleared automatically when consumed by the Activity.
     */
    private var useSkeletonForNextLoad: Boolean = false

    fun consumeSkeletonRequest(): Boolean {
        val v = useSkeletonForNextLoad
        useSkeletonForNextLoad = false
        return v
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
        useSkeletonForNextLoad = true
        suppressNextIsLoadingLoader = true
        resetAndSearch()
    }

    /**
     * Get current filter data
     */
    fun getCurrentFilter(): ActivityFilterData =
        _currentFilter.value ?: ActivityFilterData.default()

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
        useSkeletonForNextLoad = true
        suppressNextIsLoadingLoader = true
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
                iconRes = R.drawable.trp_ic_all_categories,
                keywords = null
            ),
            ActivityCategoryItem(
                id = "guided_tours",
                languageKey = LanguageConst.ADD_PLAN_CAT_GUIDED_TOURS,
                iconRes = R.drawable.trp_ic_cat_activities,
                keywords = "guided tours, free tours"
            ),
            ActivityCategoryItem(
                id = "tickets",
                languageKey = LanguageConst.ADD_PLAN_CAT_TICKETS,
                iconRes = R.drawable.trp_ic_cat_tickets,
                keywords = "tickets"
            ),
            ActivityCategoryItem(
                id = "excursions",
                languageKey = LanguageConst.ADD_PLAN_CAT_EXCURSIONS,
                iconRes = R.drawable.trp_ic_cat_excursions,
                keywords = "day trip"
            ),
            ActivityCategoryItem(
                id = "poi",
                languageKey = LanguageConst.ADD_PLAN_CAT_POI,
                iconRes = R.drawable.trp_ic_cat_poi,
                keywords = "things to do"
            ),
            ActivityCategoryItem(
                id = "food",
                languageKey = LanguageConst.ADD_PLAN_CAT_FOOD,
                iconRes = R.drawable.trp_ic_cat_food_drinks,
                keywords = "food, tasting tour"
            ),
            ActivityCategoryItem(
                id = "shows",
                languageKey = LanguageConst.ADD_PLAN_CAT_SHOWS,
                iconRes = R.drawable.trp_ic_cat_shows,
                keywords = "show"
            ),
//            ActivityCategoryItem(
//                id = "transport",
//                languageKey = LanguageConst.ADD_PLAN_CAT_TRANSPORT,
//                iconRes = R.drawable.trp_ic_cat_transfers,
//                keywords = "transfer service, transportation"
//            )
        )
    }

    // =====================
    // LOAD ACTIVITIES
    // =====================

    fun loadActivities() {
        // Validate required parameter: cityId
        if (cityId <= 0) return

        // Prevent duplicate pagination requests
        if (currentOffset > 0 && _isLoadingMore.value == true) return

        if (currentOffset == 0) {
            _isLoading.value = true
        } else {
            _isLoadingMore.value = true
        }
        // `isSearching` was the in-flight indicator for keyword API searches; since
        // search is now client-side, the spinner stays hidden permanently.
        _isSearching.value = false

        // Build keywords from selected categories only — the search text input is
        // applied client-side after the response arrives (see applyClientSideSearchFilter).
        val combinedKeywords = buildCombinedKeywords()

        // Get filter values
        val filter = _currentFilter.value ?: ActivityFilterData.default()
        // tour-api hiçbir filtre yokken de minPrice=1 ile çağrılır (free/teaser
        // listings dışarıda bırakılır). Kullanıcı daha yüksek bir alt sınır
        // seçtiyse onun değeri geçer.
        val minPrice = if (filter.minPrice > ActivityFilterData.DEFAULT_MIN_PRICE) {
            filter.minPrice.toInt()
        } else 1
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
                to = selectedDateString,   // Single-day range — `to` mirrors `date`
                currency = getCurrency(), // Use configured currency
                minPrice = minPrice,
                maxPrice = maxPrice,
                minDuration = minDuration,
                maxDuration = maxDuration,
                adults = planData?.travelers ?: 1, // Pass selected travelers count
                sortingBy = sort.sortingBy,
                sortingType = sort.sortingType,
                offset = currentOffset,
                limit = pageLimit
            ),
            success = { response ->
                _isLoading.value = false
                _isLoadingMore.value = false
                _isSearching.value = false

                val newProducts = response.data?.products ?: emptyList()
                val total = response.data?.total ?: 0

                Log.d(
                    "ACActivityListingVM",
                    "loadActivities success - newProducts: ${newProducts.size}, total: $total, currentOffset: $currentOffset"
                )

                if (currentOffset == 0) {
                    allActivities.clear()
                }
                allActivities.addAll(newProducts)
                apiTotal = total

                // Re-emit through the active search filter (no-op when query is blank).
                applyClientSideSearchFilter()
                // Check if more pages exist based on returned items count, not total (API may return incorrect total)
                _hasMorePages.value = newProducts.size >= pageLimit

                // First page also carries facet metadata — refresh chip strip + filter bounds
                if (currentOffset == 0) {
                    updateFacetsFromResponse(response.data?.facets)
                }

                Log.d(
                    "ACActivityListingVM",
                    "loadActivities - allActivities.size: ${allActivities.size}, newProducts.size: ${newProducts.size}, hasMorePages: ${_hasMorePages.value}"
                )
            },
            error = { error ->
                _isLoading.value = false
                _isLoadingMore.value = false
                _isSearching.value = false
                showAlert(
                    AlertType.ERROR,
                    error.errorDesc ?: getLanguageForKey(LanguageConst.COMMON_ERROR)
                )
                apiTotal = 0
                _activities.value = emptyList()
                _activityCount.value = 0
            }
        )
    }

    fun loadMoreActivities() {
        // Pause pagination while a client-side search filter is active — the adapter's
        // count is reduced by the filter, so the scroll listener would otherwise fire
        // loadMore continuously trying to fill the visible window.
        if (currentSearchQuery.isNotBlank()) return

        Log.d(
            "ACActivityListingVM",
            "loadMoreActivities called - hasMorePages: ${_hasMorePages.value}, isLoading: ${_isLoading.value}, isLoadingMore: ${_isLoadingMore.value}, currentOffset: $currentOffset, allActivities.size: ${allActivities.size}"
        )
        if (_hasMorePages.value == true && _isLoading.value != true && _isLoadingMore.value != true) {
            currentOffset += pageLimit
            Log.d(
                "ACActivityListingVM",
                "loadMoreActivities - loading next page with offset: $currentOffset"
            )
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

    /**
     * Confirm flow from [ActivityTimeSelectionBottomSheet]: keep the time picker open,
     * show a bottom-sheet "adding to itinerary" loader, create the reserved-activity
     * segment, then re-fetch the timeline so the host has the latest state cached.
     * Only after the fetch completes do we hide the loader and emit the success event
     * — the Activity then dismisses the sheet, shows the toast and finishes.
     */
    fun createReservedActivitySegment(
        tour: TourProduct,
        selectedDate: Date,
        timeSlot: String,
        slotPrice: Double?,
        isFlexible: Boolean = false
    ) {
        showBottomSheetLoader(LanguageConst.LOADING_TEXT_ADDING_TO_ITINERARY, "Adding to itinerary")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateString = dateFormat.format(selectedDate)

        createReservedActivitySegmentUseCase.on(
            params = CreateReservedActivitySegmentUseCase.Params(
                tripHash = tripHash,
                tour = tour,
                selectedDate = dateString,
                selectedTimeSlot = timeSlot,
                adults = planData?.travelers ?: 1,
                cityId = cityId,
                slotPrice = slotPrice,
                isFlexible = isFlexible
            ),
            success = { _ ->
                // Notify host app that activity was added (matches pre-refactor behavior)
                tour.productId?.let { TRPCore.notifyActivityAdded(it) }
                refreshTimelineAfterSegment(tour, selectedDate)
            },
            error = { error ->
                hideLottieLoading()
                showAlert(
                    AlertType.ERROR,
                    error.errorDesc ?: getLanguageForKey(LanguageConst.COMMON_ERROR)
                )
            }
        )
    }

    /** Second leg of the add-activity flow: re-fetch the timeline so we have the latest
     *  state before signaling success to the UI. */
    private fun refreshTimelineAfterSegment(tour: TourProduct, selectedDate: Date) {
        fetchTimelineUseCase.on(
            params = FetchTimelineUseCase.Params(tripHash = tripHash),
            success = { _ ->
                hideLottieLoading()
                _addedToItinerarySuccess.value = AddedToItineraryResult(
                    activityName = tour.title.orEmpty(),
                    selectedDate = selectedDate
                )
            },
            error = { error ->
                hideLottieLoading()
                showAlert(
                    AlertType.ERROR,
                    error.errorDesc ?: getLanguageForKey(LanguageConst.COMMON_ERROR)
                )
            }
        )
    }

    // =====================
    // FACETS
    // =====================

    /**
     * Pull facet metadata from the first facet entry (single-provider response —
     * providerId 15) and publish into the chip / filter LiveData fields.
     */
    private fun updateFacetsFromResponse(facets: List<TourFacet>?) {
        val facet = facets?.firstOrNull()
        if (facet == null) {
            _facetCategories.value = emptyList()
            _priceRangeFacet.value = null
            _durationRangeFacet.value = null
            return
        }
        _facetCategories.value = facet.categories?.filter { it.id != null && it.label != null }
            ?: emptyList()
        _priceRangeFacet.value = facet.priceRange
        _durationRangeFacet.value = facet.durationRange
    }

    /**
     * Returns the chip list for the category strip. Prefers facet categories from
     * the latest search response; falls back to the hard-coded list when facets
     * are unavailable (older backends / first paint before any response). Index 0
     * is always the "All" chip that clears selection.
     */
    fun getFacetCategoryItems(): List<ActivityCategoryItem> {
        val facets = _facetCategories.value.orEmpty()
        if (facets.isEmpty()) return getCategories()

        val items = mutableListOf<ActivityCategoryItem>()
        items += ActivityCategoryItem(
            id = "all",
            languageKey = LanguageConst.ADD_PLAN_CAT_ALL,
            iconRes = TourCategoryIconMapper.ALL_CATEGORIES_ICON,
            keywords = null
        )
        facets.forEach { cat ->
            items += ActivityCategoryItem(
                id = cat.id ?: cat.key ?: cat.label.orEmpty(),
                languageKey = "",
                iconRes = TourCategoryIconMapper.iconRes(cat.key),
                keywords = cat.label
            ).copy(displayLabel = cat.label)
        }
        return items
    }

    // =====================
    // HELPERS
    // =====================

    /**
     * Build combined keywords from selected categories.
     * The search-text input is intentionally excluded — it is applied client-side
     * after the response arrives. See [applyClientSideSearchFilter].
     */
    private fun buildCombinedKeywords(): String? {
        val indices = _selectedCategoryIndices.value ?: setOf(0)
        val categories = getCategories()

        if (indices.contains(0) || indices.isEmpty()) return null

        val categoryKeywords = indices
            .mapNotNull { categories.getOrNull(it)?.keywords }
            .filter { it.isNotBlank() }
            .joinToString(", ")

        return categoryKeywords.ifBlank { null }
    }

    fun getSelectedDate(): Date? = planData?.selectedDay

    fun getAvailableDays(): List<Date> = planData?.availableDays ?: emptyList()

    fun getSelectedDayIndex(): Int = selectedDayIndex

    fun getSdkLanguage(): String = appLanguage

    /**
     * Get currency for filter display
     */
    fun getCurrency(): String = TRPCore.core.appConfig.appCurrency

    // =====================
    // CLEANUP
    // =====================

    override fun onDestroy() {
        super.onDestroy()
    }
}
