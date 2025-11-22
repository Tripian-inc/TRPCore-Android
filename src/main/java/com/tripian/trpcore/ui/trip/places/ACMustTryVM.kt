package com.tripian.trpcore.ui.trip.places

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetMustTries
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.ui.trip_detail.ACTripDetail
import com.tripian.trpcore.ui.trip_detail.TripDetail
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import com.tripian.one.api.pois.model.Taste
import javax.inject.Inject

class ACMustTryVM @Inject constructor(val getMustTries: GetMustTries) : BaseViewModel(getMustTries) {

    var onSetTasteListener = MutableLiveData<Taste>()
    var onSetPoiListListener = MutableLiveData<List<PlaceItem>>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        val taste = arguments!!.getSerializable("taste") as Taste

        onSetTasteListener.postValue(taste)

        showLoading()

        getMustTries.on(GetMustTries.Params(taste.id), success = {
            onSetPoiListListener.postValue(it)

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

    fun onClickedPlace(place: PlaceItem) {
        startActivity(
            ACTripDetail::class, bundleOf(Pair("detail", TripDetail().apply {
                poiId = place.id
                stepId = place.stepId
            }))
        )
    }
}
