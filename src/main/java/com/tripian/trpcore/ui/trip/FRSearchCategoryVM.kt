package com.tripian.trpcore.ui.trip

import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import javax.inject.Inject

class FRSearchCategoryVM @Inject constructor() : BaseViewModel() {

    var onDismissListener = MutableLiveData<Unit>()

    fun onClickedItem(category: Int) {
        eventBus.post(EventMessage(EventConstants.SearchCategory, category))

        onDismissListener.postValue(Unit)
    }
}