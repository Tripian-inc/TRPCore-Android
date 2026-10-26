package com.tripian.trpcore.ui.timeline.activity

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
import com.tripian.trpcore.ui.timeline.compose.nav.ActivityListingArgs
import com.tripian.trpcore.util.ActivityIdFormat
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.TourCategoryIconMapper
import com.tripian.trpcore.util.extensions.appLanguage
import androidx.lifecycle.viewModelScope
import com.tripian.trpcore.repository.base.ErrorModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for Activity/Tour listing screen
 * iOS Reference: ActivityListingVC
 */
class ACActivityListingVM @Inject constructor(
    private val searchToursUseCase: SearchToursUseCase,
    private val createReservedActivitySegmentUseCase: CreateReservedActivitySegmentUseCase,
    private val fetchTimelineUseCase: FetchTimelineUseCase,
    private val timelineRepository: com.tripian.trpcore.repository.TimelineRepository
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

    /** Ids of the selected category chips; empty means the "All" chip. */
    private var selectedCategoryIds: Set<String> = emptySet()

    private val _selectedCategoryIndices = MutableLiveData(setOf(0))

    /** Strip positions of [selectedCategoryIds]; index 0 is the "All" chip. */
    val selectedCategoryIndices: LiveData<Set<Int>> = _selectedCategoryIndices

    private val _showTimeSelection = MutableLiveData<TourProduct?>()
    val showTimeSelection: LiveData<TourProduct?> = _showTimeSelection

    /**
     * Emitted after the reserved-activity segment was created AND the timeline was
     * re-fetched. Carries the tour title and selected date for the success toast;
     * the Activity sets it back to null after consuming.
     */
    data class AddedToItineraryResult(val activityName: String, val selectedDate: Date)

    private val _addedToItinerarySuccess = MutableLiveData<AddedToItineraryResult?>()
    val addedToItinerarySuccess: LiveData<AddedToItineraryResult?> = _addedToItinerarySuccess

    fun clearAddedToItinerarySuccess() {
        _addedToItinerarySuccess.value = null
    }

    private val _addSegmentError = MutableLiveData<String?>()
    val addSegmentError: LiveData<String?> = _addSegmentError

    fun clearAddSegmentError() {
        _addSegmentError.value = null
    }

    private val _currentFilter = MutableLiveData(ActivityFilterData.default())
    val currentFilter: LiveData<ActivityFilterData> = _currentFilter

    private val _currentSort = MutableLiveData(SortOption.POPULARITY)

    private val _scrollToTop = MutableLiveData<Boolean>()
    val scrollToTop: LiveData<Boolean> = _scrollToTop

    /**
     * Facet-driven category strip. Rebuilt from every first-page response whose
     * request carried no category filter and whose facet lists at least one
     * category; any other response leaves it untouched.
     * Multi-select; chip "All" (index 0) clears the selection.
     */
    private val _facetCategories = MutableLiveData<List<TourFacetCategory>>(emptyList())
    val facetCategories: LiveData<List<TourFacetCategory>> = _facetCategories

    /**
     * Bounds for the price / duration filter sliders, seeded once per listing by the
     * first page-0 response with usable bounds; null falls back to the sheet's defaults.
     */
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

    /** City of the current listing — used to resolve its timezone for the
     *  past-slot check in the time selection sheet. */
    fun getCityId(): Int = cityId
    private var selectedDayIndex: Int = 0
    private var cityLat: Double = 0.0
    private var cityLng: Double = 0.0
    /** Format: "yyyy-MM-dd" */
    private var selectedDateString: String? = null
    private var currentSearchQuery: String = ""
    private var allActivities: MutableList<TourProduct> = mutableListOf()

    /** Total result count reported by the API for the current query. */
    private var apiTotal: Int = 0

    /** Backend returns at most this many tours per call. */
    private val fetchLimit: Int = 10

    /**
     * "yyyy-MM-dd" → activity ids that day already holds. Seeded from the timeline
     * snapshot this screen was opened with and kept up to date locally by
     * [markActivityAdded], so re-opening the time selection sheet reflects an add
     * without a timeline round-trip.
     */
    private val activityIdsByDay: MutableMap<String, MutableList<String>> = mutableMapOf()

    /** Excluded on every day of the trip: bookings anywhere in it plus removed favorites. */
    private var tripWideExcludedActivityIds: List<String> = emptyList()

    private val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Offset of the next page to request for the current query. */
    private var nextOffset: Int = 0
    private var hasMorePages: Boolean = false
    private val _loadingMore = MutableLiveData(false)
    /** True while a further page is being appended; drives the bottom loading indicator. */
    val loadingMore: LiveData<Boolean> = _loadingMore
    private var isLoadingMore: Boolean
        get() = _loadingMore.value == true
        set(value) {
            if (_loadingMore.value != value) _loadingMore.value = value
        }
    private var fetchJob: Job? = null

    // =====================
    // INITIALIZATION
    // =====================

    /**
     * Launch arguments of the Compose route the listing was last initialized with;
     * compared by identity on entry to tell a new navigation from a recomposition.
     */
    internal var initializedArgs: ActivityListingArgs? = null

    fun initialize(
        planData: AddPlanData,
        tripHash: String,
        plannedActivityIdsByDay: Map<String, List<String>> = emptyMap(),
        tripWideExcludedActivityIds: List<String> = emptyList()
    ) {
        resetListingState()
        this.planData = planData
        this.tripHash = tripHash
        this.cityId = planData.selectedCity?.id ?: 0
        this.tripWideExcludedActivityIds = tripWideExcludedActivityIds
        activityIdsByDay.clear()
        plannedActivityIdsByDay.forEach { (day, ids) ->
            activityIdsByDay[day] = ids.toMutableList()
        }
        com.tripian.trpcore.util.CityTimeZones.register(listOfNotNull(planData.selectedCity))
        this.selectedDayIndex = planData.selectedDayIndex

        val cityCoordinate = planData.selectedCity?.coordinate
        this.cityLat = cityCoordinate?.lat ?: 0.0
        this.cityLng = cityCoordinate?.lng ?: 0.0

        planData.selectedDay?.let { date ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            this.selectedDateString = dateFormat.format(date)
        }

        loadActivities()
    }

    /**
     * Restores the default filter, sort, search query, category selection and facet
     * slider bounds of a fresh listing, so the next response seeds the bounds again.
     */
    private fun resetListingState() {
        _currentFilter.value = ActivityFilterData.default()
        _priceRangeFacet.value = null
        _durationRangeFacet.value = null
        _currentSort.value = SortOption.DEFAULT
        currentSearchQuery = ""
        _selectedCategoryIndices.value = setOf(0)
        _facetCategories.value = emptyList()
        selectedCategoryIds = emptySet()
    }

    // =====================
    // SEARCH
    // =====================

    /**
     * Keyword search applied by the API; the search bar debounces typing. A blank
     * query re-fetches the unfiltered list.
     */
    fun search(query: String) {
        if (currentSearchQuery == query) return
        currentSearchQuery = query
        loadActivities(useSkeleton = true)
    }

    // =====================
    // CATEGORY SELECTION
    // =====================

    /**
     * Maps the strip positions reported by the chip adapter to category ids and
     * reloads. Index 0 ("All") or an empty set selects every category.
     */
    fun onCategorySelectionChanged(selectedIndices: Set<Int>) {
        selectedCategoryIds = if (selectedIndices.isEmpty() || 0 in selectedIndices) {
            emptySet()
        } else {
            val items = getFacetCategoryItems()
            selectedIndices
                .mapNotNull { items.getOrNull(it)?.id }
                .filterTo(LinkedHashSet()) { it.isNotBlank() }
        }
        publishSelectedCategoryIndices()
        loadActivities(useSkeleton = true)
    }

    private fun publishSelectedCategoryIndices() {
        val items = getFacetCategoryItems()
        val indices = items.indices
            .filter { it != 0 && items[it].id in selectedCategoryIds }
            .toSet()
        _selectedCategoryIndices.value = indices.ifEmpty { setOf(0) }
    }

    /**
     * Builds the comma-separated category-id string sent to the tour search API
     * from [selectedCategoryIds]; null when "All" is selected.
     */
    private fun buildCategoryIds(): String? =
        selectedCategoryIds
            .filter { it.isNotBlank() }
            .joinToString(",")
            .ifBlank { null }

    /**
     * When `true`, the next `_isLoading = true` transition will NOT trigger the
     * full-screen Lottie loader. Cleared by the Activity via [consumeLoaderSuppression].
     */
    private var suppressNextIsLoadingLoader: Boolean = false

    fun consumeLoaderSuppression(): Boolean {
        val v = suppressNextIsLoadingLoader
        suppressNextIsLoadingLoader = false
        return v
    }

    /**
     * When `true`, the next `_isLoading = true` transition renders as the inline
     * shimmer skeleton. Cleared by the Activity via [consumeSkeletonRequest].
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
        loadActivities(useSkeleton = true)
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
        loadActivities(useSkeleton = true)
    }

    fun getCurrentSort(): SortOption = _currentSort.value ?: SortOption.DEFAULT

    // =====================
    // LOAD ACTIVITIES
    // =====================

    /**
     * Fetches the first page of tours for the city and resets pagination. Search,
     * category, price / duration filter and sort are always applied by the API.
     * Further pages are appended by [loadMoreActivities] until every product the API
     * counts has been loaded. minPrice=1 excludes free tours.
     *
     * @param useSkeleton when true, the reload renders as the inline shimmer
     *        skeleton instead of the full-screen Lottie.
     */
    fun loadActivities(useSkeleton: Boolean = false) {
        if (cityId <= 0) return

        fetchJob?.cancel()
        isLoadingMore = false

        if (useSkeleton) {
            useSkeletonForNextLoad = true
            suppressNextIsLoadingLoader = true
        }
        _isLoading.value = true

        val params = buildSearchParams(offset = 0)
        fetchJob = viewModelScope.launch {
            fetchPage(params, isPagination = false, scrollToTop = useSkeleton)
        }
    }

    /** Appends the next page when the API reports more products than are loaded. */
    fun loadMoreActivities() {
        if (!hasMorePages || isLoadingMore || _isLoading.value == true) return
        isLoadingMore = true
        val params = buildSearchParams(offset = nextOffset)
        fetchJob = viewModelScope.launch {
            fetchPage(params, isPagination = true, scrollToTop = false)
        }
    }

    /** Snapshots the current search, category, filter and sort state into a request. */
    private fun buildSearchParams(offset: Int): SearchToursUseCase.Params {
        val filter = _currentFilter.value ?: ActivityFilterData.default()
        val sort = _currentSort.value ?: SortOption.DEFAULT
        return SearchToursUseCase.Params(
            cityId = cityId,
            lat = cityLat,
            lng = cityLng,
            keywords = currentSearchQuery.trim().ifBlank { null },
            tagIds = null,
            categoryIds = buildCategoryIds(),
            providerId = 15,
            date = selectedDateString,
            to = selectedDateString,
            currency = getCurrency(),
            minPrice = filter.minPrice.toInt().coerceAtLeast(1),
            maxPrice = filter.maxPrice.toInt()
                .takeIf { filter.maxPrice != ActivityFilterData.DEFAULT_MAX_PRICE },
            minDuration = filter.minDuration.toInt()
                .takeIf { filter.minDuration != ActivityFilterData.DEFAULT_MIN_DURATION },
            maxDuration = filter.maxDuration.toInt()
                .takeIf { filter.maxDuration != ActivityFilterData.DEFAULT_MAX_DURATION },
            adults = (planData?.travelers ?: 1).coerceAtLeast(1),
            sortingBy = sort.sortingBy,
            sortingType = sort.sortingType,
            offset = offset,
            limit = fetchLimit
        )
    }

    /**
     * Runs [params] and applies the response. The category strip may only be rebuilt
     * from a first-page response whose own request carried no category filter.
     */
    private suspend fun fetchPage(
        params: SearchToursUseCase.Params,
        isPagination: Boolean,
        scrollToTop: Boolean
    ) {
        val offset = params.offset
        val canRebuildCategoryStrip =
            !isPagination && offset == 0 && params.categoryIds.isNullOrEmpty()

        val result = runCatching { searchToursUseCase(params) }
        coroutineContext.ensureActive()

        isLoadingMore = false
        if (!isPagination) _isLoading.value = false

        result
            .onSuccess { response ->
                val products = response.data?.products ?: emptyList()
                if (!isPagination) allActivities.clear()
                val loadedIds = allActivities.map { it.productId }.toHashSet()
                val newProducts = products.filter { loadedIds.add(it.productId) }
                allActivities.addAll(newProducts)
                apiTotal = response.data?.total ?: allActivities.size
                nextOffset = offset + products.size
                hasMorePages = newProducts.isNotEmpty() && allActivities.size < apiTotal
                updateFacetsFromResponse(
                    facets = response.data?.facets,
                    isFirstPage = !isPagination && offset == 0,
                    rebuildCategoryStrip = canRebuildCategoryStrip
                )
                _activities.value = allActivities.toList()
                _activityCount.value = apiTotal
                if (scrollToTop) _scrollToTop.value = true
            }
            .onFailure { error ->
                val message = (error as? ErrorModel)?.errorDesc
                    ?: error.message
                    ?: getLanguageForKey(LanguageConst.COMMON_ERROR)
                showAlert(AlertType.ERROR, message)
                if (!isPagination) {
                    allActivities.clear()
                    apiTotal = 0
                    hasMorePages = false
                    _activities.value = emptyList()
                    _activityCount.value = 0
                }
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

    /**
     * What each day already holds; the time selection sheet blocks the days holding
     * the picked activity and the chosen day's ids ship as `excludedActivityIds`.
     */
    fun plannedActivityIdsByDay(): Map<String, List<String>> =
        activityIdsByDay.mapValues { it.value.toList() }

    /** Records a day just taken by [tour] so re-opening the sheet reflects it right away. */
    private fun markActivityAdded(tour: TourProduct, day: Date) {
        val id = ActivityIdFormat.make(
            activityId = tour.productId,
            providerId = tour.providerId,
            cityId = tour.cityId.takeIf { it > 0 } ?: cityId.takeIf { it > 0 }
        )
        if (id.isEmpty()) return

        val dayIds = activityIdsByDay.getOrPut(dayKeyFormat.format(day)) { mutableListOf() }
        if (id !in dayIds) dayIds += id
    }

    // =====================
    // CREATE SEGMENT
    // =====================

    /**
     * Confirm flow from [ActivityTimeSelectionBottomSheet]: creates the
     * reserved-activity segment, re-fetches the timeline, and only then emits the
     * success event. The loader itself is the sheet's own inline overlay, driven by
     * the caller (see [ACActivityListing.showTimeSelectionBottomSheet]) — this VM
     * never shows a separate loader.
     */
    fun createReservedActivitySegment(
        tour: TourProduct,
        selectedDate: Date,
        timeSlot: String,
        slotPrice: Double?,
        isFlexible: Boolean = false
    ) {
        val dateString = dayKeyFormat.format(selectedDate)

        viewModelScope.launch {
            runCatching {
                createReservedActivitySegmentUseCase(
                    CreateReservedActivitySegmentUseCase.Params(
                        tripHash = tripHash,
                        tour = tour,
                        selectedDate = dateString,
                        selectedTimeSlot = timeSlot,
                        adults = planData?.travelers ?: 1,
                        cityId = cityId,
                        slotPrice = slotPrice,
                        isFlexible = isFlexible,
                        excludedActivityIds = (
                            tripWideExcludedActivityIds +
                                activityIdsByDay[dateString].orEmpty()
                            ).distinct()
                    )
                )
            }
                .onSuccess {
                    markActivityAdded(tour, selectedDate)
                    tour.productId?.let { TRPCore.notifyActivityAdded(it) }
                    refreshTimelineAfterSegment(tour, selectedDate)
                }
                .onFailure { t ->
                    val msg = (t as? ErrorModel)?.errorDesc ?: t.message
                    _addSegmentError.value = msg ?: getLanguageForKey(LanguageConst.COMMON_ERROR)
                }
        }
    }

    /** Second leg of the add-activity flow: re-fetches the timeline and caches it so
     *  the timeline screen applies it on return without a second GET. */
    private fun refreshTimelineAfterSegment(tour: TourProduct, selectedDate: Date) {
        viewModelScope.launch {
            runCatching { fetchTimelineUseCase(FetchTimelineUseCase.Params(tripHash = tripHash)) }
                .onSuccess { timeline ->
                    timelineRepository.cacheGeneratedTimeline(tripHash, timeline)
                    _addedToItinerarySuccess.value = AddedToItineraryResult(
                        activityName = tour.title.orEmpty(),
                        selectedDate = selectedDate
                    )
                }
                .onFailure { t ->
                    val msg = (t as? ErrorModel)?.errorDesc ?: t.message
                    _addSegmentError.value = msg ?: getLanguageForKey(LanguageConst.COMMON_ERROR)
                }
        }
    }

    // =====================
    // FACETS
    // =====================

    /**
     * Seeds the filter slider bounds from the first facet entry when [isFirstPage] is
     * true and, when [rebuildCategoryStrip] is true, rebuilds the chip strip from its
     * categories.
     */
    private fun updateFacetsFromResponse(
        facets: List<TourFacet>?,
        isFirstPage: Boolean,
        rebuildCategoryStrip: Boolean
    ) {
        val facet = facets?.firstOrNull()
        if (isFirstPage) seedRangeFacets(facet)
        if (rebuildCategoryStrip) rebuildCategoryStrip(facet?.categories)
    }

    /**
     * Publishes each usable price / duration range of [facet] that is not seeded yet;
     * a seeded range is kept until [resetListingState] clears it, so narrowing filters
     * never shrink the slider bounds.
     */
    private fun seedRangeFacets(facet: TourFacet?) {
        if (_priceRangeFacet.value == null) {
            facet?.priceRange?.takeIf { it.hasUsableBounds() }?.let { _priceRangeFacet.value = it }
        }
        if (_durationRangeFacet.value == null) {
            facet?.durationRange?.takeIf { it.hasUsableBounds() }?.let { _durationRangeFacet.value = it }
        }
    }

    /**
     * Replaces the chip strip with [categories] and drops selected ids that are no
     * longer offered. An empty or missing list keeps the current strip.
     */
    private fun rebuildCategoryStrip(categories: List<TourFacetCategory>?) {
        val chips = categories.orEmpty().filter { it.id != null && it.label != null }
        if (chips.isEmpty()) return

        val current = _facetCategories.value.orEmpty()
        if (chips.map { Triple(it.id, it.label, it.key) } != current.map { Triple(it.id, it.label, it.key) }) {
            _facetCategories.value = chips
        }
        selectedCategoryIds = reconcileCategorySelection(
            selectedIds = selectedCategoryIds,
            availableIds = chips.mapNotNull { it.id }
        )
        publishSelectedCategoryIndices()
    }

    private fun TourFacetPriceRange.hasUsableBounds(): Boolean {
        val min = minimum?.amount ?: return false
        val max = maximum?.amount ?: return false
        return max > min
    }

    private fun TourFacetDurationRange.hasUsableBounds(): Boolean {
        val min = minimumMinutes ?: return false
        val max = maximumMinutes ?: return false
        return max > min
    }

    /**
     * Returns the chip list for the category strip. Index 0 is always the "All"
     * chip; the rest come from the latest rebuilt facet snapshot. Before the first
     * non-empty facet arrives, only "All" is shown.
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

    fun getCurrency(): String = TRPCore.core.appConfig.appCurrency

    companion object {
        /**
         * Keeps the [selectedIds] still present in [availableIds], preserving order.
         * An empty result means the "All" chip.
         */
        internal fun reconcileCategorySelection(
            selectedIds: Set<String>,
            availableIds: Collection<String>
        ): Set<String> {
            val available = availableIds.toHashSet()
            return selectedIds.filterTo(LinkedHashSet()) { it in available }
        }
    }
}
