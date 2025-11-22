package com.tripian.trpcore.domain

import android.text.TextUtils
import com.tripian.one.api.users.model.EmptyResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class ResetPassword @Inject constructor() : BaseUseCase<EmptyResponse, ResetPassword.Params>() {

    class Params(val password: String, val hash: String)

    override fun on(params: Params?) {
        when {
            params!!.password.length < 6 -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ENTER_PSW),
                        type = AlertType.ERROR
                    )
                )
            }
            TextUtils.isEmpty(params.hash) -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ENTER_EMAIL_HASH),
                        type = AlertType.ERROR
                    )
                )
            }
            else -> {
                addObservable {
                    tripianUserRepository.resetPassword(params.password, params.hash)
                }
            }
        }
    }

    override fun isRequiredRefreshToken(): Boolean {
        return false
    }
}