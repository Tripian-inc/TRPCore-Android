package com.tripian.trpcore.base

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject

class FRWarningDialogVM @Inject constructor() : BaseViewModel() {

    var onSetTitleListener = MutableLiveData<String>()
    var onSetDescriptionListener = MutableLiveData<String>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        onSetTitleListener.postValue(arguments!!.getString("title"))
        onSetDescriptionListener.postValue(arguments!!.getString("contentText"))
    }
}