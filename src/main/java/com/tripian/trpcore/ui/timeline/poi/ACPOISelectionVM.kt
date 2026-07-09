package com.tripian.trpcore.ui.timeline.poi

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ACPOISelectionVM
 * ViewModel for POI selection screen
 */
class ACPOISelectionVM @Inject constructor(
    private val poiRepository: PoiRepository
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
        fetchPois(useFullScreen = true)
    }

    fun search(query: String) {
        currentSearchQuery = query
        fetchPois(useFullScreen = false)
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategory.value = categoryId
        val updatedCategories = _categories.value?.map {
            it.copy(isSelected = it.id == categoryId)
        }
        _categories.value = updatedCategories
        fetchPois(useFullScreen = false)
    }

    /**
     * @param useFullScreen `true` for the first fetch of the screen (full-screen
     *   Lottie), `false` for search / category / any subsequent fetch
     *   (bottom-sheet Lottie — keeps the user's filters and the list in view).
     */
    private fun fetchPois(useFullScreen: Boolean) {
        val cityId = city?.id ?: return
        _isLoading.value = true

        if (useFullScreen) showFullScreenLoaderNoText() else showBottomSheetLoaderNoText()

        val categoryIds = _selectedCategory.value?.let { listOf(it.toIntOrNull() ?: -1) }

        viewModelScope.launch {
            runCatching {
                if (currentSearchQuery.isNotBlank()) {
                    poiRepository.searchAsync(cityId, currentSearchQuery, categoryIds)
                } else {
                    poiRepository.getPoiWithCategoriesAsync(cityId, categoryIds ?: listOf(-1), 1, 50)
                }
            }.onSuccess { response ->
                _isLoading.value = false
                hideLottieLoading()
                _pois.value = response.data ?: emptyList()
            }.onFailure { error ->
                _isLoading.value = false
                hideLottieLoading()
                showAlert(AlertType.ERROR, error.message ?: getLanguageForKey(LanguageConst.COMMON_ERROR))
                _pois.value = emptyList()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
