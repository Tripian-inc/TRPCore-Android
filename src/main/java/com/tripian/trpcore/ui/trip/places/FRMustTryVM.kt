package com.tripian.trpcore.ui.trip.places

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetCity
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import com.tripian.one.api.pois.model.Taste
import javax.inject.Inject

class FRMustTryVM @Inject constructor(val getCity: GetCity) : BaseViewModel(getCity) {

    val onSetTasteListener = MutableLiveData<List<Taste>>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        showLoading()

        getCity.on(success = {
            onSetTasteListener.postValue(it.data?.mustTries)
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })
    }

    fun onClickedTaste(taste: Taste) {
        startActivity(ACMustTry::class, bundleOf(Pair("taste", taste)))
    }
}
