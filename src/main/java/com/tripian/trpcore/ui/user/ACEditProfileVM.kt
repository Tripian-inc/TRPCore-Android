package com.tripian.trpcore.ui.user

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.navigateToFragment
import javax.inject.Inject

class ACEditProfileVM @Inject constructor() : BaseViewModel() {

    var onSetTitleListener = MutableLiveData<String>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        if (arguments != null && arguments!!.containsKey("changePassword")) {
            onSetTitleListener.postValue(getLanguageForKey(LanguageConst.CHANGE_PSW))

            navigateToFragment(fragment = FRChangePassword.newInstance(), addToBackStack = false)
        } else {
            onSetTitleListener.postValue(getLanguageForKey(LanguageConst.PROFILE))
            navigateToFragment(fragment = FREditProfile.newInstance(), addToBackStack = false)
        }
    }

    fun onClickedClose() {
        finishActivity()
    }
}