package com.tripian.trpcore.ui.timeline.activity

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.tour.model.TourFacet
import com.tripian.one.api.tour.model.TourFacetCategory
import com.tripian.one.api.tour.model.TourFacetDurationRange
import com.tripian.one.api.tour.model.TourFacetPriceRange
import com.tripian.one.api.tour.model.TourProduct
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
import androidx.lifecycle.viewModelScope
import com.tripian.trpcore.repository.base.ErrorModel
import kotlinx.coroutines.launch
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

    private val _activityCount = MutableLiveData<Int>()
    val activityCount: LiveData<Int> = _activityCount

    // Multiple category selection support
    private val _selectedCategoryIndices = MutableLiveData(setOf(0))
    val selectedCategoryIndices: LiveData<Set<Int>> = _selectedCategoryIndices

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

    // Facet-driven category strip — populated from the FIRST non-empty search
    // response and then frozen. Subsequent filter/sort/search calls do NOT
    // mutate the chip strip so the user keeps the same set of categories to
    // choose from. Multi-select; chip "All" (index 0) clears the selection.
    private val _facetCategories = MutableLiveData<List<TourFacetCategory>>(emptyList())
    val facetCategories: LiveData<List<TourFacetCategory>> = _facetCategories

    // True once the chip strip has been populated from the initial response.
    // Guards the strip against being reshuffled by later responses.
    private var categoryStripFrozen: Boolean = false

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
    private var allActivities: MutableList<TourProduct> = mutableListOf()

    // Backend returns at most this many tours per call. The screen pulls the
    // whole list once on init and then does all filtering / sorting / search
    // locally, so this needs to be large enough to cover any plausible city.
    private val fetchLimit: Int = 10

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
     * Cache the latest query without applying the filter. The visible list is
     * only re-filtered when the user submits via the keyboard's Enter/IME
     * action (see [submitSearch]) — typing alone does not trigger a refilter.
     */
    fun updateSearchText(query: String) {
        currentSearchQuery = query
    }

    /**
     * Triggered by the keyboard's Enter / IME search action. Re-applies the
     * full local pipeline with a short skeleton flash so the change feels
     * deliberate.
     */
    fun submitSearch() {
        applyAllFiltersWithSkeleton()
    }

    // =====================
    // CATEGORY SELECTION
    // =====================

    fun onCategorySelectionChanged(selectedIndices: Set<Int>) {
        _selectedCategoryIndices.value = selectedIndices
        applyAllFiltersWithSkeleton()
    }

    /**
     * When `true`, the next `_isLoading = true` transition will NOT trigger the
     * default full-screen Lottie loader from the Activity — used in tandem with
     * [useSkeletonForNextLoad] so the skeleton variant wins. Cleared by the
     * Activity via [consumeLoaderSuppression].
     */
    private var suppressNextIsLoadingLoader: Boolean = false

    fun consumeLoaderSuppression(): Boolean {
        val v = suppressNextIsLoadingLoader
        suppressNextIsLoadingLoader = false
        return v
    }

    /**
     * When `true`, the next `_isLoading = true` transition should render as the
     * inline shimmer skeleton (filter / sort / category / search) rather than
     * the full-screen Lottie. Cleared by the Activity via [consumeSkeletonRequest].
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

    fun applyFilter(filter: ActivityFilterData) {
        _currentFilter.value = filter
        applyAllFiltersWithSkeleton()
    }

    fun getCurrentFilter(): ActivityFilterData =
        _currentFilter.value ?: ActivityFilterData.default()

    fun hasActiveFilters(): Boolean = _currentFilter.value?.hasActiveFilters() == true

    fun getActiveFilterCount(): Int = _currentFilter.value?.activeFilterCount() ?: 0

    // =====================
    // SORT
    // =====================

    fun applySort(sort: SortOption) {
        _currentSort.value = sort
        applyAllFiltersWithSkeleton()
    }

    fun getCurrentSort(): SortOption = _currentSort.value ?: SortOption.DEFAULT

    // =====================
    // LOAD ACTIVITIES
    // =====================

    /**
     * Pulls the full tour list for the city in a single call. Filter / sort /
     * search / category selection are intentionally NOT forwarded to the API —
     * they all run locally on [allActivities] after the response arrives, so a
     * filter change never re-hits the network.
     */
    fun loadActivities() {
        if (cityId <= 0) return

        _isLoading.value = true

        viewModelScope.launch {
            runCatching {
                searchToursUseCase(
                    SearchToursUseCase.Params(
                        cityId = cityId,
                        lat = cityLat,
                        lng = cityLng,
                        keywords = null,
                        tagIds = null,
                        providerId = 15, // Always use providerId 15 for tour-api
                        date = selectedDateString,
                        to = selectedDateString,
                        currency = getCurrency(),
                        // Filters/sort handled locally — request everything available.
                        minPrice = 1,
                        maxPrice = null,
                        minDuration = null,
                        maxDuration = null,
                        adults = planData?.travelers ?: 1,
                        sortingBy = null,
                        sortingType = null,
                        offset = 0,
                        limit = fetchLimit
                    )
                )
            }.onSuccess { response ->
                _isLoading.value = false
                val products = response.data?.products ?: emptyList()
                allActivities.clear()
                allActivities.addAll(products)
                updateFacetsFromResponse(response.data?.facets)
                applyAllFilters()
            }.onFailure { error ->
                _isLoading.value = false
                val message = (error as? ErrorModel)?.errorDesc
                    ?: error.message
                    ?: getLanguageForKey(LanguageConst.COMMON_ERROR)
                showAlert(AlertType.ERROR, message)
                allActivities.clear()
                _activities.value = emptyList()
                _activityCount.value = 0
            }
        }
    }

    // =====================
    // LOCAL FILTER PIPELINE
    // =====================

    // Skeleton flash for filter/sort/category/search interactions — gives the
    // user a brief visual cue that the list is being recomputed, even though
    // the work happens locally.
    private val skeletonHandler = Handler(Looper.getMainLooper())
    private val skeletonShowMs: Long = 350L

    /**
     * Runs the local filter pipeline behind a short skeleton flash, then
     * scrolls the list back to the top. Used by every user-triggered list
     * change (filter / sort / category / search submit).
     */
    private fun applyAllFiltersWithSkeleton() {
        skeletonHandler.removeCallbacksAndMessages(null)
        useSkeletonForNextLoad = true
        suppressNextIsLoadingLoader = true
        _isLoading.value = true
        skeletonHandler.postDelayed({
            applyAllFilters()
            _isLoading.value = false
            _scrollToTop.value = true
        }, skeletonShowMs)
    }

    /**
     * Apply the full local filter pipeline (category → price → duration →
     * title search → sort) to [allActivities] and publish the result.
     */
    private fun applyAllFilters() {
        val byCategory = allActivities.filter { tourMatchesSelectedCategories(it) }
        val byPriceDuration = byCategory.filter { tourMatchesFilter(it) }
        val bySearch = applyTitleSearch(byPriceDuration)
        val sorted = sortActivities(bySearch)
        _activities.value = sorted
        _activityCount.value = sorted.size
    }

    private fun applyTitleSearch(list: List<TourProduct>): List<TourProduct> {
        val q = currentSearchQuery.trim()
        if (q.isBlank()) return list
        return list.filter { it.title?.contains(q, ignoreCase = true) == true }
    }

    private fun tourMatchesFilter(tour: TourProduct): Boolean {
        val filter = _currentFilter.value ?: return true
        val price = tour.currentPrice ?: tour.price
        val priceOk = price?.let {
            it >= filter.minPrice && it <= filter.maxPrice
        } ?: true
        val duration = tour.duration
        val durationOk = duration?.let {
            it >= filter.minDuration && it <= filter.maxDuration
        } ?: true
        return priceOk && durationOk
    }

    /**
     * Heuristic match between the user-selected chips and the tour's tags /
     * title. Facet entries carry the backend label as `keywords` and a stable
     * `key` / `id`; we look those up against the tour's tag strings (case
     * insensitive). "All" (index 0) or an empty selection passes everything.
     */
    private fun tourMatchesSelectedCategories(tour: TourProduct): Boolean {
        val selected = _selectedCategoryIndices.value ?: setOf(0)
        if (selected.contains(0) || selected.isEmpty()) return true

        val items = getFacetCategoryItems()
        val facets = _facetCategories.value.orEmpty()
        val tagsLower = tour.tags.orEmpty().map { it.lowercase() }
        val titleLower = tour.title.orEmpty().lowercase()

        return selected.any { idx ->
            if (idx == 0) return@any false
            val needles = mutableListOf<String>()
            items.getOrNull(idx)?.let { item ->
                item.displayLabel?.let { needles += it }
                item.keywords?.let { needles += it }
            }
            // facets list is offset by 1 (index 0 is the "All" chip).
            facets.getOrNull(idx - 1)?.let { cat ->
                cat.label?.let { needles += it }
                cat.key?.let { needles += it }
                cat.id?.let { needles += it }
            }
            val terms = needles
                .flatMap { it.split(",") }
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
                .distinct()
            terms.any { term ->
                tagsLower.any { it.contains(term) } || titleLower.contains(term)
            }
        }
    }

    private fun sortActivities(list: List<TourProduct>): List<TourProduct> {
        val sort = _currentSort.value ?: SortOption.DEFAULT
        return when (sort) {
            SortOption.POPULARITY -> list
            SortOption.RATING -> list.sortedByDescending { it.rating ?: 0.0 }
            SortOption.PRICE_LOW_TO_HIGH -> list.sortedBy { it.currentPrice ?: it.price ?: Double.MAX_VALUE }
            SortOption.DURATION_SHORT_TO_LONG -> list.sortedBy { it.duration ?: Double.MAX_VALUE }
            SortOption.DURATION_LONG_TO_SHORT -> list.sortedByDescending { it.duration ?: Double.MIN_VALUE }
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
     * providerId 15) and publish into the filter slider LiveData fields. The
     * chip strip is populated only from the FIRST non-empty response and then
     * frozen (see [categoryStripFrozen]) — subsequent filter / search / sort
     * responses refresh the price + duration ranges but never reshuffle the
     * chip strip, so the user keeps a stable set of categories to switch
     * between.
     */
    private fun updateFacetsFromResponse(facets: List<TourFacet>?) {
        val facet = facets?.firstOrNull()
        if (facet == null) {
            _priceRangeFacet.value = null
            _durationRangeFacet.value = null
            return
        }
        _priceRangeFacet.value = facet.priceRange
        _durationRangeFacet.value = facet.durationRange

        if (categoryStripFrozen) return
        val cats = facet.categories?.filter { it.id != null && it.label != null }
            ?: emptyList()
        if (cats.isNotEmpty()) {
            _facetCategories.value = cats
            categoryStripFrozen = true
        }
    }

    /**
     * Returns the chip list for the category strip. Index 0 is always the "All"
     * chip; the rest come from the frozen facet snapshot. Before the first
     * response (or if facets ever arrived empty), only "All" is shown.
     */
    fun getFacetCategoryItems(): List<ActivityCategoryItem> {
        val items = mutableListOf<ActivityCategoryItem>()
        items += ActivityCategoryItem(
            id = "all",
            languageKey = LanguageConst.ADD_PLAN_CAT_ALL,
            iconRes = TourCategoryIconMapper.ALL_CATEGORIES_ICON,
            keywords = null
        )
        _facetCategories.value.orEmpty().forEach { cat ->
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
        skeletonHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
