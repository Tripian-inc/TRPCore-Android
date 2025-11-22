package com.tripian.trpcore.ui.mytrip

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.users.model.User
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetTripianUser
import com.tripian.trpcore.domain.GetUserListener
import com.tripian.trpcore.domain.LogoutUser
import com.tripian.trpcore.ui.companion.ACManageCompanion
import com.tripian.trpcore.ui.splash.ACSplash
import com.tripian.trpcore.ui.user.ACEditProfile
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.dialog.DGActionListener
import javax.inject.Inject

class FRProfileVM @Inject constructor(
    val getTripianUser: GetTripianUser,
    val getUserListener: GetUserListener,
    val logoutUser: LogoutUser
) : BaseViewModel(getTripianUser, logoutUser, getUserListener) {

    var onSetUserListener = MutableLiveData<User>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        getTripianUser.on(success = {
            it.data?.let { onSetUserListener.postValue(it) }
        })

        getUserListener.on(success = {
            onSetUserListener.postValue(it)
        })
    }

    fun onClickedEditProfile() {
        startActivity(ACEditProfile::class)
    }

    fun onClickedCompanion() {
        startActivity(ACManageCompanion::class)
    }

    fun onClickedChangePassword() {
        startActivity(ACEditProfile::class, bundleOf(Pair("changePassword", true)))
    }

    fun onClickedHelp() {
    }

    fun onClickedAbout() {
    }

    fun onClickedLogout() {
        showDialog(
            contentText = getLanguageForKey(LanguageConst.DOU_YOU_CONFIRM),
            positiveBtn = getLanguageForKey(LanguageConst.DELETE),
            negativeBtn = getLanguageForKey(LanguageConst.CANCEL),
            positive = object : DGActionListener {
                override fun onClicked(o: Any?) {
                    logoutUser.on(success = {
                        startActivity(ACSplash::class)

                        finishActivity()
                    })
                }
            }
        )
    }
}