package com.tripian.trpcore.ui.login

import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.DoLogin
import com.tripian.trpcore.ui.mytrip.ACMyTrip
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.showLoading
import com.tripian.trpcore.util.fragment.AnimationType
import javax.inject.Inject

class FRLoginVM @Inject constructor(val doLogin: DoLogin) : BaseViewModel(doLogin) {

    fun onClickedLogin(email: String, password: String) {
        showLoading()

        doLogin.on(DoLogin.Params(email, password), success = {
            hideLoading()

            startActivity(ACMyTrip::class)
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })
    }

    fun onClickedRegister() {
        navigateToFragment(FRRegister.newInstance(), animation = AnimationType.ENTER_FROM_RIGHT)
    }

    fun onClickedForgotPassword() {
        navigateToFragment(FRForgotPassword.newInstance(), animation = AnimationType.ENTER_FROM_RIGHT)
    }
}
