package com.tripian.trpcore.ui.login

import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.DoSendMail
import com.tripian.trpcore.domain.ResetPassword
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import javax.inject.Inject

class FRForgotPasswordVM @Inject constructor(val resetPassword: ResetPassword, val doSendMail: DoSendMail) : BaseViewModel(resetPassword, doSendMail) {

    var onShowPasswordListener = MutableLiveData<Unit>()

    fun onClickedSendMail(email: String) {
        showLoading()

        doSendMail.on(DoSendMail.Params(email), success = {
            hideLoading()

            onShowPasswordListener.postValue(Unit)
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(it.type, it.errorDesc)
            }
        })
    }

    fun onClickedResetPassword(password: String, hash: String) {
        showLoading()

        resetPassword.on(ResetPassword.Params(password, hash), success = {
            hideLoading()

            showDialog(
                title = getLanguageForKey(LanguageConst.SUCCESS),
                contentText = getLanguageForKey(LanguageConst.PASSWORD_CHANGED),
                positiveBtn = getLanguageForKey(LanguageConst.CONFIRM),
                positive = object : DGActionListener {
                    override fun onClicked(o: Any?) {
                        goBack()
                    }
                })
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(it.type, it.errorDesc)
            }
        })
    }

    fun onClickedBack() {
        goBack()
    }
}
