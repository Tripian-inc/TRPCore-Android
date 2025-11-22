package com.tripian.trpcore.ui.trip.favorite

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.FavoriteListener
import com.tripian.trpcore.domain.GetFavoritePlace
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.ui.trip_detail.ACTripDetail
import com.tripian.trpcore.ui.trip_detail.TripDetail
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

class ACFavoriteVM @Inject constructor(val getFavoritePlace: GetFavoritePlace, val favoriteListener: FavoriteListener) : BaseViewModel(getFavoritePlace, favoriteListener) {

    val onSetPlaceListener = MutableLiveData<List<PlaceItem>>()
    private var cityId: Int = 0

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        cityId = arguments!!.getInt("cityId")

        updateFavorites()

        favoriteListener.on(success = {
            updateFavorites()
        })
    }

    private fun updateFavorites() {
        showLoading()

        getFavoritePlace.on(GetFavoritePlace.Params(cityId), success = {
            onSetPlaceListener.postValue(it)

            hideLoading()
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onLocationRouteEvent(item: EventMessage<MapStep>) {
        if (item.tag == EventConstants.LocationRedirect) {
            finishActivity()
        }
    }

    fun onClickedBack() {
        goBack()
    }

    fun onClickedPlace(place: PlaceItem) {
        startActivity(ACTripDetail::class, bundleOf(Pair("detail", TripDetail().apply {
            poiId = place.id
            stepId = place.stepId
        })))
    }
}
