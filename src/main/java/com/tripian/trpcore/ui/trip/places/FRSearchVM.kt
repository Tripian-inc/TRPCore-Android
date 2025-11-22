package com.tripian.trpcore.ui.trip.places

import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.SearchPlace
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.ui.trip_detail.ACTripDetail
import com.tripian.trpcore.ui.trip_detail.TripDetail
import javax.inject.Inject

class FRSearchVM @Inject constructor(val searchPlace: SearchPlace) : BaseViewModel(searchPlace) {

    @Inject
    lateinit var pageData: PageData

    val onSetPlaceListener = MutableLiveData<List<PlaceItem>>()
    var onShowProgressListener = MutableLiveData<Unit>()
    var onHideProgressListener = MutableLiveData<Unit>()
    var onShowErrorListener = MutableLiveData<Unit>()
    var onHideErrorListener = MutableLiveData<Unit>()

    private val places = ArrayList<PlaceItem>()

    fun onSearchEntered(search: String) {
        if (search.length > 2) {
            places.clear()

            onShowProgressListener.postValue(Unit)

            searchPlace.on(SearchPlace.Params(pageData.trip!!.city!!.id, search, place = null), success = {
                places.addAll(it)

                onSetPlaceListener.postValue(places)

                if (places.isNullOrEmpty()) {
                    onShowErrorListener.postValue(Unit)
                } else {
                    onHideErrorListener.postValue(Unit)
                }

                onHideProgressListener.postValue(Unit)
            }, error = {
                onHideProgressListener.postValue(Unit)
            })
        } else {
            places.clear()

            onSetPlaceListener.postValue(places)

            onHideProgressListener.postValue(Unit)
        }
    }

    fun isLastPage(): Boolean {
        return searchPlace.isLastPage
    }

    fun loadMoreItems() {
        onShowProgressListener.postValue(Unit)

        searchPlace.on(SearchPlace.Params(cityId = null, search = null, place = null), success = {
            places.addAll(it)

            onSetPlaceListener.postValue(places)

            onHideProgressListener.postValue(Unit)
        }, error = {
            onHideProgressListener.postValue(Unit)
        })
    }

    fun isLoading(): Boolean {
        return searchPlace.isLoading
    }

    fun onClickedPlace(place: PlaceItem) {
        startActivity(ACTripDetail::class, bundleOf(Pair("detail", TripDetail().apply {
            poiId = place.id
            stepId = place.stepId
        })))
    }
}
