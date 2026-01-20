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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * ACPOISelectionVM
 * POI seçim ekranının ViewModel'i
 */
class ACPOISelectionVM @Inject constructor(
    private val poiRepository: PoiRepository
) : BaseViewModel() {

    // =====================
    // LIVEDATA
    // =====================

    private val _pois = MutableLiveData<List<Poi>>()
    val pois: LiveData<List<Poi>> = _pois

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _categories = MutableLiveData<List<POICategory>>()
    val categories: LiveData<List<POICategory>> = _categories

    private val _selectedCategory = MutableLiveData<String?>()
    val selectedCategory: LiveData<String?> = _selectedCategory

    // =====================
    // STATE
    // =====================

    private var city: City? = null
    private var currentSearchQuery: String = ""
    private val disposables = CompositeDisposable()

    // =====================
    // INITIALIZATION
    // =====================

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

    // =====================
    // PUBLIC METHODS
    // =====================

    fun setCity(city: City) {
        this.city = city
    }

    fun loadPois() {
        val cityId = city?.id ?: return
        _isLoading.value = true

        // Get selected category IDs
        val categoryIds = _selectedCategory.value?.let { listOf(it.toIntOrNull() ?: -1) }

        val observable = if (currentSearchQuery.isNotBlank()) {
            poiRepository.search(cityId, currentSearchQuery, categoryIds)
        } else {
            poiRepository.getPoiWithCategories(cityId, categoryIds ?: listOf(-1), 1, 50)
        }

        disposables.add(
            observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { response ->
                        _isLoading.value = false
                        _pois.value = response.data ?: emptyList()
                    },
                    { error ->
                        _isLoading.value = false
                        showAlert(AlertType.ERROR, error.message ?: getLanguageForKey(LanguageConst.COMMON_ERROR))
                        _pois.value = emptyList()
                    }
                )
        )
    }

    fun search(query: String) {
        currentSearchQuery = query
        loadPois()
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategory.value = categoryId
        // Update category selection state
        val updatedCategories = _categories.value?.map {
            it.copy(isSelected = it.id == categoryId)
        }
        _categories.value = updatedCategories
        loadPois()
    }

    // =====================
    // LIFECYCLE
    // =====================

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }
}
