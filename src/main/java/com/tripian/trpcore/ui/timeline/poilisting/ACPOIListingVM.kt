package com.tripian.trpcore.ui.timeline.poilisting

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.pois.model.PoiCategoryGroup
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetPoiCategories
import com.tripian.trpcore.domain.manager.POICategoryManager
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.FilterData
import com.tripian.trpcore.domain.model.timeline.ManualCategory
import com.tripian.trpcore.domain.model.timeline.SortOption
import com.tripian.trpcore.domain.usecase.timeline.CreateManualPoiSegmentUseCase
import com.tripian.trpcore.domain.usecase.timeline.SearchPOIsUseCase
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.base.ResponseModelBase
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
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
    private val getPoiCategoriesUseCase: GetPoiCategories,
    private val poiRepository: PoiRepository
) : BaseViewModel() {

    // =====================
    // LIVEDATA
    // =====================

    private val _pois = MutableLiveData<List<Poi>>()
    val pois: LiveData<List<Poi>> = _pois

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSearching = MutableLiveData<Boolean>()
    val isSearching: LiveData<Boolean> = _isSearching

    private val _poiCount = MutableLiveData<Int>()
    val poiCount: LiveData<Int> = _poiCount

    private val _hasMorePages = MutableLiveData<Boolean>()
    val hasMorePages: LiveData<Boolean> = _hasMorePages

    private val _showTimeSelection = MutableLiveData<Poi?>()
    val showTimeSelection: LiveData<Poi?> = _showTimeSelection

    private val _segmentCreated = MutableLiveData<Boolean>()
    val segmentCreated: LiveData<Boolean> = _segmentCreated

    private val _isCreatingSegment = MutableLiveData<Boolean>()
    val isCreatingSegment: LiveData<Boolean> = _isCreatingSegment

    // Filter & Sort state
    private val _currentFilter = MutableLiveData(FilterData())
    val currentFilter: LiveData<FilterData> = _currentFilter

    private val _currentSort = MutableLiveData(SortOption.DEFAULT)
    val currentSort: LiveData<SortOption> = _currentSort

    // Category groups for filter
    private val _categoryGroups = MutableLiveData<List<PoiCategoryGroup>>()
    val categoryGroups: LiveData<List<PoiCategoryGroup>> = _categoryGroups

    // Scroll to top signal (triggered when page 1 is loaded)
    private val _scrollToTop = MutableLiveData<Boolean>()
    val scrollToTop: LiveData<Boolean> = _scrollToTop

    // =====================
    // STATE
    // =====================

    private var planData: AddPlanData? = null
    private var tripHash: String = ""
    private var cityId: Int = 0
    private var listingType: POIListingType = POIListingType.PLACES_OF_INTEREST
    private var currentSearchQuery: String = ""
    private var currentPage: Int = 1
    private val pageLimit: Int = 30
    private var allPois: MutableList<Poi> = mutableListOf()
    private var isLoadingMore: Boolean = false
    private var totalCount: Int = 0

    private var searchHandler: Handler? = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val searchDebounceMs = 500L

    // =====================
    // INITIALIZATION
    // =====================

    fun initialize(planData: AddPlanData, tripHash: String, listingType: POIListingType) {
        this.planData = planData
        this.tripHash = tripHash
        this.cityId = planData.selectedCity?.id ?: 0
        this.listingType = listingType

        // Fetch categories for filter UI
        fetchCategories()

        // Prefetch categories for POICategoryManager (for listing type filtering)
        POICategoryManager.prefetchIfNeeded(poiRepository) {
            // Initial load after categories are ready
            loadPOIs()
        }
    }

    // =====================
    // FETCH CATEGORIES
    // =====================

    /**
     * Category IDs that identify "Eat & Drink" groups
     * Must match POICategoryManager.EAT_AND_DRINK_CATEGORY_IDS
     */
    private val EAT_AND_DRINK_CATEGORY_IDS = listOf(3, 4, 24)

    private fun fetchCategories() {
        getPoiCategoriesUseCase.on(
            params = Unit,
            success = { categoryModel ->
                val allGroups = categoryModel?.groups ?: emptyList()

                // Filter groups based on listing type
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
            },
            error = { _ ->
                _categoryGroups.value = emptyList()
            }
        )
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
        isLoadingMore = false  // Ensure fresh load, not pagination
        loadPOIs()
    }

    // =====================
    // LOAD POIs
    // =====================

    fun loadPOIs() {
        if (cityId <= 0) return

        // isLoadingMore = true means pagination, false means fresh load
        val isPagination = isLoadingMore
        if (!isPagination) {
            _isLoading.value = true
            currentPage = 1
        }
        _isSearching.value = currentSearchQuery.isNotEmpty()

        // Get current filter and sort options
        val filter = _currentFilter.value ?: FilterData()
        val sort = _currentSort.value ?: SortOption.DEFAULT

        // 1. First get category IDs based on listing type
        val listingTypeCategoryIds = POICategoryManager.getCategoryIds(listingType) ?: emptyList()

        // 2. If user selected categories from filter, intersect with listing type categories
        val categoryIds = if (filter.selectedCategoryIds.isNotEmpty()) {
            // Intersection: only categories that are both in listing type AND user selection
            listingTypeCategoryIds.filter { it in filter.selectedCategoryIds }
        } else {
            // No filter selection, use all listing type categories
            listingTypeCategoryIds
        }.takeIf { it.isNotEmpty() }

        // Calculate page to fetch
        val pageToFetch = if (isPagination) currentPage + 1 else 1

        searchPOIsUseCase.on(
            params = SearchPOIsUseCase.Params(
                cityId = cityId,
                search = if (currentSearchQuery.isNotBlank()) currentSearchQuery else null,
                categoryIds = categoryIds,
                page = pageToFetch,
                limit = pageLimit,
                sortingBy = sort.sortingBy,
                sortingType = sort.sortingType
            ),
            success = { response ->
                _isLoading.value = false
                _isSearching.value = false
                isLoadingMore = false

                val newPois = response.data ?: emptyList()
                totalCount = response.pagination?.total ?: newPois.size

                if (!isPagination) {
                    allPois.clear()
                    // Scroll to top when page 1 is loaded (filter, sort, or search change)
                    _scrollToTop.value = true
                }
                allPois.addAll(newPois)

                // Update current page after successful fetch
                currentPage = pageToFetch

                _pois.value = allPois.toList()
                _poiCount.value = totalCount
                _hasMorePages.value = allPois.size < totalCount
            },
            error = { error ->
                _isLoading.value = false
                _isSearching.value = false
                isLoadingMore = false
                showAlert(AlertType.ERROR, error.errorDesc ?: getLanguageForKey(LanguageConst.COMMON_ERROR))
                if (!isPagination) {
                    _pois.value = emptyList()
                    _poiCount.value = 0
                }
            }
        )
    }

    fun loadMorePOIs() {
        if (_hasMorePages.value == true && !isLoadingMore && _isLoading.value != true) {
            isLoadingMore = true
            loadPOIs()
        }
    }

    // =====================
    // TIME SELECTION
    // =====================

    fun onPOIAddClicked(poi: Poi) {
        _showTimeSelection.value = poi
    }

    fun clearTimeSelection() {
        _showTimeSelection.value = null
    }

    // =====================
    // FILTER & SORT
    // =====================

    /**
     * Apply filter and reload POIs
     */
    fun applyFilter(filter: FilterData) {
        _currentFilter.value = filter
        resetAndSearch()
    }

    /**
     * Apply sort option and reload POIs
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

    // =====================
    // CREATE SEGMENT
    // =====================

    fun createManualPoiSegment(poi: Poi, selectedDate: Date, startTime: String, endTime: String) {
        _isCreatingSegment.value = true

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(selectedDate)

        createManualPoiSegmentUseCase.on(
            params = CreateManualPoiSegmentUseCase.Params(
                tripHash = tripHash,
                poi = poi,
                selectedDate = dateString,
                startTime = startTime,
                endTime = endTime,
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

    fun getSelectedDate(): Date? = planData?.selectedDay

    fun getAvailableDays(): List<Date> = planData?.availableDays ?: emptyList()

    fun getListingType(): POIListingType = listingType

    // =====================
    // CLEANUP
    // =====================

    override fun onDestroy() {
        searchRunnable?.let { searchHandler?.removeCallbacks(it) }
        searchHandler = null
        super.onDestroy()
    }
}

/**
 * POI Listing Type
 */
enum class POIListingType {
    PLACES_OF_INTEREST,
    EAT_AND_DRINK
}
