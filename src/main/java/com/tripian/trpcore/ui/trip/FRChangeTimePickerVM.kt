package com.tripian.trpcore.ui.trip

import android.os.Bundle
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.goBack
import javax.inject.Inject

class FRChangeTimePickerVM @Inject constructor() : BaseViewModel() {

    var stepId: Int? = null

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)
    }

    fun onClickedOk(startTime: String, endTime: String) {
        goBack()
        eventBus.post(EventMessage(EventConstants.ChangeTimePicker, Triple(stepId, startTime, endTime)))

    }
}
