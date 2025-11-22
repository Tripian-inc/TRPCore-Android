package com.tripian.trpcore.domain

import android.text.TextUtils
import com.tripian.one.api.users.model.LoginResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.isValidEmail
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class DoLogin @Inject constructor() : BaseUseCase<LoginResponse, DoLogin.Params>() {

    class Params(val email: String, val password: String)

    override fun on(params: Params?) {
        when {
            TextUtils.isEmpty(params!!.email) -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ERROR_ENTER_EMAIL),
                        type = AlertType.ERROR
                    )
                )
            }

            !isValidEmail(params.email) -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ERROR_EMAIL_FORMAT),
                        type = AlertType.ERROR
                    )
                )
            }

            TextUtils.isEmpty(params.password) -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ERROR_PASSWORD),
                        type = AlertType.ERROR
                    )
                )
            }

            else -> {
                addObservable {
                    tripianUserRepository.login(params.email, params.password)
                }
            }
        }
    }

    override fun isRequiredRefreshToken(): Boolean {
        return false
    }
}