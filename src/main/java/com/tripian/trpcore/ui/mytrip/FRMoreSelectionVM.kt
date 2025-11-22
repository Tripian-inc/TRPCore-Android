package com.tripian.trpcore.ui.mytrip

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.trip.model.Trip
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.DeleteTrip
import com.tripian.trpcore.ui.createtrip.ACCreateTrip
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.formatDate
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import javax.inject.Inject

class FRMoreSelectionVM @Inject constructor(val deleteTrip: DeleteTrip) :
    BaseViewModel(deleteTrip) {

    var onSetTitleListener = MutableLiveData<Pair<String, String>>()
    var onDismissListener = MutableLiveData<Unit>()
    var onEditDisableListener = MutableLiveData<Unit>()

    var trip: Trip? = null

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        trip = arguments!!["trip"] as Trip

        if (!arguments!!.getBoolean("editEnable")) {
            onEditDisableListener.postValue(Unit)
        }

        onSetTitleListener.postValue(
            Pair(
                trip!!.city?.name ?: "",
                "${trip!!.tripProfile?.arrivalDatetime?.formatDate()} - ${trip!!.tripProfile?.departureDatetime?.formatDate()}"
            )
        )
    }

    fun onClickedEdit() {
        startActivity(ACCreateTrip::class, bundleOf(Pair("trip", trip)))

        onDismissListener.postValue(Unit)
    }

    fun onClickedDelete() {
        showDialog(
            contentText = getLanguageForKey(LanguageConst.DELETE_TRIP_QUESTION),
            positiveBtn = getLanguageForKey(LanguageConst.DELETE),
            negativeBtn = getLanguageForKey(LanguageConst.CANCEL),
            positive = object : DGActionListener {
                override fun onClicked(o: Any?) {
                    showLoading()

                    deleteTrip.on(DeleteTrip.Params(trip!!.tripHash!!), success = {
                        hideLoading()

                        eventBus.post(EventMessage(EventConstants.UpdateTrip, Unit))

                        onDismissListener.postValue(Unit)
                    }, error = {
                        hideLoading()

                        if (it.type == AlertType.DIALOG) {
                            showDialog(contentText = it.errorDesc)
                        } else {
                            showAlert(AlertType.ERROR, it.errorDesc)
                        }
                    })
                }
            })
    }
}