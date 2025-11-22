package com.tripian.trpcore.ui.overview

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.FetchButterflyTrip
import com.tripian.trpcore.domain.GetOverViewItems
import com.tripian.trpcore.domain.model.Butterfly
import com.tripian.trpcore.domain.model.ButterflyItem
import com.tripian.trpcore.ui.trip.ACTripMode
import com.tripian.trpcore.ui.trip_detail.ACTripDetail
import com.tripian.trpcore.ui.trip_detail.TripDetail
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import com.tripian.one.api.trip.model.Trip
import javax.inject.Inject

class ACOverViewVM @Inject constructor(
    val fetchTrip: FetchButterflyTrip, val getOverViewItems: GetOverViewItems
) : BaseViewModel(fetchTrip, getOverViewItems) {

    var onSetTabItemListener = MutableLiveData<List<Butterfly>>()
    var onSetItemsListener = MutableLiveData<List<ButterflyItem>>()
    var onProgressListener = MutableLiveData<Boolean>()

    private var items: List<Butterfly>? = null
    private var trip: Trip? = null

    private var selectedTab = 0

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        trip = arguments!!.getSerializable("trip") as Trip

        showLoading()

        fetchTrip.on(FetchButterflyTrip.Params(trip!!.tripHash!!), success = {
            trip = it.data

            getOverViewItems.on(GetOverViewItems.Params(trip!!.tripHash!!), success = { res ->
                items = res.items

                items?.let {
                    onSetTabItemListener.postValue(it)

                    if (it.size > selectedTab) {
                        onSetItemsListener.postValue(it[selectedTab].items)
                    }
                }

                onProgressListener.postValue(res.generated)

                hideLoading()
            }, error = {
                hideLoading()

                if (it.type == AlertType.DIALOG) {
                    showDialog(contentText = it.errorDesc)
                } else {
                    showAlert(AlertType.ERROR, it.errorDesc)
                }
            })
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })
    }

    fun onClickedClose() {
        startActivity(ACTripMode::class, bundleOf(Pair("trip", trip)))

        finishActivity()
    }

    fun onClickedItem(item: ButterflyItem) {
        startActivity(ACTripDetail::class, bundleOf(Pair("detail", TripDetail().apply {
            poiId = item.step!!.poi!!.id!!
            butterflyMode = true
        })))
    }

    fun onTabSelected(position: Int) {
        selectedTab = position

        if (items != null && items!!.size > selectedTab) {
            onSetItemsListener.postValue(items!![selectedTab].items)
        }
    }
}
