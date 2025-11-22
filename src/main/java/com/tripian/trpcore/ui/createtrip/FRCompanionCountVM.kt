package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import javax.inject.Inject

class FRCompanionCountVM @Inject constructor() : BaseViewModel() {

    @Inject
    lateinit var pageData: PageData

    var onSetAdultListener = MutableLiveData<String>()
    var onSetChildListener = MutableLiveData<String>()
    var onDismissListener = MutableLiveData<Unit>()

    private var adultCount = 0
    private var childCount = 0

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        adultCount = pageData.adult
        childCount = pageData.child

        onSetAdultListener.postValue("$adultCount")
        onSetChildListener.postValue("$childCount")
    }

    fun onClickedUpdate() {
        pageData.adult = adultCount
        pageData.child = childCount

        eventBus.post(EventMessage(EventConstants.CompanionCountSelect, Unit))

        onDismissListener.postValue(Unit)
    }

    fun onClickedMinusAdult() {
        if (adultCount > 1) {
            onSetAdultListener.postValue("${--adultCount}")
        }
    }

    fun onClickedPlusAdult() {
        if (adultCount < 20) {
            onSetAdultListener.postValue("${++adultCount}")
        }
    }

    fun onClickedMinusChild() {
        if (childCount > 0) {
            onSetChildListener.postValue("${--childCount}")
        }
    }

    fun onClickedPlusChild() {
        if (childCount < 20) {
            onSetChildListener.postValue("${++childCount}")
        }
    }
}