package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.FetchPlace
import com.tripian.trpcore.domain.SearchAddress
import com.tripian.trpcore.domain.model.PlaceAutocomplete
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import com.tripian.one.api.cities.model.City
import javax.inject.Inject

class ACSearchAddressVM @Inject constructor(val searchAddress: SearchAddress, val fetchPlace: FetchPlace) : BaseViewModel(searchAddress, fetchPlace) {

    var city: City? = null

    var onSetPlacesListener = MutableLiveData<List<PlaceAutocomplete>>()
    var onShowProgressListener = MutableLiveData<Unit>()
    var onHideProgressListener = MutableLiveData<Unit>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        city = arguments!!.getSerializable("city") as City
    }

    fun onClickedBack() {
        goBack()
    }

    fun find(search: String) {
        onShowProgressListener.postValue(Unit)

        searchAddress.on(SearchAddress.Params(city!!, search), success = {
            onSetPlacesListener.postValue(it)

            onHideProgressListener.postValue(Unit)
        }, error = {
            onHideProgressListener.postValue(Unit)
        })
    }

    fun onClickedItem(place: PlaceAutocomplete) {
        showLoading()

        fetchPlace.on(FetchPlace.Params(place), success = {
            hideLoading()

            if (it != null) {
                eventBus.post(EventMessage(EventConstants.GooglePlace, it))

                goBack()
            }
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })
    }
}
