package com.tripian.trpcore.domain

import android.text.TextUtils
import com.tripian.one.api.users.model.UserResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class UpdateUserPassword @Inject constructor() :
    BaseUseCase<UserResponse, UpdateUserPassword.Params>() {

    class Params(val oldPassword: String, val password: String, val passwordConfirm: String)

    override fun on(params: Params?) {
        when {
            TextUtils.isEmpty(params!!.oldPassword) -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ENTER_PSW),
                        type = AlertType.ERROR
                    )
                )
            }
            TextUtils.isEmpty(params.password) -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ENTER_PSW),
                        type = AlertType.ERROR
                    )
                )
            }

            TextUtils.equals(params.password, params.passwordConfirm).not() -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ERROR_PASSWORD_EQUAL),
                        type = AlertType.ERROR
                    )
                )
            }

            else -> {
                addObservable {
                    tripianUserRepository.updatePassword(
                        oldPassword = params.oldPassword,
                        password = params.password
                    )
                }
            }
        }
    }
}