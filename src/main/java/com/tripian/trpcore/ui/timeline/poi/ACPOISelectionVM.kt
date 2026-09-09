package com.tripian.trpcore.ui.timeline.poi

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.usecase.timeline.SearchPOIsUseCase
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * ACPOISelectionVM
 * ViewModel for the POI selection screen. Shares the POI listing's search use case:
 * search is debounced, every fresh load (search or category) cancels the in-flight
 * request, and further pages are appended as the list scrolls.
 */
class ACPOISelectionVM @Inject constructor(
    private val searchPOIsUseCase: SearchPOIsUseCase
) : BaseViewModel() {

    private val _pois = MutableLiveData<List<Poi>>()
    val pois: LiveData<List<Poi>> = _pois

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _categories = MutableLiveData<List<POICategory>>()
    val categories: LiveData<List<POICategory>> = _categories

    private val _selectedCategory = MutableLiveData<String?>()
    val selectedCategory: LiveData<String?> = _selectedCategory

    private var city: City? = null
    private var currentSearchQuery: String = ""

    private val loadedPois = mutableListOf<Poi>()
    private var currentPage = 1
    private var hasMorePages = false
    private val _loadingMore = MutableLiveData(false)
    /** True while a further page is being appended; drives the bottom loading indicator. */
    val loadingMore: LiveData<Boolean> = _loadingMore
    private var isLoadingMore: Boolean
        get() = _loadingMore.value == true
        set(value) {
            if (_loadingMore.value != value) _loadingMore.value = value
        }
    private var fetchJob: Job? = null

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)
        initializeCategories()
    }

    private fun initializeCategories() {
        val categoryList = listOf(
            POICategory(id = "1", name = getLanguageForKey(LanguageConst.ATTRACTIONS), isSelected = false),
            POICategory(id = "2", name = getLanguageForKey(LanguageConst.RESTAURANTS), isSelected = false),
            POICategory(id = "3", name = getLanguageForKey(LanguageConst.CAFES), isSelected = false),
            POICategory(id = "4", name = getLanguageForKey(LanguageConst.NIGHTLIFE), isSelected = false),
            POICategory(id = "5", name = getLanguageForKey(LanguageConst.ADD_PLAN_ACTIVITIES), isSelected = false),
            POICategory(id = "6", name = getLanguageForKey(LanguageConst.SEE_DO), isSelected = false)
        )
        _categories.value = categoryList
    }

    fun setCity(city: City) {
        this.city = city
    }

    /** Initial screen load — full-screen Lottie covers the empty list flash. */
    fun loadPois() {
        fetchFirstPage(useFullScreen = true)
    }

    /** Clearing the query reloads immediately; typing is debounced. */
    fun search(query: String) {
        currentSearchQuery = query
        val debounceMs = if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MS
        fetchFirstPage(useFullScreen = false, debounceMs = debounceMs)
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategory.value = categoryId
        val updatedCategories = _categories.value?.map {
            it.copy(isSelected = it.id == categoryId)
        }
        _categories.value = updatedCategories
        fetchFirstPage(useFullScreen = false)
    }

    /** Appends the next page when one is available and nothing else is loading. */
    fun loadMorePois() {
        if (!hasMorePages || isLoadingMore || _isLoading.value == true) return
        isLoadingMore = true
        fetchJob = viewModelScope.launch {
            fetchPage(page = currentPage + 1, isPagination = true)
        }
    }

    /**
     * @param useFullScreen `true` for the first fetch of the screen (full-screen
     *   Lottie), `false` for search / category (bottom-sheet Lottie keeps the
     *   filters and list in view).
     * @param debounceMs Delay before the request; a newer fetch cancels it.
     */
    private fun fetchFirstPage(useFullScreen: Boolean, debounceMs: Long = 0L) {
        if (city == null) return
        fetchJob?.cancel()
        isLoadingMore = false

        fetchJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            _isLoading.value = true
            if (useFullScreen) showFullScreenLoaderNoText() else showBottomSheetLoaderNoText()
            fetchPage(page = 1, isPagination = false)
        }
    }

    private suspend fun fetchPage(page: Int, isPagination: Boolean) {
        val cityId = city?.id ?: return
        val categoryIds = _selectedCategory.value?.toIntOrNull()?.let { listOf(it) }

        val result = runCatching {
            searchPOIsUseCase(
                SearchPOIsUseCase.Params(
                    cityId = cityId,
                    search = currentSearchQuery.trim().ifBlank { null },
                    categoryIds = categoryIds,
                    page = page,
                    limit = PAGE_LIMIT
                )
            )
        }
        coroutineContext.ensureActive()

        isLoadingMore = false
        if (!isPagination) {
            _isLoading.value = false
            hideLottieLoading()
        }

        result
            .onSuccess { response ->
                val newPois = response.data ?: emptyList()
                if (!isPagination) loadedPois.clear()
                loadedPois.addAll(newPois)
                currentPage = page
                val totalCount = response.pagination?.total ?: loadedPois.size
                hasMorePages = newPois.isNotEmpty() && loadedPois.size < totalCount
                _pois.value = loadedPois.toList()
            }
            .onFailure { t ->
                val message = (t as? ErrorModel)?.errorDesc ?: t.message
                showAlert(AlertType.ERROR, message ?: getLanguageForKey(LanguageConst.COMMON_ERROR))
                if (!isPagination) {
                    loadedPois.clear()
                    hasMorePages = false
                    _pois.value = emptyList()
                }
            }
    }

    private companion object {
        const val PAGE_LIMIT = 30
        const val SEARCH_DEBOUNCE_MS = 650L
    }
}
