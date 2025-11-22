package com.tripian.trpcore.ui.trip.places

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.pois.model.PoiCategory
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetPlaceAlternative
import com.tripian.trpcore.domain.GetPlaceWithCategory
import com.tripian.trpcore.domain.GetPlacesInTrip
import com.tripian.trpcore.domain.SearchPlace
import com.tripian.trpcore.domain.TripListener
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.ui.common.FRPoiCategories
import com.tripian.trpcore.ui.trip_detail.ACTripDetail
import com.tripian.trpcore.ui.trip_detail.TripDetail
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.showLoading
import javax.inject.Inject

class ACPlacesVM @Inject constructor(
    val tripModelRepository: TripModelRepository,
    val getPlacesInTrip: GetPlacesInTrip,
    val getPlaceWithCategory: GetPlaceWithCategory,
    val getPlaceAlternative: GetPlaceAlternative,
    val tripListener: TripListener,
    val searchPlace: SearchPlace
) : BaseViewModel(
    getPlacesInTrip,
    getPlaceWithCategory,
    getPlaceAlternative,
    tripListener,
    searchPlace
) {

    @Inject
    lateinit var pageData: PageData

    val onSetSearchPlaceListener = MutableLiveData<List<PlaceItem>>()
    val onSetPlaceListener = MutableLiveData<Pair<Boolean, List<PlaceItem>>>()

    var onSetCategoriesListener = MutableLiveData<String>()
    var onShowProgressListener = MutableLiveData<Unit>()
    var onHideProgressListener = MutableLiveData<Unit>()
    var onShowErrorListener = MutableLiveData<Boolean>()
    var onShowListErrorListener = MutableLiveData<Boolean>()

    private var selectedCategories: List<PoiCategory> = listOf()

    private val places = ArrayList<PlaceItem>()

    private var isSearchEnable = false
    private var isFilterEnable = false

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        pageData.tripHash = arguments!!.getString("tripHash")!!

        pageData.trip = tripModelRepository.trip

        tripListener.on(success = {
            places.clear()
        })
        onClickedCategories()
    }

    fun onClickedBack() {
        goBack()
    }

    private fun update() {
        showLoading()

        getPlacesInTrip.on(GetPlacesInTrip.Params(getSelectedCategoryIds()), success = {
            places.clear()
            places.addAll(it)

            onSetPlaceListener.value = Pair(true, places)

            getPlaceAlternative.on(GetPlaceAlternative.Params(getSelectedCategoryIds()), success = {
                it.forEach { i -> if (!places.any { p -> p.id == i.id }) places.add(i) }

                onSetPlaceListener.value = Pair(true, places)
            })

            getPlaces(true)
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })
    }

    private fun getPlaces(isClear: Boolean) {
        showLoading()

        getPlaceWithCategory.on(
            GetPlaceWithCategory.Params(pageData.trip?.city?.id, getSelectedCategoryIds()),
            success = {
                it.forEach { i -> if (!places.any { p -> p.id == i.id }) places.add(i) }

                onSetPlaceListener.value = Pair(isClear, places)

                hideLoading()
            },
            error = {
                onSetPlaceListener.postValue(Pair(isClear, places))

                hideLoading()
            })
    }

    fun onClickedCategories() {

        navigateToFragment(
            fragment = FRPoiCategories.newInstance(
                selectedItemIds = if (isFilterEnable) selectedCategories else emptyList(),
                onSelectItemIds = {
                    selectedCategories = it
                    isFilterEnable = true
                    onSetCategoriesListener.postValue(getSelectedCategoryNames())
                    update()
                },
                onAllItemIds = {
                    selectedCategories = it
                    isFilterEnable = false
                    onSetCategoriesListener.postValue("")
                    update()
                },
                onClosed = {
                    checkSelectedCategories()
                }),
            addToBackStack = false
        )
    }

    private fun checkSelectedCategories() {
        onShowErrorListener.postValue(selectedCategories.isEmpty())
    }

    private fun getSelectedCategoryNames(): String {

        return if (isFilterEnable.not())
            ""
        else
            selectedCategories.joinToString(", ") { it.name.toString() }
    }

    private fun getSelectedCategoryIds(): List<Int> {
        return selectedCategories.map { it.id }
    }

    fun onSearchEntered(search: String) {
        search(search)
    }

    fun onClickedPlace(place: PlaceItem) {
        startActivity(
            ACTripDetail::class, bundleOf(Pair("detail", TripDetail().apply {
                poiId = place.id
                stepId = place.stepId
            }))
        )
    }

    fun isLoading(): Boolean {
        return getPlaceWithCategory.isLoading
    }

    fun isLastPage(): Boolean {
        return getPlaceWithCategory.isLastPage
    }

    fun loadMoreItems() {
        if (!isSearchEnable) {
            getPlaces(isClear = false)
        }
    }

    fun search(search: String) {
        if (search.length > 2) {
            isSearchEnable = true

            onShowProgressListener.postValue(Unit)

            searchPlace.on(
                SearchPlace.Params(
                    pageData.trip!!.city!!.id,
                    search,
                    place = null,
                    categoryIds = getSelectedCategoryIds()
                ),
                success = {
                    onSetSearchPlaceListener.postValue(it)
                    onShowListErrorListener.postValue(it.isEmpty())

                    onHideProgressListener.postValue(Unit)
                },
                error = {
                    onHideProgressListener.postValue(Unit)
                })
        } else {
            isSearchEnable = false

            onSetPlaceListener.postValue(Pair(true, places))

            onHideProgressListener.postValue(Unit)
        }
    }
}
