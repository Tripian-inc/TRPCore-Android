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
import com.tripian.trpcore.util.ActivityIdFormat
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

    private val _selectedCategoryIndices = MutableLiveData(setOf(0))
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
     * Facet-driven category strip — populated from the first non-empty search
     * response and then frozen so later calls never reshuffle the chips.
     * Multi-select; chip "All" (index 0) clears the selection.
     */
    private val _facetCategories = MutableLiveData<List<TourFacetCategory>>(emptyList())
    val facetCategories: LiveData<List<TourFacetCategory>> = _facetCategories

    /** Guards the chip strip against being reshuffled by later responses. */
    private var categoryStripFrozen: Boolean = false

    /** Bounds for the price / duration filter sliders; null falls back to the sheet's defaults. */
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

    /** Total result count reported by the API for the last request; shown when no local narrowing is active. */
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

    // =====================
    // INITIALIZATION
    // =====================

    fun initialize(
        planData: AddPlanData,
        tripHash: String,
        plannedActivityIdsByDay: Map<String, List<String>> = emptyMap(),
        tripWideExcludedActivityIds: List<String> = emptyList()
    ) {
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

    // =====================
    // SEARCH
    // =====================

    /**
     * Title search over the already fetched tours. Runs entirely locally, so it
     * is applied as the user types.
     */
    fun search(query: String) {
        if (currentSearchQuery == query) return
        currentSearchQuery = query
        applyAllFilters()
        _scrollToTop.value = true
    }

    // =====================
    // CATEGORY SELECTION
    // =====================

    fun onCategorySelectionChanged(selectedIndices: Set<Int>) {
        _selectedCategoryIndices.value = selectedIndices
        loadActivities(useSkeleton = true)
    }

    /**
     * Builds the comma-separated category-id string sent to the tour search API
     * from the selected chips. "All" (index 0) or an empty selection returns
     * null so the API returns every category.
     */
    private fun buildCategoryIds(): String? {
        val indices = _selectedCategoryIndices.value ?: setOf(0)
        if (indices.contains(0) || indices.isEmpty()) return null

        val items = getFacetCategoryItems()
        return indices
            .mapNotNull { items.getOrNull(it)?.id }
            .filter { it.isNotBlank() }
            .joinToString(",")
            .ifBlank { null }
    }

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
     * Fetches the tour list for the city. Category chips are forwarded to the API
     * as categoryIds; price / duration / title search / sort run locally on
     * [allActivities] after the response arrives. minPrice=1 excludes free tours.
     *
     * @param useSkeleton when true, the reload renders as the inline shimmer
     *        skeleton instead of the full-screen Lottie.
     */
    fun loadActivities(useSkeleton: Boolean = false) {
        if (cityId <= 0) return

        if (useSkeleton) {
            useSkeletonForNextLoad = true
            suppressNextIsLoadingLoader = true
        }
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
                        categoryIds = buildCategoryIds(),
                        providerId = 15,
                        date = selectedDateString,
                        to = selectedDateString,
                        currency = getCurrency(),
                        minPrice = 1,
                        maxPrice = null,
                        minDuration = null,
                        maxDuration = null,
                        adults = (planData?.travelers ?: 1).coerceAtLeast(1),
                        sortingBy = "score",
                        sortingType = "desc",
                        offset = 0,
                        limit = fetchLimit
                    )
                )
            }.onSuccess { response ->
                _isLoading.value = false
                val products = response.data?.products ?: emptyList()
                allActivities.clear()
                allActivities.addAll(products)
                apiTotal = response.data?.total ?: products.size
                updateFacetsFromResponse(response.data?.facets)
                applyAllFilters()
                if (useSkeleton) _scrollToTop.value = true
            }.onFailure { error ->
                _isLoading.value = false
                val message = (error as? ErrorModel)?.errorDesc
                    ?: error.message
                    ?: getLanguageForKey(LanguageConst.COMMON_ERROR)
                showAlert(AlertType.ERROR, message)
                allActivities.clear()
                apiTotal = 0
                _activities.value = emptyList()
                _activityCount.value = 0
            }
        }
    }

    // =====================
    // LOCAL FILTER PIPELINE
    // =====================

    /** Skeleton flash for user-triggered local list recomputes. */
    private val skeletonHandler = Handler(Looper.getMainLooper())
    private val skeletonShowMs: Long = 350L

    /**
     * Runs the local filter pipeline behind a short skeleton flash, then
     * scrolls the list back to the top. Used by every user-triggered list
     * change (filter / sort / category).
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
     * Apply the local filter pipeline (price → duration → title search → sort)
     * to [allActivities] and publish the result. Category is applied server-side
     * via categoryIds (see [loadActivities]). Publishes the API total as the count
     * when no local narrowing is active, otherwise the visible size.
     */
    private fun applyAllFilters() {
        val byPriceDuration = allActivities.filter { tourMatchesFilter(it) }
        val bySearch = applyTitleSearch(byPriceDuration)
        val sorted = sortActivities(bySearch)
        _activities.value = sorted
        val hasLocalNarrowing = currentSearchQuery.isNotBlank() ||
            (_currentFilter.value?.hasActiveFilters() == true)
        _activityCount.value = if (hasLocalNarrowing) sorted.size else apiTotal
    }

    private fun applyTitleSearch(list: List<TourProduct>): List<TourProduct> {
        val q = currentSearchQuery.trim()
        if (q.isBlank()) return list
        return list.filter { it.title?.contains(q, ignoreCase = true) == true }
    }

    /**
     * The default filter carries the slider's own bounds, not a user choice, so it
     * must let every tour through — a tour longer than the default 24h ceiling is
     * still part of an unfiltered list.
     */
    private fun tourMatchesFilter(tour: TourProduct): Boolean {
        val filter = _currentFilter.value ?: return true
        if (!filter.hasActiveFilters()) return true

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
     * Pulls facet metadata from the first facet entry and publishes it into the
     * filter slider LiveData fields. The chip strip is populated only from the
     * first non-empty response and then frozen (see [categoryStripFrozen]).
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

    fun getCurrency(): String = TRPCore.core.appConfig.appCurrency

    // =====================
    // CLEANUP
    // =====================

    override fun onDestroy() {
        skeletonHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
