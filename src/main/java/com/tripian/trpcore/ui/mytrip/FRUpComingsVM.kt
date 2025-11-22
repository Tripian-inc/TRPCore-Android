package com.tripian.trpcore.ui.mytrip

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.trip.model.Trip
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.DeleteTrip
import com.tripian.trpcore.domain.GetUserTrip
import com.tripian.trpcore.repository.UserReactionRepository
import com.tripian.trpcore.ui.createtrip.ACCreateTrip
import com.tripian.trpcore.ui.trip.ACTripMode
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.showLoading
import javax.inject.Inject

class FRUpComingsVM @Inject constructor(val getUserTrip: GetUserTrip,
                                        val deleteTrip: DeleteTrip,
                                        val userReactionRepository: UserReactionRepository) :
    BaseViewModel() {

    var onSetTripsListener = MutableLiveData<List<Trip>>()
    var onShowProgressListener = MutableLiveData<Unit>()
    var onHideProgressListener = MutableLiveData<Unit>()

    var onShowErrorListener = MutableLiveData<Unit>()
    var onHideErrorListener = MutableLiveData<Unit>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        update(true)
    }

    fun onClickedMore(trip: Trip) {
        navigateToFragment(fragment = FRMoreSelection.newInstance(trip))
    }

    fun onRefresh() {
        update()
    }

    private fun update(useCache: Boolean = false) {
        onShowProgressListener.postValue(Unit)

        getUserTrip.on(GetUserTrip.Params(isUpComing = true, useCache = useCache), success = {
            if (it.data != null && it.data!!.isNotEmpty()) {
                onHideErrorListener.postValue(Unit)
                onSetTripsListener.postValue(it.data)
            } else {
                onShowErrorListener.postValue(Unit)
            }

            onHideProgressListener.postValue(Unit)
        }, error = {
            onHideProgressListener.postValue(Unit)
            onShowErrorListener.postValue(Unit)

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })
    }

    fun onClickedItem(trip: Trip) {
        userReactionRepository.reactions = null

        startActivity(ACTripMode::class, bundleOf(Pair("trip", trip)))
    }

    fun onClickedEdit(trip: Trip) {
        startActivity(ACCreateTrip::class, bundleOf(Pair("trip", trip)))
    }

    fun onClickedDelete(trip: Trip) {
        showDialog(
            contentText = getLanguageForKey(LanguageConst.DELETE_TRIP_QUESTION),
            positiveBtn = getLanguageForKey(LanguageConst.DELETE),
            negativeBtn = getLanguageForKey(LanguageConst.CANCEL),
            positive = object : DGActionListener {
                override fun onClicked(o: Any?) {
                    showLoading()
                    deleteTrip.on(DeleteTrip.Params(trip.tripHash!!), success = {
                        hideLoading()
                        update()
                    }, error = {
                        hideLoading()
                        if (it.type == AlertType.DIALOG) {
                            showDialog(contentText = it.errorDesc)
                        } else {
                            showAlert(it.type, it.errorDesc)
                        }
                    })
                }
            })
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public fun onUpdateEvent(item: EventMessage<Unit>) {
//        if (item.tag == EventConstants.UpdateTrip) {
//            update()
//        }
//    }

    fun onClickedCreate() {
        startActivity(ACCreateTrip::class)
    }
}
