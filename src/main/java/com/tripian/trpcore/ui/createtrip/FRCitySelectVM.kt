package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetCities
import com.tripian.trpcore.domain.SearchCity
import com.tripian.trpcore.domain.model.CitySelect
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.showLoading
import com.tripian.trpcore.util.fragment.AnimationType
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import javax.inject.Inject

class FRCitySelectVM @Inject constructor(val getCities: GetCities, val searchCity: SearchCity) : BaseViewModel(getCities, searchCity) {

    @Inject
    lateinit var pageData: PageData

    var onSetCityListener = MutableLiveData<Pair<Boolean, List<CitySelect>?>>()
    var onDismissListener = MutableLiveData<Unit>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        setCities(isLoadingEnable = true)
    }

    private fun setCities(isLoadingEnable: Boolean = false) {
        if (isLoadingEnable) {
            showLoading()
        }

        getCities.on(success = {
            it.second.let { onSetCityListener.postValue(Pair(false, it)) }

            if (it.first) {
                hideLoading()
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

    fun onSearchEntered(search: String) {
        if (TextUtils.isEmpty(search)) {
            setCities(isLoadingEnable = false)
        } else {
            searchCity.on(SearchCity.Params(search), success = {
                onSetCityListener.postValue(Pair(true, it))
            })
        }
    }

    fun onSelectedCity(city: City) {
        pageData.city = city

        eventBus.post(EventMessage(EventConstants.DestinationCitySelect, Unit))
        onDismissListener.postValue(Unit)
//        navigateToFragment(fragment = FRPropertiesSelect.newInstance(), animation = AnimationType.ENTER_FROM_RIGHT)
    }
}
