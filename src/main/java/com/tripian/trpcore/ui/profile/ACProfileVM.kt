package com.tripian.trpcore.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetUser
import com.tripian.trpcore.domain.LogoutUser
import com.tripian.trpcore.ui.common.ACWebPage
import com.tripian.trpcore.ui.companion.ACManageCompanion
import com.tripian.trpcore.ui.companion.CompanionMode
import com.tripian.trpcore.ui.profile.change_langugae.FRLanguageSelect
import com.tripian.trpcore.ui.splash.ACSplash
import com.tripian.trpcore.ui.user.ACEditProfile
import com.tripian.trpcore.util.extensions.capitalized
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.navigateToFragment
import javax.inject.Inject

class ACProfileVM @Inject constructor(val getUser: GetUser, val logoutUser: LogoutUser) : BaseViewModel(getUser) {

    var onSetUserNameListener = MutableLiveData<String>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        getUser.on(success = {
            it.firstName?.let { firstName ->
                val userName = "Hi, ${firstName.capitalized()}"
                onSetUserNameListener.postValue(userName)
            }
        })
    }

    fun onClickedBack() {
        goBack()
    }

    fun onClickedCompanion() {
        startActivity(ACManageCompanion::class, Bundle().apply {
            putSerializable("mode", CompanionMode.PROFILE)
        })
    }

    fun onClickedPersonal() {
        startActivity(ACEditProfile::class)
    }

    fun onClickedLanguage() {

        navigateToFragment(fragment = FRLanguageSelect.newInstance(), addToBackStack = false)
    }

    fun onClickedToe() {
        startActivity(
            ACWebPage::class,
            bundleOf(Pair("url", "https://www.tripian.com/docs/l/tos_t.html"))
        )
    }

    fun onClickedPP() {
        startActivity(
            ACWebPage::class,
            bundleOf(Pair("url", "https://www.tripian.com/docs/l/pp_t.html"))
        )
    }

    fun onClickedAboutUs() {
        startActivity(
            ACWebPage::class,
            bundleOf(Pair("url", "https://www.tripian.com/about.html"))
        )
    }

    fun logout() {
        logoutUser.on(Unit, success = {
            startActivity(ACSplash::class, flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}
