package com.tripian.trpcore.ui.timeline.poilisting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.pois.model.PoiCategoryGroup
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetPoiCategories
import com.tripian.trpcore.domain.manager.POICategoryManager
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.FilterData
import com.tripian.trpcore.domain.model.timeline.SortOption
import com.tripian.trpcore.domain.usecase.timeline.CreateManualPoiSegmentUseCase
import com.tripian.trpcore.domain.usecase.timeline.FetchTimelineUseCase
import com.tripian.trpcore.domain.usecase.timeline.SearchPOIsUseCase
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.CityTimeZones
import com.tripian.trpcore.util.LanguageConst
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ACPOIListingVM
 * ViewModel for POI listing screen
 * iOS Reference: POIListingVC
 */
class ACPOIListingVM @Inject constructor(
    private val searchPOIsUseCase: SearchPOIsUseCase,
    private val createManualPoiSegmentUseCase: CreateManualPoiSegmentUseCase,
    private val fetchTimelineUseCase: FetchTimelineUseCase,
    private val getPoiCategoriesUseCase: GetPoiCategories,
    private val poiRepository: PoiRepository,
    private val timelineRepository: com.tripian.trpcore.repository.TimelineRepository
) : BaseViewModel() {

    private val _pois = MutableLiveData<List<Poi>>()
    val pois: LiveData<List<Poi>> = _pois

    private val _poiCount = MutableLiveData<Int>()
    val poiCount: LiveData<Int> = _poiCount

    private val _hasMorePages = MutableLiveData<Boolean>()
    val hasMorePages: LiveData<Boolean> = _hasMorePages

    private val _showTimeSelection = MutableLiveData<Poi?>()
    val showTimeSelection: LiveData<Poi?> = _showTimeSelection

    /**
     * Emitted after the manual-POI segment was created AND the timeline was
     * re-fetched successfully. Carries the POI name and the selected date so the
     * Activity can format and show the success toast. Activity sets it back to null
     * after consuming.
     */
    data class AddedToItineraryResult(val poiName: String, val selectedDate: Date)

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

    private val _currentFilter = MutableLiveData(FilterData())
    val currentFilter: LiveData<FilterData> = _currentFilter

    private val _currentSort = MutableLiveData(SortOption.DEFAULT)
    val currentSort: LiveData<SortOption> = _currentSort

    private val _categoryGroups = MutableLiveData<List<PoiCategoryGroup>>()
    val categoryGroups: LiveData<List<PoiCategoryGroup>> = _categoryGroups

    /** Fires when page 1 loads or sort changes. */
    private val _scrollToTop = MutableLiveData<Boolean>()
    val scrollToTop: LiveData<Boolean> = _scrollToTop

    fun clearScrollToTop() {
        _scrollToTop.value = false
    }

    private var planData: AddPlanData? = null
    private var tripHash: String = ""
    private var cityId: Int = 0
    private var listingType: POIListingType = POIListingType.PLACES_OF_INTEREST
    private var selectedDayIndex: Int = 0
    private var currentSearchQuery: String = ""
    private var currentPage: Int = 1
    private val pageLimit: Int = 30
    private var allPois: MutableList<Poi> = mutableListOf()
    private val _loadingMore = MutableLiveData(false)
    /** True while a further page is being appended; drives the bottom loading indicator. */
    val loadingMore: LiveData<Boolean> = _loadingMore
    private var isLoadingMore: Boolean
        get() = _loadingMore.value == true
        set(value) {
            if (_loadingMore.value != value) _loadingMore.value = value
        }
    private var totalCount: Int = 0

    /** Sets up state and loads POIs. The loader is shown before category prefetch to avoid a blank-screen flash. */
    fun initialize(planData: AddPlanData, tripHash: String, listingType: POIListingType) {
        this.planData = planData
        this.tripHash = tripHash
        this.cityId = planData.selectedCity?.id ?: 0
        this.listingType = listingType
        this.selectedDayIndex = planData.selectedDayIndex

        showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_PLACES, "")

        fetchCategories()

        POICategoryManager.prefetchIfNeeded(poiRepository) {
            loadPOIs(useFullScreen = true)
        }
    }

    /**
     * Category IDs that identify "Eat & Drink" groups
     * Must match POICategoryManager.EAT_AND_DRINK_CATEGORY_IDS
     */
    private val EAT_AND_DRINK_CATEGORY_IDS = listOf(3, 4, 24)

    private fun fetchCategories() {
        viewModelScope.launch {
            runCatching { getPoiCategoriesUseCase(Unit) }
                .onSuccess { categoryModel ->
                    val allGroups = categoryModel?.groups ?: emptyList()
                    val filteredGroups = allGroups.filter { group ->
                        val groupCategoryIds = group.categories?.map { it.id } ?: emptyList()
                        val hasEatAndDrinkCategory = groupCategoryIds.any { id ->
                            EAT_AND_DRINK_CATEGORY_IDS.contains(id)
                        }
                        when (listingType) {
                            POIListingType.EAT_AND_DRINK -> hasEatAndDrinkCategory
                            POIListingType.PLACES_OF_INTEREST -> !hasEatAndDrinkCategory
                        }
                    }
                    _categoryGroups.value = filteredGroups
                }
                .onFailure { _categoryGroups.value = emptyList() }
        }
    }

    /**
     * Runs the POI search for [query], resetting pagination. Debounced by the
     * search bar, so every call here is meant to hit the service.
     */
    fun search(query: String) {
        if (currentSearchQuery == query) return
        currentSearchQuery = query
        resetAndSearch()
    }

    private fun resetAndSearch() {
        isLoadingMore = false
        loadPOIs(useFullScreen = false)
    }

    /**
     * @param useFullScreen `true` for the very first load (covers the empty-list
     *   flash with the full-screen Lottie). `false` for every subsequent fresh
     *   load — filter, sort, search — which uses the lighter bottom-sheet Lottie
     *   so the filter chips and search field remain visible while loading.
     */
    fun loadPOIs(useFullScreen: Boolean = false) {
        if (cityId <= 0) return

        val isPagination = isLoadingMore
        if (!isPagination) {
            if (useFullScreen) {
                showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_PLACES, "")
            } else {
                showBottomSheetLoader(LanguageConst.LOADING_TEXT_GETTING_PLACES, "")
            }
            currentPage = 1
        }

        val filter = _currentFilter.value ?: FilterData()
        val sort = _currentSort.value ?: SortOption.DEFAULT

        val listingTypeCategoryIds = POICategoryManager.getCategoryIds(listingType) ?: emptyList()

        val categoryIds = if (filter.selectedCategoryIds.isNotEmpty()) {
            listingTypeCategoryIds.filter { it in filter.selectedCategoryIds }
        } else {
            listingTypeCategoryIds
        }.takeIf { it.isNotEmpty() }

        val pageToFetch = if (isPagination) currentPage + 1 else 1

        val sortingBy = if (sort == SortOption.POPULARITY) null else sort.sortingBy
        val sortingType = if (sort == SortOption.POPULARITY) null else sort.sortingType

        viewModelScope.launch {
            runCatching {
                searchPOIsUseCase(
                    SearchPOIsUseCase.Params(
                        cityId = cityId,
                        search = currentSearchQuery.ifBlank { null },
                        categoryIds = categoryIds,
                        page = pageToFetch,
                        limit = pageLimit,
                        sortingBy = sortingBy,
                        sortingType = sortingType
                    )
                )
            }
                .onSuccess { response ->
                    hideLottieLoading()
                    isLoadingMore = false
                    val newPois = response.data ?: emptyList()
                    totalCount = response.pagination?.total ?: newPois.size
                    if (!isPagination) {
                        allPois.clear()
                        _scrollToTop.value = true
                    }
                    allPois.addAll(newPois)
                    currentPage = pageToFetch
                    _pois.value = allPois.toList()
                    _poiCount.value = totalCount
                    _hasMorePages.value = allPois.size < totalCount
                }
                .onFailure { t ->
                    val msg = (t as? ErrorModel)?.errorDesc ?: t.message
                    hideLottieLoading()
                    isLoadingMore = false
                    showAlert(AlertType.ERROR, msg)
                    if (!isPagination) {
                        _pois.value = emptyList()
                        _poiCount.value = 0
                    }
                }
        }
    }

    fun loadMorePOIs() {
        if (_hasMorePages.value == true && !isLoadingMore) {
            isLoadingMore = true
            loadPOIs()
        }
    }

    fun onPOIAddClicked(poi: Poi) {
        _showTimeSelection.value = poi
    }

    fun clearTimeSelection() {
        _showTimeSelection.value = null
    }

    /**
     * Apply filter and reload POIs
     */
    fun applyFilter(filter: FilterData) {
        _currentFilter.value = filter
        resetAndSearch()
    }

    /**
     * Apply sort option and reload POIs from API
     * POPULARITY: no sorting params sent (API default)
     * RATING: sends sorting params to API
     */
    fun applySort(sort: SortOption) {
        _currentSort.value = sort
        resetAndSearch()
    }

    /**
     * Clear all filters and reload POIs
     */
    fun clearFilters() {
        _currentFilter.value = FilterData()
        resetAndSearch()
    }

    /**
     * Confirm flow from [TimeSelectionBottomSheet]: creates the manual-POI segment,
     * then re-fetches the timeline. The success event is emitted only after the
     * fetch completes. The loader itself is the sheet's own inline overlay, driven
     * by the caller (see [ACPOIListing.showTimeRangeBottomSheet]) — this VM never
     * shows a separate loader.
     */
    fun createManualPoiSegment(poi: Poi, selectedDate: Date, startTime: String, endTime: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateString = dateFormat.format(selectedDate)

        viewModelScope.launch {
            runCatching {
                createManualPoiSegmentUseCase(
                    CreateManualPoiSegmentUseCase.Params(
                        tripHash = tripHash,
                        poi = poi,
                        selectedDate = dateString,
                        startTime = startTime,
                        endTime = endTime,
                        cityId = cityId
                    )
                )
            }
                .onSuccess { refreshTimelineAfterSegment(poi, selectedDate) }
                .onFailure { t ->
                    val msg = (t as? ErrorModel)?.errorDesc ?: t.message
                    _addSegmentError.value = msg ?: getLanguageForKey(LanguageConst.COMMON_ERROR)
                }
        }
    }

    /** Second leg of the add-POI flow: re-fetches and caches the timeline so the
     *  timeline screen applies it on return without a second GET, then signals success. */
    private fun refreshTimelineAfterSegment(poi: Poi, selectedDate: Date) {
        viewModelScope.launch {
            runCatching { fetchTimelineUseCase(FetchTimelineUseCase.Params(tripHash = tripHash)) }
                .onSuccess { timeline ->
                    timelineRepository.cacheGeneratedTimeline(tripHash, timeline)
                    _addedToItinerarySuccess.value = AddedToItineraryResult(
                        poiName = poi.name.orEmpty(),
                        selectedDate = selectedDate
                    )
                }
                .onFailure { t ->
                    val msg = (t as? ErrorModel)?.errorDesc ?: t.message
                    _addSegmentError.value = msg ?: getLanguageForKey(LanguageConst.COMMON_ERROR)
                }
        }
    }

    fun getSelectedDate(): Date? = planData?.selectedDay

    fun minSelectableTimeForSelectedDay(): String? {
        val day = planData?.selectedDay ?: return null
        return CityTimeZones.minSelectableTimeRounded(day, planData?.selectedCity)
    }

    fun defaultStartTimeForSelectedDay(): String? {
        val day = planData?.selectedDay ?: return null
        return CityTimeZones.defaultStartTime(day, planData?.selectedCity)
    }

    fun getAvailableDays(): List<Date> = planData?.availableDays ?: emptyList()

    fun getListingType(): POIListingType = listingType

    fun getSelectedDayIndex(): Int = selectedDayIndex

}

/**
 * POI Listing Type
 */
enum class POIListingType {
    PLACES_OF_INTEREST,
    EAT_AND_DRINK
}
