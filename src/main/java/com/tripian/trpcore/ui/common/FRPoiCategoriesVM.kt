package com.tripian.trpcore.ui.common

import android.os.Build
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.pois.model.PoiCategory
import com.tripian.one.api.pois.model.PoiCategoryGroup
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetPoiCategories
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import javax.inject.Inject

class FRPoiCategoriesVM @Inject constructor(
    private val getPoiCategories: GetPoiCategories
) : BaseViewModel() {

    private var poiCategories: List<PoiCategoryGroup>? = null

    var selectedCategories: List<PoiCategory> = listOf()

    var onPoiCategoriesListener = MutableLiveData<List<PoiCategoryGroup>>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)
        getPoiCategories()

        selectedCategories = if (arguments?.containsKey("selectedCategories") == true) {
            val companionsSerializable =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arguments?.getSerializable("selectedCategories", ArrayList::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    arguments?.getSerializable("selectedCategories")
                }
            if (companionsSerializable is List<*>) {
                companionsSerializable.filterIsInstance<PoiCategory>()
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

    }

    fun getAllPoiCategories(): List<PoiCategory> {
        return poiCategories?.flatMap { it.categories.orEmpty() }.orEmpty()
    }

    private fun getPoiCategories() {
        showLoading()
        getPoiCategories.on(success = {
            hideLoading()
            poiCategories = it.groups
            onPoiCategoriesListener.postValue(poiCategories ?: listOf())
        },
            error = {
                hideLoading()
                showAlert(AlertType.ERROR, it.errorDesc)
            })
    }

    fun onClickedBack() {
        goBack()
    }
}
