package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hours
import javax.inject.Inject

class FRTimePickerVM @Inject constructor() : BaseViewModel() {

    var tag = ""

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        tag = arguments!!.getString("tag")!!
    }

    fun onClickedOk(time: String) {
        eventBus.post(EventMessage(EventConstants.TimePicker, Pair(tag, time)))

        goBack()
    }
}
