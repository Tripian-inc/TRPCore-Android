package com.tripian.trpcore.ui.trip.places

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetPlaceAlternative
import com.tripian.trpcore.domain.GetPlaceWithCategory
import com.tripian.trpcore.domain.GetPlacesInTrip
import com.tripian.trpcore.domain.SearchPlace
import com.tripian.trpcore.domain.TripListener
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.ui.trip_detail.ACTripDetail
import com.tripian.trpcore.ui.trip_detail.TripDetail
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.showLoading
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

class FRPlacesVM @Inject constructor(
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

    var onShowProgressListener = MutableLiveData<Unit>()
    var onHideProgressListener = MutableLiveData<Unit>()
    var onShowErrorListener = MutableLiveData<Unit>()
    var onHideErrorListener = MutableLiveData<Unit>()

    private lateinit var place: Place

    private val places = ArrayList<PlaceItem>()

    private var isSearchEnable = false

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        place = arguments!!.getSerializable("place") as Place

        tripListener.on(success = {
            places.clear()

            update()
        })

        update()
    }

    private fun update() {
        showLoading()
    }

    private fun getPlaces(isClear: Boolean) {
        showLoading()
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

    fun onClickedPlace(place: PlaceItem) {
        startActivity(
            ACTripDetail::class, bundleOf(Pair("detail", TripDetail().apply {
                poiId = place.id
                stepId = place.stepId
            }))
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onSearchEvent(item: EventMessage<Pair<String, Place>>) {
        if (item.tag == EventConstants.SearchPlace && item.value != null && item.value!!.second == place) {
            search(item.value!!.first)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onSearchCancelEvent(item: EventMessage<Int>) {
        if (item.tag == EventConstants.SearchPlaceCancel) {
            onSetPlaceListener.postValue(Pair(false, places))

            isSearchEnable = false
            onHideProgressListener.postValue(Unit)
        }
    }

    fun search(search: String) {
        if (search.length > 2) {
            isSearchEnable = true

            onShowProgressListener.postValue(Unit)

            searchPlace.on(
                SearchPlace.Params(pageData.trip!!.city!!.id, search, place = place),
                success = {
                    onSetSearchPlaceListener.postValue(it)

                    if (it.isNullOrEmpty()) {
                        onShowErrorListener.postValue(Unit)
                    } else {
                        onHideErrorListener.postValue(Unit)
                    }

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