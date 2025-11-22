package com.tripian.trpcore.ui.user

import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.UpdateUserPassword
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import javax.inject.Inject

class FRChangePasswordVM @Inject constructor(private val updateUserPassword: UpdateUserPassword) :
    BaseViewModel(updateUserPassword) {

    var onClearPasswordListener = MutableLiveData<Unit>()

    fun onClickedUpdate(oldPassword: String, password: String, passwordConfirm: String) {
        showLoading()

        updateUserPassword.on(
            UpdateUserPassword.Params(oldPassword, password, passwordConfirm),
            success = {
                hideLoading()

                onClearPasswordListener.postValue(Unit)

                showDialog(
                    title = getLanguageForKey(LanguageConst.SUCCESS),
                    contentText = getLanguageForKey(LanguageConst.SUCCESS_PSW_CHANGE)
                )
            },
            error = {
                hideLoading()

                if (it.type == AlertType.DIALOG) {
                    showDialog(contentText = it.errorDesc)
                } else {
                    showAlert(AlertType.ERROR, it.errorDesc)
                }
            })
    }
}