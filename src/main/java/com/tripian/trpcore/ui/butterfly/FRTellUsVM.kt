package com.tripian.trpcore.ui.butterfly

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.UpdateReaction
import com.tripian.trpcore.domain.model.ButterflyItem
import com.tripian.trpcore.util.extensions.showLoading
import javax.inject.Inject

class FRTellUsVM @Inject constructor(val updateReaction: UpdateReaction) : BaseViewModel(updateReaction) {

    var onDismissListener = MutableLiveData<Unit>()

    var item: ButterflyItem? = null

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        item = arguments!!.getSerializable("item") as ButterflyItem
    }

    fun onClickedVisitedBefore() {
        showLoading()

//        updateReaction.on(UpdateReaction.Params(item!!.reaction!!.id!!, item!!.step!!, ReactionType.THUMBS_DOWN, strings.getString(R.string.i_ve_been_there_before)), success = {
//            hideLoading()
//
//            // Remove from butterfly list
//            eventBus.post(EventMessage(EventConstants.ButterflyWhy, item!!.step!!.id))
//
//            onDismissListener.postValue(Unit)
//        }, error = {
//            hideLoading()
//
//            if (it.type == AlertType.DIALOG) {
//                showDialog(contentText = it.errorDesc)
//            } else {
//                showAlert(AlertType.ERROR, it.errorDesc)
//            }
//        })
    }

    fun onClickedNotLike() {
        showLoading()

//        updateReaction.on(UpdateReaction.Params(item!!.reaction!!.id!!, item!!.step!!, ReactionType.THUMBS_DOWN, strings.getString(R.string.i_don_t_like_this_place)), success = {
//            hideLoading()
//
//            // Remove from butterfly list
//            eventBus.post(EventMessage(EventConstants.ButterflyWhy, item!!.step!!.id))
//
//            onDismissListener.postValue(Unit)
//        }, error = {
//            hideLoading()
//
//            if (it.type == AlertType.DIALOG) {
//                showDialog(contentText = it.errorDesc)
//            } else {
//                showAlert(AlertType.ERROR, it.errorDesc)
//            }
//        })
    }
}
