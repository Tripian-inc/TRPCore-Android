package com.tripian.trpcore.ui.butterfly

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.AddReaction
import com.tripian.trpcore.domain.DeleteReaction
import com.tripian.trpcore.domain.FetchButterflyTrip
import com.tripian.trpcore.domain.GetButterflyItems
import com.tripian.trpcore.domain.model.Butterfly
import com.tripian.trpcore.domain.model.ButterflyItem
import com.tripian.trpcore.ui.trip.ACTripMode
import com.tripian.trpcore.ui.trip_detail.ACTripDetail
import com.tripian.trpcore.ui.trip_detail.TripDetail
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.ReactionType
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.remove
import com.tripian.trpcore.util.extensions.showLoading
import com.tripian.one.api.trip.model.Trip
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

class ACButterflyVM @Inject constructor(
    val fetchTrip: FetchButterflyTrip, val getButterflies: GetButterflyItems,
    val addReaction: AddReaction, val deleteReaction: DeleteReaction
) : BaseViewModel(fetchTrip, getButterflies, addReaction, deleteReaction) {

    var onSetAdapterListener = MutableLiveData<List<Butterfly>>()
    var onCloseWarningListener = MutableLiveData<Unit>()

    private var items: List<Butterfly>? = null
    private var trip: Trip? = null

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        trip = arguments!!.getSerializable("trip") as Trip

        showLoading()

        fetchTrip.on(FetchButterflyTrip.Params(trip!!.tripHash!!), success = {
            trip = it.data

            getButterflies.on(GetButterflyItems.Params(trip!!.tripHash!!), success = { butterItems ->
                items = butterItems

                onSetAdapterListener.postValue(items)

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

    fun onClickedLike(item: ButterflyItem) {
        addReaction.on(AddReaction.Params(item.step!!.poi!!.id!!, item.step!!.id!!, ReactionType.THUMBS_UP), success = {
            item.reaction = it.data
        })
    }

    fun onClickedDislike(item: ButterflyItem) {
        addReaction.on(AddReaction.Params(item.step!!.poi!!.id!!, item.step!!.id!!, ReactionType.THUMBS_DOWN), success = {
            item.reaction = it.data
        })
    }

    fun onClickedUndo(item: ButterflyItem) {
        item.reaction?.let { deleteReaction.on(DeleteReaction.Params(it.id!!)) }
    }

    fun onClickedTellUs(item: ButterflyItem) {
        navigateToFragment(FRTellUs.newInstance(item))
    }

    fun onClickedItemClose(item: ButterflyItem) {
        items?.forEach { fly ->
            fly.items.remove { it.category == item.category && it.step?.id == item.step?.id }
        }

        onSetAdapterListener.postValue(items)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onThumbsDownReasonSelected(item: EventMessage<Int>) {
        if (item.tag == EventConstants.ButterflyWhy) {
            items?.forEach { category ->
                category.items.remove { it.step?.id == item.value }
            }

            onSetAdapterListener.postValue(items)
        }
    }

    fun onClickedCloseWarning() {
        onCloseWarningListener.postValue(Unit)
    }
}
