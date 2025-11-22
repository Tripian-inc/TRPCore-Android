package com.tripian.trpcore.ui.trip.my_offers

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.offers.model.Offer
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetMyOffers
import com.tripian.trpcore.domain.GetMyOffersListener
import com.tripian.trpcore.domain.RemoveOffer
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.ui.trip_detail.ACTripDetail
import com.tripian.trpcore.ui.trip_detail.TripDetail
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import javax.inject.Inject

class ACMyOffersVM @Inject constructor(
    val getMyOffers: GetMyOffers,
    val removeOffer: RemoveOffer,
    val getMyOffersListener: GetMyOffersListener
) : BaseViewModel(getMyOffers, getMyOffersListener) {

    val onSetPlaceListener = MutableLiveData<List<PlaceItem>>()

    var onSetAdapterListener = MutableLiveData<ArrayList<Offer>>()
    var onNotifyAdapter = MutableLiveData<Int>()
    private var dateFrom: String? = null
    private var dateTo: String? = null

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        dateFrom = arguments!!.getString("dateFrom")
        dateTo = arguments!!.getString("dateTo")

//        updateMyOffers()

        getMyOffersListener.on(success = {
            val offers: ArrayList<Offer> = arrayListOf()
            it.forEach { poi ->
                poi.offers?.let { poiOffers -> offers.addAll(poiOffers) }
            }
            onSetAdapterListener.postValue(offers)
        })

        onRefresh()
    }

    private fun onRefresh() {
        showLoading()

        getMyOffers.on(GetMyOffers.Params(dateFrom, dateTo, isClear = false), success = {

            val offers: ArrayList<Offer> = arrayListOf()
            it.forEach { poi ->
                poi.offers?.let { poiOffers -> offers.addAll(poiOffers) }
            }
            onSetAdapterListener.postValue(offers)
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

    fun removeToOffer(pos: Int, item: Offer) {
        showLoading()

        removeOffer.on(RemoveOffer.Params(item.id), success = {
            onNotifyAdapter.postValue(pos)

            showAlert(AlertType.SUCCESS, getLanguageForKey(LanguageConst.SUCCESS))

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

    fun onOfferClicked(item: Offer) {
        startActivity(ACTripDetail::class, bundleOf(Pair("detail", TripDetail().apply {
            poiId = item.tripianPoiId
        })))
    }

    fun onClickedBack() {
        goBack()
    }
}
