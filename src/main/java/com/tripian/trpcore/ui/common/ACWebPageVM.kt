package com.tripian.trpcore.ui.common

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import javax.inject.Inject

class ACWebPageVM @Inject constructor() : BaseViewModel() {

    var onSetWebUrlListener = MutableLiveData<String>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        onSetWebUrlListener.postValue(arguments!!.getString("url"))
    }

    fun onProgressChanged(progress: Int) {
        if (progress == 100) {
            hideLoading()
        } else {
            showLoading()
        }
    }

    fun onClickedClose() {
        finishActivity()
    }
}
